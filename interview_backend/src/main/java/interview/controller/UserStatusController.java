package interview.controller;

import interview.util.UserContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user-status")
public class UserStatusController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserStatusController.class);
    
    @Autowired
    private UserContextUtil userContextUtil;
    
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkUserStatus(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            HttpSession session = request.getSession(false);
            boolean hasSession = session != null;
            String sessionId = hasSession ? session.getId() : null;
            
            Long userId = userContextUtil.getCurrentUserId();
            
            result.put("success", true);
            result.put("hasSession", hasSession);
            result.put("sessionId", sessionId);
            result.put("userId", userId);
            result.put("isLoggedIn", userId != null);
            
            logger.info("用户状态检查 - 会话存在: {}, 会话ID: {}, 用户ID: {}", 
                       hasSession, sessionId, userId);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("检查用户状态失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }
} 