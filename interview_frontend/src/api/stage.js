import request from '@/utils/request';

export function getStageProgress(sessionId) {
  return request.get(`/api/stage-progress/session/${sessionId}`);
}

export function updateStageStatus(sessionId, stage, status) {
  return request.put(`/api/stage-progress/session/${sessionId}/stage/${stage}/status`, { params: { status } });
}

export function startStage(sessionId, stage) {
  return request.put(`/api/stage-progress/session/${sessionId}/stage/${stage}/start`);
}

export function completeStage(sessionId, stage, action = 'COMPLETE') {
  if (action === 'START') {
    return request.put(`/api/stage-progress/session/${sessionId}/stage/${stage}/start`);
  }
  return request.put(`/api/stage-progress/session/${sessionId}/stage/${stage}/complete`);
}

export function createStageProgress(data) {
  return request.post('/api/stage-progress', data);
} 