package interview.controller;

import interview.model.JobApplication;
import interview.service.JobApplicationService;
import interview.util.UserContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 简历投递控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/job-applications")
public class JobApplicationController {
    
    @Autowired
    private JobApplicationService jobApplicationService;
    
    @Autowired
    private UserContextUtil userContextUtil;
    
    /**
     * 投递简历
     */
    @PostMapping
    public ResponseEntity<JobApplication> applyJob(@RequestBody Map<String, Object> payload) {
        Long userId = userContextUtil.getCurrentUserId();
        Integer jobId = Integer.valueOf(payload.get("jobId").toString());
        Long resumeId = payload.get("resumeId") != null ? Long.valueOf(payload.get("resumeId").toString()) : null;
        
        log.info("用户投递简历: 用户={}, 岗位={}, 简历={}", userId, jobId, resumeId);
        
        try {
            JobApplication application = jobApplicationService.applyJob(userId, jobId, resumeId);
            log.info("投递成功，申请ID={}", application.getId());
            return ResponseEntity.ok(application);
        } catch (IllegalStateException e) {
            log.warn("投递失败: {}", e.getMessage());
            return ResponseEntity.status(409).body(null);
        } catch (NoSuchElementException e) {
            log.error("投递失败: {}", e.getMessage());
            return ResponseEntity.status(404).body(null);
        }
    }
    
    /**
     * 获取当前用户的投递记录
     */
    @GetMapping("/user")
    public ResponseEntity<List<JobApplication>> getUserApplications(
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "X-User-ID", required = false) String xUserId) {
        
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
        
        // 如果还没有，尝试从上下文获取
        if (finalUserId == null) {
            finalUserId = userContextUtil.getCurrentUserId();
            log.info("从上下文获取到用户ID: {}", finalUserId);
        }
        
        log.info("获取用户ID={}的投递记录", finalUserId);
        
        // 如果仍然没有userId，返回400错误
        if (finalUserId == null) {
            log.error("获取投递记录失败: 未找到用户ID");
            return ResponseEntity.badRequest().body(null);
        }
        
        try {
            List<JobApplication> applications = jobApplicationService.getUserApplications(finalUserId);
            log.info("成功获取到{}条投递记录", applications.size());
            
            // 输出每条记录的状态，便于调试
            for (JobApplication app : applications) {
                log.info("投递记录 ID={}, 岗位ID={}, 状态={}", app.getId(), app.getJobId(), app.getStatus());
            }
            
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            log.error("获取用户ID={}的投递记录时发生错误: {}", finalUserId, e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }
    
    /**
     * 获取投递详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobApplication> getApplicationById(@PathVariable Long id) {
        JobApplication application = jobApplicationService.getApplicationById(id);
        return ResponseEntity.ok(application);
    }
    
    /**
     * 获取指定岗位的投递记录
     */
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<JobApplication>> getJobApplications(@PathVariable Integer jobId) {
        List<JobApplication> applications = jobApplicationService.getJobApplications(jobId);
        return ResponseEntity.ok(applications);
    }
    
    /**
     * 更新投递状态
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<JobApplication> updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> payload) {
        
        Long userId = userContextUtil.getCurrentUserId();
        Integer status = Integer.valueOf(payload.get("status").toString());
        String feedback = (String) payload.get("feedback");
        
        log.info("更新投递状态: 用户={}, 申请={}, 状态={}", userId, id, status);
        
        JobApplication application = jobApplicationService.updateApplicationStatus(id, status, feedback);
        
        return ResponseEntity.ok(application);
    }
    
    /**
     * 检查用户是否已投递某岗位
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkApplicationStatus(
            @RequestParam Integer jobId) {
        
        Long userId = userContextUtil.getCurrentUserId();
        List<JobApplication> applications = jobApplicationService.getUserApplications(userId);
        
        boolean hasApplied = applications.stream()
                .anyMatch(app -> app.getJobId().equals(jobId));
        
        Map<String, Object> result = new HashMap<>();
        result.put("applied", hasApplied);
        result.put("exists", hasApplied); // 添加exists字段，与applied保持一致
        
        if (hasApplied) {
            JobApplication application = applications.stream()
                    .filter(app -> app.getJobId().equals(jobId))
                    .findFirst()
                    .orElse(null);
            
            result.put("status", application != null ? application.getStatus() : null);
            result.put("applicationId", application != null ? application.getId() : null);
        }
        
        return ResponseEntity.ok(result);
    }
} 