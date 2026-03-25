package interview.repository;

import interview.model.InterviewStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterviewStatisticsRepository extends JpaRepository<InterviewStatistics, Long> {
    
    Optional<InterviewStatistics> findByUserId(Long userId);
} 