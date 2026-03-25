package interview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

/**
 * 安全配置类
 * 提供密码编码器和其他安全相关配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    /**
     * 密码编码器
     * 使用BCrypt算法进行密码加密
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.info("创建密码编码器: BCryptPasswordEncoder");
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 配置安全过滤链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("配置Security过滤链...");
        http
            .csrf(csrf -> csrf.disable()) // 禁用CSRF
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 启用CORS
            .authorizeHttpRequests(auth -> auth
                // 允许访问公开接口
                .requestMatchers("/api/users/login", "/api/users/register", "/api/users/check-username").permitAll()
                // 允许访问静态资源
                .requestMatchers("/api/avatars/**", "/api/resume/photos/**").permitAll()
                // 允许访问Swagger文档
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**").permitAll()
                // 允许WebSocket连接
                .requestMatchers("/ws/**").permitAll()
                // 允许健康检查
                .requestMatchers("/api/user-status/check").permitAll()
                // 其他请求需要认证
                .anyRequest().permitAll()
            );
        
        logger.info("Security过滤链配置完成");
        return http.build();
    }
    
    /**
     * 自定义CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        logger.info("配置CORS来源...");
        CorsConfiguration configuration = new CorsConfiguration();
        // 使用具体的前端地址，支持开发和生产环境
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:5173",    // Vite开发服务器
            "http://127.0.0.1:5173",    // 本地回环地址
            "http://localhost:*",       // 支持其他本地端口
            "http://127.0.0.1:*"        // 支持其他本地端口
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "X-User-ID"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        logger.info("CORS来源配置完成");
        return source;
    }
} 