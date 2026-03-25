import { ref, onUnmounted } from 'vue'

/**
 * 题目计时器组合式API
 * @param {Object} options - 配置项
 * @param {Function} options.onTimeUp - 倒计时结束时的回调函数
 * @returns {Object} 计时器相关的状态和方法
 */
export function useQuestionTimer({ onTimeUp }) {
  // 状态定义
  const countdown = ref(0)
  const timer = ref(null)
  const isRunning = ref(false)
  
  /**
   * 启动倒计时
   * @param {number} seconds - 倒计时秒数
   * @param {Function} [callback] - 每秒回调函数
   */
  const startCountdown = (seconds, callback) => {
    // 清除可能存在的旧计时器
    stopCountdown()
    
    // 设置初始倒计时值
    countdown.value = seconds
    isRunning.value = true
    
    // 启动新的计时器
    timer.value = setInterval(() => {
      if (countdown.value > 0) {
        countdown.value--
        if (callback) callback(countdown.value)
      } else {
        stopCountdown()
        if (onTimeUp) onTimeUp()
      }
    }, 1000)
  }
  
  /**
   * 停止倒计时
   */
  const stopCountdown = () => {
    if (timer.value) {
      clearInterval(timer.value)
      timer.value = null
      isRunning.value = false
    }
  }
  
  /**
   * 重置倒计时
   * @param {number} seconds - 倒计时秒数
   */
  const resetCountdown = (seconds) => {
    stopCountdown()
    countdown.value = seconds
  }
  
  /**
   * 格式化时间为 MM:SS 格式
   * @param {number} seconds - 秒数
   * @returns {string} 格式化后的时间字符串
   */
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
  
  // 组件卸载时清除计时器
  onUnmounted(() => {
    stopCountdown()
  })
  
  return {
    // 状态
    countdown,
    isRunning,
    
    // 方法
    startCountdown,
    stopCountdown,
    resetCountdown,
    formatTime
  }
} 