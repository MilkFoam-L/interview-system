package interview.service;

/**
 * 语音转写服务接口
 */
public interface SpeechTranscriptionService {
    
    /**
     * 处理音频数据并进行转写
     * @param audioData 音频数据（PCM格式）
     * @param sessionId 会话ID，用于区分不同用户
     */
    void processAudio(byte[] audioData, String sessionId);
    
    /**
     * 结束转写会话
     * @param sessionId 会话ID
     */
    void endTranscription(String sessionId);
} 