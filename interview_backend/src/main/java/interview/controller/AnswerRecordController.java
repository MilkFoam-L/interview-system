package interview.controller;

import interview.model.entity.AnswerRecord;
import interview.model.dto.AnswerRecordDTO;
import interview.service.AnswerRecordService;
import interview.model.entity.CodeExecution;
import interview.repository.CodeExecutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/answerRecord")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AnswerRecordController {
    @Autowired
    private AnswerRecordService answerRecordService;
    
    @Autowired
    private CodeExecutionRepository codeExecutionRepository;

    @PostMapping("")
    public ResponseEntity<AnswerRecordDTO> saveOrUpdateAnswer(@RequestBody AnswerRecordDTO dto) {
        try {
            // 从dto中获取sessionId，但service使用interviewId
            Long sessionId = dto.getSessionId();
            if (sessionId != null && dto.getInterviewId() == null) {
                dto.setInterviewId(sessionId); // 兼容处理，sessionId作为interviewId使用
            }
            
            // 确保必要字段不为null
            if (dto.getUserId() == null) {
                System.err.println("保存答案记录失败: userId不能为空");
                return ResponseEntity.badRequest().build();
            }
            
            // 验证用户ID的有效性（无需创建用户，只需确保ID存在且合理）
            if (dto.getUserId() <= 0) {
                System.err.println("保存答案记录失败: userId必须为正数");
                return ResponseEntity.badRequest().build();
            }
            
            if (dto.getQuestionId() == null) {
                System.err.println("保存答案记录失败: questionId不能为空");
                return ResponseEntity.badRequest().build();
            }
            
            // 确保提交时间不为null
            if (dto.getSubmitTime() == null) {
                dto.setSubmitTime(LocalDateTime.now()); // 设置为当前时间
            }
            
            // 确保开始时间不为null
            if (dto.getStartTime() == null) {
                LocalDateTime submitTime = dto.getSubmitTime();
                // 设置为提交时间前60秒或当前时间前60秒
                dto.setStartTime(submitTime != null ? 
                    submitTime.minusSeconds(60) : LocalDateTime.now().minusSeconds(60));
            }
            
            AnswerRecord result = answerRecordService.saveOrUpdateAnswer(dto);
            
            // 转换为DTO，确保所有字段完整传递
            AnswerRecordDTO responseDTO = convertToDTO(result);
            // 保持sessionId字段，便于前端识别
            responseDTO.setSessionId(sessionId);
            
            // 添加CORS响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            headers.add("Access-Control-Allow-Headers", "*");
            
            return ResponseEntity.ok().headers(headers).body(responseDTO);
        } catch (Exception e) {
            System.err.println("保存答案记录失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 将AnswerRecord实体转换为DTO
     */
    private AnswerRecordDTO convertToDTO(AnswerRecord record) {
        AnswerRecordDTO dto = new AnswerRecordDTO();
        dto.setId(record.getId());
        dto.setUserId(record.getUserId());
        dto.setQuestionId(record.getQuestionId());
        dto.setInterviewId(record.getInterviewId());
        dto.setUserAnswer(record.getUserAnswer());
        dto.setType(record.getType());
        dto.setStartTime(record.getStartTime());
        dto.setSubmitTime(record.getSubmitTime());
        dto.setTimeUsed(record.getTimeUsed());
        dto.setIsCorrect(record.getIsCorrect());
        dto.setScore(record.getScore());
        dto.setAnalysis(record.getAnalysis()); // 确保analysis字段传递
        dto.setCreateTime(record.getCreateTime());
        dto.setUpdateTime(record.getUpdateTime());
        
        // 从 CodeExecution 表中获取性能数据
        if (record.getId() != null) {
            try {
                java.util.List<CodeExecution> executions = codeExecutionRepository.findByAnswerId(record.getId());
                if (!executions.isEmpty()) {
                    CodeExecution latestExecution = executions.get(executions.size() - 1); // 获取最新的执行记录
                    dto.setMemoryUsage(latestExecution.getMemoryUsage() != null ? 
                        latestExecution.getMemoryUsage().longValue() : null);
                    dto.setExecutionTime(latestExecution.getExecutionTime() != null ? 
                        latestExecution.getExecutionTime().longValue() : null);
                }
            } catch (Exception e) {
                System.err.println("获取代码执行性能数据失败: " + e.getMessage());
            }
        }
        
        return dto;
    }
    
    @GetMapping("/user/{userId}/question/{questionId}")
    public AnswerRecord getAnswerRecord(
        @PathVariable Long userId, 
        @PathVariable Long questionId,
        @RequestParam(required = false) Long interviewId) {
        
        try {
            if (interviewId != null) {
                // 使用精确查询（带interviewId）
                return answerRecordService.getByUserIdAndQuestionIdAndInterviewId(userId, questionId, interviewId);
            } else {
                // 回退到传统查询
                return answerRecordService.getByUserIdAndQuestionId(userId, questionId);
            }
        } catch (Exception e) {
            System.err.println("获取答案记录失败: " + e.getMessage());
            return null;
        }
    }
    
    @GetMapping("/user/{userId}")
    public List<AnswerRecord> getUserAnswerRecords(@PathVariable Long userId) {
        try {
            return answerRecordService.getAnswerRecordsByUserId(userId);
        } catch (Exception e) {
            System.err.println("获取用户答案记录失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @GetMapping("/user/{userId}/session/{sessionId}")
    public List<AnswerRecord> getUserSessionAnswers(
        @PathVariable Long userId, 
        @PathVariable Long sessionId) {
        
        try {
            return answerRecordService.getAnswerRecordsByUserIdAndInterviewId(userId, sessionId);
        } catch (Exception e) {
            System.err.println("获取用户会话答案记录失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
} 