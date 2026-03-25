import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { submitAnswer, getAnswerRecord } from '../api/answerRecord'

/**
 * 格式化日期为后端期望的格式 (yyyy-MM-ddTHH:mm:ss)
 * @param {Date} date - 日期对象
 * @returns {string} 格式化后的日期字符串
 */
function formatDateForBackend(date) {
  const pad = (num) => String(num).padStart(2, '0')

  const year = date.getFullYear()
  const month = pad(date.getMonth() + 1)
  const day = pad(date.getDate())
  const hours = pad(date.getHours())
  const minutes = pad(date.getMinutes())
  const seconds = pad(date.getSeconds())

  // 使用ISO 8601格式，这是Java LocalDateTime默认支持的格式
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

/**
 * 答题管理组合式API
 * @param {Object} options - 配置项
 * @param {import('vue').Ref<Array>} options.questions - 题目数组
 * @param {import('vue').Ref<number>} options.currentIndex - 当前题目索引
 * @param {import('vue').Ref<number>} options.interviewSessionId - 面试会话ID
 * @param {import('vue').Ref<number>} options.userId - 用户ID
 * @returns {Object} 答题相关的状态和方法
 */
export function useAnswers({ questions, currentIndex, interviewSessionId, userId }) {
  // 状态定义
  const answers = ref([])
  const startTimes = ref([])
  const submitTimes = ref([])
  const answerRecords = ref({})
  const answerLoading = ref(false)
  
  /**
   * 初始化答案数组
   * @param {Array} newQuestions - 新的题目数组
   */
  const initializeAnswers = (newQuestions) => {
    // 初始化答案数组
    answers.value = newQuestions.map(() => '')
    
    // 初始化提交时间数组
    submitTimes.value = newQuestions.map(() => null)
    
    // 初始化开始时间数组（仅初始化为null，不立即设置时间）
    // 实际开始时间将在首次进入题目时设置
    startTimes.value = newQuestions.map(() => null)
  }
  
  /**
   * 设置题目开始时间
   * @param {number} index - 题目索引
   * @returns {string} 格式化后的开始时间
   */
  const setQuestionStartTime = (index) => {
    if (index < 0 || index >= questions.value.length) {
      return null
    }
    
    // 只有首次进入题目时才设置开始时间
    if (!startTimes.value[index]) {
      const now = new Date()
      startTimes.value[index] = formatDateForBackend(now)
      return startTimes.value[index]
    }
    
    return startTimes.value[index]
  }
  
  /**
   * 提交答案
   * @param {number} index - 题目索引
   * @param {string} answer - 用户答案
   * @returns {Promise<Object>} 提交结果
   */
  const submitUserAnswer = async (index, answer) => {
    if (index < 0 || index >= questions.value.length) {
      return null
    }
    
    const question = questions.value[index]
    if (!question) {
      return null
    }
    
    answerLoading.value = true
    
    try {
      // 获取当前题目ID
      const qid = question.questionId
      
      // 计算答题时间（秒）
      const now = new Date()
      const startTime = new Date(startTimes.value[index])
      
      // 计算实际用时，根据题目类型使用不同的最大值限制
      const isCodeQuestion = question.inputType === 'code'
      const maxTime = isCodeQuestion ? 300 : 60 // 代码题5分钟，其他题1分钟
      
      // 检查是否已经有记录的答题时间
      const existingRecord = answerRecords.value[qid]
      let timeUsed
      
      // 如果已经有记录的答题时间，继续使用该时间，否则计算新的答题时间
      if (existingRecord && existingRecord.timeUsed) {
        timeUsed = existingRecord.timeUsed
      } else {
        // 首次回答，计算用时
        timeUsed = Math.floor((now - startTime) / 1000)
        
        // 限制最大答题时间
        if (timeUsed > maxTime) {
          timeUsed = maxTime
        }
      }
      
      // 格式化时间为后端期望的格式
      const formattedStartTime = formatDateForBackend(startTime)
      const formattedSubmitTime = formatDateForBackend(now)
      
      // 提交答案
      await submitAnswer({
        sessionId: interviewSessionId.value,
        interviewId: interviewSessionId.value,
        questionId: qid,
        userId: userId.value,
        userAnswer: answer,
        type: question.stage,
        startTime: formattedStartTime,
        submitTime: formattedSubmitTime,
        timeUsed: timeUsed
      })
      
      // 更新状态
      answers.value[index] = answer
      submitTimes.value[index] = formattedSubmitTime
      
      // 获取更新后的答案记录
      const res = await getAnswerRecord(userId.value, qid)
      
      // 更新答案记录
      if (res && res.data) {
        answerRecords.value = { ...answerRecords.value, [qid]: res.data }
      } else if (res) {
        answerRecords.value = { ...answerRecords.value, [qid]: res }
      }
      
      return res
    } catch (error) {
      console.error(`提交答案失败: ${error.message || '未知错误'}`)
      return null
    } finally {
      answerLoading.value = false
    }
  }
  
  /**
   * 提交超时答案
   * @param {number} index - 题目索引
   * @param {string} answer - 用户答案
   * @param {number} maxTimeUsed - 最大用时（秒）
   * @returns {Promise<Object>} 提交结果
   */
  const submitTimeoutAnswer = async (index, answer, maxTimeUsed) => {
    // 完全禁用此功能，不执行任何提交操作
    return null
  }
  
  /**
   * 加载题目的答案记录
   * @param {number} index - 题目索引
   * @returns {Promise<Object>} 答案记录
   */
  const loadAnswerRecord = async (index) => {
    if (index < 0 || index >= questions.value.length) {
      return null
    }
    
    const question = questions.value[index]
    if (!question) {
      return null
    }
    
    try {
      const qid = question.questionId
      const res = await getAnswerRecord(userId.value, qid)
      
      if (res && res.data) {
        answerRecords.value = { ...answerRecords.value, [qid]: res.data }
        return res.data
      } else if (res) {
        answerRecords.value = { ...answerRecords.value, [qid]: res }
        return res
      }
      
      return null
    } catch (error) {
      return null
    }
  }
  
  // 监听题目变化，初始化答案数组
  watch(questions, (newQuestions) => {
    if (newQuestions && newQuestions.length > 0) {
      initializeAnswers(newQuestions)
    }
  })
  
  return {
    // 状态
    answers,
    startTimes,
    submitTimes,
    answerRecords,
    answerLoading,
    
    // 方法
    initializeAnswers,
    setQuestionStartTime,
    submitUserAnswer,
    submitTimeoutAnswer,
    loadAnswerRecord,
    formatDateForBackend
  }
} 