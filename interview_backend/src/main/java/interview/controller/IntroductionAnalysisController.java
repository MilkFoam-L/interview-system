package interview.controller;

import interview.dto.IntroductionAnalysisRequest;
import interview.dto.IntroductionAnalysisResponse;
import interview.model.IntroductionAnalysis;
import interview.service.IntroductionAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 自我介绍分析控制器
 */
@RestController
@RequestMapping("/api/introduction-analysis")
@CrossOrigin(origins = "*")
public class IntroductionAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(IntroductionAnalysisController.class);
    
    @Autowired
    private IntroductionAnalysisService analysisService;
    
    /**
     * 分析自我介绍
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeIntroduction(@RequestBody IntroductionAnalysisRequest request) {
        try {
            logger.info("收到自我介绍分析请求，会话ID：{}", request.getSessionId());
            
            IntroductionAnalysisResponse response = analysisService.analyzeAndSave(request);
            
            if (response.isSuccess()) {
                logger.info("自我介绍分析成功，会话ID：{}，整体得分：{}", 
                           request.getSessionId(), response.getOverallScore());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("自我介绍分析失败：{}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("处理自我介绍分析请求异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "处理分析请求异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取自我介绍分析结果
     */
    @GetMapping("/result/{sessionId}")
    public ResponseEntity<?> getAnalysisResult(@PathVariable Long sessionId) {
        try {
            logger.info("查询自我介绍分析结果，会话ID：{}", sessionId);
            
            IntroductionAnalysis analysis = analysisService.getBySessionId(sessionId);
            
            if (analysis != null) {
                logger.info("✅ 找到分析结果 - ID: {}, SessionId: {}, 分数: fluency={}, expression={}, content={}, overall={}, 分析结果长度: {}", 
                           analysis.getId(), 
                           analysis.getSessionId(),
                           analysis.getFluencyScore(),
                           analysis.getExpressionScore(), 
                           analysis.getContentScore(),
                           analysis.getOverallScore(),
                           analysis.getAnalysisResult() != null ? analysis.getAnalysisResult().length() : 0);
                
                // 构建响应数据
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("data", analysis);
                result.put("message", "查询成功");
                
                logger.info("返回数据: success={}, data.sessionId={}, data.fluencyScore={}", 
                           result.get("success"), 
                           analysis.getSessionId(), 
                           analysis.getFluencyScore());
                
                return ResponseEntity.ok(result);
            } else {
                logger.warn("❌ 未找到会话ID为 {} 的分析结果", sessionId);
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "未找到指定会话的分析结果");
                
                return ResponseEntity.ok(result);
            }
            
        } catch (Exception e) {
            logger.error("查询自我介绍分析结果异常，会话ID：{}", sessionId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询分析结果异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 重新分析自我介绍
     */
    @PostMapping("/reanalyze/{sessionId}")
    public ResponseEntity<?> reanalyzeIntroduction(@PathVariable Long sessionId) {
        try {
            logger.info("重新分析自我介绍，会话ID：{}", sessionId);
            
            IntroductionAnalysisResponse response = analysisService.reanalyze(sessionId);
            
            if (response.isSuccess()) {
                logger.info("重新分析成功，会话ID：{}，整体得分：{}", 
                           sessionId, response.getOverallScore());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("重新分析失败：{}", response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("重新分析自我介绍异常，会话ID：{}", sessionId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "重新分析异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 检查分析服务状态
     */
    @GetMapping("/health")
    public ResponseEntity<?> checkServiceHealth() {
        try {
            boolean available = analysisService.isServiceAvailable();
            
            Map<String, Object> result = new HashMap<>();
            result.put("status", available ? "UP" : "DOWN");
            result.put("message", available ? "自我介绍分析服务正常" : "自我介绍分析服务不可用");
            result.put("available", available);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("检查自我介绍分析服务状态异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "检查服务状态异常: " + e.getMessage());
            error.put("available", false);
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取分析维度说明
     */
    @GetMapping("/dimensions")
    public ResponseEntity<?> getAnalysisDimensions() {
        try {
            Map<String, Object> dimensions = new HashMap<>();
            
            Map<String, String> fluency = new HashMap<>();
            fluency.put("name", "语言流畅度");
            fluency.put("description", "语言表达的流畅性、清晰度和语法正确性");
            fluency.put("range", "0-10分");
            
            Map<String, String> expression = new HashMap<>();
            expression.put("name", "表达能力");
            expression.put("description", "逻辑结构、语言组织能力和说服力");
            expression.put("range", "0-10分");
            
            Map<String, String> content = new HashMap<>();
            content.put("name", "内容质量");
            content.put("description", "信息完整性、相关性和专业匹配度");
            content.put("range", "0-10分");
            
            Map<String, String> overall = new HashMap<>();
            overall.put("name", "整体得分");
            overall.put("description", "综合表现的专业性和整体印象");
            overall.put("range", "0-10分");
            
            dimensions.put("fluencyScore", fluency);
            dimensions.put("expressionScore", expression);
            dimensions.put("contentScore", content);
            dimensions.put("overallScore", overall);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", dimensions);
            result.put("message", "获取分析维度说明成功");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("获取分析维度说明异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取分析维度说明异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}