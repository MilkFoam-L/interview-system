# 枢鉴-智能面试系统

![项目版本](https://img.shields.io/badge/version-0.0.1--SNAPSHOT-blue)
![Java版本](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)
![Vue.js](https://img.shields.io/badge/Vue.js-3.4-42b883)

## 项目介绍

枢鉴-智能面试系统是一个基于Spring Boot和Vue.js的智能面试平台，通过集成讯飞AI能力（讯飞星火大模型X1.5、语音识别、语音合成、人脸比对），为面试官和候选人提供全方位的在线面试解决方案。系统支持完整的四阶段面试流程：自我介绍 → 基础问答 → 场景模拟 → 代码实操，并提供AI驱动的综合评估报告。

### 面试流程

1. **准备阶段**：设备检测（摄像头/麦克风/网络）+ 人脸识别验证
2. **自我介绍**（60秒）：语音转写 + 表情捕获 + AI分析
3. **基础问答**（10道选择题）：按岗位智能出题 + AI答题分析
4. **场景模拟**（5轮对话）：AI面试官根据简历提问 + STAR法则评估
5. **代码实操**（3道编程题）：在线编码 + AI代码评判

### 主要功能

- **用户管理**：支持用户注册、登录、个人信息管理，区分候选人和面试官角色
- **简历解析**：自动解析候选人上传的简历（支持Word和文本格式）
- **人脸验证**：基于讯飞新版API（api.xf-yun.com）的人脸比对验证
- **在线面试**：支持实时视频面试，包括人脸检测和表情分析
- **AI辅助**：利用讯飞星火X1.5大模型（spark-x）生成面试问题和评估回答
- **语音识别**：讯飞实时语音转写（RTASR），支持自我介绍和场景问答的语音输入
- **语音合成**：讯飞TTS语音合成，面试各环节语音指引（支持跳过）
- **智能评分**：12维能力矩阵评估 + 综合报告 + 个性化建议
- **数据分析**：面试结果统计和可视化展示（ECharts）
- **企业管理**：数据中心、岗位管理、题库管理、人才管理四大模块

## 系统架构

### 后端技术栈

- **基础框架**：Spring Boot 3.5.3
- **ORM框架**：Spring Data JPA
- **安全框架**：Spring Security
- **数据库**：MySQL 8.0
- **API文档**：SpringDoc OpenAPI (Swagger UI)
- **缓存**：Redis（Jedis）
- **消息队列**：Apache Kafka
- **AI集成**：讯飞开放平台API（星火X1.5大模型、语音识别、语音合成、人脸比对）
- **WebSocket**：Spring WebSocket + STOMP
- **工具库**：Lombok、Apache POI、OkHttp、Gson/FastJSON

### 前端技术栈

- **基础框架**：Vue.js 3.4
- **UI组件库**：Element Plus 2.5.6
- **状态管理**：Pinia 2.3.1
- **路由管理**：Vue Router 4.2.5
- **HTTP客户端**：Axios
- **可视化图表**：ECharts 5.6.0
- **WebSocket客户端**：SockJS 1.6.1 + STOMP.js 2.3.3
- **动画效果**：GSAP 3.13.0
- **构建工具**：Vite 5.0.11

## 详细配置说明

### 后端配置文件

系统主要配置集中在`application.properties`文件中，以下是关键配置项：

#### 1. 数据库配置
```properties
# 数据库连接配置
spring.datasource.url=jdbc:mysql://your-database-server:3306/interview_cloud?useSSL=true&serverTimezone=Asia/Shanghai
spring.datasource.username=your-username
spring.datasource.password=your-password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA配置
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

#### 2. 讯飞API配置

> 配置文件模板见 `application.properties.example`，复制后填入真实密钥即可。

```properties
# 讯飞星火大模型配置（场景问答用）
xfyun.spark.appId=your-app-id
xfyun.spark.apiKey=your-api-key
xfyun.spark.apiSecret=your-api-secret
xfyun.spark.hostUrl=https://spark-api.xf-yun.com/v4.0/chat
xfyun.spark.domain=4.0Ultra

# 讯飞星火X1.5开放API（AI评估/判题用，model必须为spark-x）
xfyun.spark-x1.api-password=your-api-password
xfyun.spark-x1.host-url=https://spark-api-open.xf-yun.com/v2/chat/completions
xfyun.spark-x1.model=spark-x

# 讯飞语音合成TTS配置
xfyun.tts.appId=your-app-id
xfyun.tts.apiKey=your-api-key
xfyun.tts.apiSecret=your-api-secret

# 讯飞实时语音转写RTASR配置（需确认appId已开通RTASR服务）
xunfei.rtasr-app-id=your-app-id
xunfei.rtasr-api-key=your-api-key

# 讯飞人脸检测/比对配置（使用新版api.xf-yun.com + HMAC-SHA256鉴权）
xunfei.face-detect.app-id=your-app-id
xunfei.face-detect.api-key=your-api-key
xunfei.face-detect.api-secret=your-api-secret
xunfei.face-detect.service-id=your-service-id
xunfei.face-detect.request-url=https://api.xf-yun.com/v1/private/your-service-id
```

#### 2.5 Kafka & Redis 配置
```properties
# Kafka
spring.kafka.bootstrap-servers=your-kafka-host:9092
spring.kafka.consumer.group-id=interview-system-group

# Redis
spring.data.redis.host=127.0.0.1
spring.data.redis.port=6379
spring.data.redis.password=your-redis-password
```

#### 3. 服务器和安全配置
```properties
# 服务器端口配置
server.port=8081

# 跨域配置
spring.mvc.cors.allowed-origins=*
spring.mvc.cors.allowed-methods=GET,POST,PUT,DELETE
spring.mvc.cors.allowed-headers=*

# 文件上传限制
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# 会话配置
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.name=INTERVIEW_SESSION
spring.session.store-type=jdbc
```

#### 4. 日志配置
```properties
# 日志级别配置
logging.level.root=WARN
logging.level.interview=INFO
logging.level.interview.controller=INFO
logging.level.interview.service=INFO
logging.pattern.console=%d{HH:mm:ss} %-5level %logger{20} - %msg%n
logging.charset.console=UTF-8
```

### 后端关键配置类

#### 1. XunfeiApiConfig.java
负责管理讯飞API的配置参数，采用Spring Boot的`@ConfigurationProperties`注解自动绑定配置文件中的属性。

```java
@Data
@Component
@ConfigurationProperties(prefix = "xfyun")
public class XunfeiApiConfig {
    private SparkConfig spark = new SparkConfig();
    private TtsConfig tts = new TtsConfig();
    private RtasrConfig rtasr = new RtasrConfig();
    
    @Data
    public static class SparkConfig {
        private String appId;
        private String apiKey;
        private String apiSecret;
        private String hostUrl;
        private String domain;
    }
    
    // 其他配置类...
}
```

#### 2. WebConfig.java
处理Web相关配置，包括CORS跨域和静态资源映射：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 简历照片映射
        registry.addResourceHandler("/api/resume/photos/**")
                .addResourceLocations("file:interview_backend/uploads/photos/");
                
        // 用户头像映射
        registry.addResourceHandler("/api/avatars/**")
                .addResourceLocations("file:interview_backend/uploads/avatars/");
    }
    
    // 跨域配置等...
}
```

#### 3. SecurityConfig.java
处理安全相关配置，包括认证、授权等：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/users/login", "/api/users/register").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }
    
    // 其他安全配置...
}
```

## API调用说明

### 后端API概览

系统API主要分为以下几个模块：

#### 1. 用户管理API
- `POST /api/users/register` - 用户注册
- `POST /api/users/login` - 用户登录
- `GET /api/users/current` - 获取当前登录用户
- `POST /api/users/upload-avatar` - 上传用户头像

#### 2. 面试相关API
- `GET /api/interview-sessions` - 获取面试会话列表
- `POST /api/interview-sessions` - 创建新的面试会话
- `GET /api/interview-sessions/{id}` - 获取面试会话详情

#### 3. 简历管理API
- `POST /api/resumes/upload` - 上传简历
- `GET /api/resumes/{id}` - 获取简历详情
- `GET /api/resumes/parse` - 解析简历

#### 4. AI服务API
- `POST /api/tts` - 文本转语音
- `POST /api/scenario/question` - 生成面试问题
- `POST /api/face-verification` - 人脸验证

#### 5. 数据统计API
- `GET /api/statistics/interviews` - 获取面试统计数据
- `GET /api/statistics/performance/{userId}` - 获取用户表现统计

### API调用示例

#### 1. 用户登录

**请求示例：**
```bash
curl -X POST http://localhost:8081/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"username":"example", "password":"password123"}'
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userId": 123,
    "username": "example"
  }
}
```

#### 2. 获取面试问题

**请求示例：**
```bash
curl -X POST http://localhost:8081/api/scenario/question \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{"jobPosition":"Java开发工程师", "difficulty":"medium", "questionType":"technical"}'
```

**响应：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "question": "请描述Java中的多线程安全问题及常见解决方案?",
    "analysis": "这个问题考察候选人对并发编程的理解..."
  }
}
```

#### 3. 文本转语音

**请求示例：**
```bash
curl -X POST http://localhost:8081/api/tts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{"text":"欢迎参加本次面试"}'
```

**响应：**
返回音频二进制数据流

## 前端使用说明

### 前端目录结构

```
interview_frontend/
├── public/                   # 静态资源
├── src/                      # 源代码目录
│   ├── api/                  # API接口封装
│   ├── assets/               # 静态资源
│   ├── components/           # 公共组件
│   ├── layout/               # 布局组件
│   ├── router/               # 路由配置
│   ├── stores/               # Pinia状态管理
│   ├── utils/                # 工具函数
│   ├── views/                # 页面组件
│   ├── App.vue               # 根组件
│   └── main.js               # 入口文件
├── package.json              # 项目依赖
└── vite.config.js            # Vite配置
```

### 前端API调用

前端使用封装的Axios实例进行API调用，主要通过`@/utils/request.js`统一处理请求和响应。

#### 请求封装示例

```javascript
// src/api/tts.js
import request from '@/utils/request'

export function fetchTtsAudio(text) {
  return request({
    url: '/api/tts',
    method: 'post',
    data: { text },
    responseType: 'arraybuffer',
  });
}
```

#### 使用示例

```javascript
// 在Vue组件中使用
import { fetchTtsAudio } from '@/api/tts';

export default {
  methods: {
    async playAudio() {
      try {
        const response = await fetchTtsAudio('欢迎参加面试');
        const audioBlob = new Blob([response], { type: 'audio/mp3' });
        const audioUrl = URL.createObjectURL(audioBlob);
        const audio = new Audio(audioUrl);
        audio.play();
      } catch (error) {
        console.error('播放音频失败:', error);
      }
    }
  }
}
```

### 用户认证和授权

前端使用拦截器自动处理认证信息：

```javascript
// src/utils/request.js (部分代码)
service.interceptors.request.use(
  config => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  }
)

// 响应拦截器处理登录过期
service.interceptors.response.use(
  response => { /* ... */ },
  async error => {
    // 401错误处理
    if (error.response && error.response.status === 401) {
      await handleUnauthorized('登录已过期')
    }
    return Promise.reject(error)
  }
)
```

## 快速开始

### 系统要求

- JDK 17+
- Maven 3.8+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+
- Apache Kafka 3.0+（可选，用于异步消息处理）

### 后端部署

1. 克隆仓库到本地：
   ```bash
   git clone https://github.com/your-username/interview-system.git
   cd interview-system
   ```

2. 复制配置模板并填入真实密钥：
   ```bash
   cd interview_backend/src/main/resources
   cp application.properties.example application.properties
   # 编辑 application.properties，填入数据库、Redis、Kafka、讯飞API等凭证
   ```

3. 注意事项：
   - `application.properties` 包含敏感密钥，已被 `.gitignore` 排除，切勿提交
   - 讯飞各服务（TTS/RTASR/人脸/星火）需在对应appId下分别开通
   - 星火X1.5的model值必须为 `spark-x`

4. 编译并运行后端：
   ```bash
   cd interview_backend
   mvn clean install
   mvn spring-boot:run
   ```
   后端默认将在 8081 端口启动。

### 前端部署

1. 安装依赖：
   ```bash
   cd interview_frontend
   npm install
   ```

2. 开发环境运行：
   ```bash
   npm run dev
   ```
   前端开发服务器将在 http://localhost:5173 启动

3. 生产环境构建：
   ```bash
   npm run build
   ```

## API文档

启动后端服务后，可通过以下地址访问Swagger UI API文档：
```
http://localhost:8081/swagger-ui/index.html
```

## 项目结构

```
interview-system/
├── interview_backend/              # 后端项目
│   ├── src/main/java/interview/    # Java源代码
│   │   ├── ai/                     # AI相关服务
│   │   ├── config/                 # 配置类
│   │   ├── controller/             # API控制器
│   │   ├── dto/                    # 数据传输对象
│   │   ├── model/                  # 数据模型
│   │   ├── repository/             # 数据访问层
│   │   ├── service/                # 业务逻辑层
│   │   ├── util/                   # 工具类
│   │   └── websocket/              # WebSocket相关
│   ├── src/main/resources/         # 资源文件
│   └── uploads/                    # 上传文件存储
│       ├── avatars/                # 用户头像
│       └── photos/                 # 简历照片
├── interview_frontend/             # 前端项目
│   ├── public/                     # 静态资源
│   └── src/                        # Vue源代码
│       ├── assets/                 # 图片等资源
│       ├── components/             # Vue组件
│       ├── router/                 # 路由配置
│       ├── store/                  # 状态管理
│       └── views/                  # 页面视图
└── README.md                       # 项目文档
```

## 贡献指南

1. Fork本仓库
2. 创建你的特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交你的改动 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建新的Pull Request

## 许可证

该项目采用 [MIT](https://opensource.org/licenses/MIT) 许可证。

## 联系方式

如有任何问题或建议，请联系：
- 项目维护者: 唐啟泰 (milkfoam163@gmail.com)
