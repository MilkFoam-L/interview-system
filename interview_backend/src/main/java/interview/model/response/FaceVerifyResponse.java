package interview.model.response;

/**
 * 人脸验证响应对象
 */
public class FaceVerifyResponse {
    private boolean success; // 是否成功
    private String message; // 消息
    private double similarity; // 相似度得分
    private boolean samePerson; // 是否为同一个人
    
    // Getter 和 Setter 方法
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
    
    public double getSimilarity() {
        return similarity;
    }
    
    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
    
    public boolean isSamePerson() {
        return samePerson;
    }
    
    public void setSamePerson(boolean samePerson) {
        this.samePerson = samePerson;
    }
} 