import request from '@/utils/request'

export function fetchSchedules(params) {
  return request({
    url: '/api/interview-schedule/list',
    method: 'get',
    params
  })
} 