package interview.service;

import interview.model.response.FaceVerifyResponse;

/**
 * 人脸验证服务接口
 */
public interface FaceVerifyService {
    /**
     * 验证两张人脸照片是否为同一个人
     * 
     * @param faceImageBase64 实时拍摄的人脸照片的Base64编码
     * @param idCardImageBase64 身份证照片的Base64编码
     * @return 验证结果
     */
    FaceVerifyResponse verifyFace(String faceImageBase64, String idCardImageBase64);
} 