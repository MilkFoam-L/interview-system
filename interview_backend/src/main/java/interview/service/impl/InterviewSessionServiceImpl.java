package interview.service.impl;

import interview.model.entity.InterviewSession;
import interview.repository.ApplicationRecordRepository;
import interview.repository.InterviewSessionRepository;
import interview.repository.JobRepository;
import interview.repository.InterviewStageProgressRepository;
import interview.model.entity.InterviewStageProgress;
import interview.service.InterviewSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterviewSessionServiceImpl implements InterviewSessionService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewSessionServiceImpl.class);
    
    private final InterviewSessionRepository sessionRepository;
    private final JobRepository jobRepository;
    private final ApplicationRecordRepository applicationRepository;
    private final interview.repository.JobApplicationRepository jobApplicationRepository;
    private final InterviewStageProgressRepository stageProgressRepository;
    
    @Autowired
    public InterviewSessionServiceImpl(
            InterviewSessionRepository sessionRepository,
            JobRepository jobRepository,
            ApplicationRecordRepository applicationRepository,
            interview.repository.JobApplicationRepository jobApplicationRepository,
            InterviewStageProgressRepository stageProgressRepository) {
        this.sessionRepository = sessionRepository;
        this.jobRepository = jobRepository;
        this.applicationRepository = applicationRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.stageProgressRepository = stageProgressRepository;
    }

    @Override
    @Transactional
    public InterviewSession createSessionWithUserId(Integer jobId, Long applicationId, Long userId) {
        logger.info("开始创建面试会话: jobId={}, applicationId={}, userId={}", jobId, applicationId, userId);

        Long jobApplicationId = null;
        // 关联 job_applications
        List<interview.model.JobApplication> jaList = jobApplicationRepository.findByUserIdAndJobId(userId, jobId);
        if (!jaList.isEmpty()) {
            jobApplicationId = jaList.get(0).getId();
            logger.info("匹配到 JobApplication, id={}", jobApplicationId);
        }

        // 不再强制创建 application_records；仅当前端显式传入或已有
        if (applicationId == null) {
            List<interview.model.ApplicationRecord> arList = applicationRepository.findByUserIdAndJobId(userId, jobId);
            if (!arList.isEmpty()) {
                applicationId = arList.get(0).getId();
                logger.info("匹配到 ApplicationRecord, id={}", applicationId);
            }
        }
        
        try {
            // 验证jobId是否存在
            var job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new EntityNotFoundException("找不到岗位ID: " + jobId));
            
            logger.info("找到岗位: {}, 类型: {}", job.getTitle(), job.getCategory());
            
            // 创建新的面试会话
            InterviewSession session = new InterviewSession();
            session.setUserId(userId);
            session.setJobId(jobId);
            session.setApplicationId(applicationId);
            session.setJobApplicationId(jobApplicationId);
            session.setSessionStatus("PREPARING");
            session.setStartTime(LocalDateTime.now());
            session.setPositionType(job.getCategoryType());
            
            // 保存会话
            InterviewSession savedSession = sessionRepository.save(session);
            logger.info("面试会话创建成功: id={}", savedSession.getId());

            // 自动初始化面试阶段进度 - 提前初始化所有阶段
            String[] stages = {"INTRODUCTION", "BASIC_QA", "SCENARIO", "CODE_TEST"};
            for (String stage : stages) {
                InterviewStageProgress progress = new InterviewStageProgress();
                progress.setSessionId(savedSession.getId());
                progress.setStage(stage);
                progress.setStatus("NOT_STARTED");
                progress.setCreateTime(LocalDateTime.now());
                progress.setUpdateTime(LocalDateTime.now());
                stageProgressRepository.save(progress);
            }
            logger.info("已为会话ID={}初始化所有面试阶段进度", savedSession.getId());
            
            // 为提高性能，这里进行延迟加载所需的关联实体
            try {
                // 尝试预加载关联的申请记录
                if (applicationId != null) {
                    applicationRepository.findById(applicationId);
                    logger.info("已预加载会话关联的申请记录 ID={}", applicationId);
                }
            } catch (Exception e) {
                // 仅记录日志，不影响主流程
                logger.warn("预加载关联实体失败，不影响主流程: {}", e.getMessage());
            }
            
            return savedSession;
        } catch (Exception e) {
            logger.error("创建面试会话失败", e);
            throw e; // 重新抛出异常，让Spring事务管理器处理回滚
        }
    }

    @Override
    public InterviewSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("面试会话不存在: " + id));
    }

    @Override
    @Transactional
    public InterviewSession updateSessionStatus(Long id, String status) {
        InterviewSession session = getSession(id);
        session.setSessionStatus(status);
        
        if ("COMPLETED".equals(status)) {
            session.setEndTime(LocalDateTime.now());
            // 计算持续时间（秒）
            if (session.getStartTime() != null) {
                long durationInSeconds = java.time.Duration.between(
                        session.getStartTime(), session.getEndTime()).getSeconds();
                session.setDuration((int) durationInSeconds);
            }
        } else if ("ONGOING".equals(status)) {
            session.setStartTime(LocalDateTime.now());
        }
        
        return sessionRepository.save(session);
    }

    @Override
    public List<InterviewSession> getUserSessions(Long userId) {
        return sessionRepository.findByUserId(userId);
    }
} 