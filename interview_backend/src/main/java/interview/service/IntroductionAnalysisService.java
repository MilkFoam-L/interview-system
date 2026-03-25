package interview.service;

import interview.dto.IntroductionAnalysisRequest;
import interview.dto.IntroductionAnalysisResponse;
import interview.model.IntroductionAnalysis;

/**
 * 自我介绍分析服务接口
 */
public interface IntroductionAnalysisService {
    
    /**
     * 分析自我介绍并保存结果
     * 
     * @param request 分析请求
     * @return 分析响应
     */
    IntroductionAnalysisResponse analyzeAndSave(IntroductionAnalysisRequest request);
    
    /**
     * 根据会话ID获取分析结果
     * 
     * @param sessionId 会话ID
     * @return 分析结果
     */
    IntroductionAnalysis getBySessionId(Long sessionId);
    
    /**
     * 检查分析服务是否可用
     * 
     * @return 是否可用
     */
    boolean isServiceAvailable();
    
    /**
     * 重新分析指定会话的自我介绍
     * 
     * @param sessionId 会话ID
     * @return 分析响应
     */
    IntroductionAnalysisResponse reanalyze(Long sessionId);
}