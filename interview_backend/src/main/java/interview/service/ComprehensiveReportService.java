package interview.service;

import interview.model.ComprehensiveReport;
import interview.model.FaceExpressionRecord;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ComprehensiveReportService {

    /**
     * 获取表情分析数据
     */
    List<FaceExpressionRecord> getExpressionRecords(Long sessionId);

    /**
     * 获取STAR结构分数
     */
    Map<String, Object> getStarData(Long sessionId);

    /**
     * 获取问题回答质量分析
     */
    Map<String, Object> getAnswerQuality(Long sessionId);

    /**
     * 获取能力评估矩阵数据（demo）
     */
    Map<String, Object> getAbilityMatrix(Long sessionId);
    
    /**
     * 分页获取用户报告列表
     */
    Page<ComprehensiveReport> getReportList(Long userId, int page, int size, String sortBy, 
                                          String searchKeyword, LocalDateTime startTime, 
                                          LocalDateTime endTime, String filterType);
    
    /**
     * 获取用户统计数据
     */
    Map<String, Object> getUserStats(Long userId);

    /**
     * 创建或更新面试开始时间
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param companyName 公司名称
     * @param positionName 岗位名称
     * @return 创建或更新的综合报告
     */
    ComprehensiveReport createOrUpdateStartTime(Long sessionId, Long userId, Integer jobId, 
                                               String companyName, String positionName);

    /**
     * 更新面试结束时间并计算时长
     * @param sessionId 面试会话ID
     * @return 更新后的综合报告
     */
    ComprehensiveReport updateEndTimeAndDuration(Long sessionId);

    /**
     * 根据会话ID获取综合报告
     * @param sessionId 面试会话ID
     * @return 综合报告（如果存在）
     */
    Optional<ComprehensiveReport> getBySessionId(Long sessionId);

    /**
     * 生成并更新STAR分数到综合报告
     * @param sessionId 面试会话ID
     * @return 更新后的综合报告
     */
    ComprehensiveReport generateAndUpdateStarScores(Long sessionId);

    /**
     * 分析基础问答环节并更新到综合报告
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     * @return 更新后的综合报告
     */
    ComprehensiveReport analyzeAndUpdateBasicQA(Long sessionId, Long userId);

    /**
     * 获取技术能力分析结果（基础问答 + 代码题）
     * @param sessionId 面试会话ID
     * @return 技术分析结果
     */
    Map<String, Object> getTechAnalysis(Long sessionId);

    /**
     * 生成并更新能力评估矩阵到综合报告
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     * @return 更新后的综合报告
     */
    ComprehensiveReport generateAndUpdateAbilityMatrix(Long sessionId, Long userId);

    /**
     * 生成并更新能力评估矩阵到综合报告
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     * @param forceRegenerate 是否强制重新生成，绕过缓存
     * @return 更新后的综合报告
     */
    ComprehensiveReport generateAndUpdateAbilityMatrix(Long sessionId, Long userId, boolean forceRegenerate);

    /**
     * 检查能力评估矩阵是否已生成
     * @param sessionId 面试会话ID
     * @return 是否已生成
     */
    boolean isAbilityMatrixGenerated(Long sessionId);

    /**
     * 生成并更新综合分析简报到综合报告
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     * @return 更新后的综合报告
     */
    ComprehensiveReport generateAndUpdateComprehensiveAnalysis(Long sessionId, Long userId);

    /**
     * 生成并更新综合分析简报到综合报告
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     * @param forceRegenerate 是否强制重新生成，绕过缓存
     * @return 更新后的综合报告
     */
    ComprehensiveReport generateAndUpdateComprehensiveAnalysis(Long sessionId, Long userId, boolean forceRegenerate);

    /**
     * 检查综合分析简报是否已生成
     * @param sessionId 面试会话ID
     * @return 是否已生成
     */
    boolean isComprehensiveAnalysisGenerated(Long sessionId);

    /**
     * 获取综合分析简报数据
     * @param sessionId 面试会话ID
     * @return 综合分析简报数据
     */
    Map<String, Object> getComprehensiveAnalysis(Long sessionId);

    /**
     * 生成并更新个性化建议到综合报告
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     * @return 更新后的综合报告
     */
    ComprehensiveReport generateAndUpdatePersonalizedSuggestions(Long sessionId, Long userId);

    /**
     * 生成并更新个性化建议到综合报告
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     * @param forceRegenerate 是否强制重新生成，绕过缓存
     * @return 更新后的综合报告
     */
    ComprehensiveReport generateAndUpdatePersonalizedSuggestions(Long sessionId, Long userId, boolean forceRegenerate);

    /**
     * 检查个性化建议是否已生成
     * @param sessionId 面试会话ID
     * @return 是否已生成
     */
    boolean isPersonalizedSuggestionsGenerated(Long sessionId);

    /**
     * 获取个性化建议数据
     * @param sessionId 面试会话ID
     * @return 个性化建议数据
     */
    Map<String, Object> getPersonalizedSuggestions(Long sessionId);
} 