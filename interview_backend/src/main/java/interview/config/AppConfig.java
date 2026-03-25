package interview.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置类
 */
@Configuration
@EnableConfigurationProperties({
    XunFeiConfig.class, 
    FaceDetectConfig.class,
    ServerConfig.class,
    DatabaseConfig.class,
    XunfeiApiConfig.class
})
public class AppConfig {
    // 配置启用
} 