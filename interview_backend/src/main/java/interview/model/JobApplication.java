package interview.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 简历投递记录实体类
 */
@Data
@Entity
@Table(name = "job_applications")
public class JobApplication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "job_id", nullable = false)
    private Integer jobId;
    
    @Column(name = "resume_id")
    private Long resumeId;
    
    /**
     * 状态(0-待审核,1-已通过,2-已拒绝,3-已安排面试)
     */
    private Integer status;
    
    private String feedback;
    
    @Lob
    @Column(columnDefinition = "TEXT", name = "resumeSnapshot")
    private String resumeSnapshot;
    
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "ai_screening_status", nullable = false)
    private Byte aiScreeningStatus = 0; // 0-待处理,1-已通过,2-已拒绝
    
    @Lob
    @Column(name = "ai_screening_reason", columnDefinition = "TEXT")
    private String aiScreeningReason;
    
    @Column(name = "ai_screening_notified_at")
    private LocalDateTime aiScreeningNotifiedAt;
    
    @Column(name = "ai_screening_notified_by")
    private Long aiScreeningNotifiedBy;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Job job;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resume_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Resume resume;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) {
            status = 0; // 默认待审核
        }
        if (aiScreeningStatus == null) {
            aiScreeningStatus = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
} 