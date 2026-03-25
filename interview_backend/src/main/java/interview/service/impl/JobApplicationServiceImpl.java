package interview.service.impl;

import interview.model.ApplicationRecord;
import interview.model.JobApplication;
import interview.repository.ApplicationRecordRepository;
import interview.repository.JobApplicationRepository;
import interview.repository.JobRepository;
import interview.repository.ResumeRepository;
import interview.repository.UserRepository;
import interview.service.JobApplicationService;
import interview.service.InterviewStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import interview.model.InterviewStatistics;

/**
 * 简历投递服务实现类
 */
@Service
public class JobApplicationServiceImpl implements JobApplicationService {
    
    @Autowired
    private JobApplicationRepository jobApplicationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private ResumeRepository resumeRepository;
    
    @Autowired
    private ApplicationRecordRepository applicationRecordRepository;
    
    @Autowired
    private InterviewStatisticsService statisticsService;
    
    @Override
    @Transactional
    public JobApplication applyJob(Long userId, Integer jobId, Long resumeId) {
        // 验证用户和岗位是否存在
        userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("用户不存在"));
        
        jobRepository.findById(jobId)
                .orElseThrow(() -> new NoSuchElementException("岗位不存在"));
        
        // 检查是否已经投递过
        List<JobApplication> existingApplications = jobApplicationRepository.findByUserIdAndJobId(userId, jobId);
        if (!existingApplications.isEmpty()) {
            throw new IllegalStateException("您已经投递过该岗位");
        }
        
        // 创建新的投递记录
        JobApplication jobApplication = new JobApplication();
        jobApplication.setUserId(userId);
        jobApplication.setJobId(jobId);
        jobApplication.setResumeId(resumeId);
        jobApplication.setStatus(1); // 直接设置为待面试状态
        
        // 保存投递记录
        JobApplication savedApplication = jobApplicationRepository.save(jobApplication);
        
        // 将简历信息快照保存到投递记录中
        resumeRepository.findById(resumeId).ifPresent(resume -> {
            savedApplication.setResumeSnapshot(resume.getContent());
            jobApplicationRepository.save(savedApplication);
        });
        
        return savedApplication;
    }
    
    @Override
    public List<JobApplication> getUserApplications(Long userId) {
        return jobApplicationRepository.findByUserId(userId);
    }
    
    @Override
    public List<JobApplication> getJobApplications(Integer jobId) {
        return jobApplicationRepository.findByJobId(jobId);
    }
    
    @Override
    @Transactional
    public JobApplication updateApplicationStatus(Long applicationId, Integer status, String feedback) {
        JobApplication jobApplication = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("投递记录不存在"));
        
        // 记录原状态，用于判断是否从状态1转变为状态2
        Integer oldStatus = jobApplication.getStatus();
        
        jobApplication.setStatus(status);
        if (feedback != null) {
            jobApplication.setFeedback(feedback);
        }
        
        // 保存JobApplication状态更新
        JobApplication updatedApplication = jobApplicationRepository.save(jobApplication);
        System.out.println("已更新JobApplication状态为: " + status);
        
        // 根据JobApplication状态同步更新ApplicationRecord状态
        if (status != null) {
            // 查找对应的ApplicationRecord记录
            List<ApplicationRecord> records = applicationRecordRepository.findByUserIdAndJobId(
                    jobApplication.getUserId(), jobApplication.getJobId());
            
            ApplicationRecord applicationRecord;
            
            if (records.isEmpty()) {
                // 如果不存在，则创建新记录
                applicationRecord = new ApplicationRecord();
                applicationRecord.setUserId(jobApplication.getUserId());
                applicationRecord.setJobId(jobApplication.getJobId());
                applicationRecord.setSubmitTime(LocalDateTime.now());
                    
                System.out.println("创建新的ApplicationRecord记录: userId=" + jobApplication.getUserId() 
                    + ", jobId=" + jobApplication.getJobId());
            } else {
                // 使用已有记录
                applicationRecord = records.get(0);
                System.out.println("使用已有的ApplicationRecord记录: id=" + applicationRecord.getId() + ", 当前状态: " + applicationRecord.getStatus());
            }
            
            // 根据JobApplication状态码设置ApplicationRecord状态
            String oldRecordStatus = applicationRecord.getStatus();
            switch(status) {
                case 0: // 未投递/待审核
                    applicationRecord.setStatus("SUBMITTED");
                    break;
                case 1: // 待面试(合并了原来的已投递和待面试状态)
                    applicationRecord.setStatus("PENDING");
                    break;
                case 3: // 已面试
                    applicationRecord.setStatus("ONGOING");
                    break;
                case 4: // 已完成/查看分析报告
                    // 如果之前不是COMPLETED状态，才设置完成时间
                    if (!"COMPLETED".equals(oldRecordStatus)) {
                    applicationRecord.setStatus("COMPLETED");
                    applicationRecord.setCompleteTime(LocalDateTime.now());
                        System.out.println("设置ApplicationRecord状态为COMPLETED，记录面试已完成");
                    }
                    break;
                default:
                    // 其他状态不处理
                    break;
            }
            
            // 同步反馈信息
            if (feedback != null) {
                applicationRecord.setFeedback(feedback);
            }
            
            // 特别处理：确保统计更新
            boolean isTransitionToPending = (oldStatus != null && oldStatus != 1 && status == 1) 
                    || (oldRecordStatus != null && !oldRecordStatus.equals("PENDING") && "PENDING".equals(applicationRecord.getStatus()));
            
            // 保存记录
            applicationRecord = applicationRecordRepository.save(applicationRecord);
            System.out.println("ApplicationRecord保存完成，ID=" + applicationRecord.getId() 
                    + ", 状态=" + applicationRecord.getStatus());
            
            // 无论是否状态转换，都强制更新统计数据，确保待面试数量准确
                InterviewStatistics statistics = statisticsService.updateUserStatistics(jobApplication.getUserId());
            System.out.println("已更新统计数据，待面试数量=" + statistics.getPendingCount());
        }
        
        return updatedApplication;
    }
    
    @Override
    public JobApplication getApplicationById(Long applicationId) {
        return jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("投递记录不存在"));
    }
} 