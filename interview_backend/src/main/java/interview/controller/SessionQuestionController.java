package interview.controller;

import interview.model.entity.SessionQuestion;
import interview.model.dto.QuestionDetailDTO;
import interview.service.SessionQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/session-questions")
public class SessionQuestionController {
    @Autowired
    private SessionQuestionService sessionQuestionService;

    // 分配题目
    @PostMapping("/assign")
    public Map<String, Object> assignQuestions(@RequestBody Map<String, Object> req) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 记录请求参数
            System.out.println("收到分配题目请求: " + req);
            
            // 确保必要的参数不为null
            if (req.get("sessionId") == null) {
                response.put("success", false);
                response.put("message", "sessionId不能为空");
                response.put("data", new ArrayList<>());
                return response;
            }
            if (req.get("stage") == null) {
                response.put("success", false);
                response.put("message", "stage不能为空");
                response.put("data", new ArrayList<>());
                return response;
            }
            
            Long sessionId = Long.valueOf(req.get("sessionId").toString());
            String stage = req.get("stage").toString();
            int count = req.get("count") != null ? Integer.parseInt(req.get("count").toString()) : 5;
            
            System.out.println("分配题目参数 - sessionId: " + sessionId + ", stage: " + stage + ", count: " + count);
            
            List<SessionQuestion> questions = sessionQuestionService.assignQuestions(sessionId, stage, count);
            response.put("success", true);
            response.put("message", "题目分配成功");
            response.put("data", questions);
            return response;
        } catch (IllegalArgumentException e) {
            // 记录错误并返回错误响应
            System.err.println("参数错误: " + e.getMessage());
            response.put("success", false);
            response.put("message", "参数错误: " + e.getMessage());
            response.put("data", new ArrayList<>());
            return response;
        } catch (RuntimeException e) {
            // 特定的业务逻辑错误，给出明确提示
            System.err.println("分配题目时发生错误: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("data", new ArrayList<>());
            return response;
        } catch (Exception e) {
            // 其他未知错误，记录日志并返回错误响应
            System.err.println("分配题目时发生未知错误: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "分配题目失败: " + e.getMessage());
            response.put("data", new ArrayList<>());
            return response;
        }
    }

    // 获取题目（联表查详情）
    @GetMapping("")
    public List<QuestionDetailDTO> getSessionQuestions(@RequestParam(required = false) Long sessionId, 
                                                      @RequestParam(required = false) String stage) {
        try {
            if (sessionId == null) {
                throw new IllegalArgumentException("sessionId不能为空");
            }
            if (stage == null || stage.trim().isEmpty()) {
                throw new IllegalArgumentException("stage不能为空");
            }
            
            return sessionQuestionService.getSessionQuestionsWithDetail(sessionId, stage);
        } catch (Exception e) {
            // 记录错误并返回空列表，避免500错误
            System.err.println("获取题目时发生错误: " + e.getMessage());
            return new ArrayList<>();
        }
    }
} 