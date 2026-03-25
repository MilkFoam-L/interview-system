package interview.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuestionDetailDTO {
    private Long questionId;
    private String stage;
    private Integer sequence;
    private String status;
    // questions 表字段
    private String content;
    private String inputType;
    private List<String> options;
    private String difficulty;
    private String tags;
    private String correctAnswer;
    // ...如需可扩展更多字段
} 