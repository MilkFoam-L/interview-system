package interview.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import interview.service.TranscriptionEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriTemplate;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音转写WebSocket处理器
 */
@Component
public class SpeechWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SpeechWebSocketHandler.class);
    private static final UriTemplate SESSION_URI_TEMPLATE = new UriTemplate("/ws/speech/{sessionId}");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionIds = new ConcurrentHashMap<>();

    @Autowired
    @Lazy
    private TranscriptionEventService eventService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从URI中提取会话ID
        Map<String, String> uriVariables = SESSION_URI_TEMPLATE.match(session.getUri().getPath());
        String sessionId = uriVariables.get("sessionId");

        if (sessionId != null) {
            sessions.put(sessionId, session);
            sessionIds.put(session, sessionId);
            logger.info("WebSocket连接已建立: sessionId={}", sessionId);
        } else {
            logger.error("无法从URI中提取会话ID");
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String type = jsonNode.get("type").asText();
            String sessionId = jsonNode.get("sessionId").asText();

            if ("audio".equals(type)) {
                // 处理音频数据
                String audioData = jsonNode.get("audioData").asText();
                byte[] decodedAudio = Base64.getDecoder().decode(audioData);
                eventService.processAudio(decodedAudio, sessionId);
            } else if ("end".equals(type)) {
                // 结束转写
                eventService.endTranscription(sessionId);
            }
        } catch (Exception e) {
            logger.error("处理WebSocket消息失败", e);
            sendError(session, "处理消息失败: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = sessionIds.remove(session);
        if (sessionId != null) {
            sessions.remove(sessionId);
            eventService.endTranscription(sessionId);
            logger.info("WebSocket连接已关闭: sessionId={}, status={}", sessionId, status);
        }
    }

    /**
     * 发送转写结果
     */
    public void sendTranscriptionResult(String sessionId, String result) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(Map.of(
                        "type", "transcription",
                        "content", result
                ));
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                logger.error("发送转写结果失败", e);
            }
        }
    }

    /**
     * 发送错误消息
     */
    public void sendError(WebSocketSession session, String errorMessage) {
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(Map.of(
                        "type", "error",
                        "content", errorMessage
                ));
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                logger.error("发送错误消息失败", e);
            }
        }
    }

    /**
     * 发送错误消息（通过会话ID）
     */
    public void sendError(String sessionId, String errorMessage) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null) {
            sendError(session, errorMessage);
        }
    }
}

