import request from '@/utils/request'

// 为API请求结果创建缓存
const apiCache = new Map();
const CACHE_TTL = 30 * 1000; // 缓存有效期：30秒

/**
 * 带缓存的API请求
 * @param {String} cacheKey 缓存键
 * @param {Function} apiCall API调用函数
 * @returns {Promise} API结果Promise
 */
function cachedApiCall(cacheKey, apiCall) {
  const now = Date.now();
  const cachedData = apiCache.get(cacheKey);

  if (cachedData && now - cachedData.timestamp < CACHE_TTL) {
    console.log(`使用缓存数据: ${cacheKey}`);
    return Promise.resolve(cachedData.data);
  }

  return apiCall().then(response => {
    apiCache.set(cacheKey, {
      data: response,
      timestamp: now
    });
    return response;
  });
}

/**
 * 创建面试申请
 * @param {Object} data 申请数据
 * @returns {Promise} 包含申请结果的Promise
 */
export function createApplication(data) {
  console.log('调用API: 创建面试申请', data);
  // 写操作不缓存
  return request({
    url: '/api/job-applications',
    method: 'post',
    data
  }).then(response => {
    // 写操作成功后，使相关缓存失效
    const userCacheKey = `getUserApplications`;
    apiCache.delete(userCacheKey);
    console.log('创建申请API返回数据:', response);
    return response;
  }).catch(error => {
    console.error('创建申请API错误:', error);
    throw error;
  });
}

/**
 * 获取用户的申请记录
 * @param {Boolean} forceRefresh 是否强制刷新
 * @returns {Promise} 包含申请记录的Promise
 */
export function getUserApplications() {
  console.log('调用API: 获取用户申请记录');

  // 获取用户ID
  const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
  const userId = userInfo.id;

  if (!userId) {
    console.error('获取用户申请记录失败: 未找到用户ID');
    return Promise.reject(new Error('未找到用户ID'));
  }

  console.log('获取用户ID为', userId, '的申请记录');

  // 使用更简单的方式调用API
  return request.get('/api/job-applications/user', {
    params: { userId },
    headers: { 'X-User-ID': String(userId) }
  }).then(response => {
    console.log('获取申请记录API返回数据:', response);
    return response;
  }).catch(error => {
    console.error('获取申请记录API错误:', error);
    throw error;
  });
}

/**
 * 更新面试时间
 * @param {Number} applicationId 申请记录ID
 * @param {String} interviewTime 面试时间
 * @returns {Promise} 包含更新结果的Promise
 */
export function updateInterviewTime(applicationId, interviewTime) {
  console.log('调用API: 更新面试时间', applicationId, interviewTime);
  // 写操作不缓存
  return request({
    url: `/api/job-applications/${applicationId}/interview-time`,
    method: 'put',
    data: {
      interviewTime: interviewTime
    }
  }).then(response => {
    // 写操作成功后，使相关缓存失效
    const userCacheKey = `getUserApplications`;
    apiCache.delete(userCacheKey);
    console.log('更新面试时间API返回数据:', response);
    return response;
  }).catch(error => {
    console.error('更新面试时间API错误:', error);
    throw error;
  });
}

/**
 * 直接投递简历到岗位，带 resumeId
 * @param {Object} params 包含jobId和resumeId的对象
 * @returns {Promise} 包含投递结果的Promise
 */
export function applyJobWithResume({ jobId, resumeId }) {
  console.log('调用API: 投递简历', jobId, resumeId)

  // 确保参数存在且有效
  if (!jobId || !resumeId) {
    console.error('投递简历缺少必要参数', { jobId, resumeId })
    return Promise.reject(new Error('缺少必要参数'))
  }

  return request({
    url: '/api/job-applications',
    method: 'post',
    data: { jobId, resumeId }
  }).then(res => {
    // 写操作成功后，使相关缓存失效
    const userCacheKey = `getUserApplications`;
    apiCache.delete(userCacheKey);
    console.log('投递简历返回:', res)
    return res
  }).catch(error => {
    console.error('投递简历API错误:', error)
    throw error
  })
}

/**
 * 清除API缓存
 * @param {String} key 缓存键，不提供则清除所有缓存
 */
export function clearApiCache(key) {
  if (key) {
    apiCache.delete(key);
    console.log(`已清除指定缓存: ${key}`);
  } else {
    apiCache.clear();
    console.log('已清除所有API缓存');
  }
}