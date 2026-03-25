package interview.controller;

import interview.service.ResumeGenerationService;
import interview.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/resume")
public class ResumeController {

    @Autowired
    private ResumeGenerationService resumeGenerationService;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB

    @PostMapping("/upload-photo")
    public ResponseEntity<?> uploadPhoto(@RequestParam("file") MultipartFile file) {
        try {
            // 验证文件类型
            if (!FileUtil.isValidImageFile(file)) {
                return ResponseEntity.badRequest().body(Map.of("error", "只支持JPG和PNG格式的图片"));
            }

            // 验证文件大小
            if (!FileUtil.isFileSizeValid(file, MAX_FILE_SIZE)) {
                return ResponseEntity.badRequest().body(Map.of("error", "图片大小不能超过2MB"));
            }

            // 保存文件
            String filePath = FileUtil.savePhoto(file);
            
            // 返回文件URL
            return ResponseEntity.ok(Map.of("url", "/api/resume/photos/" + filePath));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "上传失败：" + e.getMessage()));
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateResume(@RequestBody Map<String, String> request) {
        try {
            String prompt = request.get("prompt");
            String content = resumeGenerationService.generateResume(prompt);
            return ResponseEntity.ok(Map.of("content", content));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveResume(@RequestBody Map<String, String> request) {
        try {
            String originalData = request.get("originalData");
            String generatedContent = request.get("generatedContent");
            resumeGenerationService.saveResume(originalData, generatedContent);
            return ResponseEntity.ok(Map.of("message", "保存成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
} 