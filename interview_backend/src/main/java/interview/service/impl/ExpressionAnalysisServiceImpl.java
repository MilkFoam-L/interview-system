package interview.service.impl;

import interview.service.ExpressionAnalysisService;
import interview.util.UserContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 表情分析服务实现
 */
@Service
public class ExpressionAnalysisServiceImpl implements ExpressionAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExpressionAnalysisServiceImpl.class);
    
    @Autowired
    private UserContextUtil userContextUtil;
    
    @Override
    public Map<String, Object> getExpressionAnalysisResult(Long userId) {
        // 记录使用的用户ID
        logger.info("正在获取用户ID: {} 的表情分析结果", userId);
        
        if (userId == null) {
            // 如果传入的userId为空，才尝试从上下文获取
            userId = userContextUtil.getCurrentUserId();
            logger.info("传入的userId为空，从上下文获取: {}", userId);
        }
        
        // 这里模拟表情分析数据，实际项目中应当从数据库获取或调用相关服务
        Map<String, Object> expressionData = new HashMap<>();
        
        try {
            // 模拟表情数据
            Random random = new Random();
            
            // 模拟主要表情
            String[] expressions = {"happy", "neutral", "surprised", "sad", "angry"};
            String dominantExpression = expressions[random.nextInt(expressions.length)];
            expressionData.put("dominantExpression", dominantExpression);
            
            // 模拟表情计数
            Map<String, Integer> expressionCounts = new HashMap<>();
            expressionCounts.put("happy", 10 + random.nextInt(50));
            expressionCounts.put("neutral", 30 + random.nextInt(60));
            expressionCounts.put("surprised", 5 + random.nextInt(20));
            expressionCounts.put("sad", 0 + random.nextInt(10));
            expressionCounts.put("angry", 0 + random.nextInt(5));
            expressionData.put("expressionCounts", expressionCounts);
            
            // 计算总数
            int totalCount = expressionCounts.values().stream().mapToInt(Integer::intValue).sum();
            expressionData.put("totalCount", totalCount);
            
            // 计算百分比
            Map<String, Double> percentages = new HashMap<>();
            for (Map.Entry<String, Integer> entry : expressionCounts.entrySet()) {
                double percentage = (double) entry.getValue() / totalCount * 100;
                percentages.put(entry.getKey(), Math.round(percentage * 10) / 10.0);
            }
            expressionData.put("percentages", percentages);
            
            expressionData.put("userId", userId); // 添加用户ID到结果中
            expressionData.put("status", "success");
            
        } catch (Exception e) {
            logger.error("生成表情分析数据时出错: {}", e.getMessage(), e);
            expressionData.put("status", "error");
            expressionData.put("message", "无法获取表情分析数据: " + e.getMessage());
        }
        
        return expressionData;
    }
} 