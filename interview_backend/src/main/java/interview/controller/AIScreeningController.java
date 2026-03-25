package interview.controller;

import interview.service.AIScreeningService;
import interview.util.UserContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/ai-screening")
public class AIScreeningController {

    @Autowired
    private AIScreeningService aiScreeningService;

    @Autowired
    private UserContextUtil userContextUtil;

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> data = aiScreeningService.queryCandidates(keyword, position, status, page, size);
        return ResponseEntity.ok(data);
    }

    @PutMapping("/{applicationId}/notify")
    @Transactional
    public ResponseEntity<Void> notifyPass(@PathVariable Long applicationId) {
        Long operator = userContextUtil.getCurrentUserId();
        aiScreeningService.notifyPass(applicationId, operator, LocalDateTime.now());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{applicationId}/reject")
    @Transactional
    public ResponseEntity<Void> reject(@PathVariable Long applicationId, @RequestBody Map<String, Object> payload) {
        Long operator = userContextUtil.getCurrentUserId();
        String reason = payload == null ? null : String.valueOf(payload.get("reason"));
        if (reason == null || reason.trim().isEmpty() || "null".equalsIgnoreCase(reason)) {
            return ResponseEntity.badRequest().build();
        }
        aiScreeningService.reject(applicationId, reason, operator);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch/notify")
    @Transactional
    public ResponseEntity<Void> batchNotify(@RequestBody Map<String, Object> payload) {
        Long operator = userContextUtil.getCurrentUserId();
        Object raw = payload == null ? null : payload.get("ids");
        if (!(raw instanceof List<?>)) {
            return ResponseEntity.badRequest().build();
        }
        @SuppressWarnings("unchecked")
        List<?> ids = (List<?>) raw;
        List<Long> appIds = ids.stream()
                .filter(Objects::nonNull)
                .map(o -> ((Number) o).longValue())
                .collect(java.util.stream.Collectors.toList());
        if (appIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        aiScreeningService.batchNotifyPass(appIds, operator, LocalDateTime.now());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch/reject")
    @Transactional
    public ResponseEntity<Void> batchReject(@RequestBody Map<String, Object> payload) {
        Long operator = userContextUtil.getCurrentUserId();
        Object raw = payload == null ? null : payload.get("ids");
        if (!(raw instanceof List<?>)) {
            return ResponseEntity.badRequest().build();
        }
        String reason = payload == null ? null : String.valueOf(payload.get("reason"));
        if (reason == null || reason.trim().isEmpty() || "null".equalsIgnoreCase(reason)) {
            return ResponseEntity.badRequest().build();
        }
        @SuppressWarnings("unchecked")
        List<?> ids = (List<?>) raw;
        List<Long> appIds = ids.stream()
                .filter(Objects::nonNull)
                .map(o -> ((Number) o).longValue())
                .collect(java.util.stream.Collectors.toList());
        if (appIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        aiScreeningService.batchReject(appIds, reason, operator);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{applicationId}/report")
    public ResponseEntity<Map<String, Object>> report(@PathVariable Long applicationId) {
        Map<String, Object> report = aiScreeningService.getLatestReportByApplicationId(applicationId);
        return ResponseEntity.ok(report);
    }
} 