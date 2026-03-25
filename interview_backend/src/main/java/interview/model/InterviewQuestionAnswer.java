package interview.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 面试问答表映射，对应数据库表 interview_question_answers。
 */
@Data
@Entity
@Table(name = "interview_question_answers")
public class InterviewQuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "round_no", nullable = false)
    private Integer roundNo;

    private String dimension;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String question;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String answer;

    private Double relevance;

    private Integer score;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String feedback;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
