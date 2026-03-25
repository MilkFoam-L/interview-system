package interview.repository;

import interview.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    
    /**
     * 根据用户ID查找简历投递记录
     * 
     * @param userId 用户ID
     * @return 简历投递记录列表
     */
    List<JobApplication> findByUserId(Long userId);
    
    /**
     * 根据用户ID和岗位ID查找简历投递记录
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @return 简历投递记录列表
     */
    List<JobApplication> findByUserIdAndJobId(Long userId, Integer jobId);
    
    /**
     * 根据岗位ID查找简历投递记录
     * 
     * @param jobId 岗位ID
     * @return 简历投递记录列表
     */
    List<JobApplication> findByJobId(Integer jobId);
    
    /**
     * 根据状态查找简历投递记录
     * 
     * @param status 状态
     * @return 简历投递记录列表
     */
    List<JobApplication> findByStatus(Integer status);

    /**
     * 根据简历ID查找投递记录，用于判断简历是否被占用
     */
    List<JobApplication> findByResumeId(Long resumeId);
} 