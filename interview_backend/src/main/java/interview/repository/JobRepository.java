package interview.repository;

import interview.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer>, JpaSpecificationExecutor<Job> {

    @Override
    @EntityGraph(attributePaths = {"company"})
    List<Job> findAll(Specification<Job> spec);

    @Override
    @EntityGraph(attributePaths = {"company"})
    Page<Job> findAll(Specification<Job> spec, Pageable pageable);
    
    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.company WHERE j.id = :id")
    Optional<Job> findByIdWithCompany(@Param("id") Integer id);

    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.company")
    List<Job> findAllWithCompany();
    
    /**
     * 根据岗位类别查询
     * 
     * @param category 岗位类别
     * @return 岗位列表
     */
    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.company WHERE j.category = :category")
    List<Job> findByCategoryWithCompany(@Param("category") String category);
    
    /**
     * 根据岗位类型查询
     * 
     * @param categoryType 岗位类型
     * @return 岗位列表
     */
    @Query("SELECT j FROM Job j LEFT JOIN FETCH j.company WHERE j.categoryType = :categoryType")
    List<Job> findByCategoryTypeWithCompany(@Param("categoryType") String categoryType);
    
    /**
     * 根据岗位名称关键字模糊查询
     * 
     * @param title 岗位名称关键字
     * @return 岗位列表
     */
    List<Job> findByTitleContaining(String title);
    
    /**
     * 根据工作地点查询
     * 
     * @param location 工作地点
     * @return 岗位列表
     */
    List<Job> findByLocation(String location);

    /**
     * 截止某时间点的在招岗位数量
     */
    @Query(value = "SELECT COUNT(*) FROM job j WHERE j.company_id = :companyId AND j.status = 0 AND j.created_at <= :end", nativeQuery = true)
    long countOpenJobsAsOf(@Param("companyId") Long companyId, @Param("end") LocalDateTime end);

    /**
     * 时间窗内新发布的在招岗位数量
     */
    @Query(value = "SELECT COUNT(*) FROM job j WHERE j.company_id = :companyId AND j.status = 0 AND j.created_at BETWEEN :start AND :end", nativeQuery = true)
    long countNewOpenJobsBetween(@Param("companyId") Long companyId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /**
     * 指定标题的在招岗位数量
     */
    @Query(value = "SELECT COUNT(*) FROM job j WHERE j.company_id = :companyId AND j.title = :title AND j.status = 0", nativeQuery = true)
    long countOpenJobsByTitle(@Param("companyId") Long companyId, @Param("title") String title);
}