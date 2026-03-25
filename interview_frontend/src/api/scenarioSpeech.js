// 基于浏览器 WebSocket 的场景语音转写工具

// 用法示例：
// import { connectScenarioSpeech } from "@/api/scenarioSpeech";
// const ws = connectScenarioSpeech(scenarioId, questionNo, onSubtitle);
// // 在录音循环中
// ws.send(JSON.stringify({ type: 'audio', sessionId, audioData: base64PCM }));
// // 录音结束
// ws.send(JSON.stringify({ type: 'end', sessionId }));

/**
 * 建立场景级语音转写 WebSocket
 * @param {string|number} scenarioId  场景 ID
 * @param {number} questionNo         当前题号（从 1 开始）
 * @param {(subtitle:string)=>void} onSubtitle 回调：增量字幕
 * @param {(err:string)=>void} onError        回调：错误信息
 * @returns {WebSocket}
 */
export function connectScenarioSpeech(scenarioId, questionNo, onSubtitle, onError) {
  // 与后端约定 sessionId = `${scenarioId}-${questionNo}`
  const sessionId = `${scenarioId}-${questionNo}`;
  const wsUrl = `${import.meta.env.VITE_WS_BASE || location.origin.replace(/^http/, 'ws')}/ws/speech/${sessionId}`;
  const ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    console.info('[ScenarioSpeech] WebSocket opened', wsUrl);
  };

  ws.onmessage = (ev) => {
    try {
      const data = JSON.parse(ev.data);
      if (data.type === 'transcription') {
        onSubtitle && onSubtitle(data.content || '');
      } else if (data.type === 'error') {
        onError && onError(data.content || '未知错误');
      }
    } catch (e) {
      // 兼容旧实现：服务器可能直接推纯文本增量
      onSubtitle && onSubtitle(ev.data);
    }
  };

  ws.onerror = (ev) => {
    console.error('[ScenarioSpeech] WebSocket error', ev);
    onError && onError('WebSocket error');
  };

  return ws;
} 