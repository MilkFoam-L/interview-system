package interview.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "company_info")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CompanyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "legal_name")
    private String legalName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "industry")
    private String industry;

    @Column(name = "company_type")
    private String companyType;

    @Column(name = "established_date")
    @Temporal(TemporalType.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date establishedDate;

    @Column(name = "company_size")
    private String companySize;

    @Column(name = "financing_stage")
    private String financingStage;

    @Column(name = "listed_status")
    private Boolean listedStatus;

    @Column(name = "stock_code")
    private String stockCode;

    @Column(name = "detailed_address")
    private String detailedAddress;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "company_slogan")
    private String companySlogan;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "company_culture", columnDefinition = "TEXT")
    private String companyCulture;

    @Column(name = "product_description", columnDefinition = "TEXT")
    private String productDescription;

    @Column(name = "benefits", columnDefinition = "JSON")
    private String benefits;

    @Column(name = "work_time")
    private String workTime;

    @Column(name = "is_verified")
    private Boolean isVerified;

    @Column(name = "verification_status")
    private String verificationStatus;

    @Column(name = "business_license_url")
    private String businessLicenseUrl;

    @Column(name = "created_at", updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "cnt_jobs")
    private String cntJobs;

    // 构造函数
    public CompanyInfo() {
        this.isDeleted = false;
        this.isVerified = false;
    }
} 