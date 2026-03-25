import request from '@/utils/request';

// 开始场景化问答，生成 5 道题
export const startScenario = (sessionId) =>
  request.post(`/api/sessions/${sessionId}/scenario/start`, null, {
    timeout: 180000, // 增加超时时间到180秒(3分钟)，因为生成5道场景题需要AI大量处理时间
  });

// 获取问题列表（含已作答情况）
export const fetchQuestions = (sessionId) =>
  request.get(`/api/sessions/${sessionId}/scenario`);

// 提交答案
export const submitAnswer = (sessionId, roundNo, answer) =>
  request.post(`/api/sessions/${sessionId}/scenario/answer`, answer, {
    params: { roundNo },
    headers: { 'Content-Type': 'text/plain' },
    timeout: 120000, // 增加超时时间到120秒，因为场景答案AI分析需要更长时间
  });

// 生成追问
export const generateFollowUp = (sessionId, roundNo) =>
  request.post(`/api/sessions/${sessionId}/scenario/follow-up`, null, {
    params: { roundNo },
    timeout: 90000, // 增加超时时间到90秒，因为追问生成也需要AI处理
  });

// 检查并完成阶段
export const completeStage = (sessionId) =>
  request.post(`/api/sessions/${sessionId}/scenario/complete`, null, {
    timeout: 120000, // 增加超时时间到120秒，因为完成阶段可能涉及AI分析
  });

// 获取综合报告
export const fetchScenarioReport = (sessionId) =>
  request.get(`/api/sessions/${sessionId}/scenario/report`, {
    timeout: 120000, // 增加超时时间到120秒，因为生成报告可能涉及AI分析
  }); 