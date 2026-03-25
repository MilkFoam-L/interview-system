<template>
  <el-dialog
    v-model="dialogVisible"
    title="自我介绍环节"
    width="400px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="self-introduction-guide-dialog"
    center
  >
    <!-- 指引内容 -->
    <div class="guide-content">
      <!-- 主要信息 -->
      <div class="main-info">
        <div class="info-icon">
          <el-icon class="icon-large" :class="{ 'playing': isPlayingVoice }">
            <UserFilled />
          </el-icon>
        </div>
        <div class="info-text">
          <h3 class="title">60秒自我介绍</h3>
          <p class="description">请保持自信，展示最好的自己</p>
        </div>
      </div>
      
      <!-- 重要提醒 -->
      <div class="warning-box">
        <div class="warning-icon">
          <el-icon>
            <Warning />
          </el-icon>
        </div>
        <div class="warning-text">
          请确保摄像头中只有您一个人，否则面试将被中断
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
          @click="handleStart"
          class="start-btn"
        >
          <el-icon><VideoPlay /></el-icon>
          {{ isPlayingVoice ? '跳过语音' : '开始自我介绍' }}
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
import { ref, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  UserFilled,
  Warning,
  Mic, 
  VideoPlay,
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
const emit = defineEmits(['close', 'start'])

// 响应式数据
const dialogVisible = ref(false)
const isPlayingVoice = ref(false)
const voiceCompleted = ref(false)
const isAudioReady = ref(false)
const audioElementRef = ref(null)

// 语音指引文本
const guideText = '接下来是60秒自我介绍环节。请介绍您的教育背景、专业技能和工作经验。注意：摄像头中只能有您一个人，否则面试将被中断。保持自信，注视摄像头，准备好后点击开始按钮。'

// 标记是否已请求停止
const voiceStopped = ref(false)

// 停止语音播放
const stopVoice = () => {
  console.log('🔇 自我介绍指引：强制停止语音播放')
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
    console.log('🎵 开始播放自我介绍指引语音')
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
      console.log('🎵 自我介绍指引语音开始播放')
    } else {
      URL.revokeObjectURL(audioUrl)
    }
  } catch (error) {
    if (!voiceStopped.value) {
      console.error('🎵 播放自我介绍指引语音失败:', error)
    }
    console.error('语音播放失败，请稍后重试')
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
  console.log('🎵 自我介绍指引语音播放结束')
  isPlayingVoice.value = false
  voiceCompleted.value = true
}

// 音频准备就绪
const handleAudioCanPlay = () => {
  isAudioReady.value = true
  console.log('🎵 自我介绍指引音频准备就绪')
}

// 音频播放错误
const handleAudioError = (error) => {
  // 只在实际播放状态下才报告错误，避免停止时的误报
  if (isPlayingVoice.value) {
    console.error('🎵 自我介绍指引音频播放错误:', error)
    console.error('语音播放出现错误')
  } else {
    console.log('🔇 音频停止时的正常错误，忽略')
  }
  isPlayingVoice.value = false
  voiceCompleted.value = true
}

// 开始自我介绍按钮点击
const handleStart = () => {
  console.log('用户确认开始自我介绍环节')
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
.self-introduction-guide-dialog {
  --el-dialog-border-radius: 16px;
}

.self-introduction-guide-dialog :deep(.el-dialog) {
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}

.self-introduction-guide-dialog :deep(.el-dialog__header) {
  padding: 32px 32px 20px 32px;
  text-align: center;
  border-bottom: none;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px 16px 0 0;
}

.self-introduction-guide-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  color: white;
}

.self-introduction-guide-dialog :deep(.el-dialog__body) {
  padding: 20px 28px 16px 28px;
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

.warning-box {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: linear-gradient(135deg, #fff7e6 0%, #fef5e7 100%);
  border: 2px solid #ffa940;
  border-radius: 12px;
  margin-bottom: 18px;
  text-align: left;
}

.warning-icon {
  flex-shrink: 0;
  color: #fa8c16;
  font-size: 18px;
  margin-top: 1px;
}

.warning-text {
  flex: 1;
  font-size: 14px;
  color: #d46b08;
  line-height: 1.4;
}

.voice-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  border-radius: 20px;
  border: 2px solid #91d5ff;
  margin-bottom: 12px;
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
  padding: 16px 0;
}

.start-btn {
  padding: 14px 40px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 24px;
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
  border: none;
  transition: all 0.3s ease;
  min-width: 180px;
}

.start-btn:hover {
  background: linear-gradient(135deg, #389e0d 0%, #237804 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(82, 196, 26, 0.4);
}

.start-btn .el-icon {
  margin-right: 8px;
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
  .self-introduction-guide-dialog :deep(.el-dialog) {
    margin: 0 16px;
    width: calc(100% - 32px);
  }
  
  .self-introduction-guide-dialog :deep(.el-dialog__header) {
    padding: 20px 20px 14px 20px;
  }
  
  .self-introduction-guide-dialog :deep(.el-dialog__body) {
    padding: 16px 20px 12px 20px;
  }
  
  .self-introduction-guide-dialog :deep(.el-dialog__title) {
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
  
  .start-btn {
    padding: 12px 32px;
    font-size: 15px;
    min-width: 160px;
  }
}
</style>