package interview.controller;

import interview.model.Resume;
import interview.service.ResumeFileService;
import interview.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历文件上传/下载/管理
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resumes")
public class ResumesController {

    private final ResumeFileService resumeFileService;
    private final UserContextUtil userContextUtil;

    /**
     * 上传简历文件
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadResume(@RequestParam("file") MultipartFile file) {
        try {
            Long userId = userContextUtil.getCurrentUserId();
            log.info("用户ID={}上传简历: {}", userId, file.getOriginalFilename());
            
            // 验证用户是否已登录
            if (userId == null) {
                log.warn("上传简历失败: 用户未登录");
                Map<String, Object> errorRes = new HashMap<>();
                errorRes.put("error", "未登录或会话已过期，请重新登录");
                errorRes.put("success", false);
                return ResponseEntity.status(401).body(errorRes);
            }
            
            // 验证文件是否为空
            if (file == null || file.isEmpty()) {
                log.warn("上传简历失败: 文件为空, userId={}", userId);
                Map<String, Object> errorRes = new HashMap<>();
                errorRes.put("error", "请选择要上传的简历文件");
                errorRes.put("success", false);
                return ResponseEntity.badRequest().body(errorRes);
            }
            
            // 验证文件大小 (限制为10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                log.warn("上传简历失败: 文件过大, userId={}, fileSize={}", userId, file.getSize());
                Map<String, Object> errorRes = new HashMap<>();
                errorRes.put("error", "文件大小不能超过10MB");
                errorRes.put("success", false);
                return ResponseEntity.badRequest().body(errorRes);
            }
            
            Resume resume = resumeFileService.uploadResume(userId, file);
            log.info("简历上传成功: userId={}, resumeId={}, fileName={}", userId, resume.getId(), resume.getName());
            
            Map<String, Object> res = new HashMap<>();
            res.put("id", resume.getId());
            res.put("name", resume.getName());
            res.put("success", true);
            res.put("message", "简历上传成功");
            return ResponseEntity.ok(res);
            
        } catch (Exception e) {
            log.error("上传简历失败: {}", e.getMessage(), e);
            Map<String, Object> errorRes = new HashMap<>();
            errorRes.put("error", "上传失败: " + e.getMessage());
            errorRes.put("success", false);
            return ResponseEntity.status(500).body(errorRes);
        }
    }

    /**
     * 获取当前用户简历列表
     */
    @GetMapping
    public ResponseEntity<List<Resume>> listResumes() {
        Long userId = userContextUtil.getCurrentUserId();
        return ResponseEntity.ok(resumeFileService.listResumes(userId));
    }

    /**
     * 下载简历
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadResume(@PathVariable("id") Long id) {
        Resume resume = resumeFileService.getResume(id);
        String fileName = URLEncoder.encode(resume.getName(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resume.getFileData());
    }

    /**
     * 预览简历 —— 直接返回字节流，由前端自行处理；PDF 可用 <embed>，其他类型可另行处理
     */
    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> previewResume(@PathVariable("id") Long id) {
        Resume resume = resumeFileService.getResume(id);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if ("PDF".equalsIgnoreCase(resume.getType())) {
            mediaType = MediaType.APPLICATION_PDF;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resume.getFileData());
    }

    /**
     * 更新简历名称
     */
    @PutMapping("/{id}/name")
    public ResponseEntity<Resume> updateResumeName(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        return ResponseEntity.ok(resumeFileService.updateResumeName(id, name));
    }

    /**
     * 删除简历
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResume(@PathVariable("id") Long id) {
        resumeFileService.deleteResume(id);
        return ResponseEntity.ok().build();
    }
} 