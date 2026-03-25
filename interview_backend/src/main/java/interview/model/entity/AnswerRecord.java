package interview.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "answer_record")
public class AnswerRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "interview_id")
    private Long interviewId;
    
    @Transient // 不映射到数据库表中
    private Long sessionId; // 用于与前端交互，暂存sessionId

    @Column(name = "user_answer", columnDefinition = "text")
    private String userAnswer;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "time_used")
    private Integer timeUsed;

    @Column(name = "is_correct")
    private Integer isCorrect;

    @Column(name = "score", precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "analysis", columnDefinition = "text")
    private String analysis;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
} 