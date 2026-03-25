package interview.service;

import interview.model.dto.CodeJudgeRequest;
import interview.model.dto.CodeJudgeResult;

/**
 * 代码判题服务接口
 */
public interface CodeJudgeService {
    
    /**
     * 执行代码判题
     * @param request 判题请求
     * @return 判题结果
     */
    CodeJudgeResult judge(CodeJudgeRequest request);
    
    /**
     * 检查编程语言是否支持
     * @param language 编程语言
     * @return 是否支持
     */
    boolean isSupportedLanguage(String language);
    
    /**
     * 解析题目中的测试用例数据
     * @param question 题目对象
     * @return 测试用例JSON字符串，如果没有测试用例返回null
     */
    String extractTestCases(interview.model.entity.Question question);
}
