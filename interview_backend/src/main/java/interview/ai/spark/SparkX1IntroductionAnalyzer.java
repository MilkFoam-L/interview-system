package interview.ai.spark;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import interview.config.SparkX1Config;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * 星火X1.5自我介绍分析客户端
 * 使用星火X1.5开放API对自我介绍进行多维度分析
 */
@Component
public class SparkX1IntroductionAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(SparkX1IntroductionAnalyzer.class);
    
    @Autowired
    private SparkX1Config config;
    
    private final OkHttpClient client;
    
    public SparkX1IntroductionAnalyzer() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * 分析自我介绍
     */
    public IntroductionAnalysisResult analyzeIntroduction(String introductionText, int duration) {
        try {
            logger.info("开始分析自我介绍，文本长度：{}，时长：{}秒", 
                       introductionText.length(), duration);
            
            // 构建分析提示词
            String prompt = buildAnalysisPrompt(introductionText, duration);
            
            // 发送请求到星火X1.5
            String response = callSparkX1API(prompt);
            
            // 解析分析结果
            IntroductionAnalysisResult result = parseAnalysisResult(response);
            
            logger.info("自我介绍分析完成，整体得分：{}", result.getOverallScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("自我介绍分析失败", e);
            return buildErrorResult("分析失败：" + e.getMessage());
        }
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
        
        logger.debug("发送自我介绍分析请求到星火X1.5：{}", config.getHostUrl());
        
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
            logger.debug("收到分析响应，长度：{} 字符", responseBody.length());
            
            return responseBody;
        }
    }
    
    /**
     * 构建自我介绍分析提示词
     */
    private String buildAnalysisPrompt(String introductionText, int duration) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的HR面试官和语言评估专家，请对以下自我介绍进行多维度分析评估：\n\n");
        prompt.append("自我介绍内容：\n").append(introductionText).append("\n\n");
        prompt.append("介绍时长：").append(duration).append("秒\n\n");
        
        prompt.append("请从以下四个维度进行专业评估（每个维度0-10分）：\n\n");
        
        prompt.append("1. 语言流畅度 (0-10分):\n");
        prompt.append("   - 语言表达是否流畅自然\n");
        prompt.append("   - 是否有明显的停顿、重复或语法错误\n");
        prompt.append("   - 语速是否适中，表达是否清晰\n\n");
        
        prompt.append("2. 表达能力 (0-10分):\n");
        prompt.append("   - 逻辑结构是否清晰\n");
        prompt.append("   - 语言组织能力\n");
        prompt.append("   - 表达的条理性和说服力\n\n");
        
        prompt.append("3. 内容质量 (0-10分):\n");
        prompt.append("   - 信息的完整性和相关性\n");
        prompt.append("   - 是否包含教育背景、技能经验等关键信息\n");
        prompt.append("   - 内容的专业性和匹配度\n");
        prompt.append("   - 是否突出了个人优势和特色\n\n");
        
        prompt.append("4. 整体印象 (0-10分):\n");
        prompt.append("   - 综合表现的专业性\n");
        prompt.append("   - 给面试官留下的整体印象\n");
        prompt.append("   - 自信程度和职业素养体现\n\n");
        
        prompt.append("请以JSON格式返回分析结果，包含以下字段：\n");
        prompt.append("{\n");
        prompt.append("  \"fluencyScore\": 语言流畅度得分(0-10, 保留1位小数),\n");
        prompt.append("  \"expressionScore\": 表达能力得分(0-10, 保留1位小数),\n");
        prompt.append("  \"contentScore\": 内容质量得分(0-10, 保留1位小数),\n");
        prompt.append("  \"overallScore\": 整体得分(0-10, 保留1位小数),\n");
        prompt.append("  \"analysisResult\": \"详细的分析结果和改进建议，包括优点和需要改进的地方\",\n");
        prompt.append("  \"keyStrengths\": [\"优势点1\", \"优势点2\", \"优势点3\"],\n");
        prompt.append("  \"improvementSuggestions\": [\"改进建议1\", \"改进建议2\", \"改进建议3\"]\n");
        prompt.append("}\n\n");
        
        prompt.append("注意事项：\n");
        prompt.append("- 评分要客观公正，基于实际表现\n");
        prompt.append("- 分析要具体深入，避免空泛的评价\n");
        prompt.append("- 改进建议要具有针对性和可操作性\n");
        prompt.append("- 整体得分应该是前三个维度的综合体现，不是简单平均\n");
        
        return prompt.toString();
    }
    
    /**
     * 解析分析结果
     */
    private IntroductionAnalysisResult parseAnalysisResult(String responseBody) {
        try {
            JSONObject response = JSON.parseObject(responseBody);
            
            if (response.getJSONArray("choices") == null || response.getJSONArray("choices").isEmpty()) {
                logger.warn("星火X1.5响应格式错误：{}", responseBody);
                return buildErrorResult("AI模型响应格式错误");
            }
            
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message.getString("content");
            
            logger.debug("AI返回分析内容：{}", content);
            
            // 提取JSON部分
            String jsonContent = extractJsonFromResponse(content);
            
            if (jsonContent == null) {
                logger.warn("无法从响应中提取JSON：{}", content);
                return buildErrorResult("AI模型返回格式错误");
            }
            
            // 解析JSON
            JSONObject jsonObject = JSON.parseObject(jsonContent);
            
            IntroductionAnalysisResult result = new IntroductionAnalysisResult();
            
            // 解析各项得分
            result.setFluencyScore(parseScore(jsonObject, "fluencyScore"));
            result.setExpressionScore(parseScore(jsonObject, "expressionScore"));
            result.setContentScore(parseScore(jsonObject, "contentScore"));
            result.setOverallScore(parseScore(jsonObject, "overallScore"));
            
            // 解析分析结果
            result.setAnalysisResult(jsonObject.getString("analysisResult"));
            
            // 解析优势和建议（可选字段）
            if (jsonObject.containsKey("keyStrengths")) {
                JSONArray strengths = jsonObject.getJSONArray("keyStrengths");
                if (strengths != null) {
                    result.setKeyStrengths(strengths.toJavaList(String.class));
                }
            }
            
            if (jsonObject.containsKey("improvementSuggestions")) {
                JSONArray suggestions = jsonObject.getJSONArray("improvementSuggestions");
                if (suggestions != null) {
                    result.setImprovementSuggestions(suggestions.toJavaList(String.class));
                }
            }
            
            result.setSuccess(true);
            
            return result;
                
        } catch (Exception e) {
            logger.error("解析自我介绍分析结果失败", e);
            return buildErrorResult("解析AI分析结果失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析得分，确保在0-10范围内并保留1位小数
     */
    private BigDecimal parseScore(JSONObject jsonObject, String key) {
        try {
            Object scoreObj = jsonObject.get(key);
            if (scoreObj == null) {
                return BigDecimal.ZERO;
            }
            
            BigDecimal score;
            if (scoreObj instanceof Number) {
                score = new BigDecimal(scoreObj.toString());
            } else {
                score = new BigDecimal(scoreObj.toString());
            }
            
            // 确保分数在0-10范围内
            if (score.compareTo(BigDecimal.ZERO) < 0) {
                score = BigDecimal.ZERO;
            } else if (score.compareTo(BigDecimal.TEN) > 0) {
                score = BigDecimal.TEN;
            }
            
            // 保留1位小数
            return score.setScale(1, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            logger.warn("解析得分失败，字段：{}，错误：{}", key, e.getMessage());
            return BigDecimal.ZERO;
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
    private IntroductionAnalysisResult buildErrorResult(String errorMessage) {
        IntroductionAnalysisResult result = new IntroductionAnalysisResult();
        result.setSuccess(false);
        result.setAnalysisResult("分析失败：" + errorMessage);
        result.setFluencyScore(BigDecimal.ZERO);
        result.setExpressionScore(BigDecimal.ZERO);
        result.setContentScore(BigDecimal.ZERO);
        result.setOverallScore(BigDecimal.ZERO);
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
     * 自我介绍分析结果数据结构
     */
    public static class IntroductionAnalysisResult {
        private BigDecimal fluencyScore;      // 语言流畅度得分
        private BigDecimal expressionScore;   // 表达能力得分
        private BigDecimal contentScore;      // 内容质量得分
        private BigDecimal overallScore;      // 整体得分
        private String analysisResult;        // 分析结果
        private java.util.List<String> keyStrengths;           // 关键优势
        private java.util.List<String> improvementSuggestions; // 改进建议
        private boolean success;              // 是否成功
        
        // Getters and Setters
        public BigDecimal getFluencyScore() { return fluencyScore; }
        public void setFluencyScore(BigDecimal fluencyScore) { this.fluencyScore = fluencyScore; }
        
        public BigDecimal getExpressionScore() { return expressionScore; }
        public void setExpressionScore(BigDecimal expressionScore) { this.expressionScore = expressionScore; }
        
        public BigDecimal getContentScore() { return contentScore; }
        public void setContentScore(BigDecimal contentScore) { this.contentScore = contentScore; }
        
        public BigDecimal getOverallScore() { return overallScore; }
        public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }
        
        public String getAnalysisResult() { return analysisResult; }
        public void setAnalysisResult(String analysisResult) { this.analysisResult = analysisResult; }
        
        public java.util.List<String> getKeyStrengths() { return keyStrengths; }
        public void setKeyStrengths(java.util.List<String> keyStrengths) { this.keyStrengths = keyStrengths; }
        
        public java.util.List<String> getImprovementSuggestions() { return improvementSuggestions; }
        public void setImprovementSuggestions(java.util.List<String> improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
}

