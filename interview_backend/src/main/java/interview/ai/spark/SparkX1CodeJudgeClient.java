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
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 星火X1代码判题客户端
 * 使用星火X1开放API进行代码判题
 */
@Component
public class SparkX1CodeJudgeClient {
    
    private static final Logger logger = LoggerFactory.getLogger(SparkX1CodeJudgeClient.class);
    
    @Autowired
    private SparkX1Config config;
    
    private final OkHttpClient client;
    
    public SparkX1CodeJudgeClient() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * 执行代码判题
     */
    public SparkX1Model.JudgeResult judgeCode(SparkX1Model.CodeJudgeRequest request) {
        try {
            logger.info("开始执行代码判题，语言：{}，题目：{}", 
                       request.getLanguage(), 
                       request.getProblemDescription().substring(0, Math.min(50, request.getProblemDescription().length())));
            
            // 构建提示词
            String prompt = buildCodeJudgePrompt(request);
            
            // 发送请求到星火X1
            String response = callSparkX1API(prompt);
            
            // 解析结果
            SparkX1Model.JudgeResult result = parseJudgeResult(response);
            
            logger.info("代码判题完成，结果：{}，得分：{}", result.getResultType(), result.getScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("代码判题失败", e);
            return buildErrorResult("系统错误：" + e.getMessage());
        }
    }
    
    /**
     * 快速判题（简化版）
     */
    public SparkX1Model.JudgeResult quickJudge(String problemDescription, 
                                               String userCode, 
                                               String language) {
        try {
            logger.info("开始快速判题，语言：{}", language);
            
            // 构建简化提示词
            String prompt = buildSimpleCodeJudgePrompt(problemDescription, userCode, language);
            
            // 发送请求到星火X1
            String response = callSparkX1API(prompt);
            
            // 解析结果
            SparkX1Model.JudgeResult result = parseJudgeResult(response);
            
            logger.info("快速判题完成，结果：{}", result.getResultType());
            
            return result;
            
        } catch (Exception e) {
            logger.error("快速判题失败", e);
            return buildErrorResult("系统错误：" + e.getMessage());
        }
    }
    
    /**
     * 调用星火X1开放API
     */
    private String callSparkX1API(String prompt) throws IOException {
        // 构建请求体
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("user", "interview_system_user");
        jsonObject.put("model", config.getModel());
        jsonObject.put("stream", false);
        jsonObject.put("max_tokens", config.getMaxTokens());
        
        // 创建消息数组
        JSONArray messagesArray = new JSONArray();
        JSONObject messageObject = new JSONObject();
        messageObject.put("role", "user");
        messageObject.put("content", prompt);
        messageObject.put("temperature", config.getTemperature());
        messagesArray.add(messageObject);
        jsonObject.put("messages", messagesArray);
        
        logger.debug("发送请求到星火X1：{}", config.getHostUrl());
        logger.debug("请求内容长度：{} 字符", jsonObject.toString().length());
        
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
            logger.debug("收到响应，长度：{} 字符", responseBody.length());
            
            return responseBody;
        }
    }
    
    /**
     * 解析判题结果
     */
    private SparkX1Model.JudgeResult parseJudgeResult(String responseBody) {
        try {
            JSONObject response = JSON.parseObject(responseBody);
            
            if (response.getJSONArray("choices") == null || response.getJSONArray("choices").isEmpty()) {
                logger.warn("星火X1响应格式错误：{}", responseBody);
                return buildErrorResult("AI模型响应格式错误");
            }
            
            JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            String content = message.getString("content");
            
            logger.debug("AI返回内容：{}", content);
            
            // 提取JSON部分
            String jsonContent = extractJsonFromResponse(content);
            
            if (jsonContent == null) {
                logger.warn("无法从响应中提取JSON：{}", content);
                return buildErrorResult("AI模型返回格式错误");
            }
            
            // 解析JSON
            JSONObject jsonObject = JSON.parseObject(jsonContent);
            
            return SparkX1Model.JudgeResult.builder()
                .resultType(jsonObject.getString("resultType"))
                .score(jsonObject.getInteger("score"))
                .evaluation(jsonObject.getString("evaluation"))
                .analysis(jsonObject.getString("analysis"))
                .suggestions(jsonObject.getJSONArray("suggestions") != null ? 
                           jsonObject.getJSONArray("suggestions").toJavaList(String.class) : null)
                .passedCases(jsonObject.getInteger("passedCases"))
                .totalCases(jsonObject.getInteger("totalCases"))
                .errorMessage(jsonObject.getString("errorMessage"))
                .executionTime(jsonObject.getLong("executionTime"))
                .memoryUsage(jsonObject.getDouble("memoryUsage"))
                .passed(jsonObject.getBoolean("passed"))
                .build();
                
        } catch (Exception e) {
            logger.error("解析判题结果失败", e);
            return buildErrorResult("解析AI判题结果失败：" + e.getMessage());
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
    private SparkX1Model.JudgeResult buildErrorResult(String errorMessage) {
        return SparkX1Model.JudgeResult.builder()
            .resultType("SE")
            .score(0)
            .evaluation("系统错误，无法完成判题")
            .analysis("判题系统发生内部错误")
            .suggestions(Arrays.asList("请检查代码格式", "请联系管理员"))
            .passedCases(0)
            .totalCases(0)
            .errorMessage(errorMessage)
            .executionTime(0L)
            .memoryUsage(0.0)
            .passed(false)
            .build();
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
     * 构建代码判题提示词
     */
    private String buildCodeJudgePrompt(SparkX1Model.CodeJudgeRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的代码判题系统，请评估以下代码：\n\n");
        prompt.append("题目描述：").append(request.getProblemDescription()).append("\n\n");
        prompt.append("编程语言：").append(request.getLanguage()).append("\n\n");
        
        // 为Shell语言添加特殊说明
        if ("shell".equalsIgnoreCase(request.getLanguage()) || 
            "sh".equalsIgnoreCase(request.getLanguage()) || 
            "bash".equalsIgnoreCase(request.getLanguage())) {
            prompt.append("注意：这是Shell脚本代码，请重点关注：\n");
            prompt.append("- 语法正确性（脚本是否能正确执行）\n");
            prompt.append("- 逻辑实现（是否满足题目要求）\n");
            prompt.append("- 最佳实践（是否遵循Shell编程规范）\n");
            prompt.append("- 错误处理（是否有适当的错误处理机制）\n\n");
        }
        
        prompt.append("用户代码：\n").append(request.getUserCode()).append("\n\n");
        
        if (request.getTestCases() != null && !request.getTestCases().isEmpty()) {
            prompt.append("测试用例：\n");
            for (int i = 0; i < request.getTestCases().size(); i++) {
                var testCase = request.getTestCases().get(i);
                prompt.append("用例").append(i+1).append("：\n");
                prompt.append("输入：").append(testCase.getInput()).append("\n");
                prompt.append("期望输出：").append(testCase.getExpectedOutput()).append("\n\n");
            }
        }
        
        prompt.append("请以JSON格式返回判题结果，包含以下字段：\n");
        prompt.append("{\n");
        prompt.append("  \"resultType\": \"AC|WA|TLE|MLE|RE|CE|SE\",\n");
        prompt.append("  \"score\": 分数(0-100),\n");
        prompt.append("  \"evaluation\": \"评价\",\n");
        prompt.append("  \"analysis\": \"详细分析\",\n");
        prompt.append("  \"suggestions\": [\"建议1\", \"建议2\"],\n");
        prompt.append("  \"passedCases\": 通过的测试用例数,\n");
        prompt.append("  \"totalCases\": 总测试用例数,\n");
        prompt.append("  \"passed\": true/false\n");
        prompt.append("}");
        
        return prompt.toString();
    }
    
    /**
     * 构建简化代码判题提示词
     */
    private String buildSimpleCodeJudgePrompt(String problemDescription, String userCode, String language) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的代码判题系统，请快速评估以下代码：\n\n");
        prompt.append("题目描述：").append(problemDescription).append("\n\n");
        prompt.append("编程语言：").append(language).append("\n\n");
        
        // 为Shell语言添加特殊说明
        if ("shell".equalsIgnoreCase(language) || 
            "sh".equalsIgnoreCase(language) || 
            "bash".equalsIgnoreCase(language)) {
            prompt.append("注意：这是Shell脚本代码，请快速评估脚本的语法正确性和逻辑实现。\n\n");
        }
        
        prompt.append("用户代码：\n").append(userCode).append("\n\n");
        
        prompt.append("请以JSON格式返回简化判题结果，包含以下字段：\n");
        prompt.append("{\n");
        prompt.append("  \"resultType\": \"AC|WA|CE|SE\",\n");
        prompt.append("  \"score\": 分数(0-100),\n");
        prompt.append("  \"evaluation\": \"评价\",\n");
        prompt.append("  \"analysis\": \"简要分析\",\n");
        prompt.append("  \"passed\": true/false\n");
        prompt.append("}");
        
        return prompt.toString();
    }
}