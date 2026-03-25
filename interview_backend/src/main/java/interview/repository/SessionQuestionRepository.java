package interview.repository;

import interview.model.entity.SessionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SessionQuestionRepository extends JpaRepository<SessionQuestion, Long> {
    List<SessionQuestion> findBySessionIdAndStageOrderBySequenceAsc(Long sessionId, String stage);
    List<SessionQuestion> findBySessionId(Long sessionId);
    boolean existsBySessionIdAndStage(Long sessionId, String stage);
} 