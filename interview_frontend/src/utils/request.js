import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, silentLogout, getUserInfo } from './auth'
import { useUserStore } from '@/stores/index'
import { ElMessageBox } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: '',
  timeout: 60000, // 增加到60秒，适应AI判题时间
  withCredentials: true
})

// 用于记录是否正在验证用户状态，避免重复验证
let isValidatingUser = false
// 用于存储等待验证结果的请求队列
let pendingRequests = []

// request拦截器
service.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = getToken()
    
    // 只有在token存在时才设置到请求头中
    if (token && token !== 'null' && token !== 'undefined') {
      // 为所有请求头添加token
      config.headers['Authorization'] = `Bearer ${token}`
    }
    
    // 设置 Content-Type
    // 对于非 multipart 情况默认使用 application/json
    // 若请求体是 FormData，保持浏览器自动设置的 multipart/form-data；否则后端无法识别
    const isFormData = config.data instanceof FormData
    if (!config.headers['Content-Type'] && config.method !== 'get' && !isFormData) {
      config.headers['Content-Type'] = 'application/json'
    }
    
    // 从store或本地存储获取用户信息，尝试添加用户ID
    try {
      const userInfo = getUserInfo();
      if (userInfo && userInfo.id) {
        config.headers['X-User-ID'] = userInfo.id;
      } else {
        // 尝试从其他来源获取
        try {
          const userStr = localStorage.getItem('user');
          if (userStr) {
            const user = JSON.parse(userStr);
            if (user && user.id) {
              config.headers['X-User-ID'] = user.id;
            }
          }
        } catch (e) {
          // 静默处理
        }
      }
    } catch (e) {
      // 静默处理
    }
    
    return config
  },
  error => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// response拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 如果是Blob类型，直接返回Blob数据（用于文件下载）
    if (response.config.responseType === 'blob') {
      return response.data
    }
    
    // 判断是否是API返回的标准格式
    if (res.code && res.code !== 200) {
      ElMessage({
        message: res.message || '请求失败',
        type: 'error',
        duration: 3 * 1000
      })
      
      // 处理特定的错误码
      if (res.code === 401) {
        // token过期或未登录，但不立即跳转，而是验证用户状态
        handleUnauthorized('登录已过期')
      }
      
      return Promise.reject(res)
    } else {
      // 直接返回响应数据，而不是整个响应对象
      return res
    }
  },
  async error => {
    console.error('响应错误:', error)
    
    // 处理网络错误
    if (!error.response) {
      ElMessage({
        message: '网络连接失败，请检查您的网络',
        type: 'error',
        duration: 3000
      })
      return Promise.reject(error)
    }
    
    // 处理HTTP状态码错误
    const { status, data } = error.response
    let message = data?.message || data?.error || '请求失败'
    
    switch (status) {
      case 400:
        message = data?.message || data?.error || '请求参数错误'
        break
      case 401:
        // 未授权错误，进行智能处理
        await handleUnauthorized('未授权，请重新登录')
        return Promise.reject(error)
      case 403:
        message = '拒绝访问'
        break
      case 404:
        message = '请求的资源不存在'
        break
      case 500:
        message = '服务器内部错误'
        break
    }
    
    // 对于非401错误，显示错误消息
    if (status !== 401) {
      ElMessage({
        message: message,
        type: 'error',
        duration: 3000
      })
    }
    
    return Promise.reject(error)
  }
)

// 处理未授权错误的智能函数
async function handleUnauthorized(defaultMessage) {
  // 如果正在验证用户状态，直接返回
  if (isValidatingUser) {
    return
  }
  
  // 避免重复验证
  isValidatingUser = true
  
  try {
    // 使用pinia store中的验证方法
    const userStore = useUserStore()
    const isValid = await userStore.validateUserStatus()
    
    if (isValid) {
      // 用户状态有效，可能是临时的网络问题或后端重启后恢复了
      console.log('用户状态验证成功，无需重新登录')
      ElMessage({
        message: '连接已恢复',
        type: 'success',
        duration: 2000
      })
    } else {
      // 用户状态无效，需要重新登录
      console.log('用户状态验证失败，需要重新登录')
      
      // 静默清除本地存储
      silentLogout()
      
      // 显示友好的提示消息
      ElMessage({
        message: '登录状态已过期，请重新登录',
        type: 'warning',
        duration: 4000
      })
      
      // 延迟跳转到登录页面
      setTimeout(() => {
        // 检查当前是否已经在登录页面
        if (window.location.pathname !== '/login') {
          const currentPath = window.location.pathname + window.location.search
          window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
        }
      }, 2000)
    }
  } catch (error) {
    console.error('验证用户状态时发生错误:', error)
    
    // 验证失败时，也清除本地存储并提示重新登录
    silentLogout()
    
    ElMessage({
      message: '验证登录状态失败，请重新登录',
      type: 'error',
      duration: 4000
    })
    
    setTimeout(() => {
      if (window.location.pathname !== '/login') {
        const currentPath = window.location.pathname + window.location.search
        window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
      }
    }, 2000)
  } finally {
    isValidatingUser = false
  }
}

export default service 