package interview.service.impl;

import interview.model.FaceVerification;
import interview.model.response.FaceVerifyResponse;
import interview.repository.FaceVerificationRepository;
import interview.service.FaceVerificationService;
import interview.service.FaceVerifyService;
import interview.util.ImageFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 人脸验证服务实现
 */
@Service
public class FaceVerificationServiceImpl implements FaceVerificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceVerificationServiceImpl.class);
    
    @Autowired
    private FaceVerifyService faceVerifyService;
    
    @Autowired
    private FaceVerificationRepository faceVerificationRepository;
    
    @Autowired
    private ImageFileUtil imageFileUtil;
    
    @Override
    @Transactional
    public FaceVerifyResponse verifyAndSave(String name, String idNumber, String faceImageBase64, String idCardImageBase64) {
        // 使用默认用户ID
        Long userId = 1L;
        return verifyAndSave(name, idNumber, faceImageBase64, idCardImageBase64, userId);
    }
    
    @Override
    @Transactional
    public FaceVerifyResponse verifyAndSave(String name, String idNumber, String faceImageBase64, String idCardImageBase64, 
                                           Long userId) {
        logger.info("开始进行人脸验证，用户: {}, 身份证号: {}, userId: {}", 
                   name, idNumber, userId);
        
        // 保存图片到文件系统
        String faceImagePath = imageFileUtil.saveImage(faceImageBase64, idNumber, "face");
        String idCardImagePath = imageFileUtil.saveImage(idCardImageBase64, idNumber, "idcard");
        
        // 调用已有的人脸比对服务
        FaceVerifyResponse verifyResponse = faceVerifyService.verifyFace(faceImageBase64, idCardImageBase64);
        
        // 创建验证记录
        FaceVerification record = new FaceVerification();
        record.setName(name);
        record.setIdNumber(idNumber);
        record.setFaceImagePath(faceImagePath);
        record.setIdCardImagePath(idCardImagePath);
        record.setSimilarity(verifyResponse.getSimilarity());
        record.setIsSamePerson(verifyResponse.isSamePerson());
        record.setVerificationResult(verifyResponse.getMessage());
        record.setStatus(verifyResponse.isSuccess() ? 1 : 2); // 1成功，2失败
        record.setApiResponse(verifyResponse.toString());
        record.setUserId(userId);
        
        // 保存记录到数据库
        faceVerificationRepository.save(record);
        logger.info("人脸验证完成，结果: {}, 相似度: {}", verifyResponse.isSamePerson() ? "通过" : "不通过", verifyResponse.getSimilarity());
        
        return verifyResponse;
    }
    
    @Override
    public FaceVerification findById(Long id) {
        return faceVerificationRepository.findById(id).orElse(null);
    }
    
    @Override
    public FaceVerification findLatestByIdNumber(String idNumber) {
        List<FaceVerification> records = faceVerificationRepository.findTop5ByIdNumberOrderByCreateTimeDesc(idNumber);
        return records.isEmpty() ? null : records.get(0);
    }
} 