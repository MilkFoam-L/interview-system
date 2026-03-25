package interview.ai.spark;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import interview.config.SparkX1Config;
import interview.dto.FaceExpressionSummaryResponse;
import interview.model.ComprehensiveReport;
import interview.model.IntroductionAnalysis;
import interview.model.ScenarioReport;
import interview.model.entity.AnswerRecord;
import interview.model.entity.Question;
import interview.repository.AnswerRecordRepository;
import interview.repository.ComprehensiveReportRepository;
import interview.repository.IntroductionAnalysisRepository;
import interview.repository.QuestionRepository;
import interview.repository.ScenarioReportRepository;
import interview.service.FaceExpressionService;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 星火X1.5能力评估矩阵分析器
 * 综合面试各环节数据，生成六维能力评估矩阵
 */
@Component
public class SparkX1AbilityMatrixAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(SparkX1AbilityMatrixAnalyzer.class);
    
    @Autowired
    private SparkX1Config config;
    
    @Autowired
    private ComprehensiveReportRepository comprehensiveReportRepository;
    
    @Autowired
    private AnswerRecordRepository answerRecordRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private IntroductionAnalysisRepository introductionAnalysisRepository;
    
    @Autowired
    private ScenarioReportRepository scenarioReportRepository;
    
    @Autowired
    private FaceExpressionService faceExpressionService;
    
    private final OkHttpClient client;
    
    public SparkX1AbilityMatrixAnalyzer() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * 分析并生成能力评估矩阵
     */
    public AbilityMatrixAnalysisResult analyzeAbilityMatrix(Long sessionId, Long userId) {
        try {
            logger.info("开始生成能力评估矩阵，会话ID：{}，用户ID：{}", sessionId, userId);
            
            // 收集各环节数据
            ComprehensiveDataCollection dataCollection = collectComprehensiveData(sessionId, userId);
            
            if (!dataCollection.hasValidData()) {
                logger.warn("未找到足够的面试数据生成能力评估矩阵，会话ID：{}", sessionId);
                return buildErrorResult("未找到足够的面试数据");
            }
            
            // 构建分析提示词
            String prompt = buildAbilityMatrixPrompt(dataCollection);
            
            // 发送请求到星火X1.5
            String response = callSparkX1API(prompt);
            
            // 解析分析结果
            AbilityMatrixAnalysisResult result = parseAnalysisResult(response);
            result.setSessionId(sessionId);
            result.setUserId(userId);
            
            logger.info("能力评估矩阵生成完成，技术能力：{}，学习能力：{}", 
                       result.getTechnicalAbility(), result.getLearningAbility());
            
            return result;
            
        } catch (Exception e) {
            logger.error("能力评估矩阵分析失败", e);
            return buildErrorResult("分析失败：" + e.getMessage());
        }
    }
    
    /**
     * 收集综合数据
     */
    private ComprehensiveDataCollection collectComprehensiveData(Long sessionId, Long userId) {
        ComprehensiveDataCollection collection = new ComprehensiveDataCollection();
        
        try {
            // 1. 获取综合报告基本数据
            ComprehensiveReport report = comprehensiveReportRepository.findBySessionId(sessionId).orElse(null);
            collection.setReport(report);
            
            // 1.1 获取场景报告数据（问答质量）
            ScenarioReport scenarioReport = scenarioReportRepository.findById(sessionId).orElse(null);
            collection.setScenarioReport(scenarioReport);
            
            // 2. 获取基础问答数据
            List<AnswerRecord> basicQARecords = answerRecordRepository
                .findByInterviewIdAndTypeOrderByCreateTime(sessionId, "BASIC_QA");
            collection.setBasicQARecords(basicQARecords);
            
            // 3. 获取题目信息
            if (!basicQARecords.isEmpty()) {
                Map<Long, Question> questionMap = questionRepository
                    .findAllById(basicQARecords.stream().map(AnswerRecord::getQuestionId).collect(Collectors.toList()))
                    .stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
                collection.setQuestionMap(questionMap);
            }
            
            // 4. 获取自我介绍分析数据
            IntroductionAnalysis introAnalysis = introductionAnalysisRepository.findBySessionId(sessionId).orElse(null);
            collection.setIntroductionAnalysis(introAnalysis);
            
            // 5. 获取表情分析数据
            try {
                var expressionSummary = faceExpressionService.getExpressionSummary(sessionId);
                collection.setExpressionData(expressionSummary);
            } catch (Exception e) {
                logger.warn("获取表情分析数据失败：{}", e.getMessage());
            }
            
            logger.info("数据收集完成 - 报告：{}，场景报告：{}，基础问答：{}道，自我介绍：{}，表情分析：{}", 
                       report != null ? "有" : "无",
                       scenarioReport != null ? "有" : "无",
                       basicQARecords.size(),
                       introAnalysis != null ? "有" : "无",
                       collection.getExpressionData() != null ? "有" : "无");
            
        } catch (Exception e) {
            logger.error("收集综合数据失败", e);
        }
        
        return collection;
    }
    
    /**
     * 构建能力评估矩阵分析提示词
     */
    private String buildAbilityMatrixPrompt(ComprehensiveDataCollection data) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位资深的HR专家和心理学评估师，请基于以下面试全程数据，对候选人进行六维能力评估：\n\n");
        
        // 添加基础信息
        prompt.append("=== 面试基础信息 ===\n");
        if (data.getReport() != null) {
            ComprehensiveReport report = data.getReport();
            prompt.append("公司：").append(report.getCompanyName() != null ? report.getCompanyName() : "未知").append("\n");
            prompt.append("职位：").append(report.getPositionName() != null ? report.getPositionName() : "未知").append("\n");
            prompt.append("面试时长：").append(report.getDuration() != null ? report.getDuration() : 0).append("分钟\n");
            prompt.append("综合评分：").append(report.getTotalScore() != null ? report.getTotalScore() : 0).append("分\n");
        }
        prompt.append("\n");
        
        // 添加STAR结构分析数据
        prompt.append("=== STAR结构表现分析 ===\n");
        if (data.getReport() != null) {
            ComprehensiveReport report = data.getReport();
            prompt.append("情境描述(S)：").append(report.getStarSituationScore() != null ? report.getStarSituationScore() : 0).append("分\n");
            prompt.append("任务分析(T)：").append(report.getStarTaskScore() != null ? report.getStarTaskScore() : 0).append("分\n");
            prompt.append("行动执行(A)：").append(report.getStarActionScore() != null ? report.getStarActionScore() : 0).append("分\n");
            prompt.append("结果总结(R)：").append(report.getStarResultScore() != null ? report.getStarResultScore() : 0).append("分\n");
        }
        prompt.append("\n");
        
        // 添加问题回答质量数据
        prompt.append("=== 问题回答质量分析 ===\n");
        if (data.getScenarioReport() != null) {
            ScenarioReport scenarioReport = data.getScenarioReport();
            prompt.append("技术问题：").append(scenarioReport.getTechScore() != null ? scenarioReport.getTechScore() : 0).append("分\n");
            prompt.append("项目经验：").append(scenarioReport.getProjectScore() != null ? scenarioReport.getProjectScore() : 0).append("分\n");
            prompt.append("团队协作：").append(scenarioReport.getTeamScore() != null ? scenarioReport.getTeamScore() : 0).append("分\n");
            prompt.append("沟通表达：").append(scenarioReport.getPlanScore() != null ? scenarioReport.getPlanScore() : 0).append("分\n");
            prompt.append("解决方案：").append(scenarioReport.getAttitudeScore() != null ? scenarioReport.getAttitudeScore() : 0).append("分\n");
        } else {
            prompt.append("无问答质量数据\n");
        }
        prompt.append("\n");
        
        // 添加基础问答数据
        prompt.append("=== 基础知识测评数据 ===\n");
        if (data.getBasicQARecords() != null && !data.getBasicQARecords().isEmpty()) {
            int totalQuestions = data.getBasicQARecords().size();
            int correctAnswers = 0;
            double totalTime = 0;
            
            for (AnswerRecord record : data.getBasicQARecords()) {
                Question question = data.getQuestionMap().get(record.getQuestionId());
                if (question != null && record.getUserAnswer() != null && 
                    record.getUserAnswer().trim().equals(question.getCorrectAnswer().trim())) {
                    correctAnswers++;
                }
                if (record.getTimeUsed() != null) {
                    totalTime += record.getTimeUsed();
                }
            }
            
            double accuracy = totalQuestions > 0 ? (double) correctAnswers / totalQuestions * 100 : 0;
            double avgTime = totalQuestions > 0 ? totalTime / totalQuestions : 0;
            
            prompt.append("总题数：").append(totalQuestions).append("道\n");
            prompt.append("正确数：").append(correctAnswers).append("道\n");
            prompt.append("准确率：").append(String.format("%.1f", accuracy)).append("%\n");
            prompt.append("平均用时：").append(String.format("%.1f", avgTime)).append("秒/题\n");
        } else {
            prompt.append("无基础问答数据\n");
        }
        prompt.append("\n");
        
        // 添加自我介绍分析数据
        prompt.append("=== 自我介绍表现分析 ===\n");
        if (data.getIntroductionAnalysis() != null) {
            IntroductionAnalysis intro = data.getIntroductionAnalysis();
            prompt.append("语言流畅度：").append(intro.getFluencyScore() != null ? intro.getFluencyScore() : 0).append("分\n");
            prompt.append("表达能力：").append(intro.getExpressionScore() != null ? intro.getExpressionScore() : 0).append("分\n");
            prompt.append("内容质量：").append(intro.getContentScore() != null ? intro.getContentScore() : 0).append("分\n");
            prompt.append("综合评分：").append(intro.getOverallScore() != null ? intro.getOverallScore() : 0).append("分\n");
            prompt.append("介绍时长：").append(intro.getDuration() != null ? intro.getDuration() : 0).append("秒\n");
        } else {
            prompt.append("无自我介绍分析数据\n");
        }
        prompt.append("\n");
        
        // 添加表情分析数据
        prompt.append("=== 面试表情表现分析 ===\n");
        if (data.getExpressionData() != null && data.getExpressionData().isSuccess()) {
            FaceExpressionSummaryResponse expressionData = data.getExpressionData();
            prompt.append("主要表情：").append(expressionData.getDominantExpression() != null ? expressionData.getDominantExpression() : "未知").append("\n");
            prompt.append("总采样数：").append(expressionData.getTotalSamples()).append("次\n");
            if (expressionData.getExpressionDistribution() != null) {
                expressionData.getExpressionDistribution().forEach((emotion, count) -> {
                    prompt.append(emotion).append("：").append(count).append("次\n");
                });
            }
        } else {
            prompt.append("无表情分析数据\n");
        }
        prompt.append("\n");
        
        // 添加评估要求
        prompt.append("=== 六维能力评估要求 ===\n");
        prompt.append("请基于以上所有数据，对候选人进行以下六个维度的能力评估（每个维度0-100分）：\n\n");
        
        prompt.append("1. 技术能力 (0-100分):\n");
        prompt.append("   - 基于基础问答准确率、技术问题回答质量\n");
        prompt.append("   - 专业知识掌握程度和技术深度\n");
        prompt.append("   - 解决技术问题的能力\n\n");
        
        prompt.append("2. 学习能力 (0-100分):\n");
        prompt.append("   - 基于知识面广度、反应速度\n");
        prompt.append("   - 对新技术的理解和接受能力\n");
        prompt.append("   - 自我提升和知识更新意识\n\n");
        
        prompt.append("3. 团队协作 (0-100分):\n");
        prompt.append("   - 基于团队协作相关问题的回答质量\n");
        prompt.append("   - 沟通配合能力和团队意识\n");
        prompt.append("   - 冲突处理和协调能力\n\n");
        
        prompt.append("4. 沟通表达 (0-100分):\n");
        prompt.append("   - 基于自我介绍的表达能力和语言流畅度\n");
        prompt.append("   - STAR结构的逻辑性和清晰度\n");
        prompt.append("   - 问题理解和回答的准确性\n\n");
        
        prompt.append("5. 创新思维 (0-100分):\n");
        prompt.append("   - 基于解决方案的创新性和独特性\n");
        prompt.append("   - 思维的灵活性和开放性\n");
        prompt.append("   - 对问题的深入思考和创新见解\n\n");
        
        prompt.append("6. 抗压能力 (0-100分):\n");
        prompt.append("   - 基于面试过程中的表情稳定性\n");
        prompt.append("   - 回答问题时的冷静和从容程度\n");
        prompt.append("   - 面对挑战性问题的应对能力\n\n");
        
        prompt.append("请以JSON格式返回评估结果：\n");
        prompt.append("{\n");
        prompt.append("  \"technicalAbility\": 技术能力评分(0-100),\n");
        prompt.append("  \"learningAbility\": 学习能力评分(0-100),\n");
        prompt.append("  \"teamworkAbility\": 团队协作评分(0-100),\n");
        prompt.append("  \"communicationAbility\": 沟通表达评分(0-100),\n");
        prompt.append("  \"innovationAbility\": 创新思维评分(0-100),\n");
        prompt.append("  \"stressResistance\": 抗压能力评分(0-100),\n");
        prompt.append("  \"overallAssessment\": \"综合能力评估总结\",\n");
        prompt.append("  \"strengthAreas\": [\"优势领域1\", \"优势领域2\", \"优势领域3\"],\n");
        prompt.append("  \"improvementAreas\": [\"待提升领域1\", \"待提升领域2\"],\n");
        prompt.append("  \"careerSuggestions\": [\"职业发展建议1\", \"职业发展建议2\"],\n");
        prompt.append("  \"abilityAnalysis\": {\n");
        prompt.append("    \"technical\": \"技术能力详细分析\",\n");
        prompt.append("    \"learning\": \"学习能力详细分析\",\n");
        prompt.append("    \"teamwork\": \"团队协作详细分析\",\n");
        prompt.append("    \"communication\": \"沟通表达详细分析\",\n");
        prompt.append("    \"innovation\": \"创新思维详细分析\",\n");
        prompt.append("    \"stress\": \"抗压能力详细分析\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        
        prompt.append("评估要求：\n");
        prompt.append("- 评分要客观公正，基于实际面试表现数据\n");
        prompt.append("- 分析要深入具体，有理有据\n");
        prompt.append("- 建议要有针对性和可操作性\n");
        prompt.append("- 充分考虑不同维度数据的权重和相关性\n");
        prompt.append("- 如果某个环节数据缺失，基于现有数据进行合理推断\n");
        
        return prompt.toString();
    }
    
    /**
     * 调用星火X1.5开放API
     */
    private String callSparkX1API(String prompt) throws IOException {
        // 构建请求体
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", config.getModel());
        jsonObject.put("stream", false);
        jsonObject.put("max_tokens", config.getMaxTokens());
        jsonObject.put("temperature", 0.3);

        // 创建消息数组
        JSONArray messagesArray = new JSONArray();
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", prompt);
        messagesArray.add(messageObject);
        jsonObject.put("messages", messagesArray);
        
        logger.debug("发送能力矩阵分析请求到星火X1.5：{}", config.getHostUrl());
        
        // 构建HTTP请求
        RequestBody body = RequestBody.create(jsonObject.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(config.getHostUrl())
                .addHeader("Authorization", "Bearer " + config.getApiPassword())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
        
        // 发送请求
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("星火X1.5 API调用失败，状态码：{}", response.code());
                throw new IOException("API调用失败，状态码：" + response.code());
            }
            
            String responseBody = response.body().string();
            logger.debug("收到能力矩阵分析响应，长度：{} 字符", responseBody.length());
            
            return responseBody;
        }
    }
    
    /**
     * 解析分析结果
     */
    private AbilityMatrixAnalysisResult parseAnalysisResult(String responseBody) {
        try {
            JSONObject response = JSON.parseObject(responseBody);
            
            if (response.getJSONArray("choices") == null || response.getJSONArray("choices").isEmpty()) {
                logger.warn("星火X1.5响应格式错误：{}", responseBody);
                return buildErrorResult("AI模型响应格式错误");
            }
            
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message.getString("content");
            
            logger.debug("AI返回能力矩阵分析内容：{}", content);
            
            // 提取JSON部分
            String jsonContent = extractJsonFromResponse(content);
            
            if (jsonContent == null) {
                logger.warn("无法从响应中提取JSON：{}", content);
                return buildErrorResult("AI模型返回格式错误");
            }
            
            // 解析JSON
            JSONObject jsonObject = JSON.parseObject(jsonContent);
            
            AbilityMatrixAnalysisResult result = new AbilityMatrixAnalysisResult();
            
            // 解析六维能力评分
            result.setTechnicalAbility(parseScore(jsonObject, "technicalAbility"));
            result.setLearningAbility(parseScore(jsonObject, "learningAbility"));
            result.setTeamworkAbility(parseScore(jsonObject, "teamworkAbility"));
            result.setCommunicationAbility(parseScore(jsonObject, "communicationAbility"));
            result.setInnovationAbility(parseScore(jsonObject, "innovationAbility"));
            result.setStressResistance(parseScore(jsonObject, "stressResistance"));
            
            // 解析分析内容
            result.setOverallAssessment(jsonObject.getString("overallAssessment"));
            
            // 解析数组字段
            if (jsonObject.containsKey("strengthAreas")) {
                JSONArray strengths = jsonObject.getJSONArray("strengthAreas");
                if (strengths != null) {
                    result.setStrengthAreas(strengths.toJavaList(String.class));
                }
            }
            
            if (jsonObject.containsKey("improvementAreas")) {
                JSONArray improvements = jsonObject.getJSONArray("improvementAreas");
                if (improvements != null) {
                    result.setImprovementAreas(improvements.toJavaList(String.class));
                }
            }
            
            if (jsonObject.containsKey("careerSuggestions")) {
                JSONArray suggestions = jsonObject.getJSONArray("careerSuggestions");
                if (suggestions != null) {
                    result.setCareerSuggestions(suggestions.toJavaList(String.class));
                }
            }
            
            // 解析详细分析
            if (jsonObject.containsKey("abilityAnalysis")) {
                JSONObject analysis = jsonObject.getJSONObject("abilityAnalysis");
                if (analysis != null) {
                    result.setTechnicalAnalysis(analysis.getString("technical"));
                    result.setLearningAnalysis(analysis.getString("learning"));
                    result.setTeamworkAnalysis(analysis.getString("teamwork"));
                    result.setCommunicationAnalysis(analysis.getString("communication"));
                    result.setInnovationAnalysis(analysis.getString("innovation"));
                    result.setStressAnalysis(analysis.getString("stress"));
                }
            }
            
            result.setSuccess(true);
            
            return result;
                
        } catch (Exception e) {
            logger.error("解析能力矩阵分析结果失败", e);
            return buildErrorResult("解析AI分析结果失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析得分，确保在0-100范围内
     */
    private Integer parseScore(JSONObject jsonObject, String key) {
        try {
            Object scoreObj = jsonObject.get(key);
            if (scoreObj == null) {
                return 50; // 默认中等分数
            }
            
            int score = Integer.parseInt(scoreObj.toString());
            
            // 确保分数在0-100范围内
            if (score < 0) {
                return 0;
            } else if (score > 100) {
                return 100;
            }
            
            return score;
            
        } catch (Exception e) {
            logger.warn("解析得分失败，字段：{}，错误：{}", key, e.getMessage());
            return 50; // 默认中等分数
        }
    }
    
    /**
     * 从响应中提取JSON内容
     */
    private String extractJsonFromResponse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        
        // 查找JSON块
        int jsonStart = content.indexOf("{");
        if (jsonStart == -1) {
            return null;
        }
        
        int jsonEnd = content.lastIndexOf("}");
        if (jsonEnd == -1 || jsonEnd <= jsonStart) {
            return null;
        }
        
        return content.substring(jsonStart, jsonEnd + 1);
    }
    
    /**
     * 构建错误结果
     */
    private AbilityMatrixAnalysisResult buildErrorResult(String errorMessage) {
        AbilityMatrixAnalysisResult result = new AbilityMatrixAnalysisResult();
        result.setSuccess(false);
        result.setOverallAssessment("分析失败：" + errorMessage);
        // 设置默认分数
        result.setTechnicalAbility(50);
        result.setLearningAbility(50);
        result.setTeamworkAbility(50);
        result.setCommunicationAbility(50);
        result.setInnovationAbility(50);
        result.setStressResistance(50);
        return result;
    }
    
    /**
     * 检查服务是否可用
     */
    public boolean isAvailable() {
        return config.getEnabled() && 
               config.getApiPassword() != null && 
               !config.getApiPassword().isEmpty();
    }
    
    /**
     * 综合数据收集类
     */
    public static class ComprehensiveDataCollection {
        private ComprehensiveReport report;
        private ScenarioReport scenarioReport;
        private List<AnswerRecord> basicQARecords;
        private Map<Long, Question> questionMap;
        private IntroductionAnalysis introductionAnalysis;
        private FaceExpressionSummaryResponse expressionData; // 表情分析数据
        
        public boolean hasValidData() {
            return report != null || 
                   scenarioReport != null ||
                   (basicQARecords != null && !basicQARecords.isEmpty()) ||
                   introductionAnalysis != null ||
                   expressionData != null;
        }
        
        // Getters and Setters
        public ComprehensiveReport getReport() { return report; }
        public void setReport(ComprehensiveReport report) { this.report = report; }
        
        public ScenarioReport getScenarioReport() { return scenarioReport; }
        public void setScenarioReport(ScenarioReport scenarioReport) { this.scenarioReport = scenarioReport; }
        
        public List<AnswerRecord> getBasicQARecords() { return basicQARecords; }
        public void setBasicQARecords(List<AnswerRecord> basicQARecords) { this.basicQARecords = basicQARecords; }
        
        public Map<Long, Question> getQuestionMap() { return questionMap; }
        public void setQuestionMap(Map<Long, Question> questionMap) { this.questionMap = questionMap; }
        
        public IntroductionAnalysis getIntroductionAnalysis() { return introductionAnalysis; }
        public void setIntroductionAnalysis(IntroductionAnalysis introductionAnalysis) { this.introductionAnalysis = introductionAnalysis; }
        
        public FaceExpressionSummaryResponse getExpressionData() { return expressionData; }
        public void setExpressionData(FaceExpressionSummaryResponse expressionData) { this.expressionData = expressionData; }
    }
    
    /**
     * 能力评估矩阵分析结果数据结构
     */
    public static class AbilityMatrixAnalysisResult {
        private Long sessionId;                        // 会话ID
        private Long userId;                          // 用户ID
        private Integer technicalAbility;            // 技术能力评分
        private Integer learningAbility;             // 学习能力评分
        private Integer teamworkAbility;             // 团队协作评分
        private Integer communicationAbility;       // 沟通表达评分
        private Integer innovationAbility;          // 创新思维评分
        private Integer stressResistance;           // 抗压能力评分
        private String overallAssessment;           // 综合评估
        private List<String> strengthAreas;         // 优势领域
        private List<String> improvementAreas;      // 待提升领域
        private List<String> careerSuggestions;     // 职业发展建议
        private String technicalAnalysis;           // 技术能力详细分析
        private String learningAnalysis;            // 学习能力详细分析
        private String teamworkAnalysis;            // 团队协作详细分析
        private String communicationAnalysis;       // 沟通表达详细分析
        private String innovationAnalysis;          // 创新思维详细分析
        private String stressAnalysis;              // 抗压能力详细分析
        private boolean success;                     // 是否成功
        
        // Getters and Setters
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Integer getTechnicalAbility() { return technicalAbility; }
        public void setTechnicalAbility(Integer technicalAbility) { this.technicalAbility = technicalAbility; }
        
        public Integer getLearningAbility() { return learningAbility; }
        public void setLearningAbility(Integer learningAbility) { this.learningAbility = learningAbility; }
        
        public Integer getTeamworkAbility() { return teamworkAbility; }
        public void setTeamworkAbility(Integer teamworkAbility) { this.teamworkAbility = teamworkAbility; }
        
        public Integer getCommunicationAbility() { return communicationAbility; }
        public void setCommunicationAbility(Integer communicationAbility) { this.communicationAbility = communicationAbility; }
        
        public Integer getInnovationAbility() { return innovationAbility; }
        public void setInnovationAbility(Integer innovationAbility) { this.innovationAbility = innovationAbility; }
        
        public Integer getStressResistance() { return stressResistance; }
        public void setStressResistance(Integer stressResistance) { this.stressResistance = stressResistance; }
        
        public String getOverallAssessment() { return overallAssessment; }
        public void setOverallAssessment(String overallAssessment) { this.overallAssessment = overallAssessment; }
        
        public List<String> getStrengthAreas() { return strengthAreas; }
        public void setStrengthAreas(List<String> strengthAreas) { this.strengthAreas = strengthAreas; }
        
        public List<String> getImprovementAreas() { return improvementAreas; }
        public void setImprovementAreas(List<String> improvementAreas) { this.improvementAreas = improvementAreas; }
        
        public List<String> getCareerSuggestions() { return careerSuggestions; }
        public void setCareerSuggestions(List<String> careerSuggestions) { this.careerSuggestions = careerSuggestions; }
        
        public String getTechnicalAnalysis() { return technicalAnalysis; }
        public void setTechnicalAnalysis(String technicalAnalysis) { this.technicalAnalysis = technicalAnalysis; }
        
        public String getLearningAnalysis() { return learningAnalysis; }
        public void setLearningAnalysis(String learningAnalysis) { this.learningAnalysis = learningAnalysis; }
        
        public String getTeamworkAnalysis() { return teamworkAnalysis; }
        public void setTeamworkAnalysis(String teamworkAnalysis) { this.teamworkAnalysis = teamworkAnalysis; }
        
        public String getCommunicationAnalysis() { return communicationAnalysis; }
        public void setCommunicationAnalysis(String communicationAnalysis) { this.communicationAnalysis = communicationAnalysis; }
        
        public String getInnovationAnalysis() { return innovationAnalysis; }
        public void setInnovationAnalysis(String innovationAnalysis) { this.innovationAnalysis = innovationAnalysis; }
        
        public String getStressAnalysis() { return stressAnalysis; }
        public void setStressAnalysis(String stressAnalysis) { this.stressAnalysis = stressAnalysis; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        /**
         * 将分析结果转换为JSON字符串，用于存储到数据库
         */
        public String toJsonString() {
            JSONObject json = new JSONObject();
            
            // 六维能力评分
            JSONArray matrixData = new JSONArray();
            matrixData.add(createAbilityItem("技术能力", this.technicalAbility));
            matrixData.add(createAbilityItem("学习能力", this.learningAbility));
            matrixData.add(createAbilityItem("团队协作", this.teamworkAbility));
            matrixData.add(createAbilityItem("沟通表达", this.communicationAbility));
            matrixData.add(createAbilityItem("创新思维", this.innovationAbility));
            matrixData.add(createAbilityItem("抗压能力", this.stressResistance));
            
            json.put("abilityMatrix", matrixData);
            json.put("overallAssessment", this.overallAssessment);
            json.put("strengthAreas", this.strengthAreas);
            json.put("improvementAreas", this.improvementAreas);
            json.put("careerSuggestions", this.careerSuggestions);
            
            // 详细分析
            JSONObject detailedAnalysis = new JSONObject();
            detailedAnalysis.put("technical", this.technicalAnalysis);
            detailedAnalysis.put("learning", this.learningAnalysis);
            detailedAnalysis.put("teamwork", this.teamworkAnalysis);
            detailedAnalysis.put("communication", this.communicationAnalysis);
            detailedAnalysis.put("innovation", this.innovationAnalysis);
            detailedAnalysis.put("stress", this.stressAnalysis);
            json.put("detailedAnalysis", detailedAnalysis);
            
            json.put("success", this.success);
            
            return json.toString();
        }
        
        private JSONObject createAbilityItem(String name, Integer value) {
            JSONObject item = new JSONObject();
            item.put("name", name);
            item.put("value", value != null ? value : 50);
            return item;
        }
    }
}
