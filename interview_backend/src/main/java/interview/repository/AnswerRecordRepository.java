package interview.repository;

import interview.model.entity.AnswerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {
    List<AnswerRecord> findByUserId(Long userId);
    
    List<AnswerRecord> findByQuestionId(Long questionId);
    
    List<AnswerRecord> findByInterviewId(Long interviewId);
    
    List<AnswerRecord> findByUserIdAndInterviewId(Long userId, Long interviewId);
    
    List<AnswerRecord> findByUserIdAndQuestionId(Long userId, Long questionId);
    
    List<AnswerRecord> findByUserIdAndQuestionIdAndInterviewId(Long userId, Long questionId, Long interviewId);
    
    /**
     * 根据面试会话ID和答题类型查询答题记录，按创建时间排序
     */
    List<AnswerRecord> findByInterviewIdAndTypeOrderByCreateTime(Long interviewId, String type);
} 