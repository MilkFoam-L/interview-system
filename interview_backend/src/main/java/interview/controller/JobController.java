package interview.controller;

import interview.dto.JobDTO;
import interview.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import interview.dto.CompanyInfoDTO;
import interview.service.CompanyInfoService;
import interview.util.UserContextUtil;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 岗位控制器
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {
    
    @Autowired
    private JobService jobService;

    @Autowired
    private UserContextUtil userContextUtil;

    @Autowired
    private CompanyInfoService companyInfoService;
    
    /**
     * 获取所有岗位列表
     */
    @GetMapping
    public ResponseEntity<List<JobDTO>> getAllJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String salary,
            @RequestParam(required = false) Integer workType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryType) {
        
        if (keyword != null || location != null || education != null || 
            salary != null || category != null || categoryType != null || workType != null) {
            List<JobDTO> filteredJobs = jobService.getFilteredJobs(
                    keyword, location, education, salary, category, categoryType, workType);
            return ResponseEntity.ok(filteredJobs);
        }
        
        List<JobDTO> jobs = jobService.findAll();
        return ResponseEntity.ok(jobs);
    }
    
    /**
     * 获取岗位详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobDTO> getJobById(@PathVariable Integer id) {
        return jobService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 按类别查询岗位
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<JobDTO>> getJobsByCategory(@PathVariable String category) {
        List<JobDTO> jobs = jobService.findByCategory(category);
        return ResponseEntity.ok(jobs);
    }
    
    /**
     * 按岗位类型查询
     */
    @GetMapping("/type/{categoryType}")
    public ResponseEntity<List<JobDTO>> getJobsByCategoryType(@PathVariable String categoryType) {
        List<JobDTO> jobs = jobService.findByCategoryType(categoryType);
        return ResponseEntity.ok(jobs);
    }

    /**
     * 获取当前登录公司下的岗位列表
     */
    @GetMapping("/my")
    public ResponseEntity<List<JobDTO>> getMyCompanyJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String salary,
            @RequestParam(required = false) Integer workType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryType,
            @RequestParam(required = false) Integer status
    ) {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<CompanyInfoDTO> companyOpt = companyInfoService.findByUserId(userId);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Long companyId = companyOpt.get().getId();

        List<JobDTO> mine = jobService.findByCompany(
                companyId, keyword, location, education, salary, category, categoryType, workType, status);
        return ResponseEntity.ok(mine);
    }

    /**
     * 获取当前登录公司下的岗位分页列表
     */
    @GetMapping("/my/page")
    public ResponseEntity<Page<JobDTO>> getMyCompanyJobsPage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String salary,
            @RequestParam(required = false) Integer workType,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryType,
            @RequestParam(required = false) Integer status,
            Pageable pageable
    ) {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<CompanyInfoDTO> companyOpt = companyInfoService.findByUserId(userId);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Long companyId = companyOpt.get().getId();

        Page<JobDTO> page = jobService.findByCompany(
                companyId, keyword, location, education, salary, category, categoryType, workType, status, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * 获取当前登录公司下的岗位“部门/分类”
     */
    @GetMapping("/my/departments")
    public ResponseEntity<List<Map<String, Object>>> getMyCompanyDepartments() {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Optional<CompanyInfoDTO> companyOpt = companyInfoService.findByUserId(userId);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Long companyId = companyOpt.get().getId();

        List<JobDTO> mine = jobService.findByCompany(
                companyId, null, null, null, null, null, null, null, null);

        Map<String, List<String>> categoryToTypes = new HashMap<>();
        for (JobDTO j : mine) {
            String cat = j.getCategory();
            String type = j.getCategoryType();
            if (cat == null || cat.trim().isEmpty()) {
                continue;
            }
            categoryToTypes.computeIfAbsent(cat, k -> new ArrayList<>());
            if (type != null && !type.trim().isEmpty() && !categoryToTypes.get(cat).contains(type)) {
                categoryToTypes.get(cat).add(type);
            }
        }

        List<Map<String, Object>> tree = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : categoryToTypes.entrySet()) {
            Map<String, Object> parent = new HashMap<>();
            parent.put("name", e.getKey());
            List<Map<String, Object>> children = e.getValue().stream().map(t -> {
                Map<String, Object> child = new HashMap<>();
                child.put("name", t);
                return child;
            }).collect(Collectors.toList());
            parent.put("children", children);
            tree.add(parent);
        }

        return ResponseEntity.ok(tree);
    }

    /**
     * 创建岗位
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createJob(@RequestBody JobDTO jobDTO) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        Optional<CompanyInfoDTO> companyOpt = companyInfoService.findByUserId(userId);
        if (!companyOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "公司信息不存在，请先完善公司信息");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            JobDTO createdJob = jobService.createJob(jobDTO, companyOpt.get().getId());
            response.put("success", true);
            response.put("message", "岗位创建成功");
            response.put("data", createdJob);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新岗位
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateJob(@PathVariable Integer id, @RequestBody JobDTO jobDTO) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        Optional<CompanyInfoDTO> companyOpt = companyInfoService.findByUserId(userId);
        if (!companyOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "公司信息不存在");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            JobDTO updatedJob = jobService.updateJob(id, jobDTO, companyOpt.get().getId());
            response.put("success", true);
            response.put("message", "岗位更新成功");
            response.put("data", updatedJob);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除岗位
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteJob(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        Optional<CompanyInfoDTO> companyOpt = companyInfoService.findByUserId(userId);
        if (!companyOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "公司信息不存在");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            jobService.deleteJob(id, companyOpt.get().getId());
            response.put("success", true);
            response.put("message", "岗位删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
