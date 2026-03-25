package interview.service.impl;

import interview.ai.spark.SparkX1CodeJudgeClient;
import interview.ai.spark.SparkX1Model;
import interview.model.enums.JudgeResultType;
import interview.model.enums.ProgrammingLanguage;
import interview.service.AICodeJudgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * AI代码判题服务实现类
 * 替换原有的Docker代码执行方案，使用AI进行代码判题
 */
@Service
public class AICodeJudgeServiceImpl implements AICodeJudgeService {
    
    private static final Logger logger = LoggerFactory.getLogger(AICodeJudgeServiceImpl.class);
    
    @Autowired
    private SparkX1CodeJudgeClient sparkX1Client;
    
    // 轻量级混合策略配置
    @Value("${judge.hybrid.enabled:false}")
    private boolean hybridEnabled;
    
    @Value("${judge.hybrid.quality-weight:0.2}")
    private double qualityWeight;
    
    @Value("${judge.hybrid.max-adjustment:15}")
    private int maxAdjustment;
    
    /**
     * 执行代码判题
     */
    @Override
    public SparkX1Model.JudgeResult judgeCode(String problemDescription, 
                                              String userCode, 
                                              String language, 
                                              List<SparkX1Model.CodeJudgeRequest.TestCase> testCases) {
        try {
            logger.info("开始AI代码判题，语言：{}，测试用例数：{}", language, testCases != null ? testCases.size() : 0);
            
            // 验证输入参数
            if (!validateInput(problemDescription, userCode, language)) {
                return buildValidationErrorResult();
            }
            
            // 检查语言支持
            if (!ProgrammingLanguage.isSupported(language)) {
                return buildUnsupportedLanguageResult(language);
            }
            
            // 构建判题请求
            SparkX1Model.CodeJudgeRequest request = SparkX1Model.CodeJudgeRequest.builder()
                .problemDescription(problemDescription)
                .userCode(userCode)
                .language(language)
                .testCases(testCases != null ? testCases : Arrays.asList())
                .timeLimit(30) // 默认30秒
                .memoryLimit(128) // 默认128MB
                .build();
            
            // 执行判题
            SparkX1Model.JudgeResult result = sparkX1Client.judgeCode(request);
            
            // 后处理结果
            result = postProcessResult(result, language);
            
            // 轻量级混合策略优化
            result = applyLightweightOptimization(result, userCode, language);
            
            logger.info("AI代码判题完成，结果：{}，得分：{}", result.getResultType(), result.getScore());
            
            return result;
            
        } catch (Exception e) {
            logger.error("AI代码判题失败", e);
            return buildSystemErrorResult(e.getMessage());
        }
    }
    
    /**
     * 快速判题（无测试用例）
     */
    @Override
    public SparkX1Model.JudgeResult quickJudge(String problemDescription, 
                                               String userCode, 
                                               String language) {
        try {
            logger.info("开始AI快速判题，语言：{}", language);
            
            // 验证输入参数
            if (!validateInput(problemDescription, userCode, language)) {
                return buildValidationErrorResult();
            }
            
            // 检查语言支持
            if (!ProgrammingLanguage.isSupported(language)) {
                return buildUnsupportedLanguageResult(language);
            }
            
            // 执行快速判题
            SparkX1Model.JudgeResult result = sparkX1Client.quickJudge(problemDescription, userCode, language);
            
            // 后处理结果
            result = postProcessResult(result, language);
            
            // 轻量级混合策略优化
            result = applyLightweightOptimization(result, userCode, language);
            
            logger.info("AI快速判题完成，结果：{}", result.getResultType());
            
            return result;
            
        } catch (Exception e) {
            logger.error("AI快速判题失败", e);
            return buildSystemErrorResult(e.getMessage());
        }
    }
    
    /**
     * 代码质量分析
     */
    @Override
    public SparkX1Model.JudgeResult analyzeCodeQuality(String userCode, String language) {
        try {
            logger.info("开始代码质量分析，语言：{}", language);
            
            if (userCode == null || userCode.trim().isEmpty()) {
                return buildValidationErrorResult();
            }
            
            // 这里可以调用专门的代码质量分析方法
            // 暂时使用快速判题的方式
            return quickJudge("代码质量分析", userCode, language);
            
        } catch (Exception e) {
            logger.error("代码质量分析失败", e);
            return buildSystemErrorResult(e.getMessage());
        }
    }
    
    /**
     * 验证输入参数
     */
    private boolean validateInput(String problemDescription, String userCode, String language) {
        if (problemDescription == null || problemDescription.trim().isEmpty()) {
            logger.warn("题目描述为空");
            return false;
        }
        
        if (userCode == null || userCode.trim().isEmpty()) {
            logger.warn("用户代码为空");
            return false;
        }
        
        if (language == null || language.trim().isEmpty()) {
            logger.warn("编程语言为空");
            return false;
        }
        
        return true;
    }
    
    /**
     * 后处理判题结果
     */
    private SparkX1Model.JudgeResult postProcessResult(SparkX1Model.JudgeResult result, String language) {
        if (result == null) {
            return buildSystemErrorResult("AI模型返回空结果");
        }
        
        // 规范化结果类型
        String resultType = normalizeResultType(result.getResultType());
        result.setResultType(resultType);
        
        // 验证得分范围
        if (result.getScore() == null || result.getScore() < 0 || result.getScore() > 100) {
            result.setScore(0);
        }
        
        // 设置默认值
        if (result.getPassedCases() == null) {
            result.setPassedCases(0);
        }
        
        if (result.getTotalCases() == null) {
            result.setTotalCases(0);
        }
        
        if (result.getPassed() == null) {
            result.setPassed("AC".equals(resultType));
        }
        
        return result;
    }
    
    /**
     * 规范化结果类型
     */
    private String normalizeResultType(String resultType) {
        if (resultType == null || resultType.trim().isEmpty()) {
            return "SE";
        }
        
        String normalized = resultType.toUpperCase().trim();
        
        // 验证是否为有效的结果类型
        try {
            JudgeResultType.fromCode(normalized);
            return normalized;
        } catch (Exception e) {
            logger.warn("无效的结果类型：{}，设置为系统错误", resultType);
            return "SE";
        }
    }
    
    /**
     * 构建参数验证错误结果
     */
    private SparkX1Model.JudgeResult buildValidationErrorResult() {
        return SparkX1Model.JudgeResult.builder()
            .resultType("SE")
            .score(0)
            .evaluation("输入参数错误")
            .analysis("题目描述、用户代码或编程语言参数缺失或为空")
            .suggestions(Arrays.asList("请检查输入参数", "确保所有必要字段都不为空"))
            .passedCases(0)
            .totalCases(0)
            .errorMessage("输入参数验证失败")
            .executionTime(0L)
            .memoryUsage(0.0)
            .passed(false)
            .build();
    }
    
    /**
     * 构建不支持语言错误结果
     */
    private SparkX1Model.JudgeResult buildUnsupportedLanguageResult(String language) {
        return SparkX1Model.JudgeResult.builder()
            .resultType("SE")
            .score(0)
            .evaluation("不支持的编程语言")
            .analysis("系统暂不支持" + language + "语言的代码判题")
            .suggestions(Arrays.asList("请使用支持的编程语言", "支持的语言：Java、Python、C++、C、JavaScript等"))
            .passedCases(0)
            .totalCases(0)
            .errorMessage("不支持的编程语言：" + language)
            .executionTime(0L)
            .memoryUsage(0.0)
            .passed(false)
            .build();
    }
    
    /**
     * 构建系统错误结果
     */
    private SparkX1Model.JudgeResult buildSystemErrorResult(String errorMessage) {
        return SparkX1Model.JudgeResult.builder()
            .resultType("SE")
            .score(0)
            .evaluation("系统错误")
            .analysis("判题系统发生内部错误，无法完成代码判题")
            .suggestions(Arrays.asList("请稍后重试", "如问题持续存在，请联系管理员"))
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
    @Override
    public boolean isServiceAvailable() {
        try {
            return sparkX1Client.isAvailable();
        } catch (Exception e) {
            logger.error("检查AI代码判题服务可用性失败", e);
            return false;
        }
    }
    
    /**
     * 获取支持的编程语言列表
     */
    @Override
    public List<String> getSupportedLanguages() {
        return Arrays.asList(
            "java", "python", "cpp", "c", "go", "rust",
            "javascript", "typescript", "php", "ruby",
            "csharp", "swift", "kotlin", "scala", "r", "sql",
            "shell", "dockerfile", "yaml", "junit"
        );
    }
    
    /**
     * 轻量级混合策略优化
     * 在原有AI判题分数基础上进行微调，而不是重新计算
     */
    private SparkX1Model.JudgeResult applyLightweightOptimization(
            SparkX1Model.JudgeResult originalResult, String userCode, String language) {
        
        if (!hybridEnabled || originalResult == null || originalResult.getScore() == null) {
            return originalResult;
        }
        
        try {
            int originalScore = originalResult.getScore();
            int qualityScore = quickCodeQualityCheck(userCode, language);
            
            // 计算调整分数：原分数 * (1 - qualityWeight) + 质量分数 * qualityWeight
            double adjustedScore = originalScore * (1 - qualityWeight) + qualityScore * qualityWeight;
            
            // 限制调整范围，避免过度偏离原始AI判题结果
            int finalScore = Math.max(
                originalScore - maxAdjustment,
                Math.min(originalScore + maxAdjustment, (int) Math.round(adjustedScore))
            );
            
            // 确保分数在有效范围内
            finalScore = Math.max(0, Math.min(100, finalScore));
            
            // 如果分数有调整，增强分析说明
            if (finalScore != originalScore) {
                enhanceAnalysis(originalResult, originalScore, finalScore, qualityScore);
                logger.debug("混合策略优化：{} -> {}, 质量分数: {}", originalScore, finalScore, qualityScore);
            }
            
            originalResult.setScore(finalScore);
            return originalResult;
            
        } catch (Exception e) {
            logger.error("轻量级优化失败，返回原始结果", e);
            return originalResult;
        }
    }
    
    /**
     * 快速代码质量检测（不超过30行代码）
     */
    private int quickCodeQualityCheck(String userCode, String language) {
        int score = 70; // 基础分数
        
        try {
            // 1. 基本语法检查（括号匹配）
            if (isParenthesesBalanced(userCode)) {
                score += 10;
            } else {
                score -= 15;
            }
            
            // 2. 代码长度合理性
            int length = userCode.trim().length();
            if (length > 20 && length < 500) {
                score += 5;
            } else if (length > 1000) {
                score -= 10;
            }
            
            // 3. 基本逻辑检查
            if (hasReturnStatement(userCode, language) && !hasObviousInfiniteLoop(userCode)) {
                score += 10;
            }
            
            // 4. 代码风格
            if (hasReasonableNaming(userCode)) {
                score += 5;
            }
            
        } catch (Exception e) {
            logger.debug("代码质量检测异常，使用默认分数", e);
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * 增强分析说明
     */
    private void enhanceAnalysis(SparkX1Model.JudgeResult result, int originalScore, int finalScore, int qualityScore) {
        try {
            String enhancement = String.format(
                "\n\n【混合评估】AI分数: %d, 代码质量: %d, 最终分数: %d",
                originalScore, qualityScore, finalScore
            );
            
            String currentAnalysis = result.getAnalysis() != null ? result.getAnalysis() : "";
            result.setAnalysis(currentAnalysis + enhancement);
            
            // 添加质量改进建议
            List<String> suggestions = result.getSuggestions() != null ? 
                new ArrayList<>(result.getSuggestions()) : new ArrayList<>();
            
            if (qualityScore < 70) {
                suggestions.add("建议优化代码结构和逻辑");
            }
            if (finalScore > originalScore) {
                suggestions.add("代码质量良好，继续保持");
            }
            
            result.setSuggestions(suggestions);
            
        } catch (Exception e) {
            logger.debug("增强分析失败", e);
        }
    }
    
    // ===== 轻量级检测工具方法 =====
    
    private boolean isParenthesesBalanced(String code) {
        int balance = 0;
        for (char c : code.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') balance++;
            if (c == ')' || c == ']' || c == '}') balance--;
            if (balance < 0) return false;
        }
        return balance == 0;
    }
    
    private boolean hasReturnStatement(String code, String language) {
        return code.toLowerCase().contains("return");
    }
    
    private boolean hasObviousInfiniteLoop(String code) {
        return code.contains("while(true)") || code.contains("while (true)") || 
               code.contains("for(;;)") || code.contains("for (;;)");
    }
    
    private boolean hasReasonableNaming(String code) {
        // 检查是否有长度大于1的变量名（避免单字母变量过多）
        return code.matches(".*\\b[a-zA-Z_][a-zA-Z0-9_]{2,}\\b.*");
    }
}
