package interview.controller;

import interview.service.RecruitmentAnalysisService;
import interview.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 招聘数据分析控制器
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruitment-analysis")
public class RecruitmentAnalysisController {

    private final UserContextUtil userContextUtil;

    @Autowired(required = false)
    private RecruitmentAnalysisService recruitmentAnalysisService;

    /**
     * KPI 概览数据
     */
    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> getKpis(
            @RequestParam(defaultValue = "30days") String timeRange
    ) {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("[RecruitmentAnalysis] 获取KPI概览, userId={}, timeRange={}", userId, timeRange);

        Map<String, Object> data = (recruitmentAnalysisService != null)
                ? recruitmentAnalysisService.getKpis(userId, timeRange)
                : Collections.emptyMap();

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * 时间线图（简历投递数量分析）
     */
    @GetMapping("/timeline")
    public ResponseEntity<Map<String, Object>> getTimeline(
            @RequestParam(defaultValue = "30days") String timeRange,
            @RequestParam(defaultValue = "all") String displayType
    ) {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("[RecruitmentAnalysis] 获取时间线数据, userId={}, timeRange={}, displayType={}", userId, timeRange, displayType);

        Map<String, Object> data = (recruitmentAnalysisService != null)
                ? recruitmentAnalysisService.getTimeline(userId, timeRange, displayType)
                : Collections.emptyMap();

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * 招聘漏斗
     */
    @GetMapping("/funnel")
    public ResponseEntity<Map<String, Object>> getFunnel(
            @RequestParam(defaultValue = "total") String period
    ) {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("[RecruitmentAnalysis] 获取漏斗数据, userId={}, period={}", userId, period);

        Map<String, Object> data = (recruitmentAnalysisService != null)
                ? recruitmentAnalysisService.getFunnel(userId, period)
                : Collections.emptyMap();

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * 岗位热度
     */
    @GetMapping("/heat")
    public ResponseEntity<Map<String, Object>> getHeat(
            @RequestParam(defaultValue = "total") String period,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("[RecruitmentAnalysis] 获取岗位热度, userId={}, period={}, limit={}", userId, period, limit);

        Map<String, Object> data = (recruitmentAnalysisService != null)
                ? recruitmentAnalysisService.getHeat(userId, period, limit)
                : Collections.emptyMap();

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * 侧边栏统计 + 热门职位榜单
     */
    @GetMapping("/sidebar")
    public ResponseEntity<Map<String, Object>> getSidebar(
            @RequestParam(defaultValue = "quarter") String period,
            @RequestParam(defaultValue = "3") Integer topN,
            @RequestParam(defaultValue = "30days") String timeRangeForTop
    ) {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("[RecruitmentAnalysis] 获取侧边栏统计, userId={}, period={}, topN={}, timeRangeForTop={}", userId, period, topN, timeRangeForTop);

        Map<String, Object> data = (recruitmentAnalysisService != null)
                ? recruitmentAnalysisService.getSidebar(userId, period, topN, timeRangeForTop)
                : Collections.emptyMap();

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    /**
     * 刷新统计
     */
    @GetMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh() {
        Long userId = userContextUtil.getCurrentUserId();
        log.info("[RecruitmentAnalysis] 手动刷新统计, userId={}", userId);

        Map<String, Object> data = (recruitmentAnalysisService != null)
                ? recruitmentAnalysisService.refresh(userId)
                : Collections.emptyMap();

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }
} 