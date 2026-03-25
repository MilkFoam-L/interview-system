package interview.service;

import interview.dto.JobDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface JobService {
    List<JobDTO> getFilteredJobs(String keyword, String location, String education, String salary, String category, String categoryType, Integer workType);

    Optional<JobDTO> findById(Integer id);

    List<JobDTO> findAll();

    List<JobDTO> findByCategory(String category);

    List<JobDTO> findByCategoryType(String categoryType);

    Page<JobDTO> queryJobs(String keyword, String location, String education, String salary, String category, String categoryType, Integer workType, Integer status, Pageable pageable);

    List<JobDTO> findByCompany(Long companyId, String keyword, String location, String education, String salary, String category, String categoryType, Integer workType, Integer status);

    Page<JobDTO> findByCompany(Long companyId, String keyword, String location, String education, String salary, String category, String categoryType, Integer workType, Integer status, Pageable pageable);

    JobDTO createJob(JobDTO jobDTO, Long companyId);

    JobDTO updateJob(Integer id, JobDTO jobDTO, Long companyId);

    void deleteJob(Integer id, Long companyId);
}