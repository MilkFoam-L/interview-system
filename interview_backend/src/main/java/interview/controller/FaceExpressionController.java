package interview.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import interview.dto.FaceExpressionRequest;
import interview.dto.FaceExpressionResponse;
import interview.dto.FaceExpressionSummaryResponse;
import interview.service.FaceExpressionService;

/**
 * 面部表情控制器
 * 提供API接口处理表情分析请求
 */
@RestController
@RequestMapping("/api/face-expression")
public class FaceExpressionController {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceExpressionController.class);
    
    @Autowired
    private FaceExpressionService faceExpressionService;
    
    /**
     * 分析面部表情
     * 
     * @param request 表情分析请求
     * @return 表情分析响应
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeFaceExpression(@RequestBody FaceExpressionRequest request) {
        try {
            logger.info("收到表情分析请求，会话ID: {}, 用户ID: {}", request.getSessionId(), request.getUserId());
            FaceExpressionResponse response = faceExpressionService.analyzeExpression(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("处理表情分析请求异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "处理表情分析请求异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取表情分析摘要
     * 
     * @param sessionId 会话ID
     * @return 表情分析摘要响应
     */
    @GetMapping("/summary/{sessionId}")
    public ResponseEntity<?> getExpressionSummary(@PathVariable Long sessionId) {
        try {
            logger.info("获取表情分析摘要，会话ID: {}", sessionId);
            FaceExpressionSummaryResponse response = faceExpressionService.getExpressionSummary(sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取表情分析摘要异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取表情分析摘要异常: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 测试表情分析API可用性
     * 
     * @return 测试结果
     */
    @GetMapping("/test")
    public ResponseEntity<?> testFaceExpressionApi() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "面部表情分析API服务正常运行");
        result.put("version", "1.0.0");
        return ResponseEntity.ok(result);
    }
} 