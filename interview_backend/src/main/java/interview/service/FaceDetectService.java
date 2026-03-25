package interview.service;

/**
 * 面部检测服务接口，用于调用讯飞API
 */
public interface FaceDetectService {
    
    /**
     * 分析面部表情
     * 
     * @param imageData 图片数据（Base64编码）
     * @return 表情代码（0:惊讶；1:害怕；2:厌恶；3:高兴；4:悲伤；5:生气；6:正常）
     */
    int detectExpression(String imageData);
    
    /**
     * 获取表情代码对应的中文名称
     * 
     * @param expressionCode 表情代码
     * @return 表情名称
     */
    String getExpressionName(int expressionCode);
} 