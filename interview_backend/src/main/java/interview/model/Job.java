package interview.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "job")
public class Job {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(name = "title")
    private String title;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id")
    private CompanyInfo company;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "salary")
    private String salary;
    
    @Column(name = "requirements")
    private String requirements;
    
    @Column(name = "experience")
    private String experience;
    
    @Column(name = "education")
    private String education;
    
    @Column(name = "category")
    private String category;
    
    @Column(name = "categoryType")
    private String categoryType;

    @Column(name = "workType")
    private Integer workType;

    @Column(name = "duties")
    private String duties;

    @Column(name = "status")
    private Integer status;

    @Column(name = "applications")
    private String applications;

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

