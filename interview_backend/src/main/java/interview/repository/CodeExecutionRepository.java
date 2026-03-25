package interview.repository;

import interview.model.entity.CodeExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 代码执行记录数据访问接口
 */
@Repository
public interface CodeExecutionRepository extends JpaRepository<CodeExecution, Long> {
    
    /**
     * 根据答题记录ID查找执行记录
     */
    List<CodeExecution> findByAnswerId(Long answerId);
    
    /**
     * 根据会话ID查找执行记录
     */
    List<CodeExecution> findBySessionId(Long sessionId);
    
    /**
     * 根据答题记录ID和会话ID查找执行记录
     */
    Optional<CodeExecution> findByAnswerIdAndSessionId(Long answerId, Long sessionId);
    
    /**
     * 根据状态查找执行记录
     */
    List<CodeExecution> findByStatus(String status);
    
    /**
     * 根据编程语言查找执行记录
     */
    List<CodeExecution> findByLanguage(String language);
    
    /**
     * 查找指定会话中指定状态的执行记录数量
     */
    @Query("SELECT COUNT(ce) FROM CodeExecution ce WHERE ce.sessionId = :sessionId AND ce.status = :status")
    long countBySessionIdAndStatus(@Param("sessionId") Long sessionId, @Param("status") String status);
    
    /**
     * 查找指定答题记录的最新执行记录
     */
    @Query("SELECT ce FROM CodeExecution ce WHERE ce.answerId = :answerId ORDER BY ce.createTime DESC")
    List<CodeExecution> findByAnswerIdOrderByCreateTimeDesc(@Param("answerId") Long answerId);
    
    /**
     * 查找指定会话中按时间排序的执行记录
     */
    @Query("SELECT ce FROM CodeExecution ce WHERE ce.sessionId = :sessionId ORDER BY ce.createTime DESC")
    List<CodeExecution> findBySessionIdOrderByCreateTimeDesc(@Param("sessionId") Long sessionId);
    
    /**
     * 根据执行时间范围查找记录（用于性能分析）
     */
    @Query("SELECT ce FROM CodeExecution ce WHERE ce.executionTime BETWEEN :minTime AND :maxTime")
    List<CodeExecution> findByExecutionTimeBetween(@Param("minTime") Integer minTime, @Param("maxTime") Integer maxTime);
}
