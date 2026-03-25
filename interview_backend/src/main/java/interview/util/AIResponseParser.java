package interview.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import interview.ai.spark.SparkX1Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI响应解析工具类
 * 解析AI模型返回的判题结果
 */
public class AIResponseParser {
    
    private static final Logger logger = LoggerFactory.getLogger(AIResponseParser.class);
    
    // JSON提取正则表达式
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[^{}]*(?:\\{[^{}]*\\}[^{}]*)*\\}");
    
    /**
     * 解析AI判题响应
     */
    public static SparkX1Model.JudgeResult parseJudgeResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            logger.warn("AI响应为空");
            return buildErrorResult("AI响应为空");
        }
        
        try {
            // 提取JSON内容
            String jsonContent = extractJsonContent(response);
            
            if (jsonContent == null) {
                logger.warn("无法从响应中提取JSON内容：{}", response);
                return buildErrorResult("AI响应格式错误");
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
            logger.error("解析AI判题响应失败", e);
            return buildErrorResult("解析AI响应失败：" + e.getMessage());
        }
    }
    
    /**
     * 提取JSON内容
     */
    public static String extractJsonContent(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }
        
        // 方法1：查找第一个完整的JSON对象
        int start = response.indexOf("{");
        if (start != -1) {
            int braceCount = 0;
            int end = start;
            
            for (int i = start; i < response.length(); i++) {
                char c = response.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        end = i;
                        break;
                    }
                }
            }
            
            if (braceCount == 0 && end > start) {
                return response.substring(start, end + 1);
            }
        }
        
        // 方法2：使用正则表达式
        Matcher matcher = JSON_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group();
        }
        
        // 方法3：尝试清理和修复JSON
        return cleanAndRepairJson(response);
    }
    
    /**
     * 清理和修复JSON内容
     */
    private static String cleanAndRepairJson(String response) {
        try {
            // 移除markdown代码块标记
            response = response.replaceAll("```json", "")
                              .replaceAll("```", "")
                              .trim();
            
            // 查找可能的JSON内容
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            
            if (start != -1 && end != -1 && end > start) {
                String jsonCandidate = response.substring(start, end + 1);
                
                // 验证是否为有效JSON
                JSON.parseObject(jsonCandidate);
                return jsonCandidate;
            }
            
        } catch (Exception e) {
            logger.debug("JSON清理和修复失败", e);
        }
        
        return null;
    }
    
    /**
     * 验证JSON格式
     */
    public static boolean isValidJson(String jsonString) {
        try {
            JSON.parseObject(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 提取错误信息
     */
    public static String extractErrorMessage(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "响应为空";
        }
        
        // 查找常见的错误关键词
        String[] errorKeywords = {"error", "错误", "异常", "失败", "Error", "Exception"};
        
        for (String keyword : errorKeywords) {
            int index = response.toLowerCase().indexOf(keyword.toLowerCase());
            if (index != -1) {
                // 提取错误信息前后50个字符
                int start = Math.max(0, index - 50);
                int end = Math.min(response.length(), index + keyword.length() + 50);
                return response.substring(start, end).trim();
            }
        }
        
        // 如果没找到错误关键词，返回响应的前100个字符
        return response.length() > 100 ? 
               response.substring(0, 100) + "..." : 
               response;
    }
    
    /**
     * 构建错误结果
     */
    private static SparkX1Model.JudgeResult buildErrorResult(String errorMessage) {
        return SparkX1Model.JudgeResult.builder()
            .resultType("SE")
            .score(0)
            .evaluation("解析错误")
            .analysis("无法解析AI模型返回的判题结果")
            .suggestions(Arrays.asList("请检查AI模型配置", "请联系管理员"))
            .passedCases(0)
            .totalCases(0)
            .errorMessage(errorMessage)
            .executionTime(0L)
            .memoryUsage(0.0)
            .passed(false)
            .build();
    }
    
    /**
     * 标准化结果类型
     */
    public static String normalizeResultType(String resultType) {
        if (resultType == null || resultType.trim().isEmpty()) {
            return "SE";
        }
        
        String normalized = resultType.toUpperCase().trim();
        
        // 支持的结果类型
        String[] validTypes = {"AC", "WA", "CE", "RE", "TLE", "MLE", "PC", "SE", "PD", "JG"};
        
        for (String validType : validTypes) {
            if (validType.equals(normalized)) {
                return normalized;
            }
        }
        
        // 如果不是有效类型，返回系统错误
        return "SE";
    }
}
