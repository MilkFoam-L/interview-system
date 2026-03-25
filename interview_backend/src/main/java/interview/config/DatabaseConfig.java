package interview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;

/**
 * 数据库配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class DatabaseConfig {
    // 默认值配置
    private static final String DEFAULT_URL = "jdbc:mysql://rm-cn-3ic4cpsrn0001elo.rwlb.rds.aliyuncs.com:3306/interview_cloud?useSSL=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8&useUnicode=true";
    private static final String DEFAULT_USERNAME = "interview_cloud";
    private static final String DEFAULT_PASSWORD = "interviewDB_";
    private static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    /**
     * 数据库连接URL
     */
    private String url;
    
    /**
     * 数据库用户名
     */
    private String username;
    
    /**
     * 数据库密码
     */
    private String password;
    
    /**
     * 数据库驱动类名
     */
    private String driverClassName;

    /**
     * 获取数据库URL，如果为空则使用默认值
     */
    public String getEffectiveUrl() {
        return url != null ? url : DEFAULT_URL;
    }
    
    /**
     * 获取数据库用户名，如果为空则使用默认值
     */
    public String getEffectiveUsername() {
        return username != null ? username : DEFAULT_USERNAME;
    }
    
    /**
     * 获取数据库密码，如果为空则使用默认值
     */
    public String getEffectivePassword() {
        return password != null ? password : DEFAULT_PASSWORD;
    }
    
    /**
     * 获取数据库驱动类名，如果为空则使用默认值
     */
    public String getEffectiveDriverClassName() {
        return driverClassName != null ? driverClassName : DEFAULT_DRIVER;
    }
} 