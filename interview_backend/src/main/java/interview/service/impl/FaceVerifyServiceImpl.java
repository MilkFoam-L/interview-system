package interview.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;
import interview.config.XunFeiConfig;
import interview.model.response.FaceVerifyResponse;
import interview.service.FaceVerifyService;
import interview.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * 人脸验证服务实现
 */
@Service
public class FaceVerifyServiceImpl implements FaceVerifyService {

    private static final Logger logger = LoggerFactory.getLogger(FaceVerifyServiceImpl.class);
    
    // 定义相似度阈值，大于此值认为是同一个人
    private static final double SIMILARITY_THRESHOLD = 0.8;
    
    @Autowired
    @Qualifier("xunFeiConfig")
    private XunFeiConfig xunfeiConfig;

    @Override
    public FaceVerifyResponse verifyFace(String faceImageBase64, String idCardImageBase64) {
        FaceVerifyResponse response = new FaceVerifyResponse();
        
        try {
            // 构建HTTP请求头
            Map<String, String> header = buildHttpHeader();
            
            // 构建请求体
            String requestBody = "face_image=" + URLEncoder.encode(faceImageBase64, "UTF-8") + 
                                "&watermark_image=" + URLEncoder.encode(idCardImageBase64, "UTF-8");
            
            // 发送请求 - 使用静态方法
            String result = HttpUtil.doPostStatic(xunfeiConfig.getWatermarkUrl(), header, requestBody);
            
            logger.info("人脸验证原始结果: {}", result);
            
            // 解析比对结果
            if (result != null) {
                JSONObject jsonResult = new JSONObject(result);
                String code = jsonResult.getString("code");
                
                if ("0".equals(code)) {
                    // 成功获取到比对结果
                    double score = jsonResult.getDouble("data");
                    
                    logger.info("人脸比对相似度得分: {}", score);
                    
                    // 根据相似度评判是否为同一个人
                    boolean isSamePerson = score >= SIMILARITY_THRESHOLD;
                    
                    response.setSuccess(true);
                    response.setSimilarity(score);
                    response.setSamePerson(isSamePerson);
                    response.setMessage(isSamePerson ? "人脸验证通过" : "人脸验证不通过");
                    
                } else {
                    // 处理错误情况
                    String desc = jsonResult.getString("desc");
                    logger.error("【比对失败】: 错误码: {}, 错误描述: {}", code, desc);
                    
                    response.setSuccess(false);
                    response.setMessage("人脸验证失败: " + desc);
                    response.setSimilarity(0);
                    response.setSamePerson(false);
                }
            } else {
                response.setSuccess(false);
                response.setMessage("调用人脸验证服务失败");
                response.setSimilarity(0);
                response.setSamePerson(false);
            }
        } catch (Exception e) {
            logger.error("人脸验证服务异常", e);
            response.setSuccess(false);
            response.setMessage("人脸验证服务异常: " + e.getMessage());
            response.setSimilarity(0);
            response.setSamePerson(false);
        }
        
        return response;
    }
    
    /**
     * 构建HTTP请求头
     */
    private Map<String, String> buildHttpHeader() throws UnsupportedEncodingException, JSONException {
        String curTime = System.currentTimeMillis() / 1000L + "";
        
        JSONObject param = new JSONObject();
        param.put("get_image", true);
        String params = param.toString();
        String paramBase64 = new String(Base64.encodeBase64(params.getBytes("UTF-8")));
        
        String checkSum = DigestUtils.md5Hex(xunfeiConfig.getApiKey() + curTime + paramBase64);
        
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/x-www-form-urlencoded; charset=utf-8");
        header.put("X-Param", paramBase64);
        header.put("X-CurTime", curTime);
        header.put("X-CheckSum", checkSum);
        header.put("X-Appid", xunfeiConfig.getAppId());
        
        return header;
    }
} 