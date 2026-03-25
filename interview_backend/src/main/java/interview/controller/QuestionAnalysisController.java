package interview.controller;

import interview.service.QuestionAnalysisService;
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
 * 题库数据分析
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/question-analysis")
public class QuestionAnalysisController {

	private final UserContextUtil userContextUtil;

	@Autowired(required = false)
	private QuestionAnalysisService questionAnalysisService;

	private ResponseEntity<Map<String, Object>> ok(Object data) {
		Map<String, Object> resp = new HashMap<>();
		resp.put("code", 0);
		resp.put("msg", "success");
		resp.put("data", data);
		return ResponseEntity.ok(resp);
	}

	/**
	 * KPI 概览
	 */
	@GetMapping("/kpis")
	public ResponseEntity<Map<String, Object>> getKpis() {
		Long userId = userContextUtil.getCurrentUserId();
		log.info("[QuestionAnalysis] 获取KPI, userId={}", userId);

		Map<String, Object> data = (questionAnalysisService != null)
				? questionAnalysisService.getKpis(userId)
				: Collections.emptyMap();
		return ok(data);
	}

	/**
	 * 题型正确率/错误率
	 */
	@GetMapping("/type-accuracy")
	public ResponseEntity<Map<String, Object>> getTypeAccuracy() {
		Long userId = userContextUtil.getCurrentUserId();
		log.info("[QuestionAnalysis] 获取题型正确率, userId={}", userId);

		Map<String, Object> data = (questionAnalysisService != null)
				? questionAnalysisService.getTypeAccuracy(userId)
				: Collections.emptyMap();
		return ok(data);
	}

	/**
	 * 题型占比
	 */
	@GetMapping("/type-distribution")
	public ResponseEntity<Map<String, Object>> getTypeDistribution() {
		Long userId = userContextUtil.getCurrentUserId();
		log.info("[QuestionAnalysis] 获取题型占比, userId={}", userId);

		Map<String, Object> data = (questionAnalysisService != null)
				? questionAnalysisService.getTypeDistribution(userId)
				: Collections.emptyMap();
		return ok(data);
	}

	/**
	 * 分类占比
	 */
	@GetMapping("/category-distribution")
	public ResponseEntity<Map<String, Object>> getCategoryDistribution(
			@RequestParam(defaultValue = "major") String mode,
			@RequestParam(defaultValue = "12") Integer limit
	) {
		Long userId = userContextUtil.getCurrentUserId();
		String normalizedMode = (mode == null ? "major" : mode.toLowerCase());
		if (!"major".equals(normalizedMode) && !"sub".equals(normalizedMode)) {
			normalizedMode = "major";
		}
		if (limit == null || limit <= 0) { limit = 12; }

		log.info("[QuestionAnalysis] 获取分类占比, userId={}, mode={}, limit={}", userId, normalizedMode, limit);
		Map<String, Object> data = (questionAnalysisService != null)
				? questionAnalysisService.getCategoryDistribution(userId, normalizedMode, limit)
				: Collections.emptyMap();
		return ok(data);
	}

	/**
	 * 难度分布
	 */
	@GetMapping("/difficulty-distribution")
	public ResponseEntity<Map<String, Object>> getDifficultyDistribution() {
		Long userId = userContextUtil.getCurrentUserId();
		log.info("[QuestionAnalysis] 获取难度分布, userId={}", userId);

		Map<String, Object> data = (questionAnalysisService != null)
				? questionAnalysisService.getDifficultyDistribution(userId)
				: Collections.emptyMap();
		return ok(data);
	}

	/**
	 * 标签题量分布
	 */
	@GetMapping("/tag-distribution")
	public ResponseEntity<Map<String, Object>> getTagDistribution(
			@RequestParam(defaultValue = "12") Integer limit
	) {
		Long userId = userContextUtil.getCurrentUserId();
		if (limit == null || limit <= 0) { limit = 12; }
		log.info("[QuestionAnalysis] 获取标签分布, userId={}, limit={}", userId, limit);

		Map<String, Object> data = (questionAnalysisService != null)
				? questionAnalysisService.getTagDistribution(userId, limit)
				: Collections.emptyMap();
		return ok(data);
	}

	/**
	 * 题库更新信息
	 */
	@GetMapping("/update-info")
	public ResponseEntity<Map<String, Object>> getUpdateInfo() {
		Long userId = userContextUtil.getCurrentUserId();
		log.info("[QuestionAnalysis] 获取更新信息, userId={}", userId);

		Map<String, Object> data = (questionAnalysisService != null)
				? questionAnalysisService.getUpdateInfo(userId)
				: Collections.emptyMap();
		return ok(data);
	}

	/**
	 * 手动刷新
	 */
	@GetMapping("/refresh")
	public ResponseEntity<Map<String, Object>> refresh() {
		Long userId = userContextUtil.getCurrentUserId();
		log.info("[QuestionAnalysis] 手动刷新, userId={}", userId);

		Map<String, Object> data = (questionAnalysisService != null)
				? questionAnalysisService.refresh(userId)
				: Collections.emptyMap();
		return ok(data);
	}
} 