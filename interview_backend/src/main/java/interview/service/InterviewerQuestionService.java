package interview.service;

import interview.dto.QuestionDTO;
import interview.model.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 面试官题目管理服务接口
 */
public interface InterviewerQuestionService {
    
    /**
     * 分页查询题目
     */
    Page<Question> getQuestions(Pageable pageable, String category, String categoryType, 
                               String type, String difficulty, String keyword);
    
    /**
     * 根据ID获取题目
     */
    Question getQuestionById(Long id);
    
    /**
     * 根据ID获取题目详情（包含用户名信息）
     */
    QuestionDTO getQuestionDetailById(Long id);
    
    /**
     * 创建题目
     */
    Question createQuestion(Question question);
    
    /**
     * 更新题目
     */
    Question updateQuestion(Long id, Question question);
    
    /**
     * 删除题目（硬删除）
     */
    void deleteQuestion(Long id, Long operatorId);
    
    /**
     * 批量删除题目
     */
    Map<String, Object> batchDeleteQuestions(List<Long> questionIds, Long operatorId);
    
    /**
     * 从Excel文件批量导入题目
     */
    Map<String, Object> uploadFromExcel(MultipartFile file, Long operatorId);
    
    /**
     * 生成Excel模板
     */
    byte[] generateExcelTemplate();
    
    /**
     * 获取题目统计信息
     */
    Map<String, Object> getQuestionStatistics(String category, String categoryType, String type);
    
    /**
     * 获取所有类别选项
     */
    Map<String, List<String>> getAllCategories();
    
    /**
     * 切换题目启用状态
     */
    Question toggleQuestionActive(Long id, Long operatorId);
}