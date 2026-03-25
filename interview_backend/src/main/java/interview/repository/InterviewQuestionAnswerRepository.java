package interview.repository;

import interview.model.InterviewQuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionAnswerRepository extends JpaRepository<InterviewQuestionAnswer, Long> {
    List<InterviewQuestionAnswer> findBySessionIdOrderByRoundNo(Long sessionId);
    List<InterviewQuestionAnswer> findBySessionIdAndRoundNo(Long sessionId, Integer roundNo);
    
    long countBySessionIdAndAnswerIsNull(Long sessionId);
} 