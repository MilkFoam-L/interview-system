import request from '../utils/request'
import cacheService from '../services/cacheService'

/**
 * 分配题目给会话
 * @param {number} sessionId - 会话ID
 * @param {string} stage - 环节名称
 * @param {number} count - 题目数量
 * @returns {Promise<Object>} 分配结果
 */
export async function assignQuestions(sessionId, stage, count) {
  try {
    // 严格按照后端期望的格式构造请求体
    const requestData = {
      sessionId: sessionId,
      stage: stage,
      count: count
    };
    
    // 先检查是否已有题目，避免重复分配
    try {
      const existingQuestions = await request.get(`/api/session-questions`, {
        params: {
          sessionId: sessionId,
          stage: stage
        }
      });
      
      if (existingQuestions && Array.isArray(existingQuestions) && existingQuestions.length > 0) {
        return existingQuestions;
      }
    } catch (checkError) {
      // 检查失败，继续尝试分配新题目
    }
    
    // 发送请求分配新题目
    const response = await request({
      url: `/api/session-questions/assign`,
      method: 'post',
      data: requestData,
      timeout: 15000 // 增加超时时间到15秒
    });
    
    console.log('题目分配API响应:', response);
    
    // 检查新的响应格式
    if (response && response.success === false) {
      console.error('题目分配失败:', response.message);
      throw new Error(response.message || '题目分配失败');
    }
    
    // 提取数据部分
    const questionsData = response.data || response;
    
    // 验证响应是否有效
    if (!questionsData || !Array.isArray(questionsData)) {
      console.error('题目分配响应无效:', questionsData);
      throw new Error('题目分配失败：响应格式错误');
    }
    
    if (questionsData.length === 0) {
      console.warn('题目分配响应为空数组');
      throw new Error('题目分配失败：未获取到题目');
    }
    
    console.log(`成功分配${stage}环节题目，题目数量:`, questionsData.length);
    
    // 缓存题目数据
    cacheService.set(cacheKey, questionsData, 30 * 60 * 1000); // 缓存30分钟
    
    return questionsData;
  } catch (error) {
    // 如果分配失败，尝试获取已有题目作为备选方案
    try {
      const fallbackQuestions = await request.get(`/api/session-questions`, {
        params: {
          sessionId: sessionId,
          stage: stage
        }
      });
      
      if (fallbackQuestions && Array.isArray(fallbackQuestions) && fallbackQuestions.length > 0) {
        return fallbackQuestions;
      }
      
      throw new Error(`未找到现有题目`);
    } catch (fallbackError) {
      throw new Error(`无法获取题目: ${error.message || '未知错误'}`);
    }
  }
}

/**
 * 获取会话的题目
 * @param {number} sessionId - 会话ID
 * @param {string} stage - 环节名称
 * @param {boolean} [useCache=true] - 是否使用缓存
 * @returns {Promise<Array>} 题目列表
 */
export async function getSessionQuestions(sessionId, stage, useCache = true) {
  const cacheKey = `session_questions_${sessionId}_${stage}`;
  
  // 如果不使用缓存，直接请求
  if (!useCache) {
    return fetchSessionQuestions(sessionId, stage);
  }
  
  // 使用缓存服务
  return cacheService.getOrSet(cacheKey, () => fetchSessionQuestions(sessionId, stage), {
    ttl: 60 * 1000 // 缓存1分钟
  });
}

/**
 * 实际获取会话题目的函数
 * @param {number} sessionId - 会话ID
 * @param {string} stage - 环节名称
 * @returns {Promise<Array>} 题目列表
 */
async function fetchSessionQuestions(sessionId, stage) {
  try {
    // 严格按照后端要求的格式构造参数
    const params = {
      sessionId: sessionId,
      stage: stage
    };
    
    // 请求接口获取题目，增加超时
    const response = await request({
      url: `/api/session-questions`,
      method: 'get',
      params: params,
      timeout: 8000 // 增加超时时间到8秒
    });
    
    // 如果获取到有效数据
    if (response && Array.isArray(response) && response.length > 0) {
      return response;
    }
    
    // 没有找到题目，尝试分配新题目
    const questionCount = stage === 'BASIC_QA' ? 10 : stage === 'CODE_TEST' ? 3 : 5;
    return await assignQuestions(sessionId, stage, questionCount);
  } catch (error) {
    // 如果获取失败，延迟一下再尝试分配新题目
    try {
      await new Promise(resolve => setTimeout(resolve, 1000)); // 延迟1秒
      const questionCount = stage === 'BASIC_QA' ? 10 : stage === 'CODE_TEST' ? 3 : 5;
      return await assignQuestions(sessionId, stage, questionCount);
    } catch (assignError) {
      throw new Error(`无法获取或分配题目: ${error.message || '未知错误'}`);
    }
  }
}

/**
 * 获取题目详情
 * @param {number} questionId - 题目ID
 * @param {boolean} [useCache=true] - 是否使用缓存
 * @returns {Promise<Object>} 题目详情
 */
export async function getQuestionDetail(questionId, useCache = true) {
  const cacheKey = `question_detail_${questionId}`;
  
  // 如果不使用缓存，直接请求
  if (!useCache) {
    return fetchQuestionDetail(questionId);
  }
  
  // 使用缓存服务
  return cacheService.getOrSet(cacheKey, () => fetchQuestionDetail(questionId), {
    ttl: 5 * 60 * 1000 // 缓存5分钟
  });
}

/**
 * 实际获取题目详情的函数
 * @param {number} questionId - 题目ID
 * @returns {Promise<Object>} 题目详情
 */
async function fetchQuestionDetail(questionId) {
  try {
    const response = await request.get(`/api/questions/${questionId}`);
    if (!response) {
      throw new Error(`获取题目详情失败：服务器返回空数据`);
    }
    return response;
  } catch (error) {
    throw new Error(`获取题目详情失败: ${error.message || '未知错误'}`);
  }
}

/**
 * 清除会话题目缓存
 * @param {number} sessionId - 会话ID
 * @param {string} [stage] - 环节名称，如果不提供则清除所有环节的缓存
 */
export function clearSessionQuestionsCache(sessionId, stage) {
  if (stage) {
    const cacheKey = `session_questions_${sessionId}_${stage}`;
    cacheService.delete(cacheKey);
  } else {
    // 清除所有相关缓存
    const status = cacheService.getStatus();
    if (status && status.items) {
      status.items.forEach(item => {
        if (item.key.startsWith(`session_questions_${sessionId}`)) {
          cacheService.delete(item.key);
        }
      });
    }
  }
} 