package interview.service.impl;

import interview.model.entity.SessionQuestion;
import interview.model.entity.InterviewSession;
import interview.model.entity.Question; // 假设你的题目实体为 Question
import interview.model.entity.AnswerRecord;
import interview.model.dto.QuestionDetailDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import interview.repository.SessionQuestionRepository;
import interview.repository.InterviewSessionRepository;
import interview.repository.QuestionRepository;
import interview.repository.JobRepository;
import interview.repository.AnswerRecordRepository;
import interview.service.SessionQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SessionQuestionServiceImpl implements SessionQuestionService {
    @Autowired
    private SessionQuestionRepository sessionQuestionRepository;
    @Autowired
    private InterviewSessionRepository interviewSessionRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private AnswerRecordRepository answerRecordRepository;

    @Override
    @Transactional
    public List<SessionQuestion> assignQuestions(Long sessionId, String stage, int count) {
        // 已分配则直接返回
        if (sessionQuestionRepository.existsBySessionIdAndStage(sessionId, stage)) {
            return sessionQuestionRepository.findBySessionIdAndStageOrderBySequenceAsc(sessionId, stage);
        }
        
        // 获取会话信息 - 增加错误处理
        Optional<InterviewSession> sessionOpt = interviewSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            throw new RuntimeException("找不到会话ID: " + sessionId + " 对应的面试会话，请检查会话是否存在");
        }
        
        InterviewSession session = sessionOpt.get();
        Long userId = session.getUserId(); // 获取用户ID，用于检查历史答题记录
        String categoryType = session.getPositionType(); // 细分类型
        
        // 获取岗位大类
        Integer jobId = session.getJobId();
        String category = null;
        if (jobId != null) {
            var jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isPresent()) {
                category = jobOpt.get().getCategory();
            }
        }
        
        // 1. 查询题库，优先按categoryType筛选 (只获取启用的题目)
        String targetType = stageToType(stage);
        List<Question> candidates = questionRepository.findActiveByCategoryTypeAndType(categoryType, targetType);
        
        // 2. 如果数量不足，用大类category补充 (只获取启用的题目)
        if (candidates.size() < count * 2 && category != null) { // 确保有足够的候选题目
            List<Question> more = questionRepository.findActiveByCategoryAndType(category, targetType);
            // 合并去重
            Set<Long> ids = candidates.stream().map(Question::getId).collect(Collectors.toSet());
            for (Question q : more) {
                if (!ids.contains(q.getId())) {
                    candidates.add(q);
                }
            }
        }
        
        // 3. 兜底逻辑：如果仍然没有题目，尝试查询所有类型的启用题目
        if (candidates.isEmpty()) {
            log.warn("未找到 categoryType={}, type={} 的题目，尝试兜底查询", categoryType, targetType);
            
            // 将变量设为final以供lambda使用
            final String finalCategoryType = categoryType;
            final String finalCategory = category;
            
            // 先尝试按categoryType查询所有启用的题目
            candidates = questionRepository.findByIsActive(true).stream()
                .filter(q -> finalCategoryType.equals(q.getCategoryType()))
                .collect(Collectors.toList());
            
            // 如果还是没有，按category查询
            if (candidates.isEmpty() && finalCategory != null) {
                candidates = questionRepository.findByIsActive(true).stream()
                    .filter(q -> finalCategory.equals(q.getCategory()))
                    .collect(Collectors.toList());
            }
            
            // 最后兜底：查询所有启用的题目
            if (candidates.isEmpty()) {
                log.warn("兜底查询：使用所有启用的题目");
                candidates = questionRepository.findByIsActive(true);
            }
        }
        
        // 3. 获取用户历史答题记录
        List<AnswerRecord> userAnswers = answerRecordRepository.findByUserId(userId);
        Set<Long> answeredQuestionIds = userAnswers.stream()
            .map(AnswerRecord::getQuestionId)
            .collect(Collectors.toSet());
        
        // 4. 将候选题目分为"未回答过"和"已回答过"两组
        List<Question> newQuestions = candidates.stream()
            .filter(q -> !answeredQuestionIds.contains(q.getId()))
            .collect(Collectors.toList());
        
        List<Question> answeredQuestions = candidates.stream()
            .filter(q -> answeredQuestionIds.contains(q.getId()))
            .collect(Collectors.toList());
        
        // 5. 智能选题策略
        List<Question> selected = new ArrayList<>();
        
        // 5.1 打乱顺序，增加随机性
        Collections.shuffle(newQuestions, new Random(System.currentTimeMillis()));
        Collections.shuffle(answeredQuestions, new Random(System.currentTimeMillis()));
        
        // 5.2 优先选择未回答过的题目
        int newQuestionCount = Math.min(count, newQuestions.size());
        for (int i = 0; i < newQuestionCount; i++) {
            selected.add(newQuestions.get(i));
        }
        
        // 5.3 如果未回答题不足，从已回答题中补充（优先选择回答时间较早的）
        if (selected.size() < count && !answeredQuestions.isEmpty()) {
            // 对已回答题目按最近回答时间排序（如果需要，可以进一步完善）
            int remainingCount = count - selected.size();
            for (int i = 0; i < Math.min(remainingCount, answeredQuestions.size()); i++) {
                selected.add(answeredQuestions.get(i));
            }
        }
        
        // 6. 确保选择了足够的题目
        if (selected.size() < count) {
            // 如果选择的题目不够，再随机补充一些
            final List<Question> selectedFinal = new ArrayList<>(selected); // 创建一个final副本
            List<Question> remaining = candidates.stream()
                .filter(q -> !selectedFinal.contains(q))
                .collect(Collectors.toList());
            Collections.shuffle(remaining, new Random(System.currentTimeMillis()));
            
            int needMore = count - selected.size();
            for (int i = 0; i < Math.min(needMore, remaining.size()); i++) {
                selected.add(remaining.get(i));
            }
        }
        
        // 最后确保不超过count
        if (selected.size() > count) {
            selected = selected.subList(0, count);
        }
        
        // 写入session_questions
        LocalDateTime now = LocalDateTime.now();
        int seq = 1;
        
        // 清除可能已存在的记录（避免重复）
        List<SessionQuestion> existingQuestions = sessionQuestionRepository.findBySessionIdAndStageOrderBySequenceAsc(sessionId, stage);
        if (!existingQuestions.isEmpty()) {
            sessionQuestionRepository.deleteAll(existingQuestions);
        }
        
        // 保存新分配的题目
        List<SessionQuestion> result = new ArrayList<>();
        for (Question q : selected) {
            SessionQuestion sq = new SessionQuestion();
            sq.setSessionId(sessionId);
            sq.setQuestionId(q.getId());
            sq.setStage(stage);
            sq.setSequence(seq++);
            sq.setStatus("PENDING");
            sq.setCreateTime(now);
            sq.setUpdateTime(now);
            result.add(sessionQuestionRepository.save(sq));
        }
        
        return result;
    }

    @Override
    public List<SessionQuestion> getSessionQuestions(Long sessionId, String stage) {
        return sessionQuestionRepository.findBySessionIdAndStageOrderBySequenceAsc(sessionId, stage);
    }

    @Override
    public List<QuestionDetailDTO> getSessionQuestionsWithDetail(Long sessionId, String stage) {
        List<SessionQuestion> sqs = sessionQuestionRepository.findBySessionIdAndStageOrderBySequenceAsc(sessionId, stage);
        List<Long> qids = sqs.stream().map(SessionQuestion::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionRepository.findAllById(qids);
        // Map questionId -> Question
        java.util.Map<Long, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));
        List<QuestionDetailDTO> result = new java.util.ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (SessionQuestion sq : sqs) {
            Question q = questionMap.get(sq.getQuestionId());
            if (q == null) continue;
            QuestionDetailDTO dto = new QuestionDetailDTO();
            dto.setQuestionId(sq.getQuestionId());
            dto.setStage(sq.getStage());
            dto.setSequence(sq.getSequence());
            dto.setStatus(sq.getStatus());
            dto.setContent(q.getContent());
            dto.setInputType(q.getInputType());
            // 选项字段为JSON字符串，需转为List
            try {
                if (q.getOptions() != null) {
                    dto.setOptions(objectMapper.readValue(q.getOptions(), new TypeReference<List<String>>(){}));
                }
            } catch (Exception e) {
                dto.setOptions(null);
            }
            dto.setDifficulty(q.getDifficulty());
            dto.setTags(q.getTags());
            dto.setCorrectAnswer(q.getCorrectAnswer());
            result.add(dto);
        }
        return result;
    }

    // 工具方法：将环节名映射为题目类型 - 修复基础问答类型映射
    private String stageToType(String stage) {
        switch (stage) {
            case "BASIC_QA": return "basic"; // 如果数据库中没有basic类型，会在下面添加兜底逻辑
            case "CODE_TEST": return "code";
            case "SCENARIO": return "scenario";
            default: return "basic";
        }
    }
} 