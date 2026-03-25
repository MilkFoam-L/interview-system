package interview.service;

import interview.model.JobApplication;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历投递服务接口
 */
public interface JobApplicationService {
    
    /**
     * 投递简历
     * 
     * @param userId 用户ID
     * @param jobId 岗位ID
     * @param resumeId 简历ID
     * @return 投递记录
     */
    JobApplication applyJob(Long userId, Integer jobId, Long resumeId);
    
    /**
     * 获取用户的投递记录
     * 
     * @param userId 用户ID
     * @return 投递记录列表
     */
    List<JobApplication> getUserApplications(Long userId);
    
    /**
     * 获取岗位的投递记录
     * 
     * @param jobId 岗位ID
     * @return 投递记录列表
     */
    List<JobApplication> getJobApplications(Integer jobId);
    
    /**
     * 更新投递状态
     * 
     * @param applicationId 投递记录ID
     * @param status 新状态
     * @param feedback 反馈信息
     * @return 更新后的投递记录
     */
    JobApplication updateApplicationStatus(Long applicationId, Integer status, String feedback);
    
    /**
     * 获取投递记录详情
     * 
     * @param applicationId 投递记录ID
     * @return 投递记录
     */
    JobApplication getApplicationById(Long applicationId);
} 