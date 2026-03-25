package interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 讯飞人脸检测配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "xunfei.face-detect")
public class FaceDetectConfig {
    /**
     * 请求地址
     */
    private String requestUrl;
    
    /**
     * APPID
     */
    private String appId;
    
    /**
     * API Secret
     */
    private String apiSecret;
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * 服务ID
     */
    private String serviceId;
    
    // 手动添加getter和setter
    public String getRequestUrl() {
        return requestUrl;
    }
    
    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }
    
    public String getAppId() {
        return appId;
    }
    
    public void setAppId(String appId) {
        this.appId = appId;
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
    
    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
} 