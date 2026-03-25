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
 * 星火X1.5综合分析简报分析器
 * 综合面试各环节数据，生成AI综合分析简报
 */
@Component
public class SparkX1ComprehensiveAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(SparkX1ComprehensiveAnalyzer.class);
    
    @Autowired
    private SparkX1Config config;
    
    @Autowired
    private ComprehensiveReportRepository comprehensiveReportRepository;
    
    @Autowired
    private AnswerRecordRepository answerRecordRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ScenarioReportRepository scenarioReportRepository;
    
    @Autowired
    private IntroductionAnalysisRepository introductionAnalysisRepository;
    
    @Autowired
    private FaceExpressionService faceExpressionService;
    
    /**
     * 综合面试数据收集类
     */
    private static class ComprehensiveDataCollection {
        public ComprehensiveReport comprehensiveReport;
        public ScenarioReport scenarioReport;
        public List<AnswerRecord> answerRecords;
        public List<Question> questions;
        public IntroductionAnalysis introductionAnalysis;
        public Object expressionSummary;
        
        // 计算平均分数
        public double getAverageScore() {
            if (scenarioReport == null) return 0.0;
            
            double total = 0.0;
            int count = 0;
            
            if (scenarioReport.getTechScore() != null) {
                total += scenarioReport.getTechScore().doubleValue();
                count++;
            }
            if (scenarioReport.getProjectScore() != null) {
                total += scenarioReport.getProjectScore().doubleValue();
                count++;
            }
            if (scenarioReport.getTeamScore() != null) {
                total += scenarioReport.getTeamScore().doubleValue();
                count++;
            }
            if (scenarioReport.getPlanScore() != null) {
                total += scenarioReport.getPlanScore().doubleValue();
                count++;
            }
            if (scenarioReport.getAttitudeScore() != null) {
                total += scenarioReport.getAttitudeScore().doubleValue();
                count++;
            }
            
            return count > 0 ? total / count : 0.0;
        }
        
        // 获取STAR平均分数
        public double getStarAverageScore() {
            if (comprehensiveReport == null) return 0.0;
            
            double total = 0.0;
            int count = 0;
            
            if (comprehensiveReport.getStarSituationScore() != null) {
                total += comprehensiveReport.getStarSituationScore().doubleValue();
                count++;
            }
            if (comprehensiveReport.getStarTaskScore() != null) {
                total += comprehensiveReport.getStarTaskScore().doubleValue();
                count++;
            }
            if (comprehensiveReport.getStarActionScore() != null) {
                total += comprehensiveReport.getStarActionScore().doubleValue();
                count++;
            }
            if (comprehensiveReport.getStarResultScore() != null) {
                total += comprehensiveReport.getStarResultScore().doubleValue();
                count++;
            }
            
            return count > 0 ? total / count : 0.0;
        }
    }
    
    /**
     * 综合分析结果类
     */
    public static class ComprehensiveAnalysisResult {
        private String overallAssessment; // 综合表现评估
        private Integer technicalScore; // 技术能力分数
        private Integer communicationScore; // 沟通表达分数
        private Integer learningScore; // 学习能力分数
        private List<String> advantages; // 面试亮点
        private List<String> improvements; // 改进建议
        private String detailedAnalysis; // 详细分析
        
        // Getters and Setters
        public String getOverallAssessment() { return overallAssessment; }
        public void setOverallAssessment(String overallAssessment) { this.overallAssessment = overallAssessment; }
        
        public Integer getTechnicalScore() { return technicalScore; }
        public void setTechnicalScore(Integer technicalScore) { this.technicalScore = technicalScore; }
        
        public Integer getCommunicationScore() { return communicationScore; }
        public void setCommunicationScore(Integer communicationScore) { this.communicationScore = communicationScore; }
        
        public Integer getLearningScore() { return learningScore; }
        public void setLearningScore(Integer learningScore) { this.learningScore = learningScore; }
        
        public List<String> getAdvantages() { return advantages; }
        public void setAdvantages(List<String> advantages) { this.advantages = advantages; }
        
        public List<String> getImprovements() { return improvements; }
        public void setImprovements(List<String> improvements) { this.improvements = improvements; }
        
        public String getDetailedAnalysis() { return detailedAnalysis; }
        public void setDetailedAnalysis(String detailedAnalysis) { this.detailedAnalysis = detailedAnalysis; }
        
        /**
         * 转换为JSON字符串用于数据库存储
         */
        public String toJsonString() {
            JSONObject json = new JSONObject();
            json.put("overallAssessment", this.overallAssessment);
            json.put("technicalScore", this.technicalScore);
            json.put("communicationScore", this.communicationScore);
            json.put("learningScore", this.learningScore);
            json.put("advantages", this.advantages);
            json.put("improvements", this.improvements);
            json.put("detailedAnalysis", this.detailedAnalysis);
            return json.toJSONString();
        }
    }
    
    /**
     * 分析综合面试表现并生成分析简报
     */
    public String analyzeComprehensiveReport(Long sessionId, Long userId) {
        try {
            logger.info("开始综合分析简报生成，会话ID: {}, 用户ID: {}", sessionId, userId);
            
            // 收集数据
            ComprehensiveDataCollection data = collectComprehensiveData(sessionId, userId);
            if (data == null) {
                logger.warn("未找到足够的面试数据进行综合分析");
                return null;
            }
            
            // 构建AI分析提示词
            String prompt = buildComprehensiveAnalysisPrompt(data);
            logger.debug("构建的AI分析提示词: {}", prompt);
            
            // 调用SparkX1 API
            String responseBody = callSparkX1API(prompt);
            if (responseBody == null) {
                logger.error("SparkX1 API调用失败");
                return null;
            }
            
            // 解析分析结果
            ComprehensiveAnalysisResult result = parseComprehensiveAnalysisResult(responseBody);
            if (result == null) {
                logger.error("综合分析结果解析失败");
                return null;
            }
            
            logger.info("综合分析简报生成成功");
            return result.toJsonString();
            
        } catch (Exception e) {
            logger.error("综合分析简报生成失败", e);
            return null;
        }
    }
    
    /**
     * 收集综合面试数据
     */
    private ComprehensiveDataCollection collectComprehensiveData(Long sessionId, Long userId) {
        try {
            ComprehensiveDataCollection data = new ComprehensiveDataCollection();
            
            // 获取综合报告
            data.comprehensiveReport = comprehensiveReportRepository.findBySessionId(sessionId).orElse(null);
            if (data.comprehensiveReport == null) {
                logger.warn("未找到会话 {} 的综合报告", sessionId);
                return null;
            }
            
            // 获取场景报告 (使用sessionId作为主键)
            data.scenarioReport = scenarioReportRepository.findById(sessionId).orElse(null);
            
            // 获取问答记录 (使用interviewId关联sessionId)
            data.answerRecords = answerRecordRepository.findByInterviewId(sessionId);
            
            // 获取问题信息
            List<Long> questionIds = data.answerRecords.stream()
                    .map(AnswerRecord::getQuestionId)
                    .collect(Collectors.toList());
            data.questions = questionRepository.findAllById(questionIds);
            
            // 获取自我介绍分析
            data.introductionAnalysis = introductionAnalysisRepository.findBySessionId(sessionId).orElse(null);
            
            // 获取表情分析摘要
            try {
                data.expressionSummary = faceExpressionService.getExpressionSummary(sessionId);
            } catch (Exception e) {
                logger.warn("获取表情分析摘要失败: {}", e.getMessage());
                data.expressionSummary = null;
            }
            
            logger.info("数据收集完成: 答题记录{}条, 问题{}个", 
                data.answerRecords.size(), data.questions.size());
            
            return data;
            
        } catch (Exception e) {
            logger.error("收集综合面试数据失败", e);
            return null;
        }
    }
    
    /**
     * 构建综合分析提示词
     */
    private String buildComprehensiveAnalysisPrompt(ComprehensiveDataCollection data) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请基于以下面试数据，生成一份AI综合分析简报。\n\n");
        
        // 基本信息
        prompt.append("## 面试基本信息\n");
        if (data.comprehensiveReport != null) {
            prompt.append("- 公司: ").append(data.comprehensiveReport.getCompanyName()).append("\n");
            prompt.append("- 职位: ").append(data.comprehensiveReport.getPositionName()).append("\n");
            prompt.append("- 面试时长: ").append(data.comprehensiveReport.getDuration()).append("分钟\n");
        }
        
        // STAR结构分析
        prompt.append("\n## STAR结构表现\n");
        if (data.comprehensiveReport != null) {
            prompt.append("- Situation得分: ").append(data.comprehensiveReport.getStarSituationScore()).append("\n");
            prompt.append("- Task得分: ").append(data.comprehensiveReport.getStarTaskScore()).append("\n");
            prompt.append("- Action得分: ").append(data.comprehensiveReport.getStarActionScore()).append("\n");
            prompt.append("- Result得分: ").append(data.comprehensiveReport.getStarResultScore()).append("\n");
            prompt.append("- STAR平均得分: ").append(String.format("%.1f", data.getStarAverageScore())).append("\n");
        }
        
        // 各维度分析
        prompt.append("\n## 各维度分析得分\n");
        if (data.scenarioReport != null) {
            prompt.append("- 技术能力: ").append(data.scenarioReport.getTechScore()).append("\n");
            prompt.append("- 项目经验: ").append(data.scenarioReport.getProjectScore()).append("\n");
            prompt.append("- 团队协作: ").append(data.scenarioReport.getTeamScore()).append("\n");
            prompt.append("- 沟通表达: ").append(data.scenarioReport.getPlanScore()).append("\n");
            prompt.append("- 解决方案: ").append(data.scenarioReport.getAttitudeScore()).append("\n");
            prompt.append("- 综合平均分: ").append(String.format("%.1f", data.getAverageScore())).append("\n");
        }
        
        // 问答表现
        prompt.append("\n## 问答表现分析\n");
        if (!data.answerRecords.isEmpty()) {
            prompt.append("共回答了").append(data.answerRecords.size()).append("个问题:\n");
            
            Map<Long, Question> questionMap = data.questions.stream()
                    .collect(Collectors.toMap(Question::getId, q -> q));
            
            for (AnswerRecord record : data.answerRecords) {
                Question question = questionMap.get(record.getQuestionId());
                if (question != null) {
                    prompt.append("- 问题: ").append(question.getContent()).append("\n");
                    prompt.append("  回答: ").append(record.getUserAnswer() != null ? 
                        record.getUserAnswer().substring(0, Math.min(100, record.getUserAnswer().length())) : "无文本答案")
                        .append(record.getUserAnswer() != null && record.getUserAnswer().length() > 100 ? "..." : "").append("\n");
                }
            }
        }
        
        // 自我介绍分析
        if (data.introductionAnalysis != null) {
            prompt.append("\n## 自我介绍分析\n");
            prompt.append("- 表达流畅度: ").append(data.introductionAnalysis.getFluencyScore()).append("\n");
            prompt.append("- 内容完整性: ").append(data.introductionAnalysis.getContentScore()).append("\n");
            prompt.append("- 表达能力: ").append(data.introductionAnalysis.getExpressionScore()).append("\n");
            prompt.append("- 综合得分: ").append(data.introductionAnalysis.getOverallScore()).append("\n");
            if (data.introductionAnalysis.getAnalysisResult() != null) {
                prompt.append("- 分析结果: ").append(data.introductionAnalysis.getAnalysisResult()).append("\n");
            }
        }
        
        // 表情分析
        if (data.expressionSummary != null) {
            prompt.append("\n## 表情分析\n");
            try {
                FaceExpressionSummaryResponse expressionResponse = (FaceExpressionSummaryResponse) data.expressionSummary;
                prompt.append("- 分析是否成功: ").append(expressionResponse.isSuccess() ? "是" : "否").append("\n");
                prompt.append("- 主要表情: ").append(expressionResponse.getDominantExpression()).append("\n");
                prompt.append("- 总样本数: ").append(expressionResponse.getTotalSamples()).append("\n");
                
                if (expressionResponse.getExpressionDistribution() != null) {
                    prompt.append("- 表情分布: ").append(expressionResponse.getExpressionDistribution().toString()).append("\n");
                }
            } catch (Exception e) {
                logger.warn("解析表情分析数据时出错: {}", e.getMessage());
            }
        }
        
        // 输出要求
        prompt.append("\n## 分析要求\n");
        prompt.append("请根据以上数据生成综合分析简报，包含以下JSON格式的内容：\n");
        prompt.append("{\n");
        prompt.append("  \"overallAssessment\": \"根据面试全程的AI分析，用2-3句话总结候选人的整体表现\",\n");
        prompt.append("  \"technicalScore\": 技术能力得分(0-100整数),\n");
        prompt.append("  \"communicationScore\": 沟通表达得分(0-100整数),\n");
        prompt.append("  \"learningScore\": 学习能力得分(0-100整数),\n");
        prompt.append("  \"advantages\": [\"面试亮点1\", \"面试亮点2\", \"面试亮点3\"],\n");
        prompt.append("  \"improvements\": [\"改进建议1\", \"改进建议2\", \"改进建议3\"],\n");
        prompt.append("  \"detailedAnalysis\": \"详细的综合分析说明\"\n");
        prompt.append("}\n");
        prompt.append("注意：请确保返回有效的JSON格式，分数要合理反映候选人的实际表现水平。");
        
        return prompt.toString();
    }
    
    /**
     * 调用SparkX1 API
     */
    private String callSparkX1API(String prompt) {
        try {
            logger.info("调用SparkX1 API进行综合分析...");
            
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                    .readTimeout(config.getReadTimeout(), TimeUnit.MILLISECONDS)
                    .build();
            
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", config.getModel());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature());
            
            JSONArray messages = new JSONArray();
            JSONObject message = new JSONObject();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBody.put("messages", messages);
            
            Request request = new Request.Builder()
                    .url(config.getHostUrl())
                    .addHeader("Authorization", "Bearer " + config.getApiPassword())
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(MediaType.parse("application/json"), requestBody.toJSONString()))
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("SparkX1 API调用失败: HTTP {}", response.code());
                    return null;
                }
                
                String responseBody = response.body().string();
                logger.debug("SparkX1 API响应: {}", responseBody);
                return responseBody;
            }
            
        } catch (IOException e) {
            logger.error("SparkX1 API调用异常", e);
            return null;
        }
    }
    
    /**
     * 解析综合分析结果
     */
    private ComprehensiveAnalysisResult parseComprehensiveAnalysisResult(String responseBody) {
        try {
            JSONObject response = JSON.parseObject(responseBody);
            JSONArray choices = response.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                logger.error("API响应中未找到choices数组");
                return null;
            }
            
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message.getString("content");
            
            // 提取JSON内容
            String jsonContent = extractJsonFromContent(content);
            if (jsonContent == null) {
                logger.error("无法从API响应中提取JSON内容");
                return null;
            }
            
            JSONObject analysisJson = JSON.parseObject(jsonContent);
            
            ComprehensiveAnalysisResult result = new ComprehensiveAnalysisResult();
            result.setOverallAssessment(analysisJson.getString("overallAssessment"));
            result.setTechnicalScore(analysisJson.getInteger("technicalScore"));
            result.setCommunicationScore(analysisJson.getInteger("communicationScore"));
            result.setLearningScore(analysisJson.getInteger("learningScore"));
            result.setDetailedAnalysis(analysisJson.getString("detailedAnalysis"));
            
            // 解析advantages数组
            JSONArray advantagesArray = analysisJson.getJSONArray("advantages");
            if (advantagesArray != null) {
                result.setAdvantages(advantagesArray.toJavaList(String.class));
            }
            
            // 解析improvements数组
            JSONArray improvementsArray = analysisJson.getJSONArray("improvements");
            if (improvementsArray != null) {
                result.setImprovements(improvementsArray.toJavaList(String.class));
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("解析综合分析结果失败", e);
            return null;
        }
    }
    
    /**
     * 从API响应内容中提取JSON
     */
    private String extractJsonFromContent(String content) {
        if (content == null) return null;
        
        // 查找JSON开始和结束位置
        int startIndex = content.indexOf('{');
        int endIndex = content.lastIndexOf('}');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return content.substring(startIndex, endIndex + 1);
        }
        
        return null;
    }
    
    /**
     * 检查分析器是否可用
     */
    public boolean isAvailable() {
        return config.getEnabled() && config.getApiPassword() != null && !config.getApiPassword().trim().isEmpty();
    }
}
