import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { getUserInfo, setUserInfo, clearAuth, getToken } from '@/utils/auth'

// 用户状态管理store
export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const isLoggedIn = computed(() => !!userInfo.value)
  const isValidating = ref(false)

  // 初始化用户信息
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
      // 静默处理
    }
    
    // 然后尝试从auth工具中获取
    try {
      const storedUserInfo = getUserInfo()
      if (storedUserInfo && storedUserInfo.id) {
        userInfo.value = storedUserInfo
        return
      }
    } catch (e) {
      // 静默处理
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
      // 静默处理
    }
  }

  // 设置用户信息
  const setUser = (user) => {
    try {
      // 确保用户对象有效
      if (!user || typeof user !== 'object') {
        return;
      }
      
      // 标准化用户ID为数字类型
      if (user.id && typeof user.id === 'string') {
        user.id = Number(user.id)
        // 如果转换后不是有效数字，记录警告但继续使用原始值
        if (isNaN(user.id)) {
          user.id = null
        }
      }
      
      // 将userInfo复制到本地变量
      const userCopy = { ...user }
      userInfo.value = userCopy
      
      // 使用auth工具存储用户信息
      setUserInfo(userCopy)
      
      // 额外在localStorage中保存一份用户信息的备份
      localStorage.setItem('user_info', JSON.stringify(userCopy))
      localStorage.setItem('user', JSON.stringify(userCopy))
      
      // 额外保存在sessionStorage中，避免页面刷新后丢失
      try {
        sessionStorage.setItem('logged_in_user', JSON.stringify(userCopy))
      } catch (e) {
        // 静默处理
      }
    } catch (error) {
      // 尝试最小化方式保存关键信息
      try {
        const minimalUser = {
          id: user?.id || null,
          username: user?.username || null,
          role: user?.role || 'user'
        }
        userInfo.value = minimalUser
        setUserInfo(minimalUser)
        localStorage.setItem('user_info', JSON.stringify(minimalUser))
        localStorage.setItem('user', JSON.stringify(minimalUser))
      } catch (innerError) {
        // 静默处理
      }
    }
  }

  // 清除用户信息
  const clearUser = () => {
    userInfo.value = null
    clearAuth()
  }

  // 验证用户状态
  const validateUserStatus = async () => {
    if (isValidating.value) return false
    
    isValidating.value = true
    
    try {
      // 如果本地没有用户信息，直接返回false
      if (!userInfo.value) {
        isValidating.value = false
        return false
      }

      // 调用后端验证接口
      const token = getToken();
      const headers = {
        'Content-Type': 'application/json',
        'Cache-Control': 'no-cache, no-store',
        'Pragma': 'no-cache'
      };
      
      // 只有在token存在时才添加Authorization头
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      const res = await request({
        url: '/api/users/validate-session',
        method: 'get',
        headers,
        timeout: 5000 // 5秒超时
      });
      
      if (res && res.valid) {
        // 验证成功，更新用户信息
        if (res.userId && res.role) {
          const updatedUser = {
            ...userInfo.value,
            id: res.userId,
            role: res.role
          }
          setUser(updatedUser)
        }
        isValidating.value = false
        return true
      } else {
        // 验证失败，清除用户信息
        clearUser()
        isValidating.value = false
        return false
      }
    } catch (error) {
      console.error('验证用户状态失败:', error)
      
      // 针对不同错误类型进行处理
      if (error.response?.status === 401) {
        // 401未授权，清除用户信息
        clearUser()
        isValidating.value = false
        return false
      } else if (error.response?.status === 500 || !error.response) {
        // 服务器错误或网络错误，不立即清除用户信息
        // 因为这可能是暂时性的服务器问题
        console.warn('服务器或网络错误，保留用户会话状态')
        isValidating.value = false
        
        // 使用本地存储的token检查是否有效
        const token = getToken()
        if (token) {
          try {
            // 尝试检查token是否过期（简单验证）
            const tokenData = token.split('.')[1]
            if (tokenData) {
              const decodedData = JSON.parse(atob(tokenData))
              const expTime = decodedData.exp * 1000
              
              // 如果token未过期，暂时认为会话有效
              if (expTime > Date.now()) {
                console.log('Token尚未过期，暂时认为会话有效')
                return true
              } else {
                console.log('Token已过期，清除会话')
                clearUser()
                return false
              }
            }
          } catch (tokenError) {
            console.error('验证token失败:', tokenError)
          }
        }
        
        // 默认保守处理，返回false
        return false
      } else {
        // 其他错误类型，保持不变
        isValidating.value = false
        return false
      }
    }
  }

  // 获取当前用户信息
  const getCurrentUser = async () => {
    try {
      const res = await request({
        url: '/api/users/current',
        method: 'get'
      })
      
      if (res) {
        const user = {
          id: res.id,
          username: res.username,
          realName: res.realName,
          email: res.email,
          phone: res.phone,
          role: res.role,
          avatarUrl: res.avatarUrl
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
      throw error
    }
  }

  // 初始化
  initUserInfo()

  return {
    userInfo,
    isLoggedIn,
    isValidating,
    setUser,
    clearUser,
    validateUserStatus,
    getCurrentUser
  }
})

// 定义一个示例 store
export const useMainStore = defineStore('main', {
  state: () => ({
    count: 0,
    name: 'Counter'
  }),
  actions: {
    increment() {
      this.count++
    }
  },
  getters: {
    doubleCount: (state) => state.count * 2
  }
}) 