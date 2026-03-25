package interview.util;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/**
 * 简历解析工具：支持 .pdf、.docx 与 .txt，返回纯文本；并可生成简易摘要。
 */
@Component
public class ResumeParserUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(ResumeParserUtil.class);

    /**
     * 提取文本内容
     */
    public static String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("文件名为空");
        }
        
        logger.info("解析简历文件: {}", filename);
        String lower = filename.toLowerCase();
        
        if (lower.endsWith(".pdf")) {
            try (InputStream is = file.getInputStream();
                 PDDocument document = PDDocument.load(is)) {
                PDFTextStripper pdfStripper = new PDFTextStripper();
                String text = pdfStripper.getText(document);
                logger.info("PDF文件解析成功，提取文本长度: {}", text.length());
                return text;
            }
        } else if (lower.endsWith(".docx")) {
            try (InputStream is = file.getInputStream();
                 XWPFDocument doc = new XWPFDocument(is);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                return extractor.getText();
            }
        } else if (lower.endsWith(".txt")) {
            try (InputStream is = file.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                return new String(bytes, StandardCharsets.UTF_8);
            }
        }
        
        logger.warn("不支持的文件类型: {}", filename);
        throw new IllegalArgumentException("暂不支持的文件类型: " + filename);
    }

    /**
     * 生成摘要，默认截取前 1000 字符。
     */
    public static String generateSummary(String content) {
        if (content == null) {
            return null;
        }
        String trimmed = content.trim();
        int maxLen = 1000;
        return trimmed.length() > maxLen ? trimmed.substring(0, maxLen) : trimmed;
    }
} 