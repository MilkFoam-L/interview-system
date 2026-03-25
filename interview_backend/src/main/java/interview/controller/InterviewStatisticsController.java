package interview.controller;

import interview.dto.InterviewStatsDTO;
import interview.model.ApplicationRecord;
import interview.model.InterviewStatistics;
import interview.repository.ApplicationRecordRepository;
import interview.repository.InterviewStatisticsRepository;
import interview.service.InterviewStatisticsService;
import interview.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
public class InterviewStatisticsController {

    private final InterviewStatisticsService statisticsService;
    private final ApplicationRecordRepository applicationRecordRepository;
    private final InterviewStatisticsRepository statisticsRepository;
    private final UserContextUtil userContextUtil;

    /**
     * 获取当前登录用户的面试统计数据
     * @return 面试统计数据
     */
    @GetMapping("/interview")
    public ResponseEntity<InterviewStatsDTO> getUserInterviewStatistics() {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("获取用户ID={}的面试统计数据", userId);
        
        // 先输出当前用户的所有申请记录状态分布，用于调试
        try {
            List<Object[]> statusCounts = applicationRecordRepository.countByStatusForUser(userId);
            log.info("用户ID={}的申请记录状态分布:", userId);
            for (Object[] row : statusCounts) {
                String status = (String) row[0];
                Long count = (Long) row[1];
                log.info("  状态 {} 的记录数: {}", status, count);
            }
        } catch (Exception e) {
            log.warn("获取用户申请记录状态分布失败:", e);
        }
        
        InterviewStatsDTO stats = statisticsService.getUserStatistics(userId);
        log.info("返回用户ID={}的统计数据: 已投递={}, 待面试={}, 已完成={}, 通过率={}%", 
                userId, stats.getSubmittedCount(), stats.getPendingCount(), 
                stats.getCompletedCount(), stats.getPassRate());
        return ResponseEntity.ok(stats);
    }

    /**
     * 手动更新当前登录用户的面试统计数据
     * @return 更新后的面试统计数据
     */
    @GetMapping("/interview/refresh")
    public ResponseEntity<InterviewStatsDTO> refreshUserInterviewStatistics() {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("刷新用户ID={}的面试统计数据", userId);
        statisticsService.updateUserStatistics(userId);
        InterviewStatsDTO stats = statisticsService.getUserStatistics(userId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * 清空当前登录用户的面试统计数据
     * 简化版本直接设置为0
     * @return 操作结果
     */
    @PostMapping("/interview/clear")
    @Transactional
    public ResponseEntity<Map<String, Object>> clearUserInterviewStatistics() {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("清空用户ID={}的面试统计数据", userId);
        
        try {
            // 直接修改统计数据为0
            Optional<InterviewStatistics> statisticsOpt = statisticsRepository.findByUserId(userId);
            
            if (statisticsOpt.isPresent()) {
                InterviewStatistics statistics = statisticsOpt.get();
                statistics.setSubmittedCount(0);
                statistics.setPendingCount(0);
                statistics.setCompletedCount(0);
                statistics.setPassedCount(0);
                statistics.setPassRate(BigDecimal.ZERO);
                statistics.setLastUpdated(LocalDateTime.now());
                
                statisticsRepository.save(statistics);
                log.info("已将用户ID={}的统计数据重置为0", userId);
            } else {
                log.info("用户ID={}没有统计数据记录", userId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "统计数据已重置为0");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("清空用户ID={}的统计数据时出错: {}", userId, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "清空数据失败: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }
} 