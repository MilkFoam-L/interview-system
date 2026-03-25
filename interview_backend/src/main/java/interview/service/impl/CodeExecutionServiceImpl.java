package interview.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import interview.model.dto.CodeJudgeResult;
import interview.model.entity.CodeExecution;
import interview.repository.CodeExecutionRepository;
import interview.service.CodeExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 代码执行记录服务实现类
 */
@Slf4j
@Service
public class CodeExecutionServiceImpl implements CodeExecutionService {
    
    @Autowired
    private CodeExecutionRepository codeExecutionRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional
    public CodeExecution save(CodeExecution codeExecution) {
        return codeExecutionRepository.save(codeExecution);
    }
    
    @Override
    @Transactional
    public CodeExecution createExecutionRecord(Long answerId, Long sessionId, String code,
                                             String language, CodeJudgeResult judgeResult) {
        try {
            // 检查是否已存在该题目的记录，实现一题一记录
            CodeExecution execution = codeExecutionRepository
                .findByAnswerIdAndSessionId(answerId, sessionId)
                .orElse(null);
            
            // 如果不存在则创建新记录
            if (execution == null) {
                execution = new CodeExecution();
                execution.setAnswerId(answerId);
                execution.setSessionId(sessionId);
            }
            
            // 更新代码和语言信息
            execution.setCode(code);
            execution.setLanguage(language);
            
            // 设置执行状态
            String status = mapJudgeResultToStatus(judgeResult);
            execution.setStatus(status);
            
            // 设置执行结果
            if (judgeResult != null) {
                // 构建执行结果摘要
                String resultSummary = String.format("通过: %d/%d, 得分: %d",
                    judgeResult.getPassedCount() != null ? judgeResult.getPassedCount() : 0,
                    judgeResult.getTotalCount() != null ? judgeResult.getTotalCount() : 0,
                    judgeResult.getScore() != null ? judgeResult.getScore() : 0);
                execution.setExecutionResult(resultSummary);
                
                // 设置错误信息
                execution.setErrorMessage(judgeResult.getErrorMessage());
                
                // 设置执行时间和内存使用
                execution.setExecutionTime(judgeResult.getExecutionTime() != null ? 
                    judgeResult.getExecutionTime().intValue() : null);
                execution.setMemoryUsage(judgeResult.getMemoryUsage() != null ? 
                    judgeResult.getMemoryUsage().intValue() : null);
                
                // 设置AI分析结果（将详细的判题结果转换为JSON）
                try {
                    String aiAnalysis = objectMapper.writeValueAsString(judgeResult);
                    execution.setAiAnalysis(aiAnalysis);
                } catch (Exception e) {
                    log.warn("转换判题结果为JSON失败: {}", e.getMessage());
                    execution.setAiAnalysis(buildSimpleAnalysis(judgeResult));
                }
            } else {
                execution.setStatus(CodeExecution.Status.ERROR);
                execution.setErrorMessage("判题结果为空");
                execution.setAiAnalysis("系统错误：无法获取判题结果");
            }
            
            return codeExecutionRepository.save(execution);
            
        } catch (Exception e) {
            log.error("创建/更新代码执行记录失败: {}", e.getMessage(), e);
            
            // 尝试创建或更新错误记录
            CodeExecution errorExecution = codeExecutionRepository
                .findByAnswerIdAndSessionId(answerId, sessionId)
                .orElse(new CodeExecution());
                
            errorExecution.setAnswerId(answerId);
            errorExecution.setSessionId(sessionId);
            errorExecution.setCode(code);
            errorExecution.setLanguage(language != null ? language : "unknown");
            errorExecution.setStatus(CodeExecution.Status.ERROR);
            errorExecution.setErrorMessage("系统错误: " + e.getMessage());
            errorExecution.setAiAnalysis("系统在处理代码执行记录时发生异常");
            
            return codeExecutionRepository.save(errorExecution);
        }
    }
    
    @Override
    public CodeExecution findById(Long id) {
        return codeExecutionRepository.findById(id).orElse(null);
    }
    
    @Override
    public List<CodeExecution> findByAnswerId(Long answerId) {
        return codeExecutionRepository.findByAnswerId(answerId);
    }
    
    @Override
    public List<CodeExecution> findBySessionId(Long sessionId) {
        return codeExecutionRepository.findBySessionId(sessionId);
    }
    
    @Override
    public CodeExecution findLatestByAnswerId(Long answerId) {
        List<CodeExecution> executions = codeExecutionRepository
            .findByAnswerIdOrderByCreateTimeDesc(answerId);
        return executions.isEmpty() ? null : executions.get(0);
    }
    
    @Override
    public long countSuccessfulBySessionId(Long sessionId) {
        return codeExecutionRepository.countBySessionIdAndStatus(sessionId, CodeExecution.Status.SUCCESS);
    }
    
    @Override
    public long countFailedBySessionId(Long sessionId) {
        return codeExecutionRepository.countBySessionIdAndStatus(sessionId, CodeExecution.Status.ERROR);
    }
    
    @Override
    @Transactional
    public void deleteById(Long id) {
        codeExecutionRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public CodeExecution updateStatus(Long id, String status, String errorMessage) {
        CodeExecution execution = codeExecutionRepository.findById(id).orElse(null);
        if (execution != null) {
            execution.setStatus(status);
            execution.setErrorMessage(errorMessage);
            return codeExecutionRepository.save(execution);
        }
        return null;
    }
    
    /**
     * 将判题结果状态映射到执行状态
     */
    private String mapJudgeResultToStatus(CodeJudgeResult judgeResult) {
        if (judgeResult == null) {
            return CodeExecution.Status.ERROR;
        }
        
        String judgeStatus = judgeResult.getStatus();
        if (judgeStatus == null) {
            return CodeExecution.Status.ERROR;
        }
        
        switch (judgeStatus) {
            case "AC": // Accepted - 完全正确
            case "PA": // Partial Accepted - 部分正确
                return CodeExecution.Status.SUCCESS;
            case "WA": // Wrong Answer
            case "TLE": // Time Limit Exceeded
            case "MLE": // Memory Limit Exceeded  
            case "RE": // Runtime Error
            case "CE": // Compile Error
            case "SE": // System Error
            default:
                return CodeExecution.Status.ERROR;
        }
    }
    
    /**
     * 构建简单的分析结果
     */
    private String buildSimpleAnalysis(CodeJudgeResult judgeResult) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("判题状态: ").append(judgeResult.getStatus() != null ? 
            judgeResult.getStatus() : "未知").append("\n");
        analysis.append("通过测试: ").append(judgeResult.getPassedCount() != null ? 
            judgeResult.getPassedCount() : 0).append("/").append(judgeResult.getTotalCount() != null ? 
            judgeResult.getTotalCount() : 0).append("\n");
        analysis.append("得分: ").append(judgeResult.getScore() != null ? 
            judgeResult.getScore() : 0).append("/100");
        
        if (judgeResult.getErrorMessage() != null && !judgeResult.getErrorMessage().trim().isEmpty()) {
            analysis.append("\n错误: ").append(judgeResult.getErrorMessage());
        }
        
        return analysis.toString();
    }
}
