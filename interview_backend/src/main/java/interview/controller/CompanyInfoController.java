package interview.controller;

import interview.dto.CompanyInfoDTO;
import interview.model.CompanyInfo;
import interview.service.CompanyInfoService;
import interview.util.UserContextUtil;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

@RestController
@RequestMapping("/api/companies")
@AllArgsConstructor
public class CompanyInfoController {
    private final CompanyInfoService companyInfoService;
    private final UserContextUtil userContextUtil;

    @GetMapping
    public Page<CompanyInfoDTO> getCompanies(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String industry,
        @RequestParam(required = false) String companySize,
        @RequestParam(required = false) String financing,
        Pageable pageable) {
        return companyInfoService.findCompanies(keyword, industry, companySize, financing, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyInfoDTO> getCompanyById(@PathVariable Long id) {
        return companyInfoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取当前面试官的公司信息
     */
    @GetMapping("/my-company")
    public ResponseEntity<CompanyInfoDTO> getMyCompany() {
        Long userId = userContextUtil.getCurrentUserId();
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        return companyInfoService.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建公司信息
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCompany(
            @RequestBody CompanyInfo companyInfo) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            CompanyInfo createdCompany = companyInfoService.createCompany(companyInfo, userId);
            response.put("success", true);
            response.put("message", "公司信息创建成功");
            response.put("data", createdCompany);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 更新公司信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCompany(
            @PathVariable Long id,
            @RequestBody CompanyInfo companyInfo) {
        Map<String, Object> response = new HashMap<>();
        
        System.out.println("DEBUG - updateCompany: contactPhone = " + companyInfo.getContactPhone());
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            CompanyInfo updatedCompany = companyInfoService.updateCompany(id, companyInfo, userId);
            response.put("success", true);
            response.put("message", "公司信息更新成功");
            response.put("data", updatedCompany);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("DEBUG - updateCompany异常: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 上传公司Logo
     */
    @PostMapping("/{id}/logo")
    public ResponseEntity<Map<String, Object>> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            String logoUrl = companyInfoService.uploadLogo(id, file, userId);
            response.put("success", true);
            response.put("message", "Logo上传成功");
            response.put("data", logoUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 上传营业执照
     */
    @PostMapping("/{id}/license")
    public ResponseEntity<Map<String, Object>> uploadBusinessLicense(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        Long userId = userContextUtil.getCurrentUserId();
        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            String licenseUrl = companyInfoService.uploadBusinessLicense(id, file, userId);
            response.put("success", true);
            response.put("message", "营业执照上传成功");
            response.put("data", licenseUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 