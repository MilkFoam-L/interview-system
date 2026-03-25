package interview.service.impl;

import interview.model.JobApplication;
import interview.model.entity.InterviewSession;
import interview.repository.ComprehensiveReportRepository;
import interview.repository.InterviewSessionRepository;
import interview.repository.JobApplicationRepository;
import interview.service.AIScreeningService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIScreeningServiceImpl implements AIScreeningService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private ComprehensiveReportRepository comprehensiveReportRepository;

    @Autowired
    private InterviewSessionRepository interviewSessionRepository;

    @Override
    public Map<String, Object> queryCandidates(String keyword, String position, Integer status, int page, int size) {
        int pageIndex = Math.max(page, 1) - 1;
        int limit = Math.max(size, 1);
        int offset = pageIndex * limit;

        StringBuilder baseSql = new StringBuilder();
        baseSql.append("FROM job_applications ja \n")
                .append("JOIN users u ON u.id = ja.user_id \n")
                .append("JOIN job j ON j.id = ja.job_id \n")
                .append("LEFT JOIN user_details ud ON ud.user_id = u.id \n")
                .append("LEFT JOIN (\n")
                .append("  SELECT cr1.* FROM comprehensive_reports cr1 \n")
                .append("  JOIN (SELECT user_id, job_id, MAX(start_time) AS max_start FROM comprehensive_reports GROUP BY user_id, job_id) t\n")
                .append("    ON t.user_id = cr1.user_id AND t.job_id = cr1.job_id AND t.max_start = cr1.start_time\n")
                .append(") cr ON cr.user_id = ja.user_id AND cr.job_id = ja.job_id \n")
                .append("WHERE 1=1 ");

        Map<String, Object> params = new HashMap<>();

        if (status != null) {
            baseSql.append(" AND ja.ai_screening_status = :status");
            params.put("status", status);
        }
        if (position != null && !position.trim().isEmpty()) {
            baseSql.append(" AND j.title LIKE :position");
            params.put("position", "%" + position.trim() + "%");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            baseSql.append(" AND (u.real_name LIKE :kw OR u.username LIKE :kw OR j.title LIKE :kw ");
            baseSql.append(" OR (ud.skills IS NOT NULL AND ud.skills LIKE :kw))");
            params.put("kw", "%" + keyword.trim() + "%");
        }

        String countSql = "SELECT COUNT(1) " + baseSql;
        Query countQuery = entityManager.createNativeQuery(countSql);
        params.forEach(countQuery::setParameter);
        Number total = (Number) countQuery.getSingleResult();

        String dataSql = "SELECT ja.id AS application_id, u.id AS user_id, COALESCE(u.real_name, u.username) AS name, " +
                "j.id AS job_id, j.title AS position, cr.total_score AS score, cr.start_time AS interview_time, " +
                "ja.ai_screening_status AS status_code, ud.skills AS skills_json " +
                baseSql +
                " ORDER BY COALESCE(cr.total_score, 0) DESC, COALESCE(cr.start_time, '1970-01-01 00:00:00') DESC " +
                " LIMIT :limit OFFSET :offset";

        Query dataQuery = entityManager.createNativeQuery(dataSql);
        params.forEach(dataQuery::setParameter);
        dataQuery.setParameter("limit", limit);
        dataQuery.setParameter("offset", offset);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();

        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("applicationId", ((Number) r[0]).longValue());
            m.put("userId", ((Number) r[1]).longValue());
            m.put("name", r[2]);
            m.put("jobId", ((Number) r[3]).intValue());
            m.put("position", r[4]);
            m.put("score", r[5] == null ? null : Double.valueOf(r[5].toString()));
            m.put("createTime", r[6]);
            Integer statusCode = r[7] == null ? 0 : ((Number) r[7]).intValue();
            m.put("status", statusLabel(statusCode));
            m.put("statusCode", statusCode);
            m.put("skills", parseSkills(r[8]));
            list.add(m);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total == null ? 0 : total.longValue());
        return result;
    }

    private String statusLabel(int code) {
        switch (code) {
            case 1:
                return "已通过";
            case 2:
                return "已拒绝";
            default:
                return "待处理";
        }
    }

    private List<String> parseSkills(Object skillsJson) {
        if (skillsJson == null) return Collections.emptyList();
        String s = String.valueOf(skillsJson).trim();
        if (s.isEmpty()) return Collections.emptyList();
        if (s.startsWith("[") && s.endsWith("]")) {
            String inner = s.substring(1, s.length() - 1).trim();
            if (inner.isEmpty()) return Collections.emptyList();
            return Arrays.stream(inner.split(","))
                    .map(x -> x.replace("\"", "").replace("'", "").trim())
                    .filter(x -> !x.isEmpty())
                    .collect(Collectors.toList());
        }
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void notifyPass(Long applicationId, Long operatorUserId, LocalDateTime notifyTime) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("申请记录不存在"));
        app.setAiScreeningStatus((byte)1);
        app.setAiScreeningNotifiedAt(notifyTime == null ? LocalDateTime.now() : notifyTime);
        app.setAiScreeningNotifiedBy(operatorUserId);
        jobApplicationRepository.save(app);
    }

    @Override
    @Transactional
    public void reject(Long applicationId, String reason, Long operatorUserId) {
        JobApplication app = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("申请记录不存在"));
        app.setAiScreeningStatus((byte)2);
        app.setAiScreeningReason(reason);
        jobApplicationRepository.save(app);
    }

    @Override
    @Transactional
    public void batchNotifyPass(List<Long> applicationIds, Long operatorUserId, LocalDateTime notifyTime) {
        if (applicationIds == null || applicationIds.isEmpty()) return;
        LocalDateTime n = notifyTime == null ? LocalDateTime.now() : notifyTime;
        List<JobApplication> apps = jobApplicationRepository.findAllById(applicationIds);
        for (JobApplication app : apps) {
            app.setAiScreeningStatus((byte)1);
            app.setAiScreeningNotifiedAt(n);
            app.setAiScreeningNotifiedBy(operatorUserId);
        }
        jobApplicationRepository.saveAll(apps);
    }

    @Override
    @Transactional
    public void batchReject(List<Long> applicationIds, String reason, Long operatorUserId) {
        if (applicationIds == null || applicationIds.isEmpty()) return;
        List<JobApplication> apps = jobApplicationRepository.findAllById(applicationIds);
        for (JobApplication app : apps) {
            app.setAiScreeningStatus((byte)2);
            app.setAiScreeningReason(reason);
        }
        jobApplicationRepository.saveAll(apps);
    }

    @Override
    public Map<String, Object> getLatestReportByApplicationId(Long applicationId) {
        InterviewSession session = interviewSessionRepository.findByApplicationId(applicationId);
        if (session == null) return Collections.emptyMap();
        return comprehensiveReportRepository.findBySessionId(session.getId())
                .map(cr -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("sessionId", cr.getSessionId());
                    m.put("totalScore", cr.getTotalScore());
                    m.put("startTime", cr.getStartTime());
                    m.put("endTime", cr.getEndTime());
                    m.put("duration", cr.getDuration());
                    m.put("starRating", cr.getStarRating());
                    m.put("starSituationScore", cr.getStarSituationScore());
                    m.put("starTaskScore", cr.getStarTaskScore());
                    m.put("starActionScore", cr.getStarActionScore());
                    m.put("starResultScore", cr.getStarResultScore());
                    m.put("comprehensiveAnalysis", cr.getComprehensiveAnalysis());
                    m.put("improvements", cr.getImprovements());
                    return m;
                })
                .orElse(Collections.emptyMap());
    }
} 