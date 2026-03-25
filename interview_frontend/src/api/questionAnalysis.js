import request from '@/utils/request'
import { getUserInfo } from '@/utils/auth'

// 题库分析
export function getQuestionKpis() {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/question-analysis/kpis', {
    params: { userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => {
    const data = (res && res.data) ? res.data : (res || {})
    return {
      totalQuestions: Number(data.totalQuestions || 0),
      activeQuestions: Number(data.activeQuestions || 0),
      usedQuestions: Number(data.usedQuestions || 0),
      coverageRate: Number(data.coverageRate || (data.totalQuestions ? (100 * (Number(data.usedQuestions || 0)) / Number(data.totalQuestions)).toFixed ? Number((100 * (Number(data.usedQuestions || 0)) / Number(data.totalQuestions)).toFixed(2)) : 0 : 0))
    }
  }).catch(() => ({ totalQuestions: 0, activeQuestions: 0, usedQuestions: 0, coverageRate: 0 }))
}

// 题型正确率/错误率
export function getTypeAccuracy() {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/question-analysis/type-accuracy', {
    params: { userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => {
    const data = (res && res.data) ? res.data : (res || { items: [] })
    const items = Array.isArray(data.items) ? data.items : []
    return items.map(it => ({
      type: it.type || it.name || 'unknown',
      correctRate: Number(it.correctRate || 0),
      wrongRate: Number(it.wrongRate || (100 - Number(it.correctRate || 0))),
      attempts: Number(it.attempts || 0)
    }))
  }).catch(() => ([
    { type: 'basic', correctRate: 62, wrongRate: 38, attempts: 0 },
    { type: 'code', correctRate: 48, wrongRate: 52, attempts: 0 },
    { type: 'choice', correctRate: 72, wrongRate: 28, attempts: 0 }
  ]))
}

// 题型占比
export function getQuestionTypeDistribution() {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/question-analysis/type-distribution', {
    params: { userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => {
    const data = (res && res.data) ? res.data : (res || { items: [] })
    const items = Array.isArray(data.items) ? data.items : []
    return items.map(it => ({ name: it.name || it.type, value: Number(it.value || it.count || 0) }))
  }).catch(() => ([
    { name: 'basic', value: 45 },
    { name: 'code', value: 35 },
    { name: 'scenario', value: 20 }
  ]))
}

// 分类占比
export function getCategoryDistribution(mode = 'major', limit = 12) {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/question-analysis/category-distribution', {
    params: { userId, mode, limit, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => {
    const data = (res && res.data) ? res.data : (res || { items: [] })
    const items = Array.isArray(data.items) ? data.items : []
    return items.map(it => ({ name: it.name || it.category || it.categoryType, value: Number(it.value || it.count || 0) }))
  }).catch(() => {
    if (mode === 'major') {
      return [
        { name: 'IT/互联网', value: 36 },
        { name: '人工智能', value: 28 },
        { name: '大数据', value: 22 },
        { name: '智能系统', value: 18 },
        { name: '物联网', value: 16 }
      ]
    }
    return [
      { name: '云计算', value: 18 },
      { name: '前端开发', value: 22 },
      { name: '后端开发', value: 26 },
      { name: '数据分析', value: 14 },
      { name: '测试/QA', value: 12 },
      { name: '算法', value: 16 },
      { name: '网络安全', value: 10 },
      { name: '运维/DevOps', value: 12 }
    ]
  })
}

// 难度分布
export function getDifficultyDistribution() {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/question-analysis/difficulty-distribution', {
    params: { userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => {
    const data = (res && res.data) ? res.data : (res || {})
    const simple = Number(data.simple || data.easy || 0)
    const medium = Number(data.medium || 0)
    const hard = Number(data.hard || 0)
    return { simple, medium, hard }
  }).catch(() => ({ simple: 42, medium: 38, hard: 20 }))
}

// 标签下题量分布
export function getTagDistribution(limit = 12) {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/question-analysis/tag-distribution', {
    params: { userId, limit, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => {
    const data = (res && res.data) ? res.data : (res || { items: [] })
    const items = Array.isArray(data.items) ? data.items : []
    return items.map(it => ({ tag: it.tag || it.name, count: Number(it.count || 0) }))
  }).catch(() => ([
    { tag: 'Java', count: 28 },
    { tag: 'Spring', count: 21 },
    { tag: 'MySQL', count: 19 },
    { tag: 'Redis', count: 16 },
    { tag: 'Vue', count: 14 },
    { tag: 'React', count: 12 }
  ]))
}

// 更新信息
export function getQuestionBankUpdateInfo() {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/question-analysis/update-info', {
    params: { userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => {
    const data = (res && res.data) ? res.data : (res || {})
    return {
      daysSinceUpdate: Number(data.daysSinceUpdate || 0),
      lastUpdatedAt: data.lastUpdatedAt || ''
    }
  }).catch(() => ({ daysSinceUpdate: 15, lastUpdatedAt: '' }))
}

// 整体刷新
export function refreshQuestionAnalysis() {
  const { id: userId } = getUserInfo() || {}
  return request.get('/api/question-analysis/refresh', {
    params: { userId, _t: Date.now() },
    headers: { 'X-User-ID': String(userId || '') }
  }).then(res => (res && res.data) ? res.data : (res || {}))
} 