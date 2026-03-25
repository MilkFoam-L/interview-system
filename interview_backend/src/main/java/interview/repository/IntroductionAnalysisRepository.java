package interview.repository;

import interview.model.IntroductionAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 自我介绍分析数据访问层
 */
@Repository
public interface IntroductionAnalysisRepository extends JpaRepository<IntroductionAnalysis, Long> {
    
    /**
     * 根据会话ID查找分析记录
     */
    Optional<IntroductionAnalysis> findBySessionId(Long sessionId);
    
    /**
     * 检查会话ID是否已存在分析记录
     */
    boolean existsBySessionId(Long sessionId);
    
    /**
     * 根据会话ID删除分析记录
     */
    void deleteBySessionId(Long sessionId);
    
    /**
     * 查询指定得分范围的分析记录
     */
    @Query("SELECT ia FROM IntroductionAnalysis ia WHERE ia.overallScore BETWEEN :minScore AND :maxScore")
    List<IntroductionAnalysis> findByOverallScoreBetween(@Param("minScore") Double minScore, 
                                                        @Param("maxScore") Double maxScore);
    
    /**
     * 查询平均得分统计
     */
    @Query("SELECT AVG(ia.fluencyScore) as avgFluency, " +
           "AVG(ia.expressionScore) as avgExpression, " +
           "AVG(ia.contentScore) as avgContent, " +
           "AVG(ia.overallScore) as avgOverall " +
           "FROM IntroductionAnalysis ia")
    Object[] getAverageScores();
    
    /**
     * 查询最近的分析记录
     */
    @Query("SELECT ia FROM IntroductionAnalysis ia ORDER BY ia.createTime DESC")
    List<IntroductionAnalysis> findRecentAnalyses(@Param("limit") int limit);
}