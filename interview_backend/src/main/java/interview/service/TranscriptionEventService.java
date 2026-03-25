package interview.service;

/**
 * 转写事件服务，作为WebSocketHandler和TranscriptionService之间的桥梁
 */
public interface TranscriptionEventService {
    
    /**
     * 发送转写结果到前端
     */
    void sendTranscriptionResult(String sessionId, String result);
    
    /**
     * 发送错误消息到前端
     */
    void sendError(String sessionId, String errorMessage);
    
    /**
     * 处理音频数据
     */
    void processAudio(byte[] audioData, String sessionId);
    
    /**
     * 结束转写
     */
    void endTranscription(String sessionId);
} 