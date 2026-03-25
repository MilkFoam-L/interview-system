package interview.ai.prompt;

import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * 个性化建议提示词构建器
 * 为星火X1.5模型构建个性化建议分析的提示词
 */
@Component
public class PersonalizedSuggestionsPromptBuilder {
    
    /**
     * 构建个性化建议分析提示词
     * 
     * @param interviewData 面试数据
     * @return 个性化建议分析提示词
     */
    public String buildPersonalizedSuggestionsPrompt(Map<String, Object> interviewData) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一位资深的职业发展导师和面试专家，请基于以下面试表现数据为候选人生成个性化的学习建议和改进方案。\n\n");
        
        // 添加面试数据上下文
        prompt.append("## 面试表现数据\n");
        
        // 基础问答表现
        if (interviewData.containsKey("basicQAAnalysis")) {
            Map<String, Object> basicQA = (Map<String, Object>) interviewData.get("basicQAAnalysis");
            prompt.append("### 基础问答环节\n");
            if (basicQA.containsKey("summary")) {
                prompt.append("表现总结: ").append(basicQA.get("summary")).append("\n");
            }
            if (basicQA.containsKey("score")) {
                prompt.append("得分: ").append(basicQA.get("score")).append("分\n");
            }
        }
        
        // 场景面试表现
        if (interviewData.containsKey("scenarioAnalysis")) {
            Map<String, Object> scenario = (Map<String, Object>) interviewData.get("scenarioAnalysis");
            prompt.append("### 场景面试环节\n");
            if (scenario.containsKey("starScores")) {
                prompt.append("STAR结构表现: ").append(scenario.get("starScores")).append("\n");
            }
        }
        
        // 代码测试表现
        if (interviewData.containsKey("codeTestAnalysis")) {
            Map<String, Object> codeTest = (Map<String, Object>) interviewData.get("codeTestAnalysis");
            prompt.append("### 代码测试环节\n");
            if (codeTest.containsKey("summary")) {
                prompt.append("编程能力评估: ").append(codeTest.get("summary")).append("\n");
            }
        }
        
        // 能力评估矩阵
        if (interviewData.containsKey("abilityMatrix")) {
            prompt.append("### 能力评估矩阵\n");
            prompt.append("各项能力得分: ").append(interviewData.get("abilityMatrix")).append("\n");
        }
        
        // 综合分析结果
        if (interviewData.containsKey("comprehensiveAnalysis")) {
            Map<String, Object> comprehensive = (Map<String, Object>) interviewData.get("comprehensiveAnalysis");
            prompt.append("### 综合表现评估\n");
            if (comprehensive.containsKey("overallAssessment")) {
                prompt.append("总体评估: ").append(comprehensive.get("overallAssessment")).append("\n");
            }
            if (comprehensive.containsKey("technicalScore")) {
                prompt.append("技术能力得分: ").append(comprehensive.get("technicalScore")).append("分\n");
            }
            if (comprehensive.containsKey("communicationScore")) {
                prompt.append("沟通表达得分: ").append(comprehensive.get("communicationScore")).append("分\n");
            }
            if (comprehensive.containsKey("learningScore")) {
                prompt.append("学习能力得分: ").append(comprehensive.get("learningScore")).append("分\n");
            }
        }
        
        // 分析要求
        prompt.append("\n## 分析要求\n");
        prompt.append("请基于以上面试表现数据，生成个性化的学习建议和改进方案。分析应包含：\n\n");
        
        prompt.append("1. **技能提升建议**: 针对技术能力薄弱环节的具体学习建议\n");
        prompt.append("2. **沟通改进方案**: 针对表达和沟通方面的改进建议\n");
        prompt.append("3. **学习路径规划**: 制定3-6个月的学习计划和里程碑\n");
        prompt.append("4. **实践项目推荐**: 推荐适合的实践项目来提升技能\n");
        prompt.append("5. **面试技巧优化**: 针对面试表现的具体改进建议\n");
        prompt.append("6. **职业发展建议**: 基于当前能力水平的职业发展路径建议\n\n");
        
        // 输出格式要求
        prompt.append("## 输出格式要求\n");
        prompt.append("请以JSON格式返回分析结果，结构如下：\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"skillImprovements\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"category\": \"技能分类\",\n");
        prompt.append("      \"currentLevel\": \"当前水平描述\",\n");
        prompt.append("      \"targetLevel\": \"目标水平描述\",\n");
        prompt.append("      \"suggestions\": [\"具体建议1\", \"具体建议2\"]\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"communicationImprovements\": {\n");
        prompt.append("    \"strengths\": [\"沟通优势1\", \"沟通优势2\"],\n");
        prompt.append("    \"weaknesses\": [\"待改进点1\", \"待改进点2\"],\n");
        prompt.append("    \"actionPlan\": [\"改进行动1\", \"改进行动2\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"learningPath\": {\n");
        prompt.append("    \"phase1\": {\n");
        prompt.append("      \"duration\": \"1-2个月\",\n");
        prompt.append("      \"focus\": \"学习重点\",\n");
        prompt.append("      \"goals\": [\"目标1\", \"目标2\"]\n");
        prompt.append("    },\n");
        prompt.append("    \"phase2\": {\n");
        prompt.append("      \"duration\": \"3-4个月\",\n");
        prompt.append("      \"focus\": \"学习重点\",\n");
        prompt.append("      \"goals\": [\"目标1\", \"目标2\"]\n");
        prompt.append("    },\n");
        prompt.append("    \"phase3\": {\n");
        prompt.append("      \"duration\": \"5-6个月\",\n");
        prompt.append("      \"focus\": \"学习重点\",\n");
        prompt.append("      \"goals\": [\"目标1\", \"目标2\"]\n");
        prompt.append("    }\n");
        prompt.append("  },\n");
        prompt.append("  \"practiceProjects\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"项目名称\",\n");
        prompt.append("      \"description\": \"项目描述\",\n");
        prompt.append("      \"difficulty\": \"难度等级\",\n");
        prompt.append("      \"skills\": [\"涉及技能1\", \"涉及技能2\"],\n");
        prompt.append("      \"duration\": \"预计完成时间\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"interviewTips\": {\n");
        prompt.append("    \"preparation\": [\"面试准备建议1\", \"面试准备建议2\"],\n");
        prompt.append("    \"presentation\": [\"表达技巧1\", \"表达技巧2\"],\n");
        prompt.append("    \"technical\": [\"技术面试建议1\", \"技术面试建议2\"]\n");
        prompt.append("  },\n");
        prompt.append("  \"careerAdvice\": {\n");
        prompt.append("    \"currentPosition\": \"当前职业定位\",\n");
        prompt.append("    \"nextSteps\": [\"下一步职业发展建议1\", \"建议2\"],\n");
        prompt.append("    \"longTermGoal\": \"长期职业目标建议\",\n");
        prompt.append("    \"industryTrends\": [\"行业趋势分析1\", \"趋势分析2\"]\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("注意：\n");
        prompt.append("1. 建议要具体、可操作，避免空泛的表述\n");
        prompt.append("2. 学习路径要循序渐进，符合认知规律\n");
        prompt.append("3. 项目推荐要结合实际技能水平，难度适中\n");
        prompt.append("4. 职业建议要结合当前市场需求和发展趋势\n");
        prompt.append("5. 确保返回的是有效的JSON格式\n");
        
        return prompt.toString();
    }
}
