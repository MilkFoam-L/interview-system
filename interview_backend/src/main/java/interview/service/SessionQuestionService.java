package interview.service;

import interview.model.entity.SessionQuestion;
import interview.model.dto.QuestionDetailDTO;
import java.util.List;

public interface SessionQuestionService {
    List<SessionQuestion> assignQuestions(Long sessionId, String stage, int count);
    List<SessionQuestion> getSessionQuestions(Long sessionId, String stage);
    List<QuestionDetailDTO> getSessionQuestionsWithDetail(Long sessionId, String stage);
} 