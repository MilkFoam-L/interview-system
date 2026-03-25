import request from '@/utils/request'

/**
 * 获取当前面试官的公司信息
 */
export function getMyCompanyInfo() {
  return request({
    url: '/api/companies/my-company',
    method: 'get'
  })
}

/**
 * 创建公司信息
 * @param {Object} data 公司信息数据
 */
export function createCompanyInfo(data) {
  return request({
    url: '/api/companies',
    method: 'post',
    data
  })
}

/**
 * 更新公司信息
 * @param {number} id 公司ID
 * @param {Object} data 公司信息数据
 */
export function updateCompanyInfo(id, data) {
  return request({
    url: `/api/companies/${id}`,
    method: 'put',
    data
  })
}

/**
 * 上传公司Logo
 * @param {number} companyId 公司ID
 * @param {File} file 图片文件
 */
export function uploadCompanyLogo(companyId, file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: `/api/companies/${companyId}/logo`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 上传营业执照
 * @param {number} companyId 公司ID
 * @param {File} file 图片文件
 */
export function uploadBusinessLicense(companyId, file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return request({
    url: `/api/companies/${companyId}/license`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取公司信息详情
 * @param {number} id 公司ID
 */
export function getCompanyById(id) {
  return request({
    url: `/api/companies/${id}`,
    method: 'get'
  })
}

/**
 * 获取公司列表（带搜索）
 * @param {Object} params 搜索参数
 */
export function getCompanies(params) {
  return request({
    url: '/api/companies',
    method: 'get',
    params
  })
}

