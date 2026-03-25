package interview.service;

import interview.model.FaceVerification;
import interview.model.response.FaceVerifyResponse;

/**
 * 人脸验证服务接口
 */
public interface FaceVerificationService {
    
    /**
     * 进行人脸验证并保存记录
     *
     * @param name 用户姓名
     * @param idNumber 身份证号
     * @param faceImageBase64 人脸图片Base64
     * @param idCardImageBase64 身份证图片Base64
     * @return 验证结果
     */
    FaceVerifyResponse verifyAndSave(String name, String idNumber, String faceImageBase64, String idCardImageBase64);
    
    /**
     * 进行人脸验证并保存记录（带用户ID）
     *
     * @param name 用户姓名
     * @param idNumber 身份证号
     * @param faceImageBase64 人脸图片Base64
     * @param idCardImageBase64 身份证图片Base64
     * @param userId 用户ID
     * @return 验证结果
     */
    FaceVerifyResponse verifyAndSave(String name, String idNumber, String faceImageBase64, String idCardImageBase64, 
                                    Long userId);
    
    /**
     * 根据ID查询验证记录
     *
     * @param id 记录ID
     * @return 验证记录
     */
    FaceVerification findById(Long id);
    
    /**
     * 根据身份证号查询最近验证记录
     *
     * @param idNumber 身份证号
     * @return 验证记录列表
     */
    FaceVerification findLatestByIdNumber(String idNumber);
} 