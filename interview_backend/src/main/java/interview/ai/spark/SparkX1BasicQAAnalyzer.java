package interview.ai.spark;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import interview.config.SparkX1Config;
import interview.model.entity.AnswerRecord;
import interview.model.entity.Question;
import interview.repository.AnswerRecordRepository;
import interview.repository.QuestionRepository;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 星火X1基础问答分析客户端
 * 使用星火X1开放API对基础问答环节进行智能分析
 */
@Component
public class SparkX1BasicQAAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(SparkX1BasicQAAnalyzer.class);
    
    @Autowired
    private SparkX1Config config;
    
    @Autowired
    private AnswerRecordRepository answerRecordRepository;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    private final OkHttpClient client;
    
    public SparkX1BasicQAAnalyzer() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * 分析基础问答环节
     */
    public BasicQAAnalysisResult analyzeBasicQA(Long sessionId, Long userId) {
        try {
            logger.info("开始分析基础问答环节，会话ID：{}，用户ID：{}", sessionId, userId);
            
            // 获取基础问答的答题记录
            List<AnswerRecord> answerRecords = answerRecordRepository
                .findByInterviewIdAndTypeOrderByCreateTime(sessionId, "BASIC_QA");
            
            if (answerRecords.isEmpty()) {
                logger.warn("未找到基础问答答题记录，会话ID：{}", sessionId);
                return buildErrorResult("未找到基础问答答题记录");
            }
            
            logger.info("找到{}道基础问答环节的答题记录", answerRecords.size());
            
            // 获取题目详情
            Map<Long, Question> questionMap = questionRepository
                .findAllById(answerRecords.stream().map(AnswerRecord::getQuestionId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
            
            // 构建分析提示词
            String prompt = buildAnalysisPrompt(answerRecords, questionMap);
            
            // 发送请求到星火X1
            String response = callSparkX1API(prompt);
            
            // 解析分析结果
            BasicQAAnalysisResult result = parseAnalysisResult(response);
            result.setSessionId(sessionId);
            result.setUserId(userId);
            result.setTotalQuestions(answerRecords.size());
            
            logger.info("基础问答分析完成，技术能力评分：{}", result.getTechnicalScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("基础问答分析失败", e);
            return buildErrorResult("分析失败：" + e.getMessage());
        }
    }
    
    /**
     * 调用星火X1开放API
     */
    private String callSparkX1API(String prompt) throws IOException {
        // 构建请求体
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", "interview_basicqa_analyzer");
        jsonObject.put("model", config.getModel());
        jsonObject.put("stream", false);
        jsonObject.put("max_tokens", config.getMaxTokens());
        
        // 创建消息数组
        JSONArray messagesArray = new JSONArray();
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", prompt);
        messageObject.put("temperature", 0.2); // 降低温度确保结果客观
        messagesArray.add(messageObject);
        jsonObject.put("messages", messagesArray);
        
        logger.debug("发送基础问答分析请求到星火X1：{}", config.getHostUrl());
        
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
                logger.error("星火X1 API调用失败，状态码：{}", response.code());
                throw new IOException("API调用失败，状态码：" + response.code());
            }
            
            String responseBody = response.body().string();
            logger.debug("收到基础问答分析响应，长度：{} 字符", responseBody.length());
            
            return responseBody;
        }
    }
    
    /**
     * 构建基础问答分析提示词
     */
    private String buildAnalysisPrompt(List<AnswerRecord> answerRecords, Map<Long, Question> questionMap) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的技术面试官和能力评估专家，请对以下基础问答环节（10道基础选择题）的答题情况进行深度分析：\n\n");
        
        // 过滤出基础问答类型的题目（只分析选择题，排除代码题）
        List<AnswerRecord> basicQARecords = answerRecords.stream()
            .filter(record -> {
                Question question = questionMap.get(record.getQuestionId());
                return question != null && "basic".equals(question.getType());
            })
            .collect(Collectors.toList());
            
        // 统计基本信息
        int totalQuestions = basicQARecords.size();
        int correctAnswers = 0;
        StringBuilder questionDetails = new StringBuilder();
        
        logger.info("基础问答分析：总答题记录{}道，基础选择题{}道", answerRecords.size(), totalQuestions);
        
        for (int i = 0; i < basicQARecords.size(); i++) {
            AnswerRecord record = basicQARecords.get(i);
            Question question = questionMap.get(record.getQuestionId());
            
            if (question != null) {
                questionDetails.append("题目").append(i + 1).append("：\n");
                questionDetails.append("问题：").append(question.getContent()).append("\n");
                questionDetails.append("正确答案：").append(question.getCorrectAnswer()).append("\n");
                questionDetails.append("用户答案：").append(record.getUserAnswer()).append("\n");
                questionDetails.append("答题用时：").append(record.getTimeUsed()).append("秒\n");
                questionDetails.append("题目标签：").append(question.getTags()).append("\n");
                questionDetails.append("难度级别：").append(question.getDifficulty()).append("\n");
                
                // 简单判断是否正确（这里可以根据实际情况优化判断逻辑）
                if (record.getUserAnswer() != null && 
                    record.getUserAnswer().trim().equals(question.getCorrectAnswer().trim())) {
                    correctAnswers++;
                    questionDetails.append("答题结果：正确✓\n");
                } else {
                    questionDetails.append("答题结果：错误✗\n");
                }
                questionDetails.append("\n");
            }
        }
        
        prompt.append("基础信息：\n");
        prompt.append("基础选择题总数：").append(totalQuestions).append("道\n");
        prompt.append("正确题目数：").append(correctAnswers).append("道\n");
        prompt.append("准确率：").append(String.format("%.1f", totalQuestions > 0 ? (double) correctAnswers / totalQuestions * 100 : 0.0)).append("%\n\n");
        
        prompt.append("详细答题情况：\n");
        prompt.append(questionDetails.toString());
        
        prompt.append("请从以下维度进行专业评估（每个维度0-100分）：\n\n");
        
        prompt.append("1. 技术能力评分 (0-100分):\n");
        prompt.append("   - 基于10道基础选择题的答题准确率和题目难度\n");
        prompt.append("   - 对基础技术概念的掌握程度\n");
        prompt.append("   - 答题的准确性和完整性\n\n");
        
        prompt.append("2. 知识覆盖度 (0-100分):\n");
        prompt.append("   - 在不同技术领域的表现\n");
        prompt.append("   - 知识结构的广度和深度\n");
        prompt.append("   - 对各种技术栈的了解程度\n\n");
        
        prompt.append("3. 反应速度 (0-100分):\n");
        prompt.append("   - 答题用时的合理性\n");
        prompt.append("   - 思考和决策的效率\n");
        prompt.append("   - 对问题的理解和反应能力\n\n");
        
        prompt.append("请以JSON格式返回分析结果，包含以下字段：\n");
        prompt.append("{\n");
        prompt.append("  \"technicalScore\": 技术能力评分(0-100),\n");
        prompt.append("  \"knowledgeCoverage\": 知识覆盖度评分(0-100),\n");
        prompt.append("  \"responseSpeed\": 反应速度评分(0-100),\n");
        prompt.append("  \"overallAssessment\": \"综合评估和总结\",\n");
        prompt.append("  \"strengthAnalysis\": \"优势分析\",\n");
        prompt.append("  \"weaknessAnalysis\": \"不足分析\",\n");
        prompt.append("  \"knowledgeGaps\": [\"知识盲点1\", \"知识盲点2\", \"知识盲点3\"],\n");
        prompt.append("  \"improvementSuggestions\": [\"改进建议1\", \"改进建议2\", \"改进建议3\"],\n");
        prompt.append("  \"techStackAssessment\": {\n");
        prompt.append("    \"frontend\": \"前端技术掌握情况\",\n");
        prompt.append("    \"backend\": \"后端技术掌握情况\",\n");
        prompt.append("    \"database\": \"数据库技术掌握情况\",\n");
        prompt.append("    \"algorithm\": \"算法基础掌握情况\"\n");
        prompt.append("  }\n");
        prompt.append("}\n\n");
        
        prompt.append("评估要求：\n");
        prompt.append("- 评分要客观公正，基于10道基础选择题的实际答题表现\n");
        prompt.append("- 分析要深入具体，避免泛泛而谈\n");
        prompt.append("- 改进建议要有针对性和可操作性\n");
        prompt.append("- 技术评估要区分不同技术栈的掌握程度\n");
        prompt.append("- 注意：本次分析仅针对基础选择题，不包括代码实操题\n");
        
        return prompt.toString();
    }
    
    /**
     * 解析分析结果
     */
    private BasicQAAnalysisResult parseAnalysisResult(String responseBody) {
        try {
            JSONObject response = JSON.parseObject(responseBody);
            
            if (response.getJSONArray("choices") == null || response.getJSONArray("choices").isEmpty()) {
                logger.warn("星火X1响应格式错误：{}", responseBody);
                return buildErrorResult("AI模型响应格式错误");
            }
            
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message.getString("content");
            
            logger.debug("AI返回基础问答分析内容：{}", content);
            
            // 提取JSON部分
            String jsonContent = extractJsonFromResponse(content);
            
            if (jsonContent == null) {
                logger.warn("无法从响应中提取JSON：{}", content);
                return buildErrorResult("AI模型返回格式错误");
            }
            
            // 解析JSON
            JSONObject jsonObject = JSON.parseObject(jsonContent);
            
            BasicQAAnalysisResult result = new BasicQAAnalysisResult();
            
            // 解析各项评分
            result.setTechnicalScore(parseScore(jsonObject, "technicalScore"));
            result.setKnowledgeCoverage(parseScore(jsonObject, "knowledgeCoverage"));
            result.setResponseSpeed(parseScore(jsonObject, "responseSpeed"));
            
            // 解析分析内容
            result.setOverallAssessment(jsonObject.getString("overallAssessment"));
            result.setStrengthAnalysis(jsonObject.getString("strengthAnalysis"));
            result.setWeaknessAnalysis(jsonObject.getString("weaknessAnalysis"));
            
            // 解析数组字段
            if (jsonObject.containsKey("knowledgeGaps")) {
                JSONArray gaps = jsonObject.getJSONArray("knowledgeGaps");
                if (gaps != null) {
                    result.setKnowledgeGaps(gaps.toJavaList(String.class));
                }
            }
            
            if (jsonObject.containsKey("improvementSuggestions")) {
                JSONArray suggestions = jsonObject.getJSONArray("improvementSuggestions");
                if (suggestions != null) {
                    result.setImprovementSuggestions(suggestions.toJavaList(String.class));
                }
            }
            
            // 解析技术栈评估
            if (jsonObject.containsKey("techStackAssessment")) {
                JSONObject techStack = jsonObject.getJSONObject("techStackAssessment");
                if (techStack != null) {
                    result.setFrontendAssessment(techStack.getString("frontend"));
                    result.setBackendAssessment(techStack.getString("backend"));
                    result.setDatabaseAssessment(techStack.getString("database"));
                    result.setAlgorithmAssessment(techStack.getString("algorithm"));
                }
            }
            
            result.setSuccess(true);
            
            return result;
                
        } catch (Exception e) {
            logger.error("解析基础问答分析结果失败", e);
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
                return 0;
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
            return 0;
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
    private BasicQAAnalysisResult buildErrorResult(String errorMessage) {
        BasicQAAnalysisResult result = new BasicQAAnalysisResult();
        result.setSuccess(false);
        result.setOverallAssessment("分析失败：" + errorMessage);
        result.setTechnicalScore(0);
        result.setKnowledgeCoverage(0);
        result.setResponseSpeed(0);
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
     * 基础问答分析结果数据结构
     */
    public static class BasicQAAnalysisResult {
        private Long sessionId;                        // 会话ID
        private Long userId;                          // 用户ID
        private Integer totalQuestions;               // 总题目数
        private Integer technicalScore;               // 技术能力评分
        private Integer knowledgeCoverage;            // 知识覆盖度评分
        private Integer responseSpeed;                // 反应速度评分
        private String overallAssessment;             // 综合评估
        private String strengthAnalysis;              // 优势分析
        private String weaknessAnalysis;              // 不足分析
        private List<String> knowledgeGaps;          // 知识盲点
        private List<String> improvementSuggestions; // 改进建议
        private String frontendAssessment;           // 前端技术评估
        private String backendAssessment;            // 后端技术评估
        private String databaseAssessment;           // 数据库技术评估
        private String algorithmAssessment;          // 算法基础评估
        private boolean success;                      // 是否成功
        
        // Getters and Setters
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public Integer getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
        
        public Integer getTechnicalScore() { return technicalScore; }
        public void setTechnicalScore(Integer technicalScore) { this.technicalScore = technicalScore; }
        
        public Integer getKnowledgeCoverage() { return knowledgeCoverage; }
        public void setKnowledgeCoverage(Integer knowledgeCoverage) { this.knowledgeCoverage = knowledgeCoverage; }
        
        public Integer getResponseSpeed() { return responseSpeed; }
        public void setResponseSpeed(Integer responseSpeed) { this.responseSpeed = responseSpeed; }
        
        public String getOverallAssessment() { return overallAssessment; }
        public void setOverallAssessment(String overallAssessment) { this.overallAssessment = overallAssessment; }
        
        public String getStrengthAnalysis() { return strengthAnalysis; }
        public void setStrengthAnalysis(String strengthAnalysis) { this.strengthAnalysis = strengthAnalysis; }
        
        public String getWeaknessAnalysis() { return weaknessAnalysis; }
        public void setWeaknessAnalysis(String weaknessAnalysis) { this.weaknessAnalysis = weaknessAnalysis; }
        
        public List<String> getKnowledgeGaps() { return knowledgeGaps; }
        public void setKnowledgeGaps(List<String> knowledgeGaps) { this.knowledgeGaps = knowledgeGaps; }
        
        public List<String> getImprovementSuggestions() { return improvementSuggestions; }
        public void setImprovementSuggestions(List<String> improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }
        
        public String getFrontendAssessment() { return frontendAssessment; }
        public void setFrontendAssessment(String frontendAssessment) { this.frontendAssessment = frontendAssessment; }
        
        public String getBackendAssessment() { return backendAssessment; }
        public void setBackendAssessment(String backendAssessment) { this.backendAssessment = backendAssessment; }
        
        public String getDatabaseAssessment() { return databaseAssessment; }
        public void setDatabaseAssessment(String databaseAssessment) { this.databaseAssessment = databaseAssessment; }
        
        public String getAlgorithmAssessment() { return algorithmAssessment; }
        public void setAlgorithmAssessment(String algorithmAssessment) { this.algorithmAssessment = algorithmAssessment; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        /**
         * 将分析结果转换为JSON字符串，用于存储到数据库
         */
        public String toJsonString() {
            JSONObject json = new JSONObject();
            json.put("technicalScore", this.technicalScore);
            json.put("knowledgeCoverage", this.knowledgeCoverage);
            json.put("responseSpeed", this.responseSpeed);
            json.put("overallAssessment", this.overallAssessment);
            json.put("strengthAnalysis", this.strengthAnalysis);
            json.put("weaknessAnalysis", this.weaknessAnalysis);
            json.put("knowledgeGaps", this.knowledgeGaps);
            json.put("improvementSuggestions", this.improvementSuggestions);
            
            JSONObject techStackAssessment = new JSONObject();
            techStackAssessment.put("frontend", this.frontendAssessment);
            techStackAssessment.put("backend", this.backendAssessment);
            techStackAssessment.put("database", this.databaseAssessment);
            techStackAssessment.put("algorithm", this.algorithmAssessment);
            json.put("techStackAssessment", techStackAssessment);
            
            json.put("totalQuestions", this.totalQuestions);
            json.put("success", this.success);
            
            return json.toString();
        }
    }
}
