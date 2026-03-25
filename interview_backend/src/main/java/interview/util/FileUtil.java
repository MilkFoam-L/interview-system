package interview.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 文件工具类
 */
@Component
public class FileUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    
    // 基础存储路径 - 与 ImageFileUtil 保持一致
    private static final String BASE_PATH = "D:/interview_images";
    
    // 定义图片存储路径
    private static final String UPLOAD_DIR = BASE_PATH + "/face_images2";
    
    /**
     * 读取文件为字节数组
     * 
     * @param filePath 文件路径
     * @return 字节数组
     * @throws IOException IO异常
     */
    public static byte[] read(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }
    
    /**
     * 文件转Base64
     * 
     * @param filePath 文件路径
     * @return Base64字符串
     */
    public static String fileToBase64(String filePath) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.error("文件转Base64失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 保存Base64为文件
     * 
     * @param base64Str Base64字符串
     * @param outputPath 输出文件路径
     * @return 是否成功
     */
    public static boolean base64ToFile(String base64Str, String outputPath) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Str);
            Files.write(Paths.get(outputPath), bytes);
            return true;
        } catch (Exception e) {
            logger.error("Base64转文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 创建临时文件
     * 
     * @param base64Image Base64编码的图片
     * @param prefix 文件前缀
     * @param suffix 文件后缀
     * @return 临时文件路径
     * @throws IOException IO异常
     */
    public static String createTempFile(String base64Image, String prefix, String suffix) throws IOException {
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);
        String tempDir = UPLOAD_DIR; // 使用统一的 UPLOAD_DIR 路径
        File dir = new File(tempDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String tempFileName = prefix + System.currentTimeMillis() + suffix;
        java.nio.file.Path tempPath = Paths.get(tempDir, tempFileName);
        Files.write(tempPath, imageBytes);
        return tempPath.toString();
    }
    
    /**
     * 验证是否为有效的图片文件
     * 
     * @param file 上传的文件
     * @return 是否为有效的图片文件
     */
    public static boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals("image/jpeg") || 
                                      contentType.equals("image/png") || 
                                      contentType.equals("image/jpg"));
    }
    
    /**
     * 验证文件大小是否有效
     * 
     * @param file 上传的文件
     * @param maxSize 最大文件大小（字节）
     * @return 文件大小是否有效
     */
    public static boolean isFileSizeValid(MultipartFile file, long maxSize) {
        return file != null && file.getSize() <= maxSize;
    }
    
    /**
     * 保存上传的照片
     * 
     * @param file 上传的文件
     * @return 保存后的文件名
     * @throws IOException IO异常
     */
    public static String savePhoto(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        
        // 创建上传目录
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? 
                           originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String filename = UUID.randomUUID().toString() + extension;
        
        // 保存文件
        Path targetPath = Paths.get(UPLOAD_DIR).resolve(filename);
        Files.copy(file.getInputStream(), targetPath);
        
        return filename;
    }
} 