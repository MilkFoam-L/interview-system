package interview.service.impl;

import com.alibaba.fastjson.JSON;
import interview.ai.SparkClient;
import interview.ai.spark.SparkX1BasicQAAnalyzer;
import interview.ai.spark.SparkX1AbilityMatrixAnalyzer;
import interview.ai.spark.SparkX1ComprehensiveAnalyzer;
import interview.ai.spark.SparkX1PersonalizedSuggestionsAnalyzer;
import interview.model.ComprehensiveReport;
import interview.model.FaceExpressionRecord;
import interview.model.ScenarioModel;
import interview.model.ScenarioReport;
import interview.model.entity.CodeExecution;
import interview.repository.ComprehensiveReportRepository;
import interview.repository.FaceExpressionRecordRepository;
import interview.repository.ScenarioReportRepository;
import interview.service.ComprehensiveReportService;
import interview.service.CodeExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ComprehensiveReportServiceImpl implements ComprehensiveReportService {
    private static final Logger log = LoggerFactory.getLogger(ComprehensiveReportServiceImpl.class);
    private final ComprehensiveReportRepository comprehensiveReportRepository;
    private final ScenarioReportRepository scenarioReportRepository;
    private final FaceExpressionRecordRepository faceExpressionRecordRepository;
    private final SparkClient sparkClient;
    private final SparkX1BasicQAAnalyzer basicQAAnalyzer;
    private final SparkX1AbilityMatrixAnalyzer abilityMatrixAnalyzer;
    private final SparkX1ComprehensiveAnalyzer comprehensiveAnalyzer;
    private final SparkX1PersonalizedSuggestionsAnalyzer personalizedSuggestionsAnalyzer;
    private final CodeExecutionService codeExecutionService;

    @Autowired
    public ComprehensiveReportServiceImpl(ComprehensiveReportRepository comprehensiveReportRepository,
                                          ScenarioReportRepository scenarioReportRepository,
                                          FaceExpressionRecordRepository faceExpressionRecordRepository,
                                          SparkClient sparkClient,
                                          SparkX1BasicQAAnalyzer basicQAAnalyzer,
                                          SparkX1AbilityMatrixAnalyzer abilityMatrixAnalyzer,
                                          SparkX1ComprehensiveAnalyzer comprehensiveAnalyzer,
                                          SparkX1PersonalizedSuggestionsAnalyzer personalizedSuggestionsAnalyzer,
                                          CodeExecutionService codeExecutionService) {
        this.comprehensiveReportRepository = comprehensiveReportRepository;
        this.scenarioReportRepository = scenarioReportRepository;
        this.faceExpressionRecordRepository = faceExpressionRecordRepository;
        this.sparkClient = sparkClient;
        this.basicQAAnalyzer = basicQAAnalyzer;
        this.abilityMatrixAnalyzer = abilityMatrixAnalyzer;
        this.comprehensiveAnalyzer = comprehensiveAnalyzer;
        this.personalizedSuggestionsAnalyzer = personalizedSuggestionsAnalyzer;
        this.codeExecutionService = codeExecutionService;
    }

    @Override
    public List<FaceExpressionRecord> getExpressionRecords(Long sessionId) {
        log.info("获取表情分析数据 sessionId={}", sessionId);
        return faceExpressionRecordRepository.findBySessionId(sessionId);
    }

    @Override
    public Map<String, Object> getStarData(Long sessionId) {
        log.info("获取STAR结构分数 sessionId={}", sessionId);
        Map<String, Object> result = new HashMap<>();
        comprehensiveReportRepository.findBySessionId(sessionId).ifPresent(report -> {
            result.put("star_situation_score", report.getStarSituationScore());
            result.put("star_task_score", report.getStarTaskScore());
            result.put("star_action_score", report.getStarActionScore());
            result.put("star_result_score", report.getStarResultScore());
        });
        return result;
    }

    @Override
    public Map<String, Object> getAnswerQuality(Long sessionId) {
        log.info("获取问题回答质量分析 sessionId={}", sessionId);
        Map<String, Object> result = new HashMap<>();
        scenarioReportRepository.findById(sessionId).ifPresent(report -> {
            result.put("tech_score", report.getTechScore());
            result.put("project_score", report.getProjectScore());
            result.put("team_score", report.getTeamScore());
            result.put("plan_score", report.getPlanScore());
            result.put("attitude_score", report.getAttitudeScore());
        });
        return result;
    }

    @Override
    public Map<String, Object> getAbilityMatrix(Long sessionId) {
        log.info("获取能力评估矩阵数据 sessionId={}", sessionId);
        Map<String, Object> result = new HashMap<>();
        
        try {
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (reportOpt.isPresent()) {
                ComprehensiveReport report = reportOpt.get();
                String abilityMatrixData = report.getAbilityMatrixData();
                
                // 如果数据存在，直接返回
                if (abilityMatrixData != null && !abilityMatrixData.trim().isEmpty()) {
                    try {
                        // 解析JSON数据并格式化为前端需要的格式
                        Map<String, Object> parsedData = JSON.parseObject(abilityMatrixData);
                        result.put("ability_matrix_data", parsedData);
                        log.info("能力评估矩阵数据获取成功 sessionId={}", sessionId);
                    } catch (Exception e) {
                        log.warn("解析能力矩阵数据失败 sessionId={}", sessionId, e);
                        result.put("ability_matrix_data", null);
                    }
                } else {
                    // 数据不存在，返回空数据（前端会显示默认数据）
                    log.info("能力评估矩阵数据尚未生成 sessionId={}", sessionId);
                    result.put("ability_matrix_data", null);
                }
            } else {
                log.warn("未找到综合报告 sessionId={}", sessionId);
                result.put("ability_matrix_data", null);
            }
        } catch (Exception e) {
            log.error("获取能力评估矩阵数据失败 sessionId={}", sessionId, e);
            result.put("ability_matrix_data", null);
        }
        
        return result;
    }

    @Override
    public Page<ComprehensiveReport> getReportList(Long userId, int page, int size, String sortBy, 
                                                 String searchKeyword, LocalDateTime startTime, 
                                                 LocalDateTime endTime, String filterType) {
        log.info("获取用户报告列表 userId={}, page={}, size={}, sortBy={}, searchKeyword={}, filterType={}", 
                userId, page, size, sortBy, searchKeyword, filterType);
        
        // 处理排序
        Sort sort;
        if ("score".equals(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "totalScore");
        } else {
            sort = Sort.by(Sort.Direction.DESC, "startTime");
        }
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // 处理搜索关键词
        String companyKeyword = null;
        String positionKeyword = null;
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            String keyword = searchKeyword.trim();
            companyKeyword = keyword;
            positionKeyword = keyword;
        }
        
        // 处理筛选类型
        BigDecimal minScore = null;
        if ("highScore".equals(filterType)) {
            minScore = new BigDecimal("80");
        }
        
        // 处理日期范围
        if ("recent".equals(filterType)) {
            endTime = LocalDateTime.now();
            startTime = endTime.minusDays(7);
        }
        
        return comprehensiveReportRepository.findByUserIdWithFilters(
                userId, companyKeyword, positionKeyword, startTime, endTime, minScore, pageable);
    }

    @Override
    public Map<String, Object> getUserStats(Long userId) {
        log.info("获取用户统计数据 userId={}", userId);
        
        Map<String, Object> stats = new HashMap<>();
        
        // 获取所有报告用于统计
        List<ComprehensiveReport> allReports = comprehensiveReportRepository.findByUserIdOrderByStartTimeDesc(userId);
        
        // 总面试次数
        stats.put("totalInterviews", allReports.size());
        
        if (!allReports.isEmpty()) {
            // 平均得分
            double averageScore = allReports.stream()
                    .mapToDouble(report -> report.getTotalScore().doubleValue())
                    .average()
                    .orElse(0.0);
            stats.put("averageScore", Math.round(averageScore));
            
            // 高分面试数
            Long highScoreCount = comprehensiveReportRepository.countHighScoreReports(userId, new BigDecimal("80"));
            stats.put("highScoreCount", highScoreCount);
            
            // 总时长
            int totalDuration = allReports.stream()
                    .mapToInt(ComprehensiveReport::getDuration)
                    .sum();
            stats.put("totalDuration", totalDuration);
            
            // 最新面试记录 - 直接返回实体对象
            Optional<ComprehensiveReport> lastInterview = comprehensiveReportRepository.findTopByUserIdOrderByStartTimeDesc(userId);
            lastInterview.ifPresent(report -> stats.put("lastInterview", report));
        } else {
            stats.put("averageScore", 0);
            stats.put("highScoreCount", 0);
            stats.put("totalDuration", 0);
            stats.put("lastInterview", null);
        }
        
        return stats;
    }

    @Override
    public ComprehensiveReport createOrUpdateStartTime(Long sessionId, Long userId, Integer jobId, 
                                                      String companyName, String positionName) {
        log.info("创建或更新面试开始时间 sessionId={}, userId={}, jobId={}, companyName={}, positionName={}", 
                sessionId, userId, jobId, companyName, positionName);
        
        try {
            // 查找是否已存在该会话的综合报告
            Optional<ComprehensiveReport> existingReport = comprehensiveReportRepository.findBySessionId(sessionId);
            
            ComprehensiveReport report;
            LocalDateTime startTime = LocalDateTime.now();
            
            if (existingReport.isPresent()) {
                // 更新现有报告的开始时间
                report = existingReport.get();
                report.setStartTime(startTime);
                log.info("更新现有综合报告的开始时间 reportId={}, startTime={}", report.getId(), startTime);
            } else {
                // 创建新的综合报告
                report = new ComprehensiveReport();
                report.setSessionId(sessionId);
                report.setUserId(userId);
                report.setJobId(jobId);
                report.setCompanyName(companyName);
                report.setPositionName(positionName);
                report.setStartTime(startTime);
                
                // 设置默认值
                report.setEndTime(startTime);
                report.setDuration(0);
                report.setTotalScore(BigDecimal.ZERO);
                report.setStarRating(BigDecimal.ZERO);
                
                log.info("创建新的综合报告 sessionId={}, startTime={}", sessionId, startTime);
            }
            
            ComprehensiveReport savedReport = comprehensiveReportRepository.save(report);
            log.info("成功保存综合报告 reportId={}, startTime={}", savedReport.getId(), savedReport.getStartTime());
            
            return savedReport;
        } catch (Exception e) {
            log.error("创建或更新面试开始时间失败 sessionId={}", sessionId, e);
            throw new RuntimeException("创建或更新面试开始时间失败: " + e.getMessage());
        }
    }

    @Override
    public ComprehensiveReport updateEndTimeAndDuration(Long sessionId) {
        log.info("更新面试结束时间并计算时长 sessionId={}", sessionId);
        
        try {
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            
            if (!reportOpt.isPresent()) {
                log.error("未找到会话对应的综合报告 sessionId={}", sessionId);
                throw new RuntimeException("未找到会话对应的综合报告");
            }
            
            ComprehensiveReport report = reportOpt.get();
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = report.getStartTime();
            
            // 计算时长（分钟），舍弃秒数
            long durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
            
            report.setEndTime(endTime);
            report.setDuration((int) durationMinutes);
            
            ComprehensiveReport savedReport = comprehensiveReportRepository.save(report);
            
            log.info("成功更新面试结束时间 reportId={}, startTime={}, endTime={}, duration={}分钟", 
                    savedReport.getId(), startTime, endTime, durationMinutes);
            
            // 面试完成后，自动生成能力评估矩阵
            try {
                if (abilityMatrixAnalyzer.isAvailable()) {
                    log.info("面试完成，开始自动生成能力评估矩阵 sessionId={}", sessionId);
                    Long userId = savedReport.getUserId();
                    if (userId != null) {
                        generateAndUpdateAbilityMatrix(sessionId, userId);
                        log.info("能力评估矩阵自动生成完成 sessionId={}", sessionId);
                    } else {
                        log.warn("无法获取用户ID，跳过能力矩阵自动生成 sessionId={}", sessionId);
                    }
                } else {
                    log.warn("能力矩阵分析服务不可用，跳过自动生成 sessionId={}", sessionId);
                }
            } catch (Exception e) {
                // 这里只记录错误，不影响面试结束流程
                log.error("自动生成能力评估矩阵失败 sessionId={}", sessionId, e);
            }
            
            // 面试完成后，自动生成综合分析简报
            try {
                if (comprehensiveAnalyzer.isAvailable()) {
                    log.info("面试完成，开始自动生成综合分析简报 sessionId={}", sessionId);
                    Long userId = savedReport.getUserId();
                    if (userId != null) {
                        generateAndUpdateComprehensiveAnalysis(sessionId, userId);
                        log.info("综合分析简报自动生成完成 sessionId={}", sessionId);
                    } else {
                        log.warn("无法获取用户ID，跳过综合分析简报自动生成 sessionId={}", sessionId);
                    }
                } else {
                    log.warn("综合分析服务不可用，跳过自动生成 sessionId={}", sessionId);
                }
            } catch (Exception e) {
                // 这里只记录错误，不影响面试结束流程
                log.error("自动生成综合分析简报失败 sessionId={}", sessionId, e);
            }
            
            return savedReport;
        } catch (Exception e) {
            log.error("更新面试结束时间失败 sessionId={}", sessionId, e);
            throw new RuntimeException("更新面试结束时间失败: " + e.getMessage());
        }
    }

    @Override
    public Optional<ComprehensiveReport> getBySessionId(Long sessionId) {
        log.info("根据会话ID获取综合报告 sessionId={}", sessionId);
        return comprehensiveReportRepository.findBySessionId(sessionId);
    }

    @Override
    @Transactional
    public ComprehensiveReport generateAndUpdateStarScores(Long sessionId) {
        log.info("生成并更新STAR分数 sessionId={}", sessionId);
        
        try {
            // 获取综合报告
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (!reportOpt.isPresent()) {
                log.error("未找到会话对应的综合报告 sessionId={}", sessionId);
                throw new RuntimeException("未找到会话对应的综合报告");
            }
            
            ComprehensiveReport report = reportOpt.get();
            
            // 获取场景报告的问答内容
            Optional<ScenarioReport> scenarioReportOpt = scenarioReportRepository.findById(sessionId);
            if (!scenarioReportOpt.isPresent() || scenarioReportOpt.get().getContent() == null) {
                log.warn("未找到场景报告内容，使用默认STAR分数 sessionId={}", sessionId);
                setDefaultStarScores(report);
                return comprehensiveReportRepository.save(report);
            }
            
            String reportText = scenarioReportOpt.get().getContent();
            
            // STAR分析Prompt
            String prompt = ScenarioModel.generateStarAnalysisPrompt(reportText);
            
            // 调用大模型进行分析
            String analysisResult = sparkClient.ask(prompt);
            
            // 解析分析结果
            ScenarioModel.StarAnalysisResult starResult = ScenarioModel.parseStarAnalysisResult(analysisResult);
            
            // 更新综合报告的STAR分数
            int situationScore = (int) Math.round(starResult.getSituationScore());
            int taskScore = (int) Math.round(starResult.getTaskScore());
            int actionScore = (int) Math.round(starResult.getActionScore());
            int resultScore = (int) Math.round(starResult.getResultScore());
            
            report.setStarSituationScore(BigDecimal.valueOf(situationScore));
            report.setStarTaskScore(BigDecimal.valueOf(taskScore));
            report.setStarActionScore(BigDecimal.valueOf(actionScore));
            report.setStarResultScore(BigDecimal.valueOf(resultScore));
            
            // 计算并更新总分和星级评分
            int averageScore = (situationScore + taskScore + actionScore + resultScore) / 4;
            report.setTotalScore(BigDecimal.valueOf(averageScore));
            report.setStarRating(calculateStarRating(BigDecimal.valueOf(averageScore)));
            
            ComprehensiveReport savedReport = comprehensiveReportRepository.save(report);
            log.info("STAR分数已保存 reportId={}", savedReport.getId());
            
            return savedReport;
            
        } catch (Exception e) {
            log.error("生成并更新STAR分数失败 sessionId={}", sessionId, e);
            // 失败时使用默认分数
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (reportOpt.isPresent()) {
                ComprehensiveReport report = reportOpt.get();
                setDefaultStarScores(report);
                return comprehensiveReportRepository.save(report);
            }
            throw new RuntimeException("生成并更新STAR分数失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置默认STAR分数
     */
    private void setDefaultStarScores(ComprehensiveReport report) {
        BigDecimal defaultScore = BigDecimal.valueOf(61);
        report.setStarSituationScore(defaultScore);
        report.setStarTaskScore(defaultScore);
        report.setStarActionScore(defaultScore);
        report.setStarResultScore(defaultScore);
        if (report.getTotalScore().equals(BigDecimal.ZERO)) {
            report.setTotalScore(defaultScore);
        }
        report.setStarRating(calculateStarRating(report.getTotalScore()));
    }
    
    /**
     * 基于总分计算星级评分（0-5分制）
     * @param totalScore 总分（0-100分）
     * @return 星级评分（0-5分，保留1位小数）
     */
    private BigDecimal calculateStarRating(BigDecimal totalScore) {
        if (totalScore == null) {
            return BigDecimal.valueOf(0.0);
        }

        BigDecimal starRating = totalScore
            .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP)
            .multiply(BigDecimal.valueOf(5))
            .setScale(1, BigDecimal.ROUND_HALF_UP);

        if (starRating.compareTo(BigDecimal.valueOf(5.0)) > 0) {
            starRating = BigDecimal.valueOf(5.0);
        } else if (starRating.compareTo(BigDecimal.ZERO) < 0) {
            starRating = BigDecimal.valueOf(0.0);
        }
        
        log.debug("总分{}转换为星级评分{}", totalScore, starRating);
        return starRating;
    }

    @Override
    @Transactional
    public ComprehensiveReport analyzeAndUpdateBasicQA(Long sessionId, Long userId) {
        log.info("分析基础问答环节并更新到综合报告 sessionId={}, userId={}", sessionId, userId);
        
        try {
            // 检查分析器是否可用
            if (!basicQAAnalyzer.isAvailable()) {
                log.warn("星火X1基础问答分析器不可用，跳过分析 sessionId={}", sessionId);
                throw new RuntimeException("基础问答分析器不可用");
            }
            
            // 获取综合报告
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (!reportOpt.isPresent()) {
                log.error("未找到会话对应的综合报告 sessionId={}", sessionId);
                throw new RuntimeException("未找到会话对应的综合报告");
            }
            
            ComprehensiveReport report = reportOpt.get();
            
            // 调用星火X1进行基础问答分析
            SparkX1BasicQAAnalyzer.BasicQAAnalysisResult analysisResult = 
                basicQAAnalyzer.analyzeBasicQA(sessionId, userId);
            
            if (!analysisResult.isSuccess()) {
                log.warn("基础问答分析失败，使用默认值 sessionId={}", sessionId);
                // 分析失败时不更新字段，保持原有值
                return report;
            }
            
            // 将分析结果转换为JSON并存储到basic_qa_analysis字段
            String analysisJson = analysisResult.toJsonString();
            report.setBasicQaAnalysis(analysisJson);
            
            // 保存更新后的报告
            ComprehensiveReport savedReport = comprehensiveReportRepository.save(report);
            
            log.info("基础问答分析结果已保存 reportId={}, technicalScore={}", 
                    savedReport.getId(), analysisResult.getTechnicalScore());
            
            return savedReport;
            
        } catch (Exception e) {
            log.error("基础问答分析失败 sessionId={}", sessionId, e);
            // 分析失败时返回原有报告，不做修改
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (reportOpt.isPresent()) {
                return reportOpt.get();
            }
            throw new RuntimeException("基础问答分析失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getTechAnalysis(Long sessionId) {
        log.info("获取技术能力分析结果 sessionId={}", sessionId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取综合报告
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (!reportOpt.isPresent()) {
                log.warn("未找到综合报告 sessionId={}", sessionId);
                result.put("basicQaAnalysis", null);
                result.put("codeAnalysis", null);
                result.put("overallScore", 0);
                result.put("summary", "暂无分析数据");
                return result;
            }
            
            ComprehensiveReport report = reportOpt.get();
            
            // 获取基础问答分析结果
            String basicQaAnalysisJson = report.getBasicQaAnalysis();
            Map<String, Object> basicQaAnalysis = null;
            if (basicQaAnalysisJson != null && !basicQaAnalysisJson.trim().isEmpty()) {
                try {
                    basicQaAnalysis = JSON.parseObject(basicQaAnalysisJson);
                } catch (Exception e) {
                    log.warn("解析基础问答分析结果失败 sessionId={}", sessionId, e);
                }
            }
            
            // 获取代码题分析结果
            Map<String, Object> codeAnalysis = getCodeAnalysisResults(sessionId);
            
            // 计算综合评分
            int overallScore = 0;
            StringBuilder summary = new StringBuilder();
            
            if (basicQaAnalysis != null) {
                Integer technicalScore = (Integer) basicQaAnalysis.get("technicalScore");
                if (technicalScore != null) {
                    overallScore = technicalScore;
                }
                
                String analysisText = (String) basicQaAnalysis.get("analysis");
                if (analysisText != null && !analysisText.trim().isEmpty()) {
                    summary.append("基础问答分析：").append(analysisText);
                }
            }
            
            if (codeAnalysis != null) {
                // 处理代码分析结果
                if (summary.length() > 0) {
                    summary.append("\\n\\n");
                }
                
                Integer codeScore = (Integer) codeAnalysis.get("averageScore");
                Integer totalCodes = (Integer) codeAnalysis.get("totalCodes");
                Integer successfulCodes = (Integer) codeAnalysis.get("successfulCodes");
                
                if (codeScore != null && codeScore > 0) {
                    // 如果有基础问答得分，计算加权平均
                    if (overallScore > 0) {
                        overallScore = (int) Math.round((overallScore + codeScore) / 2.0);
                    } else {
                        overallScore = codeScore;
                    }
                }
                
                summary.append("代码实操分析：")
                       .append("共完成").append(totalCodes).append("道代码题，")
                       .append("成功通过").append(successfulCodes).append("道，")
                       .append("平均得分").append(codeScore).append("分。");
                
                String codeDetails = (String) codeAnalysis.get("details");
                if (codeDetails != null && !codeDetails.trim().isEmpty()) {
                    summary.append(" ").append(codeDetails);
                }
            }
            
            if (summary.length() == 0) {
                summary.append("技术能力评估正在进行中，请稍后查看详细分析结果。");
            }
            
            result.put("basicQaAnalysis", basicQaAnalysis);
            result.put("codeAnalysis", codeAnalysis);
            result.put("overallScore", overallScore);
            result.put("summary", summary.toString());
            
            log.info("技术分析结果获取完成 sessionId={}, overallScore={}", sessionId, overallScore);
            return result;
            
        } catch (Exception e) {
            log.error("获取技术分析结果失败 sessionId={}", sessionId, e);
            result.put("basicQaAnalysis", null);
            result.put("codeAnalysis", null);
            result.put("overallScore", 0);
            result.put("summary", "获取分析数据失败，请稍后重试");
            return result;
        }
    }

    // 保持向后兼容的重载方法
    @Override
    @Transactional
    public ComprehensiveReport generateAndUpdateAbilityMatrix(Long sessionId, Long userId) {
        return generateAndUpdateAbilityMatrix(sessionId, userId, false);
    }

    @Override
    @Transactional
    public ComprehensiveReport generateAndUpdateAbilityMatrix(Long sessionId, Long userId, boolean forceRegenerate) {
        log.info("开始生成能力评估矩阵 sessionId={}, userId={}, forceRegenerate={}", sessionId, userId, forceRegenerate);
        
        try {
            // 检查服务是否可用
            if (!abilityMatrixAnalyzer.isAvailable()) {
                log.warn("能力矩阵分析服务不可用");
                throw new RuntimeException("能力矩阵分析服务暂不可用");
            }
            
            // 获取或创建综合报告
            ComprehensiveReport report = comprehensiveReportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("未找到面试报告，会话ID：" + sessionId));
            
            // 检查是否已经生成过（除非强制重新生成）
            if (!forceRegenerate && report.getAbilityMatrixData() != null && !report.getAbilityMatrixData().trim().isEmpty()) {
                log.info("能力评估矩阵已存在，跳过生成 sessionId={}", sessionId);
                return report;
            }
            
            if (forceRegenerate) {
                log.info("强制重新生成能力评估矩阵 sessionId={}", sessionId);
            }
            
            // 使用AI分析器生成能力矩阵
            SparkX1AbilityMatrixAnalyzer.AbilityMatrixAnalysisResult analysisResult = 
                abilityMatrixAnalyzer.analyzeAbilityMatrix(sessionId, userId);
            
            if (!analysisResult.isSuccess()) {
                log.error("能力矩阵分析失败 sessionId={}, error={}", sessionId, analysisResult.getOverallAssessment());
                throw new RuntimeException("能力矩阵分析失败：" + analysisResult.getOverallAssessment());
            }
            
            // 保存分析结果到数据库
            report.setAbilityMatrixData(analysisResult.toJsonString());
            ComprehensiveReport savedReport = comprehensiveReportRepository.save(report);
            
            log.info("能力评估矩阵生成完成 sessionId={}, 技术能力={}, 学习能力={}", 
                    sessionId, analysisResult.getTechnicalAbility(), analysisResult.getLearningAbility());
            
            return savedReport;
            
        } catch (Exception e) {
            log.error("生成能力评估矩阵失败 sessionId={}", sessionId, e);
            throw new RuntimeException("生成能力评估矩阵失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAbilityMatrixGenerated(Long sessionId) {
        log.debug("检查能力评估矩阵是否已生成 sessionId={}", sessionId);
        
        try {
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (reportOpt.isPresent()) {
                String abilityMatrixData = reportOpt.get().getAbilityMatrixData();
                boolean isGenerated = abilityMatrixData != null && !abilityMatrixData.trim().isEmpty();
                log.debug("能力评估矩阵生成状态 sessionId={}, isGenerated={}", sessionId, isGenerated);
                return isGenerated;
            }
            log.debug("未找到综合报告 sessionId={}", sessionId);
            return false;
        } catch (Exception e) {
            log.error("检查能力评估矩阵状态失败 sessionId={}", sessionId, e);
            return false;
        }
    }

    // 保持向后兼容的重载方法
    @Override
    @Transactional
    public ComprehensiveReport generateAndUpdateComprehensiveAnalysis(Long sessionId, Long userId) {
        return generateAndUpdateComprehensiveAnalysis(sessionId, userId, false);
    }

    @Override
    @Transactional
    public ComprehensiveReport generateAndUpdateComprehensiveAnalysis(Long sessionId, Long userId, boolean forceRegenerate) {
        log.info("开始生成综合分析简报 sessionId={}, userId={}, forceRegenerate={}", sessionId, userId, forceRegenerate);
        
        try {
            // 检查分析器是否可用
            if (!comprehensiveAnalyzer.isAvailable()) {
                log.warn("综合分析器不可用，无法生成分析简报");
                throw new RuntimeException("综合分析器服务暂不可用");
            }
            
            // 获取或创建综合报告
            ComprehensiveReport report = comprehensiveReportRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("未找到会话 " + sessionId + " 的综合报告"));
            
            // 检查是否已经生成过综合分析（除非强制重新生成）
            if (!forceRegenerate && report.getComprehensiveAnalysis() != null && !report.getComprehensiveAnalysis().trim().isEmpty()) {
                log.info("综合分析简报已存在，直接返回 sessionId={}", sessionId);
                return report;
            }
            
            if (forceRegenerate) {
                log.info("强制重新生成综合分析简报 sessionId={}", sessionId);
            }
            
            log.info("调用AI分析器生成综合分析简报...");
            String analysisResultJson = comprehensiveAnalyzer.analyzeComprehensiveReport(sessionId, userId);
            
            if (analysisResultJson == null || analysisResultJson.trim().isEmpty()) {
                throw new RuntimeException("AI分析器返回空结果");
            }
            
            // 保存分析结果到数据库
            report.setComprehensiveAnalysis(analysisResultJson);
            ComprehensiveReport savedReport = comprehensiveReportRepository.save(report);
            
            log.info("综合分析简报生成完成 sessionId={}", sessionId);
            
            return savedReport;
            
        } catch (Exception e) {
            log.error("生成综合分析简报失败 sessionId={}", sessionId, e);
            throw new RuntimeException("生成综合分析简报失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean isComprehensiveAnalysisGenerated(Long sessionId) {
        log.debug("检查综合分析简报是否已生成 sessionId={}", sessionId);
        
        try {
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (reportOpt.isPresent()) {
                String comprehensiveAnalysis = reportOpt.get().getComprehensiveAnalysis();
                boolean isGenerated = comprehensiveAnalysis != null && !comprehensiveAnalysis.trim().isEmpty();
                log.debug("综合分析简报生成状态 sessionId={}, isGenerated={}", sessionId, isGenerated);
                return isGenerated;
            }
            log.debug("未找到综合报告 sessionId={}", sessionId);
            return false;
        } catch (Exception e) {
            log.error("检查综合分析简报状态失败 sessionId={}", sessionId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getComprehensiveAnalysis(Long sessionId) {
        log.info("获取综合分析简报数据 sessionId={}", sessionId);
        
        try {
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (reportOpt.isPresent()) {
                ComprehensiveReport report = reportOpt.get();
                String comprehensiveAnalysisJson = report.getComprehensiveAnalysis();
                
                log.info("数据库中的综合分析简报原始数据 sessionId={}, 数据长度={}, 数据内容前200字符={}", 
                    sessionId, 
                    comprehensiveAnalysisJson != null ? comprehensiveAnalysisJson.length() : 0,
                    comprehensiveAnalysisJson != null && comprehensiveAnalysisJson.length() > 200 ? 
                        comprehensiveAnalysisJson.substring(0, 200) + "..." : comprehensiveAnalysisJson);
                
                if (comprehensiveAnalysisJson != null && !comprehensiveAnalysisJson.trim().isEmpty()) {
                    // 解析JSON字符串为Map
                    Map<String, Object> analysisData = JSON.parseObject(comprehensiveAnalysisJson, Map.class);
                    log.info("成功解析综合分析简报JSON数据 sessionId={}, 解析后的数据结构keys={}", sessionId, analysisData != null ? analysisData.keySet() : "null");
                    log.info("返回给Controller的完整数据 sessionId={}, analysisData={}", sessionId, analysisData);
                    return analysisData;
                } else {
                    log.warn("综合分析简报数据为空 sessionId={}", sessionId);
                    return null;
                }
            } else {
                log.warn("未找到会话的综合报告 sessionId={}", sessionId);
                return null;
            }
        } catch (Exception e) {
            log.error("获取综合分析简报数据失败 sessionId={}", sessionId, e);
            return null;
        }
    }

    @Override
    public ComprehensiveReport generateAndUpdatePersonalizedSuggestions(Long sessionId, Long userId) {
        return generateAndUpdatePersonalizedSuggestions(sessionId, userId, false);
    }

    @Override
    public ComprehensiveReport generateAndUpdatePersonalizedSuggestions(Long sessionId, Long userId, boolean forceRegenerate) {
        log.info("开始生成个性化建议 sessionId={}, userId={}, forceRegenerate={}", sessionId, userId, forceRegenerate);
        
        try {
            // 检查分析器是否可用
            if (!personalizedSuggestionsAnalyzer.isAvailable()) {
                log.warn("个性化建议分析器不可用，无法生成个性化建议");
                throw new RuntimeException("个性化建议分析器服务暂不可用");
            }
            
            // 获取或创建综合报告
            ComprehensiveReport report = comprehensiveReportRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("未找到会话 " + sessionId + " 的综合报告"));
            
            // 检查是否已经生成过个性化建议（除非强制重新生成）
            if (!forceRegenerate && report.getImprovements() != null && !report.getImprovements().trim().isEmpty()) {
                log.info("个性化建议已存在，直接返回 sessionId={}", sessionId);
                return report;
            }
            
            if (forceRegenerate) {
                log.info("强制重新生成个性化建议 sessionId={}", sessionId);
            }
            
            // 收集面试数据用于个性化建议分析
            Map<String, Object> interviewData = collectInterviewDataForPersonalizedSuggestions(sessionId);
            
            log.info("调用AI分析器生成个性化建议...");
            String suggestionsResultJson = personalizedSuggestionsAnalyzer.analyzePersonalizedSuggestions(sessionId, userId, interviewData);
            
            if (suggestionsResultJson == null || suggestionsResultJson.trim().isEmpty()) {
                throw new RuntimeException("AI分析器返回空结果");
            }
            
            // 保存分析结果到数据库
            report.setImprovements(suggestionsResultJson);
            ComprehensiveReport savedReport = comprehensiveReportRepository.save(report);
            
            log.info("个性化建议生成完成 sessionId={}", sessionId);
            
            return savedReport;
            
        } catch (Exception e) {
            log.error("生成个性化建议失败 sessionId={}", sessionId, e);
            throw new RuntimeException("生成个性化建议失败：" + e.getMessage(), e);
        }
    }

    @Override
    public boolean isPersonalizedSuggestionsGenerated(Long sessionId) {
        log.debug("检查个性化建议是否已生成 sessionId={}", sessionId);
        
        try {
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (reportOpt.isPresent()) {
                String improvements = reportOpt.get().getImprovements();
                boolean isGenerated = improvements != null && !improvements.trim().isEmpty();
                log.debug("个性化建议生成状态 sessionId={}, isGenerated={}", sessionId, isGenerated);
                return isGenerated;
            }
            log.debug("未找到综合报告 sessionId={}", sessionId);
            return false;
        } catch (Exception e) {
            log.error("检查个性化建议状态失败 sessionId={}", sessionId, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getPersonalizedSuggestions(Long sessionId) {
        log.info("获取个性化建议数据 sessionId={}", sessionId);
        
        try {
            Optional<ComprehensiveReport> reportOpt = comprehensiveReportRepository.findBySessionId(sessionId);
            if (reportOpt.isPresent()) {
                ComprehensiveReport report = reportOpt.get();
                String improvementsJson = report.getImprovements();
                
                log.info("数据库中的个性化建议原始数据 sessionId={}, 数据长度={}, 数据内容前200字符={}", 
                    sessionId, 
                    improvementsJson != null ? improvementsJson.length() : 0,
                    improvementsJson != null && improvementsJson.length() > 200 ? 
                        improvementsJson.substring(0, 200) + "..." : improvementsJson);
                
                if (improvementsJson != null && !improvementsJson.trim().isEmpty()) {
                    // 提取JSON内容（去除markdown代码块标记）
                    String cleanJson = extractJsonFromMarkdown(improvementsJson);
                    if (cleanJson != null && !cleanJson.trim().isEmpty()) {
                        // 解析JSON字符串为Map
                        Map<String, Object> suggestionsData = JSON.parseObject(cleanJson, Map.class);
                        log.info("成功解析个性化建议JSON数据 sessionId={}, 解析后的数据结构keys={}", sessionId, suggestionsData != null ? suggestionsData.keySet() : "null");
                        log.info("返回给Controller的完整数据 sessionId={}, suggestionsData={}", sessionId, suggestionsData);
                        return suggestionsData;
                    } else {
                        log.warn("无法从markdown中提取有效JSON内容 sessionId={}", sessionId);
                        return null;
                    }
                } else {
                    log.warn("个性化建议数据为空 sessionId={}", sessionId);
                    return null;
                }
            } else {
                log.warn("未找到会话的综合报告 sessionId={}", sessionId);
                return null;
            }
        } catch (Exception e) {
            log.error("获取个性化建议数据失败 sessionId={}", sessionId, e);
            return null;
                }
    }
    
    /**
     * 从markdown代码块中提取JSON内容
     */
    private String extractJsonFromMarkdown(String markdownContent) {
        if (markdownContent == null || markdownContent.trim().isEmpty()) {
            return null;
        }
        
        String content = markdownContent.trim();
        
        // 检查是否包含markdown代码块标记
        if (content.startsWith("```json") && content.endsWith("```")) {
            // 去除开头的```json和结尾的```
            content = content.substring(7); // 去除"```json"
            content = content.substring(0, content.length() - 3); // 去除结尾的"```"
            return content.trim();
        } else if (content.startsWith("```") && content.endsWith("```")) {
            // 去除开头的```和结尾的```
            int firstNewline = content.indexOf('\n');
            if (firstNewline != -1) {
                content = content.substring(firstNewline + 1); // 去除第一行的```xxx
            } else {
                content = content.substring(3); // 去除```
            }
            content = content.substring(0, content.length() - 3); // 去除结尾的```
            return content.trim();
        } else {
            // 没有markdown标记，直接返回
            return content;
        }
    }
    
    /**
     * 收集面试数据用于个性化建议分析
     */
    private Map<String, Object> collectInterviewDataForPersonalizedSuggestions(Long sessionId) {
        Map<String, Object> interviewData = new HashMap<>();
        
        try {
            // 获取基础问答分析
            ComprehensiveReport report = comprehensiveReportRepository.findBySessionId(sessionId)
                    .orElseThrow(() -> new RuntimeException("未找到会话 " + sessionId + " 的综合报告"));
            
            if (report.getBasicQaAnalysis() != null && !report.getBasicQaAnalysis().trim().isEmpty()) {
                try {
                    Map<String, Object> basicQA = JSON.parseObject(report.getBasicQaAnalysis(), Map.class);
                    interviewData.put("basicQAAnalysis", basicQA);
                } catch (Exception e) {
                    log.warn("解析基础问答分析数据失败 sessionId={}", sessionId, e);
                }
            }
            
            // 场景面试分析字段不存在于数据库中，已删除相关代码
            

            
            // 获取能力矩阵分析
            if (report.getAbilityMatrixData() != null && !report.getAbilityMatrixData().trim().isEmpty()) {
                try {
                    Map<String, Object> abilityMatrix = JSON.parseObject(report.getAbilityMatrixData(), Map.class);
                    interviewData.put("abilityMatrix", abilityMatrix);
                } catch (Exception e) {
                    log.warn("解析能力矩阵分析数据失败 sessionId={}", sessionId, e);
                }
            }
            
            // 获取综合分析
            if (report.getComprehensiveAnalysis() != null && !report.getComprehensiveAnalysis().trim().isEmpty()) {
                try {
                    Map<String, Object> comprehensive = JSON.parseObject(report.getComprehensiveAnalysis(), Map.class);
                    interviewData.put("comprehensiveAnalysis", comprehensive);
                } catch (Exception e) {
                    log.warn("解析综合分析数据失败 sessionId={}", sessionId, e);
                }
            }
            
            log.info("收集面试数据完成 sessionId={}, 数据项数量={}", sessionId, interviewData.size());
            
        } catch (Exception e) {
            log.error("收集面试数据失败 sessionId={}", sessionId, e);
        }
        
        return interviewData;
    }
    
    /**
     * 获取代码分析结果
     */
    private Map<String, Object> getCodeAnalysisResults(Long sessionId) {
        try {
            // 获取该会话的所有代码执行记录
            List<CodeExecution> codeExecutions = codeExecutionService.findBySessionId(sessionId);
            
            if (codeExecutions == null || codeExecutions.isEmpty()) {
                log.info("会话{}无代码执行记录", sessionId);
                return null;
            }
            
            log.info("会话{}共有{}条代码执行记录", sessionId, codeExecutions.size());
            
            Map<String, Object> codeAnalysis = new HashMap<>();
            int totalCodes = codeExecutions.size();
            int successfulCodes = 0;
            int totalScore = 0;
            StringBuilder details = new StringBuilder();
            
            for (CodeExecution execution : codeExecutions) {
                String aiAnalysisJson = execution.getAiAnalysis();
                
                if (aiAnalysisJson != null && !aiAnalysisJson.trim().isEmpty()) {
                    try {
                        // 解析AI分析结果
                        Map<String, Object> aiAnalysis = JSON.parseObject(aiAnalysisJson);
                        
                        // 获取得分
                        Object scoreObj = aiAnalysis.get("score");
                        int score = 0;
                        if (scoreObj instanceof Number) {
                            score = ((Number) scoreObj).intValue();
                        }
                        totalScore += score;
                        
                        // 获取状态
                        String status = (String) aiAnalysis.get("status");
                        if ("AC".equals(status) || "SUCCESS".equals(execution.getStatus())) {
                            successfulCodes++;
                        }
                        
                        // 构建详细信息
                        if (details.length() > 0) {
                            details.append(" ");
                        }
                        details.append("题目").append(codeExecutions.indexOf(execution) + 1)
                               .append("得分").append(score).append("分");
                        
                        Object passedCountObj = aiAnalysis.get("passedCount");
                        Object totalCountObj = aiAnalysis.get("totalCount");
                        if (passedCountObj instanceof Number && totalCountObj instanceof Number) {
                            int passedCount = ((Number) passedCountObj).intValue();
                            int totalCount = ((Number) totalCountObj).intValue();
                            details.append("(通过").append(passedCount).append("/").append(totalCount).append(")");
                        }
                        details.append("；");
                        
                    } catch (Exception e) {
                        log.warn("解析代码执行AI分析结果失败 sessionId={}, executionId={}", 
                                sessionId, execution.getId(), e);
                    }
                }
            }
            
            int averageScore = totalCodes > 0 ? totalScore / totalCodes : 0;
            
            codeAnalysis.put("totalCodes", totalCodes);
            codeAnalysis.put("successfulCodes", successfulCodes);
            codeAnalysis.put("totalScore", totalScore);
            codeAnalysis.put("averageScore", averageScore);
            codeAnalysis.put("details", details.toString());
            
            log.info("代码分析结果 sessionId={}, 总题数={}, 成功数={}, 平均分={}", 
                    sessionId, totalCodes, successfulCodes, averageScore);
            
            return codeAnalysis;
            
        } catch (Exception e) {
            log.error("获取代码分析结果失败 sessionId={}", sessionId, e);
            return null;
        }
    }
} 