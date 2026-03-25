package interview.dto;

/**
 * 面部表情分析请求DTO
 */
public class FaceExpressionRequest {
    
    private Long sessionId;
    private Long userId;
    private String candidateName;
    private String candidateIdNumber;
    private String imageBase64;
    private Integer remainingTime; // 剩余时间（秒）
    private String capture_id; // 捕获唯一ID（统一使用此名称）
    private Long timestamp; // 时间戳
    private String capture_time; // 捕获时间（ISO格式）
    private Integer count; // 计数
    
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
    
    public String getImageBase64() {
        return imageBase64;
    }
    
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
    
    public Integer getRemainingTime() {
        return remainingTime;
    }
    
    public void setRemainingTime(Integer remainingTime) {
        this.remainingTime = remainingTime;
    }
    
    public String getCapture_id() {
        return capture_id;
    }
    
    public void setCapture_id(String capture_id) {
        this.capture_id = capture_id;
    }
    
    // 为了兼容旧代码，保留getCaptureId/setCaptureId方法，内部使用capture_id
    public String getCaptureId() {
        return capture_id;
    }
    
    public void setCaptureId(String captureId) {
        this.capture_id = captureId;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getCapture_time() {
        return capture_time;
    }
    
    public void setCapture_time(String capture_time) {
        this.capture_time = capture_time;
    }
    
    public Integer getCount() {
        return count;
    }
    
    public void setCount(Integer count) {
        this.count = count;
    }
} 