import request from '@/utils/request'

/**
 * 分析自我介绍
 * 
 * @param {Object} data - 分析请求数据
 * @param {Number} data.sessionId - 会话ID
 * @param {String} data.introductionText - 自我介绍文本
 * @param {Number} data.duration - 介绍时长(秒)
 * @returns {Promise} 分析结果
 */
export function analyzeIntroduction(data) {
  console.log('调用自我介绍分析API，参数:', {
    sessionId: data.sessionId,
    textLength: data.introductionText ? data.introductionText.length : 0,
    duration: data.duration
  });
  
  return request({
    url: '/api/introduction-analysis/analyze',
    method: 'post',
    data: data,
    timeout: 60000, // 60秒超时，AI分析需要更长时间
    headers: {
      'Content-Type': 'application/json'
    }
  }).catch(error => {
    console.error('自我介绍分析API调用失败:', error);
    
    // 解析错误响应
    let errorMessage = error.message || '未知错误';
    if (error.response && error.response.data) {
      console.error('错误响应数据:', error.response.data);
      if (typeof error.response.data === 'string') {
        errorMessage = error.response.data;
      } else if (error.response.data.message) {
        errorMessage = error.response.data.message;
      }
    }
    
    // 返回错误结果
    return {
      success: false,
      message: '自我介绍分析失败: ' + errorMessage,
      error: true
    };
  });
}

/**
 * 获取自我介绍分析结果
 * 
 * @param {Number} sessionId - 会话ID
 * @returns {Promise} 分析结果
 */
export function getIntroductionAnalysis(sessionId) {
  console.log('获取自我介绍分析结果，会话ID:', sessionId);
  
  return request({
    url: `/api/introduction-analysis/result/${sessionId}`,
    method: 'get',
    params: {
      timestamp: Date.now() // 防止缓存
    },
    timeout: 60000 // 增加超时时间到60秒
  }).catch(error => {
    console.error('获取自我介绍分析结果失败:', error);
    return {
      success: false,
      message: '获取分析结果失败: ' + (error.message || '未知错误'),
      data: null,
      error: true
    };
  });
}

/**
 * 重新分析自我介绍
 * 
 * @param {Number} sessionId - 会话ID
 * @returns {Promise} 重新分析结果
 */
export function reanalyzeIntroduction(sessionId) {
  console.log('重新分析自我介绍，会话ID:', sessionId);
  
  return request({
    url: `/api/introduction-analysis/reanalyze/${sessionId}`,
    method: 'post',
    timeout: 60000
  }).catch(error => {
    console.error('重新分析自我介绍失败:', error);
    return {
      success: false,
      message: '重新分析失败: ' + (error.message || '未知错误'),
      error: true
    };
  });
}

/**
 * 检查分析服务状态
 * 
 * @returns {Promise} 服务状态
 */
export function checkAnalysisServiceHealth() {
  return request({
    url: '/api/introduction-analysis/health',
    method: 'get',
    timeout: 5000
  }).catch(error => {
    console.error('检查自我介绍分析服务状态失败:', error);
    return {
      status: 'ERROR',
      message: '检查服务状态失败',
      available: false,
      error: true
    };
  });
}

/**
 * 获取分析维度说明
 * 
 * @returns {Promise} 分析维度说明
 */
export function getAnalysisDimensions() {
  return request({
    url: '/api/introduction-analysis/dimensions',
    method: 'get',
    timeout: 5000
  }).catch(error => {
    console.error('获取分析维度说明失败:', error);
    return {
      success: false,
      message: '获取分析维度说明失败',
      data: null,
      error: true
    };
  });
}