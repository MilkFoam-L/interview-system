package interview.service;

import interview.model.InterviewQuestionAnswer;
import interview.model.Resume;
import interview.model.entity.InterviewStageProgress;
import interview.model.entity.InterviewSession;
import interview.repository.InterviewQuestionAnswerRepository;
import interview.repository.InterviewStageProgressRepository;
import interview.repository.InterviewSessionRepository;
import interview.repository.ResumeRepository;
import interview.repository.ScenarioReportRepository;
import interview.repository.JobApplicationRepository;
import interview.repository.JobRepository;
import interview.model.JobApplication;
import interview.model.ScenarioReport;
import interview.model.ScenarioModel;
import interview.model.Job;
import interview.model.CompanyInfo;
import interview.ai.SparkClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;
import java.util.Random;

/**
 * Phase-0: 仅负责生成 5 个场景化问题并写入 interview_question_answers。
 */
@Service
public class ScenarioQuestionService {

    private static final Logger log = LoggerFactory.getLogger(ScenarioQuestionService.class);

    private static final String[] DIMENSIONS = {
            "核心技术能力", "关键项目经验", "团队协作能力",
            "职业发展规划", "工作态度与价值观"
    };

    private final InterviewSessionRepository sessionRepository;
    private final ResumeRepository resumeRepository;
    private final InterviewQuestionAnswerRepository qaRepository;
    private final InterviewStageProgressRepository stageProgressRepository;
    private final ScenarioReportRepository reportRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final SparkClient sparkClient; // 添加SparkClient
    private final ComprehensiveReportService comprehensiveReportService; // 添加综合分析报告服务
    private final ObjectMapper objectMapper;

    @Autowired
    public ScenarioQuestionService(InterviewSessionRepository sessionRepository,
                                   ResumeRepository resumeRepository,
                                   InterviewQuestionAnswerRepository qaRepository,
                                   InterviewStageProgressRepository stageProgressRepository,
                                   ScenarioReportRepository reportRepository,
                                   JobApplicationRepository jobApplicationRepository,
                                   JobRepository jobRepository,
                                   SparkClient sparkClient,
                                   ComprehensiveReportService comprehensiveReportService) { // 注入综合分析报告服务
        this.sessionRepository = sessionRepository;
        this.resumeRepository = resumeRepository;
        this.qaRepository = qaRepository;
        this.stageProgressRepository = stageProgressRepository;
        this.reportRepository = reportRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobRepository = jobRepository;
        this.sparkClient = sparkClient; // 保存SparkClient
        this.comprehensiveReportService = comprehensiveReportService; // 保存综合分析报告服务
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取岗位详细信息
     * @param jobId 岗位ID
     * @return 格式化的岗位信息，包括岗位需求、职责等
     */
    private String getJobDetails(Integer jobId) {
        if (jobId == null) {
            return "";
        }
        
        try {
            Job job = jobRepository.findByIdWithCompany(jobId).orElse(null);
            if (job == null) {
                return "";
            }
            
            StringBuilder jobDetails = new StringBuilder();
            jobDetails.append("岗位名称: ").append(job.getTitle()).append("\n");
            
            // 添加公司信息
            if (job.getCompany() != null) {
                CompanyInfo company = job.getCompany();
                jobDetails.append("公司名称: ").append(company.getCompanyName()).append("\n");
                jobDetails.append("公司行业: ").append(company.getIndustry()).append("\n");
            }
            
            // 添加岗位要求
            if (job.getRequirements() != null && !job.getRequirements().isBlank()) {
                jobDetails.append("岗位要求: ").append(job.getRequirements()).append("\n");
            }
            
            // 添加工作职责
            if (job.getDuties() != null && !job.getDuties().isBlank()) {
                jobDetails.append("工作职责: ").append(job.getDuties()).append("\n");
            }
            
            // 添加经验和学历要求
            if (job.getExperience() != null && !job.getExperience().isBlank()) {
                jobDetails.append("经验要求: ").append(job.getExperience()).append("\n");
            }
            
            if (job.getEducation() != null && !job.getEducation().isBlank()) {
                jobDetails.append("学历要求: ").append(job.getEducation()).append("\n");
            }
            
            return jobDetails.toString();
        } catch (Exception e) {
            log.error("获取岗位详情失败", e);
            return "";
        }
    }
    
    /**
     * 获取历史问答记录
     * @param sessionId 面试会话ID
     * @return 历史问答记录
     */
    private String getHistoricalQA(Long sessionId) {
        List<InterviewQuestionAnswer> historicalQA = qaRepository.findBySessionIdOrderByRoundNo(sessionId);
        if (historicalQA.isEmpty()) {
            return "";
        }
        
        StringBuilder qaHistory = new StringBuilder("历史问答记录:\n");
        for (InterviewQuestionAnswer qa : historicalQA) {
            if (qa.getQuestion() != null && qa.getAnswer() != null) {
                qaHistory.append("问题: ").append(qa.getQuestion()).append("\n");
                qaHistory.append("回答: ").append(qa.getAnswer()).append("\n\n");
            }
        }
        
        return qaHistory.toString();
    }

    /**
     * 初始化场景化问答阶段：
     * 1. 读取简历文本
     * 2. 生成每个维度的问题（简单模板）
     * 3. 写入 interview_question_answers
     * 4. 更新 stage_progress = IN_PROGRESS
     *
     * @return 已保存的问题列表
     */
    @Transactional
    public List<InterviewQuestionAnswer> startScenario(Long sessionId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("面试会话不存在: " + sessionId));

        // 更新阶段进度为 IN_PROGRESS
        stageProgressRepository.findBySessionId(sessionId).stream()
                .filter(p -> "SCENARIO".equals(p.getStage()))
                .findFirst()
                .ifPresent(p -> {
                    p.setStatus("IN_PROGRESS");
                    p.setStartTime(LocalDateTime.now());
                    stageProgressRepository.save(p);
                });

        // ------ 读取简历文本 ------
        String resumeText;
        Integer jobId = null;
        JobApplication jobApplication = null;

        // 1) 优先使用本次投递的简历（通过 jobApplicationId → resumeId）
        Resume targetResume = null;
        if (session.getJobApplicationId() != null) {
            jobApplication = jobApplicationRepository.findById(session.getJobApplicationId()).orElse(null);
            if (jobApplication != null) {
                if (jobApplication.getResumeId() != null) {
                    targetResume = resumeRepository.findById(jobApplication.getResumeId()).orElse(null);
                }
                jobId = jobApplication.getJobId();
            }
        }

        // 2) 若仍未找到，退回到用户最新上传的简历
        if (targetResume == null) {
            resumeText = fetchResumeText(session.getUserId());
        } else {
            resumeText = Stream.of(targetResume.getSummary(), targetResume.getContent())
                              .filter(Objects::nonNull)
                              .filter(s -> !s.isBlank())
                              .findFirst()
                              .orElse("");
        }
        
        // 获取岗位详情
        String jobDetails = getJobDetails(jobId);
        
        // 获取历史问答记录
        String historicalQA = getHistoricalQA(sessionId);

        // 生成并保存问题
        List<InterviewQuestionAnswer> result = new ArrayList<>();
        for (int i = 0; i < DIMENSIONS.length; i++) {
            String dim = DIMENSIONS[i];
            String question = generateQuestionAI(dim, resumeText, jobDetails, historicalQA);

            InterviewQuestionAnswer qa = new InterviewQuestionAnswer();
            qa.setSessionId(sessionId);
            qa.setRoundNo(i + 1);
            qa.setDimension(dim);
            qa.setQuestion(question);
            qa.setAnswer(null);
            qaRepository.save(qa);
            result.add(qa);
        }
        return qaRepository.findBySessionIdOrderByRoundNo(sessionId);
    }

    /**
     * 获取已生成的问题（按 roundNo 排序）。
     */
    public List<InterviewQuestionAnswer> listQuestions(Long sessionId) {
        return qaRepository.findBySessionIdOrderByRoundNo(sessionId);
    }

    /**
     * 提交某一轮的回答；Phase-0 仅保存文本，不做评分。
     */
    @Transactional
    public InterviewQuestionAnswer submitAnswer(Long sessionId, Integer roundNo, String answer) {
        List<InterviewQuestionAnswer> list = qaRepository.findBySessionIdAndRoundNo(sessionId, roundNo);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("找不到对应的问题，session=" + sessionId + " round=" + roundNo);
        }
        
        InterviewQuestionAnswer qa = list.get(0);
        
        // 如果已有回答，则将新回答追加到现有回答中
        if (qa.getAnswer() != null && !qa.getAnswer().isEmpty()) {
            qa.setAnswer(qa.getAnswer() + "\n\n应聘者回答：" + answer);
        } else {
            qa.setAnswer(answer);
        }

        // ----- Phase-1 评分逻辑 -----
        try {
            // 评分时只考虑最后一次回答
            String latestAnswer = answer; 
            String evalPrompt = ScenarioModel.evaluateAnswer(qa.getQuestion().split("\n\n追问：")[0], latestAnswer, roundNo - 1);
            String evalResp   = sparkClient.ask(evalPrompt); // 使用实例方法

            int relevanceScore = 0, completeness = 0, logic = 0, specificity = 0;
            String feedback = "";

            for (String line : evalResp.split("\n")) {
                line = line.trim();
                if (line.startsWith("相关性得分：")) {
                    relevanceScore = parseIntSafe(line.substring(6));
                } else if (line.startsWith("完整性得分：")) {
                    completeness = parseIntSafe(line.substring(6));
                } else if (line.startsWith("逻辑性得分：")) {
                    logic = parseIntSafe(line.substring(6));
                } else if (line.startsWith("具体性得分：")) {
                    specificity = parseIntSafe(line.substring(6));
                } else if (line.startsWith("总体评价：")) {
                    feedback = line.substring(5).trim();
                }
            }

            int totalScore = relevanceScore + completeness + logic + specificity;
            qa.setScore(totalScore);
            qa.setRelevance(relevanceScore / 30.0);
            qa.setFeedback(feedback);

            // 构造 AnswerScore 并放入全局，供总评分使用
            ScenarioModel.AnswerScore as = new ScenarioModel.AnswerScore(DIMENSIONS[roundNo-1]);
            as.setRelevanceScore(relevanceScore);
            as.setCompletenessScore(completeness);
            as.setLogicScore(logic);
            as.setSpecificityScore(specificity);
            as.setFeedback(feedback);
            ScenarioModel.answerScores.put(roundNo-1, as);

        } catch (Exception e) {
            // 调用大模型失败，回退到默认分数
            int fallbackScore = 60;
            qa.setScore(fallbackScore);
            qa.setRelevance(0.5);
            qa.setFeedback("AI 评分失败，使用默认分数");
        }

        qaRepository.save(qa);
        return qa;
    }
    
    /**
     * 生成追问，并将其添加到现有问题中
     */
    @Transactional
    public InterviewQuestionAnswer generateFollowUpQuestion(Long sessionId, Integer roundNo) {
        List<InterviewQuestionAnswer> list = qaRepository.findBySessionIdAndRoundNo(sessionId, roundNo);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("找不到对应的问题，session=" + sessionId + " round=" + roundNo);
        }
        
        InterviewQuestionAnswer qa = list.get(0);
        
        // 如果没有回答，无法生成追问
        if (qa.getAnswer() == null || qa.getAnswer().isEmpty()) {
            throw new IllegalArgumentException("应聘者尚未回答问题，无法生成追问");
        }
        
        String originalQuestion = qa.getQuestion();
        String lastAnswer = qa.getAnswer();
        
        // 如果有多轮对话，获取最后一个回答
        if (lastAnswer.contains("\n\n应聘者回答：")) {
            String[] parts = lastAnswer.split("\n\n应聘者回答：");
            lastAnswer = parts[parts.length - 1];
        }
        
        try {
            // 获取岗位信息，用于生成更相关的追问
            JobApplication jobApplication = null;
            Integer jobId = null;
            
            // 通过sessionId获取interviewSession
            InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
            if (session != null && session.getJobApplicationId() != null) {
                jobApplication = jobApplicationRepository.findById(session.getJobApplicationId()).orElse(null);
                if (jobApplication != null) {
                    jobId = jobApplication.getJobId();
                }
            }
            
            // 获取岗位详情
            String jobDetails = getJobDetails(jobId);
            
            // 获取完整的问答历史
            String questionHistory = qa.getQuestion();
            String answerHistory = qa.getAnswer();
            
            // 解析出所有之前的问题和追问
            String[] questionParts = questionHistory.split("\n\n追问：");
            String initialQuestion = questionParts[0]; // 原始问题
            
            // 构建问答历史
            StringBuilder conversationHistory = new StringBuilder();
            conversationHistory.append("原始问题：").append(initialQuestion).append("\n\n");
            
            // 添加之前的追问和回答（如果有）
            String[] answerParts = answerHistory.split("\n\n应聘者回答：");
            for (int i = 0; i < Math.min(questionParts.length - 1, answerParts.length - 1); i++) {
                conversationHistory.append("追问").append(i + 1).append("：").append(questionParts[i + 1]).append("\n\n");
                conversationHistory.append("回答").append(i + 1).append("：").append(answerParts[i + 1]).append("\n\n");
            }
            
            // 添加最新回答
            conversationHistory.append("最新回答：").append(lastAnswer);
            
            // 预先检查回答是否过于简短或无意义
            boolean isValidAnswer = isAnswerValid(lastAnswer);
            if (!isValidAnswer) {
                log.info("回答过于简短或无意义，直接跳过追问: {}", lastAnswer);
                return null;
            }
            
            // 使用简化的追问判断逻辑
            boolean shouldAskFollowUp = shouldAskFollowUpSimple(lastAnswer, originalQuestion.split("\n\n追问：")[0]);
            String noFollowUpReason = shouldAskFollowUp ? "" : "回答被判定为低质量，不进行追问";
            
            // 如果回答质量不足，返回null表示不需要追问
            if (!shouldAskFollowUp) {
                log.info("不进行追问，原因: {}", noFollowUpReason);
                return null;
            }
            
            // 继续生成追问的提示词，包含岗位信息
            StringBuilder promptContext = new StringBuilder(conversationHistory.toString());
            
            if (!jobDetails.isEmpty()) {
                promptContext.append("\n\n=== 应聘岗位信息 ===\n").append(jobDetails);
            }

            // 提取关键维度信息的定义和侧重点
            String dimensionFocus;
            switch(qa.getDimension()) {
                case "核心技术能力":
                    dimensionFocus = "技术精通度、解决方案设计能力、技术创新能力、技术难题解决经验";
                    break;
                case "关键项目经验":
                    dimensionFocus = "项目管理能力、团队协作、技术选型、解决项目中的挑战、项目成果";
                    break;
                case "团队协作能力":
                    dimensionFocus = "沟通能力、冲突解决、团队角色、跨部门协作、团队领导力";
                    break;
                case "职业发展规划":
                    dimensionFocus = "长期职业目标、自我提升计划、对行业趋势的认识、职业发展路径";
                    break;
                case "工作态度与价值观":
                    dimensionFocus = "工作动机、职业道德、责任心、对压力的态度、自我激励";
                    break;
                default:
                    dimensionFocus = "综合面试能力";
            }
            
            // 随机选择追问风格
            String followUpStyle = getRandomFollowUpStyle(qa.getDimension());
            
            String prompt = String.format(
                "你是一位经验丰富、善于倾听的资深面试官，正在与候选人进行关于\"%s\"的深入交流。\n\n" +
                "===对话背景===\n%s\n===背景结束===\n\n" +
                "候选人刚才的回答很有价值，现在需要进一步了解一些细节。请以自然、专业的方式继续这次对话：\n\n" +
                "💡 追问指导原则：\n" +
                "• 表现出对候选人回答的兴趣和认可\n" +
                "%s\n" +
                "• 重点关注%s方面，但要让对话流畅自然\n" +
                "• 从候选人的回答中找到一个具体的点来深入\n" +
                "• 营造轻松的对话氛围，避免审问式的提问\n" +
                "• 控制在40字以内，让问题听起来自然真诚\n" +
                "• 避免套路化表达，使用多样化的问法和开场\n\n" +
                "请直接以面试官的口吻说出下一个问题，让对话自然延续：",
                qa.getDimension(),
                promptContext.toString(),
                followUpStyle,
                dimensionFocus
            );
            
            String followUpQuestion = sparkClient.ask(prompt);
            
            // 清理大模型返回的内容
            followUpQuestion = followUpQuestion.trim();
            if (followUpQuestion.startsWith("追问：")) {
                followUpQuestion = followUpQuestion.substring(3).trim();
            }
            
            // 去掉可能的前缀
            if (followUpQuestion.startsWith("问题：") || followUpQuestion.startsWith("问题:")) {
                followUpQuestion = followUpQuestion.substring(3).trim();
            }
            
            // 简单的后处理，确保问题完整且自然
            if (followUpQuestion.length() > 0 && !followUpQuestion.endsWith("？") && !followUpQuestion.endsWith("?")) {
                if (followUpQuestion.contains("吗") || followUpQuestion.contains("呢") || 
                    followUpQuestion.contains("如何") || followUpQuestion.contains("怎么")) {
                    followUpQuestion += "？";
                }
            }
            
            // 更新问题字段，添加追问
            if (qa.getQuestion().contains("\n\n追问：")) {
                // 已有追问，添加新的追问
                qa.setQuestion(qa.getQuestion() + "\n\n追问：" + followUpQuestion);
            } else {
                // 第一次追问
                qa.setQuestion(originalQuestion + "\n\n追问：" + followUpQuestion);
            }
            
            qaRepository.save(qa);
            return qa;
        } catch (Exception e) {
            log.error("生成追问失败", e);
            throw new RuntimeException("生成追问失败: " + e.getMessage());
        }
    }
    
    /**
     * 预先检查回答是否有效（不是过于简短或无意义的内容）
     * 修改为更宽松的检查，避免误判有效回答
     */
    private boolean isAnswerValid(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            log.info("回答为空，判定为无效");
            return false;
        }
        
        String trimmedAnswer = answer.trim();
        log.info("检查回答有效性: [{}], 长度: {}", trimmedAnswer, trimmedAnswer.length());
        
        // 检查纯数字（包括多位数字）
        if (trimmedAnswer.matches("^\\d+$")) {
            log.info("回答为纯数字: [{}]，判定为无效", trimmedAnswer);
            return false;
        }
        
        // 检查各种Test变体
        if (trimmedAnswer.matches("(?i)^(test|testing|测试|tst|t|tt|ttt)$")) {
            log.info("回答为测试文本: [{}]，判定为无效", trimmedAnswer);
            return false;
        }
        
        // 检查重复字符（如：aaa, 111, ...)
        if (trimmedAnswer.matches("^(.)\\1{2,}$")) {
            log.info("回答为重复字符: [{}]，判定为无效", trimmedAnswer);
            return false;
        }
        
        // 检查乱码模式
        if (isGarbledText(trimmedAnswer)) {
            log.info("回答疑似乱码: [{}]，判定为无效", trimmedAnswer);
            return false;
        }
        
        // 检查明显无意义的内容
        String[] invalidPatterns = {
            "^[a-zA-Z]$",           // 单个字母
            "^[好坏是否对错]$",        // 单个简单词
            "^[哦嗯啊呃嗯呢]$",       // 单个语气词
            "^[\\s\\p{Punct}]+$",   // 纯标点符号和空格
            "^(ok|OK|yes|no|YES|NO)$", // 简单英文回应
            "^[。，！？；：\\u201c\\u201d\\u2018\\u2019（）【】]$", // 单个中文标点
            "^(好的|是的|不是|没有)$"    // 简单中文回应
        };
        
        for (String pattern : invalidPatterns) {
            if (trimmedAnswer.matches(pattern)) {
                log.info("回答匹配无效模式: [{}]，判定为无效", pattern);
                return false;
            }
        }
        
        // 检查是否为极短且无意义的回答
        if (trimmedAnswer.length() <= 2) {
            log.info("回答过短且可能无意义: [{}]，判定为无效", trimmedAnswer);
            return false;
        }
        
        log.info("回答通过基本有效性检查，交由AI评估");
        return true;
    }
    
    /**
     * 检查是否为乱码文本
     */
    private boolean isGarbledText(String text) {
        if (text == null || text.length() < 3) {
            return false;
        }
        
        // 检查是否包含大量特殊字符或控制字符
        int specialCharCount = 0;
        int totalChars = text.length();
        
        for (char c : text.toCharArray()) {
            // 控制字符或非打印字符
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') {
                specialCharCount++;
            }
            // 大量连续的特殊符号
            else if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && 
                     !"，。！？；：\u201c\u201d\u2018\u2019（）【】、".contains(String.valueOf(c))) {
                specialCharCount++;
            }
        }
        
        // 如果特殊字符占比超过50%，可能是乱码
        double specialCharRatio = (double) specialCharCount / totalChars;
        if (specialCharRatio > 0.5) {
            return true;
        }
        
        // 检查是否为随机字符组合（连续的无意义字符）
        if (text.matches(".*[a-zA-Z]{5,}.*") && !containsCommonWords(text)) {
            // 包含长串字母但不包含常见单词，可能是乱码
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否包含常见单词
     */
    private boolean containsCommonWords(String text) {
        String lowerText = text.toLowerCase();
        String[] commonWords = {
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one", "our", "out", "day", "get", "has", "him", "his", "how", "man", "new", "now", "old", "see", "two", "way", "who", "boy", "did", "its", "let", "put", "say", "she", "too", "use",
            "我", "你", "他", "她", "的", "了", "在", "是", "有", "和", "就", "都", "会", "说", "可以", "这个", "那个", "什么", "怎么", "为什么", "因为", "所以", "但是", "如果", "虽然", "项目", "技术", "开发", "工作", "经验", "学习", "使用", "实现", "问题", "解决"
        };
        
        for (String word : commonWords) {
            if (lowerText.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 简化版追问判断：只判断是否为低质量回答
     * 对于非低质量回答，默认都进行追问
     */
    private boolean shouldAskFollowUpSimple(String answer, String question) {
        String simpleEvalPrompt = String.format(
            "判断以下回答是否为低质量回答（返回true表示低质量，false表示可以追问）：\n\n" +
            "问题：%s\n" +
            "回答：%s\n\n" +
            "低质量回答包括：\n" +
            "1. 完全无关的内容：\n" +
            "   - 日常闲聊（如\"你吃饭了吗\"、\"今天天气真好\"、\"你好吗\"）\n" +
            "   - 与面试或工作完全无关的话题\n" +
            "   - 问候语或客套话\n" +
            "2. 无意义内容：\n" +
            "   - 纯数字（如\"123\"、\"456\"、\"1\"）\n" +
            "   - 测试文本（如\"test\"、\"testing\"、\"测试\"）\n" +
            "   - 重复字符（如\"aaa\"、\"111\"、\"...\"）\n" +
            "   - 乱码或随机字符串\n" +
            "   - 单个词汇（如单独的\"好\"、\"是\"、\"ok\"）\n" +
            "3. 敷衍回答：\n" +
            "   - 明显不认真的回答\n" +
            "   - 故意回避问题的内容\n\n" +
            "注意：\n" +
            "- 即使回答很简短，只要与面试问题相关且有实际内容，就不算低质量\n" +
            "- 技术相关的简短回答（如\"用过Spring Boot\"、\"做过电商项目\"）是有效的\n" +
            "- 工作经验相关的简短描述是有效的\n\n" +
            "请只返回：true 或 false",
            question, answer
        );
        
        try {
            String result = sparkClient.ask(simpleEvalPrompt);
            boolean isLowQuality = result.trim().toLowerCase().contains("true");
            log.info("简化评估 - 问题: {}, 回答: {}, 是否低质量: {}", 
                    question.length() > 50 ? question.substring(0, 50) + "..." : question, 
                    answer.length() > 50 ? answer.substring(0, 50) + "..." : answer, 
                    isLowQuality);
            return !isLowQuality; // 返回是否应该追问
        } catch (Exception e) {
            log.warn("简化评估失败，默认进行追问: {}", e.getMessage());
            return true; // 默认追问
        }
    }
    
    /**
     * 随机选择问题风格模板，避免模板化问题
     */
    private String getRandomQuestionStyle(String dimension) {
        Random random = new Random();
        
        // 通用问题风格模板
        String[] generalStyles = {
            "• 自然开场：可以从简历中的某个亮点或经历自然引入话题，如\"看到你在XX项目中...\"、\"你的XX经验很丰富...\"",
            "• 场景导入：直接描述一个工作场景，然后询问处理方式，如\"假设在项目中遇到...\"、\"如果你需要...\"",
            "• 经验探讨：直接询问相关经验和看法，如\"能分享一下你在XX方面的经验吗？\"、\"你是如何看待...\"",
            "• 问题解决：提出一个实际问题让候选人分析，如\"如果遇到XX情况，你会怎么处理？\"",
            "• 技术深入：从技术角度直接切入，如\"在XX技术选择上...\"、\"关于XX的实现...\"",
            "• 团队协作：从团队合作角度提问，如\"在团队中你通常...\"、\"与同事协作时...\"",
            "• 挑战回顾：询问过往的挑战和解决方案，如\"遇到过什么技术难题？\"、\"最具挑战性的项目是...\""
        };
        
        // 根据维度添加特定风格
        String[] dimensionSpecificStyles = getDimensionSpecificStyles(dimension);
        
        // 合并通用和维度特定的风格
        String[] allStyles = new String[generalStyles.length + dimensionSpecificStyles.length];
        System.arraycopy(generalStyles, 0, allStyles, 0, generalStyles.length);
        System.arraycopy(dimensionSpecificStyles, 0, allStyles, generalStyles.length, dimensionSpecificStyles.length);
        
        return allStyles[random.nextInt(allStyles.length)];
    }
    
    /**
     * 随机选择追问风格模板，避免模板化追问
     */
    private String getRandomFollowUpStyle(String dimension) {
        Random random = new Random();
        
        // 通用追问风格模板
        String[] generalFollowUpStyles = {
            "• 深入挖掘：基于回答内容深入提问，如\"能具体说说...\"、\"这个过程中...\"、\"你是怎么...\"",
            "• 经验细节：询问具体的操作细节，如\"具体是如何实现的？\"、\"当时的情况是...\"",
            "• 思路探索：了解思考过程，如\"你的考虑是什么？\"、\"为什么选择这种方法？\"",
            "• 对比分析：询问不同方案的比较，如\"还有其他方案吗？\"、\"相比之下...\"",
            "• 结果影响：关注结果和影响，如\"最终效果如何？\"、\"带来了什么改变？\"",
            "• 挑战应对：了解遇到的困难，如\"遇到什么困难？\"、\"最大的挑战是...\"",
            "• 团队协作：询问团队合作情况，如\"团队是如何配合的？\"、\"其他人的反应...\"",
            "• 后续发展：关注后续情况，如\"后来怎么样了？\"、\"有什么后续计划？\""
        };
        
        // 根据维度添加特定的追问风格
        String[] dimensionSpecificFollowUps = getDimensionSpecificFollowUpStyles(dimension);
        
        // 合并通用和维度特定的风格
        String[] allStyles = new String[generalFollowUpStyles.length + dimensionSpecificFollowUps.length];
        System.arraycopy(generalFollowUpStyles, 0, allStyles, 0, generalFollowUpStyles.length);
        System.arraycopy(dimensionSpecificFollowUps, 0, allStyles, generalFollowUpStyles.length, dimensionSpecificFollowUps.length);
        
        return allStyles[random.nextInt(allStyles.length)];
    }
    
    /**
     * 获取维度特定的追问风格
     */
    private String[] getDimensionSpecificFollowUpStyles(String dimension) {
        switch (dimension) {
            case "技术能力":
                return new String[]{
                    "• 技术深度：深入技术细节，如\"具体用了什么技术？\"、\"性能如何优化的？\"",
                    "• 架构决策：询问技术选择，如\"为什么选择这个框架？\"、\"架构上有什么考虑？\"",
                    "• 问题解决：了解解决过程，如\"怎么调试的？\"、\"如何定位问题的？\""
                };
            case "沟通协调":
                return new String[]{
                    "• 沟通过程：了解沟通细节，如\"是怎么沟通的？\"、\"对方的反应如何？\"",
                    "• 协调方式：询问协调策略，如\"如何达成一致的？\"、\"怎么平衡各方需求？\"",
                    "• 冲突处理：关注冲突解决，如\"当时是什么情况？\"、\"最终如何解决的？\""
                };
            case "逻辑思维":
                return new String[]{
                    "• 分析过程：了解分析步骤，如\"分析思路是什么？\"、\"考虑了哪些因素？\"",
                    "• 决策依据：询问决策理由，如\"基于什么做的决定？\"、\"权衡了哪些方面？\"",
                    "• 验证方式：关注验证过程，如\"如何验证可行性？\"、\"怎么确保正确性？\""
                };
            case "学习能力":
                return new String[]{
                    "• 学习过程：了解学习方法，如\"是怎么学的？\"、\"用了多长时间？\"",
                    "• 实践应用：询问应用情况，如\"学完后如何实践的？\"、\"遇到什么困难？\"",
                    "• 持续提升：关注持续学习，如\"还在继续学习吗？\"、\"有什么心得？\""
                };
            case "抗压能力":
                return new String[]{
                    "• 压力感受：了解压力体验，如\"当时压力大吗？\"、\"感觉如何？\"",
                    "• 应对方式：询问应对策略，如\"是怎么调节的？\"、\"用了什么方法？\"",
                    "• 效果反馈：关注应对效果，如\"效果怎么样？\"、\"有什么收获？\""
                };
            default:
                return new String[]{
                    "• 具体细节：了解更多细节，如\"能展开说说吗？\"、\"具体是什么情况？\"",
                    "• 个人感受：询问个人体验，如\"你的感受如何？\"、\"有什么体会？\""
                };
        }
    }

    /**
     * 获取维度特定的问题风格
     */
    private String[] getDimensionSpecificStyles(String dimension) {
        switch (dimension) {
            case "技术能力":
                return new String[]{
                    "• 技术深度：从具体技术栈入手，如\"在XX技术的使用上...\"、\"关于XX框架的选择...\"",
                    "• 架构思维：从系统设计角度提问，如\"如何设计一个...\"、\"系统架构上你会考虑...\"",
                    "• 代码质量：关注编程实践，如\"代码review中你关注哪些点？\"、\"如何保证代码质量？\""
                };
            case "沟通协调":
                return new String[]{
                    "• 沟通场景：描述具体沟通情境，如\"当与产品经理意见不一致时...\"、\"如何向非技术人员解释...\"",
                    "• 团队协作：关注团队合作，如\"在跨团队协作中...\"、\"如何处理团队内部分歧？\"",
                    "• 冲突解决：询问冲突处理，如\"遇到技术分歧时...\"、\"如何平衡不同的需求？\""
                };
            case "逻辑思维":
                return new String[]{
                    "• 问题分析：提出分析型问题，如\"如何分析这个问题？\"、\"你的思路是什么？\"",
                    "• 方案对比：询问方案选择，如\"有几种解决方案...\"、\"如何权衡不同方案？\"",
                    "• 系统思维：从全局角度提问，如\"从整体上看...\"、\"如何确保方案的可行性？\""
                };
            case "学习能力":
                return new String[]{
                    "• 学习经历：询问学习过程，如\"是如何学习XX技术的？\"、\"在学习新技术时...\"",
                    "• 适应能力：关注适应性，如\"面对新的技术栈...\"、\"如何快速上手新项目？\"",
                    "• 持续改进：询问成长意识，如\"如何保持技术更新？\"、\"有什么学习计划？\""
                };
            case "抗压能力":
                return new String[]{
                    "• 压力场景：描述高压情境，如\"在紧急项目中...\"、\"当面临多个deadline时...\"",
                    "• 挑战应对：询问挑战处理，如\"最大的工作压力是什么？\"、\"如何应对突发情况？\"",
                    "• 情绪管理：关注心理调节，如\"如何保持工作状态？\"、\"压力大时如何调节？\""
                };
            default:
                return new String[]{
                    "• 实际应用：结合工作场景提问，如\"在实际工作中...\"、\"你会如何处理...\"",
                    "• 经验分享：询问相关经验，如\"能举个例子说明...\"、\"你的做法通常是...\""
                };
        }
    }

    /**
     * 从JSON字符串中提取指定字段的值
     */
    private String extractJsonValue(String jsonStr, String fieldName) {
        try {
            String pattern = "\"" + fieldName + "\"\\s*:\\s*";
            int startIndex = jsonStr.indexOf(pattern);
            if (startIndex == -1) {
                return null;
            }
            
            startIndex += pattern.length();
            
            // 跳过可能的引号
            if (jsonStr.charAt(startIndex) == '"') {
                startIndex++;
                int endIndex = jsonStr.indexOf('"', startIndex);
                if (endIndex > startIndex) {
                    return jsonStr.substring(startIndex, endIndex);
                }
            } else {
                // 数字或布尔值
                int endIndex = startIndex;
                while (endIndex < jsonStr.length() && 
                       (Character.isDigit(jsonStr.charAt(endIndex)) || 
                        jsonStr.charAt(endIndex) == '.' ||
                        Character.isLetter(jsonStr.charAt(endIndex)))) {
                    endIndex++;
                }
                if (endIndex > startIndex) {
                    return jsonStr.substring(startIndex, endIndex).replaceAll("[,}\\s].*", "");
                }
            }
        } catch (Exception e) {
            log.warn("提取JSON字段值失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 检查是否完成所有问答并更新阶段状态
     */
    @Transactional
    public void checkAndCompleteStage(Long sessionId) {
        // 检查每个roundNo是否都有回答
        boolean allAnswered = true;
        for (int i = 1; i <= DIMENSIONS.length; i++) {
            List<InterviewQuestionAnswer> qaList = qaRepository.findBySessionIdAndRoundNo(sessionId, i);
            if (qaList.isEmpty() || qaList.get(0).getAnswer() == null || qaList.get(0).getAnswer().isEmpty()) {
                allAnswered = false;
                break;
            }
        }
        
        // 如全部回答完毕，则将阶段置为 COMPLETED
        if (allAnswered) {
            InterviewStageProgress p = stageProgressRepository.findBySessionIdAndStage(sessionId, "SCENARIO");
            if (p != null) {
                p.setStatus("COMPLETED");
                p.setEndTime(LocalDateTime.now());
                if (p.getStartTime() != null) {
                    long dur = java.time.Duration.between(p.getStartTime(), p.getEndTime()).getSeconds();
                    p.setDuration((int) dur);
                }
                stageProgressRepository.save(p);
            }

            // 生成综合报告
            generateScenarioReport(sessionId);
        }
    }

    private void generateScenarioReport(Long sessionId) {
        List<InterviewQuestionAnswer> all = qaRepository.findBySessionIdOrderByRoundNo(sessionId);
        if (all.isEmpty()) return;

        // 调用 ScenarioModel 计算总分
        try {
            double rawScore = ScenarioModel.calculateWeightedTotalScore();
            double totalScore = new java.math.BigDecimal(rawScore).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();
            String level = totalScore >= 90 ? "优秀" : totalScore >= 80 ? "良好" : totalScore >= 70 ? "中等" : totalScore >= 60 ? "及格" : "不及格";

            ScenarioReport report = new ScenarioReport();
            report.setSessionId(sessionId);
            report.setTotalScore(totalScore);
            report.setScore(totalScore);
            report.setLevel(level);
            String reportText = ScenarioModel.generateScoreReport();

            // 组合问答文本 Q1,A1,...
            StringBuilder qaBuilder = new StringBuilder();
            all.forEach(qa -> {
                qaBuilder.append("Q").append(qa.getRoundNo()).append(": ")
                         .append(qa.getQuestion() == null ? "" : qa.getQuestion()).append("\n")
                         .append("A").append(qa.getRoundNo()).append(": ")
                         .append(qa.getAnswer() == null ? "" : qa.getAnswer()).append("\n");
            });

            report.setContent(qaBuilder.toString().trim()); // 存问答内容
            report.setReport(reportText); // 保持报告字段

            // ------ 关联 resumeId ------
            InterviewSession session = sessionRepository.findById(sessionId).orElse(null);
            Long resumeId = null;
            if (session != null && session.getJobApplicationId() != null) {
                // 1) 通过 jobApplicationId 获取
                jobApplicationRepository.findById(session.getJobApplicationId()).ifPresent(app -> {
                    if (app.getResumeId() != null) {
                        report.setResumeId(app.getResumeId());
                    }
                });
            }
            if (report.getResumeId() == null && session != null) {
                // 2) 若仍为空，回退到用户最新上传的简历
                if (session != null) {
                    Optional<Resume> latestResume = resumeRepository.findByUserIdOrderByUpdateTimeDesc(session.getUserId()).stream().findFirst();
                    latestResume.ifPresent(r -> report.setResumeId(r.getId()));
                }
            }
            if (report.getResumeId() == null) {
                // 3) 最后兜底 -1
                report.setResumeId(-1L);
            }

            // 维度细分来自 answerScores
            ScenarioModel.answerScores.values().forEach(s -> {
                switch (s.getDimension()) {
                    case "核心技术能力": report.setTechScore(s.getTotalScore()); break;
                    case "关键项目经验": report.setProjectScore(s.getTotalScore()); break;
                    case "团队协作能力": report.setTeamScore(s.getTotalScore()); break;
                    case "职业发展规划": report.setPlanScore(s.getTotalScore()); break;
                    case "工作态度与价值观": report.setAttitudeScore(s.getTotalScore()); break;
                }
            });

            reportRepository.save(report);
        } catch (Exception ex) {
            // 忽略报告生成失败
        }
    }

    /**
     * 读取最新简历的纯文本。
     */
    private String fetchResumeText(Long userId) {
        Optional<Resume> resumeOpt = resumeRepository.findByUserIdOrderByUpdateTimeDesc(userId).stream().findFirst();
        if (resumeOpt.isEmpty()) {
            return "";
        }
        Resume resume = resumeOpt.get();
        return Stream.of(resume.getSummary(), resume.getContent(), resume.getOriginalData())
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .findFirst()
                .orElse("");
    }

    /**
     * 固定模板生成问题，可后续接入大模型。
     */
    private String generateQuestionSimple(String dimension, String resumeText) {
        switch (dimension) {
            case "核心技术能力":
                return "我看到您简历中使用了多种技术栈，能否选择一个您最有信心的技术领域，结合具体的项目经历，说说您是如何解决过一个比较有挑战性的技术问题的？包括问题的背景、您的解决思路和最终效果。";
            case "关键项目经验":
                return "请选择您简历中最有代表性的一个项目详细介绍一下。我特别想了解这个项目的技术难点在哪里，您在其中承担了什么关键角色，以及最终为公司或团队带来了什么价值？";
            case "团队协作能力":
                return "在您的工作经历中，肯定遇到过需要跨部门协作的情况，比如和产品、测试、前端等不同角色的同事配合。能分享一个印象比较深刻的协作经历吗？当时遇到了什么挑战，您是如何处理的？";
            case "职业发展规划":
                return "基于您目前的技术背景和工作经验，您对自己未来3-5年的职业发展是怎么规划的？特别是在技术深度和广度方面，您觉得还需要重点提升哪些能力？";
            case "工作态度与价值观":
                return "每个开发者都会遇到工作压力比较大的时候，比如项目deadline很紧、需求频繁变更、或者遇到难以解决的技术问题。能分享一下您是如何应对这些挑战的吗？您觉得什么样的工作态度对技术团队来说是最重要的？";
            default:
                return "请您先简单介绍一下自己的技术背景和工作经历，重点说说您认为最有价值的项目经验。";
        }
    }

    /**
     * 调用 Spark 大模型，根据简历文本动态生成问题；失败则回退到固定模板。
     */
    private String generateQuestionAI(String dimension, String resumeText) {
        return generateQuestionAI(dimension, resumeText, "", "");
    }
    
    /**
     * 调用 Spark 大模型，根据简历文本、岗位信息和历史问答动态生成问题；失败则回退到固定模板。
     * @param dimension 评估维度
     * @param resumeText 简历文本
     * @param jobDetails 岗位详情
     * @param historicalQA 历史问答记录
     * @return 生成的面试问题
     */
    private String generateQuestionAI(String dimension, String resumeText, String jobDetails, String historicalQA) {
        // 无简历文本或过短时直接用模板
        if (resumeText == null || resumeText.length() < 20) {
            return generateQuestionSimple(dimension, resumeText);
        }
        
        // 裁剪过长的文本
        resumeText = resumeText.substring(0, Math.min(1500, resumeText.length()));
        jobDetails = jobDetails != null && jobDetails.length() > 0 ? jobDetails.substring(0, Math.min(1000, jobDetails.length())) : "";
        historicalQA = historicalQA != null && historicalQA.length() > 0 ? historicalQA.substring(0, Math.min(1000, historicalQA.length())) : "";

        try {
            // 构建更完整的上下文信息
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("=== 候选人简历摘要 ===\n").append(resumeText).append("\n\n");
            
            if (jobDetails != null && !jobDetails.isEmpty()) {
                contextBuilder.append("=== 应聘岗位信息 ===\n").append(jobDetails).append("\n\n");
            }
            
            if (historicalQA != null && !historicalQA.isEmpty()) {
                contextBuilder.append("=== 历史问答记录 ===\n").append(historicalQA).append("\n\n");
            }
            
            // 根据不同维度设计专门的提示词模板
            String dimensionGuidance = getDimensionSpecificGuidance(dimension);
            
            // 随机选择问题风格模板
            String questionStyle = getRandomQuestionStyle(dimension);
            
            String prompt = String.format(
                "你是一位温和、专业的资深面试官，拥有丰富的人才评估经验。今天你要面试一位%s岗位的候选人，你已经仔细阅读了他的简历。\n\n" +
                "===候选人档案===\n%s\n" +
                "===本轮交流重点：%s===\n%s\n\n" +
                "现在，请以轻松但专业的方式开始关于\"%s\"的对话。你想通过一个贴近实际工作的问题来了解候选人：\n\n" +
                "🎨 问题设计要求：\n" +
                "%s\n" +
                "• 贴近真实工作：基于候选人的实际经验设计场景\n" +
                "• 展现专业洞察：问题要能体现你对行业和技术的深度理解\n" +
                "• 鼓励深入分享：让候选人有机会展示%s方面的真实能力\n" +
                "• 保持对话感：70-120字，既有足够背景又不冗长\n" +
                "• 避免套路化：不要总是使用相同的开场白和问法\n\n" +
                "请直接以面试官的口吻提出问题，让对话自然开始：",
                getJobTitle(jobDetails),
                contextBuilder.toString(),
                dimension,
                dimensionGuidance,
                dimension,
                questionStyle,
                dimension
            );

            String resp = sparkClient.ask(prompt);
            
            // 只保留首行，去掉多余前后缀
            String question = resp.split("\n")[0].trim();
            
            // -------- 前缀清洗 --------
            String qLower = question.toLowerCase();
            // 去掉"问题："
            if (question.startsWith("问题：") || question.startsWith("问题:")) {
                question = question.substring(3).trim();
            }
            // 去掉"好的，以下是…面试问题："等说辞
            if (qLower.contains("面试问题") && question.contains("：")) {
                int idx = question.lastIndexOf("：");
                if (idx >= 0 && idx + 1 < question.length()) {
                    question = question.substring(idx + 1).trim();
                }
            }
            // 去掉开头的"——"或"- "之类
            question = question.replaceFirst("^[—\\-\\s]+", "").trim();
            
            // 记录生成的问题
            log.info("生成维度{}的问题: {}", dimension, question);
            
            // 若大模型返回空，再退回模板
            return question.isBlank() ? generateQuestionSimple(dimension, resumeText) : question;
        } catch (Exception ex) {
            log.warn("调用大模型生成问题失败，使用默认模板. dimension={} reason={}", dimension, ex.getMessage());
            return generateQuestionSimple(dimension, resumeText);
        }
    }

    private int parseIntSafe(String s) {
        try {
            String num = s.replaceAll("[^0-9]", "");
            return Integer.parseInt(num);
        } catch (NumberFormatException e) {
            return 0; // 或抛出异常，取决于需求
        }
    }
    
    /**
     * 根据岗位信息提取岗位标题
     */
    private String getJobTitle(String jobDetails) {
        if (jobDetails == null || jobDetails.isEmpty()) {
            return "技术";
        }
        
        // 尝试从岗位详情中提取岗位名称
        String[] lines = jobDetails.split("\n");
        for (String line : lines) {
            if (line.startsWith("岗位名称:") || line.startsWith("岗位名称：")) {
                String title = line.substring(line.indexOf(":") > 0 ? line.indexOf(":") + 1 : line.indexOf("：") + 1).trim();
                return title.isEmpty() ? "技术" : title;
            }
        }
        
        return "技术";
    }
    
    /**
     * 获取针对特定维度的专门指导内容
     */
    private String getDimensionSpecificGuidance(String dimension) {
        switch (dimension) {
            case "核心技术能力":
                return "【评估重点】技术深度、问题解决能力、技术选型、架构思维\n" +
                       "【问题类型建议】\n" +
                       "- 基于简历中的具体技术栈，设计一个实际的技术难题场景\n" +
                       "- 询问在项目中遇到的具体技术挑战及解决方案\n" +
                       "- 探讨技术选型的考虑因素和权衡\n" +
                       "- 了解对新技术的学习和应用能力\n" +
                       "【真实场景示例】\n" +
                       "\"我看到你在简历中提到使用过Redis做缓存，假如现在你负责的电商系统在双11期间出现了缓存穿透问题，导致数据库压力激增，你会如何分析和解决这个问题？\"";
                
            case "关键项目经验":
                return "【评估重点】项目管理、团队协作、技术实施、问题解决、成果产出\n" +
                       "【问题类型建议】\n" +
                       "- 深入了解简历中某个具体项目的实施过程\n" +
                       "- 询问项目中遇到的最大挑战及应对策略\n" +
                       "- 探讨项目的技术架构设计和演进过程\n" +
                       "- 了解项目的业务价值和成果\n" +
                       "【真实场景示例】\n" +
                       "\"你提到参与了一个微服务架构改造项目，能详细说说这个项目中你主要负责的部分吗？在从单体架构拆分为微服务的过程中，遇到了哪些技术挑战，你是如何解决的？\"";
                
            case "团队协作能力":
                return "【评估重点】沟通表达、冲突处理、团队合作、领导力、跨部门协作\n" +
                       "【问题类型建议】\n" +
                       "- 询问在团队中承担的角色和责任\n" +
                       "- 了解处理团队冲突的具体经验\n" +
                       "- 探讨与不同角色（产品、测试、运维）的协作方式\n" +
                       "- 考察指导新人或带领团队的经验\n" +
                       "【真实场景示例】\n" +
                       "\"假如在你参与的项目中，前端同事对后端接口设计有不同意见，认为现有的接口不够灵活，而你觉得当前设计已经能满足需求，你会如何处理这种分歧？\"";
                
            case "职业发展规划":
                return "【评估重点】职业目标、学习能力、行业认知、自我驱动力、发展潜力\n" +
                       "【问题类型建议】\n" +
                       "- 了解对所应聘岗位和公司的认知\n" +
                       "- 探讨短期和长期的职业发展目标\n" +
                       "- 询问持续学习和技能提升的计划\n" +
                       "- 了解对行业趋势和技术发展的看法\n" +
                       "【真实场景示例】\n" +
                       "\"考虑到目前AI技术快速发展，你觉得这对传统的后端开发工作会带来什么影响？你是如何规划自己在这个变化中的发展方向的？\"";
                
            case "工作态度与价值观":
                return "【评估重点】工作责任心、抗压能力、学习态度、团队精神、职业操守\n" +
                       "【问题类型建议】\n" +
                       "- 了解面对工作压力和困难时的态度\n" +
                       "- 探讨对代码质量和技术规范的重视程度\n" +
                       "- 询问处理工作与生活平衡的方式\n" +
                       "- 了解对团队文化和公司价值观的认同\n" +
                       "【真实场景示例】\n" +
                       "\"假如在项目上线前夕发现了一个可能影响用户体验的bug，但修复这个bug可能需要延期发布，而业务方压力很大希望按时上线，你会如何处理这种情况？\"";
                
            default:
                return "综合评估候选人的专业能力和素质表现";
        }
    }
}