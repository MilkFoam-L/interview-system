package interview.service.impl;

import interview.repository.AnswerRecordRepository;
import interview.repository.QuestionRepository;
import interview.service.QuestionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAnalysisServiceImpl implements QuestionAnalysisService {

	private final QuestionRepository questionRepository;
	private final AnswerRecordRepository answerRecordRepository;

	@Override
	public Map<String, Object> getKpis(Long userId) {
		long total = questionRepository.count();
		long active = questionRepository.countActiveQuestions();
		long used = questionRepository.findAll().stream()
				.filter(q -> q.getUsageCount() != null && q.getUsageCount() > 0)
				.count();
		double coverage = (total > 0) ? (used * 100.0 / total) : 0.0;
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("totalQuestions", total);
		data.put("activeQuestions", active);
		data.put("usedQuestions", used);
		data.put("coverageRate", round2(coverage));
		return data;
	}

	@Override
	public Map<String, Object> getTypeAccuracy(Long userId) {
		List<interview.model.entity.AnswerRecord> all = answerRecordRepository.findAll();
		Map<String, List<interview.model.entity.AnswerRecord>> byType = all.stream()
				.filter(a -> a.getType() != null)
				.collect(Collectors.<interview.model.entity.AnswerRecord, String>groupingBy(a -> a.getType().toLowerCase()));
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		for (String type : Arrays.asList("basic", "choice", "code")) {
			List<interview.model.entity.AnswerRecord> list = byType.getOrDefault(type, Collections.<interview.model.entity.AnswerRecord>emptyList());
			long total = list.size();
			long correct = 0;
			for (interview.model.entity.AnswerRecord a : list) {
				if (a.getIsCorrect() != null && a.getIsCorrect().intValue() == 1) correct++;
			}
			double correctRate = total > 0 ? correct * 100.0 / total : 0.0;
			double wrongRate = 100.0 - correctRate;
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("type", type);
			row.put("correctRate", round2(correctRate));
			row.put("wrongRate", round2(wrongRate));
			row.put("attempts", total);
			items.add(row);
		}
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("items", items);
		return data;
	}

	@Override
	public Map<String, Object> getTypeDistribution(Long userId) {
		List<interview.model.entity.Question> all = questionRepository.findAll();
		Map<String, Long> counter = new HashMap<String, Long>();
		for (interview.model.entity.Question q : all) {
			String key = (q.getType() == null ? "unknown" : q.getType().toLowerCase());
			counter.put(key, counter.containsKey(key) ? counter.get(key) + 1L : 1L);
		}
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		for (Map.Entry<String, Long> e : counter.entrySet()) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("name", e.getKey());
			m.put("value", e.getValue());
			items.add(m);
		}
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("items", items);
		return data;
	}

	@Override
	public Map<String, Object> getCategoryDistribution(Long userId, String mode, Integer limit) {
		List<interview.model.entity.Question> all = questionRepository.findAll();
		List<interview.model.entity.Question> active = new ArrayList<interview.model.entity.Question>();
		for (interview.model.entity.Question q : all) {
			if (Boolean.TRUE.equals(q.getIsActive())) active.add(q);
		}
		Map<String, Long> counter = new HashMap<String, Long>();
		if ("sub".equalsIgnoreCase(mode)) {
			for (interview.model.entity.Question q : active) {
				String key = orEmpty(q.getCategoryType());
				counter.put(key, counter.containsKey(key) ? counter.get(key) + 1L : 1L);
			}
		} else {
			for (interview.model.entity.Question q : active) {
				String key = orEmpty(q.getCategory());
				counter.put(key, counter.containsKey(key) ? counter.get(key) + 1L : 1L);
			}
		}
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		List<Map.Entry<String, Long>> sorted = new ArrayList<Map.Entry<String, Long>>(counter.entrySet());
		sorted.sort(new Comparator<Map.Entry<String, Long>>() {
			@Override public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) { return Long.compare(b.getValue(), a.getValue()); }
		});
		int lim = (limit == null || limit <= 0) ? 12 : limit.intValue();
		for (int i = 0; i < sorted.size() && i < lim; i++) {
			Map.Entry<String, Long> e = sorted.get(i);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("name", e.getKey());
			m.put("value", e.getValue());
			items.add(m);
		}
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("items", items);
		return data;
	}

	@Override
	public Map<String, Object> getDifficultyDistribution(Long userId) {
		List<interview.model.entity.Question> all = questionRepository.findAll();
		Map<String, Long> counter = new HashMap<String, Long>();
		for (interview.model.entity.Question q : all) {
			String k = (q.getDifficulty() == null ? "medium" : q.getDifficulty().toLowerCase());
			counter.put(k, counter.containsKey(k) ? counter.get(k) + 1L : 1L);
		}
		long total = 0L;
		for (Long v : counter.values()) total += v;
		double easy = pct(counter.containsKey("easy") ? counter.get("easy") : 0L, total);
		double medium = pct(counter.containsKey("medium") ? counter.get("medium") : 0L, total);
		double hard = pct(counter.containsKey("hard") ? counter.get("hard") : 0L, total);
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("simple", round2(easy));
		data.put("medium", round2(medium));
		data.put("hard", round2(hard));
		return data;
	}

	@Override
	public Map<String, Object> getTagDistribution(Long userId, Integer limit) {
		List<interview.model.entity.Question> all = questionRepository.findAll();
		Map<String, Long> counter = new HashMap<String, Long>();
		for (interview.model.entity.Question q : all) {
			String tags = q.getTags();
			if (tags == null) continue;
			String trimmed = tags.trim();
			if (trimmed.isEmpty()) continue;
			for (String t : trimmed.split(",")) {
				String k = t == null ? "" : t.trim();
				if (k.isEmpty()) continue;
				counter.put(k, counter.containsKey(k) ? counter.get(k) + 1L : 1L);
			}
		}
		List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
		List<Map.Entry<String, Long>> sorted = new ArrayList<Map.Entry<String, Long>>(counter.entrySet());
		sorted.sort(new Comparator<Map.Entry<String, Long>>() {
			@Override public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) { return Long.compare(b.getValue(), a.getValue()); }
		});
		int lim = (limit == null || limit <= 0) ? 12 : limit.intValue();
		for (int i = 0; i < sorted.size() && i < lim; i++) {
			Map.Entry<String, Long> e = sorted.get(i);
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("tag", e.getKey());
			m.put("count", e.getValue());
			items.add(m);
		}
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("items", items);
		return data;
	}

	@Override
	public Map<String, Object> getUpdateInfo(Long userId) {
		List<interview.model.entity.Question> all = questionRepository.findAll();
		LocalDateTime last = null;
		for (interview.model.entity.Question q : all) {
			LocalDateTime t = q.getUpdateTime() != null ? q.getUpdateTime() : q.getCreateTime();
			if (t == null) continue;
			if (last == null || t.isAfter(last)) last = t;
		}
		long days = 0L;
		if (last != null) {
			days = Duration.between(last, LocalDateTime.now()).toDays();
		}
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("daysSinceUpdate", days);
		data.put("lastUpdatedAt", last == null ? null : last.toString());
		return data;
	}

	@Override
	public Map<String, Object> refresh(Long userId) {
		return Collections.<String, Object>singletonMap("refreshed", Boolean.TRUE);
	}

	private static String orEmpty(String v) { return v == null ? "" : v; }

	private static double pct(long part, long total) { return total > 0 ? part * 100.0 / total : 0.0; }

	private static double round2(double val) { return Math.round(val * 100.0) / 100.0; }
} 