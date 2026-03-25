package interview.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import interview.dto.CompanyInfoDTO;
import interview.model.CompanyInfo;
import interview.repository.CompanyInfoRepository;
import interview.service.CompanyInfoService;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
@AllArgsConstructor
public class CompanyInfoServiceImpl implements CompanyInfoService {

    private final CompanyInfoRepository companyInfoRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Page<CompanyInfoDTO> findCompanies(String keyword, String industry, String companySize, String financing, Pageable pageable) {
        Specification<CompanyInfo> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.or(
                    cb.like(root.get("companyName"), "%" + keyword + "%"),
                    cb.like(root.get("industry"), "%" + keyword + "%"),
                    cb.like(root.get("companyDescription"), "%" + keyword + "%")
                ));
            }
            if (StringUtils.hasText(industry)) {
                predicates.add(cb.equal(root.get("industry"), industry));
            }
            if (StringUtils.hasText(companySize)) {
                predicates.add(cb.equal(root.get("companySize"), companySize));
            }
            if (StringUtils.hasText(financing)) {
                predicates.add(cb.equal(root.get("financingStage"), financing));
            }
            predicates.add(cb.equal(root.get("isDeleted"), false));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return companyInfoRepository.findAll(spec, pageable).map(this::convertToDTO);
    }

    @Override
    public Optional<CompanyInfoDTO> findById(Long id) {
        return companyInfoRepository.findById(id)
                .filter(companyInfo -> !companyInfo.getIsDeleted())
                .map(this::convertToDTO);
    }

    private CompanyInfoDTO convertToDTO(CompanyInfo company) {
        CompanyInfoDTO dto = new CompanyInfoDTO();
        dto.setId(company.getId());
        dto.setName(company.getCompanyName());
        dto.setLegalName(company.getLegalName());
        dto.setIndustry(company.getIndustry());
        dto.setType(company.getCompanyType());
        dto.setEstablishedDate(company.getEstablishedDate());
        dto.setSize(company.getCompanySize());
        dto.setFinancing(company.getFinancingStage());
        dto.setListedStatus(company.getListedStatus());
        dto.setStockCode(company.getStockCode());
        dto.setLocation(company.getDetailedAddress());
        dto.setEmail(company.getContactEmail());
        dto.setPhone(company.getContactPhone());
        dto.setWebsiteUrl(company.getWebsiteUrl());
        dto.setLogoUrl(company.getLogoUrl());
        dto.setSlogan(company.getCompanySlogan());
        dto.setDescription(company.getCompanyDescription());
        dto.setCulture(company.getCompanyCulture());
        dto.setProductDescription(company.getProductDescription());
        dto.setWorkTime(company.getWorkTime());

        if (StringUtils.hasText(company.getBenefits())) {
            try {
                TypeReference<Map<String, String>> typeRef = new TypeReference<>() {};
                Map<String, String> benefitsMap = objectMapper.readValue(company.getBenefits(), typeRef);
                dto.setBenefits(benefitsMap);
                dto.setWelfare(new ArrayList<>(benefitsMap.values()));
            } catch (IOException e) {
                dto.setBenefits(null);
                dto.setWelfare(new ArrayList<>());
            }
        } else {
             dto.setWelfare(new ArrayList<>());
        }
        try {
            dto.setJobCount(Integer.parseInt(company.getCntJobs()));
        } catch (NumberFormatException e) {
            dto.setJobCount(0);
        }

        return dto;
    }

    @Override
    public Optional<CompanyInfoDTO> findByUserId(Long userId) {
        Optional<CompanyInfo> companyInfo = companyInfoRepository.findByUserIdAndIsDeletedFalse(userId);
        return companyInfo.map(this::convertToDTO);
    }

    @Override
    public CompanyInfo createCompany(CompanyInfo companyInfo, Long userId) {
        // 检查该用户是否已经有公司信息
        Optional<CompanyInfo> existingCompany = companyInfoRepository.findByUserIdAndIsDeletedFalse(userId);
        if (existingCompany.isPresent()) {
            throw new RuntimeException("该用户已经关联了公司信息");
        }
        
        // 设置用户ID和默认值
        companyInfo.setUserId(userId);
        companyInfo.setIsDeleted(false);
        companyInfo.setIsVerified(false);
        companyInfo.setCreatedAt(new Date());
        companyInfo.setUpdatedAt(new Date());
        
        return companyInfoRepository.save(companyInfo);
    }

    @Override
    public CompanyInfo updateCompany(Long id, CompanyInfo companyInfo, Long userId) {
        // 查找现有公司信息
        Optional<CompanyInfo> existingCompany = companyInfoRepository.findById(id);
        if (!existingCompany.isPresent()) {
            throw new RuntimeException("公司信息不存在");
        }
        
        CompanyInfo existing = existingCompany.get();
        
        // 验证权限：只有关联的用户才能修改
        if (!Objects.equals(existing.getUserId(), userId)) {
            throw new RuntimeException("无权限修改此公司信息");
        }
        
        // 更新字段
        existing.setCompanyName(companyInfo.getCompanyName());
        existing.setLegalName(companyInfo.getLegalName());
        existing.setRegistrationNumber(companyInfo.getRegistrationNumber());
        existing.setIndustry(companyInfo.getIndustry());
        existing.setCompanyType(companyInfo.getCompanyType());
        existing.setEstablishedDate(companyInfo.getEstablishedDate());
        existing.setCompanySize(companyInfo.getCompanySize());
        existing.setFinancingStage(companyInfo.getFinancingStage());
        existing.setListedStatus(companyInfo.getListedStatus());
        existing.setStockCode(companyInfo.getStockCode());
        existing.setDetailedAddress(companyInfo.getDetailedAddress());
        existing.setContactEmail(companyInfo.getContactEmail());
        existing.setContactPhone(companyInfo.getContactPhone());
        existing.setWebsiteUrl(companyInfo.getWebsiteUrl());
        existing.setCompanySlogan(companyInfo.getCompanySlogan());
        existing.setCompanyDescription(companyInfo.getCompanyDescription());
        existing.setCompanyCulture(companyInfo.getCompanyCulture());
        existing.setProductDescription(companyInfo.getProductDescription());
        existing.setWorkTime(companyInfo.getWorkTime());
        existing.setBenefits(companyInfo.getBenefits());
        existing.setUpdatedAt(new Date());
        
        return companyInfoRepository.save(existing);
    }

    @Override
    public String uploadLogo(Long companyId, MultipartFile file, Long userId) {
        // 验证公司信息和权限
        CompanyInfo company = validateCompanyAccess(companyId, userId);
        
        try {
            // 创建上传目录
            String uploadDir = "interview_backend/uploads/logos/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = "company_" + companyId + "_logo_" + System.currentTimeMillis() + fileExtension;
            
            // 保存文件
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 生成访问URL
            String logoUrl = "/api/companies/logos/" + fileName;
            
            // 更新数据库
            company.setLogoUrl(logoUrl);
            company.setUpdatedAt(new Date());
            companyInfoRepository.save(company);
            
            return logoUrl;
            
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadBusinessLicense(Long companyId, MultipartFile file, Long userId) {
        // 验证公司信息和权限
        CompanyInfo company = validateCompanyAccess(companyId, userId);
        
        try {
            // 创建上传目录
            String uploadDir = "interview_backend/uploads/licenses/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = "company_" + companyId + "_license_" + System.currentTimeMillis() + fileExtension;
            
            // 保存文件
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // 生成访问URL
            String licenseUrl = "/api/companies/licenses/" + fileName;
            
            // 更新数据库
            company.setBusinessLicenseUrl(licenseUrl);
            company.setUpdatedAt(new Date());
            companyInfoRepository.save(company);
            
            return licenseUrl;
            
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证公司访问权限
     */
    private CompanyInfo validateCompanyAccess(Long companyId, Long userId) {
        Optional<CompanyInfo> companyOpt = companyInfoRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            throw new RuntimeException("公司信息不存在");
        }
        
        CompanyInfo company = companyOpt.get();
        if (!Objects.equals(company.getUserId(), userId)) {
            throw new RuntimeException("无权限访问此公司信息");
        }
        
        return company;
    }
} 