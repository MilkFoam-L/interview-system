package interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 星火X1大模型配置类
 * 负责管理API密钥、请求参数等配置信息
 * 使用星火X1开放API，只需要APIPassword认证
 */
@Data
@Component
@ConfigurationProperties(prefix = "xfyun.spark-x1")
public class SparkX1Config {
    
    /**
     * API密码 - 星火X1开放API认证凭据
     * 格式：key:secret
     */
    private String apiPassword = "rCGYHXqURhxBCYgiDoDx:mgwfvPABvogmzrswdnFI";
    
    /**
     * API主机地址 - 星火X1开放API
     */
    private String hostUrl = "https://spark-api-open.xf-yun.com/v2/chat/completions";
    
    /**
     * 模型名称
     */
    private String model = "x1";
    
    /**
     * 最大Token数
     */
    private Integer maxTokens = 4096;
    
    /**
     * 温度参数（0-1，值越高越随机）
     */
    private Double temperature = 0.1;
    
    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout = 30000;
    
    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 60000;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetry = 3;
    
    /**
     * 是否启用AI代码判题
     */
    private Boolean enabled = true;
}
