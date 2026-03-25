<template>
  <div class="fin-layout">
    <!-- 侧边栏 -->
    <aside class="fin-sidebar" :class="{ collapsed: isCollapse }">
      <div class="sidebar-logo">
        <img src="../assets/logo.png" alt="Logo" class="logo-img" />
        <span class="logo-text" v-show="!isCollapse">企业后台</span>
      </div>
      <nav class="sidebar-menu">
        <template v-for="item in menuItems" :key="item.path">
          <!-- 菜单项 -->
          <div
              :class="['sidebar-menu-item', { active: activeMenu === item.path || isSubMenuActive(item) }]"
              @click="handleMenuItemClick(item)"
          >
            <div class="menu-item-content">
              <el-icon :size="22"><component :is="item.icon" /></el-icon>
              <span v-show="!isCollapse">{{ item.label }}</span>
              <el-icon v-if="item.children && !isCollapse" class="expand-icon" :class="{ 'is-expanded': expandedMenus.includes(item.path) }">
                <ArrowDown />
              </el-icon>
            </div>
          </div>

          <!-- 子菜单 -->
          <el-collapse-transition>
            <div class="submenu-wrapper" v-show="!isCollapse && item.children && expandedMenus.includes(item.path)">
              <div
                  v-for="child in item.children"
                  :key="child.path"
                  :class="['sidebar-submenu-item', { active: activeMenu === child.path }]"
                  @click.stop="handleMenuClick(child.path)"
              >
                <span>{{ child.label }}</span>
              </div>
            </div>
          </el-collapse-transition>
        </template>
      </nav>
      <div class="sidebar-bottom">
        <el-icon class="sidebar-bottom-icon" @click="handleLogout"><SwitchButton /></el-icon>
      </div>
    </aside>
    <!-- 主体 -->
    <div class="fin-main">
      <!-- 头部导航栏 -->
      <header class="fin-header">
        <div class="header-left">
          <el-icon class="header-collapse-btn" @click="toggleSidebar">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <span class="header-breadcrumb">
            <el-icon><Menu /></el-icon>
            <b>{{ breadcrumbs[0] }}</b>
            <template v-if="breadcrumbs.length > 1">
              / {{ breadcrumbs[1] }}
            </template>
            <template v-if="breadcrumbs.length > 2">
              / {{ breadcrumbs[2] }}
            </template>
          </span>
        </div>
        <div class="header-right">
          <!-- 通知 -->
          <el-dropdown>
            <el-button circle class="header-icon-btn">
              <el-icon><Bell /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in notifications" :key="item.id" disabled style="white-space:normal;">
                  <div style="display:flex;justify-content:space-between;align-items:center;width:200px;">
                    <span>{{ item.content }}</span>
                    <span style="color:#888;font-size:13px;">{{ item.time }}</span>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item v-if="notifications.length === 0" disabled>
                  暂无通知
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <!-- 设置 -->
          <el-dropdown>
            <el-button circle class="header-icon-btn">
              <el-icon><Setting /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="openThemeDialog">主题切换</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <!-- 头像/用户名 -->
          <el-dropdown>
            <div style="cursor:pointer;">
              <el-avatar
                  v-if="userInfo.avatarUrl"
                  :size="36"
                  :src="userInfo.avatarUrl"
                  @error="avatarLoadError"
              />
              <DefaultAvatar
                  v-else
                  :username="displayName"
                  :size="36"
                  color="#264653"
              />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goCompanyInfo">企业信息</el-dropdown-item>
                <el-dropdown-item @click="openChangeAvatarDialog">修改头像</el-dropdown-item>
                <el-dropdown-item @click="openChangePwdDialog">修改密码</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <span class="user-name">{{ displayName }}</span>
        </div>
      </header>
      <main class="fin-content">
        <router-view></router-view>
      </main>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog
        title="修改密码"
        v-model="passwordDialogVisible"
        width="400px"
    >
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input type="password" v-model="passwordForm.oldPassword" placeholder="请输入旧密码"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input type="password" v-model="passwordForm.newPassword" placeholder="请输入新密码"></el-input>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input type="password" v-model="passwordForm.confirmPassword" placeholder="请确认新密码"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitChangePassword">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 上传头像弹窗 -->
    <el-dialog
        title="修改头像"
        v-model="avatarDialogVisible"
        width="400px"
    >
      <div class="avatar-upload-container">
        <el-upload
            class="avatar-uploader"
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleAvatarChange"
        >
          <img v-if="avatarPreview" :src="avatarPreview" class="avatar-preview" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <div class="avatar-tips">点击上方区域选择图片</div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="avatarDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAvatar" :disabled="!avatarFile">上传</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { 
  User, 
  Calendar, 
  DataAnalysis, 
  Setting, 
  Briefcase, 
  Files, 
  Expand, 
  Fold,
  ArrowDown,
  SwitchButton,
  Menu,
  Bell,
  Plus
} from '@element-plus/icons-vue';
import { getUserInfo } from '@/utils/auth';
import auth from '@/utils/auth';
import request from '@/utils/request';
import axios from 'axios';
import DefaultAvatar from '@/components/DefaultAvatar.vue';

const route = useRoute();
const router = useRouter();
const isCollapse = ref(false);
const expandedMenus = ref<string[]>([]); // 存储已展开的菜单
const userInfo = ref<any>({
  id: null,
  username: '',
  realName: '',
  avatarUrl: '',
  email: '',
  role: ''
});

  // 显示名称优先使用用户名
  const displayName = computed(() => {
    const currentUsername = userInfo.value?.username;
    return (currentUsername && currentUsername.trim()) ? currentUsername : '面试官';
  });

// 头像加载错误时的处理
  const avatarLoadError = () => {
  userInfo.value.avatarUrl = '';
}

// 修正头像URL路径
const fixAvatarUrl = (url: string) => {
  if (!url) return '';
  if (url.startsWith('http') || url.startsWith('data:')) {
    return url;
  }
  if (url.startsWith('/avatars/') && !url.startsWith('/api/')) {
    return '/api' + url;
  }
  return url;
}

// 面试官系统菜单项
const menuItems = [
  {
    path: '/interviewer/data-center/recruitment-analysis',
    label: '数据中心',
    icon: 'DataAnalysis'
  },
  {
    path: '/interviewer/position-management/position-list',
    label: '岗位管理',
    icon: 'Briefcase'
  },
  {
    path: '/interviewer/question-bank',
    label: '题库中心',
    icon: 'Files',
    children: [
      { path: '/interviewer/data-center/question-analysis', label: '题库分析看板', icon: 'DataAnalysis' },
      { path: '/interviewer/question-bank/management', label: '题目管理', icon: 'Files' }
    ]
  },
  {
    path: '/interviewer/talent-management/ai-screening',
    label: '人才管理',
    icon: 'User'
  },
  {
    path: '/interviewer/interview-management/schedule',
    label: '面试管理',
    icon: 'Calendar'
  },
  {
    path: '/interviewer/system-settings/company-info',
    label: '企业信息中心',
    icon: 'Setting'
  }
];

// 判断子菜单是否激活
const isSubMenuActive = (item: any) => {
  if (!item.children) return false;
  return item.children.some((child: any) => child.path === route.path);
}

const activeMenu = computed(() => {
  return route.path;
});

const breadcrumbs = computed(() => {
  if (route.path === '/interviewer') {
    return ['首页'];
  }

  // 查找顶级菜单
  const parentMenu = menuItems.find(item => {
    if (item.path === route.path) return true;
    return item.children?.some(child => child.path === route.path);
  });

  if (!parentMenu) return ['首页'];

  // 如果是顶级菜单且没有子菜单
  if (parentMenu.path === route.path && !parentMenu.children) {
    return ['首页', parentMenu.label];
  }

  // 如果是子菜单
  const childMenu = parentMenu.children?.find(child => child.path === route.path);
  if (childMenu) {
    return ['首页', parentMenu.label, childMenu.label];
  }

  return ['首页', parentMenu.label];
});

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value;
  if (isCollapse.value) {
    expandedMenus.value = [];
  }
};

// 处理菜单项点击
const handleMenuItemClick = (item: any) => {
  if (item.children) {
    if (expandedMenus.value.includes(item.path)) {
      expandedMenus.value = expandedMenus.value.filter(path => path !== item.path);
    } else {
      expandedMenus.value.push(item.path);
    }
  } else {
    handleMenuClick(item.path);
  }
};

const handleMenuClick = (path: string) => {
  router.push(path);
};

// 修改密码相关
const passwordDialogVisible = ref(false);
const passwordFormRef = ref(null);
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 自定义密码确认校验规则
const validateConfirmPassword = (rule: any, value: string, callback: Function) => {
  if (value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入密码不一致!'));
  } else {
    callback();
  }
};

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
};

// 头像上传相关
const avatarDialogVisible = ref(false);
const avatarFile = ref(null);
const avatarPreview = ref('');

// 显示头像上传对话框
const openChangeAvatarDialog = () => {
  avatarDialogVisible.value = true;
  avatarPreview.value = userInfo.value.avatarUrl || '';
};

// 处理头像变化
const handleAvatarChange = (file: any) => {
  avatarFile.value = file;
  avatarPreview.value = URL.createObjectURL(file.raw);
};

// 提交头像
const submitAvatar = async () => {
  if (!avatarFile.value) {
    console.error('请先选择头像图片');
    return;
  }

  try {
    const formData = new FormData();
    formData.append('file', (avatarFile.value as any).raw);

    const res = await axios.post('/api/users/upload-avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-User-ID': userInfo.value.id
      },
      timeout: 60000
    });

    const data = res.data;
    if (!data || !data.avatarUrl) {
      throw new Error('上传成功但未返回头像URL');
    }

    const avatarUrl = data.avatarUrl;
    userInfo.value.avatarUrl = avatarUrl;

    const user = auth.getUserInfo();
    if (user) {
      user.avatarUrl = avatarUrl;
      auth.setUserInfo(user);
    }

    console.log('头像更新成功');
    avatarDialogVisible.value = false;
    avatarFile.value = null;
    avatarPreview.value = '';
  } catch (error: any) {
    console.error('上传头像失败:', error);
    console.error('上传头像失败: ' + (error.response?.data?.error || error.message || '未知错误'));
  }
};

// 提交修改密码
const submitChangePassword = async () => {
  if (!passwordFormRef.value) return;

  try {
    await (passwordFormRef.value as any).validate();

    if (passwordForm.oldPassword === passwordForm.newPassword) {
      console.error('新密码不能与旧密码相同');
      return;
    }

    await request.post('/api/users/change-password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    });

    console.log('密码修改成功');
    passwordDialogVisible.value = false;
    (passwordFormRef.value as any).resetFields();

  } catch (error: any) {
    console.error('修改密码失败:', error);
    console.error(error.response?.data?.error || '修改密码失败');
  }
};

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    
    auth.logout('已成功退出登录');
  } catch (error) {
    console.log('用户取消登出');
  }
};

// 头部交互相关
const notifications = ref([
  { id: 1, content: '有新的面试安排', time: '1小时前' },
  { id: 2, content: '审批请求已提交', time: '昨天' }
]);

function openThemeDialog() {
  ElMessage.info('主题切换功能开发中...');
}

function openChangePwdDialog() {
  passwordDialogVisible.value = true;
}

function goCompanyInfo() {
  router.push('/interviewer/system-settings/company-info');
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await request.get('/api/users/current');
    const data = (res && res.data) ? res.data : res;
    if (data) {
      const extractedUsername = String(data.username || '');
      const extractedRealName = String(data.realName || '');
      
      userInfo.value = {
        id: data.id,
        username: extractedUsername,
        realName: extractedRealName,
        avatarUrl: fixAvatarUrl(data.avatarUrl),
        email: data.email || '',
        role: data.role || ''
      };

      const user = auth.getUserInfo();
      if (user) {
        user.username = data.username;
        user.realName = data.realName;
        user.avatarUrl = fixAvatarUrl(data.avatarUrl);
        auth.setUserInfo(user);
        console.log('更新后的本地用户信息:', user);
      }

      checkAvatarExists();
    }
  } catch (error) {
    console.error('获取用户信息失败');
  }
};

// 检查头像是否存在
const checkAvatarExists = async () => {
  if (!userInfo.value.avatarUrl) return;

  try {
    const res = await request.get('/api/users/check-avatar');
    if (res && res.data && !res.data.exists) {
      console.warn('头像文件不存在，使用默认头像');
      userInfo.value.avatarUrl = '';
    }
  } catch (error) {
    console.error('检查头像失败:', error);
  }
};

onMounted(() => {
  fetchUserInfo();
});
</script>

<style scoped>
.fin-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background: linear-gradient(to bottom, #557F93 0%, #BFAFA1 100%);
}
.fin-sidebar {
  width: 210px;
  flex-shrink: 0;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0 12px 0;
  transition: width 0.3s;
}
.fin-sidebar.collapsed {
  width: 64px;
  flex-shrink: 0;
}
.sidebar-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  margin-bottom: 32px;
  padding: 0 12px;
  overflow: hidden;
}
.logo-img {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  transition: margin 0.3s ease;
  object-fit: contain;
  object-position: center;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  padding: 2px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
}
.logo-text {
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  margin-left: 10px;
  letter-spacing: 1px;
  white-space: nowrap;
  transition: opacity 0.3s ease, width 0.3s ease;
  opacity: 1;
  width: auto;
}
.collapsed .logo-img {
  margin-right: 0;
}
.collapsed .logo-text {
  opacity: 0;
  width: 0;
  margin: 0;
}
.sidebar-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.sidebar-menu-item {
  width: 88%;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  color: #ffffff;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  white-space: nowrap;
}

.menu-item-content {
  display: flex;
  align-items: center;
  width: 100%;
  height: 100%;
  padding-left: 18px;
  gap: 14px;
}

.expand-icon {
  margin-left: auto;
  margin-right: 10px;
  transition: transform 0.3s ease;
}

.expand-icon.is-expanded {
  transform: rotate(180deg);
}

.sidebar-submenu-item {
  width: 88%;
  height: 40px;
  margin: 4px auto;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  color: #ffffff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  padding-left: 36px;
  box-sizing: border-box;
  overflow: hidden;
  white-space: nowrap;
}

.submenu-wrapper {
  width: 100%;
}

.sidebar-menu-item span {
  transition: opacity 0.3s ease;
  opacity: 1;
}
.collapsed .sidebar-menu-item {
  justify-content: center;
  padding-left: 0;
  width: 44px;
}
.collapsed .menu-item-content {
  justify-content: center;
  padding-left: 0;
}
.collapsed .sidebar-menu-item span {
  opacity: 0;
  width: 0;
  margin: 0;
}
.sidebar-menu-item.active, .sidebar-menu-item:hover,
.sidebar-submenu-item.active, .sidebar-submenu-item:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
  backdrop-filter: blur(10px);
}
.sidebar-bottom {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-top: 18px;
}
.sidebar-bottom-icon {
  color: #ffffff;
  font-size: 22px;
  cursor: pointer;
  transition: color 0.2s;
}
.sidebar-bottom-icon:hover {
  color: #fff;
}
.fin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.fin-header {
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  height: 64px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 18px;
}
.header-collapse-btn {
  font-size: 22px;
  color: #ffffff;
  cursor: pointer;
  margin-right: 8px;
  transition: color 0.2s;
}
.header-collapse-btn:hover {
  color: #e0e0e0;
}
.header-breadcrumb {
  font-size: 18px;
  color: #ffffff;
  display: flex;
  align-items: center;
  gap: 6px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.header-icon-btn {
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #ffffff;
  margin-right: 2px;
}
.user-name {
  margin-left: 12px;
  color: #ffffff;
  font-size: 18px;
  font-weight: bold;
}
.fin-content {
  flex: 1;
  background: transparent;
  padding: 32px 32px 0 32px;
  min-height: 0;
  overflow: auto;
}
@media (max-width: 900px) {
  .fin-sidebar {
    width: 56px !important;
    min-width: 56px !important;
  }
  .fin-header {
    padding: 0 16px;
  }
  .fin-content {
    padding: 16px 8px 0 8px;
  }
}

.avatar-upload-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.avatar-uploader {
  width: 178px;
  height: 178px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 178px;
  height: 178px;
  line-height: 178px;
  text-align: center;
}

.avatar-preview {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.avatar-tips {
  color: #909399;
  font-size: 14px;
  text-align: center;
  margin-top: 10px;
}

:deep(.el-card) {
  border-radius: 20px !important;
  background: #ffffff !important;
  border: none !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 0 4px 16px rgba(0, 0, 0, 0.04) !important;
}

:deep(.el-card__header) {
  border-bottom: 1px solid #f0f2f5 !important;
  padding: 14px 16px !important;
  font-weight: 700 !important;
  color: #5A6B59 !important;
}

:deep(.el-button--primary) {
  background: #A8B5A0 !important;
  border: 1px solid #9CAF88 !important;
  color: #ffffff !important;
}

:deep(.el-button--primary:hover) {
  background: #9CAF88 !important;
  transform: translateY(-1px);
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper),
:deep(.el-date-editor) {
  background: #ffffff !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06) inset !important;
  border-radius: 12px !important;
}

:deep(.el-table) {
  background: #ffffff !important;
  border-radius: 16px !important;
  overflow: hidden;
}

:deep(.el-table th) {
  background: #f7f9fb !important;
  color: #5A6B59 !important;
  font-weight: 700 !important;
}

:deep(.el-table td) {
  background: #ffffff !important;
}

:deep(.el-tag--success) { background: #EAF3EC !important; border-color: #CFE6D4 !important; color: #3E7A54 !important; }
:deep(.el-tag--warning) { background: #FFF5E5 !important; border-color: #FFE3BF !important; color: #A86B22 !important; }
:deep(.el-tag--info) { background: #EFF4FB !important; border-color: #D9E6F7 !important; color: #3F6799 !important; }
:deep(.el-tag--danger) { background: #FDECEC !important; border-color: #F7C7C7 !important; color: #9D3E3E !important; }

.fin-content::-webkit-scrollbar { width: 8px; height: 8px; }
.fin-content::-webkit-scrollbar-track { background: rgba(255,255,255,0.08); border-radius: 8px; }
.fin-content::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.35); border-radius: 8px; }
.fin-content::-webkit-scrollbar-thumb:hover { background: rgba(255,255,255,0.55); }

.fin-content :deep(.page-header) {
  position: static;
  top: auto;
  z-index: auto;
  margin: 0 0 var(--content-gap) 0;
  padding: 14px 18px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #ffffff;
  min-height: var(--header-h);
}
.fin-content :deep(.page-header h1) { margin: 0 0 4px 0; font-size: 20px; font-weight: 800; color: #ffffff; }
.fin-content :deep(.page-header p) { margin: 0; font-size: 13px; color: rgba(255,255,255,0.9); }

.fin-content :deep(.search-toolbar),
.fin-content :deep(.action-toolbar) {
  margin-bottom: var(--content-gap);
}

.fin-content :deep(.search-toolbar) {
  position: static;
  top: auto;
  z-index: auto;
}

.fin-content :deep(.search-card) {
  position: static;
  top: auto;
  z-index: auto;
}

.fin-content :deep(.filter-card) {
  position: static;
  top: auto;
  z-index: auto;
}

.fin-content :deep(.el-card__body) {
  padding: 20px;
}

.fin-content :deep(.pagination-container) { margin-top: 16px; }
.fin-content :deep(.pagination-container .el-pagination) { justify-content: flex-end; }

.fin-content :deep(.el-table .el-table__cell) { padding: 10px 8px; }
.fin-content :deep(.el-table .cell) { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fin-content :deep(.el-table th .cell) { font-weight: 700; color: #5A6B59; }

@media (max-width: 900px) {
  .fin-content .pagination-container :deep(.el-pagination) {
    --el-pagination-font-size: 12px;
  }
}

.fin-content :deep(.position-grid) { gap: 20px; }
.fin-content :deep(.position-card) {
  border: none !important;
  border-radius: 16px !important;
  background: #ffffff !important;
  box-shadow: 0 6px 24px rgba(0,0,0,0.08), 0 2px 8px rgba(0,0,0,0.04) !important;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.fin-content :deep(.position-card:hover) {
  transform: translateY(-3px);
  box-shadow: 0 12px 32px rgba(0,0,0,0.12), 0 4px 14px rgba(0,0,0,0.06) !important;
}

.fin-content :deep(.el-tag) { margin-right: 6px; }
.fin-content :deep(.el-tag:last-child) { margin-right: 0; }
</style> 