package interview.service.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import interview.dto.FaceExpressionRequest;
import interview.dto.FaceExpressionResponse;
import interview.dto.FaceExpressionSummaryResponse;
import interview.model.FaceExpressionRecord;
import interview.model.FaceExpressionSummary;
import interview.repository.FaceExpressionRecordRepository;
import interview.repository.FaceExpressionSummaryRepository;
import interview.service.FaceDetectService;
import interview.service.FaceExpressionService;

@Service
public class FaceExpressionServiceImpl implements FaceExpressionService {
    
    private static final Logger logger = LoggerFactory.getLogger(FaceExpressionServiceImpl.class);
    
    // 本地图片存储目录
    private static final String IMAGE_STORAGE_DIR = "D:\\interview_images\\face_images2";
    
    @Autowired
    private FaceExpressionRecordRepository recordRepository;
    
    @Autowired
    private FaceExpressionSummaryRepository summaryRepository;
    
    @Autowired
    private FaceDetectService faceDetectService;

    @Override
    public FaceExpressionResponse analyzeExpression(FaceExpressionRequest request) {
        try {
            logger.info("开始分析表情，用户ID: {}, 面试会话ID: {}, 捕获ID: {}", 
                request.getUserId(), request.getSessionId(), request.getCapture_id());
            
            // 确保存储目录存在
            File sessionDir = new File(IMAGE_STORAGE_DIR, String.valueOf(request.getSessionId()));
            File userDir = new File(sessionDir, String.valueOf(request.getUserId()));
            
            if (!userDir.exists()) {
                userDir.mkdirs();
            }
            
            // 保存图片
            String imageFileName = UUID.randomUUID().toString() + ".jpg";
            File imageFile = new File(userDir, imageFileName);
            String imagePath = imageFile.getAbsolutePath();
            
            byte[] imageBytes = Base64.getDecoder().decode(request.getImageBase64());
            Files.write(imageFile.toPath(), imageBytes);
            
            logger.debug("图片已保存至: {}", imagePath);
            
            // 调用人脸检测API，获取表情代码
            logger.info("开始调用讯飞人脸检测API");
            int expressionCode = faceDetectService.detectExpression(request.getImageBase64());
            logger.info("讯飞API返回表情代码: {}, 表情: {}", expressionCode, faceDetectService.getExpressionName(expressionCode));
            
            // 创建表情记录
            FaceExpressionRecord record = new FaceExpressionRecord();
            record.setUserId(request.getUserId());
            record.setSessionId(request.getSessionId());
            record.setImagePath(imagePath);
            record.setExpressionCode(expressionCode);
            record.setExpressionName(faceDetectService.getExpressionName(expressionCode));
            
            // 设置新增字段
            record.setCapture_id(request.getCapture_id());
            
            // 添加设置candidateName和candidateIdNumber字段
            record.setCandidateName(request.getCandidateName());
            record.setCandidateIdNumber(request.getCandidateIdNumber());
            
            // 添加设置remainingTime字段
            record.setRemainingTime(request.getRemainingTime());
            
            // 处理时间戳
            if (request.getTimestamp() != null) {
                record.setTimestamp(request.getTimestamp());
            } else {
                record.setTimestamp(System.currentTimeMillis());
            }
            
            // 处理捕获时间
            if (request.getCapture_time() != null) {
                try {
                    // 将ISO格式时间字符串转换为Date对象
                    record.setCaptureTime(java.util.Date.from(
                        java.time.Instant.parse(request.getCapture_time())
                    ));
                } catch (Exception e) {
                    logger.warn("无法解析捕获时间: {}, 使用当前时间", request.getCapture_time());
                    record.setCaptureTime(new Date());
                }
            }
            
            // 设置计数
            if (request.getCount() != null) {
                record.setCount(request.getCount());
            } else {
                record.setCount(1);
            }
            
            // 保存记录
            logger.debug("保存表情记录到数据库");
            FaceExpressionRecord savedRecord = recordRepository.save(record);
            logger.info("表情记录已保存，ID: {}, 捕获ID: {}, 候选人姓名: {}, 剩余时间: {}", 
                savedRecord.getId(), savedRecord.getCapture_id(), 
                savedRecord.getCandidateName(), savedRecord.getRemainingTime());
            
            // 更新汇总数据
            logger.debug("开始更新表情汇总数据");
            FaceExpressionSummary summary = updateExpressionSummary(request.getUserId(), request.getSessionId(), expressionCode);
            
            // 如果有捕获ID，更新摘要的捕获ID
            if (request.getCapture_id() != null && summary != null) {
                summary.setCapture_id(request.getCapture_id());
                // 同时更新摘要中的candidateName和candidateIdNumber
                summary.setCandidateName(request.getCandidateName());
                summary.setCandidateIdNumber(request.getCandidateIdNumber());
                summaryRepository.save(summary);
            }
            
            // 返回结果
            FaceExpressionResponse response = new FaceExpressionResponse();
            response.setId(savedRecord.getId());
            response.setExpressionCode(expressionCode);
            response.setExpressionName(faceDetectService.getExpressionName(expressionCode));
            response.setImagePath(imagePath);
            response.setSuccess(true);
            response.setMessage("表情分析成功");
            
            // 设置捕获ID
            if (savedRecord.getCapture_id() != null) {
                response.setCaptureId(savedRecord.getCapture_id());
            } else if (request.getCapture_id() != null) {
                response.setCaptureId(request.getCapture_id());
            }
            
            return response;
        } catch (Exception e) {
            logger.error("分析表情时发生异常", e);
            throw new RuntimeException("分析表情失败: " + e.getMessage(), e);
        }
    }

    @Override
    public FaceExpressionSummaryResponse getExpressionSummary(Long sessionId) {
        try {
            logger.info("获取表情分析摘要，会话ID: {}", sessionId);
            
            Optional<FaceExpressionSummary> summaryOpt = summaryRepository.findBySessionId(sessionId);
            
            if (summaryOpt.isPresent()) {
                FaceExpressionSummary summary = summaryOpt.get();
                
                // 构建表情分布图
                Map<String, Integer> distribution = new HashMap<>();
                distribution.put("惊讶", summary.getSurprisedCount());
                distribution.put("害怕", summary.getFearCount());
                distribution.put("厌恶", summary.getDisgustCount());
                distribution.put("高兴", summary.getHappyCount());
                distribution.put("悲伤", summary.getSadCount());
                distribution.put("生气", summary.getAngryCount());
                distribution.put("正常", summary.getNeutralCount());
                
                // 获取样本图片路径
                List<String> sampleImagePaths = getSessionSampleImagePaths(sessionId);
                
                return FaceExpressionSummaryResponse.success(
                        summary.getDominantExpression(), 
                        summary.getTotalSamples(), 
                        distribution, 
                        sampleImagePaths);
            } else {
                logger.warn("未找到会话ID: {}的表情摘要", sessionId);
                return FaceExpressionSummaryResponse.failure("未找到表情摘要");
            }
        } catch (Exception e) {
            logger.error("获取表情分析摘要异常", e);
            return FaceExpressionSummaryResponse.failure("获取表情分析摘要异常: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public FaceExpressionRecord saveExpressionRecord(FaceExpressionRecord record) {
        if (record.getCreateTime() == null) {
            record.setCreateTime(new Date());
        }
        if (record.getUpdateTime() == null) {
            record.setUpdateTime(new Date());
        }
        return recordRepository.save(record);
    }

    @Override
    @Transactional
    public FaceExpressionSummary updateExpressionSummary(Long userId, Long sessionId, int expressionCode) {
        
        // 查找现有摘要或创建新摘要
        FaceExpressionSummary summary = summaryRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    FaceExpressionSummary newSummary = new FaceExpressionSummary();
                    newSummary.setSessionId(sessionId);
                    newSummary.setUserId(userId);
                    newSummary.setTotalSamples(0);
                    return newSummary;
                });
        
        // 更新表情计数
        summary.incrementExpressionCount(expressionCode);
        
        // 保存更新后的摘要
        return summaryRepository.save(summary);
    }

    @Override
    public List<String> getSessionSampleImagePaths(Long sessionId) {
        return recordRepository.findImagePathsBySessionId(sessionId);
    }
} 