import request from '@/utils/request'

/**
 * 获取表情分析数据
 */
export const getExpressionAnalysis = (sessionId) => {
  return request.get(`/api/comprehensive-report/expression/${sessionId}`)
}

/**
 * 获取STAR结构分数
 */
export const getStarScores = (sessionId) => {
  return request.get(`/api/comprehensive-report/star/${sessionId}`)
}

/**
 * 获取问题回答质量分析
 */
export const getAnswerQuality = (sessionId) => {
  return request.get(`/api/comprehensive-report/answer-quality/${sessionId}`)
}

/**
 * 获取能力评估矩阵数据
 */
export const getAbilityMatrix = (sessionId) => {
  return request.get(`/api/comprehensive-report/ability-matrix/${sessionId}`)
}

/**
 * 获取用户面试报告列表
 */
export const getReportList = (params) => {
  return request.get('/api/comprehensive-report/list', { params })
}

/**
 * 获取用户统计数据
 */
export const getUserStats = (userId) => {
  return request.get('/api/comprehensive-report/stats', { params: { userId } })
}

/**
 * 创建或更新面试开始时间
 */
export const updateStartTime = (data) => {
  return request.post('/api/comprehensive-report/start-time', data)
}

/**
 * 更新面试结束时间并计算时长
 */
export const updateEndTime = (data) => {
  return request.post('/api/comprehensive-report/end-time', data)
}

/**
 * 根据会话ID获取综合报告
 */
export const getReportBySessionId = (sessionId) => {
  return request.get(`/api/comprehensive-report/session/${sessionId}`)
}

/**
 * 生成并更新STAR分数
 */
export const generateStarScores = (sessionId) => {
  return request.post(`/api/comprehensive-report/generate-star/${sessionId}`)
}

/**
 * 获取技术能力分析结果（基础问答 + 代码题）
 */
export const getTechAnalysis = (sessionId) => {
  return request.get(`/api/comprehensive-report/tech-analysis/${sessionId}`)
}

/**
 * 生成能力评估矩阵
 */
export const generateAbilityMatrix = (sessionId, forceRegenerate = false) => {
  return request.post(`/api/comprehensive-report/generate-ability-matrix/${sessionId}`, {
    forceRegenerate
  })
}

/**
 * 检查能力评估矩阵是否已生成
 */
export const checkAbilityMatrixStatus = (sessionId) => {
  return request.get(`/api/comprehensive-report/ability-matrix-status/${sessionId}`)
}

/**
 * 生成综合分析简报
 */
export const generateComprehensiveAnalysis = (sessionId, forceRegenerate = false) => {
  return request.post(`/api/comprehensive-report/generate-comprehensive-analysis/${sessionId}`, {
    forceRegenerate
  })
}

/**
 * 检查综合分析简报是否已生成
 */
export const checkComprehensiveAnalysisStatus = (sessionId) => {
  return request.get(`/api/comprehensive-report/comprehensive-analysis-status/${sessionId}`)
}

/**
 * 获取综合分析简报数据
 */
export const getComprehensiveAnalysis = (sessionId) => {
  return request.get(`/api/comprehensive-report/comprehensive-analysis/${sessionId}`)
}

/**
 * 生成个性化建议
 */
export const generatePersonalizedSuggestions = (sessionId, forceRegenerate = false) => {
  return request.post(`/api/comprehensive-report/generate-personalized-suggestions/${sessionId}`, {
    forceRegenerate
  })
}

/**
 * 获取个性化建议数据
 */
export const getPersonalizedSuggestions = (sessionId) => {
  return request.get(`/api/comprehensive-report/personalized-suggestions/${sessionId}`)
}

/**
 * 检查个性化建议是否已生成
 */
export const checkPersonalizedSuggestionsStatus = (sessionId) => {
  return request.get(`/api/comprehensive-report/personalized-suggestions-status/${sessionId}`)
} 