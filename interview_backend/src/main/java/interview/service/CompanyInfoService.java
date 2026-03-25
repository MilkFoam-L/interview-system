package interview.service;

import interview.dto.CompanyInfoDTO;
import interview.model.CompanyInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface CompanyInfoService {
    Page<CompanyInfoDTO> findCompanies(String keyword, String industry, String companySize, String financing, Pageable pageable);

    Optional<CompanyInfoDTO> findById(Long id);
    
    Optional<CompanyInfoDTO> findByUserId(Long userId);
    
    CompanyInfo createCompany(CompanyInfo companyInfo, Long userId);
    
    CompanyInfo updateCompany(Long id, CompanyInfo companyInfo, Long userId);
    
    String uploadLogo(Long companyId, MultipartFile file, Long userId);
    
    String uploadBusinessLicense(Long companyId, MultipartFile file, Long userId);
} 