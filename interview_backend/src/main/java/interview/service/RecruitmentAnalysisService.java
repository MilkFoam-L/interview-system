package interview.service;

import java.util.Map;

public interface RecruitmentAnalysisService {

	Map<String, Object> getKpis(Long userId, String timeRange);

	Map<String, Object> getTimeline(Long userId, String timeRange, String displayType);

	Map<String, Object> getFunnel(Long userId, String period);

	Map<String, Object> getHeat(Long userId, String period, Integer limit);

	Map<String, Object> getSidebar(Long userId, String period, Integer topN, String timeRangeForTop);

	Map<String, Object> refresh(Long userId);

} 