package interview;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.PropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import interview.config.ServerConfig;
import interview.config.DatabaseConfig;

@SpringBootApplication
@EntityScan("interview.model")
@EnableJpaRepositories("interview.repository")
@PropertySource("classpath:application.properties")
public class InterviewApplication implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(InterviewApplication.class);

    @Resource
    private Environment environment;
    
    @Autowired
    private ServerConfig serverConfig;
    
    @Autowired
    private DatabaseConfig databaseConfig;
    
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(InterviewApplication.class);
        application.setDefaultProperties(java.util.Collections.singletonMap("spring.config.location", "classpath:application.properties"));
        application.run(args);
    }

    @Override
    public void run(ApplicationArguments args) {
        String serverPort = serverConfig.getPort(); // 使用ServerConfig
        logger.info("=========================");
        logger.info("应用启动在端口: {}", serverPort);
        logger.info("=========================");

        // 测试数据库连接
        logger.info("测试数据库连接...");
        try {
            // 使用DatabaseConfig获取数据库URL
            String dbUrl = databaseConfig.getUrl(); // 直接使用Lombok生成的getter
            if (dbUrl != null && !dbUrl.isEmpty()) {
                logger.info("数据库连接成功！");
            } else {
                // 如果URL为空，尝试使用带默认值的有效getter
                String effectiveUrl = databaseConfig.getEffectiveUrl();
                if (effectiveUrl != null && !effectiveUrl.isEmpty()) {
                    logger.info("使用默认数据库连接成功！");
                } else {
                    logger.error("无法获取数据库连接URL");
                }
            }
        } catch (Exception e) {
            logger.error("数据库连接测试失败: {}", e.getMessage());
        }
        logger.info("=========================");
    }
}