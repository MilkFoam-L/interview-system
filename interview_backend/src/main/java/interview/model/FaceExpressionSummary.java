package interview.model;

import java.util.Date;

import jakarta.persistence.*;

@Entity
@Table(name = "face_expression_summary")
public class FaceExpressionSummary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "candidate_name")
    private String candidateName;
    
    @Column(name = "candidate_id_number")
    private String candidateIdNumber;
    
    @Column(name = "total_samples", nullable = false)
    private int totalSamples;
    
    @Column(name = "dominant_expression")
    private String dominantExpression;
    
    @Column(name = "surprised_count")
    private Integer surprisedCount;
    
    @Column(name = "fear_count")
    private Integer fearCount;
    
    @Column(name = "disgust_count")
    private Integer disgustCount;
    
    @Column(name = "happy_count")
    private Integer happyCount;
    
    @Column(name = "sad_count")
    private Integer sadCount;
    
    @Column(name = "angry_count")
    private Integer angryCount;
    
    @Column(name = "neutral_count")
    private Integer neutralCount;
    
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
    
    public int getTotalSamples() {
        return totalSamples;
    }
    
    public void setTotalSamples(int totalSamples) {
        this.totalSamples = totalSamples;
    }
    
    public String getDominantExpression() {
        return dominantExpression;
    }
    
    public void setDominantExpression(String dominantExpression) {
        this.dominantExpression = dominantExpression;
    }
    
    public Integer getSurprisedCount() {
        return surprisedCount != null ? surprisedCount : 0;
    }
    
    public void setSurprisedCount(Integer surprisedCount) {
        this.surprisedCount = surprisedCount;
    }
    
    public Integer getFearCount() {
        return fearCount != null ? fearCount : 0;
    }
    
    public void setFearCount(Integer fearCount) {
        this.fearCount = fearCount;
    }
    
    public Integer getDisgustCount() {
        return disgustCount != null ? disgustCount : 0;
    }
    
    public void setDisgustCount(Integer disgustCount) {
        this.disgustCount = disgustCount;
    }
    
    public Integer getHappyCount() {
        return happyCount != null ? happyCount : 0;
    }
    
    public void setHappyCount(Integer happyCount) {
        this.happyCount = happyCount;
    }
    
    public Integer getSadCount() {
        return sadCount != null ? sadCount : 0;
    }
    
    public void setSadCount(Integer sadCount) {
        this.sadCount = sadCount;
    }
    
    public Integer getAngryCount() {
        return angryCount != null ? angryCount : 0;
    }
    
    public void setAngryCount(Integer angryCount) {
        this.angryCount = angryCount;
    }
    
    public Integer getNeutralCount() {
        return neutralCount != null ? neutralCount : 0;
    }
    
    public void setNeutralCount(Integer neutralCount) {
        this.neutralCount = neutralCount;
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
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
    
    // 根据表情代码增加相应表情计数
    public void incrementExpressionCount(int expressionCode) {
        switch (expressionCode) {
            case 0: // 惊讶
                if (surprisedCount == null) surprisedCount = 1;
                else surprisedCount++;
                break;
            case 1: // 害怕
                if (fearCount == null) fearCount = 1;
                else fearCount++;
                break;
            case 2: // 厌恶
                if (disgustCount == null) disgustCount = 1;
                else disgustCount++;
                break;
            case 3: // 高兴
                if (happyCount == null) happyCount = 1;
                else happyCount++;
                break;
            case 4: // 悲伤
                if (sadCount == null) sadCount = 1;
                else sadCount++;
                break;
            case 5: // 生气
                if (angryCount == null) angryCount = 1;
                else angryCount++;
                break;
            case 6: // 正常
                if (neutralCount == null) neutralCount = 1;
                else neutralCount++;
                break;
        }
        totalSamples++;
        updateDominantExpression();
    }
    
    // 更新主要表情
    private void updateDominantExpression() {
        int max = 0;
        String dominant = "未知";
        
        if (surprisedCount != null && surprisedCount > max) {
            max = surprisedCount;
            dominant = "惊讶";
        }
        if (fearCount != null && fearCount > max) {
            max = fearCount;
            dominant = "害怕";
        }
        if (disgustCount != null && disgustCount > max) {
            max = disgustCount;
            dominant = "厌恶";
        }
        if (happyCount != null && happyCount > max) {
            max = happyCount;
            dominant = "高兴";
        }
        if (sadCount != null && sadCount > max) {
            max = sadCount;
            dominant = "悲伤";
        }
        if (angryCount != null && angryCount > max) {
            max = angryCount;
            dominant = "生气";
        }
        if (neutralCount != null && neutralCount > max) {
            max = neutralCount;
            dominant = "正常";
        }
        
        this.dominantExpression = dominant;
    }
} 