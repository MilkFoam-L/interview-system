import request from '@/utils/request'

export function fetchTtsAudio(text) {
  return request({
    url: '/api/tts',
    method: 'post',
    data: { text },
    responseType: 'arraybuffer',
  });
}
 