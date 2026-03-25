package interview.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP工具类 - 使用Java 17 HttpClient
 */
@Component
public class HttpUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpUtil.class);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    /**
     * 发送POST请求
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param body 请求体
     * @return 响应结果
     */
    public String doPost(String url, Map<String, String> headers, String body) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            
            // 设置请求头
            headers.forEach(requestBuilder::header);
            
            HttpRequest request = requestBuilder.build();
            logger.debug("发送POST请求: {}", url);
            logger.debug("请求体: {}", body);
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.error("HTTP请求失败，状态码: {}", response.statusCode());
                return null;
            }
            
            return response.body();
        } catch (Exception e) {
            logger.error("HTTP请求异常: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 发送POST请求，支持不同类型的响应
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param body 请求体
     * @return 响应结果，可能是字符串或字节数组
     */
    public Object doPostWithDifferentResponse(String url, Map<String, String> headers, String body) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            
            // 设置请求头
            headers.forEach(requestBuilder::header);
            
            HttpRequest request = requestBuilder.build();
            
            // 先获取字符串响应
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.error("HTTP请求失败，状态码: {}", response.statusCode());
                return null;
            }
            
            // 检查Content-Type
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if ("audio/mpeg".equals(contentType)) {
                // 如果是音频类型，重新发送请求并获取字节响应
                HttpResponse<byte[]> byteResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("Content-Type", "audio/mpeg");
                resultMap.put("sid", byteResponse.headers().firstValue("sid").orElse(""));
                resultMap.put("body", byteResponse.body());
                return resultMap;
            } else {
                // 其他类型直接返回字符串响应
                return response.body();
            }
        } catch (Exception e) {
            logger.error("HTTP请求异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 发送POST请求（兼容旧版接口）
     * 静态版本，与原utils包下的HttpUtil兼容
     * 
     * @param urlStr 请求地址
     * @param headers 请求头
     * @param body 请求体
     * @return 响应结果
     */
    public static String doPostStatic(String urlStr, Map<String, String> headers, String body) {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            
            // 设置请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            
            // 发送请求体
            if (body != null && !body.isEmpty()) {
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body.getBytes("UTF-8"));
                    out.flush();
                }
            }
            
            // 获取响应
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("HTTP请求异常: {}", e.getMessage(), e);
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                logger.error("关闭资源失败", e);
            }
            if (conn != null) conn.disconnect();
        }
    }
} 