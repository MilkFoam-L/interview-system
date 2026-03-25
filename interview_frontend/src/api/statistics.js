import request from '@/utils/request'
import { getUserInfo } from '@/utils/auth'

/**
 * 获取用户面试统计数据
 */
export function getInterviewStatistics() {
  console.log('调用API: 获取面试统计数据');
  // 添加时间戳和随机数，确保不使用缓存
  const timestamp = new Date().getTime();
  const random = Math.floor(Math.random() * 1000);
  
  // 获取当前用户ID
  const userInfo = getUserInfo();
  const userId = userInfo ? userInfo.id : null;
  console.log('当前用户ID:', userId);
  
  // 如果没有用户ID，返回空数据
  if (!userId) {
    console.error('获取用户信息失败');
    return Promise.reject(new Error('获取用户信息失败'));
  }

  return request.get(`/api/statistics/interview`, {
    params: {
      userId,
      _t: timestamp,
      _r: random
    },
    headers: {
      'Cache-Control': 'no-cache, no-store',
      'Pragma': 'no-cache',
      'X-User-ID': String(userId)
    }
  }).then(response => {
    console.log('API返回数据:', response);

    if (response) {
      response.submittedCount = Number(response.submittedCount || 0);
      response.pendingCount = Number(response.pendingCount || 0);
      response.completedCount = Number(response.completedCount || 0);

      if (response.passRate) {
        if (typeof response.passRate !== 'string') {
          response.passRate = response.passRate.toFixed(2);
        }
        if (!response.passRate.includes('%')) {
          response.passRate = `${response.passRate}%`;
        }
      } else {
        response.passRate = '0%';
      }
      
      console.log('处理后的统计数据:', response);
    }
    
    return response;
  }).catch(error => {
    console.error('获取统计数据API错误:', error);
    throw error;
  });
}

/**
 * 刷新用户面试统计数据
 */
export function refreshInterviewStatistics() {
  console.log('调用API: 刷新面试统计数据');
  const timestamp = new Date().getTime();
  const random = Math.floor(Math.random() * 1000);
  
  // 获取当前用户ID
  const userInfo = getUserInfo();
  const userId = userInfo ? userInfo.id : null;
  console.log('当前用户ID:', userId);
  
  // 如果没有用户ID，返回空数据
  if (!userId) {
    console.error('获取用户信息失败');
    return Promise.reject(new Error('获取用户信息失败'));
  }

  return Promise.all([
    request.get(`/api/job-applications/user`, {
      params: { userId },
      headers: { 'X-User-ID': String(userId) }
    }).catch(e => {
      console.error('获取投递记录失败:', e);
      return { error: e.message };
    }),

    request.get(`/api/statistics/interview/refresh`, {
      params: {
        userId,
        _t: timestamp,
        _r: random
      },
      headers: {
        'Cache-Control': 'no-cache, no-store',
        'Pragma': 'no-cache',
        'X-User-ID': String(userId)
      }
    })
  ]).then(([applications, response]) => {
    console.log('获取到的投递记录:', applications);
    console.log('刷新API返回数据:', response);

    if (response) {
      response.submittedCount = Number(response.submittedCount || 0);
      response.pendingCount = Number(response.pendingCount || 0);
      response.completedCount = Number(response.completedCount || 0);

      if (response.passRate) {
        if (typeof response.passRate !== 'string') {
          response.passRate = response.passRate.toFixed(2);
        }
        if (!response.passRate.includes('%')) {
          response.passRate = `${response.passRate}%`;
        }
      } else {
        response.passRate = '0%';
      }

      if (Array.isArray(applications) && applications.length > 0) {
        console.log('手动计算统计数据，确保与投递记录一致');
        
        // 根据状态统计各种情况
        // 状态映射：1-已投递, 2-待面试, 3-已面试, 4-已完成
        const statusCounts = {
          submitted: 0,    // 已投递 (status = 1)
          pending: 0,      // 待面试 (status = 2) 
          interviewed: 0,  // 已面试 (status = 3)
          completed: 0     // 已完成 (status = 4)
        };
        
        applications.forEach(app => {
          if (app && typeof app.status === 'number') {
            switch(app.status) {
              case 1:
                statusCounts.submitted++;
                break;
              case 2:
                statusCounts.pending++;
                break;
              case 3:
                statusCounts.interviewed++;
                break;
              case 4:
                statusCounts.completed++;
                break;
            }
          }
        });
        
        console.log('统计结果:', statusCounts);
        console.log('原API返回:', {
          submittedCount: response.submittedCount,
          pendingCount: response.pendingCount,
          completedCount: response.completedCount
        });
        
        // 修正统计数据
        response.submittedCount = Math.max(response.submittedCount, statusCounts.submitted);
        response.pendingCount = Math.max(response.pendingCount, statusCounts.pending);
        response.interviewedCount = Math.max(response.interviewedCount || 0, statusCounts.interviewed);
        response.completedCount = Math.max(response.completedCount, statusCounts.completed);
        
        console.log('修正后统计数据:', {
          submittedCount: response.submittedCount,
          pendingCount: response.pendingCount,
          interviewedCount: response.interviewedCount,
          completedCount: response.completedCount
        });
      }
    }
    
    return response;
  }).catch(error => {
    console.error('刷新统计数据API错误:', error);
    throw error;
  });
}

/**
 * 清空用户面试统计数据
 */
export function clearInterviewStatistics() {
  console.log('调用API: 清空面试统计数据');
  
  // 获取当前用户ID
  const userInfo = getUserInfo();
  const userId = userInfo ? userInfo.id : null;
  console.log('当前用户ID:', userId);
  
  // 如果没有用户ID，返回空数据
  if (!userId) {
    console.error('获取用户信息失败');
    return Promise.reject(new Error('获取用户信息失败'));
  }
  
  return request({
    url: `/api/statistics/interview/clear?userId=${userId}`,
    method: 'post',
    headers: {
      'X-User-ID': userId
    }
  }).then(response => {
    console.log('清空API返回数据:', response);
    return response;
  }).catch(error => {
    console.error('清空统计数据API错误:', error);
    throw error;
  });
} 

/**
 * 招聘分析 - KPI
 */
export function getRecruitmentKpis(timeRange = '30days') {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/recruitment-analysis/kpis', {
    params: { timeRange, userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => (res && res.data) ? res.data : (res || {}))
}

/**
 * 招聘分析 - 时间线
 */
export function getRecruitmentTimeline(timeRange = '30days', displayType = 'all') {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/recruitment-analysis/timeline', {
    params: { timeRange, displayType, userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => (res && res.data) ? res.data : (res || { labels: [], series: [] }))
}

/**
 * 招聘分析 - 漏斗
 */
export function getRecruitmentFunnel(period = 'total') {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/recruitment-analysis/funnel', {
    params: { period, userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => (res && res.data) ? res.data : (res || {}))
}

/**
 * 招聘分析 - 岗位热度
 */
export function getRecruitmentHeat(period = 'total', limit = 10) {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/recruitment-analysis/heat', {
    params: { period, limit, userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => (res && res.data) ? res.data : (res || { items: [] }))
}

/**
 * 招聘分析 - 侧边栏统计与热门岗位
 */
export function getRecruitmentSidebar(period = 'quarter', topN = 3, timeRangeForTop = '30days') {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/recruitment-analysis/sidebar', {
    params: { period, topN, timeRangeForTop, userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => (res && res.data) ? res.data : (res || { recentStats: {}, recentJobs: [] }))
}

/**
 * 招聘分析 - 刷新
 */
export function refreshRecruitmentAnalysis() {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/recruitment-analysis/refresh', {
    params: { userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => (res && res.data) ? res.data : (res || {}))
} 