package interview.model;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "application_records")
public class ApplicationRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "job_id", nullable = false)
    private Integer jobId;
    
    @Column(name = "status", nullable = false)
    private String status;  // SUBMITTED, PENDING, COMPLETED
    
    @Column(name = "result")
    private String result;  // PASS, FAIL, null
    
    @Column(name = "submit_time", nullable = false)
    private LocalDateTime submitTime;
    
    @Column(name = "complete_time")
    private LocalDateTime completeTime;
    
    @Column(name = "feedback")
    private String feedback;
    
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
    
    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
    
    // 修改为FetchType.LAZY，并添加fetch = JsonIgnore，避免序列化问题
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User user;
    
    // 修改为FetchType.LAZY，并添加fetch = JsonIgnore，避免序列化问题
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", referencedColumnName = "id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Job job;
} 