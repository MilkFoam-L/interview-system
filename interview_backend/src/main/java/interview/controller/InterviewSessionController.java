package interview.controller;

import interview.model.entity.InterviewSession;
import interview.service.InterviewSessionService;
import interview.util.UserContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 面试会话控制器
 */
@RestController
@RequestMapping("/api/sessions")
public class InterviewSessionController {

    private static final Logger logger = LoggerFactory.getLogger(InterviewSessionController.class);
    
    private final InterviewSessionService sessionService;
    private final UserContextUtil userContextUtil;

    @Autowired
    public InterviewSessionController(InterviewSessionService sessionService, UserContextUtil userContextUtil) {
        this.sessionService = sessionService;
        this.userContextUtil = userContextUtil;
    }

    /**
     * 创建面试会话
     */
    @PostMapping
    public ResponseEntity<InterviewSession> createSession(@RequestBody Map<String, Object> request) {
        logger.info("创建面试会话请求: {}", request);
        
        Integer jobId = (Integer) request.get("jobId");
        Long applicationId = request.get("applicationId") != null ? Long.valueOf(request.get("applicationId").toString()) : null;
        // 优先从后端获取登录上下文中的用户ID，避免前端传错
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            // 兼容旧逻辑：如果上下文没有，则尝试从请求体读取
            userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
        }
        
        if (userId == null) {
            logger.error("创建面试会话失败: 用户ID为空");
            return ResponseEntity.badRequest().build();
        }
        
        logger.info("创建面试会话参数: jobId={}, applicationId={}, userId={}", jobId, applicationId, userId);
        
        InterviewSession session = sessionService.createSessionWithUserId(jobId, applicationId, userId);
        return ResponseEntity.ok(session);
    }

    /**
     * 获取面试会话
     */
    @GetMapping("/{id}")
    public ResponseEntity<InterviewSession> getSession(@PathVariable Long id) {
        logger.info("获取面试会话: id={}", id);
        InterviewSession session = sessionService.getSession(id);
        return ResponseEntity.ok(session);
    }

    /**
     * 更新面试会话状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<InterviewSession> updateSessionStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        logger.info("更新面试会话状态: id={}, status={}", id, status);
        InterviewSession session = sessionService.updateSessionStatus(id, status);
        return ResponseEntity.ok(session);
    }

    /**
     * 获取用户的所有面试会话
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InterviewSession>> getUserSessions(@PathVariable Long userId) {
        logger.info("获取用户面试会话: userId={}", userId);
        List<InterviewSession> sessions = sessionService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }
} 