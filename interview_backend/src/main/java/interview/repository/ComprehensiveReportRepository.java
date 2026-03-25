package interview.repository;

import interview.model.ComprehensiveReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComprehensiveReportRepository extends JpaRepository<ComprehensiveReport, Long> {
    
    /**
     * 根据面试会话ID查找综合分析报告
     */
    Optional<ComprehensiveReport> findBySessionId(Long sessionId);
    
    /**
     * 检查是否存在指定会话的报告
     */
    boolean existsBySessionId(Long sessionId);
    
    /**
     * 根据用户ID分页查询报告列表，支持公司名和职位名搜索
     */
    @Query("SELECT cr FROM ComprehensiveReport cr WHERE cr.userId = :userId " +
           "AND (:companyKeyword IS NULL OR LOWER(cr.companyName) LIKE LOWER(CONCAT('%', :companyKeyword, '%'))) " +
           "AND (:positionKeyword IS NULL OR LOWER(cr.positionName) LIKE LOWER(CONCAT('%', :positionKeyword, '%'))) " +
           "AND (:startTime IS NULL OR cr.startTime >= :startTime) " +
           "AND (:endTime IS NULL OR cr.startTime <= :endTime) " +
           "AND (:minScore IS NULL OR cr.totalScore >= :minScore)")
    Page<ComprehensiveReport> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("companyKeyword") String companyKeyword,
            @Param("positionKeyword") String positionKeyword,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("minScore") BigDecimal minScore,
            Pageable pageable);
    
    /**
     * 根据用户ID查询所有报告（用于统计）
     */
    List<ComprehensiveReport> findByUserIdOrderByStartTimeDesc(Long userId);
    
    /**
     * 根据用户ID查询最新的报告
     */
    Optional<ComprehensiveReport> findTopByUserIdOrderByStartTimeDesc(Long userId);
    
    /**
     * 统计用户高分报告数量
     */
    @Query("SELECT COUNT(cr) FROM ComprehensiveReport cr WHERE cr.userId = :userId AND cr.totalScore >= :minScore")
    Long countHighScoreReports(@Param("userId") Long userId, @Param("minScore") BigDecimal minScore);
} 