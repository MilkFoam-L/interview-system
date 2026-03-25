<template>
  <el-dialog 
    :model-value="modelValue"
    title=""
    :width="420"
    :align-center="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    class="completion-guide-dialog">
    
    <div class="completion-guide-content">
      <!-- 完成图标 -->
      <div class="completion-icon">
        <el-icon size="60" color="#67C23A"><SuccessFilled /></el-icon>
      </div>
      
      <!-- 感谢标题 -->
      <div class="completion-title">
        面试已完成！
      </div>
      
      <!-- 感谢内容 -->
      <div class="completion-message">
        <p>感谢您的认真面试！</p>
        <p>请耐心等待面试结果，</p>
        <p>我们会通过邮件通知您。</p>
      </div>
      
      <!-- 语音播放状态 -->
      <div class="voice-status" v-if="isPlaying">
        <el-icon class="voice-icon"><Mic /></el-icon>
        <span class="voice-text">正在播放感谢语音...</span>
      </div>
      
      <!-- 自动跳转提示 -->
      <div class="auto-redirect" v-if="!isPlaying && countdown > 0">
        <span class="redirect-text">{{ countdown }}秒后自动跳转到面试报告</span>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { SuccessFilled, Mic } from '@element-plus/icons-vue'
import { fetchTtsAudio } from '../api/tts.js'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'completion-confirmed'])

// 语音和状态管理
const isPlaying = ref(false)
const countdown = ref(0)
const audioElement = ref(null)
const countdownTimer = ref(null)

// 感谢语音文本
const completionText = `面试已经全部完成！感谢您的认真面试和精彩表现！请您耐心等待面试结果，我们会通过手机短信及时通知您。祝您面试成功！`

// 监听弹窗显示状态
watch(() => props.modelValue, async (newVal) => {
  if (newVal) {
    console.log('🎉 显示面试完成感谢弹窗')
    await playCompletionVoice()
  } else {
    stopVoice()
    clearCountdown()
  }
})

// 播放感谢语音
const playCompletionVoice = async () => {
  try {
    console.log('🎵 开始播放面试完成感谢语音')
    isPlaying.value = true
    
    // 获取TTS音频
    const audioData = await fetchTtsAudio(completionText)
    
    // 创建音频元素并播放
    audioElement.value = new Audio()
    audioElement.value.src = URL.createObjectURL(new Blob([audioData], { type: 'audio/mpeg' }))
    
    audioElement.value.onended = () => {
      console.log('🎵 面试完成感谢语音播放结束')
      isPlaying.value = false
      startCountdown()
    }
    
    audioElement.value.onerror = (error) => {
      console.error('❌ 感谢语音播放失败:', error)
      isPlaying.value = false
      startCountdown()
    }
    
    await audioElement.value.play()
    console.log('🎵 面试完成感谢语音开始播放')
    
  } catch (error) {
    console.error('❌ 获取感谢语音失败:', error)
    isPlaying.value = false
    startCountdown()
  }
}

// 停止语音播放
const stopVoice = () => {
  console.log('🔇 面试完成感谢：强制停止语音播放')
  if (audioElement.value && !audioElement.value.paused) {
    // 只有在播放中才暂停，避免重复操作
    try {
      audioElement.value.pause()
      
      // 延迟清空，避免error事件
      setTimeout(() => {
        if (audioElement.value) {
          audioElement.value = null
        }
      }, 100)
    } catch (error) {
      console.warn('停止音频时出现警告:', error)
    }
  }
  isPlaying.value = false
}

// 开始倒计时
const startCountdown = () => {
  countdown.value = 3
  countdownTimer.value = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearCountdown()
      handleCompletionConfirmed()
    }
  }, 1000)
}

// 清除倒计时
const clearCountdown = () => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
    countdownTimer.value = null
  }
  countdown.value = 0
}

// 处理完成确认
const handleCompletionConfirmed = () => {
  console.log('✅ 面试完成感谢确认，准备跳转到报告页面')
  emit('update:modelValue', false)
  emit('completion-confirmed')
}

// 清理资源
onUnmounted(() => {
  stopVoice()
  clearCountdown()
})
</script>

<style scoped>
.completion-guide-dialog :deep(.el-dialog) {
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
}

.completion-guide-dialog :deep(.el-dialog__header) {
  padding: 0;
  border-bottom: none;
}

.completion-guide-dialog :deep(.el-dialog__body) {
  padding: 30px;
}

.completion-guide-content {
  text-align: center;
}

.completion-icon {
  margin-bottom: 20px;
  animation: scaleIn 0.6s ease-out;
}

.completion-title {
  font-size: 24px;
  font-weight: bold;
  color: #67C23A;
  margin-bottom: 20px;
  animation: fadeInUp 0.8s ease-out 0.2s both;
}

.completion-message {
  font-size: 16px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 25px;
  animation: fadeInUp 0.8s ease-out 0.4s both;
}

.completion-message p {
  margin: 8px 0;
}

.voice-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #409EFF;
  font-size: 14px;
  margin-top: 20px;
}

.voice-icon {
  animation: pulse 1.5s infinite;
}

.voice-text {
  font-weight: 500;
}

.auto-redirect {
  margin-top: 20px;
}

.redirect-text {
  color: #909399;
  font-size: 14px;
  background-color: #f5f7fa;
  padding: 8px 16px;
  border-radius: 20px;
  display: inline-block;
}

/* 动画效果 */
@keyframes scaleIn {
  from {
    transform: scale(0);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes fadeInUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
}
</style>