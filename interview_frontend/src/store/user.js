import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as auth from '@/utils/auth'

// 用户头像默认值
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const defaultInterviewerAvatar = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

// 用户状态管理
export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const isLoggedIn = computed(() => !!userInfo.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  
  // 用户头像，有fallback机制
  const userAvatar = computed(() => {
    if (!userInfo.value) return defaultAvatar
    
    if (userInfo.value.avatarUrl) {
      // 检查是否为完整URL
      if (userInfo.value.avatarUrl.startsWith('http') || userInfo.value.avatarUrl.startsWith('data:')) {
        return userInfo.value.avatarUrl
      }
      
      // 相对路径，添加API前缀
      if (!userInfo.value.avatarUrl.startsWith('/api')) {
        return '/api' + userInfo.value.avatarUrl
      }
      
      return userInfo.value.avatarUrl
    }
    
    return defaultAvatar
  })
  
  // 面试官头像 - 固定值
  const interviewerAvatar = computed(() => defaultInterviewerAvatar)
  
  // 用户显示名称
  const displayName = computed(() => {
    if (!userInfo.value) return '用户'
    return userInfo.value.realName || userInfo.value.username || '用户'
  })
  
  // 从localStorage和sessionStorage初始化用户信息
  const initUserInfo = () => {
    // 首先尝试从sessionStorage中获取信息（优先级最高）
    try {
      const sessionUser = sessionStorage.getItem('logged_in_user')
      if (sessionUser) {
        const parsedUser = JSON.parse(sessionUser)
        if (parsedUser && parsedUser.id) {
          userInfo.value = parsedUser
          return
        }
      }
    } catch (e) {
      console.error('从sessionStorage获取用户信息失败', e)
    }
    
    // 然后尝试从auth工具中获取
    try {
      const storedUserInfo = auth.getUserInfo()
      if (storedUserInfo && storedUserInfo.id) {
        userInfo.value = storedUserInfo
        return
      }
    } catch (e) {
      console.error('从auth工具获取用户信息失败', e)
    }
    
    // 最后尝试从localStorage的备用键获取
    try {
      const localUser = localStorage.getItem('user')
      if (localUser) {
        const parsedUser = JSON.parse(localUser)
        if (parsedUser && parsedUser.id) {
          userInfo.value = parsedUser
          return
        }
      }
    } catch (e) {
      console.error('从localStorage获取用户信息失败', e)
    }
  }
  
  // 设置用户信息
  const setUser = (user) => {
    try {
      // 确保用户对象有效
      if (!user || typeof user !== 'object') {
        return
      }
      
      // 标准化用户ID为数字类型
      if (user.id && typeof user.id === 'string') {
        user.id = Number(user.id)
        if (isNaN(user.id)) {
          user.id = null
        }
      }
      
      // 将userInfo复制到本地变量
      const userCopy = { ...user }
      userInfo.value = userCopy
      
      // 使用auth工具存储用户信息
      auth.setUserInfo(userCopy)
      
      // 额外在localStorage中保存一份用户信息的备份
      localStorage.setItem('user_info', JSON.stringify(userCopy))
      localStorage.setItem('user', JSON.stringify(userCopy))
      
      // 额外保存在sessionStorage中，避免页面刷新后丢失
      try {
        sessionStorage.setItem('logged_in_user', JSON.stringify(userCopy))
      } catch (e) {
        console.error('保存用户信息到sessionStorage失败', e)
      }
    } catch (error) {
      console.error('保存用户信息失败', error)
    }
  }
  
  // 清除用户信息
  const clearUser = () => {
    userInfo.value = null
    auth.silentLogout()
    localStorage.removeItem('user_info')
    localStorage.removeItem('user')
    sessionStorage.removeItem('logged_in_user')
  }
  
  // 获取当前用户信息
  const refreshUserInfo = async () => {
    try {
      const response = await fetch('/api/users/current')
      if (!response.ok) {
        throw new Error('获取用户信息失败')
      }
      
      const data = await response.json()
      
      if (data) {
        const user = {
          id: data.id,
          username: data.username,
          realName: data.realName,
          email: data.email,
          phone: data.phone,
          role: data.role,
          avatarUrl: data.avatarUrl
        }
        setUser(user)
        return user
      }
      
      return null
    } catch (error) {
      console.error('获取用户信息失败:', error)
      if (error.response?.status === 401) {
        clearUser()
      }
      return null
    }
  }
  
  // 初始化
  initUserInfo()
  
  return {
    userInfo,
    isLoggedIn,
    isAdmin,
    userAvatar,
    interviewerAvatar,
    displayName,
    setUser,
    clearUser,
    refreshUserInfo
  }
}) 