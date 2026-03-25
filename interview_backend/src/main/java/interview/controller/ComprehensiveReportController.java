package interview.controller;

import interview.model.ComprehensiveReport;
import interview.service.ComprehensiveReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comprehensive-report")
public class ComprehensiveReportController {

    private static final Logger log = LoggerFactory.getLogger(ComprehensiveReportController.class);

    @Autowired
    private ComprehensiveReportService comprehensiveReportService;


    /**
     * 1. 获取表情分析数据
     */
    @GetMapping("/expression/{sessionId}")
    public Map<String, Object> getExpressionAnalysis(@PathVariable Long sessionId) {
        log.info("查询表情分析数据 sessionId={}", sessionId);
        List<Map<String, Object>> result = new ArrayList<>();
        comprehensiveReportService.getExpressionRecords(sessionId).forEach(r -> {
            Map<String, Object> item = new HashMap<>();
            item.put("expressionName", r.getExpressionName());
            item.put("count", r.getCount());
            result.add(item);
        });
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", result);
        return resp;
    }

    /**
     * 2. 获取STAR结构分数
     */
    @GetMapping("/star/{sessionId}")
    public Map<String, Object> getStarScores(@PathVariable Long sessionId) {
        log.info("查询STAR结构分数 sessionId={}", sessionId);
        Map<String, Object> data = comprehensiveReportService.getStarData(sessionId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return resp;
    }

    /**
     * 3. 获取问题回答质量分析
     */
    @GetMapping("/answer-quality/{sessionId}")
    public Map<String, Object> getAnswerQuality(@PathVariable Long sessionId) {
        log.info("查询问题回答质量分析 sessionId={}", sessionId);
        Map<String, Object> data = comprehensiveReportService.getAnswerQuality(sessionId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return resp;
    }

    /**
     * 4. 获取能力评估矩阵数据（demo）
     */
    @GetMapping("/ability-matrix/{sessionId}")
    public Map<String, Object> getAbilityMatrix(@PathVariable Long sessionId) {
        log.info("查询能力评估矩阵数据 sessionId={}", sessionId);
        Map<String, Object> data = comprehensiveReportService.getAbilityMatrix(sessionId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return resp;
    }

    /**
     * 5. 获取用户面试报告列表（支持分页、搜索、排序、筛选）
     */
    @GetMapping("/list")
    public Map<String, Object> getReportList(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(required = false) String searchKeyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String filterType) {

        log.info("查询报告列表 userId={}, page={}, size={}, sortBy={}, searchKeyword={}, filterType={}",
                userId, page, size, sortBy, searchKeyword, filterType);

        try {
            Page<ComprehensiveReport> reportPage = comprehensiveReportService.getReportList(
                    userId, page, size, sortBy, searchKeyword, startTime, endTime, filterType);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "success");
            resp.put("data", reportPage);
            return resp;

        } catch (Exception e) {
            log.error("查询报告列表失败 userId={}", userId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "查询报告列表失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 6. 获取用户统计数据
     */
    @GetMapping("/stats")
    public Map<String, Object> getUserStats(@RequestParam Long userId) {
        log.info("查询用户统计数据 userId={}", userId);

        try {
            Map<String, Object> stats = comprehensiveReportService.getUserStats(userId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "success");
            resp.put("data", stats);
            return resp;

        } catch (Exception e) {
            log.error("查询用户统计数据失败 userId={}", userId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "查询统计数据失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 7. 创建或更新面试开始时间
     */
    @PostMapping("/start-time")
    public Map<String, Object> updateStartTime(@RequestBody Map<String, Object> request) {
        log.info("更新面试开始时间请求: {}", request);

        try {
            Long sessionId = Long.valueOf(request.get("sessionId").toString());
            Long userId = Long.valueOf(request.get("userId").toString());
            Integer jobId = Integer.valueOf(request.get("jobId").toString());
            String companyName = (String) request.get("companyName");
            String positionName = (String) request.get("positionName");

            ComprehensiveReport report = comprehensiveReportService.createOrUpdateStartTime(
                    sessionId, userId, jobId, companyName, positionName);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "面试开始时间更新成功");
            resp.put("data", report);
            return resp;

        } catch (Exception e) {
            log.error("更新面试开始时间失败", e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "更新面试开始时间失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 8. 更新面试结束时间并计算时长
     */
    @PostMapping("/end-time")
    public Map<String, Object> updateEndTime(@RequestBody Map<String, Object> request) {
        log.info("更新面试结束时间请求: {}", request);

        try {
            Long sessionId = Long.valueOf(request.get("sessionId").toString());

            ComprehensiveReport report = comprehensiveReportService.updateEndTimeAndDuration(sessionId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "面试结束时间更新成功");
            resp.put("data", report);
            return resp;

        } catch (Exception e) {
            log.error("更新面试结束时间失败", e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "更新面试结束时间失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 9. 根据会话ID获取综合报告
     */
    @GetMapping("/session/{sessionId}")
    public Map<String, Object> getReportBySessionId(@PathVariable Long sessionId) {
        log.info("根据会话ID获取综合报告 sessionId={}", sessionId);

        try {
            var reportOpt = comprehensiveReportService.getBySessionId(sessionId);

            Map<String, Object> resp = new HashMap<>();
            if (reportOpt.isPresent()) {
                resp.put("code", 0);
                resp.put("msg", "查询成功");
                resp.put("data", reportOpt.get());
            } else {
                resp.put("code", 404);
                resp.put("msg", "未找到对应的综合报告");
                resp.put("data", null);
            }
            return resp;

        } catch (Exception e) {
            log.error("查询综合报告失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "查询综合报告失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 10. 生成并更新STAR分数
     */
    @PostMapping("/generate-star/{sessionId}")
    public Map<String, Object> generateStarScores(@PathVariable Long sessionId) {
        log.info("生成STAR分数请求 sessionId={}", sessionId);

        try {
            ComprehensiveReport report = comprehensiveReportService.generateAndUpdateStarScores(sessionId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "STAR分数生成成功");
            resp.put("data", report);
            return resp;

        } catch (Exception e) {
            log.error("生成STAR分数失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "生成STAR分数失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 11. 分析基础问答环节
     */
    @PostMapping("/analyze-basic-qa/{sessionId}")
    public Map<String, Object> analyzeBasicQA(@PathVariable Long sessionId, 
                                              @RequestParam Long userId) {
        log.info("分析基础问答环节请求 sessionId={}, userId={}", sessionId, userId);

        try {
            ComprehensiveReport report = comprehensiveReportService.analyzeAndUpdateBasicQA(sessionId, userId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "基础问答分析完成");
            resp.put("data", report);
            return resp;

        } catch (Exception e) {
            log.error("基础问答分析失败 sessionId={}, userId={}", sessionId, userId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "基础问答分析失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 12. 获取技术能力分析结果（基础问答 + 代码题）
     */
    @GetMapping("/tech-analysis/{sessionId}")
    public Map<String, Object> getTechAnalysis(@PathVariable Long sessionId) {
        log.info("获取技术能力分析结果 sessionId={}", sessionId);

        try {
            Map<String, Object> techAnalysis = comprehensiveReportService.getTechAnalysis(sessionId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "获取技术分析成功");
            resp.put("data", techAnalysis);
            return resp;

        } catch (Exception e) {
            log.error("获取技术分析失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "获取技术分析失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 13. 生成能力评估矩阵
     */
    @PostMapping("/generate-ability-matrix/{sessionId}")
    public Map<String, Object> generateAbilityMatrix(@PathVariable Long sessionId, @RequestBody Map<String, Object> requestBody) {
        boolean forceRegenerate = requestBody != null && Boolean.TRUE.equals(requestBody.get("forceRegenerate"));
        log.info("生成能力评估矩阵 sessionId={}, forceRegenerate={}", sessionId, forceRegenerate);

        try {
            // 这里暂时使用固定的userId，实际应该从请求中获取或从token中解析
            // TODO: 从认证信息中获取真实的userId
            Long userId = 1L; // 临时方案

            ComprehensiveReport report = comprehensiveReportService.generateAndUpdateAbilityMatrix(sessionId, userId, forceRegenerate);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "能力评估矩阵生成成功");
            resp.put("data", report);
            return resp;

        } catch (Exception e) {
            log.error("生成能力评估矩阵失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "生成能力评估矩阵失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 14. 检查能力评估矩阵是否已生成
     */
    @GetMapping("/ability-matrix-status/{sessionId}")
    public Map<String, Object> checkAbilityMatrixStatus(@PathVariable Long sessionId) {
        log.info("检查能力评估矩阵状态 sessionId={}", sessionId);

        try {
            boolean isGenerated = comprehensiveReportService.isAbilityMatrixGenerated(sessionId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "获取状态成功");
            resp.put("data", Map.of("isGenerated", isGenerated));
            return resp;

        } catch (Exception e) {
            log.error("检查能力评估矩阵状态失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "检查状态失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 15. 生成综合分析简报
     */
    @PostMapping("/generate-comprehensive-analysis/{sessionId}")
    public Map<String, Object> generateComprehensiveAnalysis(@PathVariable Long sessionId, @RequestBody Map<String, Object> requestBody) {
        boolean forceRegenerate = requestBody != null && Boolean.TRUE.equals(requestBody.get("forceRegenerate"));
        log.info("生成综合分析简报 sessionId={}, forceRegenerate={}", sessionId, forceRegenerate);

        try {
            // 这里暂时使用固定的userId，实际应该从请求中获取或从token中解析
            // TODO: 从认证信息中获取真实的userId
            Long userId = 1L; // 临时方案

            ComprehensiveReport report = comprehensiveReportService.generateAndUpdateComprehensiveAnalysis(sessionId, userId, forceRegenerate);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "综合分析简报生成成功");
            resp.put("data", report);
            return resp;

        } catch (Exception e) {
            log.error("生成综合分析简报失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "生成综合分析简报失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 16. 检查综合分析简报是否已生成
     */
    @GetMapping("/comprehensive-analysis-status/{sessionId}")
    public Map<String, Object> checkComprehensiveAnalysisStatus(@PathVariable Long sessionId) {
        log.info("检查综合分析简报状态 sessionId={}", sessionId);

        try {
            boolean isGenerated = comprehensiveReportService.isComprehensiveAnalysisGenerated(sessionId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "获取状态成功");
            resp.put("data", Map.of("isGenerated", isGenerated));
            return resp;

        } catch (Exception e) {
            log.error("检查综合分析简报状态失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "检查状态失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 17. 获取综合分析简报数据
     */
    @GetMapping("/comprehensive-analysis/{sessionId}")
    public Map<String, Object> getComprehensiveAnalysis(@PathVariable Long sessionId) {
        log.info("获取综合分析简报数据 sessionId={}", sessionId);

        try {
            Map<String, Object> analysisData = comprehensiveReportService.getComprehensiveAnalysis(sessionId);
            
            log.info("Service层返回的分析数据 sessionId={}, analysisData是否为null={}, 数据内容={}", 
                sessionId, analysisData == null, analysisData);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "获取成功");
            // 直接将analysisData作为data返回，而不是嵌套在comprehensive_analysis_data中
            resp.put("data", analysisData);
            
            log.info("Controller最终返回给前端的数据结构 sessionId={}, response={}", sessionId, resp);
            return resp;

        } catch (Exception e) {
            log.error("获取综合分析简报数据失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "获取失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 18. 生成个性化建议
     */
    @PostMapping("/generate-personalized-suggestions/{sessionId}")
    public Map<String, Object> generatePersonalizedSuggestions(@PathVariable Long sessionId, @RequestBody Map<String, Object> requestBody) {
        boolean forceRegenerate = requestBody != null && Boolean.TRUE.equals(requestBody.get("forceRegenerate"));
        log.info("生成个性化建议 sessionId={}, forceRegenerate={}", sessionId, forceRegenerate);

        try {
            // 这里暂时使用固定的userId，实际应该从请求中获取或从token中解析
            // TODO: 从认证信息中获取真实的userId
            Long userId = 1L; // 临时方案

            ComprehensiveReport report = comprehensiveReportService.generateAndUpdatePersonalizedSuggestions(sessionId, userId, forceRegenerate);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "个性化建议生成成功");
            resp.put("data", report);
            return resp;

        } catch (Exception e) {
            log.error("生成个性化建议失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "生成个性化建议失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 19. 获取个性化建议数据
     */
    @GetMapping("/personalized-suggestions/{sessionId}")
    public Map<String, Object> getPersonalizedSuggestions(@PathVariable Long sessionId) {
        log.info("获取个性化建议数据 sessionId={}", sessionId);

        try {
            Map<String, Object> suggestionsData = comprehensiveReportService.getPersonalizedSuggestions(sessionId);
            
            log.info("Service层返回的个性化建议数据 sessionId={}, suggestionsData是否为null={}, 数据内容={}", 
                sessionId, suggestionsData == null, suggestionsData);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "获取成功");
            resp.put("data", suggestionsData);
            
            log.info("Controller最终返回给前端的个性化建议数据结构 sessionId={}, response={}", sessionId, resp);
            return resp;

        } catch (Exception e) {
            log.error("获取个性化建议数据失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "获取失败: " + e.getMessage());
            return resp;
        }
    }

    /**
     * 20. 检查个性化建议是否已生成
     */
    @GetMapping("/personalized-suggestions-status/{sessionId}")
    public Map<String, Object> checkPersonalizedSuggestionsStatus(@PathVariable Long sessionId) {
        log.info("检查个性化建议状态 sessionId={}", sessionId);

        try {
            boolean isGenerated = comprehensiveReportService.isPersonalizedSuggestionsGenerated(sessionId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "获取状态成功");
            resp.put("data", Map.of("isGenerated", isGenerated));
            return resp;

        } catch (Exception e) {
            log.error("检查个性化建议状态失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "检查状态失败: " + e.getMessage());
            return resp;
        }
    }
}

// 添加另一个控制器来支持复数形式的URL路径
@RestController
@RequestMapping("/api/comprehensive-reports")
class ComprehensiveReportsController {
    
    private static final Logger log = LoggerFactory.getLogger(ComprehensiveReportsController.class);

    @Autowired
    private ComprehensiveReportService comprehensiveReportService;

    /**
     * 检查面试报告是否存在
     */
    @GetMapping("/check/{sessionId}")
    public Map<String, Object> checkReportExists(@PathVariable Long sessionId) {
        log.info("检查面试报告是否存在 sessionId={}", sessionId);

        try {
            var reportOpt = comprehensiveReportService.getBySessionId(sessionId);
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 0);
            resp.put("msg", "检查成功");
            resp.put("exists", reportOpt.isPresent());
            if (reportOpt.isPresent()) {
                resp.put("data", reportOpt.get());
            }
            return resp;

        } catch (Exception e) {
            log.error("检查面试报告失败 sessionId={}", sessionId, e);
            Map<String, Object> resp = new HashMap<>();
            resp.put("code", 500);
            resp.put("msg", "检查面试报告失败: " + e.getMessage());
            resp.put("exists", false);
            return resp;
        }
    }
} 