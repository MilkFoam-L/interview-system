package interview.service;

import interview.dto.InterviewStatsDTO;
import interview.model.InterviewStatistics;

public interface InterviewStatisticsService {
    
    /**
     * 获取用户的面试统计数据
     * @param userId 用户ID
     * @return 面试统计数据
     */
    InterviewStatsDTO getUserStatistics(Long userId);
    
    /**
     * 更新用户的面试统计数据
     * @param userId 用户ID
     * @return 更新后的面试统计数据
     */
    InterviewStatistics updateUserStatistics(Long userId);
    
    /**
     * 清空用户的面试统计数据
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean clearUserStatistics(Long userId);
} 