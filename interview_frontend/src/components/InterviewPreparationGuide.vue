<template>
  <el-dialog
    v-model="dialogVisible"
    title="面试准备"
    width="420px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="preparation-guide-dialog"
    center
  >
    <!-- 简化的指引内容 -->
    <div class="guide-content">
      <!-- 主要提示信息 -->
      <div class="main-info">
        <div class="info-icon">
          <el-icon class="icon-large" :class="{ 'playing': isPlayingVoice }">
            <User />
          </el-icon>
        </div>
        <div class="info-text">
          <h3 class="title">面试即将开始</h3>
          <p class="description">请确保设备正常，环境安静</p>
        </div>
      </div>
      
      <!-- 语音播放状态 -->
      <div class="voice-status" v-if="isPlayingVoice">
        <el-icon class="voice-icon spinning">
          <Mic />
        </el-icon>
        <span class="voice-text">正在播放语音指引</span>
      </div>
    </div>
    
    <!-- 底部按钮 -->
    <template #footer>
      <div class="dialog-footer">
        <el-button
          type="primary"
          size="large"
          @click="handleConfirm"
          class="confirm-btn"
        >
          {{ isPlayingVoice ? '跳过语音' : '我知道了' }}
        </el-button>
      </div>
    </template>
    
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
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Mic, 
  User, 
  Loading 
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
const emit = defineEmits(['close', 'confirm'])

// 响应式数据
const dialogVisible = ref(false)
const isPlayingVoice = ref(false)
const voiceCompleted = ref(false)
const isAudioReady = ref(false)
const audioElementRef = ref(null)

// 语音指引文本
const guideText = '欢迎参加面试！请确保您的摄像头和麦克风设备正常工作，周围环境安静。面试过程中请保持面部居中在摄像头画面中，以确保人脸识别的准确性。准备就绪后，点击按钮开始面试。'

// 标记是否已请求停止（防止 TTS 异步返回后继续播放）
const voiceStopped = ref(false)

// 停止语音播放
const stopVoice = () => {
  console.log('🔇 准备面试指引：强制停止语音播放')
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
    console.log('🎵 开始播放语音指引')
    isPlayingVoice.value = true
    voiceCompleted.value = false
    voiceStopped.value = false

    // 获取TTS音频数据
    const audioData = await fetchTtsAudio(guideText)

    // TTS返回后检查是否已被用户跳过
    if (voiceStopped.value) {
      console.log('🔇 用户已跳过，不再播放')
      return
    }

    if (!audioData) {
      throw new Error('获取音频数据失败')
    }

    // 创建音频Blob并播放
    const blob = new Blob([audioData], { type: 'audio/mpeg' })
    const audioUrl = URL.createObjectURL(blob)

    if (audioElementRef.value && !voiceStopped.value) {
      audioElementRef.value.src = audioUrl

      // 监听音频结束事件，清理URL
      audioElementRef.value.addEventListener('ended', () => {
        URL.revokeObjectURL(audioUrl)
      }, { once: true })

      await audioElementRef.value.play()
      console.log('🎵 音频开始播放')
    } else {
      URL.revokeObjectURL(audioUrl)
    }
  } catch (error) {
    if (!voiceStopped.value) {
      console.error('🎵 播放语音指引失败:', error)
    }
    isPlayingVoice.value = false
    voiceCompleted.value = true
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
  console.log('🎵 音频播放结束')
  isPlayingVoice.value = false
  voiceCompleted.value = true
}

// 音频准备就绪
const handleAudioCanPlay = () => {
  isAudioReady.value = true
  console.log('🎵 音频准备就绪')
}

// 音频播放错误
const handleAudioError = (error) => {
  // 只在实际播放状态下才报告错误，避免停止时的误报
  if (isPlayingVoice.value) {
    console.error('🎵 音频播放错误:', error)
    console.error('语音播放出现错误')
  } else {
    console.log('🔇 音频停止时的正常错误，忽略')
  }
  isPlayingVoice.value = false
  voiceCompleted.value = true
}

// 确认按钮点击
const handleConfirm = () => {
  console.log('用户确认已了解面试准备指引')
  stopVoice()
  emit('confirm')
  emit('close')
}

// 组件卸载时清理
onUnmounted(() => {
  stopVoice()
})
</script>

<style scoped>
.preparation-guide-dialog {
  --el-dialog-border-radius: 16px;
}

.preparation-guide-dialog :deep(.el-dialog) {
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}

.preparation-guide-dialog :deep(.el-dialog__header) {
  padding: 32px 32px 20px 32px;
  text-align: center;
  border-bottom: none;
}

.preparation-guide-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.preparation-guide-dialog :deep(.el-dialog__body) {
  padding: 0 32px 20px 32px;
}

.guide-content {
  text-align: center;
}

.main-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.info-icon {
  position: relative;
}

.icon-large {
  font-size: 64px;
  color: #667eea;
  transition: all 0.3s ease;
}

.icon-large.playing {
  color: #52c41a;
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

.voice-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px 24px;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  border-radius: 24px;
  border: 2px solid #91d5ff;
  margin-bottom: 16px;
}

.voice-icon {
  font-size: 18px;
  color: #1890ff;
}

.voice-text {
  font-size: 14px;
  color: #1890ff;
  font-weight: 500;
}

.spinning {
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.dialog-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px 0;
}

.confirm-btn {
  padding: 14px 40px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s ease;
  min-width: 140px;
}

.confirm-btn:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.status-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #666;
  font-size: 14px;
  font-weight: 500;
}

.loading-icon {
  animation: spin 1s linear infinite;
  font-size: 16px;
  color: #1890ff;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .preparation-guide-dialog :deep(.el-dialog) {
    margin: 0 16px;
    width: calc(100% - 32px);
  }
  
  .preparation-guide-dialog :deep(.el-dialog__header) {
    padding: 24px 24px 16px 24px;
  }
  
  .preparation-guide-dialog :deep(.el-dialog__body) {
    padding: 0 24px 16px 24px;
  }
  
  .preparation-guide-dialog :deep(.el-dialog__title) {
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
  
  .confirm-btn {
    padding: 12px 32px;
    font-size: 15px;
  }
}
</style>