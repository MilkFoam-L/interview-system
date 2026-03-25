package interview.ai;

import interview.model.ScenarioModel;
import interview.config.XunfeiApiConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SparkClient {
    
    private final XunfeiApiConfig xunfeiApiConfig;
    
    @Autowired
    public SparkClient(XunfeiApiConfig xunfeiApiConfig) {
        this.xunfeiApiConfig = xunfeiApiConfig;
    }
    
    /**
     * 向讯飞 Spark 大模型发送 prompt 并返回完整回答（阻塞直到结束）。
     */
    public String ask(String prompt) throws Exception {
        // 设置 ScenarioModel 全局参数
        ScenarioModel.NewQuestion = prompt;
        ScenarioModel.totalFlag   = false;
        ScenarioModel.totalAnswer = "";
        
        // 使用配置获取相关信息
        String hostUrl = xunfeiApiConfig.getSpark().getHostUrl();
        String apiKey = xunfeiApiConfig.getSpark().getApiKey();
        String apiSecret = xunfeiApiConfig.getSpark().getApiSecret();

        String authUrl = ScenarioModel.getAuthUrl(hostUrl, apiKey, apiSecret);

        OkHttpClient client = new OkHttpClient.Builder().build();
        String url = authUrl.replace("http://", "ws://")
                            .replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();

        // userId 仅用于区分用途，这里设为 auto
        WebSocket webSocket = client.newWebSocket(request,
                new ScenarioModel("auto", false));

        // 等待 totalFlag 变为 true；ScenarioModel.onMessage 在对话结束时会把 totalFlag 设回 true
        while (!ScenarioModel.totalFlag) {
            Thread.sleep(200);
        }
        // 关闭连接，释放资源
        webSocket.close(1000, "done");
        return ScenarioModel.totalAnswer;
    }
    
    /**
     * 提供一个静态方法，便于调用
     */
    public static String askStatic(String prompt) throws Exception {
        if (ScenarioModel.instance == null) {
            throw new IllegalStateException("ScenarioModel实例未初始化，请确保Spring容器已启动");
        }
        
        // 使用ScenarioModel的实例获取配置
        String hostUrl = "https://spark-api.xf-yun.com/v4.0/chat"; // 更新为v4.0
        String apiKey = ScenarioModel.instance.getSparkApiKey();
        String apiSecret = ScenarioModel.instance.getSparkApiSecret();
        
        // 设置 ScenarioModel 全局参数
        ScenarioModel.NewQuestion = prompt;
        ScenarioModel.totalFlag   = false;
        ScenarioModel.totalAnswer = "";

        String authUrl = ScenarioModel.getAuthUrl(hostUrl, apiKey, apiSecret);

        OkHttpClient client = new OkHttpClient.Builder().build();
        String url = authUrl.replace("http://", "ws://")
                            .replace("https://", "wss://");
        Request request = new Request.Builder().url(url).build();

        // userId 仅用于区分用途，这里设为 auto
        WebSocket webSocket = client.newWebSocket(request,
                new ScenarioModel("auto", false));

        // 等待 totalFlag 变为 true；ScenarioModel.onMessage 在对话结束时会把 totalFlag 设回 true
        while (!ScenarioModel.totalFlag) {
            Thread.sleep(200);
        }
        // 关闭连接，释放资源
        webSocket.close(1000, "done");
        return ScenarioModel.totalAnswer;
    }
} 