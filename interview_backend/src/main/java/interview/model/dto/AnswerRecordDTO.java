package interview.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AnswerRecordDTO {
    private Long id;
    private Long userId;
    private Long questionId;
    private Long interviewId;
    private Long sessionId;
    private String userAnswer;
    private String type;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private Integer timeUsed;
    private Integer isCorrect;
    private BigDecimal score;
    private String analysis;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long memoryUsage;
    private Long executionTime;
} 