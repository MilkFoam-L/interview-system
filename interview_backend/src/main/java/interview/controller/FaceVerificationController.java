package interview.controller;

import interview.model.FaceVerification;
import interview.model.request.FaceVerificationRequest;
import interview.model.response.FaceVerifyResponse;
import interview.service.FaceVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 人脸验证控制器
 */
@RestController
@RequestMapping("/api/face")
@Tag(name = "人脸验证", description = "人脸验证相关接口")
public class FaceVerificationController {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceVerificationController.class);
    
    @Autowired
    private FaceVerificationService faceVerificationService;
    
    /**
     * 人脸验证接口
     */
    @PostMapping("/verify")
    @Operation(summary = "人脸验证", description = "将人脸照片与身份证照片进行比对验证")
    public ResponseEntity<FaceVerifyResponse> verify(@RequestBody FaceVerificationRequest request) {
        logger.info("收到人脸验证请求，姓名: {}, 身份证号: {}", request.getName(), request.getIdNumber());
        
        // 获取当前登录用户ID（从请求参数获取，如果没有则默认为1）
        Long userId = request.getUserId() != null ? request.getUserId() : 1L;
        
        logger.info("人脸验证使用的用户ID: {}", userId);
        
        // 调用服务进行验证
        FaceVerifyResponse response = faceVerificationService.verifyAndSave(
            request.getName(),
            request.getIdNumber(),
            request.getFaceImage(),
            request.getIdCardImage(),
            userId
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取验证记录
     */
    @GetMapping("/verification/{id}")
    @Operation(summary = "获取验证记录", description = "根据ID获取验证记录详情")
    public ResponseEntity<FaceVerification> getVerification(@PathVariable Long id) {
        FaceVerification verification = faceVerificationService.findById(id);
        if (verification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(verification);
    }
    
    /**
     * 获取最近验证记录
     */
    @GetMapping("/verification/latest/{idNumber}")
    @Operation(summary = "获取最近验证记录", description = "根据身份证号获取最近的验证记录")
    public ResponseEntity<FaceVerification> getLatestVerification(@PathVariable String idNumber) {
        FaceVerification verification = faceVerificationService.findLatestByIdNumber(idNumber);
        if (verification == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(verification);
    }
} 