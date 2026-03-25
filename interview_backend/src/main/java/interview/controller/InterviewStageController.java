package interview.controller;

import interview.model.entity.InterviewStageProgress;
import interview.service.InterviewStageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/stage-progress")
public class InterviewStageController {
    @Autowired
    private InterviewStageService stageService;

    // 获取某个会话的所有阶段进度
    @GetMapping("/session/{sessionId}")
    public List<InterviewStageProgress> getStages(@PathVariable Long sessionId) {
        return stageService.getStagesBySessionId(sessionId);
    }

    // 更新某个阶段的状态
    @PutMapping("/session/{sessionId}/stage/{stage}/status")
    public InterviewStageProgress updateStageStatus(@PathVariable Long sessionId, @PathVariable String stage, @RequestParam String status) {
        return stageService.updateStageStatus(sessionId, stage, status);
    }

    // 开始某个阶段
    @PutMapping("/session/{sessionId}/stage/{stage}/start")
    public InterviewStageProgress startStage(@PathVariable Long sessionId, @PathVariable String stage) {
        return stageService.startStage(sessionId, stage);
    }

    // 完成某个阶段
    @PutMapping("/session/{sessionId}/stage/{stage}/complete")
    public InterviewStageProgress completeStage(@PathVariable Long sessionId, @PathVariable String stage) {
        return stageService.completeStage(sessionId, stage);
    }

    // 新建阶段进度（初始化）
    @PostMapping("")
    public InterviewStageProgress createStageProgress(@RequestBody InterviewStageProgress progress) {
        return stageService.createStageProgress(progress);
    }
} 