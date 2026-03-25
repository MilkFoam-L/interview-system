import request from '@/utils/request'

export function fetchMyJobs(params = {}) {
  return request({
    url: '/api/jobs/my',
    method: 'get',
    params
  })
}

export function fetchMyJobsPage(params = {}) {
  return request({
    url: '/api/jobs/my/page',
    method: 'get',
    params
  })
}

export function fetchMyDepartments() {
  return request({
    url: '/api/jobs/my/departments',
    method: 'get'
  })
}

// 获取岗位详情
export function fetchJobDetail(id) {
  return request({
    url: `/api/jobs/${id}`,
    method: 'get'
  })
}

// 创建岗位
export function createJob(data) {
  return request({
    url: '/api/jobs',
    method: 'post',
    data
  })
}

// 更新岗位
export function updateJob(id, data) {
  return request({
    url: `/api/jobs/${id}`,
    method: 'put',
    data
  })
}

// 删除岗位
export function deleteJob(id) {
  return request({
    url: `/api/jobs/${id}`,
    method: 'delete'
  })
} 