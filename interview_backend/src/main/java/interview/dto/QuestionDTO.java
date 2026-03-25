package interview.dto;

import interview.model.entity.Question;
import java.time.LocalDateTime;

/**
 * 题目数据传输对象，包含关联的用户信息
 */
public class QuestionDTO {
    private Long id;
    private String type;
    private String content;
    private String inputType;
    private String options;
    private String correctAnswer;
    private String difficulty;
    private String tags;
    private String category;
    private String categoryType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createdBy;
    private Long updatedBy;
    private Boolean isActive;
    private Integer usageCount;
    
    // 关联的用户信息
    private String createdByName;
    private String updatedByName;
    
    // 默认构造函数
    public QuestionDTO() {}
    
    // 从Question实体构造的方法
    public QuestionDTO(Question question) {
        this.id = question.getId();
        this.type = question.getType();
        this.content = question.getContent();
        this.inputType = question.getInputType();
        this.options = question.getOptions();
        this.correctAnswer = question.getCorrectAnswer();
        this.difficulty = question.getDifficulty();
        this.tags = question.getTags();
        this.category = question.getCategory();
        this.categoryType = question.getCategoryType();
        this.createTime = question.getCreateTime();
        this.updateTime = question.getUpdateTime();
        this.createdBy = question.getCreatedBy();
        this.updatedBy = question.getUpdatedBy();
        this.isActive = question.getIsActive();
        this.usageCount = question.getUsageCount();
    }
    
    // 从Question实体和用户名构造的方法
    public QuestionDTO(Question question, String createdByName, String updatedByName) {
        this(question);
        this.createdByName = createdByName;
        this.updatedByName = updatedByName;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getInputType() {
        return inputType;
    }
    
    public void setInputType(String inputType) {
        this.inputType = inputType;
    }
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }
    
    public String getCorrectAnswer() {
        return correctAnswer;
    }
    
    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
    
    public String getDifficulty() {
        return difficulty;
    }
    
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getCategoryType() {
        return categoryType;
    }
    
    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    public Long getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Integer getUsageCount() {
        return usageCount;
    }
    
    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public String getUpdatedByName() {
        return updatedByName;
    }
    
    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }
}
