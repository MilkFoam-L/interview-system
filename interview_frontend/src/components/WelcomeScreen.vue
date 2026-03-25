<template>
  <div v-if="visible" class="welcome-screen-overlay">
    <div class="welcome-screen">
      <!-- 左侧虚拟人模型 -->
      <div class="welcome-model-section">
        <div class="model-animation">
          <div class="model-circle"></div>
          <div class="model-avatar">
            <Avatar3D 
              :is-playing="isPlayingVoice"
              :audio-element="audioElementRef"
              :enable-speech-animation="true"
              :width="280"
              :height="280"
              model-path="/models/AI_image02.glb"
              class="avatar-3d"
            />
          </div>
          
          <!-- 隐藏的音频元素用于语音播放和分析 -->
          <audio
            ref="audioElementRef"
            :src="currentAudioSrc"
            @ended="handleAudioEnded"
            @canplay="handleAudioCanPlay"
            @error="handleAudioError"
            preload="auto"
            style="display: none;"
          />
        </div>
      </div>
      
      <!-- 右侧内容区域 -->
      <div class="welcome-content-section">
        <div class="welcome-content">
          <h1 class="welcome-title">{{ welcomeData.title }}</h1>
          <p class="welcome-subtitle">{{ welcomeData.subtitle }}</p>
          <div class="welcome-message">
            <p v-for="(message, index) in welcomeData.messages" :key="index">
              {{ message }}
            </p>
          </div>
        </div>
        
        <div class="welcome-footer">
          <div class="voice-status">
            <el-icon class="voice-icon" :class="{ 'playing': isPlayingVoice }">
              <VideoPlay v-if="isPlayingVoice" />
              <VideoPause v-else />
            </el-icon>
            <span>{{ isPlayingVoice ? '正在播放语音指引...' : '语音播放完成' }}</span>
          </div>
          
          <el-button 
            v-if="!isPlayingVoice" 
            type="primary" 
            size="large" 
            @click="handleContinue"
            class="continue-btn"
          >
            {{ welcomeData.buttonText }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { User, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import Avatar3D from './Avatar3D.vue'
import { fetchTtsAudio } from '@/api/tts'
import { ElMessage } from 'element-plus'

// Props定义
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  type: {
    type: String,
    required: true,
    validator: (value) => ['newUser', 'returningUser'].includes(value)
  },
  username: {
    type: String,
    default: '用户'
  },
  isPlayingVoice: {
    type: Boolean,
    default: false
  }
})

// Emits定义
const emit = defineEmits(['continue', 'close'])

// Refs
const audioElementRef = ref(null)

// 音频相关状态
const currentAudioSrc = ref('')
const isAudioReady = ref(false)

// TTS文本配置（使用讯飞语音合成API）
const ttsConfigs = {
  newUser: {
    text: '欢迎加入智能面试系统！为了让您获得更好的面试体验，建议您先完善个人资料和简历信息。这将帮助我们为您推荐更合适的岗位，并生成更准确的面试问题。让我们一起开始您的求职之旅吧！'
  },
  returningUser: {
    text: '欢迎回来！很高兴再次见到您。您可以继续浏览最新的岗位信息，选择感兴趣的职位进行投递，或者查看之前的面试记录和分析报告。我们的AI面试系统已经为您准备就绪，祝您今天求职愉快！'
  }
}

// 欢迎数据配置
const welcomeConfigs = {
  newUser: {
    title: '欢迎加入智能面试系统！',
    subtitle: (username) => `${username}，开启您的职业新篇章`,
    messages: [
      '为了让您获得更好的面试体验，建议您先完善个人资料和简历信息。',
      '这将帮助我们为您推荐更合适的岗位，并生成更准确的面试问题。', 
      '让我们一起开始您的求职之旅吧！'
    ],
    buttonText: '开始使用'
  },
  returningUser: {
    title: '欢迎回来！',
    subtitle: (username) => `${username}，很高兴再次见到您`,
    messages: [
      '您可以继续浏览最新的岗位信息，选择感兴趣的职位进行投递。',
      '或者查看之前的面试记录和分析报告。',
      '我们的AI面试系统已经为您准备就绪，祝您今天求职愉快！'
    ],
    buttonText: '继续使用'
  }
}

// 计算欢迎数据
const welcomeData = computed(() => {
  const config = welcomeConfigs[props.type]
  if (!config) return {}
  
  return {
    title: config.title,
    subtitle: config.subtitle(props.username),
    messages: config.messages,
    buttonText: config.buttonText
  }
})

// 音频处理函数
const handleAudioCanPlay = () => {
  isAudioReady.value = true
  console.log('🎵 音频文件加载完成，准备播放')
}

const handleAudioEnded = () => {
  console.log('🎵 音频播放结束')
  // 音频播放结束后的逻辑可以在这里添加
}

const handleAudioError = (error) => {
  console.warn('🎵 音频播放错误:', error)
  isAudioReady.value = false
  currentAudioSrc.value = '' // 清除错误的音频源
}

// 播放欢迎语音（使用TTS API）
const playWelcomeAudio = async () => {
  if (!audioElementRef.value) return
  
  const ttsConfig = ttsConfigs[props.type]
  if (!ttsConfig || !ttsConfig.text) {
    console.warn('🎵 未找到对应的TTS文本配置')
    return
  }
  
  try {
    console.log('🎵 开始通过TTS API生成语音...')
    
    // 调用讯飞TTS API生成音频数据
    const audioData = await fetchTtsAudio(ttsConfig.text)
    
    // 创建音频Blob对象
    const audioBlob = new Blob([audioData], { type: 'audio/mp3' })
    const audioUrl = URL.createObjectURL(audioBlob)
    
    // 设置音频源并播放
    audioElementRef.value.src = audioUrl
    currentAudioSrc.value = audioUrl
    
    // 监听音频结束事件，清理资源
    const handleEnded = () => {
      URL.revokeObjectURL(audioUrl)
      audioElementRef.value.removeEventListener('ended', handleEnded)
      console.log('🎵 语音播放完成，资源已清理')
    }
    audioElementRef.value.addEventListener('ended', handleEnded)
    
    await audioElementRef.value.play()
    console.log('🎵 TTS语音开始播放')
    
  } catch (error) {
    console.error('🎵 TTS语音生成或播放失败:', error)
    console.error('语音播放失败，请检查网络连接')
  }
}

// 停止音频播放
const stopAudio = () => {
  if (audioElementRef.value) {
    audioElementRef.value.pause()
    audioElementRef.value.currentTime = 0
  }
}

// 处理继续按钮点击
const handleContinue = () => {
  stopAudio() // 停止音频播放
  emit('continue', props.type)
  emit('close')
}

// 初始化TTS文本
const initTtsText = () => {
  const ttsConfig = ttsConfigs[props.type]
  if (ttsConfig && ttsConfig.text) {
    console.log(`🎵 准备${props.type === 'newUser' ? '新用户' : '老用户'}TTS语音文本:`, ttsConfig.text.substring(0, 50) + '...')
  } else {
    console.warn('🎵 未找到对应的TTS文本配置')
  }
}

// 监听visible变化，优化显示性能
watch(() => props.visible, (newVal) => {
  if (newVal) {
    console.log(`✅ 显示${props.type === 'newUser' ? '新用户' : '老用户'}欢迎界面`)
    console.log('🎭 3D虚拟人形象开始加载，所有用户都能看到')
    console.log('🎬 欢迎界面开始渲染，动画已启动')
    
    // 初始化TTS文本
    initTtsText()
    
    // 延迟播放音频，确保3D模型加载完成
    setTimeout(() => {
      if (props.isPlayingVoice) {
        playWelcomeAudio()
      }
    }, 1500) // 给3D模型加载预留时间
  } else {
    // 界面隐藏时停止音频
    stopAudio()
  }
}, { immediate: true })

// 监听播放状态变化
watch(() => props.isPlayingVoice, (isPlaying) => {
  if (isPlaying && props.visible) {
    playWelcomeAudio()
  } else {
    stopAudio()
  }
})

// 监听用户类型变化
watch(() => props.type, () => {
  stopAudio() // 停止当前音频
  initTtsText() // 重新初始化TTS文本
}, { immediate: true })
</script>

<style scoped>
/* 全屏欢迎界面样式 */
.welcome-screen-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #A8DADC 0%, #457B9D 50%, #264653 100%);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.4s ease-out;
}

.welcome-screen {
  max-width: 1200px;
  width: 95%;
  color: white;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 60px;
  padding: 40px;
}

/* 左侧模型区域 */
.welcome-model-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
}

.model-animation {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.model-circle {
  width: 320px;
  height: 320px;
  border: 4px solid rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  animation: pulse 3s infinite;
}

.model-avatar {
  position: absolute;
  width: 280px;
  height: 280px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.avatar-3d {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  transition: transform 0.3s ease;
}

.avatar-3d:hover {
  transform: scale(1.02);
}

/* 优化模型显示 */
.model-avatar .avatar-3d canvas {
  border-radius: 50%;
  image-rendering: auto;
}

.welcome-main-icon {
  font-size: 60px;
  color: white;
}

/* 右侧内容区域 */
.welcome-content-section {
  flex: 1.2;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 40px;
  padding-left: 40px;
}

.welcome-content {
  text-align: left;
  max-width: none;
}

.welcome-title {
  font-size: 48px;
  font-weight: bold;
  margin-bottom: 16px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
  animation: slideInDown 0.6s ease-out;
}

.welcome-subtitle {
  font-size: 24px;
  margin-bottom: 32px;
  opacity: 0.9;
  animation: slideInDown 0.6s ease-out 0.1s both;
}

.welcome-message {
  font-size: 18px;
  line-height: 1.6;
  opacity: 0.85;
  animation: slideInUp 0.6s ease-out 0.2s both;
}

.welcome-message p {
  margin-bottom: 16px;
}

.welcome-footer {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 24px;
  animation: slideInUp 0.6s ease-out 0.3s both;
}

.voice-status {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  opacity: 0.9;
}

.voice-icon {
  font-size: 24px;
  transition: transform 0.3s ease;
}

.voice-icon.playing {
  animation: voicePulse 1.5s infinite;
}

.continue-btn {
  font-size: 18px;
  padding: 16px 48px;
  border-radius: 30px;
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border: none !important;
  color: #F1FAEE !important;
  font-weight: 600;
  letter-spacing: 0.5px;
  box-shadow: 0 4px 16px rgba(38, 70, 83, 0.35);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.continue-btn:hover {
  background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important;
  transform: translateY(-3px) scale(1.03) !important;
  box-shadow: 0 10px 30px rgba(38, 70, 83, 0.45) !important;
}

/* 动画定义 */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.7;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes slideInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes voicePulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
  }
}

/* 响应式设计 */
@media (max-width: 968px) {
  .welcome-screen {
    flex-direction: column;
    gap: 40px;
    padding: 30px;
    text-align: center;
  }
  
  .welcome-model-section {
    min-height: 300px;
  }
  
  .model-circle {
    width: 250px;
    height: 250px;
  }
  
  .model-avatar {
    width: 220px;
    height: 220px;
  }
  
  .welcome-content-section {
    padding-left: 0;
  }
  
  .welcome-content {
    text-align: center;
  }
  
  .welcome-footer {
    align-items: center;
  }
  
  .welcome-title {
    font-size: 42px;
  }
  
  .welcome-subtitle {
    font-size: 22px;
  }
}

@media (max-width: 768px) {
  .welcome-screen {
    gap: 30px;
    padding: 20px;
  }
  
  .welcome-model-section {
    min-height: 250px;
  }
  
  .model-circle {
    width: 200px;
    height: 200px;
  }
  
  .model-avatar {
    width: 180px;
    height: 180px;
  }
  
  .welcome-title {
    font-size: 32px;
  }
  
  .welcome-subtitle {
    font-size: 18px;
  }
  
  .welcome-message {
    font-size: 16px;
  }
}
</style>