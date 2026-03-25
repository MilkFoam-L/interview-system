package interview.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import interview.model.FaceExpressionSummary;

@Repository
public interface FaceExpressionSummaryRepository extends JpaRepository<FaceExpressionSummary, Long> {
    
    /**
     * 根据会话ID查找表情摘要
     */
    Optional<FaceExpressionSummary> findBySessionId(Long sessionId);
    
    /**
     * 根据用户ID查找表情摘要
     */
    Optional<FaceExpressionSummary> findByUserId(Long userId);
    
    /**
     * 根据会话ID和用户ID查找表情摘要
     */
    Optional<FaceExpressionSummary> findBySessionIdAndUserId(Long sessionId, Long userId);
} 