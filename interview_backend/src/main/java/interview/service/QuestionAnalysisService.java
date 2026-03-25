package interview.service;

import java.util.Map;

/**
 * 题库分析
 */
public interface QuestionAnalysisService {
	Map<String, Object> getKpis(Long userId);

	Map<String, Object> getTypeAccuracy(Long userId);

	Map<String, Object> getTypeDistribution(Long userId);

	Map<String, Object> getCategoryDistribution(Long userId, String mode, Integer limit);

	Map<String, Object> getDifficultyDistribution(Long userId);

	Map<String, Object> getTagDistribution(Long userId, Integer limit);

	Map<String, Object> getUpdateInfo(Long userId);

	Map<String, Object> refresh(Long userId);
} 