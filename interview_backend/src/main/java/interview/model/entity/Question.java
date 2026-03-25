package interview.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "input_type", length = 50)
    private String inputType;

    @Column(name = "options", columnDefinition = "text")
    private String options;

    @Column(name = "correct_answer", columnDefinition = "text")
    private String correctAnswer;

    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Column(name = "tags", length = 255)
    private String tags;

    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "categoryType", length = 255)
    private String categoryType;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "updated_by") 
    private Long updatedBy;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "usage_count")
    private Integer usageCount = 0;
} 