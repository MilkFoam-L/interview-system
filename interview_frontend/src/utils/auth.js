/**
 * 认证相关工具函数
 */

const TOKEN_KEY = 'access_token'
const USER_KEY = 'user_info'

/**
 * 获取Token
 * @returns {string|null} 用户Token
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 设置Token
 * @param {string} token 用户Token
 */
export function setToken(token) {
  return localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 删除Token
 */
export function removeToken() {
  return localStorage.removeItem(TOKEN_KEY)
}

/**
 * 获取用户信息
 * @returns {Object|null} 用户信息对象
 */
export function getUserInfo() {
  const USER_KEY = 'user_info'; 
  const userStr = localStorage.getItem(USER_KEY);
  
  if (userStr) {
    try {
      return JSON.parse(userStr);
    } catch (e) {
      return null;
    }
  }
  return null;
}

/**
 * 设置用户信息
 * @param {Object} userInfo 用户信息对象
 */
export function setUserInfo(userInfo) {
  return localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
}

/**
 * 删除用户信息
 */
export function removeUserInfo() {
  return localStorage.removeItem(USER_KEY)
}

/**
 * 清除所有认证信息
 */
export function clearAuth() {
  removeToken()
  removeUserInfo()
}

/**
 * 验证用户状态是否有效
 * @returns {Promise<boolean>} 用户状态是否有效
 */
export async function validateUserStatus() {
  try {
    const userInfo = getUserInfo()
    if (!userInfo) {
      return false
    }

    // 调用后端验证接口
    const token = getToken()
    const headers = {
      'Content-Type': 'application/json'
    }
    
    // 只有在token有效时才添加到请求头
    if (token && token !== 'null' && token !== 'undefined') {
      headers['Authorization'] = `Bearer ${token}`
    }
    
    const response = await fetch('/api/users/validate-session', {
      method: 'GET',
      credentials: 'include',
      headers
    })

    if (response.ok) {
      const result = await response.json()
      return result.valid === true
    } else if (response.status === 401) {
      // 如果返回401，说明用户未登录或session已过期
      clearAuth()
      return false
    } else {
      console.error('验证用户状态失败:', response.status)
      return false
    }
  } catch (error) {
    console.error('验证用户状态时发生错误:', error)
    return false
  }
}

/**
 * 检查用户登录状态并尝试恢复
 * @returns {Promise<Object|null>} 用户信息或null
 */
export async function checkAndRestoreUserStatus() {
  try {
    const userInfo = getUserInfo()
    
    // 如果本地没有用户信息，直接返回null
    if (!userInfo) {
      return null
    }

    // 验证用户状态
    const isValid = await validateUserStatus()
    
    if (isValid) {
      // 状态有效，尝试获取最新的用户信息
      try {
        const token = getToken()
        const headers = {
          'Content-Type': 'application/json'
        }
        
        // 只有在token有效时才添加到请求头
        if (token && token !== 'null' && token !== 'undefined') {
          headers['Authorization'] = `Bearer ${token}`
        }
        
        const response = await fetch('/api/users/current', {
          method: 'GET',
          credentials: 'include',
          headers
        })

        if (response.ok) {
          const currentUser = await response.json()
          // 更新本地存储的用户信息
          setUserInfo(currentUser)
          return currentUser
        }
      } catch (error) {
        console.error('获取当前用户信息失败:', error)
      }
      
      // 如果获取最新用户信息失败，但验证有效，返回本地存储的用户信息
      return userInfo
    } else {
      // 状态无效，清除本地存储
      clearAuth()
      return null
    }
  } catch (error) {
    console.error('检查和恢复用户状态失败:', error)
    return null
  }
}

/**
 * 退出登录
 * @param {string} message 退出提示消息
 */
export function logout(message = '已成功退出登录') {
  // 获取用户信息用于清理相关状态
  const userInfo = getUserInfo()
  
  // 调用后端退出登录接口
  try {
    const token = getToken()
    // 异步调用后端接口，不等待结果
    const headers = {
      'Content-Type': 'application/json'
    }
    
    // 只有在token有效时才添加到请求头
    if (token && token !== 'null' && token !== 'undefined') {
      headers['Authorization'] = `Bearer ${token}`
    }
    
    fetch('/api/users/logout', {
      method: 'POST',
      credentials: 'include',
      headers
    }).catch(error => {
      console.error('调用后端退出登录接口失败:', error)
    })
  } catch (error) {
    console.error('退出登录请求错误:', error)
  }
  
  // 清除本地存储
  clearAuth()
  
  // 清除面试相关的本地存储
  localStorage.removeItem('interviewStatus')
  localStorage.removeItem('pendingInterviewJob')
  localStorage.removeItem('interviewSessionId')
  localStorage.removeItem('lastReportId')
  localStorage.removeItem('interviewTime')
  localStorage.removeItem('user')
  
  // 清除语音指引相关的状态
  if (userInfo && userInfo.id) {
    // 清除用户相关的语音指引状态
    localStorage.removeItem(`lastLoginTime_${userInfo.id}`)
    localStorage.removeItem(`voiceGuide_newUserWelcome_${userInfo.id}`)
    console.log('已清除用户语音指引状态')
  }
  
  // 清除会话相关的语音指引状态
  sessionStorage.removeItem('currentLoginSession')
  
  // 清除所有用户的会话语音状态（使用循环清理）
  const sessionKeys = Object.keys(sessionStorage)
  sessionKeys.forEach(key => {
    if (key.includes('returningUserWelcome_') && key.includes('_session')) {
      sessionStorage.removeItem(key)
    }
  })
  
  console.log('已清除所有语音指引会话状态')
  
  // 显示退出成功消息
  if (message) {
    import('element-plus').then(({ ElMessage }) => {
      console.log(message)
    })
  }
  
  // 延迟跳转到登录页，让用户看到成功消息
  setTimeout(() => {
    window.location.href = '/login'
  }, 1000)
}

/**
 * 静默退出登录（不显示消息）
 */
export function silentLogout() {
  // 获取用户信息用于清理相关状态
  const userInfo = getUserInfo()
  
  // 清除本地存储
  clearAuth()
  
  // 清除面试相关的本地存储
  localStorage.removeItem('interviewStatus')
  localStorage.removeItem('pendingInterviewJob')
  localStorage.removeItem('interviewSessionId')
  localStorage.removeItem('lastReportId')
  localStorage.removeItem('interviewTime')
  localStorage.removeItem('user')
  
  // 清除语音指引相关的状态
  if (userInfo && userInfo.id) {
    // 清除用户相关的语音指引状态
    localStorage.removeItem(`lastLoginTime_${userInfo.id}`)
    localStorage.removeItem(`voiceGuide_newUserWelcome_${userInfo.id}`)
  }
  
  // 清除会话相关的语音指引状态
  sessionStorage.removeItem('currentLoginSession')
  
  // 清除所有用户的会话语音状态
  const sessionKeys = Object.keys(sessionStorage)
  sessionKeys.forEach(key => {
    if (key.includes('returningUserWelcome_') && key.includes('_session')) {
      sessionStorage.removeItem(key)
    }
  })
}

/**
 * 获取当前用户ID
 * @returns {number|null} 用户ID
 */
export function getUserId() {
  const userInfo = getUserInfo()
  console.log('auth.js - getUserId - 获取到的用户信息:', userInfo);
  
  if (userInfo && userInfo.id) {
    console.log('auth.js - getUserId - 返回用户ID:', userInfo.id);
    return userInfo.id;
  }
  
  // 尝试从其他位置获取
  try {
    // 尝试从localStorage中的user获取
    const userStr = localStorage.getItem('user');
    console.log('auth.js - getUserId - 从localStorage获取的user:', userStr);
    
    if (userStr) {
      const user = JSON.parse(userStr);
      if (user && user.id) {
        console.log('auth.js - getUserId - 从user中获取到ID:', user.id);
        return user.id;
      }
    }
  } catch (e) {
    console.error('auth.js - getUserId - 尝试获取备用ID失败:', e);
  }
  
  console.warn('auth.js - getUserId - 未找到用户ID');
  return null;
}

export default {
  getToken,
  setToken,
  removeToken,
  getUserInfo,
  setUserInfo,
  removeUserInfo,
  clearAuth,
  validateUserStatus,
  checkAndRestoreUserStatus,
  logout,
  silentLogout,
  getUserId
} 