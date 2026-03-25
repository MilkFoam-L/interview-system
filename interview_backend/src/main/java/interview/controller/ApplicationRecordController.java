package interview.controller;

import interview.model.ApplicationRecord;
import interview.repository.ApplicationRecordRepository;
import interview.service.InterviewStatisticsService;
import interview.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/applications")
public class ApplicationRecordController {

    private final ApplicationRecordRepository applicationRecordRepository;
    private final InterviewStatisticsService statisticsService;
    private final UserContextUtil userContextUtil;

    /**
     * 获取当前用户的所有申请记录
     * @return 申请记录列表
     */
    @GetMapping("/user")
    public ResponseEntity<List<Map<String, Object>>> getUserApplications(
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-ID", required = false) String xUserId,
            @RequestHeader(value = "userId", required = false) String headerUserId,
            HttpServletRequest request) {
        
        log.info("接收到获取用户申请记录请求");
        log.info("请求参数 userId = {}", userId);
        log.info("请求头 X-User-ID = {}", xUserId);
        log.info("请求头 userId = {}", headerUserId);
        
        // 尝试从各种来源获取用户ID
        Long finalUserId = userId;
        
        // 如果参数中没有userId，尝试从请求头获取
        if (finalUserId == null && xUserId != null && !xUserId.isEmpty()) {
            try {
                finalUserId = Long.parseLong(xUserId);
                log.info("从X-User-ID头获取到用户ID: {}", finalUserId);
            } catch (NumberFormatException e) {
                log.error("解析X-User-ID头失败: {}", xUserId, e);
            }
        }
        
        // 如果还没有，尝试从userId头获取
        if (finalUserId == null && headerUserId != null && !headerUserId.isEmpty()) {
            try {
                finalUserId = Long.parseLong(headerUserId);
                log.info("从userId头获取到用户ID: {}", finalUserId);
            } catch (NumberFormatException e) {
                log.error("解析userId头失败: {}", headerUserId, e);
            }
        }
        
        // 如果还没有，尝试从上下文获取
        if (finalUserId == null) {
            finalUserId = userContextUtil.getCurrentUserId();
            log.info("从上下文获取到用户ID: {}", finalUserId);
        }
        
        // 记录日志，包含最终使用的userId
        log.info("最终使用的用户ID = {}", finalUserId);
        
        // 如果仍然没有userId，返回400错误
        if (finalUserId == null) {
            log.error("获取申请记录失败: 未找到用户ID");
            return ResponseEntity.badRequest().body(null);
        }
        
        try {
            // 获取用户的申请记录
            List<ApplicationRecord> applications = applicationRecordRepository.findByUserId(finalUserId);
            log.info("成功获取到{}条申请记录", applications.size());
            
            // 将ApplicationRecord转换为简单的Map，避免懒加载问题
            List<Map<String, Object>> result = applications.stream().map(app -> {
                Map<String, Object> appMap = new HashMap<>();
                appMap.put("id", app.getId());
                appMap.put("userId", app.getUserId());
                appMap.put("jobId", app.getJobId());
                appMap.put("status", app.getStatus());
                appMap.put("result", app.getResult());
                appMap.put("submitTime", app.getSubmitTime());
                appMap.put("completeTime", app.getCompleteTime());
                appMap.put("feedback", app.getFeedback());
                appMap.put("createTime", app.getCreateTime());
                appMap.put("updateTime", app.getUpdateTime());
                
                // 不包含关联的用户和岗位对象，避免懒加载问题
                return appMap;
            }).collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取用户ID={}的申请记录时发生错误: {}", finalUserId, e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * 创建新的申请记录
     * @param application 申请记录
     * @return 创建的申请记录
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createApplication(@RequestBody ApplicationRecord application) {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("用户ID={}创建新申请记录: {}", userId, application);
        
        // 设置用户ID和提交时间
        application.setUserId(userId);
        application.setSubmitTime(LocalDateTime.now());
        
        // 直接将状态设置为待面试，匹配前端pending状态
        application.setStatus("PENDING");
        log.info("设置申请记录状态为PENDING");
        
        try {
            // 保存申请记录
            ApplicationRecord savedApplication = applicationRecordRepository.save(application);
            log.info("已创建申请记录: {}", savedApplication);
            
            // 输出所有申请记录的状态，用于调试
            List<Object[]> statusCounts = applicationRecordRepository.countByStatusForUser(userId);
            log.info("用户ID={}的申请记录状态分布:", userId);
            for (Object[] row : statusCounts) {
                String status = (String) row[0];
                Long count = (Long) row[1];
                log.info("  状态 {} 的记录数: {}", status, count);
            }
            
            // 更新统计数据
            statisticsService.updateUserStatistics(userId);
            log.info("已更新用户ID={}的统计数据", userId);
            
            // 返回成功响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "简历投递成功，已设置为已投递状态");
            response.put("application", savedApplication);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建申请记录失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "投递失败: " + e.getMessage());
            
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 更新申请记录状态
     * @param id 申请记录ID
     * @param status 新状态
     * @return 更新后的申请记录
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApplicationRecord> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String result) {
        
        Long userId = userContextUtil.getCurrentUserId();
        log.info("用户ID={}更新申请记录ID={}的状态为: {}", userId, id, status);
        
        ApplicationRecord application = applicationRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("申请记录不存在"));
        
        // 验证是否为当前用户的申请记录
        if (!application.getUserId().equals(userId)) {
            return ResponseEntity.badRequest().build();
        }
        
        // 更新状态
        application.setStatus(status);
        
        // 如果状态是已完成，设置完成时间和结果
        if ("COMPLETED".equals(status)) {
            application.setCompleteTime(LocalDateTime.now());
            if (result != null) {
                application.setResult(result);
            }
        }
        
        ApplicationRecord updatedApplication = applicationRecordRepository.save(application);
        return ResponseEntity.ok(updatedApplication);
    }
} 