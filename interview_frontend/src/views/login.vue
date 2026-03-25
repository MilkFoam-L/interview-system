<template>
  <div class="login-page">
    <div class="auth-card" ref="loginBox">
      <!-- 左侧欢迎面板 -->
      <div class="left-pane">
        <h1 class="welcome">Welcome</h1>
        <p class="subtitle">INTRODUCING INTERVIEW SYSTEM</p>
        <img src="@/assets/logo.png" alt="logo" class="logo" />
      </div>

      <!-- 右侧表单面板 -->
      <div class="right-pane">
        <h2 class="form-title">{{ pageTitle }}</h2>

        <div v-if="!isLogin && registrationStep === 'selection'" class="role-selection">
          <p class="role-selection-tip">为了给您提供更精准的服务，请选择您的角色：</p>
          <el-button class="role-btn candidate" @click="selectRegistrationRole('CANDIDATE')">
            <el-icon><User /></el-icon>
            我是求职者
          </el-button>
          <el-button class="role-btn interviewer" @click="selectRegistrationRole('INTERVIEWER')">
            <el-icon><Briefcase /></el-icon>
            我是面试官
          </el-button>
        </div>

        <el-form v-else :model="form" :rules="rules" ref="formRef" label-width="0" class="auth-form">
          <el-form-item label="" prop="username">
            <el-input v-model="form.username" :placeholder="usernamePlaceholder" :prefix-icon="User"></el-input>
          </el-form-item>

          <el-form-item label="" prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" show-password :prefix-icon="Lock"></el-input>
          </el-form-item>

          <template v-if="!isLogin">
            <el-form-item label="" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" show-password :prefix-icon="Lock"></el-input>
            </el-form-item>

            <el-form-item v-if="form.role === 'CANDIDATE'" label="" prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" :prefix-icon="User"></el-input>
            </el-form-item>

            <el-form-item label="" prop="email">
              <el-input v-model="form.email" placeholder="邮箱" :prefix-icon="Message"></el-input>
            </el-form-item>

            <el-form-item label="" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Phone"></el-input>
            </el-form-item>
          </template>
        </el-form>

        <el-button v-if="mode !== 'register_selection'" type="primary" class="submit-btn" @click="handleSubmit" :loading="loading" :disabled="loading">{{ isLogin ? '登录' : '注册' }}</el-button>
        <div class="switch">
          <span @click="handleSwitchClick">{{ switchText }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import gsap from 'gsap'
import { useRoute } from 'vue-router'
import auth from '@/utils/auth'
import { useUserStore } from '@/stores'
import { User, Lock, Message, Phone, Briefcase } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const isLogin = ref(true)
const registrationStep = ref('selection') // 'selection' or 'form'

// 获取重定向参数
const redirectPath = computed(() => route.query.redirect || '')

// 检查是否已经登录
onMounted(async () => {
  // 检查用户是否已经登录
  if (userStore.isLoggedIn) {
    try {
      // 验证用户状态
      const isValid = await userStore.validateUserStatus()
      if (isValid) {
        // 用户已登录且状态有效，根据角色跳转到相应页面
        console.log('您已登录')
        if (redirectPath.value) {
          await router.push(redirectPath.value)
        } else if (userStore.userInfo.role === 'INTERVIEWER') {
          await router.push('/interviewer/data-center/recruitment-analysis')
        } else {
          await router.push('/')
        }
        return
      }
    } catch (error) {
      console.error('验证用户状态失败:', error)
    }
  }

  // 页面加载动画
  initPageAnimation()
})

// 动态计算当前模式
const mode = computed(() => {
  if (isLogin.value) return 'login'
  if (registrationStep.value === 'selection') return 'register_selection'
  return 'register_form'
})

// 动态计算页面标题
const pageTitle = computed(() => {
  switch (mode.value) {
    case 'login':
      return '登录'
    case 'register_selection':
      return '选择注册类型'
    case 'register_form':
      return form.role === 'CANDIDATE' ? '应聘者注册' : '面试官注册'
    default:
      return '欢迎'
  }
})

// 根据角色动态计算用户名输入框
const usernamePlaceholder = computed(() => {
  if (mode.value === 'register_form' && form.role === 'INTERVIEWER') {
    return '公司全名'
  }
  return '用户名'
})

// 动态计算切换链接文本
const switchText = computed(() => {
  switch (mode.value) {
    case 'login':
      return '创建账号'
    case 'register_selection':
      return '已有账号？去登录'
    case 'register_form':
      return '返回上一步'
    default:
      return ''
  }
})

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  email: '',
  phone: '',
  role: 'CANDIDATE'
})

const validatePass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else {
    if (form.confirmPassword !== '') {
      formRef.value?.validateField('confirmPassword')
    }
    callback()
  }
}

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = computed(() => {
  const baseRules = {
    username: [
      { required: true, message: `请输入${usernamePlaceholder.value}`, trigger: 'blur' },
      { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
    ],
    password: [
      { required: true, validator: validatePass, trigger: 'blur' },
      { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
    ]
  }

  if (mode.value === 'register_form') {
    const registrationRules = {
      ...baseRules,
      password: [{ required: true, validator: validatePass, trigger: 'blur' }],
      confirmPassword: [{ required: true, validator: validatePass2, trigger: 'blur' }],
      email: [
        { required: true, message: '请输入邮箱地址', trigger: 'blur' },
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
      ],
      phone: [
        { required: true, message: '请输入手机号', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
      ]
    }

    // 仅当注册角色为应聘者时，添加真实姓名的校验规则
    if (form.role === 'CANDIDATE') {
      registrationRules.realName = [
        { required: true, message: '请输入真实姓名', trigger: 'blur' }
      ]
    }

    return registrationRules
  }
  return baseRules
})


const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    loading.value = true

    if (isLogin.value) {
      // 登录
      const res = await request.post('/api/users/login', {
        username: form.username,
        password: form.password
      })

      // 检查响应数据
      if (!res || !res.id || !res.role) {
        throw new Error('登录响应数据格式错误')
      }

      // 使用用户store存储用户信息
      userStore.setUser(res)

      console.log('登录成功')

      try {
        // 根据重定向参数或角色跳转
        if (redirectPath.value) {
          await router.push(redirectPath.value)
        } else if (res.role === 'INTERVIEWER') {
          await router.push('/interviewer/data-center/recruitment-analysis')
        } else {
          await router.push('/')
        }
      } catch (routerError) {
        console.error('路由跳转错误:', routerError)
        console.error('页面跳转失败')
      }
    } else {
      // 注册
      await request.post('/api/users/register', {
        username: form.username,
        password: form.password,
        realName: form.realName,
        email: form.email,
        phone: form.phone,
        role: form.role
      })

      console.log('注册成功，请登录')
      isLogin.value = true
      registrationStep.value = 'selection'
      formRef.value?.resetFields()
    }
  } catch (error) {
    console.error('Error:', error)
    console.error(error.response?.data?.error || '操作失败')
  } finally {
    loading.value = false
  }
}

// 切换视图的动画
const animateSwitch = (updateStateCallback) => {
  const pane = document.querySelector('.right-pane')
  const tl = gsap.timeline({
    defaults: { ease: 'power2.inOut' },
    onComplete: () => {
      // 动画完成后，如果进入了表单视图，则淡入表单项
      if (mode.value === 'login' || mode.value === 'register_form') {
        gsap.from('.auth-form .el-form-item', {
          y: 20,
          opacity: 0,
          stagger: 0.05,
          duration: 0.4
        })
      }
    }
  })

  tl.to(pane, { opacity: 0, y: -10, duration: 0.25 })
      .add(() => {
        updateStateCallback()
        formRef.value?.resetFields()
      })
      .fromTo(pane, { opacity: 0, y: 10 }, { opacity: 1, y: 0, duration: 0.35 })
}


// 处理底部切换链接的点击事件
const handleSwitchClick = () => {
  animateSwitch(() => {
    if (mode.value === 'register_form') {
      // 从注册表单返回选择步骤
      registrationStep.value = 'selection'
    } else {
      // 在登录和角色选择之间切换
      isLogin.value = !isLogin.value
      registrationStep.value = 'selection' // 确保总是回到选择步骤
    }
  })
}

// 选择注册角色，并切换到表单
const selectRegistrationRole = (role) => {
  animateSwitch(() => {
    form.role = role
    registrationStep.value = 'form'
  })
}

// 添加动画相关的代码
const loginBox = ref(null)

// 页面加载动画
const initPageAnimation = () => {
  const tl = gsap.timeline({ defaults: { ease: 'power2.out' } })

  // 整体卡片淡入上移
  tl.from(loginBox.value, { y: 50, opacity: 0, duration: 0.8 })

      // 左侧欢迎内容逐个出现
      .from('.left-pane .welcome', { y: -30, opacity: 0, duration: 0.6 }, '-=0.4')
      .from('.left-pane .subtitle', { y: -20, opacity: 0, duration: 0.5 }, '-=0.5')
      .from('.left-pane .logo', { scale: 0, opacity: 0, duration: 0.6 }, '-=0.45')

      // 右侧面板与表单
      .from('.right-pane', { x: 40, opacity: 0, duration: 0.7 }, '-=0.6')
      .from(
          '.auth-form .el-form-item',
          { y: 20, opacity: 0, stagger: 0.05, duration: 0.4 },
          '-=0.5'
      )
}

// 选择测试账号
const selectTestAccount = (type) => {
  switch (type) {
    case 'admin':
      form.username = 'admin';
      form.password = 'admin123';
      break;
    case 'hr':
      form.username = 'hr';
      form.password = 'hr123';
      break;
    case 'user1':
      form.username = 'user1';
      form.password = 'user123';
      break;
    default:
      return;
  }

  // 自动提交表单
  handleSubmit();
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #e6f0f4 0%, #f0f7f9 100%);
  position: relative; /* enable absolutely positioned decoration */
  overflow: hidden;   /* hide overflowing blur */
}

/* animated blurred circles */
.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  width: 700px;
  height: 700px;
  border-radius: 50%;
  background: #557F93;
  opacity: 0.65;
  filter: blur(160px);
}

.login-page::before {
  top: -300px;
  left: -300px;
  animation: circleMove1 25s ease-in-out infinite alternate;
}

.login-page::after {
  bottom: -300px;
  right: -300px;
  animation: circleMove2 30s ease-in-out infinite alternate;
}

@keyframes circleMove1 {
  from {
    transform: scale(1) translate(0, 0);
  }
  to {
    transform: scale(1.2) translate(40px, 40px);
  }
}

@keyframes circleMove2 {
  from {
    transform: scale(1) translate(0, 0);
  }
  to {
    transform: scale(1.15) translate(-40px, -40px);
  }
}

.auth-card {
  width: 1000px; /* 调整宽度，让注册表单更宽敞 */
  height: 600px; /* 调整高度，容纳更多输入项 */
  display: flex;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  position: relative; /* stack above circles */
  z-index: 2;
}

.left-pane {
  flex: 1;
  background: linear-gradient(135deg, #557F93 0%, #6B92A6 100%);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px 30px;
}

.left-pane .welcome {
  font-size: 40px;
  margin: 0 0 20px;
}

.left-pane .subtitle {
  font-size: 14px;
  letter-spacing: 2px;
  margin-bottom: 60px;
  opacity: 0.9;
}

.left-pane .logo {
  width: 96px;
  height: 96px;
  object-fit: contain;
  object-position: center;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.95);
  padding: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  border: 2px solid rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(10px);
}

.right-pane {
  flex: 1;
  background: #ffffff;
  padding: 60px 40px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;

}

.form-title {
  margin: 0 0 30px;
  font-size: 28px;
  text-align: center;
  color: #333333;
  min-height: 38px; /* 防止切换标题时布局抖动 */
}

.auth-form :deep(.el-input__wrapper) {
  background: #f5f7fa;
}

.submit-btn {
  width: 100%;
  height: 40px;
  margin-top: 10px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(90deg, #557F93 0%, #6B92A6 100%);
}

.switch {
  text-align: center;
  margin-top: 20px;
  color: #666666;
}

.switch span {
  cursor: pointer;
  color: #557F93;
}

.auth-form :deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: #557F93;
  border-color: #557F93;
}

.auth-form :deep(.el-radio__input.is-checked + .el-radio__label) {
  color: #557F93;
}

/* For role selection */
.role-selection {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.role-selection-tip {
  color: #666;
  margin-bottom: 25px;
}

.role-btn {
  width: 100%;
  height: 50px;
  margin: 0 0 15px !important; /* override element-plus margin */
  font-size: 16px;
  border-radius: 12px;
}

.role-btn .el-icon {
  margin-right: 8px;
}

.role-btn.candidate {
  --el-button-bg-color: #e8f3ff;
  --el-button-border-color: #d1e7ff;
  --el-button-text-color: #0d6efd;
  --el-button-hover-bg-color: #d1e7ff;
}

.role-btn.interviewer {
  --el-button-bg-color: #fcefe9;
  --el-button-border-color: #f9e2d6;
  --el-button-text-color: #d63384;
  --el-button-hover-bg-color: #f9e2d6;
}

.quick-login {
  margin-bottom: 20px;
  text-align: center;
}

.quick-login span {
  margin-right: 10px;
  color: #555;
}

.quick-login .el-button {
  margin: 0 5px;
}


@media (max-width: 768px) {
  .auth-card {
    flex-direction: column;
    width: 90%;
    height: auto;
  }
  .left-pane {
    padding: 40px 20px;
    height: 220px;
  }
  .right-pane {
    padding: 40px 20px;
  }
}
</style>
