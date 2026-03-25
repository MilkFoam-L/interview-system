package interview.repository;

import interview.model.ApplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApplicationRecordRepository extends JpaRepository<ApplicationRecord, Long> {
    
    List<ApplicationRecord> findByUserId(Long userId);
    
    /**
     * 根据用户ID和岗位ID查找申请记录
     */
    List<ApplicationRecord> findByUserIdAndJobId(Long userId, Integer jobId);
    
    @Query("SELECT COUNT(a) FROM ApplicationRecord a WHERE a.userId = :userId AND (a.status = 'SUBMITTED' OR a.status = 'APPLIED' OR a.status = 'PENDING1')")
    Integer countSubmittedByUserId(@Param("userId") Long userId);
    
    /**
     * 计算用户的待面试记录数量（状态为PENDING或ONGOING）
     */
    @Query("SELECT COUNT(a) FROM ApplicationRecord a WHERE a.userId = :userId AND (a.status = 'PENDING' OR a.status = 'ONGOING')")
    Integer countPendingByUserId(@Param("userId") Long userId);
    
    // 删除已面试计数方法
    
    @Query("SELECT COUNT(a) FROM ApplicationRecord a WHERE a.userId = :userId AND a.status = 'COMPLETED'")
    Integer countCompletedByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(a) FROM ApplicationRecord a WHERE a.userId = :userId AND a.status = 'COMPLETED' AND a.result = 'PASS'")
    Integer countPassedByUserId(@Param("userId") Long userId);
    
    /**
     * 按状态分组统计用户的申请记录
     * 修复查询语法，确保返回正确的结果格式
     */
    @Query("SELECT a.status as status, COUNT(a) as count FROM ApplicationRecord a WHERE a.userId = :userId GROUP BY a.status")
    List<Object[]> countByStatusForUser(@Param("userId") Long userId);
    
    /**
     * 计数未投递状态的方法(系统中没有此状态，始终返回0)
     */
    @Query("SELECT 0")
    Integer countNotSubmittedByUserId(@Param("userId") Long userId);

    /**
     * 公司内在时间窗的总投递数量
     */
    @Query("SELECT COUNT(a) FROM ApplicationRecord a JOIN a.job j WHERE j.company.id = :companyId AND a.submitTime BETWEEN :start AND :end")
    long countByCompanyAndSubmitTimeBetween(@Param("companyId") Long companyId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    /**
     * 时间序列：按月统计公司投递量
     */
    @Query(value = "SELECT DATE_FORMAT(a.submit_time, '%Y-%m') AS ym, COUNT(*) AS cnt " +
            "FROM application_records a JOIN job j ON j.id = a.job_id " +
            "WHERE j.company_id = :companyId AND a.submit_time BETWEEN :start AND :end " +
            "GROUP BY ym ORDER BY ym", nativeQuery = true)
    List<Object[]> timeSeriesByMonth(@Param("companyId") Long companyId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    /**
     * 统计 period 内各 categoryType 的投递数量，取 TopN
     */
    @Query(value = "SELECT j.categoryType AS categoryType, COUNT(*) AS c " +
            "FROM application_records a JOIN job j ON j.id = a.job_id " +
            "WHERE j.company_id = :companyId AND a.submit_time BETWEEN :start AND :end " +
            "GROUP BY j.categoryType ORDER BY c DESC", nativeQuery = true)
    List<Object[]> topCategoryTypes(@Param("companyId") Long companyId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    Pageable pageable);

    /**
     * period 内按月、指定类别集合的投递量
     */
    @Query(value = "SELECT DATE_FORMAT(a.submit_time, '%Y-%m') AS ym, j.categoryType, COUNT(*) AS cnt " +
            "FROM application_records a JOIN job j ON j.id = a.job_id " +
            "WHERE j.company_id = :companyId AND a.submit_time BETWEEN :start AND :end " +
            "AND j.categoryType IN (:categoryTypes) " +
            "GROUP BY ym, j.categoryType ORDER BY ym, j.categoryType", nativeQuery = true)
    List<Object[]> timeSeriesByMonthForCategories(@Param("companyId") Long companyId,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("categoryTypes") List<String> categoryTypes);

    /**
     * 热门岗位 TopN
     */
    @Query(value = "SELECT j.id, j.title, COUNT(*) AS applicationCount " +
            "FROM application_records a JOIN job j ON j.id = a.job_id " +
            "WHERE j.company_id = :companyId AND a.submit_time BETWEEN :start AND :end " +
            "GROUP BY j.id, j.title ORDER BY applicationCount DESC", nativeQuery = true)
    List<Object[]> topJobs(@Param("companyId") Long companyId,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end,
                           Pageable pageable);

    /**
     * 在时间窗内已发送通知数量
     */
    @Query(value = "SELECT COUNT(*) FROM application_records a JOIN job j ON j.id = a.job_id " +
            "WHERE j.company_id = :companyId AND a.result IS NOT NULL AND a.complete_time BETWEEN :start AND :end",
            nativeQuery = true)
    long countNotifiedBetween(@Param("companyId") Long companyId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);
} 