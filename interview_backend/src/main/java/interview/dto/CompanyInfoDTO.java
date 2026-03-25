package interview.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class CompanyInfoDTO {
    private Long id;
    private String name;
    private String legalName;
    private String industry;
    private String type;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date establishedDate;
    private String size;
    private String financing;
    private Boolean listedStatus;
    private String stockCode;
    private String location;
    private String email;
    private String phone;
    private String websiteUrl;
    private String logoUrl;
    private String slogan;
    private String description;
    private String culture;
    private String productDescription;
    private Map<String, String> benefits;
    private String workTime;
    private List<String> welfare;
    private int jobCount;
} 