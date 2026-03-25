package interview.dto;

import java.math.BigDecimal;

/**
 * 自我介绍分析响应DTO
 */
public class IntroductionAnalysisResponse {
    
    private Long id;
    private Long sessionId;
    private String introductionText;
    private Integer duration;
    private BigDecimal fluencyScore;
    private BigDecimal expressionScore;
    private BigDecimal contentScore;
    private BigDecimal overallScore;
    private String analysisResult;
    private boolean success;
    private String message;
    
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
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * 创建成功响应
     */
    public static IntroductionAnalysisResponse success(Long id, Long sessionId) {
        IntroductionAnalysisResponse response = new IntroductionAnalysisResponse();
        response.setSuccess(true);
        response.setId(id);
        response.setSessionId(sessionId);
        response.setMessage("自我介绍分析保存成功");
        return response;
    }
    
    /**
     * 创建失败响应
     */
    public static IntroductionAnalysisResponse failure(String message) {
        IntroductionAnalysisResponse response = new IntroductionAnalysisResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
} 