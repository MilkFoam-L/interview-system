package interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * 服务器配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "server")
public class ServerConfig {
    /**
     * 服务器端口
     */
    private String port;
    
    /**
     * 上下文路径
     */
    private String contextPath;
    
    /**
     * 编码设置
     */
    private EncodingConfig encoding = new EncodingConfig();

    /**
     * 编码设置配置
     */
    @Data
    public static class EncodingConfig {
        private String charset;
        private boolean force;
        private boolean enabled;
    }
} 