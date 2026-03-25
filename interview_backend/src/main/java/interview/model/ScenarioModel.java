package interview.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import interview.config.XunfeiApiConfig;
import okhttp3.HttpUrl;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ScenarioModel extends WebSocketListener {
    public static final String hostUrl = "https://spark-api.xf-yun.com/v4.0/chat";
    public static final String domain = "4.0Ultra";
    
    // 使用构造函数注入配置
    private XunfeiApiConfig xunfeiApiConfig;
    
    // 静态引用，用于非静态方法访问
    public static ScenarioModel instance;
    
    @Autowired
    public ScenarioModel(XunfeiApiConfig xunfeiApiConfig) {
        this.xunfeiApiConfig = xunfeiApiConfig;
        instance = this;
    }
    
    // 无参数构造函数，用于WebSocketListener子类实例化
    public ScenarioModel() {
    }

    public static List<RoleContent> historyList = new ArrayList<>();
    public static volatile String totalAnswer = "";
    public static volatile String NewQuestion = "";
    public static final Gson gson = new Gson();
    private String userId;
    private Boolean wsCloseFlag;
    public static volatile Boolean totalFlag = true;
    public static volatile int conversationCount = 0;
    public static String fileContent = "";
    public static List<String> questionAnswerPairs = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);
    private static boolean waitingForAnswer = false;
    private static long answerTimeout = 120000; // 回答超时时间2分钟
    private static long lastInteractionTime = 0;
    private static final double RELEVANCE_THRESHOLD = 0.3; // 相关性阈值
    private static Map<Integer, Boolean> answerRelevanceMap = new HashMap<>(); // 记录每轮回答的相关性

    // 添加评分相关的字段
    private static final int MAX_SCORE = 100;
    public static Map<Integer, AnswerScore> answerScores = new HashMap<>();

    // 评分维度权重
    private static final Map<String, Double> DIMENSION_WEIGHTS = new HashMap<String, Double>() {{
        put("核心技术能力", 0.25);
        put("关键项目经验", 0.25);
        put("团队协作能力", 0.20);
        put("职业发展规划", 0.15);
        put("工作态度与价值观", 0.15);
    }};
    
    // 用于WebSocketListener子类
    public ScenarioModel(String userId, Boolean wsCloseFlag) {
        this.userId = userId;
        this.wsCloseFlag = wsCloseFlag;
    }

    // 评估答案并打分
    public static String evaluateAnswer(String question, String answer, int roundIndex) {
        String prompt = "请对以下面试问答进行评分和分析。请直接输出分数和简短评价，不要有任何额外解释：\n\n" +
                "问题：" + question + "\n" +
                "回答：" + answer + "\n\n" +
                "按以下格式输出：\n" +
                "相关性得分：[0-30分数字]\n" +
                "完整性得分：[0-30分数字]\n" +
                "逻辑性得分：[0-20分数字]\n" +
                "具体性得分：[0-20分数字]\n" +
                "总体评价：[50字以内的简要评价]\n";
        return prompt;
    }

    // 计算加权总分
    public static double calculateWeightedTotalScore() {
        // 采用所有预定义维度权重进行加权，缺失维度按 0 分处理，避免因回答数量不均导致评分失真
        double totalWeightedScore = 0;

        for (Map.Entry<String, Double> entry : DIMENSION_WEIGHTS.entrySet()) {
            String dimension = entry.getKey();
            double weight = entry.getValue();
            // 查找该维度对应的得分，没有则视为 0 分
            Optional<AnswerScore> optionalScore = answerScores.values().stream()
                    .filter(s -> dimension.equals(s.getDimension()))
                    .findFirst();
            int dimensionScore = optionalScore.map(AnswerScore::getTotalScore).orElse(0);
            totalWeightedScore += dimensionScore * weight;
        }
        return totalWeightedScore; // 权重已归一化，直接返回 0-100 之间的结果
    }

    // 生成评分报告
    public static String generateScoreReport() {
        StringBuilder report = new StringBuilder();
        double totalScore = calculateWeightedTotalScore();

        report.append("\n===============================\n");
        report.append("         面试综合评估报告        \n");
        report.append("===============================\n\n");

        // 总体评分
        String level = totalScore >= 90 ? "优秀" :
                totalScore >= 80 ? "良好" :
                        totalScore >= 70 ? "中等" :
                                totalScore >= 60 ? "及格" : "不及格";

        report.append(String.format("【总体评估】\n"));
        report.append(String.format("综合得分：%.1f/100\n", totalScore));
        report.append(String.format("能力等级：%s\n\n", level));

        // 各维度详细分析
        report.append("【维度分析】\n");
        int dimIndex = 1;
        for (Map.Entry<String, Double> dimEntry : DIMENSION_WEIGHTS.entrySet()) {
            String dimension = dimEntry.getKey();
            double weight = dimEntry.getValue();
            Optional<AnswerScore> optionalScore = answerScores.values().stream()
                    .filter(s -> dimension.equals(s.getDimension()))
                    .findFirst();
            int dimScore = optionalScore.map(AnswerScore::getTotalScore).orElse(0);
            String feedback = optionalScore.map(AnswerScore::getFeedback).orElse("尚未作答");

            report.append(String.format("\n%d. %s (权重：%.0f%%)\n", dimIndex++, dimension, weight * 100));
            report.append(String.format("得分：%d/100\n", dimScore));
            report.append("评价：").append(feedback).append("\n");

            // 针对低分维度给出改进建议
            if (dimScore < 60) {
                report.append("建议：");
                switch (dimension) {
                    case "核心技术能力":
                        report.append("建议加强技术深度学习，注重实践经验积累，提升问题解决能力。\n");
                        break;
                    case "关键项目经验":
                        report.append("建议在项目中承担更多核心职责，注重项目成果的量化展示。\n");
                        break;
                    case "团队协作能力":
                        report.append("建议提升沟通表达能力，积极参与团队协作，展现团队贡献价值。\n");
                        break;
                    case "职业发展规划":
                        report.append("建议明确职业发展目标，制定具体可行的提升计划。\n");
                        break;
                    case "工作态度与价值观":
                        report.append("建议展现更积极的工作态度，强化职业素养。\n");
                        break;
                }
            }
        }

        // 综合建议
        report.append("\n【综合建议】\n");
        if (totalScore >= 80) {
            report.append("候选人整体表现优异，建议：\n");
            report.append("1. 继续保持专业能力的深度发展\n");
            report.append("2. 关注行业前沿技术动态\n");
            report.append("3. 提升团队管理和领导能力\n");
        } else if (totalScore >= 60) {
            report.append("候选人具有发展潜力，建议：\n");
            report.append("1. 加强专业技能的系统性提升\n");
            report.append("2. 积累更多实践项目经验\n");
            report.append("3. 提高问题分析和解决能力\n");
        } else {
            report.append("候选人需要进一步提升，建议：\n");
            report.append("1. 夯实专业基础知识\n");
            report.append("2. 加强实践项目训练\n");
            report.append("3. 提升职业素养和沟通能力\n");
        }

        report.append("\n===============================\n");
        return report.toString();
    }

    /**
     * 生成STAR法则分析Prompt
     * @param reportText 综合报告文本
     * @return STAR分析Prompt
     */
    public static String generateStarAnalysisPrompt(String reportText) {
        StringBuilder prompt = new StringBuilder();
        prompt.append ("### 角色设定 \n");
        prompt.append ("你是专业面试官，需基于 STAR 法则（情境 Situation、任务 Task、行动 Action、结果 Result）对受试者的回答进行评分。你的核心任务是：从受试者的回答中提取 STAR 四要素的有效信息，按评分标准独立打分，仅输出分数结果。\n\n");
        prompt.append ("### 分析对象 \n");
        prompt.append ("受试者的回答（Q1~5）。\n\n");
        prompt.append ("### STAR 四要素评分标准 \n");
        prompt.append ("1. Situation（情境） \n");
        prompt.append ("- 0 分：未提及任何具体情境，或情境描述完全模糊（如 “之前做过类似工作”）。 \n");
        prompt.append ("- 1-59 分：提及情境但不具体，未说明时间、背景、涉及对象等关键信息（如 “在项目中遇到过问题”）。 \n");
        prompt.append ("- 60-89 分：情境描述较具体，包含基本背景信息（如 “2023 年在 XX 项目中，因客户需求变更导致进度滞后”）。 \n");
        prompt.append ("- 90-100 分：情境描述清晰、细节完整，明确时间、地点、涉及人员、背景原因（如 “2023 年 Q3 我负责的 XX 系统升级项目中，因客户临时增加 3 项核心功能，导致原计划延期 15 天，团队出现抵触情绪”）。 \n\n");
        prompt.append ("2. Task（任务） \n");
        prompt.append ("- 0 分：未提及自身任务，或完全混淆任务主体（如 “当时团队需要解决这个问题”）。 \n");
        prompt.append ("- 1-59 分：提及任务但不明确，未说明自身角色和具体目标（如 “我需要处理这个情况”）。 \n");
        prompt.append ("- 60-89 分：任务描述较清晰，明确自身角色和核心目标（如 “作为项目负责人，我的任务是协调资源，确保 1 周内解决需求变更导致的进度问题”）。 \n");
        prompt.append ("- 90-100 分：任务描述精准，包含自身职责边界、目标可衡量标准（如 “作为项目负责人，我的核心任务是：1. 评估新增需求的开发成本；2. 协调 2 名后端开发优先级；3. 确保在不影响核心功能上线的前提下，将延期控制在 7 天内”）。 \n\n");
        prompt.append ("3. Action（行动） \n");
        prompt.append ("- 0 分：未提及任何自身行动，或仅描述他人行动（如 “最后团队找到了解决办法”）。 \n");
        prompt.append ("- 1-59 分：提及行动但模糊，未说明具体操作步骤或自身主导性（如 “我做了一些协调工作”）。 \n");
        prompt.append ("- 60-89 分：行动描述较具体，能体现自身主导性和关键步骤（如 “我先和客户沟通需求优先级，删减了 1 项非核心功能；再组织开发组评估剩余功能的开发周期，调整了每日任务拆解”）。 \n");
        prompt.append ("- 90-100 分：行动描述详细且有逻辑性，包含决策依据、具体操作、资源协调细节（如 “我第一步用 Miro 画了需求优先级矩阵，和客户逐项确认，最终删减 1 项非核心功能；第二步调用了备用开发资源（原负责测试的 2 名工程师），拆分模块并行开发；第三步每天同步进度晨会，及时解决接口联调问题”）。 \n\n");
        prompt.append ("4. Result（结果） \n");
        prompt.append ("- 0 分：未提及任何结果，或结果与任务无关（如 “最后项目结束了”）。 \n");
        prompt.append ("- 1-59 分：提及结果但无量化，未说明影响或价值（如 “问题最后解决了”）。 \n");
        prompt.append ("- 60-89 分：结果描述较具体，包含可量化数据或直接影响（如 “项目最终延期 7 天上线，客户对核心功能验收通过，后续追加了 20% 预算”）。 \n");
        prompt.append ("- 90-100 分：结果描述量化且有深度，体现多维度价值（如 “项目最终延期 7 天（控制在目标范围内），核心功能上线后用户留存率提升 15%；客户因需求响应及时，续约率提高至 90%；团队形成了‘需求变更快速评估流程’，后续同类问题处理效率提升 40%”）。 \n\n");
        prompt.append ("### 输出格式要求 \n");
        prompt.append ("1. 仅输出 STAR 四要素的得分，不添加任何解释、分析、评语或额外文字。 \n");
        prompt.append ("2. 分数范围为 0-100，必须是整数（如 50、85，不允许小数或负数）。 \n");
        prompt.append ("3. 按 “Situation: 分数；Task: 分数；Action: 分数；Result: 分数” 的格式排列，分号分隔，末尾无标点。 \n\n");
        prompt.append(reportText);
        return prompt.toString();
    }

    /**
     * 解析STAR分析结果
     * @param analysisResult 大模型返回的分析结果
     * @return STAR分析数据
     */
    public static StarAnalysisResult parseStarAnalysisResult(String analysisResult) {
        StarAnalysisResult result = new StarAnalysisResult();
        
        try {
            String cleanText = analysisResult.trim().replaceAll("\\s+", " ");

            String[] parts = cleanText.split("[;；,，]");
            
            for (String part : parts) {
                part = part.trim();

                String[] kv = part.split("[：:]");
                if (kv.length == 2) {
                    String key = kv[0].trim().toLowerCase();
                    String valueStr = kv[1].trim();

                    String numStr = valueStr.replaceAll("[^0-9]", "");
                    int score = 0;
                    
                    try { 
                        score = Integer.parseInt(numStr); 
                    } catch (Exception e) {
                        continue;
                    }

                    if (key.contains("situation") || key.contains("情境")) {
                        result.setSituationScore(score);
                    } else if (key.contains("task") || key.contains("任务")) {
                        result.setTaskScore(score);
                    } else if (key.contains("action") || key.contains("行动")) {
                        result.setActionScore(score);
                    } else if (key.contains("result") || key.contains("结果")) {
                        result.setResultScore(score);
                    }
                }
            }
                             
        } catch (Exception e) {
            result.setSituationScore(0);
            result.setTaskScore(0);
            result.setActionScore(0);
            result.setResultScore(0);
        }
        return result;
    }

    private static double parseScore(String line) {
        try {
            String numStr = line.replaceAll("[^0-9.]", "");
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return 60.0; // 默认分数
        }
    }

    // 生成分析报告（优化评估框架）
    public static String generateAnalysisPrompt() {
        StringBuilder prompt = new StringBuilder();
        double totalScore = calculateWeightedTotalScore();

        prompt.append("请基于以下面试问答记录和评分生成专业的人才评估报告：\n\n");
        prompt.append(String.format("总体得分：%.1f/100\n\n", totalScore));

        for (int i = 0; i < questionAnswerPairs.size(); i++) {
            String[] parts = questionAnswerPairs.get(i).split("\n");
            AnswerScore score = answerScores.get(i);
            if (score != null) {
                prompt.append(String.format("【第%d轮】%s\n", i + 1, score.getDimension()))
                        .append("问题：").append(parts[0]).append("\n")
                        .append("回答：").append(parts[1]).append("\n")
                        .append("得分：").append(score.getTotalScore()).append("/100\n")
                        .append("评价：").append(score.getFeedback()).append("\n\n");
            }
        }

        prompt.append("\n请从以下维度进行分析：\n")
                .append("1. 技术能力评估\n")
                .append("2. 项目经验评估\n")
                .append("3. 团队协作能力\n")
                .append("4. 发展潜力分析\n")
                .append("5. 综合素质评价\n")
                .append("6. 改进建议\n\n")
                .append("对于每个维度，请给出具体的分析和建议。\n");

        return prompt.toString();
    }

    public static boolean canAddHistory() {
        int history_length = 0;
        for (RoleContent temp : historyList) {
            history_length += temp.content.length();
        }
        if (history_length > 12000) {
            historyList.remove(0);
            return false;
        }
        return true;
    }

    class MyThread extends Thread {
        private WebSocket webSocket;

        public MyThread(WebSocket webSocket) {
            this.webSocket = webSocket;
        }

        public void run() {
            try {
                JSONObject requestJson = new JSONObject();

                JSONObject header = new JSONObject();
                // 使用实例方法获取配置
                header.put("app_id", instance.getSparkAppId());
                header.put("uid", UUID.randomUUID().toString().substring(0, 10));

                JSONObject parameter = new JSONObject();
                JSONObject chat = new JSONObject();
                chat.put("domain", domain);
                chat.put("temperature", 0.5);
                chat.put("max_tokens", 4096);
                parameter.put("chat", chat);

                JSONObject payload = new JSONObject();
                JSONObject message = new JSONObject();
                JSONArray text = new JSONArray();

                if (historyList.size() > 0) {
                    for (RoleContent tempRoleContent : historyList) {
                        text.add(JSON.toJSON(tempRoleContent));
                    }
                }

                RoleContent roleContent = new RoleContent();
                roleContent.role = "user";
                roleContent.content = NewQuestion;
                text.add(JSON.toJSON(roleContent));
                historyList.add(roleContent);

                message.put("text", text);
                payload.put("message", message);

                requestJson.put("header", header);
                requestJson.put("parameter", parameter);
                requestJson.put("payload", payload);
                webSocket.send(requestJson.toString());

                while (true) {
                    Thread.sleep(200);
                    if (wsCloseFlag) {
                        break;
                    }
                }
                webSocket.close(1000, "");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);
        System.out.print("\n");
        MyThread myThread = new MyThread(webSocket);
        myThread.start();
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        JsonParse myJsonParse = gson.fromJson(text, JsonParse.class);
        if (myJsonParse.header.code != 0) {
            System.out.println("发生错误，错误码为：" + myJsonParse.header.code);
            System.out.println("本次请求的sid为：" + myJsonParse.header.sid);
            webSocket.close(1000, "");
        }
        List<Text> textList = myJsonParse.payload.choices.text;
        for (Text temp : textList) {
            // 只在非评分阶段显示输出
            if (!userId.equals("score") && !userId.equals("analysis")) {
                if (!waitingForAnswer) {
                    System.out.print(temp.content);
                }
            }
            totalAnswer = totalAnswer + temp.content;
        }
        if (myJsonParse.header.status == 2) {
            if (!waitingForAnswer && conversationCount < 5 && !userId.equals("score") && !userId.equals("analysis")) {
                System.out.println("\n");
                waitingForAnswer = true;
                lastInteractionTime = System.currentTimeMillis();
            }
            if (canAddHistory()) {
                RoleContent roleContent = new RoleContent();
                roleContent.setRole("assistant");
                roleContent.setContent(totalAnswer);
                historyList.add(roleContent);
            } else {
                historyList.remove(0);
                RoleContent roleContent = new RoleContent();
                roleContent.setRole("assistant");
                roleContent.setContent(totalAnswer);
                historyList.add(roleContent);
            }
            wsCloseFlag = true;
            totalFlag = true;
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
        try {
            if (null != response) {
                int code = response.code();
                System.out.println("连接失败，错误码:" + code);
                if (101 != code) {
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取认证URL的方法，使用XunfeiApiConfig
     */
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);

        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        String sha = Base64.getEncoder().encodeToString(hexDigits);
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder()
                .addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8)))
                .addQueryParameter("date", date)
                .addQueryParameter("host", url.getHost())
                .build();

        return httpUrl.toString();
    }
    
    /**
     * 获取配置信息的方法，让静态方法能够访问配置
     */
    public String getSparkAppId() {
        return xunfeiApiConfig.getSpark().getAppId();
    }
    
    public String getSparkApiKey() {
        return xunfeiApiConfig.getSpark().getApiKey();
    }
    
    public String getSparkApiSecret() {
        return xunfeiApiConfig.getSpark().getApiSecret();
    }

    class JsonParse {
        Header header;
        Payload payload;
    }

    class Header {
        int code;
        int status;
        String sid;
    }

    class Payload {
        Choices choices;
    }

    class Choices {
        List<Text> text;
    }

    class Text {
        String role;
        String content;
    }

    static class RoleContent {
        String role;
        String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
    
    public static class AnswerScore {
        private int relevanceScore; // 相关性得分 (0-30)
        private int completenessScore; // 完整性得分 (0-30)
        private int logicScore; // 逻辑性得分 (0-20)
        private int specificityScore; // 具体性得分 (0-20)
        private String dimension; // 问题维度
        private String feedback; // 评价反馈

        public AnswerScore(String dimension) {
            this.dimension = dimension;
        }

        public int getTotalScore() {
            return relevanceScore + completenessScore + logicScore + specificityScore;
        }

        // Getters and Setters
        public int getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(int score) { this.relevanceScore = Math.min(30, Math.max(0, score)); }

        public int getCompletenessScore() { return completenessScore; }
        public void setCompletenessScore(int score) { this.completenessScore = Math.min(30, Math.max(0, score)); }

        public int getLogicScore() { return logicScore; }
        public void setLogicScore(int score) { this.logicScore = Math.min(20, Math.max(0, score)); }

        public int getSpecificityScore() { return specificityScore; }
        public void setSpecificityScore(int score) { this.specificityScore = Math.min(20, Math.max(0, score)); }

        public String getDimension() { return dimension; }
        public void setDimension(String dimension) { this.dimension = dimension; }

        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }

        @Override
        public String toString() {
            return String.format("维度：%s\n得分：%d\n- 相关性：%d/30\n- 完整性：%d/30\n- 逻辑性：%d/20\n- 具体性：%d/20\n反馈：%s",
                    dimension, getTotalScore(), relevanceScore, completenessScore, logicScore, specificityScore, feedback);
        }
    }

    /**
     * STAR分析结果数据类
     */
    public static class StarAnalysisResult {
        private double situationScore;
        private double taskScore;
        private double actionScore;
        private double resultScore;
        private String starAnalysis;
        private String improvements;

        // Getters and Setters
        public double getSituationScore() { return situationScore; }
        public void setSituationScore(double score) { this.situationScore = Math.min(100.0, Math.max(0.0, score)); }

        public double getTaskScore() { return taskScore; }
        public void setTaskScore(double score) { this.taskScore = Math.min(100.0, Math.max(0.0, score)); }

        public double getActionScore() { return actionScore; }
        public void setActionScore(double score) { this.actionScore = Math.min(100.0, Math.max(0.0, score)); }

        public double getResultScore() { return resultScore; }
        public void setResultScore(double score) { this.resultScore = Math.min(100.0, Math.max(0.0, score)); }

        public String getStarAnalysis() { return starAnalysis; }
        public void setStarAnalysis(String analysis) { this.starAnalysis = analysis; }

        public String getImprovements() { return improvements; }
        public void setImprovements(String improvements) { this.improvements = improvements; }

        public double getAverageScore() {
            return (situationScore + taskScore + actionScore + resultScore) / 4.0;
        }
    }
} 