package interview.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 人脸验证记录实体类
 */
@Entity
@Table(name = "face_verification")
public class FaceVerification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户ID，关联users表
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * 用户姓名
     */
    private String name;
    
    /**
     * 身份证号码
     */
    @Column(name = "id_number")
    private String idNumber;
    
    /**
     * 身份证照片存储路径
     */
    @Column(name = "id_card_image_path")
    private String idCardImagePath;
    
    /**
     * 人脸照片存储路径
     */
    @Column(name = "face_image_path")
    private String faceImagePath;
    
    /**
     * 相似度得分
     */
    private Double similarity;
    
    /**
     * 是否为同一人(0-否,1-是)
     */
    @Column(name = "is_same_person")
    private Boolean isSamePerson;
    
    /**
     * 验证时间
     */
    @Column(name = "verification_time")
    private LocalDateTime verificationTime;
    
    /**
     * 验证结果描述
     */
    @Column(name = "verification_result")
    private String verificationResult;
    
    /**
     * 状态(0-处理中,1-成功,2-失败)
     */
    private Integer status;
    
    /**
     * API原始响应
     */
    @Column(name = "api_response")
    private String apiResponse;
    
    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    /**
     * 验证前置处理
     */
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        verificationTime = LocalDateTime.now();
        status = 0; // 初始状态为处理中
    }
    
    /**
     * 更新前置处理
     */
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getIdCardImagePath() {
        return idCardImagePath;
    }

    public void setIdCardImagePath(String idCardImagePath) {
        this.idCardImagePath = idCardImagePath;
    }

    public String getFaceImagePath() {
        return faceImagePath;
    }

    public void setFaceImagePath(String faceImagePath) {
        this.faceImagePath = faceImagePath;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public Boolean getIsSamePerson() {
        return isSamePerson;
    }

    public void setIsSamePerson(Boolean isSamePerson) {
        this.isSamePerson = isSamePerson;
    }

    public LocalDateTime getVerificationTime() {
        return verificationTime;
    }

    public void setVerificationTime(LocalDateTime verificationTime) {
        this.verificationTime = verificationTime;
    }

    public String getVerificationResult() {
        return verificationResult;
    }

    public void setVerificationResult(String verificationResult) {
        this.verificationResult = verificationResult;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getApiResponse() {
        return apiResponse;
    }

    public void setApiResponse(String apiResponse) {
        this.apiResponse = apiResponse;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
} 