package interview.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 自我介绍分析实体
 */
@Entity
@Table(name = "introduction_analysis")
public class IntroductionAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "session_id", nullable = false, unique = true)
    private Long sessionId;
    
    @Column(name = "introduction_text", columnDefinition = "TEXT")
    private String introductionText;
    
    @Column(name = "duration")
    private Integer duration;
    
    @Column(name = "fluency_score", precision = 3, scale = 1)
    private BigDecimal fluencyScore;
    
    @Column(name = "expression_score", precision = 3, scale = 1)
    private BigDecimal expressionScore;
    
    @Column(name = "content_score", precision = 3, scale = 1)
    private BigDecimal contentScore;
    
    @Column(name = "overall_score", precision = 3, scale = 1)
    private BigDecimal overallScore;
    
    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult;
    
    @Column(name = "create_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;
    
    @Column(name = "update_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = new Date();
        updateTime = new Date();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getIntroductionText() {
        return introductionText;
    }
    
    public void setIntroductionText(String introductionText) {
        this.introductionText = introductionText;
    }
    
    public Integer getDuration() {
        return duration;
    }
    
    public void setDuration(Integer duration) {
        this.duration = duration;
    }
    
    public BigDecimal getFluencyScore() {
        return fluencyScore;
    }
    
    public void setFluencyScore(BigDecimal fluencyScore) {
        this.fluencyScore = fluencyScore;
    }
    
    public BigDecimal getExpressionScore() {
        return expressionScore;
    }
    
    public void setExpressionScore(BigDecimal expressionScore) {
        this.expressionScore = expressionScore;
    }
    
    public BigDecimal getContentScore() {
        return contentScore;
    }
    
    public void setContentScore(BigDecimal contentScore) {
        this.contentScore = contentScore;
    }
    
    public BigDecimal getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(BigDecimal overallScore) {
        this.overallScore = overallScore;
    }
    
    public String getAnalysisResult() {
        return analysisResult;
    }
    
    public void setAnalysisResult(String analysisResult) {
        this.analysisResult = analysisResult;
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
}