package interview.service.impl;

import interview.service.TranscriptionEventService;
import interview.websocket.SpeechWebSocketHandler;
import interview.service.SpeechTranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 转写事件服务实现类，作为WebSocketHandler和TranscriptionService之间的桥梁
 */
@Service
public class TranscriptionEventServiceImpl implements TranscriptionEventService {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionEventServiceImpl.class);

    private SpeechWebSocketHandler webSocketHandler;
    private SpeechTranscriptionService transcriptionService;

    /**
     * 发送转写结果到前端
     */
    @Override
    public void sendTranscriptionResult(String sessionId, String result) {
        if (webSocketHandler != null) {
            webSocketHandler.sendTranscriptionResult(sessionId, result);
        } else {
            logger.error("webSocketHandler未注入，无法发送转写结果");
        }
    }

    /**
     * 发送错误消息到前端
     */
    @Override
    public void sendError(String sessionId, String errorMessage) {
        if (webSocketHandler != null) {
            webSocketHandler.sendError(sessionId, errorMessage);
        } else {
            logger.error("webSocketHandler未注入，无法发送错误消息");
        }
    }

    /**
     * 处理音频数据
     */
    @Override
    public void processAudio(byte[] audioData, String sessionId) {
        if (transcriptionService != null) {
            transcriptionService.processAudio(audioData, sessionId);
        } else {
            logger.error("transcriptionService未注入，无法处理音频数据");
        }
    }

    /**
     * 结束转写
     */
    @Override
    public void endTranscription(String sessionId) {
        if (transcriptionService != null) {
            transcriptionService.endTranscription(sessionId);
        } else {
            logger.error("transcriptionService未注入，无法结束转写");
        }
    }

    @Autowired
    @Lazy
    public void setWebSocketHandler(SpeechWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
        logger.info("注入 SpeechWebSocketHandler 成功");
    }

    @Autowired
    public void setTranscriptionService(SpeechTranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
        logger.info("注入 SpeechTranscriptionService 成功");
    }
} 