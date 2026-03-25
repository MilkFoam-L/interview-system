package interview.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代码判题结果DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeJudgeResult {
    
    /**
     * 判题状态
     * AC - Accepted (通过)
     * WA - Wrong Answer (答案错误)
     * TLE - Time Limit Exceeded (超时)
     * MLE - Memory Limit Exceeded (内存超限)
     * CE - Compile Error (编译错误)  
     * RE - Runtime Error (运行时错误)
     * SE - System Error (系统错误)
     */
    private String status;
    
    /**
     * 得分 (0-100)
     */
    private Integer score;
    
    /**
     * 通过的测试用例数量
     */
    private Integer passedCount;
    
    /**
     * 总测试用例数量
     */
    private Integer totalCount;
    
    /**
     * 执行时间 (毫秒)
     */
    private Long executionTime;
    
    /**
     * 内存使用 (KB)
     */
    private Long memoryUsage;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 详细的测试用例结果
     */
    private List<TestCaseResult> testCaseResults;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestCaseResult {
        private String input;
        private String expectedOutput;
        private String actualOutput;
        private Boolean passed;
        private String error;
    }
}
