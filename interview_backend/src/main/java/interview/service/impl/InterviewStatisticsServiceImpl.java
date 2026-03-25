package interview.service.impl;

import interview.dto.InterviewStatsDTO;
import interview.model.InterviewStatistics;
import interview.model.ApplicationRecord;
import interview.repository.ApplicationRecordRepository;
import interview.repository.InterviewStatisticsRepository;
import interview.service.InterviewStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewStatisticsServiceImpl implements InterviewStatisticsService {

    private final ApplicationRecordRepository applicationRecordRepository;
    private final InterviewStatisticsRepository statisticsRepository;

    @Override
    public InterviewStatsDTO getUserStatistics(Long userId) {
        log.info("获取用户ID={}的统计数据", userId);
        
        try {
            // 确保userId不为空
            if (userId == null) {
                log.warn("用户ID为空，返回默认统计数据");
                return createDefaultStats();
            }
            
            // 首先尝试从缓存表中获取
            Optional<InterviewStatistics> statisticsOpt = statisticsRepository.findByUserId(userId);
            
            // 如果存在且最近更新，直接返回
            if (statisticsOpt.isPresent()) {
                InterviewStatistics stats = statisticsOpt.get();
                log.info("从缓存获取用户ID={}的统计数据: 已投递={}, 待面试={}, 已完成={}, 通过率={}%", 
                        userId, stats.getSubmittedCount(), stats.getPendingCount(), 
                        stats.getCompletedCount(), stats.getPassRate());
                        
                return InterviewStatsDTO.builder()
                        .notSubmittedCount(0) // 未投递状态固定为0
                        .submittedCount(stats.getSubmittedCount() != null ? stats.getSubmittedCount() : 0)
                        .pendingCount(stats.getPendingCount() != null ? stats.getPendingCount() : 0)
                        .completedCount(stats.getCompletedCount() != null ? stats.getCompletedCount() : 0)
                        .passRate(stats.getPassRate() != null ? stats.getPassRate() : BigDecimal.ZERO)
                        .build();
            }
            
            log.info("缓存中没有找到用户ID={}的统计数据，重新计算", userId);
            // 否则重新计算并更新缓存
            try {
                InterviewStatistics updatedStats = updateUserStatistics(userId);
                return InterviewStatsDTO.builder()
                        .notSubmittedCount(0) // 未投递状态固定为0
                        .submittedCount(updatedStats.getSubmittedCount() != null ? updatedStats.getSubmittedCount() : 0)
                        .pendingCount(updatedStats.getPendingCount() != null ? updatedStats.getPendingCount() : 0)
                        .completedCount(updatedStats.getCompletedCount() != null ? updatedStats.getCompletedCount() : 0)
                        .passRate(updatedStats.getPassRate() != null ? updatedStats.getPassRate() : BigDecimal.ZERO)
                        .build();
            } catch (Exception e) {
                log.error("更新用户统计数据失败，返回默认值", e);
                return createDefaultStats();
            }
        } catch (Exception e) {
            log.error("获取用户统计数据时发生异常", e);
            return createDefaultStats();
        }
    }

    @Override
    @Transactional
    public InterviewStatistics updateUserStatistics(Long userId) {
        log.info("开始更新用户ID={}的面试统计数据", userId);
        
        try {
            // 确保userId不为空
            if (userId == null) {
                log.warn("用户ID为空，无法更新统计数据");
                throw new IllegalArgumentException("用户ID不能为空");
            }
            
            // 获取所有申请记录
            List<ApplicationRecord> allRecords = applicationRecordRepository.findByUserId(userId);
            log.info("用户ID={}的申请记录总数: {}", userId, allRecords != null ? allRecords.size() : 0);
            
            // 如果没有记录，创建一个空的统计记录
            if (allRecords == null || allRecords.isEmpty()) {
                log.info("用户ID={}没有申请记录，创建空统计数据", userId);
                return createEmptyStatistics(userId);
            }
            
            // 先输出当前用户的所有申请记录状态分布，用于调试
            List<Object[]> statusCounts = null;
            try {
                statusCounts = applicationRecordRepository.countByStatusForUser(userId);
                if (statusCounts != null) {
                    log.info("用户ID={}的申请记录状态分布:", userId);
                    for (Object[] row : statusCounts) {
                        if (row != null && row.length >= 2) {
                            String status = row[0] != null ? row[0].toString() : "null";
                            Long count = row[1] != null ? Long.valueOf(row[1].toString()) : 0L;
                            log.info("  状态 {} 的记录数: {}", status, count);
                        }
                    }
                } else {
                    log.warn("countByStatusForUser返回null");
                }
            } catch (Exception e) {
                log.warn("获取用户申请记录状态分布失败:", e);
            }
            
            // 计算已投递的记录数
            Integer submittedCount = 0;
            try {
                submittedCount = applicationRecordRepository.countSubmittedByUserId(userId);
                log.info("用户ID={}的已投递数量: {}", userId, submittedCount);
            } catch (Exception e) {
                log.warn("计算已投递数量失败，使用默认值0", e);
            }
            
            // 确保submittedCount不为null
            submittedCount = submittedCount != null ? submittedCount : 0;
            
            // 改进：待面试数量计算
            Integer pendingCount = 0;
            
            try {
                // 直接从所有记录中筛选待面试的记录
                List<ApplicationRecord> pendingRecords = allRecords.stream()
                    .filter(record -> {
                        if (record == null) {
                            return false;
                        }
                        
                        // 条件: 状态为PENDING
                        return "PENDING".equals(record.getStatus());
                    })
                    .collect(Collectors.toList());
                
                pendingCount = pendingRecords.size();
                log.info("计算得到的待面试数量: {}", pendingCount);
            } catch (Exception e) {
                log.warn("从记录中计算待面试数量失败", e);
            }
            
            if (pendingCount == 0) {
                // 兜底：如果计算结果为0，尝试直接从数据库中查询
                try {
                    pendingCount = applicationRecordRepository.countPendingByUserId(userId);
                    log.info("通过repository方法查询到的待面试数量: {}", pendingCount);
                } catch (Exception e) {
                    log.warn("通过repository查询待面试数量失败", e);
                }
            }
            
            // 确保pendingCount不为null
            pendingCount = pendingCount != null ? pendingCount : 0;
            
            // 计算已完成的记录数
            Integer completedCount = 0;
            try {
                completedCount = applicationRecordRepository.countCompletedByUserId(userId);
                log.info("用户ID={}的已完成数量: {}", userId, completedCount);
            } catch (Exception e) {
                log.warn("计算已完成数量失败，使用默认值0", e);
            }
            
            // 确保completedCount不为null
            completedCount = completedCount != null ? completedCount : 0;
            
            // 计算已通过的记录数
            Integer passedCount = 0;
            try {
                passedCount = applicationRecordRepository.countPassedByUserId(userId);
                log.info("用户ID={}的已通过数量: {}", userId, passedCount);
            } catch (Exception e) {
                log.warn("计算已通过数量失败，使用默认值0", e);
            }
            
            // 确保passedCount不为null
            passedCount = passedCount != null ? passedCount : 0;
            
            // 计算通过率
            BigDecimal passRate = BigDecimal.ZERO;
            if (completedCount > 0) {
                try {
                    passRate = BigDecimal.valueOf(passedCount)
                            .divide(BigDecimal.valueOf(completedCount), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);
                } catch (Exception e) {
                    log.warn("计算通过率失败，使用默认值0", e);
                }
            }
            log.info("用户ID={}的通过率: {}%", userId, passRate);
            
            // 查找或创建统计记录
            InterviewStatistics statistics = null;
            try {
                Optional<InterviewStatistics> statsOpt = statisticsRepository.findByUserId(userId);
                if (statsOpt.isPresent()) {
                    statistics = statsOpt.get();
                } else {
                    statistics = new InterviewStatistics();
                    statistics.setUserId(userId);
                }
            } catch (Exception e) {
                log.warn("查找统计记录失败，创建新记录", e);
                statistics = new InterviewStatistics();
                statistics.setUserId(userId);
            }
            
            // 更新统计数据
            statistics.setSubmittedCount(submittedCount);
            statistics.setPendingCount(pendingCount);
            statistics.setCompletedCount(completedCount);
            statistics.setPassedCount(passedCount);
            statistics.setPassRate(passRate);
            statistics.setLastUpdated(LocalDateTime.now());
            
            // 保存更新
            try {
                InterviewStatistics saved = statisticsRepository.save(statistics);
                log.info("已更新用户ID={}的统计数据: 已投递={}, 待面试={}, 已完成={}, 通过率={}%", 
                        userId, saved.getSubmittedCount(), saved.getPendingCount(), 
                        saved.getCompletedCount(), saved.getPassRate());
                return saved;
            } catch (Exception e) {
                log.error("保存统计数据失败", e);
                // 如果保存失败，返回未保存的对象
                return statistics;
            }
        } catch (Exception e) {
            log.error("更新用户ID={}的统计数据时出错: {}", userId, e.getMessage(), e);
            // 出错时返回一个空的统计对象，避免500错误
            return createEmptyStatistics(userId);
        }
    }
    
    @Override
    @Transactional
    public boolean clearUserStatistics(Long userId) {
        log.info("开始清空用户ID={}的面试统计数据", userId);
        
        try {
            // 查找是否存在统计记录
            Optional<InterviewStatistics> statisticsOpt = statisticsRepository.findByUserId(userId);
            
            if (statisticsOpt.isPresent()) {
                log.info("找到用户ID={}的统计记录，准备删除", userId);
                // 直接删除整条记录，而不是更新为0
                statisticsRepository.delete(statisticsOpt.get());
                log.info("已删除用户ID={}的统计记录", userId);
                
                // 创建一个新的全0记录
                InterviewStatistics statistics = new InterviewStatistics();
                statistics.setUserId(userId);
                statistics.setSubmittedCount(0);
                statistics.setPendingCount(0);
                statistics.setCompletedCount(0);
                statistics.setPassedCount(0);
                statistics.setPassRate(BigDecimal.ZERO);
                statistics.setLastUpdated(LocalDateTime.now());
                
                statisticsRepository.save(statistics);
                log.info("已为用户ID={}创建全新的清空统计记录", userId);
                return true;
            } else {
                // 如果不存在，创建一个新的全0记录
                log.info("未找到用户ID={}的统计记录，创建新记录", userId);
                InterviewStatistics statistics = new InterviewStatistics();
                statistics.setUserId(userId);
                statistics.setSubmittedCount(0);
                statistics.setPendingCount(0);
                statistics.setCompletedCount(0);
                statistics.setPassedCount(0);
                statistics.setPassRate(BigDecimal.ZERO);
                statistics.setLastUpdated(LocalDateTime.now());
                
                statisticsRepository.save(statistics);
                log.info("已为用户ID={}创建清空的统计数据", userId);
                return true;
            }
        } catch (Exception e) {
            log.error("清空用户ID={}的统计数据时出错: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建一个默认的统计数据DTO对象
     * @return 默认的统计数据DTO
     */
    private InterviewStatsDTO createDefaultStats() {
        return InterviewStatsDTO.builder()
                .notSubmittedCount(0)
                .submittedCount(0)
                .pendingCount(0)
                .completedCount(0)
                .passRate(BigDecimal.ZERO)
                .build();
    }

    /**
     * 为用户创建一个空的统计记录
     * @param userId 用户ID
     * @return 创建的统计记录
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InterviewStatistics createEmptyStatistics(Long userId) {
        log.info("为用户ID={}创建一个空的统计记录", userId);
        
        try {
            // 查找是否已存在记录
            Optional<InterviewStatistics> existingStatsOpt = statisticsRepository.findByUserId(userId);
            
            InterviewStatistics statistics;
            if (existingStatsOpt.isPresent()) {
                // 如果记录已存在，直接使用现有记录并更新其值
                statistics = existingStatsOpt.get();
                log.info("用户ID={}已存在统计记录，更新为空值", userId);
            } else {
                // 如果记录不存在，创建新记录
                statistics = new InterviewStatistics();
                statistics.setUserId(userId);
                log.info("用户ID={}不存在统计记录，创建新记录", userId);
            }
            
            // 设置所有统计值为0
            statistics.setSubmittedCount(0);
            statistics.setPendingCount(0);
            statistics.setCompletedCount(0);
            statistics.setPassedCount(0);
            statistics.setPassRate(BigDecimal.ZERO);
            statistics.setLastUpdated(LocalDateTime.now());
            
            // 保存或更新记录
            return statisticsRepository.save(statistics);
        } catch (Exception e) {
            log.error("创建空统计数据失败", e);
            // 如果保存失败，返回一个未持久化的对象
            InterviewStatistics statistics = new InterviewStatistics();
            statistics.setUserId(userId);
            statistics.setSubmittedCount(0);
            statistics.setPendingCount(0);
            statistics.setCompletedCount(0);
            statistics.setPassedCount(0);
            statistics.setPassRate(BigDecimal.ZERO);
            statistics.setLastUpdated(LocalDateTime.now());
            return statistics;
        }
    }
} 