package interview.controller;

import interview.dto.QuestionDTO;
import interview.model.entity.Question;
import interview.service.InterviewerQuestionService;
import interview.util.UserContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 面试官端题目管理控制器
 * 路径前缀: /api/interviewer/questions
 * 权限要求: INTERVIEWER角色
 */
@RestController
@RequestMapping("/api/interviewer/questions")
public class InterviewerQuestionController {
    
    @Autowired
    private InterviewerQuestionService interviewerQuestionService;
    
    @Autowired
    private UserContextUtil userContextUtil;
    
    /**
     * 获取题目分页列表
     */
    @GetMapping
    public ResponseEntity<Page<Question>> getQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryType,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword) {
        
        // 权限检查
        checkInterviewerPermission();
        
        // 解析排序参数
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Question> questions = interviewerQuestionService.getQuestions(
                pageable, category, categoryType, type, difficulty, keyword);
        return ResponseEntity.ok(questions);
    }
    
    /**
     * 根据ID获取题目详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        checkInterviewerPermission();
        Question question = interviewerQuestionService.getQuestionById(id);
        return ResponseEntity.ok(question);
    }
    
    /**
     * 根据ID获取题目详情（包含用户名信息）
     */
    @GetMapping("/{id}/detail")
    public ResponseEntity<QuestionDTO> getQuestionDetailById(@PathVariable Long id) {
        checkInterviewerPermission();
        QuestionDTO questionDetail = interviewerQuestionService.getQuestionDetailById(id);
        return ResponseEntity.ok(questionDetail);
    }
    
    /**
     * 创建新题目
     */
    @PostMapping
    public ResponseEntity<Question> createQuestion(@RequestBody Question question) {
        checkInterviewerPermission();
        
        // 设置创建者
        Long currentUserId = userContextUtil.getCurrentUserId();
        question.setCreatedBy(currentUserId);
        
        Question created = interviewerQuestionService.createQuestion(question);
        return ResponseEntity.ok(created);
    }
    
    /**
     * 更新题目
     */
    @PutMapping("/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question question) {
        checkInterviewerPermission();
        
        // 设置更新者
        Long currentUserId = userContextUtil.getCurrentUserId();
        question.setUpdatedBy(currentUserId);
        
        Question updated = interviewerQuestionService.updateQuestion(id, question);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * 删除题目（硬删除）
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        checkInterviewerPermission();
        
        Long currentUserId = userContextUtil.getCurrentUserId();
        interviewerQuestionService.deleteQuestion(id, currentUserId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 批量删除题目
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteQuestions(@RequestBody List<Long> questionIds) {
        checkInterviewerPermission();
        
        Long currentUserId = userContextUtil.getCurrentUserId();
        Map<String, Object> result = interviewerQuestionService.batchDeleteQuestions(questionIds, currentUserId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Excel批量上传题目
     */
    @PostMapping("/upload/excel")
    public ResponseEntity<Map<String, Object>> uploadExcel(@RequestParam("file") MultipartFile file) {
        checkInterviewerPermission();
        
        Long currentUserId = userContextUtil.getCurrentUserId();
        Map<String, Object> result = interviewerQuestionService.uploadFromExcel(file, currentUserId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 下载Excel模板
     */
    @GetMapping("/template/excel")
    public ResponseEntity<byte[]> downloadExcelTemplate() {
        // 临时注释权限检查用于调试
        // checkInterviewerPermission();
        
        try {
            byte[] template = interviewerQuestionService.generateExcelTemplate();
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=question_template.xlsx")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(template);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("生成模板失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取题目使用统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getQuestionStatistics(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String categoryType,
            @RequestParam(required = false) String type) {
        
        checkInterviewerPermission();
        
        Map<String, Object> statistics = interviewerQuestionService.getQuestionStatistics(category, categoryType, type);
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 获取所有类别选项
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, List<String>>> getCategories() {
        checkInterviewerPermission();
        
        Map<String, List<String>> categories = interviewerQuestionService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
    
    /**
     * 切换题目启用状态
     */
    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<Question> toggleQuestionActive(@PathVariable Long id) {
        checkInterviewerPermission();
        
        Long currentUserId = userContextUtil.getCurrentUserId();
        Question updated = interviewerQuestionService.toggleQuestionActive(id, currentUserId);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * 检查面试官权限
     */
    private void checkInterviewerPermission() {
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("用户未登录");
        }
        
        String userRole = userContextUtil.getCurrentUserRole();
        if (!"INTERVIEWER".equals(userRole)) {
            throw new SecurityException("权限不足：只有面试官可以管理题库");
        }
    }
    
    /**
     * 解析排序参数
     */
    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.trim().isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        
        String[] parts = sortParam.split(",");
        if (parts.length != 2) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        
        String property = parts[0].trim();
        String direction = parts[1].trim().toLowerCase();
        
        Sort.Direction sortDirection = "desc".equals(direction) ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
            
        return Sort.by(sortDirection, property);
    }
}