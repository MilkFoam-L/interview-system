package interview.service;

import interview.model.dto.CodeJudgeResult;
import interview.model.entity.CodeExecution;

import java.util.List;

/**
 * 代码执行记录服务接口
 */
public interface CodeExecutionService {
    
    /**
     * 保存代码执行记录
     */
    CodeExecution save(CodeExecution codeExecution);
    
    /**
     * 创建并保存代码执行记录
     */
    CodeExecution createExecutionRecord(Long answerId, Long sessionId, String code, 
                                      String language, CodeJudgeResult judgeResult);
    
    /**
     * 根据ID查找执行记录
     */
    CodeExecution findById(Long id);
    
    /**
     * 根据答题记录ID查找执行记录
     */
    List<CodeExecution> findByAnswerId(Long answerId);
    
    /**
     * 根据会话ID查找执行记录
     */
    List<CodeExecution> findBySessionId(Long sessionId);
    
    /**
     * 查找指定答题记录的最新执行记录
     */
    CodeExecution findLatestByAnswerId(Long answerId);
    
    /**
     * 统计指定会话中成功执行的记录数量
     */
    long countSuccessfulBySessionId(Long sessionId);
    
    /**
     * 统计指定会话中失败执行的记录数量  
     */
    long countFailedBySessionId(Long sessionId);
    
    /**
     * 删除执行记录
     */
    void deleteById(Long id);
    
    /**
     * 更新执行状态
     */
    CodeExecution updateStatus(Long id, String status, String errorMessage);
}
