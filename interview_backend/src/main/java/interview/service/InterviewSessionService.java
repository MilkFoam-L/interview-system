package interview.service;

import interview.model.entity.InterviewSession;

import java.util.List;

public interface InterviewSessionService {
    
    /**
     * 创建新的面试会话，使用指定的用户ID
     * @param jobId 岗位ID
     * @param applicationId 申请记录ID (可选)
     * @param userId 用户ID
     * @return 创建的面试会话
     */
    InterviewSession createSessionWithUserId(Integer jobId, Long applicationId, Long userId);
    
    /**
     * 根据ID获取面试会话
     * @param id 面试会话ID
     * @return 面试会话信息
     */
    InterviewSession getSession(Long id);
    
    /**
     * 更新面试会话状态
     * @param id 面试会话ID
     * @param status 新状态
     * @return 更新后的面试会话
     */
    InterviewSession updateSessionStatus(Long id, String status);
    
    /**
     * 获取用户的所有面试会话
     * @param userId 用户ID
     * @return 面试会话列表
     */
    List<InterviewSession> getUserSessions(Long userId);
} 