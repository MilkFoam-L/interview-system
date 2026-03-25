package interview.controller;

import interview.ai.spark.SparkX1Model;
import interview.service.AICodeJudgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 代码判题控制器
 */
@RestController
@RequestMapping("/api/code-judge")
@CrossOrigin(origins = "*")
public class CodeJudgeController {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeJudgeController.class);
    
    @Autowired
    private AICodeJudgeService aiCodeJudgeService;
    
    // 模拟提交ID生成器
    private final AtomicLong submissionIdGenerator = new AtomicLong(1);
    
    // 存储判题结果（实际项目中应该存储到数据库）
    private final Map<Long, SubmissionResult> submissionResults = new ConcurrentHashMap<>();
    
    /**
     * 提交代码判题请求
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitCode(@RequestBody CodeSubmissionRequest request) {
        try {
            logger.info("收到代码判题请求，题目ID: {}, 语言: {}", request.getQuestionId(), request.getLanguage());
            
            // 验证请求参数
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("代码不能为空"));
            }
            
            if (request.getLanguage() == null || request.getLanguage().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("编程语言不能为空"));
            }
            
            // 生成提交ID
            Long submissionId = submissionIdGenerator.getAndIncrement();
            
            // 异步执行判题（这里暂时同步执行，实际项目中应该异步）
            try {
                // 模拟题目描述（实际项目中应该从数据库获取）
                String problemDescription = getProblemDescription(request.getQuestionId());
                
                // 执行AI判题
                SparkX1Model.JudgeResult judgeResult = aiCodeJudgeService.quickJudge(
                    problemDescription, 
                    request.getCode(), 
                    request.getLanguage()
                );
                
                // 保存结果
                SubmissionResult result = new SubmissionResult();
                result.setSubmissionId(submissionId);
                result.setStatus("COMPLETED");
                result.setScore(judgeResult.getScore());
                result.setFeedback(judgeResult.getEvaluation());
                result.setAnalysis(judgeResult.getAnalysis());
                result.setSuggestions(judgeResult.getSuggestions());
                result.setResultType(judgeResult.getResultType());
                // 设置性能数据（如果AI返回为空，则设置模拟数据）
                result.setExecutionTime(judgeResult.getExecutionTime() != null ? 
                    judgeResult.getExecutionTime() : 
                    (long)(Math.random() * 100 + 20)); // 20-120ms
                result.setMemoryUsage(judgeResult.getMemoryUsage() != null ? 
                    judgeResult.getMemoryUsage() : 
                    (Math.random() * 50 + 10)); // 10-60MB
                result.setPassed(judgeResult.getPassed());
                
                submissionResults.put(submissionId, result);
                
                logger.info("代码判题完成，提交ID: {}, 得分: {}", submissionId, judgeResult.getScore());
                
                // 返回提交响应
                Map<String, Object> response = new HashMap<>();
                response.put("submissionId", submissionId);
                response.put("status", "COMPLETED");
                response.put("message", "代码判题完成");
                response.put("result", result);
                
                return ResponseEntity.ok(response);
                
            } catch (Exception e) {
                logger.error("代码判题失败", e);
                
                // 保存错误结果
                SubmissionResult errorResult = new SubmissionResult();
                errorResult.setSubmissionId(submissionId);
                errorResult.setStatus("FAILED");
                errorResult.setScore(0);
                errorResult.setFeedback("判题失败: " + e.getMessage());
                
                submissionResults.put(submissionId, errorResult);
                
                Map<String, Object> response = new HashMap<>();
                response.put("submissionId", submissionId);
                response.put("status", "FAILED");
                response.put("message", "代码判题失败: " + e.getMessage());
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            logger.error("处理代码判题请求异常", e);
            return ResponseEntity.badRequest().body(createErrorResponse("处理请求异常: " + e.getMessage()));
        }
    }
    
    /**
     * 查询判题结果
     */
    @GetMapping("/result/{submissionId}")
    public ResponseEntity<?> getJudgeResult(@PathVariable Long submissionId) {
        try {
            SubmissionResult result = submissionResults.get(submissionId);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("查询判题结果异常", e);
            return ResponseEntity.badRequest().body(createErrorResponse("查询结果异常: " + e.getMessage()));
        }
    }
    
    /**
     * 查询判题状态
     */
    @GetMapping("/status/{submissionId}")
    public ResponseEntity<?> getJudgeStatus(@PathVariable Long submissionId) {
        try {
            SubmissionResult result = submissionResults.get(submissionId);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("submissionId", submissionId);
            response.put("status", result.getStatus());
            response.put("message", getStatusMessage(result.getStatus()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("查询判题状态异常", e);
            return ResponseEntity.badRequest().body(createErrorResponse("查询状态异常: " + e.getMessage()));
        }
    }
    
    /**
     * 获取支持的编程语言
     */
    @GetMapping("/languages")
    public ResponseEntity<?> getSupportedLanguages() {
        try {
            List<String> languages = aiCodeJudgeService.getSupportedLanguages();
            return ResponseEntity.ok(languages);
        } catch (Exception e) {
            logger.error("获取支持语言异常", e);
            return ResponseEntity.badRequest().body(createErrorResponse("获取支持语言异常: " + e.getMessage()));
        }
    }
    
    /**
     * 检查服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<?> checkHealth() {
        try {
            boolean available = aiCodeJudgeService.isServiceAvailable();
            Map<String, Object> response = new HashMap<>();
            response.put("status", available ? "UP" : "DOWN");
            response.put("message", available ? "服务正常" : "服务不可用");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("检查服务状态异常", e);
            return ResponseEntity.badRequest().body(createErrorResponse("检查服务状态异常: " + e.getMessage()));
        }
    }
    
    /**
     * 获取题目描述（模拟实现）
     */
    private String getProblemDescription(Long questionId) {
        // 这里应该从数据库获取题目描述
        // 暂时使用模拟数据
        if (questionId != null && questionId == 56) {
            return "写一个 Python 函数，判断一个整数是否为素数。";
        }
        return "代码题目";
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        return error;
    }
    
    /**
     * 获取状态消息
     */
    private String getStatusMessage(String status) {
        switch (status) {
            case "PENDING":
                return "判题中...";
            case "COMPLETED":
                return "判题完成";
            case "FAILED":
                return "判题失败";
            default:
                return "未知状态";
        }
    }
    
    /**
     * 代码提交请求DTO
     */
    public static class CodeSubmissionRequest {
        private Long questionId;
        private String code;
        private String language;
        
        // Getters and Setters
        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }
    
    /**
     * 判题结果DTO
     */
    public static class SubmissionResult {
        private Long submissionId;
        private String status;
        private Integer score;
        private String feedback;
        private String analysis;
        private List<String> suggestions;
        private String resultType;
        private Long executionTime;
        private Double memoryUsage;
        private Boolean passed;
        
        // Getters and Setters
        public Long getSubmissionId() { return submissionId; }
        public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
        
        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }
        
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
        
        public String getResultType() { return resultType; }
        public void setResultType(String resultType) { this.resultType = resultType; }
        
        public Long getExecutionTime() { return executionTime; }
        public void setExecutionTime(Long executionTime) { this.executionTime = executionTime; }
        
        public Double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public Boolean getPassed() { return passed; }
        public void setPassed(Boolean passed) { this.passed = passed; }
    }
}
