package interview.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import interview.model.entity.InterviewSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对应表 comprehensive_reports
 */
@Data
@Entity
@Table(name = "comprehensive_reports")
public class ComprehensiveReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "job_id", nullable = false)
    private Integer jobId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "position_name")
    private String positionName;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "duration", nullable = false)
    private Integer duration = 0;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "star_rating", nullable = false, precision = 3, scale = 1)
    private BigDecimal starRating = BigDecimal.ZERO;

    @Column(name = "star_situation_score", precision = 5, scale = 0)
    private BigDecimal starSituationScore = BigDecimal.ZERO;

    @Column(name = "star_task_score", precision = 5, scale = 0)
    private BigDecimal starTaskScore = BigDecimal.ZERO;

    @Column(name = "star_action_score", precision = 5, scale = 0)
    private BigDecimal starActionScore = BigDecimal.ZERO;

    @Column(name = "star_result_score", precision = 5, scale = 0)
    private BigDecimal starResultScore = BigDecimal.ZERO;

    @Column(name = "ability_matrix_data", columnDefinition = "json")
    private String abilityMatrixData;

    @Lob
    @Column(name = "comprehensive_analysis", columnDefinition = "TEXT")
    private String comprehensiveAnalysis;

    @Lob
    @Column(name = "basic_qa_analysis", columnDefinition = "TEXT")
    private String basicQaAnalysis;

    @Lob
    @Column(name = "improvements", columnDefinition = "TEXT")
    private String improvements;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // 添加外键关联，使用@JsonIgnore避免循环引用
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", insertable = false, updatable = false)
    @JsonIgnore
    private InterviewSession interviewSession;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", insertable = false, updatable = false)
    @JsonIgnore
    private Job job;
    
    /**
     * 为了保持向后兼容，提供interviewTime字段的JSON序列化
     * 返回开始时间作为面试时间
     */
    @JsonProperty("interviewTime")
    public LocalDateTime getInterviewTime() {
        return this.startTime;
    }
    
    /**
     * 为了保持向后兼容，提供设置面试时间的方法
     * 设置开始时间
     */
    @JsonProperty("interviewTime")
    public void setInterviewTime(LocalDateTime interviewTime) {
        this.startTime = interviewTime;
    }
} 