package interview.service;

import interview.model.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 服务：用户上传的简历文件管理
 */
public interface ResumeFileService {

    /**
     * 上传并保存简历文件
     *
     * @param userId 当前用户ID
     * @param file   上传的文件
     * @return 保存后的 Resume 实体
     */
    Resume uploadResume(Long userId, MultipartFile file);

    /**
     * 获取用户所有简历
     */
    List<Resume> listResumes(Long userId);

    /**
     * 根据ID获取简历
     */
    Resume getResume(Long resumeId);

    /**
     * 更新简历名称
     */
    Resume updateResumeName(Long resumeId, String name);

    /**
     * 删除简历
     */
    void deleteResume(Long resumeId);
} 