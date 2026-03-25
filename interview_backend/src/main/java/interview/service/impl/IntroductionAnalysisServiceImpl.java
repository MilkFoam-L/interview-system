package interview.service.impl;

import interview.ai.spark.SparkX1IntroductionAnalyzer;
import interview.dto.IntroductionAnalysisRequest;
import interview.dto.IntroductionAnalysisResponse;
import interview.model.IntroductionAnalysis;
import interview.repository.IntroductionAnalysisRepository;
import interview.service.IntroductionAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 自我介绍分析服务实现
 */
@Service
@Transactional
public class IntroductionAnalysisServiceImpl implements IntroductionAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(IntroductionAnalysisServiceImpl.class);
    
    @Autowired
    private IntroductionAnalysisRepository repository;
    
    @Autowired
    private SparkX1IntroductionAnalyzer analyzer;
    
    @Override
    public IntroductionAnalysisResponse analyzeAndSave(IntroductionAnalysisRequest request) {
        try {
            logger.info("开始分析自我介绍，会话ID：{}，文本长度：{}", 
                       request.getSessionId(), 
                       request.getIntroductionText() != null ? request.getIntroductionText().length() : 0);
            
            // 参数验证
            if (request.getSessionId() == null) {
                return IntroductionAnalysisResponse.failure("会话ID不能为空");
            }
            
            if (request.getIntroductionText() == null || request.getIntroductionText().trim().isEmpty()) {
                return IntroductionAnalysisResponse.failure("自我介绍内容不能为空");
            }
            
            // 检查是否已存在分析记录
            Optional<IntroductionAnalysis> existingAnalysis = repository.findBySessionId(request.getSessionId());
            
            // 调用AI分析
            SparkX1IntroductionAnalyzer.IntroductionAnalysisResult analysisResult = 
                analyzer.analyzeIntroduction(request.getIntroductionText(), 
                                           request.getDuration() != null ? request.getDuration() : 60);
            
            if (!analysisResult.isSuccess()) {
                return IntroductionAnalysisResponse.failure("AI分析失败：" + analysisResult.getAnalysisResult());
            }
            
            // 创建或更新分析记录
            IntroductionAnalysis analysis;
            if (existingAnalysis.isPresent()) {
                analysis = existingAnalysis.get();
                logger.info("更新现有分析记录，会话ID：{}", request.getSessionId());
            } else {
                analysis = new IntroductionAnalysis();
                analysis.setSessionId(request.getSessionId());
                logger.info("创建新分析记录，会话ID：{}", request.getSessionId());
            }
            
            // 设置分析数据
            analysis.setIntroductionText(request.getIntroductionText());
            analysis.setDuration(request.getDuration());
            analysis.setFluencyScore(analysisResult.getFluencyScore());
            analysis.setExpressionScore(analysisResult.getExpressionScore());
            analysis.setContentScore(analysisResult.getContentScore());
            analysis.setOverallScore(analysisResult.getOverallScore());
            analysis.setAnalysisResult(analysisResult.getAnalysisResult());
            
            // 保存到数据库
            analysis = repository.save(analysis);
            
            logger.info("自我介绍分析完成并保存，ID：{}，整体得分：{}", 
                       analysis.getId(), analysis.getOverallScore());
            
            // 构建成功响应
            IntroductionAnalysisResponse response = IntroductionAnalysisResponse.success(analysis.getId(), analysis.getSessionId());
            response.setIntroductionText(analysis.getIntroductionText());
            response.setDuration(analysis.getDuration());
            response.setFluencyScore(analysis.getFluencyScore());
            response.setExpressionScore(analysis.getExpressionScore());
            response.setContentScore(analysis.getContentScore());
            response.setOverallScore(analysis.getOverallScore());
            response.setAnalysisResult(analysis.getAnalysisResult());
            
            return response;
            
        } catch (Exception e) {
            logger.error("自我介绍分析服务异常", e);
            return IntroductionAnalysisResponse.failure("分析服务异常：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public IntroductionAnalysis getBySessionId(Long sessionId) {
        try {
            logger.info("🔍 查询数据库中的自我介绍分析记录，会话ID：{}", sessionId);
            
            Optional<IntroductionAnalysis> result = repository.findBySessionId(sessionId);
            
            if (result.isPresent()) {
                IntroductionAnalysis analysis = result.get();
                logger.info("✅ 数据库查询成功 - ID: {}, SessionId: {}, 创建时间: {}", 
                           analysis.getId(), 
                           analysis.getSessionId(), 
                           analysis.getCreateTime());
                logger.info("✅ 分数数据 - fluency: {}, expression: {}, content: {}, overall: {}", 
                           analysis.getFluencyScore(), 
                           analysis.getExpressionScore(),
                           analysis.getContentScore(), 
                           analysis.getOverallScore());
                logger.info("✅ 文本数据 - 介绍文本长度: {}, 分析结果长度: {}", 
                           analysis.getIntroductionText() != null ? analysis.getIntroductionText().length() : 0,
                           analysis.getAnalysisResult() != null ? analysis.getAnalysisResult().length() : 0);
                return analysis;
            } else {
                logger.warn("❌ 数据库中未找到会话ID为 {} 的记录", sessionId);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ 数据库查询异常，会话ID：{}", sessionId, e);
            return null;
        }
    }
    
    @Override
    public boolean isServiceAvailable() {
        try {
            return analyzer.isAvailable();
        } catch (Exception e) {
            logger.error("检查自我介绍分析服务可用性失败", e);
            return false;
        }
    }
    
    @Override
    public IntroductionAnalysisResponse reanalyze(Long sessionId) {
        try {
            logger.info("重新分析自我介绍，会话ID：{}", sessionId);
            
            // 查找现有记录
            Optional<IntroductionAnalysis> existingAnalysis = repository.findBySessionId(sessionId);
            if (!existingAnalysis.isPresent()) {
                return IntroductionAnalysisResponse.failure("未找到指定会话的自我介绍记录");
            }
            
            IntroductionAnalysis analysis = existingAnalysis.get();
            
            // 构建重新分析请求
            IntroductionAnalysisRequest request = new IntroductionAnalysisRequest();
            request.setSessionId(sessionId);
            request.setIntroductionText(analysis.getIntroductionText());
            request.setDuration(analysis.getDuration());
            
            // 执行重新分析
            return analyzeAndSave(request);
            
        } catch (Exception e) {
            logger.error("重新分析自我介绍失败，会话ID：{}", sessionId, e);
            return IntroductionAnalysisResponse.failure("重新分析失败：" + e.getMessage());
        }
    }
}