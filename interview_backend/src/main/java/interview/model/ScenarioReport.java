package interview.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 对应表 scenario_reports
 */
@Data
@Entity
@Table(name = "scenario_reports")
public class ScenarioReport {

    @Id
    @Column(name = "session_id")
    private Long sessionId; // PK 同时是外键

    @Column(name = "total_score")
    private Double totalScore;

    private String level;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    /**
     * 为向后兼容旧版数据库表中名为 report 的列，新增同名字段。
     * 保存时会同时写入 content 与 report，避免 "Field 'report' doesn't have a default value" 错误。
     */
    @Lob
    @Column(name = "report", columnDefinition = "MEDIUMTEXT")
    private String report;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;

    // 维度细分评分
    @Column(name = "tech_score")
    private Integer techScore;
    @Column(name = "project_score")
    private Integer projectScore;
    @Column(name = "team_score")
    private Integer teamScore;
    @Column(name = "plan_score")
    private Integer planScore;
    @Column(name = "attitude_score")
    private Integer attitudeScore;

    /**
     * 候选人简历主键，外键关联 resumes.id；表列为 NOT NULL，所以生成报告时必须写入。
     */
    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    /**
     * 旧版本库中的score列，语义等同于 totalScore；为兼容加此字段。
     */
    @Column(name = "score")
    private Double score;
} 