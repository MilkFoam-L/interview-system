package interview.repository;

import interview.dto.QuestionDTO;
import interview.model.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {
    
    // 原有方法
    List<Question> findByCategoryTypeAndType(String categoryType, String type);
    List<Question> findByCategoryAndType(String category, String type);
    
    // 面试官端新增方法
    @Query("SELECT q FROM Question q WHERE q.isActive = true AND q.categoryType = :categoryType AND q.type = :type")
    List<Question> findActiveByCategoryTypeAndType(@Param("categoryType") String categoryType, @Param("type") String type);
    
    @Query("SELECT q FROM Question q WHERE q.isActive = true AND q.category = :category AND q.type = :type")
    List<Question> findActiveByCategoryAndType(@Param("category") String category, @Param("type") String type);
    
    @Query("SELECT DISTINCT q.category FROM Question q WHERE q.category IS NOT NULL")
    List<String> findDistinctCategories();
    
    @Query("SELECT DISTINCT q.categoryType FROM Question q WHERE q.categoryType IS NOT NULL")
    List<String> findDistinctCategoryTypes();
    
    @Query("SELECT DISTINCT q.type FROM Question q WHERE q.type IS NOT NULL")
    List<String> findDistinctTypes();
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.isActive = true")
    long countActiveQuestions();
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.isActive = true AND q.category = :category")
    long countActiveByCategory(@Param("category") String category);
    
    @Query("SELECT COUNT(q) FROM Question q WHERE q.isActive = true AND q.categoryType = :categoryType")
    long countActiveByCategoryType(@Param("categoryType") String categoryType);
    
    // 面试官端查询方法
    List<Question> findByIsActive(Boolean isActive);
    
    // 关联查询用户信息的方法
    @Query("SELECT q FROM Question q " +
           "LEFT JOIN User uc ON q.createdBy = uc.id " +
           "LEFT JOIN User uu ON q.updatedBy = uu.id " +
           "WHERE q.id = :id")
    Optional<Question> findByIdWithUsers(@Param("id") Long id);
    
    // 获取题目详情时的用户名查询
    @Query("SELECT u.username FROM User u WHERE u.id = :userId")
    Optional<String> findUsernameById(@Param("userId") Long userId);
} 