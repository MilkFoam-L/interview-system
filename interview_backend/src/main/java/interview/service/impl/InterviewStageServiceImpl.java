package interview.service.impl;

import interview.model.entity.InterviewStageProgress;
import interview.model.entity.InterviewSession;
import interview.repository.InterviewStageProgressRepository;
import interview.repository.InterviewSessionRepository;
import interview.service.InterviewStageService;
import interview.service.ComprehensiveReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InterviewStageServiceImpl implements InterviewStageService {
    
    private static final Logger log = LoggerFactory.getLogger(InterviewStageServiceImpl.class);
    
    @Autowired
    private InterviewStageProgressRepository repository;
    
    @Autowired
    private InterviewSessionRepository interviewSessionRepository;
    
    @Autowired
    private ComprehensiveReportService comprehensiveReportService;

    @Override
    public List<InterviewStageProgress> getStagesBySessionId(Long sessionId) {
        return repository.findBySessionId(sessionId);
    }

    @Override
    public InterviewStageProgress updateStageStatus(Long sessionId, String stage, String status) {
        InterviewStageProgress progress = repository.findBySessionIdAndStage(sessionId, stage);
        if (progress != null) {
            progress.setStatus(status);
            progress.setUpdateTime(LocalDateTime.now());
            repository.save(progress);
        }
        return progress;
    }

    @Override
    public InterviewStageProgress startStage(Long sessionId, String stage) {
        InterviewStageProgress progress = repository.findBySessionIdAndStage(sessionId, stage);
        if (progress != null) {
            progress.setStatus("IN_PROGRESS");
            progress.setStartTime(LocalDateTime.now());
            progress.setUpdateTime(LocalDateTime.now());
            repository.save(progress);
        }
        return progress;
    }

    @Override
    public InterviewStageProgress completeStage(Long sessionId, String stage) {
        log.info("完成面试环节 sessionId={}, stage={}", sessionId, stage);
        
        InterviewStageProgress progress = repository.findBySessionIdAndStage(sessionId, stage);
        if (progress != null) {
            progress.setStatus("COMPLETED");
            progress.setEndTime(LocalDateTime.now());
            if (progress.getStartTime() != null) {
                progress.setDuration((int) java.time.Duration.between(progress.getStartTime(), progress.getEndTime()).getSeconds());
            }
            progress.setUpdateTime(LocalDateTime.now());
            repository.save(progress);
            
            log.info("面试环节已完成 sessionId={}, stage={}, duration={}秒", 
                    sessionId, stage, progress.getDuration());
        }
        
        // 检查是否是基础问答环节完成，如果是则触发AI分析
        if ("BASIC_QA".equals(stage) && progress != null) {
            log.info("基础问答环节完成，开始触发AI分析 sessionId={}", sessionId);
            triggerBasicQAAnalysis(sessionId);
        }
        
        return progress;
    }
    
    /**
     * 触发基础问答AI分析
     */
    private void triggerBasicQAAnalysis(Long sessionId) {
        try {
            // 获取面试会话信息以获取userId
            Optional<InterviewSession> sessionOpt = interviewSessionRepository.findById(sessionId);
            
            if (!sessionOpt.isPresent()) {
                log.warn("未找到面试会话，无法触发基础问答分析 sessionId={}", sessionId);
                return;
            }
            
            InterviewSession session = sessionOpt.get();
            Long userId = session.getUserId();
            
            if (userId == null) {
                log.warn("面试会话中用户ID为空，无法触发基础问答分析 sessionId={}", sessionId);
                return;
            }
            
            log.info("开始异步执行基础问答AI分析 sessionId={}, userId={}", sessionId, userId);
            
            // 异步执行分析以避免阻塞用户操作
            new Thread(() -> {
                try {
                    comprehensiveReportService.analyzeAndUpdateBasicQA(sessionId, userId);
                    log.info("基础问答AI分析完成 sessionId={}, userId={}", sessionId, userId);
                } catch (Exception e) {
                    log.error("基础问答AI分析失败 sessionId={}, userId={}", sessionId, userId, e);
                }
            }).start();
            
        } catch (Exception e) {
            log.error("触发基础问答AI分析失败 sessionId={}", sessionId, e);
        }
    }

    @Override
    public InterviewStageProgress createStageProgress(InterviewStageProgress progress) {
        progress.setCreateTime(LocalDateTime.now());
        progress.setUpdateTime(LocalDateTime.now());
        return repository.save(progress);
    }
} 