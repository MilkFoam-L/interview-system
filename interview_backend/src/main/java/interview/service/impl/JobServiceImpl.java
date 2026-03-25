package interview.service.impl;

import interview.dto.JobDTO;
import interview.model.Job;
import interview.repository.JobRepository;
import interview.service.JobService;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JobDTO> getFilteredJobs(String keyword, String location, String education, String salary, String category, String categoryType, Integer workType) {
        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("title"), "%" + keyword + "%"));
            }
            if (StringUtils.hasText(location)) {
                predicates.add(cb.equal(root.get("location"), location));
            }
            if (StringUtils.hasText(education)) {
                predicates.add(cb.equal(root.get("education"), education));
            }
            if (StringUtils.hasText(salary)) {
                predicates.add(cb.equal(root.get("salary"), salary));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (StringUtils.hasText(categoryType)) {
                predicates.add(cb.equal(root.get("categoryType"), categoryType));
            }
            if (workType != null) {
                predicates.add(cb.equal(root.get("workType"), workType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        List<Job> jobs = jobRepository.findAll(spec);
        return jobs.stream().map(JobDTO::new).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobDTO> findById(Integer id) {
        return jobRepository.findByIdWithCompany(id).map(JobDTO::new);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<JobDTO> findAll() {
        return jobRepository.findAllWithCompany().stream().map(JobDTO::new).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobDTO> findByCategory(String category) {
        return jobRepository.findByCategoryWithCompany(category).stream().map(JobDTO::new).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobDTO> findByCategoryType(String categoryType) {
        return jobRepository.findByCategoryTypeWithCompany(categoryType).stream().map(JobDTO::new).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDTO> queryJobs(String keyword, String location, String education, String salary, String category, String categoryType, Integer workType, Integer status, Pageable pageable) {
        Specification<Job> spec = buildSpec(null, keyword, location, education, salary, category, categoryType, workType, status);
        Page<Job> page = jobRepository.findAll(spec, pageable);
        return page.map(JobDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobDTO> findByCompany(Long companyId, String keyword, String location, String education, String salary, String category, String categoryType, Integer workType, Integer status) {
        Specification<Job> spec = buildSpec(companyId, keyword, location, education, salary, category, categoryType, workType, status);
        List<Job> jobs = jobRepository.findAll(spec);
        return jobs.stream().map(JobDTO::new).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobDTO> findByCompany(Long companyId, String keyword, String location, String education, String salary, String category, String categoryType, Integer workType, Integer status, Pageable pageable) {
        Specification<Job> spec = buildSpec(companyId, keyword, location, education, salary, category, categoryType, workType, status);
        Page<Job> page = jobRepository.findAll(spec, pageable);
        return page.map(JobDTO::new);
    }

    private Specification<Job> buildSpec(Long companyId,
                                         String keyword,
                                         String location,
                                         String education,
                                         String salary,
                                         String category,
                                         String categoryType,
                                         Integer workType,
                                         Integer status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (companyId != null) {
                predicates.add(cb.equal(root.get("company").get("id"), companyId));
            }
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(root.get("title"), "%" + keyword + "%"));
            }
            if (StringUtils.hasText(location)) {
                predicates.add(cb.equal(root.get("location"), location));
            }
            if (StringUtils.hasText(education)) {
                predicates.add(cb.equal(root.get("education"), education));
            }
            if (StringUtils.hasText(salary)) {
                predicates.add(cb.equal(root.get("salary"), salary));
            }
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (StringUtils.hasText(categoryType)) {
                predicates.add(cb.equal(root.get("categoryType"), categoryType));
            }
            if (workType != null) {
                predicates.add(cb.equal(root.get("workType"), workType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    @Transactional
    public JobDTO createJob(JobDTO jobDTO, Long companyId) {
        Job job = convertToEntity(jobDTO);
        
        // 设置公司ID和其他默认值
        job.setCompany(new interview.model.CompanyInfo());
        job.getCompany().setId(companyId);
        
        // 设置默认状态为开放
        if (job.getStatus() == null) {
            job.setStatus(0);
        }
        
        // 设置默认应用数量
        if (job.getApplications() == null) {
            job.setApplications("0");
        }
        
        Job savedJob = jobRepository.save(job);
        return new JobDTO(savedJob);
    }

    @Override
    @Transactional
    public JobDTO updateJob(Integer id, JobDTO jobDTO, Long companyId) {
        Optional<Job> existingJobOpt = jobRepository.findById(id);
        if (!existingJobOpt.isPresent()) {
            throw new RuntimeException("岗位不存在");
        }
        
        Job existingJob = existingJobOpt.get();
        
        // 验证权限：确保岗位属于当前公司
        if (existingJob.getCompany() == null || !existingJob.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("无权限操作该岗位");
        }
        
        // 更新字段
        updateJobFields(existingJob, jobDTO);
        
        Job savedJob = jobRepository.save(existingJob);
        return new JobDTO(savedJob);
    }

    @Override
    @Transactional
    public void deleteJob(Integer id, Long companyId) {
        Optional<Job> existingJobOpt = jobRepository.findById(id);
        if (!existingJobOpt.isPresent()) {
            throw new RuntimeException("岗位不存在");
        }
        
        Job existingJob = existingJobOpt.get();
        
        // 验证权限：确保岗位属于当前公司
        if (existingJob.getCompany() == null || !existingJob.getCompany().getId().equals(companyId)) {
            throw new RuntimeException("无权限操作该岗位");
        }
        
        jobRepository.deleteById(id);
    }

    private Job convertToEntity(JobDTO jobDTO) {
        Job job = new Job();
        job.setId(jobDTO.getId());
        job.setTitle(jobDTO.getTitle());
        job.setLocation(jobDTO.getLocation());
        job.setSalary(jobDTO.getSalary());
        job.setRequirements(jobDTO.getRequirements());
        job.setExperience(jobDTO.getExperience());
        job.setEducation(jobDTO.getEducation());
        job.setCategory(jobDTO.getCategory());
        job.setCategoryType(jobDTO.getCategoryType());
        job.setWorkType(jobDTO.getWorkType());
        job.setDuties(jobDTO.getDuties());
        job.setStatus(jobDTO.getStatus());
        job.setApplications(jobDTO.getApplications());
        job.setDescription(jobDTO.getDescription());
        return job;
    }

    private void updateJobFields(Job existingJob, JobDTO jobDTO) {
        if (jobDTO.getTitle() != null) {
            existingJob.setTitle(jobDTO.getTitle());
        }
        if (jobDTO.getLocation() != null) {
            existingJob.setLocation(jobDTO.getLocation());
        }
        if (jobDTO.getSalary() != null) {
            existingJob.setSalary(jobDTO.getSalary());
        }
        if (jobDTO.getRequirements() != null) {
            existingJob.setRequirements(jobDTO.getRequirements());
        }
        if (jobDTO.getExperience() != null) {
            existingJob.setExperience(jobDTO.getExperience());
        }
        if (jobDTO.getEducation() != null) {
            existingJob.setEducation(jobDTO.getEducation());
        }
        if (jobDTO.getCategory() != null) {
            existingJob.setCategory(jobDTO.getCategory());
        }
        if (jobDTO.getCategoryType() != null) {
            existingJob.setCategoryType(jobDTO.getCategoryType());
        }
        if (jobDTO.getWorkType() != null) {
            existingJob.setWorkType(jobDTO.getWorkType());
        }
        if (jobDTO.getDuties() != null) {
            existingJob.setDuties(jobDTO.getDuties());
        }
        if (jobDTO.getStatus() != null) {
            existingJob.setStatus(jobDTO.getStatus());
        }
        if (jobDTO.getApplications() != null) {
            existingJob.setApplications(jobDTO.getApplications());
        }
        if (jobDTO.getDescription() != null) {
            existingJob.setDescription(jobDTO.getDescription());
        }
    }
} 