package interview.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 代码执行记录实体类
 * 对应 code_execution 表，存储代码执行的详细信息和结果
 */
@Data
@Entity
@Table(name = "code_execution")
public class CodeExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的答题记录ID
     */
    @Column(name = "answer_id", nullable = false)
    private Long answerId;
    
    /**
     * 面试会话ID
     */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;
    
    /**
     * 执行的代码内容
     */
    @Column(name = "code", columnDefinition = "TEXT", nullable = false)
    private String code;
    
    /**
     * 编程语言
     */
    @Column(name = "language", length = 50, nullable = false)
    private String language;
    
    /**
     * 执行结果
     */
    @Column(name = "execution_result", columnDefinition = "TEXT")
    private String executionResult;
    
    /**
     * 执行状态：PENDING, SUCCESS, ERROR
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;
    
    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 执行时间（毫秒）
     */
    @Column(name = "execution_time")
    private Integer executionTime;
    
    /**
     * 内存使用（KB）
     */
    @Column(name = "memory_usage")
    private Integer memoryUsage;
    
    /**
     * AI分析结果
     */
    @Column(name = "ai_analysis", columnDefinition = "TEXT")
    private String aiAnalysis;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    /**
     * 自动设置创建和更新时间
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
    
    /**
     * 执行状态枚举
     */
    public static class Status {
        public static final String PENDING = "PENDING";
        public static final String SUCCESS = "SUCCESS";  
        public static final String ERROR = "ERROR";
    }
}
