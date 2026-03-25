package interview.repository;

import interview.model.entity.InterviewStageProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterviewStageProgressRepository extends JpaRepository<InterviewStageProgress, Long> {
    List<InterviewStageProgress> findBySessionId(Long sessionId);
    InterviewStageProgress findBySessionIdAndStage(Long sessionId, String stage);
} 