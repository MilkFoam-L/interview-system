package interview.service.impl;

import com.alibaba.fastjson.JSONObject;
import interview.config.XunFeiConfig;
import interview.model.Resume;
import interview.repository.ResumeRepository;
import interview.service.ResumeGenerationService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ResumeGenerationServiceImpl implements ResumeGenerationService {

    @Autowired
    private XunFeiConfig xunFeiConfig;

    @Autowired
    private ResumeRepository resumeRepository;

    private static final String HOST_URL = "https://cn-huadong-1.xf-yun.com/v1/private/s73f4add9";
    private static final OkHttpClient CLIENT = new OkHttpClient().newBuilder()
            .connectionPool(new ConnectionPool(100, 5, TimeUnit.MINUTES))
            .readTimeout(60 * 10, TimeUnit.SECONDS)
            .build();
    
    private static final Gson gson = new Gson();

    // 响应类
    private static class ResponseData {
        Header header;
        Payload payload;
    }

    private static class Header {
        int code;
        String sid;
        String message;
    }

    private static class Payload {
        ResData resData;
    }

    private static class ResData {
        String encoding;
        String compress;
        String format;
        String text;
    }

    @Override
    public String generateResume(String prompt) {
        try {
            // 解析请求数据
            JSONObject data = JSONObject.parseObject(prompt);
            
            // 构建提示词
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("请帮我生成一份专业的简历，包含以下信息：\n");
            promptBuilder.append("姓名：").append(data.getString("name")).append("\n");
            promptBuilder.append("年龄：").append(data.getInteger("age")).append("\n");
            promptBuilder.append("联系方式：").append(data.getString("phone")).append("\n");
            promptBuilder.append("邮箱：").append(data.getString("email")).append("\n\n");

            // 添加头像信息（如果有）
            String avatarUrl = data.getString("avatar");
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                promptBuilder.append("头像：").append(avatarUrl).append("\n\n");
            }

            // 教育经历
            promptBuilder.append("教育经历：\n");
            data.getJSONArray("education").forEach(edu -> {
                JSONObject eduObj = (JSONObject) edu;
                promptBuilder.append(eduObj.getString("school"))
                        .append(" | ").append(eduObj.getString("major"))
                        .append(" | ").append(eduObj.getString("degree"))
                        .append("\n")
                        .append(eduObj.getJSONArray("timeRange").getString(0))
                        .append(" - ")
                        .append(eduObj.getJSONArray("timeRange").getString(1))
                        .append("\n\n");
            });

            // 工作经历
            promptBuilder.append("工作经历：\n");
            data.getJSONArray("workExperience").forEach(work -> {
                JSONObject workObj = (JSONObject) work;
                promptBuilder.append(workObj.getString("company"))
                        .append(" | ").append(workObj.getString("position"))
                        .append("\n")
                        .append(workObj.getJSONArray("timeRange").getString(0))
                        .append(" - ")
                        .append(workObj.getJSONArray("timeRange").getString(1))
                        .append("\n")
                        .append("工作内容：").append(workObj.getString("description"))
                        .append("\n\n");
            });

            // 技能特长
            promptBuilder.append("技能特长：\n")
                    .append(data.getString("skills"))
                    .append("\n\n");

            // 自我评价
            promptBuilder.append("自我评价：\n")
                    .append(data.getString("selfEvaluation"));

            // 调用讯飞API生成简历（这里需要实现具体的API调用逻辑）
            // TODO: 实现讯飞API调用
            String generatedContent = callXunFeiAPI(promptBuilder.toString());
            
            // 如果有头像，在生成的内容中添加头像标签
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                generatedContent = "<div class='resume-header'><img src='" + avatarUrl + 
                    "' class='resume-avatar' alt='头像'/></div>" + generatedContent;
            }

            return generatedContent;
        } catch (Exception e) {
            throw new RuntimeException("生成简历失败：" + e.getMessage());
        }
    }

    @Override
    public void saveResume(String originalData, String generatedContent) {
        Resume resume = new Resume();
        resume.setOriginalData(originalData);
        // 将生成内容直接存入 content 列，避免找不到 generatedContent 列
        resume.setContent(generatedContent);
        resumeRepository.save(resume);
    }

    private String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间戳
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "POST " + url.getPath() + " HTTP/1.1";
        
        // SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);
        
        // 拼接authorization
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", sha);
        
        // 拼接url
        return hostUrl + "?" +
                "authorization=" + Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8)) +
                "&date=" + date +
                "&host=" + url.getHost();
    }

    private String callXunFeiAPI(String prompt) {
        try {
            String appId = xunFeiConfig.getResume().getAppId();
            String apiKey = xunFeiConfig.getResume().getApiKey();
            String apiSecret = xunFeiConfig.getResume().getApiSecret();
            String hostUrl = xunFeiConfig.getResume().getUrl();
            
            // TODO: 实现讯飞API调用逻辑
            // 这里需要根据实际API规范实现
            System.out.println("使用讯飞API配置: appId=" + appId + ", apiKey=" + apiKey);
            
            // 这里是示例返回，实际需要调用讯飞API
            return "<div class='resume-content'>" + prompt + "</div>";
        } catch (Exception e) {
            System.err.println("调用讯飞API失败: " + e.getMessage());
            e.printStackTrace();
            return "<div class='resume-content'>简历生成失败，请稍后再试。</div>";
        }
    }
} 