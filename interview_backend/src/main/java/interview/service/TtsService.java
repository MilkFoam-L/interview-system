package interview.service;

import com.google.gson.Gson;
import okhttp3.HttpUrl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import interview.config.XunfeiApiConfig;

/**
 * TTS 服务：将文本合成为 MP3 字节数组，供前端下载/播放。
 */
@Service
public class TtsService {

    private final XunfeiApiConfig xunfeiApiConfig;
    
    private static final String AUE = "lame";
    private static final String TTE = "UTF8";
    private static final String VCN = "x4_yezi";

    private static final Gson GSON = new Gson();
    
    @Autowired
    public TtsService(XunfeiApiConfig xunfeiApiConfig) {
        this.xunfeiApiConfig = xunfeiApiConfig;
    }

    /**
     * 阻塞式合成，返回 MP3 数据；若失败返回空数组。
     */
    public byte[] synth(String text) {
        if (text == null || text.trim().isEmpty()) return new byte[0];
        try {
            String hostUrl = xunfeiApiConfig.getTts().getHostUrl();
            String apiKey = xunfeiApiConfig.getTts().getApiKey();
            String apiSecret = xunfeiApiConfig.getTts().getApiSecret();
            String wsUrl = getAuthUrl(hostUrl, apiKey, apiSecret).replace("https://", "wss://");
            return websocketSynth(wsUrl, text);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /* -------------------- internal -------------------- */
    private byte[] websocketSynth(String wsUrl, String text) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CountDownLatch latch = new CountDownLatch(1);
        URI uri = new URI(wsUrl);
        WebSocketClient client = new WebSocketClient(uri) {
            @Override public void onOpen(ServerHandshake handshake) {
                send(buildJson(text));
            }
            @Override public void onMessage(String message) {
                JsonParse parse = GSON.fromJson(message, JsonParse.class);
                if (parse.code != 0) {
                    latch.countDown();
                    close();
                    return;
                }
                if (parse.data != null) {
                    try {
                        byte[] audio = Base64.getDecoder().decode(parse.data.audio);
                        out.write(audio);
                    } catch (Exception ignored) {}
                    if (parse.data.status == 2) {
                        latch.countDown();
                        close();
                    }
                }
            }
            @Override public void onClose(int code, String reason, boolean remote) { latch.countDown(); }
            @Override public void onError(Exception ex) { ex.printStackTrace(); latch.countDown(); }
        };
        client.connect();
        latch.await();
        return out.toByteArray();
    }

    private String buildJson(String text) {
        String base64 = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
        return "{\n" +
                "  \"common\": { \"app_id\": \"" + xunfeiApiConfig.getTts().getAppId() + "\" },\n" +
                "  \"business\": { \"aue\": \"" + AUE + "\", \"tte\": \"" + TTE + "\", \"ent\": \"intp65\", \"vcn\": \"" + VCN + "\", \"pitch\": 50, \"speed\": 50 },\n" +
                "  \"data\": { \"status\": 2, \"text\": \"" + base64 + "\" }\n" +
                "}";
    }

    /* -------------------- auth -------------------- */
    private String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = fmt.format(new Date());
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";
        Mac mac = Mac.getInstance("hmacsha256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256"));
        String sha = Base64.getEncoder().encodeToString(mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8)));
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build();
        return httpUrl.toString();
    }

    /* -------------------- json classes -------------------- */
    private static class JsonParse { int code; String sid; Data data; }
    private static class Data { int status; String audio; }
} 