package interview.repository;

import interview.model.ScenarioReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface ScenarioReportRepository extends JpaRepository<ScenarioReport, Long> {
} 