package interview.service;

import interview.model.entity.AnswerRecord;
import interview.model.dto.AnswerRecordDTO;

import java.util.List;

public interface AnswerRecordService {
    AnswerRecord saveOrUpdateAnswer(AnswerRecordDTO dto);
    
    List<AnswerRecord> getAnswerRecordsByUserId(Long userId);
    
    List<AnswerRecord> getAnswerRecordsByQuestionId(Long questionId);
    
    List<AnswerRecord> getAnswerRecordsByInterviewId(Long interviewId);
    
    List<AnswerRecord> getAnswerRecordsByUserIdAndInterviewId(Long userId, Long interviewId);
    
    AnswerRecord getAnswerRecordById(Long id);
    
    AnswerRecord getByUserIdAndQuestionId(Long userId, Long questionId);
    
    AnswerRecord getByUserIdAndQuestionIdAndInterviewId(Long userId, Long questionId, Long interviewId);
    
    void deleteAnswerRecord(Long id);
} 