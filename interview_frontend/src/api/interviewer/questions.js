import request from '../../utils/request'

/**
 * 面试官端题目管理API
 * 路径前缀: /api/interviewer/questions
 */

/**
 * 获取题目分页列表
 * @param {Object} params - 查询参数
 * @param {number} params.page - 页码（从0开始）
 * @param {number} params.size - 每页大小
 * @param {string} [params.category] - 大类别
 * @param {string} [params.categoryType] - 细分类别
 * @param {string} [params.type] - 题目类型
 * @param {string} [params.difficulty] - 难度
 * @param {string} [params.keyword] - 关键词搜索
 * @returns {Promise<Object>} 分页结果
 */
export function getQuestions(params = {}) {
  return request({
    url: '/api/interviewer/questions',
    method: 'get',
    params
  })
}

/**
 * 根据ID获取题目详情
 * @param {number} id - 题目ID
 * @returns {Promise<Object>} 题目详情
 */
export function getQuestionById(id) {
  return request({
    url: `/api/interviewer/questions/${id}`,
    method: 'get'
  })
}

/**
 * 根据ID获取题目详情（包含用户名信息）
 * @param {number} id - 题目ID
 * @returns {Promise<Object>} 题目详情（包含createdByName和updatedByName）
 */
export function getQuestionDetailById(id) {
  return request({
    url: `/api/interviewer/questions/${id}/detail`,
    method: 'get'
  })
}

/**
 * 创建新题目
 * @param {Object} question - 题目数据
 * @returns {Promise<Object>} 创建的题目
 */
export function createQuestion(question) {
  return request({
    url: '/api/interviewer/questions',
    method: 'post',
    data: question
  })
}

/**
 * 更新题目
 * @param {number} id - 题目ID
 * @param {Object} question - 题目数据
 * @returns {Promise<Object>} 更新的题目
 */
export function updateQuestion(id, question) {
  return request({
    url: `/api/interviewer/questions/${id}`,
    method: 'put',
    data: question
  })
}

/**
 * 删除题目（硬删除）
 * @param {number} id - 题目ID
 * @returns {Promise<void>}
 */
export function deleteQuestion(id) {
  return request({
    url: `/api/interviewer/questions/${id}`,
    method: 'delete'
  })
}

/**
 * 批量删除题目
 * @param {Array<number>} questionIds - 题目ID数组
 * @returns {Promise<Object>} 删除结果
 */
export function batchDeleteQuestions(questionIds) {
  return request({
    url: '/api/interviewer/questions/batch',
    method: 'delete',
    data: questionIds
  })
}

/**
 * Excel批量上传题目
 * @param {File} file - Excel文件
 * @returns {Promise<Object>} 上传结果
 */
export function uploadQuestionsFromExcel(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: '/api/interviewer/questions/upload/excel',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 60000 // 上传文件增加超时时间
  })
}

/**
 * 下载Excel模板
 * @returns {Promise<Blob>} Excel文件流
 */
export function downloadExcelTemplate() {
  return request({
    url: '/api/interviewer/questions/template/excel',
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 获取题目使用统计
 * @param {Object} params - 统计参数
 * @param {string} [params.category] - 大类别
 * @param {string} [params.categoryType] - 细分类别
 * @param {string} [params.type] - 题目类型
 * @returns {Promise<Object>} 统计数据
 */
export function getQuestionStatistics(params = {}) {
  return request({
    url: '/api/interviewer/questions/statistics',
    method: 'get',
    params
  })
}

/**
 * 获取所有类别选项
 * @returns {Promise<Object>} 类别选项
 */
export function getQuestionCategories() {
  return request({
    url: '/api/interviewer/questions/categories',
    method: 'get'
  })
}

/**
 * 切换题目启用状态
 * @param {number} id - 题目ID
 * @returns {Promise<Object>} 更新的题目
 */
export function toggleQuestionActive(id) {
  return request({
    url: `/api/interviewer/questions/${id}/toggle-active`,
    method: 'patch'
  })
}