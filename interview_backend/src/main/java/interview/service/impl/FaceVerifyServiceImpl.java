package interview.service.impl;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import interview.config.FaceDetectConfig;
import interview.model.response.FaceVerifyResponse;
import interview.service.FaceVerifyService;
import interview.util.HttpClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 人脸验证服务实现 - 使用讯飞新版API (api.xf-yun.com)
 */
@Service
public class FaceVerifyServiceImpl implements FaceVerifyService {

    private static final Logger logger = LoggerFactory.getLogger(FaceVerifyServiceImpl.class);

    private static final double SIMILARITY_THRESHOLD = 0.8;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private FaceDetectConfig faceDetectConfig;

    @Override
    public FaceVerifyResponse verifyFace(String faceImageBase64, String idCardImageBase64) {
        FaceVerifyResponse response = new FaceVerifyResponse();

        try {
            // 构建 HMAC-SHA256 认证 URL
            String authUrl = buildAuthUrl();

            // 构建请求 JSON
            String requestJson = buildRequestJson(faceImageBase64, idCardImageBase64);

            // 设置请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");

            // 发送请求
            String result = HttpClientUtils.doPostWithHeaders(authUrl, headers, requestJson);

            logger.info("人脸比对原始结果: {}", result);

            if (result == null) {
                response.setSuccess(false);
                response.setMessage("调用人脸比对服务返回空响应");
                response.setSimilarity(0);
                response.setSamePerson(false);
                return response;
            }

            // 解析响应
            JsonNode root = objectMapper.readTree(result);
            JsonNode header = root.path("header");
            int code = header.path("code").asInt(-1);

            if (code != 0) {
                String message = header.path("message").asText("未知错误");
                logger.error("人脸比对失败，错误码: {}, 消息: {}", code, message);
                response.setSuccess(false);
                response.setMessage("人脸比对失败: " + message);
                response.setSimilarity(0);
                response.setSamePerson(false);
                return response;
            }

            // 解析比对结果（Base64 编码的 JSON）
            String base64Text = root.path("payload")
                    .path("face_compare_result")
                    .path("text").asText();

            if (base64Text == null || base64Text.isEmpty()) {
                logger.error("人脸比对响应中缺少结果数据");
                response.setSuccess(false);
                response.setMessage("人脸比对响应中缺少结果数据");
                response.setSimilarity(0);
                response.setSamePerson(false);
                return response;
            }

            String resultJson = new String(Base64.getDecoder().decode(base64Text), StandardCharsets.UTF_8);
            logger.info("人脸比对解码结果: {}", resultJson);

            JsonNode resultNode = objectMapper.readTree(resultJson);
            double score = resultNode.path("score").asDouble(0);

            boolean isSamePerson = score >= SIMILARITY_THRESHOLD;

            response.setSuccess(true);
            response.setSimilarity(score);
            response.setSamePerson(isSamePerson);
            response.setMessage(isSamePerson ? "人脸验证通过" : "人脸验证不通过");

        } catch (Exception e) {
            logger.error("人脸比对服务异常", e);
            response.setSuccess(false);
            response.setMessage("人脸比对服务异常: " + e.getMessage());
            response.setSimilarity(0);
            response.setSamePerson(false);
        }

        return response;
    }

    /**
     * 构建请求 JSON（新版 API 格式）
     */
    private String buildRequestJson(String faceImage, String idCardImage) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("  \"header\": {");
        json.append("    \"app_id\": \"").append(faceDetectConfig.getAppId()).append("\",");
        json.append("    \"status\": 3");
        json.append("  },");
        json.append("  \"parameter\": {");
        json.append("    \"").append(faceDetectConfig.getServiceId()).append("\": {");
        json.append("      \"service_kind\": \"face_compare\",");
        json.append("      \"face_compare_result\": {");
        json.append("        \"encoding\": \"utf8\",");
        json.append("        \"format\": \"json\",");
        json.append("        \"compress\": \"raw\"");
        json.append("      }");
        json.append("    }");
        json.append("  },");
        json.append("  \"payload\": {");
        json.append("    \"input1\": {");
        json.append("      \"encoding\": \"jpg\",");
        json.append("      \"image\": \"").append(faceImage).append("\"");
        json.append("    },");
        json.append("    \"input2\": {");
        json.append("      \"encoding\": \"jpg\",");
        json.append("      \"image\": \"").append(idCardImage).append("\"");
        json.append("    }");
        json.append("  }");
        json.append("}");

        return json.toString();
    }

    /**
     * 构建 HMAC-SHA256 认证 URL（与 FaceDetectServiceImpl 相同逻辑）
     */
    private String buildAuthUrl() {
        try {
            URL url = new URL(faceDetectConfig.getRequestUrl());

            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());

            String host = url.getHost();
            if (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443) {
                host = host + ":" + url.getPort();
            }

            StringBuilder builder = new StringBuilder();
            builder.append("host: ").append(host).append("\n");
            builder.append("date: ").append(date).append("\n");
            builder.append("POST ").append(url.getPath()).append(" HTTP/1.1");

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(
                    faceDetectConfig.getApiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);

            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hexDigits);

            String authorization = String.format(
                    "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    faceDetectConfig.getApiKey(), "hmac-sha256", "host date request-line", signature);

            String authBase64 = Base64.getEncoder().encodeToString(
                    authorization.getBytes(StandardCharsets.UTF_8));

            return String.format("%s?authorization=%s&host=%s&date=%s",
                    faceDetectConfig.getRequestUrl(),
                    URLEncoder.encode(authBase64, "UTF-8"),
                    URLEncoder.encode(host, "UTF-8"),
                    URLEncoder.encode(date, "UTF-8"));
        } catch (Exception e) {
            logger.error("构建认证URL异常", e);
            throw new RuntimeException("构建讯飞API认证URL失败", e);
        }
    }
}
