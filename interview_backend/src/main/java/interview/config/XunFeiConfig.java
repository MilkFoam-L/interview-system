package interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 讯飞API配置
 */
@Component
@ConfigurationProperties(prefix = "xunfei")
public class XunFeiConfig {
    /**
     * 人脸水印照比对接口地址
     */
    private String watermarkUrl = "http://api.xfyun.cn/v1/service/v1/image_identify/watermark_verification";
    
    /**
     * 实时语音转写接口地址
     */
    private String rtasrUrl = "wss://rtasr.xfyun.cn/v1/ws";
    
    /**
     * 应用ID（人脸识别）
     */
    private String appId;
    
    /**
     * API密钥（人脸识别）
     */
    private String apiKey;
    
    /**
     * 应用ID（语音转写）
     */
    private String rtasrAppId;
    
    /**
     * API密钥（语音转写）
     */
    private String rtasrApiKey;
    
    /**
     * 连接超时时间（秒）
     */
    private int rtasrConnectTimeout = 30;
    
    /**
     * 读取超时时间（秒）
     */
    private int rtasrReadTimeout = 60;
    
    /**
     * 最大重试次数
     */
    private int rtasrMaxRetry = 3;
    
    /**
     * 简历生成配置
     */
    private Resume resume = new Resume();
    
    /**
     * 人脸检测配置
     */
    private FaceDetect faceDetect = new FaceDetect();
    
    public static class Resume {
        private String appId = "168b0667";
        private String apiKey = "d6e253f3b4cedd37d14c46a9d6f2bbc0";
        private String apiSecret = "d6e253f3b4cedd37d14c46a9d6f2bbc0";
        private String url = "https://api.xfyun.cn/v1/service/v1/resume_generation";
        
        public String getAppId() {
            return appId;
        }
        
        public void setAppId(String appId) {
            this.appId = appId;
        }
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getApiSecret() {
            return apiSecret;
        }
        
        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    public static class FaceDetect {
        private String appId = "8f44cb96";
        private String apiKey = "fd2b21473ed3dfd413bc59e682091bb8";
        private String apiSecret = "NmYzNDIwNzdiNzljOGQzZjU2ZDQxMzQ0";
        private String serviceId = "s67c9c78c";
        private String requestUrl = "https://api.xf-yun.com/v1/private/s67c9c78c";
        
        public String getAppId() {
            return appId;
        }
        
        public void setAppId(String appId) {
            this.appId = appId;
        }
        
        public String getApiKey() {
            return apiKey;
        }
        
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
        
        public String getApiSecret() {
            return apiSecret;
        }
        
        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }
        
        public String getServiceId() {
            return serviceId;
        }
        
        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }
        
        public String getRequestUrl() {
            return requestUrl;
        }
        
        public void setRequestUrl(String requestUrl) {
            this.requestUrl = requestUrl;
        }
    }
    
    // Getter 和 Setter 方法
    public String getWatermarkUrl() {
        return watermarkUrl;
    }
    
    public void setWatermarkUrl(String watermarkUrl) {
        this.watermarkUrl = watermarkUrl;
    }
    
    public String getRtasrUrl() {
        return rtasrUrl;
    }
    
    public void setRtasrUrl(String rtasrUrl) {
        this.rtasrUrl = rtasrUrl;
    }
    
    public String getAppId() {
        return appId;
    }
    
    public void setAppId(String appId) {
        this.appId = appId;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getRtasrAppId() {
        return rtasrAppId;
    }
    
    public void setRtasrAppId(String rtasrAppId) {
        this.rtasrAppId = rtasrAppId;
    }
    
    public String getRtasrApiKey() {
        return rtasrApiKey;
    }
    
    public void setRtasrApiKey(String rtasrApiKey) {
        this.rtasrApiKey = rtasrApiKey;
    }
    
    public Resume getResume() {
        return resume;
    }
    
    public void setResume(Resume resume) {
        this.resume = resume;
    }
    
    public FaceDetect getFaceDetect() {
        return faceDetect;
    }
    
    public void setFaceDetect(FaceDetect faceDetect) {
        this.faceDetect = faceDetect;
    }
} 