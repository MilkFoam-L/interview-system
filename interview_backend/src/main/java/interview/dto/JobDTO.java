package interview.dto;

import interview.model.CompanyInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JobDTO {
    private Integer id;
    private String title;
    private String location;
    private String salary;
    private String requirements;
    private String experience;
    private String education;
    private String category;
    private String categoryType;
    private Integer workType;
    private String duties;
    private String description;
    private Integer status;
    private String applications;
    private CompanyDTO company;

    @Data
    @NoArgsConstructor
    public static class CompanyDTO {
        private Long id;
        private String name;
        private String slogan;
        private String logoUrl;

        public CompanyDTO(CompanyInfo companyInfo) {
            if (companyInfo != null) {
                this.id = companyInfo.getId();
                this.name = companyInfo.getCompanyName();
                this.slogan = companyInfo.getCompanySlogan();
                this.logoUrl = companyInfo.getLogoUrl();
            }
        }
    }

    public JobDTO(interview.model.Job job) {
        this.id = job.getId();
        this.title = job.getTitle();
        this.location = job.getLocation();
        this.salary = job.getSalary();
        this.requirements = job.getRequirements();
        this.experience = job.getExperience();
        this.education = job.getEducation();
        this.category = job.getCategory();
        this.categoryType = job.getCategoryType();
        this.workType = job.getWorkType();
        this.duties = job.getDuties();
        this.description = job.getDescription();
        this.status = job.getStatus();
        this.applications = job.getApplications();
        if (job.getCompany() != null) {
            this.company = new CompanyDTO(job.getCompany());
        }
    }
} 