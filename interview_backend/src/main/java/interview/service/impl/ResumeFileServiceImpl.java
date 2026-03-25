package interview.service.impl;

import interview.model.Resume;
import interview.repository.ResumeRepository;
import interview.service.ResumeFileService;
import interview.util.ResumeParserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeFileServiceImpl implements ResumeFileService {

    private final ResumeRepository resumeRepository;
    private final interview.repository.JobApplicationRepository jobApplicationRepository;

    @Override
    @Transactional
    public Resume uploadResume(Long userId, MultipartFile file) {
        try {
            // 添加详细日志
            System.out.println("ResumeFileService.uploadResume - 开始处理简历上传");
            System.out.println("userId: " + userId);
            System.out.println("文件名: " + file.getOriginalFilename());
            System.out.println("文件大小: " + file.getSize());
            System.out.println("文件类型: " + file.getContentType());
            
            // 验证参数
            if (userId == null) {
                throw new IllegalArgumentException("用户ID不能为空");
            }
            
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("文件不能为空");
            }
            
            if (file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {
                throw new IllegalArgumentException("文件名不能为空");
            }
            
            Resume resume = new Resume();
            resume.setUserId(userId);
            resume.setName(file.getOriginalFilename());
            
            // 提取文件扩展名作为类型
            String original = file.getOriginalFilename();
            String type = "UNKNOWN";
            if (original != null && original.contains(".")) {
                type = original.substring(original.lastIndexOf('.') + 1).toUpperCase();
            }
            resume.setType(type);
            
            // 解析简历内容
            String parsedText = null;
            String summary = null;
            resume.setParseStatus(1); // 解析中
            try {
                parsedText = ResumeParserUtil.extractText(file);
                summary = ResumeParserUtil.generateSummary(parsedText);
                resume.setContent(parsedText);
                resume.setSummary(summary);
                resume.setParseStatus(2); // 成功
            } catch (Exception ex) {
                System.err.println("解析简历失败: " + ex.getMessage());
                resume.setParseStatus(3); // 失败
            }
            
            System.out.println("准备读取文件内容...");
            resume.setFileData(file.getBytes());
            System.out.println("文件内容读取成功，准备保存到数据库...");
            
            Resume savedResume = resumeRepository.save(resume);
            System.out.println("简历保存成功，ID: " + savedResume.getId());
            
            return savedResume;
        } catch (IOException e) {
            System.err.println("读取文件内容失败: " + e.getMessage());
            throw new RuntimeException("读取文件内容失败: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("保存简历失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("保存简历失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Resume> listResumes(Long userId) {
        return resumeRepository.findByUserIdOrderByUpdateTimeDesc(userId);
    }

    @Override
    public Resume getResume(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("简历不存在"));
    }

    @Override
    @Transactional
    public Resume updateResumeName(Long resumeId, String name) {
        Resume resume = getResume(resumeId);
        resume.setName(name);
        return resumeRepository.save(resume);
    }

    @Override
    @Transactional
    public void deleteResume(Long resumeId) {
        // 检查是否存在引用
        if (!jobApplicationRepository.findByResumeId(resumeId).isEmpty()) {
            throw new RuntimeException("该简历已被用于职位投递，无法删除");
        }
        Resume resume = getResume(resumeId);
        resumeRepository.delete(resume);
    }
} 