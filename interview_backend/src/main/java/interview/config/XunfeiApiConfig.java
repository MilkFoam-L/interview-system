package interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 讯飞API统一配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "xfyun")
public class XunfeiApiConfig {
    
    /**
     * Spark大模型配置
     */
    private SparkConfig spark = new SparkConfig();
    
    /**
     * TTS语音合成配置
     */
    private TtsConfig tts = new TtsConfig();
    
    /**
     * RTASR实时语音识别配置
     */
    private RtasrConfig rtasr = new RtasrConfig();
    
    /**
     * Spark大模型配置
     */
    @Data
    public static class SparkConfig {
        private String appId;
        private String apiKey;
        private String apiSecret;
        private String hostUrl;
        private String domain;
    }
    
    /**
     * TTS语音合成配置
     */
    @Data
    public static class TtsConfig {
        private String appId;
        private String apiKey;
        private String apiSecret;
        private String hostUrl;
    }
    
    /**
     * RTASR实时语音识别配置
     */
    @Data
    public static class RtasrConfig {
        private String appId;
        private String apiKey;
        private String url;
    }
    
    /**
     * 获取默认的AppId（兼容旧代码）
     * 优先使用RTASR的AppId，如果不存在则使用Spark的AppId
     */
    public String getAppId() {
        if (rtasr.getAppId() != null && !rtasr.getAppId().isEmpty()) {
            return rtasr.getAppId();
        }
        return spark.getAppId();
    }
    
    /**
     * 获取默认的ApiKey（兼容旧代码）
     * 使用Spark的ApiKey
     */
    public String getApiKey() {
        return spark.getApiKey();
    }
    
    /**
     * 获取默认的ApiSecret（兼容旧代码）
     * 使用Spark的ApiSecret
     */
    public String getApiSecret() {
        return spark.getApiSecret();
    }
} 