<template>
  <el-dialog
    v-model="dialogVisible"
    title="代码实操题环节"
    width="420px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="code-test-guide-dialog"
    center
  >
    <!-- 指引内容 -->
    <div class="guide-content">
      <!-- 主要信息 -->
      <div class="main-info">
        <div class="info-icon">
          <el-icon class="icon-large" :class="{ 'playing': isPlayingVoice }">
            <Promotion />
          </el-icon>
        </div>
        <div class="info-text">
          <h3 class="title">代码实操题</h3>
          <p class="description">展现编程技能，注意时间</p>
        </div>
      </div>
      
      <!-- 时间提醒 -->
      <div class="time-reminder-box">
        <div class="time-icon">
          <el-icon>
            <AlarmClock />
          </el-icon>
        </div>
        <div class="time-text">
          每道题限时 <strong>5分钟</strong>，请合理安排作答时间
        </div>
      </div>
      
      <!-- 语音播放状态 -->
      <div class="voice-status" v-if="isPlayingVoice">
        <el-icon class="voice-icon spinning">
          <Mic />
        </el-icon>
        <span class="voice-text">正在播放语音指引</span>
        <el-button type="primary" size="small" plain @click="handleAutoStart" style="margin-left: 12px;">
          跳过
        </el-button>
      </div>

      <!-- 自动开始提示 -->
      <div class="auto-start-hint" v-if="!isPlayingVoice && voiceCompleted">
        <el-icon class="countdown-icon">
          <CaretRight />
        </el-icon>
        <span>即将自动开始代码实操...</span>
      </div>
    </div>
    
    <!-- 隐藏的音频元素 -->
    <audio
      ref="audioElementRef"
      style="display: none;"
      @ended="handleAudioEnded"
      @canplay="handleAudioCanPlay"
      @error="handleAudioError"
    />
  </el-dialog>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Promotion,
  AlarmClock,
  Mic, 
  CaretRight
} from '@element-plus/icons-vue'
import { fetchTtsAudio } from '@/api/tts'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits(['close', 'start'])

// 响应式数据
const dialogVisible = ref(false)
const isPlayingVoice = ref(false)
const voiceCompleted = ref(false)
const isAudioReady = ref(false)
const audioElementRef = ref(null)

// 语音指引文本
const guideText = '接下来是代码实操题环节。您将完成编程相关的实操题目。请注意，每道题的作答时间限制为5分钟，请合理安排时间，仔细阅读题目要求，认真编写代码。现在开始代码实操题。'

// 标记是否已请求停止
const voiceStopped = ref(false)

// 停止语音播放
const stopVoice = () => {
  console.log('🔇 代码实操指引：强制停止语音播放')
  voiceStopped.value = true
  if (audioElementRef.value) {
    try {
      audioElementRef.value.pause()
      audioElementRef.value.currentTime = 0
      audioElementRef.value.src = ''
    } catch (error) {
      console.warn('停止音频时出现警告:', error)
    }
  }
  isPlayingVoice.value = false
  voiceCompleted.value = true
}

// 播放语音指引
const playVoice = async () => {
  if (isPlayingVoice.value) return

  try {
    console.log('🎵 开始播放代码实操指引语音')
    isPlayingVoice.value = true
    voiceCompleted.value = false
    voiceStopped.value = false

    const audioData = await fetchTtsAudio(guideText)

    if (voiceStopped.value) {
      console.log('🔇 用户已跳过，不再播放')
      return
    }

    if (!audioData) {
      throw new Error('获取音频数据失败')
    }

    const blob = new Blob([audioData], { type: 'audio/mpeg' })
    const audioUrl = URL.createObjectURL(blob)

    if (audioElementRef.value && !voiceStopped.value) {
      audioElementRef.value.src = audioUrl
      audioElementRef.value.addEventListener('ended', () => {
        URL.revokeObjectURL(audioUrl)
      }, { once: true })
      await audioElementRef.value.play()
      console.log('🎵 代码实操指引语音开始播放')
    } else {
      URL.revokeObjectURL(audioUrl)
    }
  } catch (error) {
    if (!voiceStopped.value) {
      console.error('🎵 播放代码实操指引语音失败:', error)
    }
    isPlayingVoice.value = false
    voiceCompleted.value = true
    // 即使语音播放失败，也要在短暂延迟后开始实操
    setTimeout(() => {
      handleAutoStart()
    }, 1500)
  }
}

// 监听弹窗显示状态
watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal
  if (newVal) {
    // 弹窗打开时自动播放语音
    setTimeout(() => {
      playVoice()
    }, 500)
  } else {
    // 弹窗关闭时停止语音
    stopVoice()
  }
}, { immediate: true })

// 音频播放结束
const handleAudioEnded = () => {
  console.log('🎵 代码实操指引语音播放结束')
  isPlayingVoice.value = false
  voiceCompleted.value = true
  
  // 语音播放完成后，自动开始代码实操
  setTimeout(() => {
    handleAutoStart()
  }, 1500) // 1.5秒后自动开始
}

// 音频准备就绪
const handleAudioCanPlay = () => {
  isAudioReady.value = true
  console.log('🎵 代码实操指引音频准备就绪')
}

// 音频播放错误
const handleAudioError = (error) => {
  // 只在实际播放状态下才报告错误，避免停止时的误报
  if (isPlayingVoice.value) {
    console.error('🎵 代码实操指引音频播放错误:', error)
    console.error('语音播放出现错误，将自动开始代码实操')
    // 出错时也要自动开始实操
    setTimeout(() => {
      handleAutoStart()
    }, 1500)
  } else {
    console.log('🔇 音频停止时的正常错误，忽略')
  }
  isPlayingVoice.value = false
  voiceCompleted.value = true
}

// 自动开始代码实操
const handleAutoStart = () => {
  console.log('自动开始代码实操环节')
  stopVoice()
  emit('start')
  emit('close')
}

// 组件卸载时清理
onUnmounted(() => {
  stopVoice()
})
</script>

<style scoped>
.code-test-guide-dialog {
  --el-dialog-border-radius: 16px;
}

.code-test-guide-dialog :deep(.el-dialog) {
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}

.code-test-guide-dialog :deep(.el-dialog__header) {
  padding: 28px 28px 18px 28px;
  text-align: center;
  border-bottom: none;
  background: linear-gradient(135deg, #fa541c 0%, #ff7a45 100%);
  border-radius: 16px 16px 0 0;
}

.code-test-guide-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  color: white;
}

.code-test-guide-dialog :deep(.el-dialog__body) {
  padding: 20px 28px 24px 28px;
}

.guide-content {
  text-align: center;
}

.main-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.info-icon {
  position: relative;
}

.icon-large {
  font-size: 64px;
  color: #fa541c;
  transition: all 0.3s ease;
}

.icon-large.playing {
  color: #ff7a45;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.8; }
  100% { transform: scale(1); opacity: 1; }
}

.info-text {
  text-align: center;
}

.title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.2;
}

.description {
  margin: 0;
  font-size: 16px;
  color: #666;
  line-height: 1.4;
}

.time-reminder-box {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: linear-gradient(135deg, #fff2e8 0%, #fff1f0 100%);
  border: 2px solid #ffbb96;
  border-radius: 12px;
  margin-bottom: 18px;
  text-align: left;
}

.time-icon {
  flex-shrink: 0;
  color: #fa541c;
  font-size: 20px;
  margin-top: 1px;
}

.time-text {
  flex: 1;
  font-size: 14px;
  color: #d4380d;
  line-height: 1.4;
}

.time-text strong {
  color: #fa541c;
  font-size: 16px;
  font-weight: 700;
}

.voice-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #fff2e8 0%, #fff1f0 100%);
  border-radius: 20px;
  border: 2px solid #ffbb96;
  margin-bottom: 12px;
}

.voice-icon {
  font-size: 18px;
  color: #fa541c;
}

.voice-text {
  font-size: 14px;
  color: #fa541c;
  font-weight: 500;
}

.spinning {
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.auto-start-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #f6f6f6 0%, #fafafa 100%);
  border-radius: 16px;
  border: 2px solid #d9d9d9;
  color: #595959;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.countdown-icon {
  font-size: 16px;
  color: #fa541c;
  animation: slide 1s ease-in-out infinite;
}

@keyframes slide {
  0%, 100% {
    transform: translateX(0);
  }
  50% {
    transform: translateX(4px);
  }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .code-test-guide-dialog :deep(.el-dialog) {
    margin: 0 16px;
    width: calc(100% - 32px);
  }
  
  .code-test-guide-dialog :deep(.el-dialog__header) {
    padding: 20px 20px 14px 20px;
  }
  
  .code-test-guide-dialog :deep(.el-dialog__body) {
    padding: 16px 20px 20px 20px;
  }
  
  .code-test-guide-dialog :deep(.el-dialog__title) {
    font-size: 18px;
  }
  
  .icon-large {
    font-size: 48px;
  }
  
  .title {
    font-size: 20px;
  }
  
  .description {
    font-size: 14px;
  }
  
  .time-reminder-box {
    padding: 14px;
  }
  
  .time-text {
    font-size: 13px;
  }
  
  .time-text strong {
    font-size: 15px;
  }
}
</style>