package interview.model;

import java.util.Date;

import jakarta.persistence.*;

@Entity
@Table(name = "face_expression_record")
public class FaceExpressionRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "candidate_name")
    private String candidateName;
    
    @Column(name = "candidate_id_number")
    private String candidateIdNumber;
    
    @Column(name = "expression_code", nullable = false)
    private int expressionCode;
    
    @Column(name = "expression_name", nullable = false)
    private String expressionName;
    
    @Column(name = "count", nullable = false)
    private int count;
    
    @Column(name = "remaining_time")
    private Integer remainingTime;
    
    @Column(name = "timestamp")
    private Long timestamp;
    
    @Column(name = "image_path")
    private String imagePath;
    
    @Column(name = "session_id", nullable = false)
    private Long sessionId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
    
    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
    
    @Column(name = "capture_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date captureTime;
    
    @Column(name = "capture_id")
    private String capture_id;
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCandidateName() {
        return candidateName;
    }
    
    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }
    
    public String getCandidateIdNumber() {
        return candidateIdNumber;
    }
    
    public void setCandidateIdNumber(String candidateIdNumber) {
        this.candidateIdNumber = candidateIdNumber;
    }
    
    public int getExpressionCode() {
        return expressionCode;
    }
    
    public void setExpressionCode(int expressionCode) {
        this.expressionCode = expressionCode;
    }
    
    public String getExpressionName() {
        return expressionName;
    }
    
    public void setExpressionName(String expressionName) {
        this.expressionName = expressionName;
    }
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public Integer getRemainingTime() {
        return remainingTime;
    }
    
    public void setRemainingTime(Integer remainingTime) {
        this.remainingTime = remainingTime;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public Long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Date getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Date getCaptureTime() {
        return captureTime;
    }
    
    public void setCaptureTime(Date captureTime) {
        this.captureTime = captureTime;
    }
    
    public String getCapture_id() {
        return capture_id;
    }
    
    public void setCapture_id(String capture_id) {
        this.capture_id = capture_id;
    }
    
    // 为了兼容旧代码，保留getCaptureId/setCaptureId方法
    public String getCaptureId() {
        return capture_id;
    }
    
    public void setCaptureId(String captureId) {
        this.capture_id = captureId;
    }

    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
        if (timestamp == null) {
            timestamp = System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
} 