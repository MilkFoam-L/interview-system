package interview.service.impl;

import interview.model.CompanyInfo;
import interview.repository.ApplicationRecordRepository;
import interview.repository.CompanyInfoRepository;
import interview.repository.InterviewSessionRepository;
import interview.repository.JobRepository;
import interview.service.RecruitmentAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitmentAnalysisServiceImpl implements RecruitmentAnalysisService {

    private final CompanyInfoRepository companyInfoRepository;
    private final ApplicationRecordRepository applicationRecordRepository;
    private final InterviewSessionRepository interviewSessionRepository;
    private final JobRepository jobRepository;

    @Override
    public Map<String, Object> getKpis(Long userId, String timeRange) {
        Map<String, Object> data = new HashMap<>();
        Long companyId = resolveCompanyIdByUser(userId);
        if (companyId == null) return data;

        DateRange curr = resolveTimeRange(timeRange);
        DateRange prev = curr.previous();

        long totalApplications = applicationRecordRepository
                .countByCompanyAndSubmitTimeBetween(companyId, curr.start, curr.end);
        long prevApplications = applicationRecordRepository
                .countByCompanyAndSubmitTimeBetween(companyId, prev.start, prev.end);
        double applicationGrowth = growthRate(prevApplications, totalApplications);

        long publishedJobs = jobRepository.countOpenJobsAsOf(companyId, curr.end);
        long publishedJobsPrev = jobRepository.countOpenJobsAsOf(companyId, prev.end);
        double jobsGrowth = growthRate(publishedJobsPrev, publishedJobs);

        long totalInterviewed = interviewSessionRepository
                .countDistinctInterviewees(companyId, curr.start, curr.end);
        long prevInterviewed = interviewSessionRepository
                .countDistinctInterviewees(companyId, prev.start, prev.end);
        double interviewGrowth = growthRate(prevInterviewed, totalInterviewed);

        // 周新增
        DateRange week = resolveTimeRange("7days");
        long weeklyApplications = applicationRecordRepository
                .countByCompanyAndSubmitTimeBetween(companyId, week.start, week.end);
        DateRange prevWeek = week.previous();
        long prevWeekly = applicationRecordRepository
                .countByCompanyAndSubmitTimeBetween(companyId, prevWeek.start, prevWeek.end);
        double weeklyGrowth = growthRate(prevWeekly, weeklyApplications);

        data.put("totalApplications", totalApplications);
        data.put("applicationGrowth", round1(applicationGrowth));
        data.put("publishedJobs", publishedJobs);
        data.put("jobsGrowth", round1(jobsGrowth));
        data.put("totalInterviewed", totalInterviewed);
        data.put("interviewGrowth", round1(interviewGrowth));
        data.put("weeklyApplications", weeklyApplications);
        data.put("weeklyGrowth", round1(weeklyGrowth));
        return data;
    }

    @Override
    public Map<String, Object> getTimeline(Long userId, String timeRange, String displayType) {
        Map<String, Object> data = new HashMap<>();
        Long companyId = resolveCompanyIdByUser(userId);
        if (companyId == null) return data;

        DateRange curr = resolveTimeRange(timeRange);
        if (Objects.equals(displayType, "all")) {
            List<Object[]> rows = applicationRecordRepository.timeSeriesByMonth(companyId, curr.start, curr.end);
            List<String> labels = rows.stream().map(r -> String.valueOf(r[0])).collect(Collectors.toList());
            List<Long> series = rows.stream().map(r -> ((Number) r[1]).longValue()).collect(Collectors.toList());
            data.put("granularity", "month");
            data.put("labels", labels);
            data.put("series", Collections.singletonList(mapOf(
                    "name", "总投递数",
                    "data", series
            )));
            return data;
        }
        // top3
        List<Object[]> top = applicationRecordRepository.topCategoryTypes(
                companyId, curr.start, curr.end, PageRequest.of(0, 3));
        List<String> cats = top.stream().map(r -> String.valueOf(r[0])).collect(Collectors.toList());
        if (cats.isEmpty()) {
            data.put("granularity", "month");
            data.put("labels", Collections.emptyList());
            data.put("series", Collections.emptyList());
            return data;
        }
        List<Object[]> rows = applicationRecordRepository.timeSeriesByMonthForCategories(companyId, curr.start, curr.end, cats);
        Map<String, Map<String, Long>> catToPoints = new LinkedHashMap<>();
        Set<String> labelSet = new LinkedHashSet<>();
        for (Object[] r : rows) {
            String ym = String.valueOf(r[0]);
            String cat = String.valueOf(r[1]);
            long cnt = ((Number) r[2]).longValue();
            labelSet.add(ym);
            catToPoints.computeIfAbsent(cat, k -> new LinkedHashMap<>()).put(ym, cnt);
        }
        List<String> labels = new ArrayList<>(labelSet);
        List<Map<String, Object>> series = new ArrayList<>();
        for (String cat : cats) {
            Map<String, Long> points = catToPoints.getOrDefault(cat, Collections.emptyMap());
            List<Long> y = labels.stream().map(l -> points.getOrDefault(l, 0L)).collect(Collectors.toList());
            series.add(mapOf("name", cat, "data", y));
        }
        data.put("granularity", "month");
        data.put("labels", labels);
        data.put("series", series);
        return data;
    }

    @Override
    public Map<String, Object> getFunnel(Long userId, String period) {
        Map<String, Object> data = new HashMap<>();
        Long companyId = resolveCompanyIdByUser(userId);
        if (companyId == null) return data;
        DateRange curr = resolvePeriod(period);

        long applied = applicationRecordRepository.countByCompanyAndSubmitTimeBetween(companyId, curr.start, curr.end);
        long scheduled = interviewSessionRepository.countScheduled(companyId, curr.start, curr.end);
        long completed = interviewSessionRepository.countCompleted(companyId, curr.start, curr.end);
        long notified = applicationRecordRepository.countNotifiedBetween(companyId, curr.start, curr.end);

        data.put("applied", applied);
        data.put("scheduled", scheduled);
        data.put("completed", completed);
        data.put("notified", notified);
        return data;
    }

    @Override
    public Map<String, Object> getHeat(Long userId, String period, Integer limit) {
        Map<String, Object> data = new HashMap<>();
        Long companyId = resolveCompanyIdByUser(userId);
        if (companyId == null) return data;
        DateRange curr = resolvePeriod(period);

        List<Object[]> topJobs = applicationRecordRepository.topJobs(
                companyId, curr.start, curr.end, PageRequest.of(0, limit != null ? limit : 10));
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] r : topJobs) {
            Integer jobId = ((Number) r[0]).intValue();
            String title = String.valueOf(r[1]);
            long resumes = ((Number) r[2]).longValue();
            long interviews = interviewSessionRepository.countInterviewsForTitle(companyId, title, curr.start, curr.end);
            long positions = jobRepository.countOpenJobsByTitle(companyId, title);
            items.add(mapOf(
                    "id", jobId,
                    "title", title,
                    "resumes", resumes,
                    "interviews", interviews,
                    "positions", positions,
                    "percentage", resumes
            ));
        }
        long max = items.stream().mapToLong(m -> ((Number) m.get("percentage")).longValue()).max().orElse(1L);
        for (Map<String, Object> it : items) {
            long v = ((Number) it.get("percentage")).longValue();
            int pct = (int) Math.round(v * 100.0 / max);
            it.put("percentage", pct);
        }
        data.put("items", items);
        return data;
    }

    @Override
    public Map<String, Object> getSidebar(Long userId, String period, Integer topN, String timeRangeForTop) {
        Map<String, Object> data = new HashMap<>();
        Long companyId = resolveCompanyIdByUser(userId);
        if (companyId == null) return data;

        DateRange quarter = resolvePeriod("quarter");
        long quarterJobs = jobRepository.countNewOpenJobsBetween(companyId, quarter.start, quarter.end);
        long quarterInterviewees = interviewSessionRepository.countDistinctInterviewees(companyId, quarter.start, quarter.end);

        DateRange tr = resolveTimeRange(timeRangeForTop);
        List<Object[]> top = applicationRecordRepository.topJobs(
                companyId, tr.start, tr.end, PageRequest.of(0, topN != null ? topN : 3));
        List<Map<String, Object>> recentJobs = top.stream().map(r -> mapOf(
                "id", ((Number) r[0]).intValue(),
                "title", String.valueOf(r[1]),
                "applicationCount", ((Number) r[2]).longValue()
        )).collect(Collectors.toList());

        Map<String, Object> recentStats = new HashMap<>();
        recentStats.put("interviews", quarterJobs);
        recentStats.put("passed", quarterInterviewees);

        data.put("recentStats", recentStats);
        data.put("recentJobs", recentJobs);
        return data;
    }

    @Override
    public Map<String, Object> refresh(Long userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("refreshed", true);
        return data;
    }


    private Long resolveCompanyIdByUser(Long userId) {
        return companyInfoRepository.findByUserIdAndIsDeletedFalse(userId)
                .map(CompanyInfo::getId)
                .orElse(null);
    }

    private static class DateRange {
        final LocalDateTime start;
        final LocalDateTime end;
        final String key;
        DateRange(LocalDateTime s, LocalDateTime e, String key) { this.start = s; this.end = e; this.key = key; }
        DateRange previous() {
            switch (key) {
                case "7days":
                    return new DateRange(start.minusDays(7), end.minusDays(7), key);
                case "30days":
                    return new DateRange(start.minusDays(30), end.minusDays(30), key);
                case "90days":
                    return new DateRange(start.minusDays(90), end.minusDays(90), key);
                case "thisYear":
                    return new DateRange(start.minusYears(1), end.minusYears(1), key);
                default:
                    return new DateRange(start.minusDays(30), end.minusDays(30), key);
            }
        }
    }

    private DateRange resolveTimeRange(String timeRange) {
        LocalDate today = LocalDate.now();
        LocalDateTime end = LocalDateTime.of(today, LocalTime.MAX);
        LocalDateTime start;
        switch (timeRange) {
            case "7days":
                start = end.minusDays(6).with(LocalTime.MIN);
                return new DateRange(start, end, "7days");
            case "90days":
                start = end.minusDays(89).with(LocalTime.MIN);
                return new DateRange(start, end, "90days");
            case "thisYear":
                start = LocalDateTime.of(LocalDate.of(today.getYear(), 1, 1), LocalTime.MIN);
                return new DateRange(start, end, "thisYear");
            case "30days":
            default:
                start = end.minusDays(29).with(LocalTime.MIN);
                return new DateRange(start, end, "30days");
        }
    }

    private DateRange resolvePeriod(String period) {
        LocalDate today = LocalDate.now();
        switch (period) {
            case "year":
                return new DateRange(LocalDateTime.of(LocalDate.of(today.getYear(), 1, 1), LocalTime.MIN),
                        LocalDateTime.of(today, LocalTime.MAX), "year");
            case "quarter":
                int month = today.getMonthValue();
                int qStartMonth = ((month - 1) / 3) * 3 + 1;
                LocalDate qStart = LocalDate.of(today.getYear(), qStartMonth, 1);
                LocalDate qEndDate = qStart.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
                return new DateRange(LocalDateTime.of(qStart, LocalTime.MIN), LocalDateTime.of(qEndDate, LocalTime.MAX), "quarter");
            case "month":
                LocalDate mStart = today.with(TemporalAdjusters.firstDayOfMonth());
                LocalDate mEndDate = today.with(TemporalAdjusters.lastDayOfMonth());
                return new DateRange(LocalDateTime.of(mStart, LocalTime.MIN), LocalDateTime.of(mEndDate, LocalTime.MAX), "month");
            case "total":
            default:
                // total：近一年统计，避免扫全表
                LocalDateTime end = LocalDateTime.of(today, LocalTime.MAX);
                LocalDateTime start = end.minusYears(1);
                return new DateRange(start, end, "total");
        }
    }

    private static double growthRate(long prev, long curr) {
        if (prev <= 0) return curr > 0 ? 100.0 : 0.0;
        return (curr - prev) * 100.0 / prev;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }
} 