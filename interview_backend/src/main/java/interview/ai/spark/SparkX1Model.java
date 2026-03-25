package interview.ai.spark;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 星火X1模型数据结构
 * 定义与星火X1模型交互的请求和响应数据结构
 */
public class SparkX1Model {
    
    /**
     * 星火X1模型请求数据结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        
        /**
         * 用户ID
         */
        private String user;
        
        /**
         * 模型名称
         */
        private String model;
        
        /**
         * 消息列表
         */
        private List<Message> messages;
        
        /**
         * 是否流式返回
         */
        private Boolean stream;
        
        /**
         * 最大Token数
         */
        private Integer max_tokens;
        
        /**
         * 温度参数
         */
        private Double temperature;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Message {
            /**
             * 角色：user/assistant/system
             */
            private String role;
            
            /**
             * 消息内容
             */
            private String content;
        }
    }
    
    /**
     * 星火X1模型响应数据结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        
        /**
         * 响应ID
         */
        private String id;
        
        /**
         * 对象类型
         */
        private String object;
        
        /**
         * 创建时间
         */
        private Long created;
        
        /**
         * 模型名称
         */
        private String model;
        
        /**
         * 选择结果
         */
        private List<Choice> choices;
        
        /**
         * 使用情况
         */
        private Usage usage;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {
            /**
             * 索引
             */
            private Integer index;
            
            /**
             * 消息内容
             */
            private Message message;
            
            /**
             * 完成原因
             */
            private String finish_reason;
            
            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Message {
                /**
                 * 角色
                 */
                private String role;
                
                /**
                 * 内容
                 */
                private String content;
            }
        }
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Usage {
            /**
             * 提示Token数
             */
            private Integer prompt_tokens;
            
            /**
             * 完成Token数
             */
            private Integer completion_tokens;
            
            /**
             * 总Token数
             */
            private Integer total_tokens;
        }
    }
    
    /**
     * 代码判题结果数据结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JudgeResult {
        
        /**
         * 判题结果类型（AC、WA、CE等）
         */
        private String resultType;
        
        /**
         * 判题得分（0-100分）
         */
        private Integer score;
        
        /**
         * 详细评价
         */
        private String evaluation;
        
        /**
         * 代码分析
         */
        private String analysis;
        
        /**
         * 改进建议
         */
        private List<String> suggestions;
        
        /**
         * 通过的测试用例数
         */
        private Integer passedCases;
        
        /**
         * 总测试用例数
         */
        private Integer totalCases;
        
        /**
         * 错误信息（如果有）
         */
        private String errorMessage;
        
        /**
         * 执行时间（毫秒）
         */
        private Long executionTime;
        
        /**
         * 内存使用（MB）
         */
        private Double memoryUsage;
        
        /**
         * 是否通过
         */
        private Boolean passed;
    }
    
    /**
     * 代码判题请求数据结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeJudgeRequest {
        
        /**
         * 题目描述
         */
        private String problemDescription;
        
        /**
         * 用户提交的代码
         */
        private String userCode;
        
        /**
         * 编程语言
         */
        private String language;
        
        /**
         * 测试用例
         */
        private List<TestCase> testCases;
        
        /**
         * 时间限制（秒）
         */
        private Integer timeLimit;
        
        /**
         * 内存限制（MB）
         */
        private Integer memoryLimit;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TestCase {
            /**
             * 输入
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
    }
}