package interview.service.impl;

import interview.dto.QuestionDTO;
import interview.model.entity.Question;
import interview.repository.QuestionRepository;
import interview.service.InterviewerQuestionService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 面试官题目管理服务实现
 */
@Service
public class InterviewerQuestionServiceImpl implements InterviewerQuestionService {
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Override
    public Page<Question> getQuestions(Pageable pageable, String category, String categoryType, 
                                     String type, String difficulty, String keyword) {
        Specification<Question> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 面试官可以看到所有题目（包括禁用的）
            // 注释掉isActive过滤，面试官应该能管理所有题目
            
            // 类别筛选
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            
            // 细分类别筛选
            if (categoryType != null && !categoryType.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("categoryType"), categoryType));
            }
            
            // 题目类型筛选
            if (type != null && !type.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            
            // 难度筛选
            if (difficulty != null && !difficulty.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("difficulty"), difficulty));
            }
            
            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likeKeyword = "%" + keyword.trim() + "%";
                Predicate contentMatch = criteriaBuilder.like(root.get("content"), likeKeyword);
                Predicate tagsMatch = criteriaBuilder.like(root.get("tags"), likeKeyword);
                predicates.add(criteriaBuilder.or(contentMatch, tagsMatch));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return questionRepository.findAll(spec, pageable);
    }
    
    @Override
    public Question getQuestionById(Long id) {
        return questionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("题目不存在: " + id));
    }
    
    @Override
    public QuestionDTO getQuestionDetailById(Long id) {
        Question question = getQuestionById(id);
        
        // 获取创建者用户名
        String createdByName = null;
        if (question.getCreatedBy() != null) {
            createdByName = questionRepository.findUsernameById(question.getCreatedBy())
                .orElse("未知用户");
        }
        
        // 获取更新者用户名
        String updatedByName = null;
        if (question.getUpdatedBy() != null) {
            updatedByName = questionRepository.findUsernameById(question.getUpdatedBy())
                .orElse("未知用户");
        }
        
        return new QuestionDTO(question, createdByName, updatedByName);
    }
    
    @Override
    @Transactional
    public Question createQuestion(Question question) {
        // 设置默认值
        question.setIsActive(true);
        question.setUsageCount(0);
        question.setCreateTime(LocalDateTime.now());
        question.setUpdateTime(LocalDateTime.now());
        
        return questionRepository.save(question);
    }
    
    @Override
    @Transactional
    public Question updateQuestion(Long id, Question question) {
        Question existing = getQuestionById(id);
        
        // 更新字段  
        existing.setContent(question.getContent());
        existing.setCategory(question.getCategory());
        existing.setCategoryType(question.getCategoryType());
        existing.setType(question.getType());
        existing.setDifficulty(question.getDifficulty());
        existing.setCorrectAnswer(question.getCorrectAnswer());
        existing.setTags(question.getTags());
        existing.setOptions(question.getOptions());
        existing.setUpdatedBy(question.getUpdatedBy());
        existing.setUpdateTime(LocalDateTime.now());
        
        return questionRepository.save(existing);
    }
    
    @Override
    @Transactional
    public void deleteQuestion(Long id, Long operatorId) {
        Question question = getQuestionById(id);
        // 硬删除：直接从数据库删除
        questionRepository.delete(question);
    }
    
    @Override
    @Transactional
    public Map<String, Object> batchDeleteQuestions(List<Long> questionIds, Long operatorId) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (Long id : questionIds) {
            try {
                deleteQuestion(id, operatorId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add("题目ID " + id + ": " + e.getMessage());
            }
        }
        
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        return result;
    }
    
    @Override
    @Transactional
    public Map<String, Object> uploadFromExcel(MultipartFile file, Long operatorId) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // 跳过说明和表头，从第8行开始（索引7）
            for (int i = 7; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    Question question = parseRowToQuestion(row, operatorId);
                    createQuestion(question);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add("文件读取失败: " + e.getMessage());
        }
        
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        return result;
    }
    
    @Override
    public byte[] generateExcelTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("题目导入模板");
            
            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // 说明文字样式
            CellStyle instructionStyle = workbook.createCellStyle();
            Font instructionFont = workbook.createFont();
            instructionFont.setFontHeightInPoints((short) 10);
            instructionStyle.setFont(instructionFont);
            
            // 创建标题样式
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 12);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);
            
            // 创建重要提示样式
            CellStyle warningStyle = workbook.createCellStyle();
            Font warningFont = workbook.createFont();
            warningFont.setBold(true);
            warningFont.setColor(IndexedColors.DARK_RED.getIndex());
            warningStyle.setFont(warningFont);
            
            // 格式说明
            Row instructionRow1 = sheet.createRow(0);
            Cell instructionCell1 = instructionRow1.createCell(0);
            instructionCell1.setCellValue("📋 面试题目导入模板 - 请严格按照格式要求填写数据");
            instructionCell1.setCellStyle(titleStyle);
            
            Row instructionRow2 = sheet.createRow(1);
            Cell instructionCell2 = instructionRow2.createCell(0);
            instructionCell2.setCellValue("⚠️ 重要提示：从第9行开始填写数据，带*号的字段为必填项，请勿修改表头结构");
            instructionCell2.setCellStyle(warningStyle);
            
            Row instructionRow3 = sheet.createRow(2);
            Cell instructionCell3 = instructionRow3.createCell(0);
            instructionCell3.setCellValue("📝 题目类型：basic(基础题) | code(编程题) | algorithm(算法题) | system_design(系统设计题) | project(项目经验题) | behavioral(行为面试题)");
            instructionCell3.setCellStyle(instructionStyle);
            
            Row instructionRow4 = sheet.createRow(3);
            Cell instructionCell4 = instructionRow4.createCell(0);
            instructionCell4.setCellValue("🎯 难度等级：easy(简单) | medium(中等) | hard(困难) - 支持大小写，系统会自动转换为小写");
            instructionCell4.setCellStyle(instructionStyle);
            
            Row instructionRow5 = sheet.createRow(4);
            Cell instructionCell5 = instructionRow5.createCell(0);
            instructionCell5.setCellValue("📊 选项格式：选择题用JSON数组 [\"A选项\",\"B选项\",\"C选项\",\"D选项\"]，参考答案填具体选项内容（如\"extends\"）| 标签用逗号分隔：Java,面向对象,继承");
            instructionCell5.setCellStyle(instructionStyle);
            
            Row instructionRow6 = sheet.createRow(5);
            Cell instructionCell6 = instructionRow6.createCell(0);
            instructionCell6.setCellValue("🔄 输入类型说明：系统会根据题目类型和选项自动设置 - code题→code类型 | 有选项→radio(单选) | 无选项→text(文本输入)");
            instructionCell6.setCellStyle(instructionStyle);
            
            // 添加空行（第7行）
            sheet.createRow(6);
            
            // 创建表头（第8行）
            Row headerRow = sheet.createRow(7);
            String[] headers = {"题目内容*", "大类别*", "细分类别*", "题目类型*", "难度*", "参考答案*", "标签", "选项"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 创建示例数据行
            String[][] examples = {
                {"Java中哪个关键字用于继承？", "后端开发", "Java基础", "basic", "easy", "extends", "Java,继承,关键字", "[\"class\",\"extends\",\"implements\",\"interface\"]"},
                {"请解释Java中的多态性概念", "后端开发", "Java基础", "basic", "medium", "多态性是面向对象编程的重要特性，允许不同类的对象对同一消息做出响应", "Java,面向对象,多态", ""},
                {"实现快速排序算法", "算法", "排序算法", "algorithm", "hard", "使用分治思想，选择基准元素，递归排序左右子数组", "算法,排序,分治", ""},
                {"设计一个高并发的秒杀系统", "系统设计", "高并发架构", "system_design", "hard", "采用Redis缓存、消息队列、限流、数据库分库分表等技术", "系统设计,高并发", ""}
            };
            
            for (int i = 0; i < examples.length; i++) {
                Row exampleRow = sheet.createRow(8 + i);
                for (int j = 0; j < examples[i].length; j++) {
                    Cell cell = exampleRow.createCell(j);
                    cell.setCellValue(examples[i][j]);
                }
            }
            
            // 设置列宽
            sheet.setColumnWidth(0, 12000); // 题目内容
            sheet.setColumnWidth(1, 3500);  // 大类别
            sheet.setColumnWidth(2, 4000);  // 细分类别
            sheet.setColumnWidth(3, 3500);  // 题目类型
            sheet.setColumnWidth(4, 2500);  // 难度
            sheet.setColumnWidth(5, 12000); // 参考答案
            sheet.setColumnWidth(6, 4000);  // 标签
            sheet.setColumnWidth(7, 8000);  // 选项
            
            // 合并说明单元格
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 7));
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 7));
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 7));
            sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 7));
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成Excel模板失败", e);
        }
    }
    
    @Override
    public Map<String, Object> getQuestionStatistics(String category, String categoryType, String type) {
        Map<String, Object> result = new HashMap<>();
        
        // 构建查询条件
        Specification<Question> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // 查询所有题目（包括已禁用的），以获取完整统计
            
            if (category != null && !category.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), category));
            }
            if (categoryType != null && !categoryType.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("categoryType"), categoryType));
            }
            if (type != null && !type.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }
            
            return predicates.isEmpty() ? criteriaBuilder.conjunction() : 
                   criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        List<Question> allQuestions = questionRepository.findAll(spec);
        List<Question> activeQuestions = allQuestions.stream()
            .filter(Question::getIsActive)
            .collect(Collectors.toList());
        
        // 总体概况
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalQuestions", allQuestions.size());
        overview.put("activeQuestions", activeQuestions.size());
        
        // 计算已使用题目数量（usageCount > 0的题目数量）
        long usedQuestions = activeQuestions.stream()
            .filter(q -> q.getUsageCount() != null && q.getUsageCount() > 0)
            .count();
        overview.put("usedQuestions", usedQuestions);
        
        // 计算总使用次数（所有题目usageCount的总和）
        int totalUsage = activeQuestions.stream()
            .filter(q -> q.getUsageCount() != null)
            .mapToInt(Question::getUsageCount)
            .sum();
        overview.put("totalUsage", totalUsage);
        
        result.put("overview", overview);
        
        // 按大类别统计
        Map<String, Long> categoryStats = activeQuestions.stream()
            .filter(q -> q.getCategory() != null && !q.getCategory().trim().isEmpty())
            .collect(Collectors.groupingBy(Question::getCategory, Collectors.counting()));
        List<Map<String, Object>> categoryStatsList = categoryStats.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        result.put("categoryStats", categoryStatsList);
        
        // 按细分类型统计
        Map<String, Long> typeStatsMap = activeQuestions.stream()
            .filter(q -> q.getCategoryType() != null && !q.getCategoryType().trim().isEmpty())
            .collect(Collectors.groupingBy(Question::getCategoryType, Collectors.counting()));
        List<Map<String, Object>> typeStatsList = typeStatsMap.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        result.put("typeStats", typeStatsList);
        
        // 按题目类型统计
        Map<String, Long> questionTypeStats = activeQuestions.stream()
            .collect(Collectors.groupingBy(Question::getType, Collectors.counting()));
        List<Map<String, Object>> questionTypeStatsList = questionTypeStats.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", getTypeDisplayName(entry.getKey()));
                item.put("type", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
        result.put("questionTypeStats", questionTypeStatsList);
        
        // 使用排行榜（按使用次数排序，取前10个）
        List<Map<String, Object>> topUsedQuestions = activeQuestions.stream()
            .sorted((q1, q2) -> {
                Integer count1 = q1.getUsageCount() != null ? q1.getUsageCount() : 0;
                Integer count2 = q2.getUsageCount() != null ? q2.getUsageCount() : 0;
                return count2.compareTo(count1); // 降序排列
            })
            .limit(10)
            .map(question -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", question.getId());
                item.put("content", question.getContent().length() > 50 ? 
                        question.getContent().substring(0, 50) + "..." : question.getContent());
                item.put("type", question.getType());
                item.put("category", question.getCategory());
                item.put("categoryType", question.getCategoryType());
                item.put("usageCount", question.getUsageCount() != null ? question.getUsageCount() : 0);
                return item;
            })
            .collect(Collectors.toList());
        result.put("topUsedQuestions", topUsedQuestions);
        
        return result;
    }
    
    /**
     * 获取题目类型的显示名称
     */
    private String getTypeDisplayName(String type) {
        switch (type) {
            case "basic": return "基础题";
            case "programming": return "编程题";
            case "scenario": return "场景题";
            case "system_design": return "系统设计";
            default: return type;
        }
    }
    
    @Override
    public Map<String, List<String>> getAllCategories() {
        // 面试官应该能看到所有题目的类别选项（包括已禁用的）
        List<Question> allQuestions = questionRepository.findAll();
        
        Map<String, List<String>> result = new HashMap<>();
        
        // 获取所有大类别
        List<String> categories = allQuestions.stream()
            .map(Question::getCategory)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        // 获取所有细分类型
        List<String> categoryTypes = allQuestions.stream()
            .map(Question::getCategoryType)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        result.put("categories", categories);
        result.put("categoryTypes", categoryTypes);
        
        return result;
    }
    

    
    @Override
    @Transactional
    public Question toggleQuestionActive(Long id, Long operatorId) {
        Question question = getQuestionById(id);
        question.setIsActive(!question.getIsActive());
        question.setUpdatedBy(operatorId);
        question.setUpdateTime(LocalDateTime.now());
        return questionRepository.save(question);
    }
    
    /**
     * 解析Excel行数据为Question对象
     */
    private Question parseRowToQuestion(Row row, Long operatorId) {
        Question question = new Question();
        
        question.setContent(getCellStringValue(row.getCell(0)));
        question.setCategory(getCellStringValue(row.getCell(1)));
        question.setCategoryType(getCellStringValue(row.getCell(2)));
        question.setType(getCellStringValue(row.getCell(3)));
        
        // 处理难度字段：统一转换为小写存储到数据库
        String difficulty = getCellStringValue(row.getCell(4));
        if (difficulty != null) {
            difficulty = difficulty.toLowerCase().trim();
            // 验证难度值是否有效
            if (!"easy".equals(difficulty) && !"medium".equals(difficulty) && !"hard".equals(difficulty)) {
                throw new IllegalArgumentException("难度必须是 easy、medium 或 hard 之一（不区分大小写）");
            }
        }
        question.setDifficulty(difficulty);
        
        question.setCorrectAnswer(getCellStringValue(row.getCell(5)));
        question.setTags(getCellStringValue(row.getCell(6)));
        question.setOptions(getCellStringValue(row.getCell(7)));
        
        // 根据题目类型和选项字段判断输入类型
        String type = question.getType();
        String options = question.getOptions();
        
        if ("code".equals(type)) {
            // 代码题
            question.setInputType("code");
        } else if (options != null && !options.trim().isEmpty()) {
            // 有选项说明是选择题
            question.setInputType("radio");
        } else {
            // 没有选项说明是文本输入题
            question.setInputType("text");
        }
        
        question.setCreatedBy(operatorId);
        question.setIsActive(true);
        question.setUsageCount(0);
        
        return question;
    }
    
    /**
     * 获取单元格字符串值
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}