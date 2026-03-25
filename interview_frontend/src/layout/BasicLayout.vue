// ... existing code ...
<template>
  <div class="fin-layout">
    <!-- 侧边栏 -->
    <aside class="fin-sidebar" :class="{ collapsed: isCollapse }">
      <div class="sidebar-logo">
        <img src="../assets/logo.png" alt="Logo" class="logo-img" />
        <span class="logo-text" v-show="!isCollapse">面试系统</span>
      </div>
      <nav class="sidebar-menu">
        <template v-for="item in menuItems" :key="item.path">
          <!-- 菜单项 -->
          <div
              :class="['sidebar-menu-item', { active: activeMenu === item.path || isSubMenuActive(item) }]"
              @click="handleMenuItemClick(item)"
          >
            <div class="menu-item-content">
              <el-icon :size="22">
                <DataLine v-if="item.icon === 'DataLine'" />
                <Suitcase v-else-if="item.icon === 'Suitcase'" />
                <VideoCamera v-else-if="item.icon === 'VideoCamera'" />
                <PieChart v-else-if="item.icon === 'PieChart'" />
                <User v-else-if="item.icon === 'User'" />
                <Search v-else-if="item.icon === 'Search'" />
                <OfficeBuilding v-else-if="item.icon === 'OfficeBuilding'" />
                <DataLine v-else />
              </el-icon>
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
                <el-icon :size="16">
                  <Search v-if="child.icon === 'Search'" />
                  <OfficeBuilding v-else-if="child.icon === 'OfficeBuilding'" />
                  <DataLine v-else />
                </el-icon>
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
              />
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goProfile">个人中心</el-dropdown-item>
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
        <router-view />
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
<script setup>
import { ref, computed, onMounted, reactive, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataLine, VideoCamera, User, Suitcase, Fold, Expand, PieChart, Bell, Setting, Menu, SwitchButton, Plus, ArrowDown, Search, OfficeBuilding } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import auth from '@/utils/auth'
import axios from 'axios'
import DefaultAvatar from '@/components/DefaultAvatar.vue'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)
const expandedMenus = ref([]) // 存储已展开的菜单

// 默认头像
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iIzYzNjZmMSI+PHBhdGggZD0iTTEyIDJDNi40OCAyIDIgNi40OCAyIDEyczQuNDggMTAgMTAgMTAgMTAtNC40OCAxMC0xMFMxNy41MiAyIDEyIDJ6bTAgM2MyLjY3IDAgOC40IDEuMzQgOCA0djNjMCAyLjY2LTUuMzMgNC04IDRzLTgtMS4zNC04LTR2LTNjMC0yLjY2IDUuMzMtNCA4LTR6bTAgMTVjLTIuMzIgMC00LjQ1LS44LTYuMTQtMi4xMkM3LjQ5IDE2LjAyIDkuNTkgMTUgMTIgMTVzNC41MSAxLjAyIDYuMTQgMi44OEM1LjQ1IDE5LjIgMTQuMzIgMjAgMTIgMjB6bTAtN2MtMS4xMSAwLTItLjktMi0yIDAtMS4xMS44OS0yIDItMnMyIC44OSAyIDJjMCAxLjEtLjkgMi0yIDJ6Ii8+PC9zdmc+';

// 用户信息
const userInfo = ref({
  id: null,
  username: '',
  realName: '',
  avatarUrl: '',
  email: '',
  role: ''
})

// 显示名称优先使用真实姓名
const displayName = computed(() => {
  return userInfo.value.realName || userInfo.value.username || '用户'
})

// 头像加载错误时的处理
const avatarLoadError = () => {
  console.error('头像加载失败:', userInfo.value.avatarUrl);
  // 使用默认头像
  userInfo.value.avatarUrl = '';
}

// 修正头像URL路径
const fixAvatarUrl = (url) => {
  if (!url) return '';

  // 如果已经是完整的URL，则直接返回
  if (url.startsWith('http') || url.startsWith('data:')) {
    return url;
  }

  // 如果是相对路径但不是以/api开头，添加/api前缀
  if (url.startsWith('/avatars/') && !url.startsWith('/api/')) {
    console.log('修正头像URL路径:', url, ' => ', '/api' + url);
    return '/api' + url;
  }

  return url;
}

const menuItems = [
  { path: '/', label: '首页', icon: 'DataLine' },
  {
    path: '/jobs',
    label: '岗位选择',
    icon: 'Suitcase',
    children: [
      { path: '/jobs', label: '岗位搜索', icon: 'Search' },
      { path: '/companies', label: '公司列表', icon: 'OfficeBuilding' }
    ]
  },
  { path: '/interview', label: '进入面试', icon: 'VideoCamera' },
  { path: '/report', label: '面试报告', icon: 'PieChart' },
  { path: '/profile', label: '个人中心', icon: 'User' }
]

// 过滤出顶层菜单项，用于显示
const filteredMenuItems = computed(() => {
  return menuItems;
})

// 判断子菜单是否激活
const isSubMenuActive = (item) => {
  if (!item.children) return false;
  return item.children.some(child => child.path === route.path);
}

const activeMenu = computed(() => {
  // 对于/report路径，确保菜单项高亮
  if (route.path === '/report') {
    return '/report';
  }
  return route.path;
})

const breadcrumbs = computed(() => {
  if (route.path === '/') {
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
})

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value;

  // 如果是折叠状态，清空展开的菜单
  if (isCollapse.value) {
    expandedMenus.value = [];
  }
}

// 处理菜单项点击
const handleMenuItemClick = (item) => {
  if (!item || !item.path) {
    console.error('菜单项数据无效:', item)
    return
  }
  
  if (item.children) {
    // 如果有子菜单，则展开/收起子菜单
    if (expandedMenus.value.includes(item.path)) {
      expandedMenus.value = expandedMenus.value.filter(path => path !== item.path);
    } else {
      // close other open menus
      // expandedMenus.value = [item.path];
      expandedMenus.value.push(item.path);
    }
  } else {
    // 如果没有子菜单，则直接跳转
    handleMenuClick(item.path);
  }
}

const handleMenuClick = async (path) => {
  try {
    await nextTick()
    await router.push(path)
  } catch (error) {
    console.error('路由跳转失败:', error)
    if (error.name !== 'NavigationDuplicated') {
      ElMessage.error('页面跳转失败，请重试')
    }
  }
}

const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 自定义密码确认校验规则
const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

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
}

// 头像上传相关
const avatarDialogVisible = ref(false)
const avatarFile = ref(null)
const avatarPreview = ref('')

// 显示头像上传对话框
const openChangeAvatarDialog = () => {
  avatarDialogVisible.value = true
  avatarPreview.value = userInfo.value.avatarUrl || ''
}

// 处理头像变化
const handleAvatarChange = (file) => {
  avatarFile.value = file
  avatarPreview.value = URL.createObjectURL(file.raw)
}

// 提交头像
const submitAvatar = async () => {
  if (!avatarFile.value) {
    ElMessage.warning('请先选择头像图片')
    return
  }

  try {
    // 创建FormData对象上传文件
    const formData = new FormData()
    formData.append('file', avatarFile.value.raw)

    console.log('准备上传头像，文件名:', avatarFile.value.name);

    // 使用文件上传接口上传头像
    const res = await axios.post('/api/users/upload-avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        // 添加用户ID头
        'X-User-ID': userInfo.value.id
      },
      // 添加超时时间
      timeout: 60000
    })

    console.log('头像上传原始响应:', res);

    // 从axios响应中提取数据
    const data = res.data;
    console.log('头像上传成功，返回数据:', data);

    if (!data || !data.avatarUrl) {
      throw new Error('上传成功但未返回头像URL');
    }

    // 获取返回的头像URL
    const avatarUrl = data.avatarUrl;
    console.log('设置新头像URL:', avatarUrl);

    // 更新本地状态
    userInfo.value.avatarUrl = avatarUrl;

    // 更新用户缓存
    const user = auth.getUserInfo()
    if (user) {
      user.avatarUrl = avatarUrl
      auth.setUserInfo(user)
    }

    ElMessage.success('头像更新成功')

    // 关闭对话框
    avatarDialogVisible.value = false

    // 清除文件选择
    avatarFile.value = null;
    avatarPreview.value = '';
  } catch (error) {
    console.error('上传头像失败:', error)
    ElMessage.error('上传头像失败: ' + (error.response?.data?.error || error.message || '未知错误'))
  }
}

// 提交修改密码
const submitChangePassword = async () => {
  if (!passwordFormRef.value) return

  try {
    // 表单验证
    await passwordFormRef.value.validate()

    // 检查新密码是否与旧密码相同
    if (passwordForm.oldPassword === passwordForm.newPassword) {
      ElMessage.error('新密码不能与旧密码相同')
      return
    }

    // 调用接口修改密码
    await request.post('/api/users/change-password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    ElMessage.success('密码修改成功')

    // 关闭弹窗并重置表单
    passwordDialogVisible.value = false
    passwordFormRef.value.resetFields()

  } catch (error) {
    console.error('修改密码失败:', error)
    ElMessage.error(error.response?.data?.error || '修改密码失败')
  }
}

const handleLogout = () => {
  // 使用auth工具退出登录
  auth.logout('您已成功退出登录')
}

// 头部交互相关
const notifications = ref([
  { id: 1, content: '有新的面试邀请', time: '1小时前' },
  { id: 2, content: '面试结果已发布', time: '昨天' }
])
function openThemeDialog() {
  window.alert('主题切换功能开发中...')
}

function openChangePwdDialog() {
  passwordDialogVisible.value = true
}

const goProfile = async () => {
  try {
    await router.push('/profile')
  } catch (error) {
    console.error('跳转个人中心失败:', error)
    if (error.name !== 'NavigationDuplicated') {
      ElMessage.error('页面跳转失败，请重试')
    }
  }
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const res = await request.get('/api/users/current')
    if (res) {
      userInfo.value = {
        id: res.id,
        username: res.username,
        realName: res.realName,
        avatarUrl: fixAvatarUrl(res.avatarUrl),
        email: res.email,
        role: res.role
      }

      // 更新本地存储的用户信息
      const user = auth.getUserInfo()
      if (user) {
        user.username = res.username
        user.realName = res.realName
        user.avatarUrl = fixAvatarUrl(res.avatarUrl)
        auth.setUserInfo(user)
      }

      // 检查头像是否存在
      checkAvatarExists();
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 检查头像是否存在
const checkAvatarExists = async () => {
  if (!userInfo.value.avatarUrl) return;

  try {
    console.log('检查头像是否存在:', userInfo.value.avatarUrl);
    const res = await request.get('/api/users/check-avatar');
    console.log('头像检查结果:', res);

    if (res && !res.exists) {
      console.warn('头像文件不存在，使用默认头像');
      userInfo.value.avatarUrl = '';
    }
  } catch (error) {
    console.error('检查头像失败:', error);
    // 接口不存在或其他错误时，不做处理，保留现有头像URL
  }
}

onMounted(() => {
  // 获取用户信息
  fetchUserInfo()
})
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
  gap: 12px;
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

/* 头像相关样式 */
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
</style>