package interview.ai.spark;

import interview.ai.SparkClient;
import interview.ai.prompt.PersonalizedSuggestionsPromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 星火X1.5模型个性化建议分析器
 * 基于面试表现生成个性化的学习建议和改进方案
 */
@Component
public class SparkX1PersonalizedSuggestionsAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(SparkX1PersonalizedSuggestionsAnalyzer.class);
    
    @Autowired
    private SparkClient sparkClient;
    
    @Autowired
    private PersonalizedSuggestionsPromptBuilder promptBuilder;
    
    /**
     * 检查分析器是否可用
     * 
     * @return 分析器是否可用
     */
    public boolean isAvailable() {
        try {
            // 检查SparkClient是否可用
            return sparkClient != null;
        } catch (Exception e) {
            log.warn("检查个性化建议分析器可用性时发生异常", e);
            return false;
        }
    }

    /**
     * 分析面试表现并生成个性化建议
     * 
     * @param sessionId 面试会话ID
     * @param userId 用户ID
     * @param interviewData 面试数据（包含基础问答、场景面试、代码测试等各环节数据）
     * @return 个性化建议分析结果的JSON字符串
     */
    public String analyzePersonalizedSuggestions(Long sessionId, Long userId, Map<String, Object> interviewData) {
        log.info("开始使用星火X1.5模型生成个性化建议 sessionId={}, userId={}", sessionId, userId);
        
        try {
            // 1. 构建个性化建议分析提示词
            String prompt = promptBuilder.buildPersonalizedSuggestionsPrompt(interviewData);
            log.debug("个性化建议分析提示词构建完成，长度: {}", prompt.length());
            
            // 2. 调用星火X1.5模型进行分析
            log.info("调用星火X1.5模型进行个性化建议分析...");
            String analysisResult = sparkClient.ask(prompt);
            
            if (analysisResult == null || analysisResult.trim().isEmpty()) {
                log.error("星火X1.5模型返回空结果");
                throw new RuntimeException("AI分析返回空结果");
            }
            
            log.info("星火X1.5个性化建议分析完成 sessionId={}, 结果长度={}", sessionId, analysisResult.length());
            log.debug("个性化建议分析结果: {}", analysisResult);
            
            return analysisResult;
            
        } catch (Exception e) {
            log.error("星火X1.5个性化建议分析失败 sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("个性化建议分析失败: " + e.getMessage(), e);
        }
    }
}
