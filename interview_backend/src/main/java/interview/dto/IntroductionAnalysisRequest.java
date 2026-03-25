package interview.dto;

/**
 * 自我介绍分析请求DTO
 */
public class IntroductionAnalysisRequest {
    
    private Long sessionId;
    private String introductionText;
    private Integer duration;
    
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
} 