package interview.controller;

import interview.model.InterviewQuestionAnswer;
import interview.model.ScenarioReport;
import interview.repository.ScenarioReportRepository;
import interview.service.ScenarioQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场景化问答阶段（Phase 0）：仅生成并返回问题。
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/scenario")
public class ScenarioController {

    private final ScenarioQuestionService questionService;
    private final ScenarioReportRepository reportRepository;

    @Autowired
    public ScenarioController(ScenarioQuestionService questionService, ScenarioReportRepository reportRepository) {
        this.questionService = questionService;
        this.reportRepository = reportRepository;
    }

    /**
     * 启动场景化阶段，生成 5 个问题。
     */
    @PostMapping("/start")
    public ResponseEntity<List<InterviewQuestionAnswer>> start(@PathVariable Long sessionId) {
        List<InterviewQuestionAnswer> list = questionService.startScenario(sessionId);
        return ResponseEntity.ok(list);
    }

    /**
     * 获取所有问题（前端可根据 roundNo 分轮展示）。
     */
    @GetMapping
    public ResponseEntity<List<InterviewQuestionAnswer>> list(@PathVariable Long sessionId) {
        return ResponseEntity.ok(questionService.listQuestions(sessionId));
    }

    /**
     * 获取综合报告（完成后）。
     */
    @GetMapping("/report")
    public ResponseEntity<ScenarioReport> report(@PathVariable Long sessionId) {
        return reportRepository.findById(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 提交回答（Phase-0 仅保存，不评分）。
     */
    @PostMapping("/answer")
    public ResponseEntity<InterviewQuestionAnswer> answer(@PathVariable Long sessionId,
                                                           @RequestParam Integer roundNo,
                                                           @RequestBody String answer) {
        return ResponseEntity.ok(questionService.submitAnswer(sessionId, roundNo, answer));
    }
    
    /**
     * 生成追问，并与原问题合并。
     * 如果回答质量不足或不适合追问，则返回原问题或null。
     */
    @PostMapping("/follow-up")
    public ResponseEntity<InterviewQuestionAnswer> generateFollowUp(@PathVariable Long sessionId,
                                                           @RequestParam Integer roundNo) {
        InterviewQuestionAnswer result = questionService.generateFollowUpQuestion(sessionId, roundNo);
        // 如果不需要追问（返回null），则返回原问题
        if (result == null) {
            // 找到原问题
            List<InterviewQuestionAnswer> list = questionService.listQuestions(sessionId);
            for (InterviewQuestionAnswer qa : list) {
                if (qa.getRoundNo().equals(roundNo)) {
                    return ResponseEntity.ok(qa);
                }
            }
            // 如果找不到原问题，返回404
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }
    
    /**
     * 检查并完成场景阶段
     */
    @PostMapping("/complete")
    public ResponseEntity<?> completeStage(@PathVariable Long sessionId) {
        questionService.checkAndCompleteStage(sessionId);
        return ResponseEntity.ok().build();
    }
} 