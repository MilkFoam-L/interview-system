package interview.util;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户上下文工具类，用于管理当前登录用户信息
 */
@Component
public class UserContextUtil {
    
    private static final String USER_ID_KEY = "currentUserId";
    private static final String USER_ROLE_KEY = "currentUserRole";
    private static final Logger logger = LoggerFactory.getLogger(UserContextUtil.class);
    
    /**
     * 设置当前用户ID
     * 
     * @param userId 用户ID
     * @return 设置是否成功
     */
    public boolean setCurrentUserId(Long userId) {
        if (userId == null) {
            logger.warn("尝试设置空的用户ID");
            return false;
        }
        
        try {
            HttpServletRequest request = getRequest();
            if (request == null) {
                logger.error("无法设置用户ID: 请求对象为null");
                return false;
            }
            
            // 创建或获取会话
            try {
                HttpSession session = request.getSession(true);
                if (session == null) {
                    logger.error("无法设置用户ID: 会话对象为null");
                    return false;
                }
                
                session.setAttribute(USER_ID_KEY, userId);
                return true;
            } catch (IllegalStateException e) {
                // 处理会话已失效的情况
                logger.error("设置用户ID失败: 会话已失效");
                return false;
            }
        } catch (Exception e) {
            logger.error("设置用户ID过程中发生异常: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取当前用户ID
     * 
     * @return 用户ID，如果未登录则返回null
     */
    public Long getCurrentUserId() {
        try {
            HttpServletRequest request = getRequest();
            if (request == null) {
                return null;
            }
            
            // 首先尝试从会话中获取
            try {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    Long userId = (Long) session.getAttribute(USER_ID_KEY);
                    if (userId != null) {
                        return userId;
                    }
                }
            } catch (Exception e) {
            }
            
            // 然后尝试从请求参数中获取userId
            String userIdParam = request.getParameter("userId");
            if (userIdParam != null && !userIdParam.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdParam);
                    return userId;
                } catch (NumberFormatException e) {
                }
            }
            
            // 尝试从请求头中获取userId
            String userIdHeader = request.getHeader("userId");
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                try {
                    Long userId = Long.parseLong(userIdHeader);
                    return userId;
                } catch (NumberFormatException e) {
                }
            }
            
            // 尝试从X-User-ID请求头获取
            String xUserIdHeader = request.getHeader("X-User-ID");
            if (xUserIdHeader != null && !xUserIdHeader.isEmpty()) {
                try {
                    Long userId = Long.parseLong(xUserIdHeader);
                    return userId;
                } catch (NumberFormatException e) {
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从请求头中获取用户ID
     * @return 请求头中的用户ID字符串，如果不存在则返回null
     */
    public String getUserIdFromHeader() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        
        // 依次检查可能的请求头
        String userIdHeader = request.getHeader("X-User-ID");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            return userIdHeader;
        }
        
        userIdHeader = request.getHeader("userId");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            return userIdHeader;
        }
        
        // 检查Authorization头，如果是Bearer token格式
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 这里可以添加token解析逻辑，从token中提取userId
        }
        
        return null;
    }
    
    /**
     * 清除当前用户ID
     */
    public void clearCurrentUserId() {
        try {
            HttpServletRequest request = getRequest();
            if (request != null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.removeAttribute(USER_ID_KEY);
                    session.removeAttribute(USER_ROLE_KEY);
                }
            }
        } catch (Exception e) {
            logger.error("清除用户ID失败: {}", e.getMessage());
        }
    }
    
    /**
     * 设置当前用户角色
     * 
     * @param userRole 用户角色
     * @return 设置是否成功
     */
    public boolean setCurrentUserRole(String userRole) {
        if (userRole == null || userRole.trim().isEmpty()) {
            logger.warn("尝试设置空的用户角色");
            return false;
        }
        
        try {
            HttpServletRequest request = getRequest();
            if (request == null) {
                logger.error("无法设置用户角色: 请求对象为null");
                return false;
            }
            
            try {
                HttpSession session = request.getSession(true);
                if (session == null) {
                    logger.error("无法设置用户角色: 会话对象为null");
                    return false;
                }
                
                session.setAttribute(USER_ROLE_KEY, userRole);
                return true;
            } catch (IllegalStateException e) {
                logger.error("设置用户角色失败: 会话已失效");
                return false;
            }
        } catch (Exception e) {
            logger.error("设置用户角色过程中发生异常: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取当前用户角色
     * 
     * @return 用户角色，如果未设置则返回null
     */
    public String getCurrentUserRole() {
        try {
            HttpServletRequest request = getRequest();
            if (request == null) {
                return null;
            }
            
            // 首先尝试从会话中获取
            try {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    String userRole = (String) session.getAttribute(USER_ROLE_KEY);
                    if (userRole != null) {
                        return userRole;
                    }
                }
            } catch (Exception e) {
                logger.warn("从会话获取用户角色失败");
            }
            
            // 尝试从请求头中获取
            String roleHeader = request.getHeader("X-User-Role");
            if (roleHeader != null && !roleHeader.isEmpty()) {
                return roleHeader;
            }
            
            roleHeader = request.getHeader("userRole");
            if (roleHeader != null && !roleHeader.isEmpty()) {
                return roleHeader;
            }
            
            // 默认返回INTERVIEWER（临时方案，实际应该从数据库或token中获取）
            return "INTERVIEWER";
        } catch (Exception e) {
            logger.error("获取用户角色时发生异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
            return null;
        } catch (Exception e) {
            logger.error("获取请求对象失败");
            return null;
        }
    }
} 