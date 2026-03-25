package interview.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import interview.model.dto.CodeJudgeRequest;
import interview.model.dto.CodeJudgeResult;
import interview.model.dto.TestCase;
import interview.model.entity.Question;
import interview.service.CodeJudgeService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;

/**
 * 代码判题服务实现类
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "code.judge.mode", havingValue = "docker", matchIfMissing = true)
public class CodeJudgeServiceImpl implements CodeJudgeService {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
        // 编译型语言
        "java", "cpp", "c", "go", "rust", "csharp", "c#", "swift", "kotlin", "scala",
        // 解释型语言  
        "python", "javascript", "js", "typescript", "ts", "php", "ruby", "r",
        // 脚本语言
        "shell", "sh", "bash",
        // 查询语言
        "sql",
        // 配置语言
        "yaml", "yml"
    );
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CodeJudgeResult judge(CodeJudgeRequest request) {
        log.info("开始判题，语言: {}", request.getLanguage());
        
        try {
            // 1. 验证语言支持
            if (!isSupportedLanguage(request.getLanguage())) {
                return createErrorResult("不支持的编程语言: " + request.getLanguage(), "SE");
            }
            
            // 2. 解析测试用例
            List<TestCase> testCases = parseTestCases(request.getTestCases());
            if (testCases == null || testCases.isEmpty()) {
                return createErrorResult("测试用例为空", "SE");
            }
            
            // 3. Docker模式已被禁用，返回错误
            return createErrorResult("Docker判题模式已被AI判题替代，请切换到AI模式", "SE");
            
        } catch (Exception e) {
            log.error("判题过程发生异常", e);
            return createErrorResult("系统错误: " + e.getMessage(), "SE");
        }
    }

    @Override
    public boolean isSupportedLanguage(String language) {
        return language != null && SUPPORTED_LANGUAGES.contains(language.toLowerCase());
    }

    @Override
    public String extractTestCases(Question question) {
        if (question == null || !"code".equals(question.getInputType())) {
            return null;
        }
        
        // 从correctAnswer字段解析测试用例
        // 格式: JSON数组 [{"input":"1 2","expectedOutput":"3"},...]
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            // 如果没有预定义测试用例，生成默认的示例测试用例
            return generateDefaultTestCase(question);
        }
        
        // 检查是否为有效的JSON格式
        if (!isValidJson(correctAnswer)) {
            log.warn("题目 {} 的测试用例不是有效JSON格式，将触发智能生成", question.getId());
            return null; // 返回null触发智能生成
        }
        
        return correctAnswer;
    }
    
    /**
     * 解析测试用例JSON
     */
    private List<TestCase> parseTestCases(String testCasesJson) {
        if (testCasesJson == null || testCasesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(testCasesJson, new TypeReference<List<TestCase>>() {});
        } catch (Exception e) {
            log.error("解析测试用例失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 生成默认测试用例（当题目没有预定义测试用例时）
     */
    private String generateDefaultTestCase(Question question) {
        try {
            List<TestCase> defaultCases = new ArrayList<>();
            
            String content = question.getContent();
            
            // 智能识别题目类型并生成相应测试用例
            if ((content.contains("数组") && content.contains("去重")) || 
                content.contains("去重") || content.contains("unique") || content.contains("distinct")) {
                // 数组去重题目 - 增强版测试用例
                defaultCases.add(new TestCase("console.log(JSON.stringify(removeDuplicates([1,2,2,3,3,4])));", "[1,2,3,4]", "基本去重测试"));
                defaultCases.add(new TestCase("console.log(JSON.stringify(removeDuplicates([1,1,1,1])));", "[1]", "全重复元素"));
                defaultCases.add(new TestCase("console.log(JSON.stringify(removeDuplicates([1,2,3,4,5])));", "[1,2,3,4,5]", "无重复元素"));
                defaultCases.add(new TestCase("console.log(JSON.stringify(removeDuplicates([])));", "[]", "空数组测试"));
                defaultCases.add(new TestCase("console.log(JSON.stringify(removeDuplicates([5,1,3,2,5,2,1])));", "[5,1,3,2]", "复杂去重测试"));
                defaultCases.add(new TestCase("console.log(JSON.stringify(removeDuplicates([0,0,1,1,2,2])));", "[0,1,2]", "包含零值测试"));
                
            } else if (content.contains("排序")) {
                // 排序题目
                defaultCases.add(new TestCase("[3,1,4,1,5]", "[1,1,3,4,5]", "基本排序测试"));
                defaultCases.add(new TestCase("[1]", "[1]", "单元素排序"));
                defaultCases.add(new TestCase("[]", "[]", "空数组排序"));
                
            } else if (content.contains("回文")) {
                // 回文检测题目
                defaultCases.add(new TestCase("aba", "true", "奇数长度回文"));
                defaultCases.add(new TestCase("abba", "true", "偶数长度回文"));
                defaultCases.add(new TestCase("abc", "false", "非回文字符串"));
                defaultCases.add(new TestCase("a", "true", "单字符回文"));
                
            } else if (content.contains("最大值") || content.contains("最小值")) {
                // 查找最值题目
                defaultCases.add(new TestCase("[1,5,3,9,2]", "9", "基本最值测试"));
                defaultCases.add(new TestCase("[1]", "1", "单元素最值"));
                defaultCases.add(new TestCase("[-1,-5,-3]", "-1", "负数最值"));
                
            } else if (content.contains("求和") || content.contains("加法")) {
                // 求和题目
                defaultCases.add(new TestCase("[1,2,3,4,5]", "15", "基本求和测试"));
                defaultCases.add(new TestCase("[]", "0", "空数组求和"));
                defaultCases.add(new TestCase("[-1,1,-2,2]", "0", "正负数求和"));
                
            } else {
                // 通用测试用例
                defaultCases.add(new TestCase("测试输入1", "期望输出1", "基本功能测试"));
                defaultCases.add(new TestCase("测试输入2", "期望输出2", "边界条件测试"));
            }
            
            return objectMapper.writeValueAsString(defaultCases);
        } catch (Exception e) {
            log.error("生成默认测试用例失败", e);
            return "[{\"input\":\"测试输入\",\"expectedOutput\":\"期望输出\",\"description\":\"请配置具体测试用例\"}]";
        }
    }
    
    /**
     * 创建错误结果
     */
    private CodeJudgeResult createErrorResult(String errorMessage, String status) {
        CodeJudgeResult result = new CodeJudgeResult();
        result.setStatus(status);
        result.setScore(0);
        result.setPassedCount(0);
        result.setTotalCount(0);
        result.setErrorMessage(errorMessage);
        result.setTestCaseResults(new ArrayList<>());
        return result;
    }
    
    /**
     * 检查字符串是否为有效的JSON格式
     */
    private boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        
        try {
            objectMapper.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
