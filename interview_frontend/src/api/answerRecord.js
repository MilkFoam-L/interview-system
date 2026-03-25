import request from '../utils/request'
import cacheService from '../services/cacheService'

/**
 * 提交答案
 * @param {Object} data - 答案数据
 * @param {number} data.sessionId - 会话ID
 * @param {number} data.interviewId - 面试ID
 * @param {number} data.questionId - 题目ID
 * @param {number} data.userId - 用户ID
 * @param {string} data.userAnswer - 用户答案
 * @param {string} data.type - 题目类型
 * @param {string} data.startTime - 开始时间
 * @param {string} data.submitTime - 提交时间
 * @param {number} data.timeUsed - 用时（秒）
 * @returns {Promise<Object>} 提交结果
 */
export async function submitAnswer(data) {
  const cacheKey = `answer_record_${data.userId}_${data.questionId}_${data.interviewId || 'default'}`
  
  try {
    // 确保有interviewId
    if (!data.interviewId && data.sessionId) {
      data.interviewId = data.sessionId
    }
    
    // 确保开始时间格式正确
    if (!data.startTime) {
      const now = new Date()
      data.startTime = now.toISOString().replace('T', ' ').substring(0, 19)
    }
    
    // 发送请求
    const response = await request.post('/api/answerRecord', data)
    
    // 更新缓存
    if (response) {
      const answerRecord = {
        ...data,
        id: response.id,
        isCorrect: response.isCorrect || 0,
        score: response.score || 0
      }
      
      // 更新缓存，30分钟过期
      cacheService.set(cacheKey, answerRecord, { ttl: 30 * 60 * 1000 })
    }
    
    return response
  } catch (error) {
    // 重试一次
    try {
      const retryResponse = await request.post('/api/answerRecord', data)
      
      // 更新缓存
      if (retryResponse) {
        const answerRecord = {
          ...data,
          id: retryResponse.id,
          isCorrect: retryResponse.isCorrect || 0,
          score: retryResponse.score || 0
        }
        cacheService.set(cacheKey, answerRecord, { ttl: 30 * 60 * 1000 })
      }
      
      return retryResponse
    } catch (retryError) {
      throw new Error(`提交答案失败`)
    }
  }
}

/**
 * 获取答案记录
 * @param {number} userId - 用户ID
 * @param {number} questionId - 题目ID
 * @param {number} [interviewId] - 面试会话ID（必须提供以确保记录隔离）
 * @param {boolean} [useCache=false] - 是否使用缓存
 * @returns {Promise<Object>} 答案记录
 */
export async function getAnswerRecord(userId, questionId, interviewId = null, useCache = false) {
  // 修改缓存键，包含interviewId以确保不同面试会话的答案不会混淆
  const cacheKey = `answer_record_${userId}_${questionId}_${interviewId || 'default'}`
  
  // 如果不使用缓存，直接请求
  if (!useCache) {
    return fetchAnswerRecord(userId, questionId, interviewId)
  }
  
  // 使用缓存服务，答案记录缓存时间较短
  return cacheService.getOrSet(cacheKey, () => fetchAnswerRecord(userId, questionId, interviewId), {
    ttl: 30 * 1000 // 缓存30秒
  })
}

/**
 * 实际获取答案记录的函数
 * @param {number} userId - 用户ID
 * @param {number} questionId - 题目ID
 * @param {number} [interviewId] - 面试会话ID
 * @returns {Promise<Object>} 答案记录
 */
async function fetchAnswerRecord(userId, questionId, interviewId = null) {
  try {
    // 添加时间戳参数以防止缓存问题
    const timestamp = new Date().getTime();
    
    // 构建URL，如果提供了interviewId，则加入查询参数
    let url = `/api/answerRecord/user/${userId}/question/${questionId}?_t=${timestamp}`;
    if (interviewId) {
      url += `&interviewId=${interviewId}`;
    }
    
    // 请求数据
    const response = await request.get(url);
    
    if (response && (response.userId || response.userAnswer !== undefined)) {
      return response;
    }
    
    // 返回默认记录
    return createDefaultRecord(userId, questionId, interviewId);
  } catch (error) {
    // 无论什么错误，都返回一个可用的默认记录
    return createDefaultRecord(userId, questionId, interviewId);
  }
}

// 创建默认答案记录
function createDefaultRecord(userId, questionId, interviewId) {
  const now = new Date();
  const formattedNow = now.toISOString().replace('T', ' ').substring(0, 19);
  return {
    userId: userId,
    questionId: questionId,
    interviewId: interviewId,
    userAnswer: '',
    isCorrect: 0,
    timeUsed: 0,
    startTime: formattedNow,
    submitTime: formattedNow, // 确保submitTime不为null
    type: 'BASIC_QA', // 默认类型
    score: 0
  };
}

/**
 * 获取用户在某个会话中的所有答案记录
 * @param {number} userId - 用户ID
 * @param {number} sessionId - 会话ID
 * @param {boolean} [useCache=true] - 是否使用缓存
 * @returns {Promise<Array>} 答案记录列表
 */
export async function getUserSessionAnswers(userId, sessionId, useCache = true) {
  const cacheKey = `user_session_answers_${userId}_${sessionId}`
  
  // 如果不使用缓存，直接请求
  if (!useCache) {
    return fetchUserSessionAnswers(userId, sessionId)
  }
  
  // 使用缓存服务
  return cacheService.getOrSet(cacheKey, () => fetchUserSessionAnswers(userId, sessionId), {
    ttl: 60 * 1000 // 缓存1分钟
  })
}

/**
 * 实际获取用户会话答案记录的函数
 * @param {number} userId - 用户ID
 * @param {number} sessionId - 会话ID
 * @returns {Promise<Array>} 答案记录列表
 */
async function fetchUserSessionAnswers(userId, sessionId) {
  try {
    const response = await request.get(`/api/answerRecord/user/${userId}/session/${sessionId}`);
    if (response && Array.isArray(response)) {
      return response;
    }
    return [];
  } catch (error) {
    // 重试一次
    try {
      const retryResponse = await request.get(`/api/answerRecord/user/${userId}/session/${sessionId}`);
      if (retryResponse && Array.isArray(retryResponse)) {
        return retryResponse;
      }
      return [];
    } catch (retryError) {
      return []; // 返回空数组而不是抛出错误
    }
  }
}

/**
 * 清除用户答案记录缓存
 * @param {number} userId - 用户ID
 * @param {number} [questionId] - 题目ID，如果不提供则清除该用户所有答案记录缓存
 * @param {number} [interviewId] - 面试会话ID，提供则只清除特定会话的缓存
 */
export function clearAnswerRecordCache(userId, questionId, interviewId) {
  if (questionId && interviewId) {
    // 清除特定题目和会话的缓存
    const cacheKey = `answer_record_${userId}_${questionId}_${interviewId}`
    cacheService.delete(cacheKey)
  } else if (questionId) {
    // 清除特定题目的所有会话缓存
    const status = cacheService.getStatus()
    if (status && status.items) {
      status.items.forEach(item => {
        if (item.key.startsWith(`answer_record_${userId}_${questionId}_`)) {
          cacheService.delete(item.key)
        }
      })
    }
  } else if (interviewId) {
    // 清除特定会话的所有缓存
    const status = cacheService.getStatus()
    if (status && status.items) {
      status.items.forEach(item => {
        if (item.key.includes(`_${interviewId}`)) {
          cacheService.delete(item.key)
        }
      })
    }
  } else {
    // 清除用户所有相关缓存
    const status = cacheService.getStatus()
    if (status && status.items) {
      status.items.forEach(item => {
        if (item.key.startsWith(`answer_record_${userId}_`)) {
          cacheService.delete(item.key)
        }
      })
    }
  }
} 