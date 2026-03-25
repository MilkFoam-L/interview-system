package interview.controller;

import interview.service.SpeechTranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 语音转写控制器
 */
@RestController
@RequestMapping("/api/speech")
public class SpeechTranscriptionController {
    
    private static final Logger logger = LoggerFactory.getLogger(SpeechTranscriptionController.class);
    
    @Autowired
    private SpeechTranscriptionService transcriptionService;
    
    /**
     * 创建转写会话
     */
    @PostMapping("/session")
    @ResponseBody
    public Map<String, Object> createSession() {
        logger.info("收到创建转写会话请求");
        Map<String, Object> response = new HashMap<>();
        String sessionId = UUID.randomUUID().toString();
        response.put("sessionId", sessionId);
        response.put("success", true);
        logger.info("创建转写会话成功，会话ID: {}", sessionId);
        return response;
    }
} 