package interview.model.request;

/**
 * 音频数据请求模型
 */
public class AudioDataRequest {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * Base64编码的音频数据
     */
    private String audioData;
    
    // Getter 和 Setter 方法
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getAudioData() {
        return audioData;
    }
    
    public void setAudioData(String audioData) {
        this.audioData = audioData;
    }
} 