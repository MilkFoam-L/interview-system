package interview.service;

import interview.model.entity.InterviewStageProgress;
import java.util.List;

public interface InterviewStageService {
    List<InterviewStageProgress> getStagesBySessionId(Long sessionId);
    InterviewStageProgress updateStageStatus(Long sessionId, String stage, String status);
    InterviewStageProgress startStage(Long sessionId, String stage);
    InterviewStageProgress completeStage(Long sessionId, String stage);
    InterviewStageProgress createStageProgress(InterviewStageProgress progress);
} 