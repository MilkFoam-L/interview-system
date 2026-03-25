package interview.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import interview.model.FaceExpressionRecord;

@Repository
public interface FaceExpressionRecordRepository extends JpaRepository<FaceExpressionRecord, Long> {
    
    /**
     * 根据会话ID查找表情记录
     */
    List<FaceExpressionRecord> findBySessionId(Long sessionId);
    
    /**
     * 根据用户ID查找表情记录
     */
    List<FaceExpressionRecord> findByUserId(Long userId);
    
    /**
     * 根据会话ID和用户ID查找表情记录
     */
    List<FaceExpressionRecord> findBySessionIdAndUserId(Long sessionId, Long userId);
    
    /**
     * 根据会话ID获取样本图片路径
     */
    @Query("SELECT r.imagePath FROM FaceExpressionRecord r WHERE r.sessionId = ?1 AND r.imagePath IS NOT NULL")
    List<String> findImagePathsBySessionId(Long sessionId);
    
    /**
     * 根据会话ID和表情代码查找记录
     */
    List<FaceExpressionRecord> findBySessionIdAndExpressionCode(Long sessionId, int expressionCode);
} 