package interview.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 代码判题请求DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodeJudgeRequest {
    
    /**
     * 用户提交的代码
     */
    private String code;
    
    /**
     * 编程语言 (java, python, cpp, c)
     */
    private String language;
    
    /**
     * 测试用例数据 (JSON格式)
     */
    private String testCases;
    
    /**
     * 时间限制 (秒)
     */
    private Integer timeLimit = 5;
    
    /**
     * 内存限制 (MB)
     */
    private Integer memoryLimit = 128;
}
