package interview.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP客户端工具类
 * 封装常用的HTTP请求方法
 */
public class HttpClientUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);
    
    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @return 响应内容
     */
    public static String doGet(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);
            logger.debug("发送GET请求: {}", url);
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                logger.debug("GET响应状态码: {}", response.getStatusLine().getStatusCode());
                return result;
            }
        } catch (IOException e) {
            logger.error("执行GET请求异常: {}", url, e);
            return null;
        }
    }
    
    /**
     * 发送POST请求，携带JSON数据
     * 
     * @param url 请求URL
     * @param json JSON字符串
     * @return 响应内容
     */
    public static String doPostJson(String url, String json) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            
            StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            
            logger.debug("发送POST请求: {}", url);
            logger.debug("请求体: {}", json);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                int statusCode = response.getStatusLine().getStatusCode();
                String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                
                logger.debug("POST响应状态码: {}", statusCode);
                logger.debug("POST响应体: {}", result);
                
                if (statusCode >= 200 && statusCode < 300) {
                    return result;
                } else {
                    logger.error("POST请求失败，状态码: {}, 响应: {}", statusCode, result);
                    return null;
                }
            }
        } catch (IOException e) {
            logger.error("执行POST请求异常: {}", url, e);
            return null;
        }
    }
    
    /**
     * 发送POST请求，携带表单数据
     * 
     * @param url 请求URL
     * @param params 表单参数
     * @return 响应内容
     */
    public static String doPostForm(String url, Map<String, String> params) {
        // 具体实现略，根据需要补充
        return "";
    }

    /**
     * 发送POST请求，携带JSON数据和自定义请求头
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param json JSON字符串
     * @return 响应内容
     */
    public static String doPostWithHeaders(String url, Map<String, String> headers, String json) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            
            // 设置请求头
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            
            StringEntity stringEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            
            logger.debug("发送POST请求: {}", url);
            logger.debug("请求头: {}", headers);
            logger.debug("请求体: {}", json);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity entity = response.getEntity();
                int statusCode = response.getStatusLine().getStatusCode();
                String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                
                logger.debug("POST响应状态码: {}", statusCode);
                logger.debug("POST响应体: {}", result);
                
                if (statusCode >= 200 && statusCode < 300) {
                    return result;
                } else {
                    logger.error("POST请求失败，状态码: {}, 响应: {}", statusCode, result);
                    return null;
                }
            }
        } catch (IOException e) {
            logger.error("执行POST请求异常: {}", url, e);
            return null;
        }
    }
} 