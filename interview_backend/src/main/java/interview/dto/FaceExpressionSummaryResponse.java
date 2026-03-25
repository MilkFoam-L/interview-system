package interview.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 面部表情分析摘要响应DTO
 */
public class FaceExpressionSummaryResponse {
    
    private boolean success;
    private String dominantExpression;
    private int totalSamples;
    private Map<String, Integer> expressionDistribution;
    private List<String> sampleImagePaths;
    private String message;
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getDominantExpression() {
        return dominantExpression;
    }
    
    public void setDominantExpression(String dominantExpression) {
        this.dominantExpression = dominantExpression;
    }
    
    public int getTotalSamples() {
        return totalSamples;
    }
    
    public void setTotalSamples(int totalSamples) {
        this.totalSamples = totalSamples;
    }
    
    public Map<String, Integer> getExpressionDistribution() {
        return expressionDistribution;
    }
    
    public void setExpressionDistribution(Map<String, Integer> expressionDistribution) {
        this.expressionDistribution = expressionDistribution;
    }
    
    public List<String> getSampleImagePaths() {
        return sampleImagePaths;
    }
    
    public void setSampleImagePaths(List<String> sampleImagePaths) {
        this.sampleImagePaths = sampleImagePaths;
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
    public static FaceExpressionSummaryResponse success(String dominantExpression, int totalSamples, 
                                                        Map<String, Integer> expressionDistribution,
                                                        List<String> sampleImagePaths) {
        FaceExpressionSummaryResponse response = new FaceExpressionSummaryResponse();
        response.setSuccess(true);
        response.setDominantExpression(dominantExpression);
        response.setTotalSamples(totalSamples);
        response.setExpressionDistribution(expressionDistribution != null ? 
                                          expressionDistribution : new HashMap<>());
        response.setSampleImagePaths(sampleImagePaths);
        response.setMessage("表情分析摘要获取成功");
        return response;
    }
    
    /**
     * 创建失败响应
     */
    public static FaceExpressionSummaryResponse failure(String message) {
        FaceExpressionSummaryResponse response = new FaceExpressionSummaryResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setExpressionDistribution(new HashMap<>());
        return response;
    }
} 