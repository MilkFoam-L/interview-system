package interview.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 测试用例DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestCase {
    
    /**
     * 输入数据
     */
    private String input;
    
    /**
     * 期望输出
     */
    private String expectedOutput;
    
    /**
     * 测试用例描述
     */
    private String description;
}
