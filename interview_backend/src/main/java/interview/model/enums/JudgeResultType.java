package interview.model.enums;

/**
 * 判题结果类型枚举
 * 定义代码执行的各种结果状态
 */
public enum JudgeResultType {
    
    /**
     * 通过 - 代码正确，所有测试用例通过
     */
    ACCEPTED("AC", "通过", "代码正确，所有测试用例通过", true),
    
    /**
     * 答案错误 - 代码逻辑有误，输出结果不正确
     */
    WRONG_ANSWER("WA", "答案错误", "代码逻辑有误，输出结果不正确", false),
    
    /**
     * 编译错误 - 代码无法编译通过
     */
    COMPILE_ERROR("CE", "编译错误", "代码存在语法错误，无法编译", false),
    
    /**
     * 运行时错误 - 代码运行时发生异常
     */
    RUNTIME_ERROR("RE", "运行时错误", "代码运行时发生异常或错误", false),
    
    /**
     * 超时 - 代码执行时间超过限制
     */
    TIME_LIMIT_EXCEEDED("TLE", "超时", "代码执行时间超过限制", false),
    
    /**
     * 内存超限 - 代码使用内存超过限制
     */
    MEMORY_LIMIT_EXCEEDED("MLE", "内存超限", "代码使用内存超过限制", false),
    
    /**
     * 部分正确 - 部分测试用例通过
     */
    PARTIALLY_CORRECT("PC", "部分正确", "部分测试用例通过，但仍有错误", false),
    
    /**
     * 系统错误 - 判题系统内部错误
     */
    SYSTEM_ERROR("SE", "系统错误", "判题系统内部错误", false),
    
    /**
     * 待判题 - 代码提交成功，等待判题
     */
    PENDING("PD", "待判题", "代码已提交，正在等待判题", false),
    
    /**
     * 判题中 - 正在进行判题
     */
    JUDGING("JG", "判题中", "正在进行代码判题", false);
    
    private final String code;
    private final String displayName;
    private final String description;
    private final boolean isSuccess;
    
    JudgeResultType(String code, String displayName, String description, boolean isSuccess) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.isSuccess = isSuccess;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isSuccess() {
        return isSuccess;
    }
    
    /**
     * 根据代码字符串获取判题结果类型
     */
    public static JudgeResultType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        
        for (JudgeResultType type : values()) {
            if (type.getCode().equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否为最终状态（非中间状态）
     */
    public boolean isFinalState() {
        return this != PENDING && this != JUDGING;
    }
    
    /**
     * 检查是否为错误状态
     */
    public boolean isError() {
        return this == COMPILE_ERROR || 
               this == RUNTIME_ERROR || 
               this == SYSTEM_ERROR;
    }
    
    /**
     * 检查是否为超限状态
     */
    public boolean isLimitExceeded() {
        return this == TIME_LIMIT_EXCEEDED || 
               this == MEMORY_LIMIT_EXCEEDED;
    }
}
