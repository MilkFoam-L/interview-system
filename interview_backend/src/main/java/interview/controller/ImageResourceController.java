package interview.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图片资源控制器
 * 提供API接口访问本地存储的图片
 */
@RestController
@RequestMapping("/api/images")
public class ImageResourceController {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageResourceController.class);
    
    // 本地图片存储目录
    private static final String IMAGE_STORAGE_DIR = "D:\\interview_images\\face_images2";
    
    /**
     * 获取表情图片
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param imageName 图片名称
     * @return 图片响应
     */
    @GetMapping("/face/{sessionId}/{userId}/{imageName}")
    public ResponseEntity<?> getFaceImage(@PathVariable Long sessionId, 
                                           @PathVariable Long userId,
                                           @PathVariable String imageName) {
        try {
            // 构建图片路径
            File sessionDir = new File(IMAGE_STORAGE_DIR, String.valueOf(sessionId));
            File userDir = new File(sessionDir, String.valueOf(userId));
            File imageFile = new File(userDir, imageName);
            
            logger.debug("获取表情图片: {}", imageFile.getAbsolutePath());
            
            // 检查文件是否存在
            if (!imageFile.exists()) {
                logger.warn("图片不存在: {}", imageFile.getAbsolutePath());
                return ResponseEntity.notFound().build();
            }
            
            // 读取图片数据
            byte[] imageData = Files.readAllBytes(imageFile.toPath());
            
            // 确定图片类型
            String contentType = determineContentType(imageName);
            
            // 返回图片数据
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(imageData);
        } catch (IOException e) {
            logger.error("读取图片失败", e);
            return ResponseEntity.badRequest().body("读取图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 兼容性接口，通过完整路径获取图片
     * 
     * @param request HTTP请求对象
     * @return 图片响应
     */
    @GetMapping("/**")
    public ResponseEntity<?> getImageByPath(jakarta.servlet.http.HttpServletRequest request) {
        try {
            // 获取请求路径
            String requestURI = request.getRequestURI();
            String pathPart = requestURI.replaceFirst("/api/images/", "");
            
            logger.debug("图片请求路径: {}", pathPart);
            
            // 尝试匹配路径格式
            if (pathPart.startsWith("D:") || pathPart.contains("interview_images")) {
                // 如果是完整路径，则直接使用
                Path path;
                if (pathPart.startsWith("D:")) {
                    path = Paths.get(pathPart);
                } else {
                    // 如果只是相对路径，添加基础目录
                    path = Paths.get(IMAGE_STORAGE_DIR, pathPart);
                }
                
                if (!Files.exists(path)) {
                    logger.warn("图片不存在: {}", path);
                    return ResponseEntity.notFound().build();
                }
                
                byte[] imageData = Files.readAllBytes(path);
                String contentType = determineContentType(path.toString());
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(imageData);
            } else {
                logger.warn("无效的图片路径请求: {}", pathPart);
                return ResponseEntity.badRequest().body("无效的图片路径请求");
            }
        } catch (IOException e) {
            logger.error("读取图片失败", e);
            return ResponseEntity.badRequest().body("读取图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据文件名确定内容类型
     * 
     * @param fileName 文件名
     * @return 内容类型
     */
    private String determineContentType(String fileName) {
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".bmp")) {
            return "image/bmp";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        } else {
            return "application/octet-stream";
        }
    }
} 