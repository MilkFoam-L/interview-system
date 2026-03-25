package interview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewStatsDTO {
    private Integer notSubmittedCount; // 未投递数量
    private Integer submittedCount;    // 已投递数量
    private Integer pendingCount;      // 待面试数量
    private Integer completedCount;    // 已完成数量
    private BigDecimal passRate;       // 通过率
} 