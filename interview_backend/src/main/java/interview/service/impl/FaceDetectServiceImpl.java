package interview.service.impl;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import interview.config.FaceDetectConfig;
import interview.service.FaceDetectService;
import interview.util.HttpClientUtils;

import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 面部检测服务实现类，调用讯飞API
 */
@Service
public class FaceDetectServiceImpl implements FaceDetectService {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceDetectServiceImpl.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private FaceDetectConfig faceDetectConfig;
    
    // 本地图片存储目录
    private static final String IMAGE_STORAGE_DIR = "D:\\interview_images\\face_images2";
    
    @Override
    public int detectExpression(String imageData) {
        try {
            logger.info("接收到表情分析请求，图像数据长度: {}", 
                    imageData != null ? imageData.length() : 0);
            
            // 保存图片到本地临时文件
            File tempDir = new File(IMAGE_STORAGE_DIR);
            if (!tempDir.exists()) {
                boolean created = tempDir.mkdirs();
                logger.debug("创建临时目录: {}, 结果: {}", tempDir.getAbsolutePath(), created);
            }
            
            File tempFile = File.createTempFile("face_", ".jpg", tempDir);
            byte[] imageBytes = Base64.getDecoder().decode(imageData);
            Files.write(tempFile.toPath(), imageBytes);
            
            logger.debug("临时图片保存至: {}", tempFile.getAbsolutePath());
            
            // 调用讯飞API进行表情检测
            logger.info("开始调用讯飞API分析表情");
            int expressionCode = callXunfeiApi(imageData);
            
            logger.info("讯飞API检测到表情代码: {}, 表情: {}", 
                    expressionCode, getExpressionName(expressionCode));
            
            return expressionCode;
            
        } catch (Exception e) {
            logger.error("检测表情异常", e);
            // 出错时返回正常表情
            return 6;
        }
    }
    
    @Override
    public String getExpressionName(int expressionCode) {
        switch (expressionCode) {
            case 0: return "惊讶";
            case 1: return "害怕";
            case 2: return "厌恶";
            case 3: return "高兴";
            case 4: return "悲伤";
            case 5: return "生气";
            case 6: return "正常";
            default: return "未知";
        }
    }
    
    /**
     * 调用讯飞API分析表情
     */
    private int callXunfeiApi(String imageBase64) {
        try {
            // 构建认证URL
            String authUrl = buildAuthUrl();
            logger.debug("构建认证URL: {}", authUrl);
            
            // 构建请求参数
            String requestJson = buildRequestJson(imageBase64);
            
            // 设置请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            
            // 发送HTTP请求
            logger.debug("发送请求到讯飞API...");
            String response = HttpClientUtils.doPostWithHeaders(authUrl, headers, requestJson);
            
            // 检查响应是否为空
            if (response == null) {
                logger.error("调用讯飞API返回空响应");
                return 6; // 默认返回正常表情
            }
            
            // 解析响应
            logger.debug("讯飞API原始响应: {}", response);
            JsonNode root = objectMapper.readTree(response);
            JsonNode header = root.path("header");
            
            // 检查响应码
            int code = header.path("code").asInt(-1);
            if (code != 0) {
                logger.error("讯飞API返回错误码: {}, 消息: {}", 
                        code, header.path("message").asText("未知错误"));
                return 6; // 默认返回正常表情
            }
            
            // 获取Base64编码的结果文本
            JsonNode faceDetectResult = root.path("payload").path("face_detect_result");
            if (faceDetectResult.isMissingNode()) {
                logger.error("讯飞API响应中缺少face_detect_result字段");
                return 6;
            }
            
            String base64Text = faceDetectResult.path("text").asText();
            if (base64Text == null || base64Text.isEmpty()) {
                logger.error("讯飞API响应中text字段为空");
                return 6;
            }
            
            // 解码结果
            String resultJson = new String(Base64.getDecoder().decode(base64Text), StandardCharsets.UTF_8);
            logger.debug("解码后的结果JSON: {}", resultJson);
            
            // 解析表情代码
            JsonNode resultNode = objectMapper.readTree(resultJson);
            int faceNum = resultNode.path("face_num").asInt(0);
            
            if (faceNum > 0) {
                JsonNode faceNode = resultNode.path("face_1");
                if (!faceNode.isMissingNode() && faceNode.has("property")) {
                    int expression = faceNode.path("property").path("expression").asInt(6);
                    logger.info("成功解析到表情代码: {}", expression);
                    return expression;
                } else {
                    logger.warn("未找到人脸属性信息");
                }
            } else {
                logger.warn("未检测到人脸, face_num = {}", faceNum);
            }
            
            // 未检测到表情时返回正常
            return 6;
            
        } catch (Exception e) {
            logger.error("调用讯飞API或解析结果异常", e);
            return 6;
        }
    }
    
    /**
     * 构建请求JSON
     */
    private String buildRequestJson(String imageBase64) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("  \"header\": {");
        json.append("    \"app_id\": \"").append(faceDetectConfig.getAppId()).append("\",");
        json.append("    \"status\": 3");
        json.append("  },");
        json.append("  \"parameter\": {");
        json.append("    \"").append(faceDetectConfig.getServiceId()).append("\": {");
        json.append("      \"service_kind\": \"face_detect\",");
        json.append("      \"detect_property\": \"1\",");
        json.append("      \"face_detect_result\": {");
        json.append("        \"encoding\": \"utf8\",");
        json.append("        \"format\": \"json\",");
        json.append("        \"compress\": \"raw\"");
        json.append("      }");
        json.append("    }");
        json.append("  },");
        json.append("  \"payload\": {");
        json.append("    \"input1\": {");
        json.append("      \"encoding\": \"jpg\",");
        json.append("      \"image\": \"").append(imageBase64).append("\"");
        json.append("    }");
        json.append("  }");
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * 构建认证URL
     */
    private String buildAuthUrl() {
        try {
            URL url = new URL(faceDetectConfig.getRequestUrl());
            
            // 获取当前日期并格式化
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = format.format(new Date());
            
            // 获取主机
            String host = url.getHost();
            if (url.getPort() != -1 && url.getPort() != 80 && url.getPort() != 443) {
                host = host + ":" + url.getPort();
            }
            
            // 构建签名字符串
            StringBuilder builder = new StringBuilder();
            builder.append("host: ").append(host).append("\n");
            builder.append("date: ").append(date).append("\n");
            builder.append("POST ").append(url.getPath()).append(" HTTP/1.1");
            
            // 计算HMAC-SHA256签名
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(faceDetectConfig.getApiSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            
            byte[] hexDigits = mac.doFinal(builder.toString().getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hexDigits);
            
            // 构建授权字符串
            String authorization = String.format(
                    "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                    faceDetectConfig.getApiKey(), "hmac-sha256", "host date request-line", signature);
            
            // Base64编码授权字符串
            String authBase64 = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
            
            // 构建最终URL
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