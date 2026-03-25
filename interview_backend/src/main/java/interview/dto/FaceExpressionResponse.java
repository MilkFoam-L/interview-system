package interview.dto;

/**
 * 面部表情分析响应DTO
 */
public class FaceExpressionResponse {
    
    private Long id;
    private boolean success;
    private String message;
    private int expressionCode;
    private String expressionName;
    private String imagePath;
    private String capture_id;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
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
    
    /**
     * 创建成功响应
     */
    public static FaceExpressionResponse success(String expressionName, int expressionCode, String imagePath) {
        FaceExpressionResponse response = new FaceExpressionResponse();
        response.setSuccess(true);
        response.setExpressionName(expressionName);
        response.setExpressionCode(expressionCode);
        response.setImagePath(imagePath);
        response.setMessage("表情分析成功");
        return response;
    }
    
    /**
     * 创建失败响应
     */
    public static FaceExpressionResponse failure(String message) {
        FaceExpressionResponse response = new FaceExpressionResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }

    public static FaceExpressionResponse error(String message) {
        FaceExpressionResponse response = new FaceExpressionResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
} 