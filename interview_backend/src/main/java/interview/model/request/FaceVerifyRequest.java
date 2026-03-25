package interview.model.request;

/**
 * 人脸验证请求对象
 */
public class FaceVerifyRequest {
    private String faceImage; // Base64编码的人脸照片
    private String idCardImage; // Base64编码的身份证照片
    private String name; // 用户姓名
    private String idNumber; // 身份证号码
    
    // Getter 和 Setter 方法
    public String getFaceImage() {
        return faceImage;
    }
    
    public void setFaceImage(String faceImage) {
        this.faceImage = faceImage;
    }
    
    public String getIdCardImage() {
        return idCardImage;
    }
    
    public void setIdCardImage(String idCardImage) {
        this.idCardImage = idCardImage;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getIdNumber() {
        return idNumber;
    }
    
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }
} 