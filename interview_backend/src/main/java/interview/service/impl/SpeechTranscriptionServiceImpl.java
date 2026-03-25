package interview.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import interview.config.XunFeiConfig;
import interview.service.SpeechTranscriptionService;
import interview.service.TranscriptionEventService;
import cn.xfyun.api.RtasrClient;
import cn.xfyun.model.response.rtasr.RtasrResponse;
import cn.xfyun.service.rta.AbstractRtasrWebSocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;
import okio.ByteString;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.security.SignatureException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 语音转写服务实现类
 */
@Service
public class SpeechTranscriptionServiceImpl implements SpeechTranscriptionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SpeechTranscriptionServiceImpl.class);
    
    // 重试配置
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2秒
    
    @Autowired
    @Qualifier("xunFeiConfig")
    private XunFeiConfig xunfeiConfig;
    
    @Autowired
    @Lazy
    private TranscriptionEventService eventService;
    
    // 存储每个会话的客户端和结果
    private final Map<String, ClientSession> clientSessions = new ConcurrentHashMap<>();
    
    // 调度器用于重试
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    /**
     * 会话信息类，包含客户端和转写结果
     */
    private static class ClientSession {
        RtasrClient client;
        StringBuffer finalResult = new StringBuffer();
        WebSocket webSocket;
        int retryCount = 0;
        volatile boolean isConnecting = false;
        
        public ClientSession(RtasrClient client) {
            this.client = client;
        }
    }
    
    @Override
    public void processAudio(byte[] audioData, String sessionId) {
        // 获取或创建会话
        ClientSession session = clientSessions.computeIfAbsent(sessionId, this::createClientSession);
        try {
            // 如果WebSocket尚未初始化，则初始化
            if (session.webSocket == null && !session.isConnecting) {
                connectWithRetry(session, sessionId, 0);
            }
            
            // 发送音频数据（只有在连接成功后才发送）
            if (session.webSocket != null) {
                session.webSocket.send(ByteString.of(audioData));
            } else {
                logger.warn("WebSocket连接尚未建立，音频数据将被丢弃: sessionId={}", sessionId);
            }
        } catch (Exception e) {
            logger.error("处理音频数据失败: {}", e.getMessage(), e);
            eventService.sendError(sessionId, "处理音频数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 带重试的连接方法
     */
    private void connectWithRetry(ClientSession session, String sessionId, int attemptCount) {
        if (attemptCount >= MAX_RETRY_ATTEMPTS) {
            logger.error("WebSocket连接重试次数超限，放弃连接: sessionId={}", sessionId);
            eventService.sendError(sessionId, "连接讯飞语音转写服务失败，请检查网络连接");
            session.isConnecting = false;
            return;
        }
        
        session.isConnecting = true;
        session.retryCount = attemptCount;
        
        logger.info("尝试连接讯飞语音转写服务: sessionId={}, 尝试次数={}", sessionId, attemptCount + 1);
        
        try {
            // 创建WebSocket连接
            session.webSocket = session.client.newWebSocket(createWebSocketListener(sessionId));
            
            // 设置连接超时检查
            scheduler.schedule(() -> {
                if (session.webSocket == null && session.isConnecting) {
                    logger.warn("WebSocket连接超时，准备重试: sessionId={}", sessionId);
                    retryConnection(session, sessionId, attemptCount + 1);
                }
            }, 15, TimeUnit.SECONDS); // 15秒超时
            
        } catch (Exception e) {
            logger.error("创建WebSocket连接失败: sessionId={}, 错误={}", sessionId, e.getMessage());
            session.isConnecting = false;
            retryConnection(session, sessionId, attemptCount + 1);
        }
    }
    
    /**
     * 重试连接
     */
    private void retryConnection(ClientSession session, String sessionId, int nextAttempt) {
        if (nextAttempt < MAX_RETRY_ATTEMPTS) {
            logger.info("将在{}毫秒后重试连接: sessionId={}", RETRY_DELAY_MS, sessionId);
            scheduler.schedule(() -> {
                connectWithRetry(session, sessionId, nextAttempt);
            }, RETRY_DELAY_MS, TimeUnit.MILLISECONDS);
        } else {
            logger.error("连接重试次数已达上限，停止重试: sessionId={}", sessionId);
            eventService.sendError(sessionId, "连接讯飞语音转写服务失败，请稍后重试");
            session.isConnecting = false;
        }
    }
    
    @Override
    public void endTranscription(String sessionId) {
        ClientSession session = clientSessions.remove(sessionId);
        if (session != null) {
            try {
                // 发送结束标识
                if (session.client != null) {
                    session.client.sendEnd();
                }
                
                // 彻底清理会话资源
                session.isConnecting = false;
                if (session.webSocket != null) {
                    try {
                        session.webSocket.close(1000, "Session ended");
                    } catch (Exception e) {
                        logger.warn("关闭WebSocket失败: {}", e.getMessage());
                    }
                    session.webSocket = null;
                }
                
                // 清空转写结果缓冲区，避免内存泄漏
                if (session.finalResult != null) {
                    session.finalResult.setLength(0);
                }
                
                logger.info("语音转写会话已结束并清理: sessionId={}", sessionId);
            } catch (Exception e) {
                logger.error("结束转写会话失败: {}", e.getMessage(), e);
                eventService.sendError(sessionId, "结束转写会话失败: " + e.getMessage());
            }
        } else {
            logger.debug("尝试结束不存在的转写会话: sessionId={}", sessionId);
        }
    }
    
    /**
     * 创建客户端会话
     */
    private ClientSession createClientSession(String sessionId) {
        try {
            // 验证配置
            if (xunfeiConfig.getRtasrAppId() == null || xunfeiConfig.getRtasrApiKey() == null) {
                throw new RuntimeException("讯飞语音转写配置不完整，请检查appId和apiKey");
            }
            
            logger.info("正在创建讯飞实时语音转写客户端: sessionId={}, appId={}", sessionId, xunfeiConfig.getRtasrAppId());
            
            // 创建讯飞实时语音转写客户端
            RtasrClient client = new RtasrClient.Builder()
                    .signature(xunfeiConfig.getRtasrAppId(), xunfeiConfig.getRtasrApiKey())
                    .build();
            
            ClientSession session = new ClientSession(client);
            // 确保新会话的转写结果缓冲区是干净的
            session.finalResult.setLength(0);
            
            logger.info("创建讯飞实时语音转写客户端成功，sessionId={}, appId={}", sessionId, xunfeiConfig.getRtasrAppId());
            return session;
        } catch (Exception e) {
            logger.error("创建讯飞客户端失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
            throw new RuntimeException("讯飞API鉴权失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建WebSocket监听器
     */
    private AbstractRtasrWebSocketListener createWebSocketListener(String sessionId) {
        return new AbstractRtasrWebSocketListener() {
            @Override
            public void onSuccess(WebSocket webSocket, String text) {
                ClientSession session = clientSessions.get(sessionId);
                if (session != null) {
                    // 连接成功，重置标志
                    session.isConnecting = false;
                    logger.info("WebSocket连接成功: sessionId={}", sessionId);
                    
                    try {
                        RtasrResponse response = JSONObject.parseObject(text, RtasrResponse.class);
                        // 处理转写结果
                        String tempResult = handleAndReturnContent(response.getData(), session.finalResult);
                        
                        // 修复：只发送当前会话的完整结果，避免发送历史累积内容
                        // 发送本次会话的所有结果（finalResult是本次会话累积的最终结果，tempResult是临时结果）
                        String currentSessionResult = session.finalResult.toString();
                        if (!tempResult.isEmpty()) {
                            currentSessionResult = currentSessionResult.isEmpty() ? tempResult : currentSessionResult + tempResult;
                        }
                        eventService.sendTranscriptionResult(sessionId, currentSessionResult);
                    } catch (Exception e) {
                        logger.error("处理转写结果失败: {}", e.getMessage(), e);
                    }
                }
            }
            
            @Override
            public void onFail(WebSocket webSocket, Throwable t, @Nullable Response response) {
                logger.error("WebSocket连接失败: sessionId={}, 错误={}", sessionId, t.getMessage(), t);
                
                ClientSession session = clientSessions.get(sessionId);
                if (session != null) {
                    session.webSocket = null;
                    session.isConnecting = false;
                    
                    // 如果是连接超时，尝试重试
                    if (t.getMessage().contains("timeout") || t.getMessage().contains("Connect timed out")) {
                        if (session.retryCount < MAX_RETRY_ATTEMPTS) {
                            logger.info("检测到连接超时，准备重试: sessionId={}", sessionId);
                            retryConnection(session, sessionId, session.retryCount + 1);
                            return;
                        }
                    }
                }
                
                // 发送错误信息给前端
                String errorMsg = "转写服务连接失败: " + t.getMessage();
                if (t.getMessage().contains("timeout")) {
                    errorMsg = "连接讯飞服务器超时，请检查网络连接或稍后重试";
                }
                eventService.sendError(sessionId, errorMsg);
            }
            
            @Override
            public void onBusinessFail(WebSocket webSocket, String text) {
                logger.error("业务处理失败: {}", text);
                eventService.sendError(sessionId, "转写服务业务处理失败: " + text);
            }
            
            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                logger.info("WebSocket正在关闭: sessionId={}, code={}, reason={}", sessionId, code, reason);
            }
            
            @Override
            public void onClosed() {
                logger.info("WebSocket已关闭: sessionId={}", sessionId);
                ClientSession session = clientSessions.remove(sessionId);
                if (session != null) {
                    session.webSocket = null;
                    session.isConnecting = false;
                    
                    // 清空转写结果缓冲区，避免内存泄漏
                    if (session.finalResult != null) {
                        session.finalResult.setLength(0);
                    }
                    
                    logger.debug("WebSocket会话资源已清理: sessionId={}", sessionId);
                }
            }
        };
    }
    
    /**
     * 解析转写结果
     */
    private String handleAndReturnContent(String message, StringBuffer finalResult) {
        StringBuilder tempResult = new StringBuilder();
        
        try {
            // 解析服务端返回的文本内容
            JSONObject messageObj = JSON.parseObject(message);
            JSONObject cn = messageObj.getJSONObject("cn");
            JSONObject st = cn.getJSONObject("st");
            JSONArray rtArr = st.getJSONArray("rt");
            
            for (int i = 0; i < rtArr.size(); i++) {
                JSONObject rtArrObj = rtArr.getJSONObject(i);
                JSONArray wsArr = rtArrObj.getJSONArray("ws");
                for (int j = 0; j < wsArr.size(); j++) {
                    JSONObject wsArrObj = wsArr.getJSONObject(j);
                    JSONArray cwArr = wsArrObj.getJSONArray("cw");
                    for (int k = 0; k < cwArr.size(); k++) {
                        JSONObject cwArrObj = cwArr.getJSONObject(k);
                        String wStr = cwArrObj.getString("w");
                        tempResult.append(wStr);
                    }
                }
            }
            
            // 根据中间结果类型更新最终结果
            String type = st.getString("type");
            if (StringUtils.equals("1", type)) {
                // 中间结果，不保存
                return tempResult.toString();
            } else if (StringUtils.equals("0", type)) {
                // 最终结果，保存
                finalResult.append(tempResult);
                return "";
            } else {
                logger.warn("未知的转写响应类型：{}", type);
                return tempResult.toString();
            }
        } catch (Exception e) {
            logger.error("解析转写结果失败: {}", e.getMessage(), e);
            return message;
        }
    }
    
    /**
     * 获取连接状态信息（用于调试）
     */
    public String getConnectionStatus(String sessionId) {
        ClientSession session = clientSessions.get(sessionId);
        if (session == null) {
            return "会话不存在";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("会话ID: ").append(sessionId).append("\n");
        status.append("WebSocket状态: ").append(session.webSocket != null ? "已连接" : "未连接").append("\n");
        status.append("是否正在连接: ").append(session.isConnecting ? "是" : "否").append("\n");
        status.append("重试次数: ").append(session.retryCount).append("\n");
        
        return status.toString();
    }
} 