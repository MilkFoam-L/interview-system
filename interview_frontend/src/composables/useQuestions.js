import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { assignQuestions, getSessionQuestions } from '../api/sessionQuestion'

/**
 * 题目管理组合式API
 * @param {number} interviewSessionId - 面试会话ID
 * @returns {Object} 题目相关的状态和方法
 */
export function useQuestions(interviewSessionId) {
  // 状态定义
  const questions = ref([])
  const currentIndex = ref(0)
  const loadingQuestions = ref(false)
  const errorMessage = ref('')
  
  // 计算属性
  const currentQuestion = computed(() => {
    if (!questions.value.length || currentIndex.value >= questions.value.length) {
      return null
    }
    return questions.value[currentIndex.value]
  })
  
  const totalQuestions = computed(() => questions.value.length)
  const hasNextQuestion = computed(() => currentIndex.value < questions.value.length - 1)
  const hasPrevQuestion = computed(() => currentIndex.value > 0)
  
  /**
   * 加载指定环节的题目
   * @param {string} stage - 环节名称（如'BASIC_QA'或'CODE_TEST'）
   * @param {number} [maxQuestions] - 最大题目数量，如果不指定则根据环节自动设置
   * @returns {Promise<Array>} 加载的题目数组
   */
  const loadQuestions = async (stage, maxQuestions) => {
    // 如果没有指定题目数量，根据环节自动设置
    if (!maxQuestions) {
      maxQuestions = stage === 'BASIC_QA' ? 10 : stage === 'CODE_TEST' ? 3 : 5;
    }
    loadingQuestions.value = true
    errorMessage.value = ''
    
    try {
      // 分配题目，后端幂等
      await assignQuestions(interviewSessionId.value, stage, maxQuestions)
      
      // 获取题目
      const res = await getSessionQuestions(interviewSessionId.value, stage)
      
      // 根据环节过滤题目
      let filtered = []
      if (stage === 'BASIC_QA') {
        filtered = (res.data || res).filter(q => q.inputType === 'radio')
      } else if (stage === 'CODE_TEST') {
        filtered = (res.data || res).filter(q => q.inputType === 'code')
      } else {
        filtered = res.data || res
      }
      
      // 确保题目数量不超过最大值
      if (filtered.length > maxQuestions) {
        filtered = filtered.slice(0, maxQuestions)
      }
      
      questions.value = filtered
      currentIndex.value = 0
      
      return filtered
    } catch (error) {
      errorMessage.value = `加载题目失败: ${error.message || '未知错误'}`
      console.error(`加载题目失败，请刷新页面重试`)
      questions.value = []
      return []
    } finally {
      loadingQuestions.value = false
    }
  }
  
  /**
   * 切换到指定索引的题目
   * @param {number} index - 目标题目索引
   * @returns {boolean} 是否切换成功
   */
  const switchToQuestion = (index) => {
    // 边界检查
    if (index < 0 || index >= questions.value.length) {
      return false
    }
    
    currentIndex.value = index
    return true
  }
  
  /**
   * 切换到下一题
   * @returns {boolean} 是否切换成功
   */
  const nextQuestion = () => {
    if (hasNextQuestion.value) {
      return switchToQuestion(currentIndex.value + 1)
    }
    return false
  }
  
  /**
   * 切换到上一题
   * @returns {boolean} 是否切换成功
   */
  const prevQuestion = () => {
    if (hasPrevQuestion.value) {
      return switchToQuestion(currentIndex.value - 1)
    }
    return false
  }
  
  return {
    // 状态
    questions,
    currentIndex,
    loadingQuestions,
    errorMessage,
    
    // 计算属性
    currentQuestion,
    totalQuestions,
    hasNextQuestion,
    hasPrevQuestion,
    
    // 方法
    loadQuestions,
    switchToQuestion,
    nextQuestion,
    prevQuestion
  }
} 