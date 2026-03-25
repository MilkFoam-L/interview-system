package interview.service;

import interview.ai.spark.SparkX1Model;

import java.util.List;

/**
 * AI代码判题服务接口
 */
public interface AICodeJudgeService {
    
    /**
     * 执行代码判题
     * 
     * @param problemDescription 题目描述
     * @param userCode 用户代码
     * @param language 编程语言
     * @param testCases 测试用例列表
     * @return 判题结果
     */
    SparkX1Model.JudgeResult judgeCode(String problemDescription, 
                                       String userCode, 
                                       String language, 
                                       List<SparkX1Model.CodeJudgeRequest.TestCase> testCases);
    
    /**
     * 快速判题（无测试用例）
     * 
     * @param problemDescription 题目描述
     * @param userCode 用户代码
     * @param language 编程语言
     * @return 判题结果
     */
    SparkX1Model.JudgeResult quickJudge(String problemDescription, 
                                        String userCode, 
                                        String language);
    
    /**
     * 代码质量分析
     * 
     * @param userCode 用户代码
     * @param language 编程语言
     * @return 分析结果
     */
    SparkX1Model.JudgeResult analyzeCodeQuality(String userCode, String language);
    
    /**
     * 检查服务是否可用
     * 
     * @return 是否可用
     */
    boolean isServiceAvailable();
    
    /**
     * 获取支持的编程语言列表
     * 
     * @return 语言列表
     */
    List<String> getSupportedLanguages();
}

