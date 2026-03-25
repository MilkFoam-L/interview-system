package interview.service.impl;

import interview.ai.spark.SparkX1Model;
import interview.config.SparkX1Config;
import interview.model.dto.CodeJudgeRequest;
import interview.model.dto.CodeJudgeResult;
import interview.model.dto.TestCase;
import interview.model.entity.Question;
import interview.service.AICodeJudgeService;
import interview.service.CodeJudgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * AI代码判题服务适配器
 * 作为现有CodeJudgeService接口的实现，内部调用AI判题服务
 * 完美兼容现有业务逻辑，无需修改调用方代码
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "code.judge.mode", havingValue = "ai", matchIfMissing = false)
public class AICodeJudgeAdapterService implements CodeJudgeService {

    @Autowired
    private AICodeJudgeService aiCodeJudgeService;
    
    @Autowired
    private SparkX1Config sparkX1Config;
    
    // Redis
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${code.judge.mode:ai}")
    private String judgeMode;
    
    // Redis缓存配置
    private static final String JUDGE_CACHE_PREFIX = "judge:";
    private static final long CACHE_TIMEOUT = 60;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList(
        "java", "python", "cpp", "c", "javascript", "js", "typescript", "ts", 
        "go", "rust", "csharp", "c#", "php", "ruby", "kotlin", "scala", "sql",
        "shell", "sh", "bash"
    ));

    @Override
    public CodeJudgeResult judge(CodeJudgeRequest request) {
        log.info("使用AI判题服务进行代码判题，语言: {}", request.getLanguage());

        String cacheKey = generateCacheKey(request);
        CodeJudgeResult cachedResult = getCachedResult(cacheKey);
        if (cachedResult != null) {
            log.info("从缓存获取判题结果: {}", cacheKey);
            return cachedResult;
        }

        log.info("AI判题适配器服务状态检查:");
        log.info("- aiCodeJudgeService: {}", aiCodeJudgeService != null ? "已注入" : "未注入");
        log.info("- sparkX1Config: {}", sparkX1Config != null ? "已注入" : "未注入");
        if (sparkX1Config != null) {
            log.info("- sparkX1Config.enabled: {}", sparkX1Config.getEnabled());
            log.info("- sparkX1Config.apiPassword: {}", sparkX1Config.getApiPassword() != null ? "已配置" : "未配置");
        }
        
        try {
            if (!isSupportedLanguage(request.getLanguage())) {
                return createErrorResult("不支持的编程语言: " + request.getLanguage(), "SE");
            }

            List<SparkX1Model.CodeJudgeRequest.TestCase> testCases = parseTestCasesToAIFormat(request.getTestCases());
            CodeJudgeResult result;
            
            if (testCases.isEmpty()) {
                SparkX1Model.JudgeResult aiResult = aiCodeJudgeService.quickJudge(
                    "请分析这段代码的正确性和质量",
                    request.getCode(),
                    request.getLanguage()
                );
                result = convertAIResultToCompatibleFormat(aiResult, testCases);
            } else {
            SparkX1Model.JudgeResult aiResult = aiCodeJudgeService.judgeCode(
                "请根据提供的测试用例判断代码是否正确。分析代码逻辑，检查是否能产生期望的输出。",
                request.getCode(),
                request.getLanguage(),
                testCases
            );

                result = convertAIResultToCompatibleFormat(aiResult, testCases);
            }

            cacheResult(cacheKey, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("AI判题过程中发生异常: {}", e.getMessage(), e);
            return createErrorResult("判题系统异常: " + e.getMessage(), "SE");
        }
    }

    @Override
    public boolean isSupportedLanguage(String language) {
        return language != null && SUPPORTED_LANGUAGES.contains(language.toLowerCase());
    }

    @Override
    public String extractTestCases(Question question) {
        return question.getCorrectAnswer();
    }
    
    /**
     * 将JSON格式的测试用例转换为AI服务所需的格式
     */
    private List<SparkX1Model.CodeJudgeRequest.TestCase> parseTestCasesToAIFormat(String testCasesJson) {
        List<SparkX1Model.CodeJudgeRequest.TestCase> aiTestCases = new ArrayList<>();
        
        try {
            if (testCasesJson == null || testCasesJson.trim().isEmpty()) {
                return aiTestCases;
            }
            
            // 解析原有格式的测试用例
            List<TestCase> originalTestCases = objectMapper.readValue(testCasesJson, 
                new TypeReference<List<TestCase>>() {});
            
            // 转换为AI服务格式
            for (TestCase testCase : originalTestCases) {
                SparkX1Model.CodeJudgeRequest.TestCase aiTestCase = SparkX1Model.CodeJudgeRequest.TestCase.builder()
                    .input(testCase.getInput())
                    .expectedOutput(testCase.getExpectedOutput())
                    .description("测试用例")
                    .build();
                aiTestCases.add(aiTestCase);
            }
            
        } catch (Exception e) {
            log.error("解析测试用例失败", e);
        }
        
        return aiTestCases;
    }
    
    /**
     * 将AI判题结果转换为兼容的CodeJudgeResult格式
     */
    private CodeJudgeResult convertAIResultToCompatibleFormat(SparkX1Model.JudgeResult aiResult, 
                                                            List<SparkX1Model.CodeJudgeRequest.TestCase> testCases) {
        CodeJudgeResult result = new CodeJudgeResult();
        
        // 映射状态
        String status = mapAIResultTypeToStatus(aiResult.getResultType());
        result.setStatus(status);
        
        // 设置得分
        result.setScore(aiResult.getScore() != null ? aiResult.getScore().intValue() : 0);
        
        // 设置通过的测试用例数
        result.setPassedCount(aiResult.getPassedCases() != null ? aiResult.getPassedCases() : 0);
        
        // 对于快速判题模式（无测试用例），设置虚拟测试用例数量
        int totalCases = aiResult.getTotalCases() != null ? aiResult.getTotalCases() : testCases.size();
        if (totalCases == 0 && testCases.isEmpty()) {
            // 快速判题模式：根据AI评分创建虚拟测试用例显示
            int score = aiResult.getScore() != null ? aiResult.getScore().intValue() : 0;
            if (score >= 90) {
                totalCases = 1;
                result.setPassedCount(1);
            } else if (score >= 70) {
                totalCases = 2;
                result.setPassedCount(1);
            } else if (score >= 50) {
                totalCases = 3;
                result.setPassedCount(1);
            } else if (score > 0) {
                totalCases = 3;
                result.setPassedCount(0);
            } else {
                totalCases = 1;
                result.setPassedCount(0);
            }
        }
        result.setTotalCount(totalCases);
        
        // 设置执行时间和内存使用
        result.setExecutionTime(aiResult.getExecutionTime() != null ? aiResult.getExecutionTime() : 0L);
        result.setMemoryUsage(aiResult.getMemoryUsage() != null ? aiResult.getMemoryUsage().longValue() : 0L);
        
        // 设置错误信息
        result.setErrorMessage(aiResult.getErrorMessage());
        
        // 构建测试用例结果（简化版）
        List<CodeJudgeResult.TestCaseResult> testCaseResults = new ArrayList<>();
        
        if (!testCases.isEmpty()) {
            // 有实际测试用例
            for (int i = 0; i < testCases.size(); i++) {
                SparkX1Model.CodeJudgeRequest.TestCase testCase = testCases.get(i);
                CodeJudgeResult.TestCaseResult testCaseResult = new CodeJudgeResult.TestCaseResult();
                testCaseResult.setInput(testCase.getInput());
                testCaseResult.setExpectedOutput(testCase.getExpectedOutput());
                testCaseResult.setActualOutput(""); // AI判题不返回具体输出
                testCaseResult.setPassed(i < result.getPassedCount()); // 简单判断
                testCaseResults.add(testCaseResult);
            }
        } else if (result.getTotalCount() > 0) {
            // 快速判题模式：创建虚拟测试用例结果用于显示
            for (int i = 0; i < result.getTotalCount(); i++) {
                CodeJudgeResult.TestCaseResult testCaseResult = new CodeJudgeResult.TestCaseResult();
                testCaseResult.setInput("快速判题模式");
                testCaseResult.setExpectedOutput("基于AI评估");
                testCaseResult.setActualOutput("AI分析结果");
                testCaseResult.setPassed(i < result.getPassedCount());
                testCaseResults.add(testCaseResult);
            }
        }
        
        result.setTestCaseResults(testCaseResults);
        
        return result;
    }
    
    /**
     * 将AI判题结果类型映射到兼容的状态码
     */
    private String mapAIResultTypeToStatus(String resultType) {
        if (resultType == null) {
            return "SE";
        }
        
        switch (resultType.toUpperCase()) {
            case "ACCEPTED":
            case "AC":
                return "AC";
            case "WRONG_ANSWER":
            case "WA":
                return "WA";
            case "TIME_LIMIT_EXCEEDED":
            case "TLE":
                return "TLE";
            case "MEMORY_LIMIT_EXCEEDED":
            case "MLE":
                return "MLE";
            case "COMPILATION_ERROR":
            case "CE":
                return "CE";
            case "RUNTIME_ERROR":
            case "RE":
                return "RE";
            case "SYSTEM_ERROR":
            case "SE":
            default:
                return "SE";
        }
    }
    
    /**
     * 创建错误结果
     */
    private CodeJudgeResult createErrorResult(String message, String status) {
        CodeJudgeResult result = new CodeJudgeResult();
        result.setStatus(status);
        result.setScore(0);
        result.setPassedCount(0);
        result.setTotalCount(0);
        result.setExecutionTime(0L);
        result.setMemoryUsage(0L);
        result.setErrorMessage(message);
        result.setTestCaseResults(new ArrayList<>());
        return result;
    }
    
    /**
     * 基于代码结构分析给予评分
     */
    private int analyzeCodeStructureScore(String code) {
        if (code == null || code.trim().isEmpty()) {
            return 0;
        }
        
        int score = 0;
        String lowerCode = code.toLowerCase();
        
        // 基础语法结构检查
        if (lowerCode.contains("function") || lowerCode.contains("def ") || 
            lowerCode.contains("public") || lowerCode.contains("class")) {
            score += 10; // 有基本函数或类定义
        }
        
        // 循环结构检查
        if (lowerCode.contains("for") || lowerCode.contains("while")) {
            score += 10; // 使用了循环
        }
        
        // 条件判断检查
        if (lowerCode.contains("if") || lowerCode.contains("switch")) {
            score += 10; // 使用了条件判断
        }
        
        // 变量使用检查
        if (lowerCode.contains("=") && lowerCode.length() > 50) {
            score += 10; // 有变量赋值且代码有一定长度
        }
        
        // 代码长度奖励
        if (code.trim().length() > 100) {
            score += 10; // 代码有一定复杂度
        }
        
        return Math.min(score, 40); // 最高40分的结构分
    }

    /**
     * 生成缓存键
     */
    private String generateCacheKey(CodeJudgeRequest request) {
        try {
            StringBuilder keyBuilder = new StringBuilder();
            keyBuilder.append(request.getCode() != null ? request.getCode() : "");
            keyBuilder.append("|").append(request.getLanguage() != null ? request.getLanguage() : "");
            
            if (request.getTestCases() != null) {
                keyBuilder.append("|").append(request.getTestCases());
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(keyBuilder.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hashBuilder = new StringBuilder();
            for (byte b : hashBytes) {
                hashBuilder.append(String.format("%02x", b));
            }
            
            return JUDGE_CACHE_PREFIX + hashBuilder.toString();
        } catch (Exception e) {
            log.warn("生成缓存键失败: {}", e.getMessage());
            return JUDGE_CACHE_PREFIX + request.getCode().hashCode();
        }
    }
    
    /**
     * 从缓存获取判题结果
     */
    private CodeJudgeResult getCachedResult(String cacheKey) {
        try {
            return (CodeJudgeResult) redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("从缓存获取判题结果失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 缓存判题结果
     */
    private void cacheResult(String cacheKey, CodeJudgeResult result) {
        try {
            if (result != null && !"SE".equals(result.getStatus()) && !"CE".equals(result.getStatus())) {
                redisTemplate.opsForValue().set(cacheKey, result, CACHE_TIMEOUT, TimeUnit.MINUTES);
                log.debug("判题结果已缓存: {}", cacheKey);
            }
        } catch (Exception e) {
            log.warn("缓存判题结果失败: {}", e.getMessage());
        }
    }
    
    /**
     * 清除相关缓存
     */
    public void clearCache(String pattern) {
        try {
            var keys = redisTemplate.keys(JUDGE_CACHE_PREFIX + "*" + pattern + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清除判题缓存: {} 个键", keys.size());
            }
        } catch (Exception e) {
            log.warn("清除判题缓存失败: {}", e.getMessage());
        }
    }
}
