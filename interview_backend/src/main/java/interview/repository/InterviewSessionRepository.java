package interview.repository;

import interview.model.entity.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    
    /**
     * 根据用户ID查询面试会话
     * @param userId 用户ID
     * @return 面试会话列表
     */
    List<InterviewSession> findByUserId(Long userId);
    
    /**
     * 根据用户ID和岗位ID查询面试会话
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 面试会话列表
     */
    List<InterviewSession> findByUserIdAndJobId(Long userId, Integer jobId);
    
    /**
     * 根据申请记录ID查询面试会话
     * @param applicationId 申请记录ID
     * @return 面试会话
     */
    InterviewSession findByApplicationId(Long applicationId);


    /**
     * 公司在时间窗内参加面试的去重人数
     */
    @Query(value = "SELECT COUNT(DISTINCT s.user_id) FROM interview_sessions s " +
            "JOIN job j ON j.id = s.job_id " +
            "WHERE j.company_id = :companyId AND s.start_time BETWEEN :start AND :end",
            nativeQuery = true)
    long countDistinctInterviewees(@Param("companyId") Long companyId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    /**
     * 公司在时间窗内的面试安排数量
     */
    @Query(value = "SELECT COUNT(*) FROM interview_sessions s " +
            "JOIN job j ON j.id = s.job_id " +
            "WHERE j.company_id = :companyId AND s.start_time IS NOT NULL AND s.start_time BETWEEN :start AND :end",
            nativeQuery = true)
    long countScheduled(@Param("companyId") Long companyId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

    /**
     * 公司在时间窗内的完成面试数量
     */
    @Query(value = "SELECT COUNT(*) FROM interview_sessions s " +
            "JOIN job j ON j.id = s.job_id " +
            "WHERE j.company_id = :companyId AND ( (s.end_time BETWEEN :start AND :end) OR s.session_status = 'COMPLETED')",
            nativeQuery = true)
    long countCompleted(@Param("companyId") Long companyId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

    /**
     * 统计某职位标题在时间窗内的面试数量
     */
    @Query(value = "SELECT COUNT(*) FROM interview_sessions s " +
            "JOIN job j ON j.id = s.job_id " +
            "WHERE j.company_id = :companyId AND j.title = :title AND s.start_time BETWEEN :start AND :end",
            nativeQuery = true)
    long countInterviewsForTitle(@Param("companyId") Long companyId,
                                 @Param("title") String title,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);
} 