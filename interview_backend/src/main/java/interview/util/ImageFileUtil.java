package interview.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

/**
 * 图片文件工具类
 */
@Component
public class ImageFileUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageFileUtil.class);
    
    // 基础存储路径
    private static final String BASE_PATH = "D:/interview_images";
    
    /**
     * 保存Base64编码的图片
     *
     * @param base64Image Base64编码的图片数据（不包含前缀）
     * @param idNumber 用户身份证号
     * @param imageType 图片类型（face/idcard）
     * @return 保存的文件相对路径
     */
    public String saveImage(String base64Image, String idNumber, String imageType) {
        try {
            // 创建日期目录
            String dateStr = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String dirPath = BASE_PATH + "/" + imageType + "/" + dateStr;
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 生成文件名
            String fileName = idNumber + "_" + System.currentTimeMillis() + ".jpg";
            String filePath = dirPath + "/" + fileName;
            
            // 处理Base64字符串，去除可能的前缀
            String pureBase64 = base64Image;
            if (base64Image.contains(",")) {
                pureBase64 = base64Image.split(",")[1];
            }
            
            // 解码Base64并保存为文件
            byte[] imageBytes = Base64.getDecoder().decode(pureBase64);
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(imageBytes);
                fos.flush();
            }
            
            logger.info("图片已保存: {}", filePath);
            
            // 返回相对路径
            return imageType + "/" + dateStr + "/" + fileName;
            
        } catch (IOException e) {
            logger.error("保存图片失败", e);
            throw new RuntimeException("保存图片失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取图片的绝对路径
     *
     * @param relativePath 相对路径
     * @return 绝对路径
     */
    public String getAbsolutePath(String relativePath) {
        return BASE_PATH + "/" + relativePath;
    }
    
    /**
     * 删除图片文件
     *
     * @param relativePath 相对路径
     * @return 是否删除成功
     */
    public boolean deleteImage(String relativePath) {
        File file = new File(BASE_PATH + "/" + relativePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            logger.info("删除图片 {}: {}", relativePath, deleted ? "成功" : "失败");
            return deleted;
        }
        return false;
    }
} 