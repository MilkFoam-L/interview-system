package interview.controller;

import interview.model.User;
import interview.model.UserDetail;
import interview.service.UserService;
import interview.service.UserDetailService;
import interview.util.UserContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserDetailService userDetailService;
    
    @Autowired
    private UserContextUtil userContextUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        logger.info("用户注册: {}", user.getUsername());
        try {
            User registeredUser = userService.register(user);
            logger.info("注册成功: {}", user.getUsername());
            return ResponseEntity.ok(registeredUser);
        } catch (RuntimeException e) {
            logger.error("注册失败: {}, 原因: {}", user.getUsername(), e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        logger.info("用户登录: {}", username);
        
        // 验证请求参数
        if (username == null || username.trim().isEmpty() || credentials.get("password") == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "用户名和密码不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            String password = credentials.get("password");
            
            // 调用服务层进行登录验证
            User user = userService.login(username, password);
            
            if (user == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "用户不存在或密码错误");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 检查用户状态
            if (user.getStatus() != null && user.getStatus() == 0) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "账号已被禁用，请联系管理员");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 设置当前用户ID到会话中
            userContextUtil.setCurrentUserId(user.getId());
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            response.put("email", user.getEmail() != null ? user.getEmail() : "");
            response.put("phone", user.getPhone() != null ? user.getPhone() : "");
            response.put("realName", user.getRealName() != null ? user.getRealName() : "");
            
            logger.info("登录成功: {}", username);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("登录失败: {}, 原因: {}", username, e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("登录异常: {}, 错误: {}", username, e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", "登录失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        userContextUtil.clearCurrentUserId();
        logger.info("User logged out");
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "已成功退出登录");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        logger.info("Checking username availability: {}", username);
        boolean exists = userService.existsByUsername(username);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        Long userId = userContextUtil.getCurrentUserId();
        
        if (userId != null) {
            User user = userService.findById(userId);
            if (user != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("username", user.getUsername());
                response.put("role", user.getRole());
                response.put("email", user.getEmail());
                response.put("phone", user.getPhone());
                response.put("realName", user.getRealName());
                response.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
                return ResponseEntity.ok(response);
            }
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "未登录或会话已过期");
        return ResponseEntity.status(401).body(response);
    }
    
    /**
     * 验证用户会话是否有效
     * 前端调用此API检查当前用户是否已登录
     */
    @GetMapping("/validate-session")
    public ResponseEntity<?> validateSession() {
        Long userId = userContextUtil.getCurrentUserId();
        logger.info("验证用户会话，userId: {}", userId);
        
        // 检查是否有用户ID
        if (userId == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "未登录或会话已过期");
            return ResponseEntity.status(401).body(response);
        }
        
        // 从请求头中获取用户ID
        String userIdHeader = userContextUtil.getUserIdFromHeader();
        if (userIdHeader != null) {
            try {
                long headerUserId = Long.parseLong(userIdHeader);
                // 如果请求头中的用户ID与会话中的不匹配，返回错误
                if (headerUserId != userId) {
                    logger.warn("请求头中的用户ID({})与会话中的用户ID({})不匹配", headerUserId, userId);
                    Map<String, String> response = new HashMap<>();
                    response.put("error", "会话验证失败：用户ID不匹配");
                    return ResponseEntity.status(401).body(response);
                }
            } catch (NumberFormatException e) {
                logger.warn("解析请求头中的用户ID失败: {}", userIdHeader);
            }
        }
        
        // 进一步验证用户是否存在
        User user = userService.findById(userId);
        if (user == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "会话验证失败：用户不存在");
            return ResponseEntity.status(401).body(response);
        }
        
        // 返回验证成功的响应
        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("userId", userId);
        response.put("role", user.getRole());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 修改用户密码
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData) {
        Long userId = userContextUtil.getCurrentUserId();
        logger.info("修改密码请求, userId: {}", userId);
        
        // 验证用户是否已登录
        if (userId == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "未登录或会话已过期");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");
            
            // 验证参数
            if (oldPassword == null || newPassword == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "旧密码和新密码不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 调用服务修改密码
            userService.changePassword(userId, oldPassword, newPassword);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "密码修改成功");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("修改密码失败, userId: {}, error: {}", userId, e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 修改用户头像
     */
    @PostMapping("/update-avatar")
    public ResponseEntity<?> updateAvatar(@RequestBody Map<String, String> avatarData) {
        Long userId = userContextUtil.getCurrentUserId();
        logger.info("更新用户头像请求, userId: {}", userId);
        
        // 验证用户是否已登录
        if (userId == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "未登录或会话已过期");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            String avatarUrl = avatarData.get("avatarUrl");
            
            // 验证参数
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "头像URL不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 查找用户
            User user = userService.findById(userId);
            if (user == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "用户不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 更新头像URL
            user.setAvatarUrl(avatarUrl);
            userService.updateUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "头像更新成功");
            response.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("更新头像失败, userId: {}, error: {}", userId, e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 上传用户头像
     */
    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = userContextUtil.getCurrentUserId();
        logger.info("上传用户头像请求, userId: {}, 文件名: {}, 文件大小: {}", userId, file.getOriginalFilename(), file.getSize());
        
        // 验证用户是否已登录
        if (userId == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "未登录或会话已过期");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            // 验证文件类型
            String contentType = file.getContentType();
            logger.info("文件类型: {}", contentType);
            if (contentType == null || (!contentType.equals("image/jpeg") && 
                !contentType.equals("image/png") && !contentType.equals("image/jpg"))) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "只支持JPG和PNG格式的图片");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证文件大小 (限制为2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "头像大小不能超过2MB");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 创建存储目录
            String uploadDir = "interview_backend/uploads/avatars";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("创建目录: {}", uploadPath.toAbsolutePath());
            }
            
            // 获取文件扩展名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            
            // 创建基于用户ID的文件名
            String filename = "user_" + userId + extension;
            
            // 保存文件
            Path targetPath = uploadPath.resolve(filename);
            logger.info("保存文件到: {}", targetPath.toAbsolutePath());
            Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            
            // 更新用户头像URL - 使用绝对路径以确保前端可以访问
            // 注意：这里需要与前端路径保持一致
            String avatarUrl = "/api/avatars/" + filename;
            logger.info("设置头像URL: {}", avatarUrl);
            
            User user = userService.findById(userId);
            user.setAvatarUrl(avatarUrl);
            userService.updateUser(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "头像上传成功");
            response.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            logger.error("上传头像失败, userId: {}, error: {}", userId, e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", "文件上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 检查头像文件是否存在
     */
    @GetMapping("/check-avatar")
    public ResponseEntity<?> checkAvatar() {
        Long userId = userContextUtil.getCurrentUserId();
        logger.info("检查用户头像是否存在, userId: {}", userId);
        
        // 验证用户是否已登录
        if (userId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            response.put("error", "未登录或会话已过期");
            return ResponseEntity.ok(response);
        }
        
        try {
            // 查找用户
            User user = userService.findById(userId);
            if (user == null || user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("exists", false);
                return ResponseEntity.ok(response);
            }
            
            // 获取头像文件名
            String avatarUrl = user.getAvatarUrl();
            String filename = avatarUrl.substring(avatarUrl.lastIndexOf("/") + 1);
            
            // 检查文件是否存在
            String uploadDir = "interview_backend/uploads/avatars";
            Path filePath = Paths.get(uploadDir, filename);
            boolean exists = Files.exists(filePath);
            
            logger.info("头像文件 {} {}存在, 路径: {}", filename, exists ? "" : "不", filePath.toAbsolutePath());
            
            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);
            response.put("avatarUrl", avatarUrl);
            response.put("filePath", filePath.toAbsolutePath().toString());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("检查头像失败, userId: {}, error: {}", userId, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 获取用户详细信息
     */
    @GetMapping("/details")
    public ResponseEntity<?> getUserDetails() {
        Long userId = userContextUtil.getCurrentUserId();
        logger.info("获取用户详细信息, userId: {}", userId);
        
        // 验证用户是否已登录
        if (userId == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "未登录或会话已过期");
            return ResponseEntity.status(401).body(response);
        }
        
        // 获取用户详情
        UserDetail userDetail = userDetailService.getUserDetailByUserId(userId);
        
        if (userDetail == null) {
            // 如果没有详情记录，创建一个空的详情对象
            userDetail = new UserDetail();
            userDetail.setUserId(userId);
            logger.info("用户没有详情记录，返回空对象, userId: {}", userId);
        }
        
        return ResponseEntity.ok(userDetail);
    }
    
    /**
     * 更新用户资料(包括基本信息和详细信息)
     */
    @PostMapping("/update-profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, Object> profileData) {
        Long userId = userContextUtil.getCurrentUserId();
        logger.info("更新用户资料, userId: {}", userId);
        
        // 验证用户是否已登录
        if (userId == null) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "未登录或会话已过期");
            return ResponseEntity.status(401).body(response);
        }
        
        try {
            // 更新基本信息
            @SuppressWarnings("unchecked")
            Map<String, String> basicInfo = (Map<String, String>) profileData.get("basicInfo");
            if (basicInfo != null) {
                User user = userService.findById(userId);
                
                if (user == null) {
                    Map<String, String> response = new HashMap<>();
                    response.put("error", "用户不存在");
                    return ResponseEntity.badRequest().body(response);
                }
                
                // 更新用户基本信息
                if (basicInfo.get("realName") != null) {
                    user.setRealName(basicInfo.get("realName"));
                }
                
                if (basicInfo.get("email") != null) {
                    user.setEmail(basicInfo.get("email"));
                }
                
                if (basicInfo.get("phone") != null) {
                    user.setPhone(basicInfo.get("phone"));
                }
                
                userService.updateUser(user);
                logger.info("用户基本信息已更新, userId: {}", userId);
            }
            
            // 更新详细信息
            @SuppressWarnings("unchecked")
            Map<String, String> detailInfo = (Map<String, String>) profileData.get("detailInfo");
            if (detailInfo != null) {
                UserDetail userDetail = new UserDetail();
                
                if (detailInfo.get("jobTitle") != null) {
                    userDetail.setJobTitle(detailInfo.get("jobTitle"));
                }
                
                if (detailInfo.get("jobIntention") != null) {
                    userDetail.setJobIntention(detailInfo.get("jobIntention"));
                }
                
                if (detailInfo.get("experience") != null) {
                    userDetail.setExperience(detailInfo.get("experience"));
                }
                
                if (detailInfo.get("education") != null) {
                    userDetail.setEducation(detailInfo.get("education"));
                }
                
                if (detailInfo.get("location") != null) {
                    userDetail.setLocation(detailInfo.get("location"));
                }
                
                if (detailInfo.get("skills") != null) {
                    userDetail.setSkills(detailInfo.get("skills"));
                }
                
                userDetailService.updateUserDetail(userId, userDetail);
                logger.info("用户详细信息已更新, userId: {}", userId);
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "用户资料更新成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("更新用户资料失败, userId: {}, error: {}", userId, e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 