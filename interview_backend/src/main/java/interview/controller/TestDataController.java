package interview.controller;

import interview.model.ApplicationRecord;
import interview.repository.ApplicationRecordRepository;
import interview.service.InterviewStatisticsService;
import interview.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 测试数据控制器
 * 仅用于开发和测试环境
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test-data")
public class TestDataController {

    private final ApplicationRecordRepository applicationRecordRepository;
    private final InterviewStatisticsService statisticsService;
    private final UserContextUtil userContextUtil;
    private final Random random = new Random();

    /**
     * 为当前用户生成测试数据
     * @return 生成的数据统计
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateTestData() {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("为用户ID={}生成测试数据", userId);
        
        // 清除用户现有的申请记录
        applicationRecordRepository.deleteAll(applicationRecordRepository.findByUserId(userId));
        
        int submittedCount = 0;
        int pendingCount = 0;
        int completedCount = 0;
        int passedCount = 0;
        
        // 生成8-15条随机申请记录
        int totalRecords = 8 + random.nextInt(8);
        
        for (int i = 1; i <= totalRecords; i++) {
            ApplicationRecord record = new ApplicationRecord();
            record.setUserId(userId);
            record.setJobId(1 + random.nextInt(10)); // 随机职位ID (1-10)
            record.setSubmitTime(LocalDateTime.now().minusDays(random.nextInt(30))); // 过去30天内的随机时间
            
            // 随机状态
            String status;
            int statusRandom = random.nextInt(10);
            
            if (statusRandom < 3) { // 30%概率为已投递
                status = "SUBMITTED";
                submittedCount++;
            } else if (statusRandom < 6) { // 30%概率为待面试
                status = "PENDING";
                pendingCount++;
            } else { // 40%概率为已完成
                status = "COMPLETED";
                record.setCompleteTime(LocalDateTime.now().minusDays(random.nextInt(14))); // 过去14天内
                
                // 60%概率通过
                if (random.nextInt(10) < 6) {
                    record.setResult("PASS");
                    passedCount++;
                } else {
                    record.setResult("FAIL");
                }
                
                completedCount++;
            }
            
            record.setStatus(status);
            applicationRecordRepository.save(record);
        }
        
        // 更新统计数据
        statisticsService.updateUserStatistics(userId);
        
        // 返回生成的数据统计
        Map<String, Object> result = new HashMap<>();
        result.put("totalRecords", totalRecords);
        result.put("submittedCount", submittedCount);
        result.put("pendingCount", pendingCount);
        result.put("completedCount", completedCount);
        result.put("passedCount", passedCount);
        
        return ResponseEntity.ok(result);
    }
} 