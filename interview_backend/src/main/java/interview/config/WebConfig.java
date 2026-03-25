package interview.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web配置类，用于处理跨域等全局配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

    // CORS配置已移至SecurityConfig.java中统一管理，避免配置冲突

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 简历照片映射
        registry.addResourceHandler("/api/resume/photos/**")
                .addResourceLocations("file:interview_backend/uploads/photos/");
                
        // 用户头像映射
        registry.addResourceHandler("/api/avatars/**")
                .addResourceLocations("file:interview_backend/uploads/avatars/");
        
        // 公司Logo映射
        registry.addResourceHandler("/api/companies/logos/**")
                .addResourceLocations("file:interview_backend/uploads/logos/");
        
        // 营业执照映射
        registry.addResourceHandler("/api/companies/licenses/**")
                .addResourceLocations("file:interview_backend/uploads/licenses/");
        
        logger.info("资源映射配置完成");
    }
} 