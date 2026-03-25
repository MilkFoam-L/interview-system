package interview.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件控制器，用于提供静态资源文件
 */
@Controller
@RequestMapping("/api")
public class FileController {
    
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    
    /**
     * 获取用户头像
     */
    @GetMapping("/avatars/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        logger.info("请求头像文件: {}", filename);
        
        try {
            // 构建文件路径
            Path filePath = Paths.get("interview_backend/uploads/avatars").resolve(filename).normalize();
            logger.info("头像文件完整路径: {}", filePath.toAbsolutePath());
            
            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                logger.warn("头像文件不存在: {}", filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // 获取文件资源
            Resource resource = new FileSystemResource(filePath.toFile());
            
            // 确定内容类型
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            logger.info("返回头像文件: {}, 类型: {}", filename, contentType);
            
            // 返回文件资源，添加缓存控制
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource);
            
        } catch (IOException e) {
            logger.error("获取头像文件失败: {}, 错误: {}", filename, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
} 