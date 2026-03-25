package interview.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "job_id", nullable = false)
    private Integer jobId;
    
    @Column(name = "application_id")
    private Long applicationId;
    
    @Column(name = "face_verification_id")
    private Long faceVerificationId;
    
    @Column(name = "session_status", nullable = false)
    private String sessionStatus = "PREPARING";
    
    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    @Column(name = "duration")
    private Integer duration = 0;
    
    @Column(name = "progress")
    private Integer progress = 0;
    
    @Column(name = "current_stage")
    private String currentStage = "FACE_VERIFICATION";
    
    @Column(name = "speech_session_id")
    private String speechSessionId;

    // 新增：关联 job_applications.id，用于直接获取投递简历
    @Column(name = "job_application_id")
    private Long jobApplicationId;
    
    @Column(name = "position_type")
    private String positionType;
    
    @Column(name = "create_time", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @Column(name = "update_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
} 