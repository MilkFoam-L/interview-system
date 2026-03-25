package interview.model;

import lombok.Data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "interview_statistics")
public class InterviewStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "submitted_count", nullable = false)
    private Integer submittedCount = 0;
    
    @Column(name = "pending_count", nullable = false)
    private Integer pendingCount = 0;
    
    @Column(name = "completed_count", nullable = false)
    private Integer completedCount = 0;
    
    @Column(name = "passed_count", nullable = false)
    private Integer passedCount = 0;
    
    @Column(name = "pass_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal passRate = BigDecimal.ZERO;
    
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;
} 