package interview.service;

public interface ResumeGenerationService {
    /**
     * 生成简历
     * @param prompt 提示词
     * @return 生成的简历内容
     */
    String generateResume(String prompt);

    /**
     * 保存简历
     * @param originalData 原始简历数据
     * @param generatedContent 生成的简历内容
     */
    void saveResume(String originalData, String generatedContent);
} 