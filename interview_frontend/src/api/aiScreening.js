import request from '@/utils/request'

export function fetchCandidates(params) {
  return request({
    url: '/api/ai-screening/list',
    method: 'get',
    params
  })
}

export function notifyPass(applicationId) {
  return request({
    url: `/api/ai-screening/${applicationId}/notify`,
    method: 'put'
  })
}

export function rejectCandidate(applicationId, reason) {
  return request({
    url: `/api/ai-screening/${applicationId}/reject`,
    method: 'put',
    data: { reason }
  })
}

export function batchNotify(ids) {
  return request({
    url: '/api/ai-screening/batch/notify',
    method: 'post',
    data: { ids }
  })
}

export function batchReject(ids, reason) {
  return request({
    url: '/api/ai-screening/batch/reject',
    method: 'post',
    data: { ids, reason }
  })
}

export function getReport(applicationId) {
  return request({
    url: `/api/ai-screening/${applicationId}/report`,
    method: 'get'
  })
} 