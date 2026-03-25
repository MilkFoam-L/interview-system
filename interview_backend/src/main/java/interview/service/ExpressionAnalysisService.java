package interview.service;

import java.util.Map;

/**
 * 表情分析服务接口
 */
public interface ExpressionAnalysisService {
    
    /**
     * 获取用户的表情分析结果
     * 
     * @param userId 用户ID
     * @return 分析结果数据
     */
    Map<String, Object> getExpressionAnalysisResult(Long userId);
} 