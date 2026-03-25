<template>
  <el-dialog
    v-model="dialogVisible"
    title="基础问答环节"
    width="380px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="basic-qa-guide-dialog"
    center
  >
    <!-- 指引内容 -->
    <div class="guide-content">
      <!-- 主要信息 -->
      <div class="main-info">
        <div class="info-icon">
          <el-icon class="icon-large" :class="{ 'playing': isPlayingVoice }">
            <EditPen />
          </el-icon>
        </div>
        <div class="info-text">
          <h3 class="title">专业知识问答</h3>
          <p class="description">请认真思考，仔细作答</p>
        </div>
      </div>
      
      <!-- 简单提醒 -->
      <div class="tips-box">
        <div class="tips-icon">
          <el-icon>
            <InfoFilled />
          </el-icon>
        </div>
        <div class="tips-text">
          接下来将进行专业知识相关的问答环节
        </div>
      </div>
      
      <!-- 语音播放状态 -->
      <div class="voice-status" v-if="isPlayingVoice">
        <el-icon class="voice-icon spinning">
          <Mic />
        </el-icon>
        <span class="voice-text">正在播放语音指引</span>
      </div>
      
      <!-- 自动开始提示 -->
      <div class="auto-start-hint" v-if="!isPlayingVoice && voiceCompleted">
        <el-icon class="countdown-icon">
          <Timer />
        </el-icon>
        <span>即将自动开始答题...</span>
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
  EditPen,
  InfoFilled,
  Mic, 
  Timer
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
const guideText = '接下来是基础问答环节。您将回答一些与专业知识相关的题目。请仔细阅读题目，认真思考后作答。现在开始答题。'

// 停止语音播放
const stopVoice = () => {
  console.log('🔇 基础问答指引：强制停止语音播放')
  if (audioElementRef.value && !audioElementRef.value.paused) {
    // 只有在播放中才暂停，避免重复操作
    try {
      audioElementRef.value.pause()
      audioElementRef.value.currentTime = 0
      
      // 延迟清空音频源，避免播放中断导致的error事件
      setTimeout(() => {
        if (audioElementRef.value) {
          audioElementRef.value.src = ''
          audioElementRef.value.load() // 重新加载音频元素状态
        }
      }, 100)
    } catch (error) {
      console.warn('停止音频时出现警告:', error)
    }
  }
  isPlayingVoice.value = false
}

// 播放语音指引
const playVoice = async () => {
  if (isPlayingVoice.value) return
  
  try {
    console.log('🎵 开始播放基础问答指引语音')
    isPlayingVoice.value = true
    voiceCompleted.value = false
    
    // 获取TTS音频数据
    const audioData = await fetchTtsAudio(guideText)
    if (!audioData) {
      throw new Error('获取音频数据失败')
    }
    
    // 创建音频Blob并播放
    const blob = new Blob([audioData], { type: 'audio/mpeg' })
    const audioUrl = URL.createObjectURL(blob)
    
    if (audioElementRef.value) {
      audioElementRef.value.src = audioUrl
      
      // 监听音频结束事件，清理URL
      audioElementRef.value.addEventListener('ended', () => {
        URL.revokeObjectURL(audioUrl)
      }, { once: true })
      
      await audioElementRef.value.play()
      console.log('🎵 基础问答指引语音开始播放')
    }
  } catch (error) {
    console.error('🎵 播放基础问答指引语音失败:', error)
    console.error('语音播放失败，将自动开始答题')
    isPlayingVoice.value = false
    voiceCompleted.value = true
    // 即使语音播放失败，也要在短暂延迟后开始答题
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
  console.log('🎵 基础问答指引语音播放结束')
  isPlayingVoice.value = false
  voiceCompleted.value = true
  
  // 语音播放完成后，自动开始答题
  setTimeout(() => {
    handleAutoStart()
  }, 1500) // 1.5秒后自动开始
}

// 音频准备就绪
const handleAudioCanPlay = () => {
  isAudioReady.value = true
  console.log('🎵 基础问答指引音频准备就绪')
}

// 音频播放错误
const handleAudioError = (error) => {
  // 只在实际播放状态下才报告错误，避免停止时的误报
  if (isPlayingVoice.value) {
    console.error('🎵 基础问答指引音频播放错误:', error)
    console.error('语音播放出现错误，将自动开始答题')
    // 出错时也要自动开始答题
    setTimeout(() => {
      handleAutoStart()
    }, 1500)
  } else {
    console.log('🔇 音频停止时的正常错误，忽略')
  }
  isPlayingVoice.value = false
  voiceCompleted.value = true
}

// 自动开始答题
const handleAutoStart = () => {
  console.log('自动开始基础问答环节')
  emit('start')
  emit('close')
}

// 组件卸载时清理
onUnmounted(() => {
  stopVoice()
})
</script>

<style scoped>
.basic-qa-guide-dialog {
  --el-dialog-border-radius: 16px;
}

.basic-qa-guide-dialog :deep(.el-dialog) {
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(10px);
}

.basic-qa-guide-dialog :deep(.el-dialog__header) {
  padding: 28px 28px 18px 28px;
  text-align: center;
  border-bottom: none;
  background: linear-gradient(135deg, #1890ff 0%, #722ed1 100%);
  border-radius: 16px 16px 0 0;
}

.basic-qa-guide-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  color: white;
}

.basic-qa-guide-dialog :deep(.el-dialog__body) {
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
  color: #1890ff;
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

.tips-box {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f9ff 100%);
  border: 2px solid #91d5ff;
  border-radius: 12px;
  margin-bottom: 18px;
  text-align: left;
}

.tips-icon {
  flex-shrink: 0;
  color: #1890ff;
  font-size: 18px;
  margin-top: 1px;
}

.tips-text {
  flex: 1;
  font-size: 14px;
  color: #0050b3;
  line-height: 1.4;
}

.voice-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #f6ffed 0%, #f0f9ff 100%);
  border-radius: 20px;
  border: 2px solid #b7eb8f;
  margin-bottom: 12px;
}

.voice-icon {
  font-size: 18px;
  color: #52c41a;
}

.voice-text {
  font-size: 14px;
  color: #52c41a;
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
  background: linear-gradient(135deg, #fff7e6 0%, #fef5e7 100%);
  border-radius: 16px;
  border: 2px solid #ffd666;
  color: #d46b08;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}

.countdown-icon {
  font-size: 16px;
  color: #fa8c16;
  animation: tick 1s ease-in-out infinite;
}

@keyframes tick {
  0% { transform: scale(1); }
  50% { transform: scale(1.2); }
  100% { transform: scale(1); }
}

/* 响应式设计 */
@media (max-width: 480px) {
  .basic-qa-guide-dialog :deep(.el-dialog) {
    margin: 0 16px;
    width: calc(100% - 32px);
  }
  
  .basic-qa-guide-dialog :deep(.el-dialog__header) {
    padding: 20px 20px 14px 20px;
  }
  
  .basic-qa-guide-dialog :deep(.el-dialog__body) {
    padding: 16px 20px 20px 20px;
  }
  
  .basic-qa-guide-dialog :deep(.el-dialog__title) {
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
}
</style>