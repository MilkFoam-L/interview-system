package interview.service.impl;

import interview.model.dto.AnswerRecordDTO;
import interview.model.dto.CodeJudgeRequest;
import interview.model.dto.CodeJudgeResult;
import interview.model.entity.AnswerRecord;
import interview.model.entity.Question;
import interview.repository.AnswerRecordRepository;
import interview.repository.QuestionRepository;
import interview.service.AnswerRecordService;
import interview.service.CodeJudgeService;
import interview.service.CodeExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import interview.model.User;
import interview.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
@Service
public class AnswerRecordServiceImpl implements AnswerRecordService {
    @Autowired
    private AnswerRecordRepository answerRecordRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private CodeJudgeService codeJudgeService;
    
    @Autowired
    private CodeExecutionService codeExecutionService;
    
    @Autowired
    private UserRepository userRepository;
    
    private static final Logger logger = LoggerFactory.getLogger(AnswerRecordServiceImpl.class);
    // @Autowired(required = false)
    // private AutoTestCaseGeneratorService autoTestCaseGenerator; // 已删除，AI判题系统不需要

    @Override
    @Transactional
    public AnswerRecord saveOrUpdateAnswer(AnswerRecordDTO dto) {
        // 1. 首先确保我们有必要的参数
        if (dto.getUserId() == null || dto.getQuestionId() == null) {
            throw new IllegalArgumentException("userId 和 questionId 不能为空");
        }

        // 用户ID验证已在Controller层处理，这里直接使用

        // 这段逻辑已经移动到AnswerRecordController中处理

        // 2. 查找匹配的记录
        AnswerRecord answerRecord = null;
        
        if (dto.getInterviewId() != null) {
            // 使用精确查询方法找到匹配的记录
            List<AnswerRecord> records = answerRecordRepository.findByUserIdAndQuestionIdAndInterviewId(
                dto.getUserId(), dto.getQuestionId(), dto.getInterviewId());
            answerRecord = records.isEmpty() ? null : records.get(0);
        } else {
            // 如果没有interviewId，回退到传统查询
            List<AnswerRecord> records = answerRecordRepository.findByUserIdAndQuestionId(
                dto.getUserId(), dto.getQuestionId());
            answerRecord = records.isEmpty() ? null : records.get(0);
        }
        
        // 3. 如果没找到，创建新记录，并确保正确设置所有关联字段
        LocalDateTime now = LocalDateTime.now();
        boolean isFirstAnswer = false;
        
        if (answerRecord == null) {
            answerRecord = new AnswerRecord();
            answerRecord.setUserId(dto.getUserId());
            answerRecord.setQuestionId(dto.getQuestionId());
            answerRecord.setInterviewId(dto.getInterviewId()); // 可能为null
            answerRecord.setCreateTime(now);
            isFirstAnswer = true; // 标记这是首次回答
        }
        answerRecord.setUserAnswer(dto.getUserAnswer() != null ? dto.getUserAnswer() : "");
        answerRecord.setType(dto.getType());
        
        // 确保开始时间不为null
        if (dto.getStartTime() != null) {
            answerRecord.setStartTime(dto.getStartTime());
        } else if (answerRecord.getStartTime() == null) {
            answerRecord.setStartTime(now);
        }
        
        // 确保提交时间不为null
        if (dto.getSubmitTime() != null) {
            answerRecord.setSubmitTime(dto.getSubmitTime());
        } else {
            // 如果提交时间为null，使用当前时间
            answerRecord.setSubmitTime(now);
        }
        
        // 确保用时不为null
        answerRecord.setTimeUsed(dto.getTimeUsed() != null ? dto.getTimeUsed() : 0);
        answerRecord.setUpdateTime(now);

        Question question = questionRepository.findById(dto.getQuestionId()).orElse(null);
        if (question != null) {
            String inputType = question.getInputType();
            
            if ("radio".equals(inputType)) {
                // 单选题评分
            boolean correct = dto.getUserAnswer() != null && dto.getUserAnswer().equals(question.getCorrectAnswer());
            answerRecord.setIsCorrect(correct ? 1 : 0);
            answerRecord.setScore(correct ? BigDecimal.ONE : BigDecimal.ZERO);
            } else if ("code".equals(inputType)) {
            // 代码题先快速保存，然后异步判题
            handleCodeQuestionAsync(answerRecord, question, dto);
            } else if ("textarea".equals(inputType) || "text".equals(inputType)) {
                // 主观题评分 - 基于答案完整性和长度
                handleSubjectiveQuestion(answerRecord, question, dto);
            } else if ("checkbox".equals(inputType)) {
                // 多选题评分
                handleCheckboxQuestion(answerRecord, question, dto);
            } else {
                // 其他题型默认处理
                handleDefaultQuestion(answerRecord, question, dto);
            }
        }
        
        // 4. 如果是首次回答，更新题目使用次数
        if (isFirstAnswer && question != null) {
            question.setUsageCount(question.getUsageCount() == null ? 1 : question.getUsageCount() + 1);
            question.setUpdateTime(now);
            questionRepository.save(question);
        }
        
        return answerRecordRepository.save(answerRecord);
    }

    @Override
    public List<AnswerRecord> getAnswerRecordsByUserId(Long userId) {
        return answerRecordRepository.findByUserId(userId);
    }

    @Override
    public List<AnswerRecord> getAnswerRecordsByQuestionId(Long questionId) {
        return answerRecordRepository.findByQuestionId(questionId);
    }

    @Override
    public List<AnswerRecord> getAnswerRecordsByInterviewId(Long interviewId) {
        return answerRecordRepository.findByInterviewId(interviewId);
    }

    @Override
    public AnswerRecord getAnswerRecordById(Long id) {
        return answerRecordRepository.findById(id).orElse(null);
    }

    @Override
    public AnswerRecord getByUserIdAndQuestionId(Long userId, Long questionId) {
        List<AnswerRecord> records = answerRecordRepository.findByUserIdAndQuestionId(userId, questionId);
        return records.isEmpty() ? null : records.get(0);
    }
    
    @Override
    public AnswerRecord getByUserIdAndQuestionIdAndInterviewId(Long userId, Long questionId, Long interviewId) {
        List<AnswerRecord> records = answerRecordRepository.findByUserIdAndQuestionIdAndInterviewId(userId, questionId, interviewId);
        return records.isEmpty() ? null : records.get(0);
    }
    
    @Override
    public List<AnswerRecord> getAnswerRecordsByUserIdAndInterviewId(Long userId, Long interviewId) {
        return answerRecordRepository.findByUserIdAndInterviewId(userId, interviewId);
    }

    @Override
    public void deleteAnswerRecord(Long id) {
        answerRecordRepository.deleteById(id);
    }
    
    /**
     * 处理主观题评分
     */
    private void handleSubjectiveQuestion(AnswerRecord answerRecord, Question question, AnswerRecordDTO dto) {
        String userAnswer = dto.getUserAnswer();
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            answerRecord.setIsCorrect(0);
            answerRecord.setScore(BigDecimal.ZERO);
            answerRecord.setAnalysis("未提交答案");
            return;
        }
        
        // 基于答案长度和完整性进行简单评分
        String trimmedAnswer = userAnswer.trim();
        int answerLength = trimmedAnswer.length();
        
        BigDecimal score;
        if (answerLength >= 200) {
            score = new BigDecimal("0.9"); // 90分
        } else if (answerLength >= 100) {
            score = new BigDecimal("0.8"); // 80分
        } else if (answerLength >= 50) {
            score = new BigDecimal("0.7"); // 70分
        } else if (answerLength >= 20) {
            score = new BigDecimal("0.6"); // 60分
        } else {
            score = new BigDecimal("0.4"); // 40分
        }
        
        answerRecord.setIsCorrect(score.compareTo(new BigDecimal("0.6")) >= 0 ? 1 : 0);
        answerRecord.setScore(score);
        answerRecord.setAnalysis("基于答案完整性评分，答案长度: " + answerLength + " 字符");
    }
    
    /**
     * 处理多选题评分
     */
    private void handleCheckboxQuestion(AnswerRecord answerRecord, Question question, AnswerRecordDTO dto) {
        String userAnswer = dto.getUserAnswer();
        String correctAnswer = question.getCorrectAnswer();
        
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            answerRecord.setIsCorrect(0);
            answerRecord.setScore(BigDecimal.ZERO);
            answerRecord.setAnalysis("未选择答案");
            return;
        }
        
        if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
            // 如果没有标准答案，给予部分分数
            answerRecord.setIsCorrect(1);
            answerRecord.setScore(new BigDecimal("0.7"));
            answerRecord.setAnalysis("多选题已回答");
            return;
        }
        
        // 比较多选答案
        String[] userChoices = userAnswer.split(",");
        String[] correctChoices = correctAnswer.split(",");
        
        Set<String> userSet = Arrays.stream(userChoices)
                .map(String::trim)
                .collect(Collectors.toSet());
        Set<String> correctSet = Arrays.stream(correctChoices)
                .map(String::trim)
                .collect(Collectors.toSet());
        
        // 计算交集和并集
        Set<String> intersection = new HashSet<>(userSet);
        intersection.retainAll(correctSet);
        
        Set<String> union = new HashSet<>(userSet);
        union.addAll(correctSet);
        
        // 使用Jaccard相似度计算得分
        double similarity = union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        BigDecimal score = new BigDecimal(similarity).setScale(2, RoundingMode.HALF_UP);
        
        answerRecord.setIsCorrect(similarity >= 0.8 ? 1 : 0);
        answerRecord.setScore(score);
        answerRecord.setAnalysis(String.format("多选题得分: %.2f, 正确率: %.1f%%", 
                score.doubleValue(), similarity * 100));
    }
    
    /**
     * 处理其他题型默认评分
     */
    private void handleDefaultQuestion(AnswerRecord answerRecord, Question question, AnswerRecordDTO dto) {
        String userAnswer = dto.getUserAnswer();
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            answerRecord.setIsCorrect(0);
            answerRecord.setScore(BigDecimal.ZERO);
            answerRecord.setAnalysis("未提交答案");
            return;
        }
        
        // 默认给予中等分数，表示已回答
        answerRecord.setIsCorrect(1);
        answerRecord.setScore(new BigDecimal("0.75"));
        answerRecord.setAnalysis("题目已回答");
    }
    
    /**
     * 异步处理代码题判题 - 立即返回，代码判题在后台执行
     */
    private void handleCodeQuestionAsync(AnswerRecord answerRecord, Question question, AnswerRecordDTO dto) {
        String userCode = dto.getUserAnswer();
        
        // 先给代码题设置一个默认评分，避免前端等待
        if (userCode == null || userCode.trim().isEmpty()) {
            answerRecord.setIsCorrect(0);
            answerRecord.setScore(BigDecimal.ZERO);
            answerRecord.setAnalysis("未提交代码");
        } else {
            // 有代码提交，先设置一个中性评分
            answerRecord.setIsCorrect(1); // 暂时设为正确
            answerRecord.setScore(new BigDecimal("0.5")); // 暂时设为50分
            answerRecord.setAnalysis("代码已提交，正在后台分析中...");
        }
        
        // 如果有代码，启动异步判题
        if (userCode != null && !userCode.trim().isEmpty()) {
            // 异步执行实际的代码判题，不阻塞主流程
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("开始异步代码判题，answerRecord ID: {}", answerRecord.getId());
                    // 执行实际的代码判题
                    executeCodeJudgmentInBackground(answerRecord, question, dto);
                    log.info("异步代码判题完成，answerRecord ID: {}", answerRecord.getId());
                } catch (Exception e) {
                    log.error("异步代码判题失败，answerRecord ID: {}", answerRecord.getId(), e);
                }
            });
        }
    }
    
    /**
     * 在后台执行实际的代码判题逻辑
     */
    private void executeCodeJudgmentInBackground(AnswerRecord answerRecord, Question question, AnswerRecordDTO dto) {
        String userCode = dto.getUserAnswer();
        
        try {
            // 检测语言类型
            String detectedLanguage = detectLanguage(userCode);
            
            // 对于非可执行代码题目，使用专门的匹配评分逻辑
            if (isNonExecutableLanguage(detectedLanguage)) {
                handleNonExecutableCodeQuestion(answerRecord, question, userCode, detectedLanguage);
            } else {
                // 提取测试用例，如果没有则智能生成
                String testCasesJson = codeJudgeService.extractTestCases(question);
                if (testCasesJson == null || testCasesJson.trim().isEmpty() || "[]".equals(testCasesJson.trim())) {
                    log.info("题目 {} 没有预定义测试用例，将使用AI判题的快速模式", question.getId());
                    testCasesJson = "[]"; // AI判题系统会自动处理这种情况
                }
                
                // 检测编程语言（简单的启发式检测）
                String language = detectLanguage(userCode);
                
                // 构建判题请求
                CodeJudgeRequest judgeRequest = new CodeJudgeRequest();
                judgeRequest.setCode(userCode);
                judgeRequest.setLanguage(language);
                judgeRequest.setTestCases(testCasesJson);
                judgeRequest.setTimeLimit(30); // 增加到30秒适应AI判题
                judgeRequest.setMemoryLimit(128); // 128MB内存限制
                
                // 执行判题
                log.info("开始执行AI判题（后台），题目ID: {}, 代码长度: {}", question.getId(), userCode.length());
                CodeJudgeResult judgeResult = codeJudgeService.judge(judgeRequest);
                log.info("AI判题完成（后台），状态: {}, 得分: {}", judgeResult.getStatus(), judgeResult.getScore());
                
                // 专业化评分策略
                CodeEvaluationResult evaluation = calculateProfessionalScore(judgeResult);
                answerRecord.setIsCorrect(evaluation.isCorrect);
                answerRecord.setScore(evaluation.score);
                answerRecord.setAnalysis(evaluation.analysis);
                
                // 保存代码执行记录到 code_execution 表
                saveCodeExecutionRecord(answerRecord, userCode, language, judgeResult, dto);
            }
            
            // 更新数据库中的答案记录
            answerRecord.setUpdateTime(LocalDateTime.now());
            answerRecordRepository.save(answerRecord);
            log.info("代码判题结果已保存到数据库，answerRecord ID: {}", answerRecord.getId());
            
        } catch (Exception e) {
            // 智能异常处理 - 基于异常类型给予不同评分
            handleJudgeException(answerRecord, e, dto.getUserAnswer());
            
            // 即使判题异常，也要保存执行记录
            saveCodeExecutionRecordOnError(answerRecord, userCode, detectLanguage(userCode), 
                e.getMessage(), dto);
            
            // 更新数据库
            answerRecord.setUpdateTime(LocalDateTime.now());
            answerRecordRepository.save(answerRecord);
            log.error("代码判题异常，已保存错误信息到数据库，answerRecord ID: {}", answerRecord.getId(), e);
        }
    }
    
    /**
     * 处理代码题判题（原同步方法，保留作为备用）
     */
    private void handleCodeQuestion(AnswerRecord answerRecord, Question question, AnswerRecordDTO dto) {
        // 获取用户提交的代码 - 移到try块外确保catch块可访问
            String userCode = dto.getUserAnswer();
        
        try {
            if (userCode == null || userCode.trim().isEmpty()) {
                // 没有提交代码，设置为0分
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(BigDecimal.ZERO);
                answerRecord.setAnalysis("未提交代码");
                return;
            }
            
            // 检测语言类型
            String detectedLanguage = detectLanguage(userCode);
            
            // 对于非可执行代码题目，使用专门的匹配评分逻辑
            if (isNonExecutableLanguage(detectedLanguage)) {
                handleNonExecutableCodeQuestion(answerRecord, question, userCode, detectedLanguage);
                return;
            }
            
            // 提取测试用例，如果没有则智能生成
            String testCasesJson = codeJudgeService.extractTestCases(question);
            if (testCasesJson == null || testCasesJson.trim().isEmpty() || "[]".equals(testCasesJson.trim())) {
                System.out.println("题目 " + question.getId() + " 没有预定义测试用例，将使用AI判题的快速模式");
                testCasesJson = "[]"; // AI判题系统会自动处理这种情况
            }
            
            // 检测编程语言（简单的启发式检测）
            String language = detectLanguage(userCode);
            
            // 构建判题请求
            CodeJudgeRequest judgeRequest = new CodeJudgeRequest();
            judgeRequest.setCode(userCode);
            judgeRequest.setLanguage(language);
            judgeRequest.setTestCases(testCasesJson);
            judgeRequest.setTimeLimit(30); // 增加到30秒适应AI判题
            judgeRequest.setMemoryLimit(128); // 128MB内存限制
            
            // 执行判题
            System.out.println("开始执行AI判题，题目ID: " + question.getId() + ", 代码长度: " + userCode.length());
            CodeJudgeResult judgeResult = codeJudgeService.judge(judgeRequest);
            System.out.println("AI判题完成，状态: " + judgeResult.getStatus() + ", 得分: " + judgeResult.getScore());
            
            // 专业化评分策略
            CodeEvaluationResult evaluation = calculateProfessionalScore(judgeResult);
            answerRecord.setIsCorrect(evaluation.isCorrect);
            answerRecord.setScore(evaluation.score);
            answerRecord.setAnalysis(evaluation.analysis);
            
            // 保存代码执行记录到 code_execution 表
            saveCodeExecutionRecord(answerRecord, userCode, language, judgeResult, dto);
            
        } catch (Exception e) {
            // 智能异常处理 - 基于异常类型给予不同评分
            handleJudgeException(answerRecord, e, dto.getUserAnswer());
            
            // 即使判题异常，也要保存执行记录
            saveCodeExecutionRecordOnError(answerRecord, userCode, detectLanguage(userCode), 
                e.getMessage(), dto);
        }
    }
    
    /**
     * 保存代码执行记录到 code_execution 表
     * 轻量级集成，不影响现有逻辑
     */
    private void saveCodeExecutionRecord(AnswerRecord answerRecord, String code, String language, 
                                       CodeJudgeResult judgeResult, AnswerRecordDTO dto) {
        try {
            // 获取必要的 ID 信息
            Long answerId = answerRecord.getId();
            Long sessionId = dto.getInterviewId(); // 使用 interviewId 作为 sessionId
            
            // 如果还没有保存 answerRecord（首次创建），先保存以获取ID
            if (answerId == null) {
                AnswerRecord savedRecord = answerRecordRepository.save(answerRecord);
                answerId = savedRecord.getId();
            }
            
            // 如果没有 sessionId，尝试从其他地方获取或使用默认值
            if (sessionId == null) {
                sessionId = 0L; // 使用默认值，表示非会话环境下的代码执行
            }
            
            // 创建代码执行记录
            codeExecutionService.createExecutionRecord(answerId, sessionId, code, language, judgeResult);
            
            log.info("成功保存代码执行记录: answerId={}, sessionId={}, language={}", 
                answerId, sessionId, language);
            
        } catch (Exception e) {
            // 执行记录保存失败不应影响主流程
            log.warn("保存代码执行记录失败，但不影响答题记录: {}", e.getMessage());
        }
    }
    
    /**
     * 在判题异常时保存错误执行记录
     */
    private void saveCodeExecutionRecordOnError(AnswerRecord answerRecord, String code, String language, 
                                              String errorMessage, AnswerRecordDTO dto) {
        try {
            Long answerId = answerRecord.getId();
            Long sessionId = dto.getInterviewId();
            
            if (answerId == null) {
                AnswerRecord savedRecord = answerRecordRepository.save(answerRecord);
                answerId = savedRecord.getId();
            }
            
            if (sessionId == null) {
                sessionId = 0L;
            }
            
            // 创建一个表示错误状态的 CodeJudgeResult
            CodeJudgeResult errorResult = new CodeJudgeResult();
            errorResult.setStatus("SE"); // System Error
            errorResult.setScore(0);
            errorResult.setPassedCount(0);
            errorResult.setTotalCount(0);
            errorResult.setErrorMessage(errorMessage);
            
            codeExecutionService.createExecutionRecord(answerId, sessionId, code, language, errorResult);
            
            log.info("保存错误代码执行记录: answerId={}, error={}", answerId, errorMessage);
            
        } catch (Exception e) {
            log.warn("保存错误执行记录失败: {}", e.getMessage());
        }
    }
    
    /**
     * 处理非可执行代码题目（CSS、SQL等）
     */
    private void handleNonExecutableCodeQuestion(AnswerRecord answerRecord, Question question, String userCode, String language) {
        try {
            String correctAnswer = question.getCorrectAnswer();
            
            if (correctAnswer == null || correctAnswer.trim().isEmpty()) {
                // 没有标准答案，给予基础分数
                answerRecord.setIsCorrect(1);
                answerRecord.setScore(BigDecimal.valueOf(0.8)); // 给80分
                answerRecord.setAnalysis("已提交" + language.toUpperCase() + "代码，由于缺少标准答案，给予基础分数。");
                return;
            }
            
            // 计算相似度得分
            double similarity = calculateCodeSimilarity(userCode, correctAnswer, language);
            
            // 根据相似度设置分数
            BigDecimal score;
            int isCorrect;
            String analysis;
            
            if (similarity >= 0.9) {
                score = BigDecimal.ONE; // 100分
                isCorrect = 1;
                analysis = language.toUpperCase() + "代码非常优秀！与标准答案高度匹配。";
            } else if (similarity >= 0.7) {
                score = BigDecimal.valueOf(0.85); // 85分
                isCorrect = 1;
                analysis = language.toUpperCase() + "代码良好，与标准答案匹配度较高。";
            } else if (similarity >= 0.5) {
                score = BigDecimal.valueOf(0.7); // 70分
                isCorrect = 1;
                analysis = language.toUpperCase() + "代码基本正确，有改进空间。";
            } else if (similarity >= 0.3) {
                score = BigDecimal.valueOf(0.5); // 50分
                isCorrect = 0;
                analysis = language.toUpperCase() + "代码部分正确，但与标准答案差异较大。";
            } else {
                score = BigDecimal.valueOf(0.3); // 30分
                isCorrect = 0;
                analysis = language.toUpperCase() + "代码与标准答案差异很大，建议重新审视题目要求。";
            }
            
            answerRecord.setScore(score);
            answerRecord.setIsCorrect(isCorrect);
            answerRecord.setAnalysis(analysis);
            
        } catch (Exception e) {
            // 处理异常情况
            answerRecord.setIsCorrect(1);
            answerRecord.setScore(BigDecimal.valueOf(0.6)); // 给60分基础分
            answerRecord.setAnalysis("已提交" + language.toUpperCase() + "代码，系统评分时出现异常，给予基础分数。");
            System.err.println("处理" + language + "代码时出现异常: " + e.getMessage());
        }
    }
    
    /**
     * 计算代码相似度（针对CSS和SQL）
     */
    private double calculateCodeSimilarity(String userCode, String correctAnswer, String language) {
        if (userCode == null || correctAnswer == null) {
            return 0.0;
        }
        
        // SQL特殊处理：语义等价性检查
        if ("sql".equals(language)) {
            return calculateSQLSimilarity(userCode, correctAnswer);
        }
        
        // 标准化代码（去除空白字符、统一大小写等）
        String normalizedUser = normalizeCode(userCode, language);
        String normalizedCorrect = normalizeCode(correctAnswer, language);
        
        // 如果完全匹配，返回1.0
        if (normalizedUser.equals(normalizedCorrect)) {
            return 1.0;
        }
        
        // 计算关键词匹配度
        double keywordSimilarity = calculateKeywordSimilarity(normalizedUser, normalizedCorrect, language);
        
        // 计算结构相似度
        double structureSimilarity = calculateStructureSimilarity(normalizedUser, normalizedCorrect);
        
        // 综合评分：关键词占70%，结构占30%
        return keywordSimilarity * 0.7 + structureSimilarity * 0.3;
    }
    
    /**
     * 专门用于SQL的相似度计算，考虑SQL语法的语义等价性
     */
    private double calculateSQLSimilarity(String userCode, String correctAnswer) {
        // 标准化SQL代码
        String normalizedUser = normalizeCode(userCode, "sql");
        String normalizedCorrect = normalizeCode(correctAnswer, "sql");
        
        // 完全匹配检查
        if (normalizedUser.equals(normalizedCorrect)) {
            return 1.0;
        }
        
        // 解析SQL结构
        SQLStructure userStructure = parseSQLStructure(normalizedUser);
        SQLStructure correctStructure = parseSQLStructure(normalizedCorrect);
        
        // 计算结构匹配度
        double structureScore = calculateSQLStructureScore(userStructure, correctStructure);
        
        // 计算关键词匹配度
        double keywordScore = calculateSQLKeywordSimilarity(normalizedUser, normalizedCorrect);
        
        // 计算字面相似度（作为补充）
        double literalScore = calculateStructureSimilarity(normalizedUser, normalizedCorrect);
        
        // 综合评分：结构占50%，关键词占35%，字面相似度占15%
        return structureScore * 0.5 + keywordScore * 0.35 + literalScore * 0.15;
    }
    
    /**
     * SQL结构信息类
     */
    private static class SQLStructure {
        Set<String> selectColumns = new HashSet<>();
        Set<String> fromTables = new HashSet<>();
        Set<String> whereConditions = new HashSet<>();
        Set<String> groupByColumns = new HashSet<>();
        Set<String> orderByColumns = new HashSet<>();
        String sqlType = "";
        
        boolean isEmpty() {
            return selectColumns.isEmpty() && fromTables.isEmpty() && 
                   whereConditions.isEmpty() && sqlType.isEmpty();
        }
    }
    
    /**
     * 解析SQL结构
     */
    private SQLStructure parseSQLStructure(String sql) {
        SQLStructure structure = new SQLStructure();
        if (sql == null || sql.trim().isEmpty()) {
            return structure;
        }
        
        String lowerSql = sql.toLowerCase().trim();
        
        // 确定SQL类型
        if (lowerSql.startsWith("select")) {
            structure.sqlType = "select";
            
            // 解析SELECT字段
            int selectStart = lowerSql.indexOf("select") + 6;
            int fromIndex = lowerSql.indexOf("from");
            if (fromIndex > selectStart) {
                String selectPart = lowerSql.substring(selectStart, fromIndex).trim();
                String[] columns = selectPart.split(",");
                for (String col : columns) {
                    structure.selectColumns.add(col.trim().replaceAll("\\s+", " "));
                }
            }
            
            // 解析FROM表
            if (fromIndex >= 0) {
                int whereIndex = lowerSql.indexOf("where", fromIndex);
                int groupIndex = lowerSql.indexOf("group by", fromIndex);
                int orderIndex = lowerSql.indexOf("order by", fromIndex);
                
                int endIndex = Math.min(
                    whereIndex >= 0 ? whereIndex : lowerSql.length(),
                    Math.min(
                        groupIndex >= 0 ? groupIndex : lowerSql.length(),
                        orderIndex >= 0 ? orderIndex : lowerSql.length()
                    )
                );
                
                String fromPart = lowerSql.substring(fromIndex + 4, endIndex).trim();
                String[] tables = fromPart.split(",");
                for (String table : tables) {
                    structure.fromTables.add(table.trim().replaceAll("\\s+", " "));
                }
            }
            
            // 解析WHERE条件
            int whereIndex = lowerSql.indexOf("where");
            if (whereIndex >= 0) {
                int groupIndex = lowerSql.indexOf("group by", whereIndex);
                int orderIndex = lowerSql.indexOf("order by", whereIndex);
                
                int endIndex = Math.min(
                    groupIndex >= 0 ? groupIndex : lowerSql.length(),
                    orderIndex >= 0 ? orderIndex : lowerSql.length()
                );
                
                String wherePart = lowerSql.substring(whereIndex + 5, endIndex).trim();
                // 简化WHERE条件解析，按逻辑操作符分割
                String[] conditions = wherePart.split("\\s+(and|or)\\s+");
                for (String condition : conditions) {
                    structure.whereConditions.add(condition.trim().replaceAll("\\s+", " "));
                }
            }
        }
        
        return structure;
    }
    
    /**
     * 计算SQL结构匹配得分
     */
    private double calculateSQLStructureScore(SQLStructure user, SQLStructure correct) {
        if (correct.isEmpty()) {
            return user.isEmpty() ? 1.0 : 0.5;
        }
        
        if (user.isEmpty()) {
            return 0.0;
        }
        
        // SQL类型必须匹配
        if (!user.sqlType.equals(correct.sqlType)) {
            return 0.1;
        }
        
        double totalScore = 0.0;
        int components = 0;
        
        // SELECT字段匹配度
        if (!correct.selectColumns.isEmpty()) {
            components++;
            totalScore += calculateSetSimilarity(user.selectColumns, correct.selectColumns);
        }
        
        // FROM表匹配度
        if (!correct.fromTables.isEmpty()) {
            components++;
            totalScore += calculateSetSimilarity(user.fromTables, correct.fromTables);
        }
        
        // WHERE条件匹配度
        if (!correct.whereConditions.isEmpty()) {
            components++;
            totalScore += calculateSetSimilarity(user.whereConditions, correct.whereConditions);
        }
        
        return components > 0 ? totalScore / components : 0.5;
    }
    
    /**
     * 计算两个集合的相似度
     */
    private double calculateSetSimilarity(Set<String> set1, Set<String> set2) {
        if (set2.isEmpty()) {
            return set1.isEmpty() ? 1.0 : 0.0;
        }
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 1.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * 标准化代码
     */
    private String normalizeCode(String code, String language) {
        if (code == null) return "";
        
        String normalized = code.toLowerCase()
            .replaceAll("\\s+", " ")  // 多个空白字符替换为单个空格
            .replaceAll(";\\s*", ";") // 分号后的空格
            .trim();
            
        if ("css".equals(language)) {
            // CSS特殊处理：统一花括号格式
            normalized = normalized.replaceAll("\\{\\s*", "{").replaceAll("\\s*\\}", "}");
        } else if ("sql".equals(language)) {
            // SQL特殊处理：统一关键词格式，确保一致性
            normalized = normalized.replaceAll("\\s*;\\s*$", ";") // 统一分号格式
                .replaceAll("\\s*(,)\\s*", "$1 ") // 统一逗号后空格
                .replaceAll("\\s*(>|<|=|>=|<=|!=|<>)\\s*", " $1 ") // 统一操作符格式
                .replaceAll("\\s+", " ") // 多个空格合并为单个
                .trim();
        }
        
        return normalized;
    }
    
    /**
     * 计算关键词相似度
     */
    private double calculateKeywordSimilarity(String userCode, String correctAnswer, String language) {
        if ("sql".equals(language)) {
            return calculateSQLKeywordSimilarity(userCode, correctAnswer);
        }
        
        String[] userWords = userCode.split("\\W+");
        String[] correctWords = correctAnswer.split("\\W+");
        
        int matchCount = 0;
        int totalWords = correctWords.length;
        
        for (String correctWord : correctWords) {
            if (correctWord.length() > 2) { // 只考虑长度大于2的词
                for (String userWord : userWords) {
                    if (userWord.equals(correctWord)) {
                        matchCount++;
                        break;
                    }
                }
            }
        }
        
        return totalWords > 0 ? (double) matchCount / totalWords : 0.0;
    }
    
    /**
     * 专门用于SQL的关键词相似度计算
     */
    private double calculateSQLKeywordSimilarity(String userCode, String correctAnswer) {
        // SQL关键词权重设定
        Set<String> sqlKeywords = Set.of("select", "from", "where", "group", "by", "order", 
                                       "having", "join", "inner", "left", "right", "union", 
                                       "insert", "update", "delete", "create", "alter", "drop");
        
        String[] userWords = userCode.split("\\W+");
        String[] correctWords = correctAnswer.split("\\W+");
        
        Set<String> userWordSet = Arrays.stream(userWords)
            .filter(word -> word.length() > 0)
            .collect(Collectors.toSet());
        Set<String> correctWordSet = Arrays.stream(correctWords)
            .filter(word -> word.length() > 0)
            .collect(Collectors.toSet());
        
        // 计算关键词匹配度（权重更高）
        int keywordMatches = 0;
        int totalKeywords = 0;
        
        for (String word : correctWordSet) {
            if (sqlKeywords.contains(word.toLowerCase())) {
                totalKeywords++;
                if (userWordSet.contains(word)) {
                    keywordMatches++;
                }
            }
        }
        
        // 计算普通单词匹配度
        int wordMatches = 0;
        int totalWords = 0;
        
        for (String word : correctWordSet) {
            if (!sqlKeywords.contains(word.toLowerCase()) && word.length() > 1) {
                totalWords++;
                if (userWordSet.contains(word)) {
                    wordMatches++;
                }
            }
        }
        
        // 综合评分：SQL关键词占60%，普通单词占40%
        double keywordScore = totalKeywords > 0 ? (double) keywordMatches / totalKeywords : 1.0;
        double wordScore = totalWords > 0 ? (double) wordMatches / totalWords : 1.0;
        
        return keywordScore * 0.6 + wordScore * 0.4;
    }
    
    /**
     * 计算结构相似度
     */
    private double calculateStructureSimilarity(String userCode, String correctAnswer) {
        // 简单的字符串相似度计算（基于编辑距离）
        int maxLength = Math.max(userCode.length(), correctAnswer.length());
        if (maxLength == 0) return 1.0;
        
        int editDistance = calculateEditDistance(userCode, correctAnswer);
        return 1.0 - (double) editDistance / maxLength;
    }
    
    /**
     * 计算编辑距离（Levenshtein距离）
     */
    private int calculateEditDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]);
                }
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * 智能编程语言检测 - 性能优化版
     */
    private String detectLanguage(String code) {
        if (code == null || code.trim().isEmpty()) {
            return "text";
        }
        
        // 预处理代码
        String lowerCode = code.toLowerCase();
        String trimmedCode = code.trim();
        
        // 使用优化的快速检测策略
        return detectLanguageFast(code, lowerCode, trimmedCode);
    }
    
    /**
     * 修复的语言检测算法 - 优化检测优先级，避免误识别
     */
    private String detectLanguageFast(String code, String lowerCode, String trimmedCode) {
        // 1. 强特征语言优先检测（避免误识别）
        
        // Java - 强类型特征（最高优先级）- 增强版
        if (isJavaCode(code, lowerCode)) {
            return "java";
        }
        
        // JavaScript - 现代语法特征 - 增强版
        if (isJavaScriptCode(code, lowerCode)) {
            return "javascript";
        }
        
        // Python - 增强检测，包括数据科学库 - 增强版
        if (isPythonCode(code, lowerCode)) {
            return "python";
        }
        
        // C/C++ - 预处理器特征
        if (code.contains("#include")) {
            return code.contains("iostream") || code.contains("cout") ? "cpp" : "c";
        }
        
        // 2. 数据库和样式语言
        if (detectSQL(lowerCode)) {
            return "sql";
        }
        
        if (lowerCode.contains("color:") || lowerCode.contains("font-size:") ||
            code.contains("display:") || code.contains("flex")) {
            return "css";
        }
        
        // 3. Web标记语言
        if (code.contains("<!DOCTYPE") || lowerCode.contains("<html")) {
            return "html";
        }
        
        // 4. 其他编程语言
        if (code.contains("<?php")) return "php";
        if (code.contains("package main") || code.contains("func main()")) return "go";
        if (code.contains("fn main()") || code.contains("use std::")) return "rust";
        if (code.contains("using System") || code.contains("namespace ")) return "csharp";
        
        // 5. 配置文件检测（降低优先级，避免误识别普通代码）
        if (detectConfigurationFiles(code, lowerCode)) {
            return getConfigurationLanguage(code, lowerCode);
        }
        
        // 6. Shell脚本检测（最后检测，避免误识别）
        if (detectShell(code, lowerCode)) {
            return "shell";
        }
        
        // 基于语法结构的最后判断
        if (code.matches(".*\\w+\\s*\\[\\]\\s+\\w+.*")) {
            return "java";
        }
        
        return "text"; // 默认文本类型
    }
    
    /**
     * 检测配置文件类型
     */
    private boolean detectConfigurationFiles(String code, String lowerCode) {
        // Docker相关
        if (code.startsWith("FROM ") || lowerCode.contains("dockerfile") ||
            code.contains("RUN ") || code.contains("COPY ") || code.contains("WORKDIR ")) {
            return true;
        }
        
        // Kubernetes YAML
        if (code.contains("apiVersion:") || code.contains("kind:") || 
            code.contains("metadata:") || code.contains("spec:")) {
            return true;
        }
        
        // 通用YAML
        if (code.matches(".*\\w+:\\s*.*") && code.contains("  ") && !code.contains("{")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取配置文件语言类型
     */
    private String getConfigurationLanguage(String code, String lowerCode) {
        if (code.startsWith("FROM ") || code.contains("RUN ") || code.contains("COPY ")) {
            return "dockerfile";
        }
        
        if (code.contains("apiVersion:") || code.contains("kind:")) {
            return "kubernetes";
        }
        
        return "yaml";
    }
    
    /**
     * 增强的Java代码检测 - 更精确的特征识别
     */
    private boolean isJavaCode(String code, String lowerCode) {
        // 1. 强特征检测（必须有其中一个）
        if (code.contains("public class") || code.contains("public static void main") ||
            code.contains("@Test") || code.contains("import org.junit") ||
            code.contains("package ") && code.contains(";")) {
            return true;
        }
        
        // 2. Java特有语法组合检测
        int javaFeatures = 0;
        
        // 访问修饰符 + 类型声明
        if (code.matches(".*public\\s+(static\\s+)?(int|String|void|boolean|long|double)\\s+\\w+\\s*\\(.*")) {
            javaFeatures += 2;
        }
        
        // Java数组特征
        if (code.contains("int[]") || code.contains("String[]") || code.contains("char[]") ||
            code.contains("new int[") || code.contains("new String[") || code.contains("new char[")) {
            javaFeatures += 2;
        }
        
        // Java泛型和集合特征
        if (code.contains("Set<") || code.contains("List<") || code.contains("Map<") ||
            code.contains("HashSet<") || code.contains("ArrayList<") || code.contains("HashMap<")) {
            javaFeatures += 2;
        }
        
        // Java Stream API特征
        if (code.contains(".stream()") || code.contains(".mapToInt(") || code.contains("::")) {
            javaFeatures += 2;
        }
        
        // Java特有操作
        if (code.contains(".length") && code.contains("for") && code.contains("++")) {
            javaFeatures += 1;
        }
        
        // Java导入语句
        if (code.contains("import java.") || code.contains("import javax.")) {
            javaFeatures += 2;
        }
        
        // 方法定义模式
        if (code.matches(".*public\\s+static\\s+\\w+.*\\{.*") && code.contains("return")) {
            javaFeatures += 1;
        }
        
        // 需要至少3个特征才认定为Java
        return javaFeatures >= 3;
    }
    
    /**
     * 增强的JavaScript代码检测 - 更精确的特征识别
     */
    private boolean isJavaScriptCode(String code, String lowerCode) {
        // 1. 强特征检测
        if (code.contains("console.log") || code.contains("document.") || 
            code.contains("window.") || code.contains("localStorage.")) {
            return true;
        }
        
        // 2. 现代JavaScript语法
        int jsFeatures = 0;
        
        // ES6+ 特征
        if (code.contains("const ") || code.contains("let ") || code.contains("=>")) {
            jsFeatures += 2;
        }
        
        // 函数定义（排除Python）
        if (code.contains("function ") && !code.contains("def ")) {
            jsFeatures += 1;
        }
        
        // JavaScript特有操作
        if (code.contains(".push(") || code.contains(".map(") || code.contains(".filter(")) {
            jsFeatures += 1;
        }
        
        // JSON操作
        if (code.contains("JSON.parse") || code.contains("JSON.stringify")) {
            jsFeatures += 2;
        }
        
        // 回调和异步
        if (code.contains("callback") || code.contains("async") || code.contains("await")) {
            jsFeatures += 1;
        }
        
        return jsFeatures >= 2;
    }
    
    /**
     * 增强的Python检测 - 包括数据科学和机器学习库
     */
    private boolean isPythonCode(String code, String lowerCode) {
        // 1. 强特征检测
        if (code.contains("def ") || code.contains("if __name__ == '__main__':") ||
            code.contains("import numpy") || code.contains("import pandas")) {
            return true;
        }
        
        // 2. Python特征组合检测
        int pythonFeatures = 0;
        
        // 缩进语法（Python特有）
        if (code.matches(".*\\s{4,}.*") && !code.contains("{") && !code.contains(";")) {
            pythonFeatures += 2;
        }
        
        // Python内置函数
        if (code.contains("print(") || code.contains("len(") || code.contains("range(") || code.contains("str(")) {
            pythonFeatures += 1;
        }
        
        // Python导入语句
        if (code.contains("import ") || code.contains("from ") && code.contains("import")) {
            pythonFeatures += 1;
        }
        
        // 数据科学库特征
        if (code.contains("pd.") || code.contains("np.") || 
            code.contains("plt.") || code.contains("df.")) {
            pythonFeatures += 2;
        }
        
        // Python列表操作
        if (code.contains(".append(") || code.contains(".extend(") || 
            code.matches(".*\\[.*for.*in.*\\].*")) { // 列表推导式
            pythonFeatures += 1;
        }
        
        // Python字符串操作
        if (code.contains(".split(") || code.contains(".join(") || code.contains("f\"") || code.contains("f'")) {
            pythonFeatures += 1;
        }
        
        return pythonFeatures >= 2;
    }
    
    /**
     * Shell脚本检测 - 加强判断条件，避免误识别
     */
    private boolean detectShell(String code, String lowerCode) {
        // 必须有明确的Shell特征才认定为Shell脚本
        
        // 1. Shebang行是最强特征
        if (code.startsWith("#!/bin/bash") || code.startsWith("#!/bin/sh")) {
            return true;
        }
        
        // 2. 必须同时满足多个Shell特征才认定
        int shellFeatures = 0;
        
        // Shell命令特征
        if (code.contains("echo ") || code.contains("grep ") || 
            code.contains("awk ") || code.contains("sed ") ||
            code.contains("find ") || code.contains("curl ") ||
            code.contains("wget ") || code.contains("ping ") ||
            code.contains("netstat ") || code.contains("ps ")) {
            shellFeatures++;
        }
        
        // Shell语法特征
        if (code.contains("if [ ") || code.contains("for i in ") ||
            code.contains("while ") || code.matches(".*\\$\\w+.*")) {
            shellFeatures++;
        }
        
        // 管道操作
        if (code.contains(" | ") && !code.contains("function") && !code.contains("def")) {
            shellFeatures++;
        }
        
        // 只有同时满足2个以上Shell特征才认定为Shell脚本
        return shellFeatures >= 2;
    }
    
    /**
     * 增强的SQL检测
     */
    private boolean detectSQL(String lowerCode) {
        // 基础SQL关键字
        if (lowerCode.contains("select ") || lowerCode.contains("insert ") || 
            lowerCode.contains("update ") || lowerCode.contains("delete ") ||
            lowerCode.contains("create table") || lowerCode.contains("alter table")) {
            return true;
        }
        
        // SQL函数和子句
        if (lowerCode.contains("count(") || lowerCode.contains("sum(") ||
            lowerCode.contains("avg(") || lowerCode.contains("max(") ||
            lowerCode.contains("min(") || lowerCode.contains("group by") ||
            lowerCode.contains("order by") || lowerCode.contains("having")) {
            return true;
        }
        
        // JOIN操作
        if (lowerCode.contains("inner join") || lowerCode.contains("left join") ||
            lowerCode.contains("right join") || lowerCode.contains("full join")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 判断是否为非可执行语言 - 修复：只保留真正的非可执行语言
     * SQL现在也使用AI判题系统，提供更智能的评分
     */
    private boolean isNonExecutableLanguage(String language) {
        // 只保留CSS为非可执行语言，SQL现在使用AI判题
        return "css".equals(language);
    }
    
    /**
     * 获取状态描述
     */
    private String getStatusDescription(String status) {
        switch (status) {
            case "AC": return "完全正确";
            case "PA": return "部分正确";
            case "WA": return "答案错误";
            case "TLE": return "执行超时";
            case "MLE": return "内存超限";
            case "CE": return "编译错误";
            case "RE": return "运行错误";
            case "SE": return "系统错误";
            default: return "未知状态";
        }
    }
    
    /**
     * 构建详细的分析信息
     */
    private String buildDetailedAnalysis(CodeJudgeResult judgeResult) {
        StringBuilder analysis = new StringBuilder();
        
        // 基本判题信息
        analysis.append("判题结果: ").append(getStatusDescription(judgeResult.getStatus())).append("\n");
        analysis.append("得分: ").append(judgeResult.getScore()).append("/100\n");
        analysis.append("通过测试用例: ").append(judgeResult.getPassedCount())
                .append("/").append(judgeResult.getTotalCount()).append("\n");
        
        // 执行信息
        if (judgeResult.getExecutionTime() != null) {
            analysis.append("执行时间: ").append(judgeResult.getExecutionTime()).append("ms\n");
        }
        if (judgeResult.getMemoryUsage() != null) {
            analysis.append("内存使用: ").append(judgeResult.getMemoryUsage()).append("KB\n");
        }
        
        // 错误信息
        if (judgeResult.getErrorMessage() != null && !judgeResult.getErrorMessage().isEmpty()) {
            analysis.append("错误信息: ").append(judgeResult.getErrorMessage()).append("\n");
        }
        
        // 详细测试用例结果
        if (judgeResult.getTestCaseResults() != null && !judgeResult.getTestCaseResults().isEmpty()) {
            analysis.append("\n测试用例详情:\n");
            for (int i = 0; i < judgeResult.getTestCaseResults().size(); i++) {
                CodeJudgeResult.TestCaseResult testResult = judgeResult.getTestCaseResults().get(i);
                analysis.append("  用例 ").append(i + 1).append(": ");
                
                if (testResult.getPassed()) {
                    analysis.append("通过\n");
                } else {
                    analysis.append("失败");
                    if (testResult.getError() != null && !testResult.getError().isEmpty()) {
                        analysis.append(" (").append(testResult.getError()).append(")");
                    }
                    analysis.append("\n");
                    
                    // 显示输入输出对比（仅失败的用例）
                    if (testResult.getInput() != null && !testResult.getInput().isEmpty()) {
                        analysis.append("    输入: ").append(testResult.getInput().replace("\n", "\\n")).append("\n");
                    }
                    if (testResult.getExpectedOutput() != null) {
                        analysis.append("    期望: ").append(testResult.getExpectedOutput().replace("\n", "\\n")).append("\n");
                    }
                    if (testResult.getActualOutput() != null) {
                        analysis.append("    实际: ").append(testResult.getActualOutput().replace("\n", "\\n")).append("\n");
                    }
                }
            }
        }
        
        // 优化建议
        String suggestion = generateSuggestion(judgeResult);
        if (!suggestion.isEmpty()) {
            analysis.append("\n优化建议:\n").append(suggestion);
        }
        
        return analysis.toString();
    }
    
    /**
     * 根据判题结果生成优化建议
     */
    private String generateSuggestion(CodeJudgeResult judgeResult) {
        StringBuilder suggestion = new StringBuilder();
        
        if ("CE".equals(judgeResult.getStatus())) {
            suggestion.append("- 请检查代码语法错误，确保所有括号匹配\n");
            suggestion.append("- 确认类名为Main（Java）或函数名正确\n");
            suggestion.append("- 检查是否缺少必要的import语句\n");
        } else if ("WA".equals(judgeResult.getStatus()) || "PA".equals(judgeResult.getStatus())) {
            suggestion.append("- 仔细检查题目要求的输入输出格式\n");
            suggestion.append("- 确认算法逻辑是否正确处理了所有情况\n");
            suggestion.append("- 注意边界条件和特殊输入的处理\n");
            if (judgeResult.getPassedCount() > 0) {
                suggestion.append("- 已有部分测试用例通过，继续调试剩余问题\n");
            }
        } else if ("TLE".equals(judgeResult.getStatus())) {
            suggestion.append("- 算法时间复杂度可能过高，考虑优化算法\n");
            suggestion.append("- 检查是否存在死循环或无限递归\n");
            suggestion.append("- 尝试使用更高效的数据结构\n");
        } else if ("RE".equals(judgeResult.getStatus())) {
            suggestion.append("- 检查数组越界或空指针异常\n");
            suggestion.append("- 确认输入读取方式是否正确\n");
            suggestion.append("- 添加必要的异常处理\n");
        } else if ("AC".equals(judgeResult.getStatus())) {
            suggestion.append("- 恭喜！代码完全正确\n");
            suggestion.append("- 可以考虑优化代码可读性和效率\n");
        }
        
        return suggestion.toString();
    }
    
    /**
     * 专业评分结果封装类
     */
    private static class CodeEvaluationResult {
        final int isCorrect;
        final BigDecimal score;
        final String analysis;
        
        CodeEvaluationResult(int isCorrect, BigDecimal score, String analysis) {
            this.isCorrect = isCorrect;
            this.score = score;
            this.analysis = analysis;
        }
    }
    
    /**
     * 专业化代码评分策略
     * 基于业界标准的多维度评分机制
     */
    private CodeEvaluationResult calculateProfessionalScore(CodeJudgeResult judgeResult) {
        String status = judgeResult.getStatus();
        int passedCount = judgeResult.getPassedCount();
        int totalCount = judgeResult.getTotalCount();
        
        // 计算通过率
        double passRate = totalCount > 0 ? (double) passedCount / totalCount : 0.0;
        
        BigDecimal score = null;
        int isCorrect;
        String analysis;
        
        // 优先使用AI返回的实际分数
        if (judgeResult.getScore() != null && judgeResult.getScore() > 0) {
            score = new BigDecimal(judgeResult.getScore()).divide(new BigDecimal("100"));
        }
        
        switch (status) {
            case "AC": // 完全正确
                if (score == null) score = BigDecimal.ONE;
                isCorrect = 1;
                analysis = buildDetailedAnalysis(judgeResult);
                break;
                
            case "PA": // 部分正确 - 渐进式评分
                if (score == null) {
                    if (passRate >= 0.8) {
                        score = new BigDecimal("0.85");
                        isCorrect = 1;
                    } else if (passRate >= 0.6) {
                        score = new BigDecimal("0.75");
                        isCorrect = 1;
                    } else if (passRate >= 0.4) {
                        score = new BigDecimal("0.65");
                        isCorrect = 0;
                    } else if (passRate >= 0.2) {
                        score = new BigDecimal("0.50");
                        isCorrect = 0;
                    } else {
                        score = new BigDecimal("0.30");
                        isCorrect = 0;
                    }
                } else {
                    isCorrect = score.compareTo(new BigDecimal("0.6")) >= 0 ? 1 : 0;
                }
                analysis = buildDetailedAnalysis(judgeResult);
                break;
                
            case "WA": // 答案错误但有部分逻辑
                if (score == null) {
                    score = passRate > 0 ? new BigDecimal("0.25") : new BigDecimal("0.15");
                }
                isCorrect = 0;
                analysis = buildDetailedAnalysis(judgeResult);
                break;
                
            case "CE": // 编译错误 - 基础语法分
                if (score == null) {
                    score = hasBasicSyntax(judgeResult) ? new BigDecimal("0.10") : BigDecimal.ZERO;
                }
                isCorrect = 0;
                analysis = "编译错误，请检查语法。" + 
                    (hasBasicSyntax(judgeResult) ? "代码结构基本正确，获得基础分数。" : "") +
                    "\n\n" + buildDetailedAnalysis(judgeResult);
                break;
                
            case "TLE": // 超时 - 算法复杂度问题
                if (score == null) {
                    score = passRate > 0 ? new BigDecimal("0.40") : new BigDecimal("0.20");
                }
                isCorrect = 0;
                analysis = "执行超时，算法需要优化。" + 
                    (passRate > 0 ? "部分测试用例通过，逻辑基本正确。" : "") +
                    "\n\n" + buildDetailedAnalysis(judgeResult);
                break;
                
            case "RE": // 运行时错误
                if (score == null) {
                    score = passRate > 0 ? new BigDecimal("0.30") : new BigDecimal("0.10");
                }
                isCorrect = 0;
                analysis = "运行时错误，请检查边界条件处理。" +
                    "\n\n" + buildDetailedAnalysis(judgeResult);
                break;
                
            case "MLE": // 内存超限
                if (score == null) {
                    score = new BigDecimal("0.35");
                }
                isCorrect = 0;
                analysis = "内存使用超限，请优化数据结构。" +
                    "\n\n" + buildDetailedAnalysis(judgeResult);
                break;
                
            default: // 系统错误
                if (score == null) {
                    score = BigDecimal.ZERO;
                }
                isCorrect = 0;
                analysis = "系统错误: " + (judgeResult.getErrorMessage() != null ? 
                    judgeResult.getErrorMessage() : "未知错误");
        }
        
        return new CodeEvaluationResult(isCorrect, score, analysis);
    }
    
    /**
     * 检测代码是否具有基础语法结构
     */
    private boolean hasBasicSyntax(CodeJudgeResult judgeResult) {
        String errorMsg = judgeResult.getErrorMessage();
        if (errorMsg == null) return false;
        
        // 如果是简单的语法错误（缺少分号、括号不匹配等），给予基础分
        return !errorMsg.toLowerCase().contains("class") && 
               !errorMsg.toLowerCase().contains("method") &&
               !errorMsg.toLowerCase().contains("cannot find symbol");
    }
    
    /**
     * 智能异常处理 - 基于异常类型和代码内容评分
     */
    private void handleJudgeException(AnswerRecord answerRecord, Exception e, String userCode) {
        String errorMsg = e.getMessage();
        String lowerErrorMsg = errorMsg != null ? errorMsg.toLowerCase() : "";
        
        // 记录详细日志
        System.err.println("代码判题异常 - 类型: " + e.getClass().getSimpleName() + ", 消息: " + errorMsg);
        
        // 基于异常类型和代码质量给予差异化评分
        if (userCode != null && !userCode.trim().isEmpty()) {
            // 有代码提交，分析代码质量
            boolean hasBasicStructure = analyzeCodeStructure(userCode);
            
            if (lowerErrorMsg.contains("timeout") || lowerErrorMsg.contains("超时")) {
                // 超时异常 - 可能是算法复杂度问题
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(hasBasicStructure ? new BigDecimal("0.25") : new BigDecimal("0.10"));
                answerRecord.setAnalysis("执行超时，可能存在算法复杂度问题。" + 
                    (hasBasicStructure ? "代码结构合理，获得基础分数。" : "") + 
                    "\n错误详情: " + errorMsg);
            } else if (lowerErrorMsg.contains("memory") || lowerErrorMsg.contains("内存")) {
                // 内存异常
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(hasBasicStructure ? new BigDecimal("0.20") : new BigDecimal("0.05"));
                answerRecord.setAnalysis("内存使用异常。" + 
                    (hasBasicStructure ? "代码逻辑基本正确。" : "") +
                    "\n错误详情: " + errorMsg);
            } else if (lowerErrorMsg.contains("compile") || lowerErrorMsg.contains("编译")) {
                // 编译异常
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(hasBasicStructure ? new BigDecimal("0.15") : BigDecimal.ZERO);
                answerRecord.setAnalysis("代码编译失败。" + 
                    (hasBasicStructure ? "代码结构基本正确，请检查语法细节。" : "请检查代码语法。") +
                    "\n错误详情: " + errorMsg);
            } else {
                // 其他系统异常
                answerRecord.setIsCorrect(0);
                answerRecord.setScore(hasBasicStructure ? new BigDecimal("0.30") : new BigDecimal("0.10"));
                answerRecord.setAnalysis("系统处理异常，但已提交代码。" + 
                    (hasBasicStructure ? "代码结构良好，建议重新提交。" : "建议检查代码后重新提交。") +
                    "\n错误详情: " + errorMsg);
            }
        } else {
            // 没有代码提交
            answerRecord.setIsCorrect(0);
            answerRecord.setScore(BigDecimal.ZERO);
            answerRecord.setAnalysis("未提交代码且系统处理异常。\n错误详情: " + errorMsg);
        }
    }
    
    /**
     * 分析代码基础结构质量
     */
    private boolean analyzeCodeStructure(String code) {
        if (code == null || code.trim().length() < 10) return false;
        
        // 检查是否包含基本的编程结构
        boolean hasFunction = code.contains("function") || code.contains("def ") || 
                             code.contains("public ") || code.contains("private ");
        boolean hasControl = code.contains("if") || code.contains("for") || 
                            code.contains("while") || code.contains("switch");
        boolean hasVariables = code.contains("=") && !code.startsWith("=");
        boolean hasBraces = code.contains("{") && code.contains("}");
        
        // 至少满足两个条件才认为结构基本合理
        int structureCount = 0;
        if (hasFunction) structureCount++;
        if (hasControl) structureCount++;
        if (hasVariables) structureCount++;
        if (hasBraces) structureCount++;
        
        return structureCount >= 2;
    }
    
    /**
     * 深拷贝JsonNode对象
     * 解决浅拷贝导致的数据共享问题
     */
    private JsonNode deepCloneNode(JsonNode original) {
        if (original == null) {
            return null;
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            // 通过序列化和反序列化实现深拷贝
            String jsonString = mapper.writeValueAsString(original);
            return mapper.readTree(jsonString);
        } catch (Exception e) {
            log.error("深拷贝JsonNode失败", e);
            return original; // 如果深拷贝失败，返回原对象
        }
    }

} 