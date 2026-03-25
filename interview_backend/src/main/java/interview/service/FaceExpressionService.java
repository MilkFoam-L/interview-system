package interview.service;

import java.util.List;

import interview.dto.FaceExpressionRequest;
import interview.dto.FaceExpressionResponse;
import interview.dto.FaceExpressionSummaryResponse;
import interview.model.FaceExpressionRecord;
import interview.model.FaceExpressionSummary;

/**
 * 面部表情分析服务接口
 */
public interface FaceExpressionService {
    
    /**
     * 分析面部表情
     * 
     * @param request 表情分析请求
     * @return 表情分析响应
     */
    FaceExpressionResponse analyzeExpression(FaceExpressionRequest request);
    
    /**
     * 获取表情分析摘要
     * 
     * @param sessionId 会话ID
     * @return 表情分析摘要响应
     */
    FaceExpressionSummaryResponse getExpressionSummary(Long sessionId);
    
    /**
     * 保存表情记录
     * 
     * @param record 表情记录
     * @return 保存后的表情记录
     */
    FaceExpressionRecord saveExpressionRecord(FaceExpressionRecord record);
    
    /**
     * 更新表情摘要
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param expressionCode 表情代码
     * @return 更新后的表情摘要
     */
    FaceExpressionSummary updateExpressionSummary(Long userId, Long sessionId, int expressionCode);
    
    /**
     * 获取会话的样本图片路径
     * 
     * @param sessionId 会话ID
     * @return 样本图片路径列表
     */
    List<String> getSessionSampleImagePaths(Long sessionId);
} 