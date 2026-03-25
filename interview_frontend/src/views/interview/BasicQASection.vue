<template>
  <div class="basic-qa-section">
    <!-- 处理加载状态 -->
    <div v-if="loadingQuestions && questions.length === 0" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>
    
    <!-- 处理常规题目显示 -->
    <div v-else-if="questions.length > 0 && !isPlaceholderQuestion && currentIndex < questions.length && questions[currentIndex] && questions[currentIndex].inputType === 'radio'" class="question-content">
      <div class="progress-bar">
        <div class="progress-text">题目 {{ currentIndex + 1 }}/{{ questions.length }}</div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: `${((currentIndex + 1) / questions.length) * 100}%` }"></div>
        </div>
      </div>
      
      <div class="question-header">
        <div class="question-number">{{ currentIndex + 1 }}</div>
        <div class="question-title">
          {{ questions[currentIndex].content }}
        </div>
        <div class="question-timer">
          <span style="font-size: 18px; font-weight: bold;">{{ formatTime(countdown) }}</span>
        </div>
      </div>
      
      <div v-if="questions[currentIndex].description" class="question-desc">
        {{ questions[currentIndex].description }}
      </div>
      
      <div class="option-list">
        <div
          v-for="(opt, idx) in questions[currentIndex].options"
          :key="opt"
          :class="['option-item', { 
            active: answers[currentIndex] === opt,
            disabled: answerLoading
          }]"
          @click="!answerLoading && handleSelectOption(opt)"
        >
          <div class="option-letter">{{ String.fromCharCode(65 + idx) }}</div>
          <div class="option-content">{{ opt }}</div>
          <div class="option-radio">
            <div v-if="answers[currentIndex] === opt" class="radio-dot"></div>
          </div>
        </div>
      </div>
      
      <div class="action-buttons">
        <el-button
          type="primary"
          plain
          :disabled="!hasPrevQuestion"
          @click="prevQuestion">
          上一题
        </el-button>

        <el-button
          type="warning"
          plain
          @click="skipAllQuestions"
          :loading="skipping"
        >
          跳过全部（测试）
        </el-button>

        <el-button
          v-if="hasNextQuestion"
          type="primary"
          @click="nextQuestion">
          下一题
        </el-button>
        <el-button
          v-else
          type="success"
          @click="completeSection">
          完成
        </el-button>
      </div>
    </div>
    
    <!-- 处理占位符题目 -->
    <div v-else-if="isPlaceholderQuestion" class="placeholder-message">
      <el-result
        icon="info"
        title="暂无可用题目"
        sub-title="系统根据您的历史作答记录和当前岗位要求，暂时没有找到适合的题目"
      >
        <template #extra>
          <el-button type="primary" @click="completeSection">继续下一环节</el-button>
        </template>
      </el-result>
    </div>
    
    <!-- 处理无题目情况 -->
    <el-empty v-else description="没有可用的问题" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { formatTime, formatDateForBackend } from '../../utils/dateUtils.js'
import { assignQuestions, getSessionQuestions } from '../../api/sessionQuestion.js'
import { submitAnswer, getAnswerRecord } from '../../api/answerRecord.js'
import { completeStage } from '../../api/stage.js'
import cacheService from '../../services/cacheService.js'

const props = defineProps({
  interviewSessionId: {
    type: Number,
    required: true
  },
  userId: {
    type: Number,
    required: true
  },
  stage: {
    type: String,
    default: 'BASIC_QA'
  },
  autoStart: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['section-complete', 'question-change', 'answer-submit'])

// 题目相关状态
const questions = ref([])
const currentIndex = ref(0)
const answers = ref([])
const startTimes = ref([])
const submitTimes = ref([])
const answerRecords = ref({})
const loadingQuestions = ref(false)
const answerLoading = ref(false)
const isPlaceholderQuestion = ref(false) // 标记是否是占位符题目
const questionRealStartTimes = ref({}) // 记录每道题实际开始显示的时间
const answeredQuestions = ref({}) // 记录已经回答过的题目

// 计时器相关
const countdown = ref(0)
const timer = ref(null)
const perQuestionCountdown = ref(60) // 默认每道题60秒
const isCountdownPaused = ref(false) // 倒计时暂停状态

// 计算属性
const hasPrevQuestion = computed(() => currentIndex.value > 0)
const hasNextQuestion = computed(() => currentIndex.value < questions.value.length - 1)

// 监听autoStart变化，当切换到活动状态时启动计时器
watch(() => props.autoStart, (newVal) => {
  if (newVal && questions.value.length > 0 && !isPlaceholderQuestion.value) {
    console.log('autoStart变为true，启动计时器');
    // 如果题目已经加载但还没有启动计时器，现在启动
    if (!timer.value) {
      setQuestionCountdown(currentIndex.value);
      startTimer();
      loadAnswerRecord(currentIndex.value);
    }
  } else if (!newVal && timer.value) {
    console.log('autoStart变为false，停止计时器');
    stopTimer();
  }
})

// 初始化
onMounted(async () => {
  console.log('BasicQASection组件挂载，autoStart:', props.autoStart);
  
  // 检查缓存中是否已有题目
  const cacheKey = `session_questions_${props.interviewSessionId}_${props.stage}`;
  const cachedQuestions = cacheService.get(cacheKey);
  
  if (cachedQuestions && Array.isArray(cachedQuestions) && cachedQuestions.length > 0) {
    console.log(`从缓存获取${props.stage}环节题目，题目数量:`, cachedQuestions.length);
    questions.value = processQuestions(cachedQuestions);
    initializeData();
    loadingQuestions.value = false;
    
    // 如果已经是活动状态，立即开始计时
    if (props.autoStart) {
      startTimer();
    }
  }
  
  // 无论是否自动开始，都尝试预加载题目，以确保题目准备好
  // 这样即使在自我介绍阶段，题目也已经准备好了
  if (!questions.value.length || questions.value.length === 0) {
    console.log('预加载题目，确保环节切换时题目已准备好');
    await loadQuestions(true); // 传入true表示是预加载
  }
  
  if (props.autoStart) {
    // 记录环节开始时间
    try {
      await completeStage(props.interviewSessionId, props.stage, 'START')
    } catch (error) {
      console.error('记录环节开始时间失败:', error)
    }
    
    // 如果缓存中没有题目，则加载题目
    if (!questions.value.length || questions.value.length === 0) {
      console.log('缓存中没有题目，开始加载题目');
      await loadQuestions();
    } else {
      console.log('使用缓存的题目，无需重新加载');
    }
  }
})

// 清理
onUnmounted(() => {
  stopTimer()
})

// 加载题目 - 优化后的逻辑
async function loadQuestions(isPreload = false) {
  if (loadingQuestions.value && !isPreload) return
  
  // 检查缓存中是否已有题目
  const cacheKey = `session_questions_${props.interviewSessionId}_${props.stage}`;
  const cachedQuestions = cacheService.get(cacheKey);
  
  if (cachedQuestions && Array.isArray(cachedQuestions) && cachedQuestions.length > 0) {
    console.log(`从缓存获取${props.stage}环节题目`);
    questions.value = processQuestions(cachedQuestions);
    initializeData();
    loadingQuestions.value = false;
    return;
  }
  
  // 如果是预加载，不显示加载状态
  if (!isPreload) {
    loadingQuestions.value = true;
  }
  isPlaceholderQuestion.value = false // 重置占位符状态，避免显示"没有可用题目"
  
  try {
    // 检查是否已经加载过题目
    if (questions.value.length > 0 && !isPlaceholderQuestion.value) {
      loadingQuestions.value = false
      return // 已经加载过题目，不需要再次加载
    }
    
    // 尝试分配题目，但忽略错误（可能是重复分配导致的错误）
    try {
      // 基本问答环节10道题
      const maxQuestionsPerStage = 10
      await assignQuestions(props.interviewSessionId, props.stage, maxQuestionsPerStage)
      console.log(`成功分配${props.stage}环节题目`)
    } catch (assignError) {
      console.warn(`分配${props.stage}环节题目失败，可能是题目已存在:`, assignError)
      // 忽略分配错误，继续尝试获取题目
    }
    
    // 无论分配成功与否，都尝试获取已有题目
    const response = await getSessionQuestions(props.interviewSessionId, props.stage)
    
    if (response && Array.isArray(response) && response.length > 0) {
      // 处理题目数据
      questions.value = processQuestions(response)
      
      // 初始化数据
      initializeData()
      console.log(`成功加载${questions.value.length}道${props.stage}环节题目`)
      loadingQuestions.value = false
      
      // 缓存题目，确保其他地方可以使用
      const cacheKey = `session_questions_${props.interviewSessionId}_${props.stage}`;
      cacheService.set(cacheKey, response, {ttl: 5 * 60 * 1000});
    } else {
      console.warn(`未找到${props.stage}环节题目，尝试再次获取...`);
      
      // 再尝试一次获取题目
      try {
        await assignQuestions(props.interviewSessionId, props.stage, 10);
        const retryResponse = await getSessionQuestions(props.interviewSessionId, props.stage);
        
        if (retryResponse && Array.isArray(retryResponse) && retryResponse.length > 0) {
          questions.value = processQuestions(retryResponse);
          initializeData();
          console.log(`重试成功，加载了${questions.value.length}道题目`);
          loadingQuestions.value = false;
          return;
        }
      } catch (retryError) {
        console.error('重试获取题目失败:', retryError);
      }
      
      // 如果重试失败，延迟显示占位符
      setTimeout(() => {
        if (questions.value.length === 0) {
          // 没有题目，创建一个占位符题目
          questions.value = [{
            questionId: 'placeholder',
            content: '暂无可用题目',
            options: [],
            isPlaceholder: true
          }]
          isPlaceholderQuestion.value = true
        }
        loadingQuestions.value = false
      }, 1000) // 延迟1秒
    }
  } catch (error) {
    console.error('加载题目失败:', error)
    console.error('加载题目失败，请刷新页面重试')
    
    // 延迟显示错误占位符
    setTimeout(() => {
      if (questions.value.length === 0) {
        // 创建一个占位符题目
        questions.value = [{
          questionId: 'placeholder',
          content: '加载题目失败',
          options: [],
          isPlaceholder: true
        }]
        isPlaceholderQuestion.value = true
      }
      loadingQuestions.value = false
    }, 1000) // 延迟1秒
    return
  } finally {
    // loadingQuestions会在成功加载题目或延迟函数中设置为false
    // 这里不再需要设置
  }
}

// 处理题目数据
function processQuestions(res) {
  // 检查是否为占位符题目
  if (res && Array.isArray(res) && res.length === 1 && res[0].isPlaceholder) {
    isPlaceholderQuestion.value = true
    return res
  }
  
  // 根据环节筛选题目类型
  let filtered = []
  if (props.stage === 'BASIC_QA') {
    filtered = (res.data || res).filter(q => q.inputType === 'radio')
  } else if (props.stage === 'CODE_TEST') {
    filtered = (res.data || res).filter(q => q.inputType === 'code')
  } else {
    filtered = res.data || res
  }
  
  // 确保题目数量不超过最大值 - 基本问答环节10道题
  const maxQuestionsPerStage = 10
  if (filtered.length > maxQuestionsPerStage) {
    filtered = filtered.slice(0, maxQuestionsPerStage)
  }
  
  return filtered
}

// 初始化数据
function initializeData() {
  // 初始化页面状态
  currentIndex.value = 0
  answerLoading.value = false
  
  // 清空上一次的数据，确保不会混淆
  answers.value = new Array(questions.value.length).fill('')
  startTimes.value = new Array(questions.value.length).fill(null)
  submitTimes.value = new Array(questions.value.length).fill(null)
  answerRecords.value = {}
  questionRealStartTimes.value = {} // 重置实际开始时间记录
  answeredQuestions.value = {} // 重置已回答题目记录
  
  // 检查是否为占位符题目
  isPlaceholderQuestion.value = questions.value.length === 1 && 
                               questions.value[0].isPlaceholder === true
  
  if (isPlaceholderQuestion.value) {
    return // 占位符题目不需要后续处理
  }
  
  // 设置所有题目的开始时间
  if (questions.value.length > 0) {
    const now = new Date()
    const formattedNow = formatDateForBackend(now)
    
    // 为所有题目设置相同的初始开始时间
    for (let i = 0; i < questions.value.length; i++) {
      startTimes.value[i] = formattedNow
    }
    
    // 记录第一题的实际开始时间
    if (questions.value[0] && questions.value[0].questionId) {
      questionRealStartTimes.value[questions.value[0].questionId] = Date.now()
    }
    
    // 预加载题目图片（如果有）
    questions.value.forEach(question => {
      if (question.imageUrl) {
        const img = new Image();
        img.src = question.imageUrl;
      }
    });
    
    // 设置倒计时
    setQuestionCountdown(0)
    
    // 只有在自动开始模式下才启动计时器
    if (props.autoStart) {
      startTimer()
      // 加载当前题目的答案记录
      loadAnswerRecord(0)
    }
  }
}

// 设置题目倒计时
function setQuestionCountdown(index) {
  const question = questions.value[index]
  if (question && question.inputType === 'code') {
    perQuestionCountdown.value = 300 // 代码题5分钟
  } else {
    perQuestionCountdown.value = 60 // 其他题1分钟
  }
  countdown.value = perQuestionCountdown.value
}

// 开始计时器
function startTimer() {
  stopTimer() // 确保只有一个计时器在运行
  isCountdownPaused.value = false // 重置暂停状态
  timer.value = setInterval(() => {
    if (!isCountdownPaused.value && countdown.value > 0) {
      countdown.value--
    } else if (countdown.value <= 0) {
      // 倒计时结束，停止计时器
      stopTimer()
      
      // 使用handleCountdownEnd处理倒计时结束
      handleCountdownEnd()
    }
  }, 1000)
}

// 停止计时器
function stopTimer() {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null
  }
}

// 加载答案记录 - 优化后的逻辑
async function loadAnswerRecord(index) {
  if (index < 0 || index >= questions.value.length) return
  
  const qid = questions.value[index].questionId
  
  try {
    // 从后端获取答案记录
    const response = await getAnswerRecord(props.interviewSessionId, qid)
    
    if (response && response.data) {
      // 更新本地答案
      answers.value[index] = response.data.userAnswer || ''
      
      // 更新答案记录
      answerRecords.value[qid] = response.data
      
      // 标记为已回答
      answeredQuestions.value[qid] = true
      
      // 更新提交时间
      if (response.data.submitTime) {
        submitTimes.value[index] = response.data.submitTime
      }
      
      // 如果有答案记录，停止计时器
      if (index === currentIndex.value && response.data.userAnswer) {
        stopTimer()
        isCountdownPaused.value = true // 标记为暂停状态
      }
      

    }
  } catch (error) {
    console.error(`加载题目 ${index + 1} 答案记录失败:`, error)
  }
}

// 倒计时结束处理
function handleCountdownEnd() {
  if (isPlaceholderQuestion.value) {
    return // 占位符题目不需要处理
  }
  
  const currentQuestion = questions.value[currentIndex.value]
  if (!currentQuestion) return
  
  // 获取当前题目ID
  const qid = currentQuestion.questionId
  
  // 检查是否已回答，如果已回答则忽略
  if (answeredQuestions.value[qid]) {

    return
  }
  
  
  
  // 检查是否是最后一题
  const isLastQuestion = currentIndex.value === questions.value.length - 1
  
  // 如果不是最后一题，自动进入下一题
  if (!isLastQuestion) {
    nextQuestion()
  } else {
    // 是最后一题，停留在当前页面，等待用户点击下一步按钮
    console.log('答题已完成，请点击"下一步"按钮继续')
    
    // 触发答题完成事件
    emit('questions-completed')
  }
}

// 超时提交答案函数 - 彻底禁用
async function submitTimeoutAnswer() {
  // 此函数故意留空，彻底禁用超时提交功能
  return;
}

// 选择选项 - 优化后的逻辑
async function handleSelectOption(opt) {
  if (answerLoading.value) return // 防止重复点击
  answerLoading.value = true
  
  try {
    // 立即停止计时器但保持倒计时显示
    stopTimer()
    isCountdownPaused.value = true // 标记倒计时已暂停
    
    // 获取当前题目ID和索引
    const currentIdx = currentIndex.value
    const qid = questions.value[currentIdx].questionId
    
    // 更新本地答案
    answers.value[currentIdx] = opt
    
    // 检查是否是首次回答该题目
    const isFirstAnswer = !answeredQuestions.value[qid]
    
    // 获取当前时间
    const now = Date.now()
    let timeUsed = 10 // 默认最少10秒
    let formattedStartTime = ''
    let formattedSubmitTime = formatDateForBackend(new Date(now))
    
    // 如果是首次回答，计算用时并更新标记
    if (isFirstAnswer) {
      // 如果有记录实际开始时间，计算真实用时
      if (questionRealStartTimes.value[qid]) {
        timeUsed = Math.round((now - questionRealStartTimes.value[qid]) / 1000)
        
        // 确保用时合理
        if (timeUsed < 1) timeUsed = 1 // 至少1秒
        if (timeUsed > perQuestionCountdown.value) timeUsed = perQuestionCountdown.value // 不超过最大时间
      }
      
      // 根据用时计算开始时间
      const startTime = new Date(now - timeUsed * 1000)
      formattedStartTime = formatDateForBackend(startTime)
      
      // 标记题目为已回答（首次回答后立即标记）
      answeredQuestions.value[qid] = true
    } else {
      // 如果不是首次回答，使用已有的记录中的时间信息
      const existingRecord = answerRecords.value[qid]
      if (existingRecord) {
        // 只使用已有记录的时间数据，保持不变
        timeUsed = existingRecord.timeUsed || 10
        formattedStartTime = existingRecord.startTime || formatDateForBackend(new Date(now - timeUsed * 1000))
      } else {
        // 没有现有记录，使用默认值
        formattedStartTime = formatDateForBackend(new Date(now - timeUsed * 1000))
      }
    }
    

    
    // 提交答案
    const submitData = {
      sessionId: props.interviewSessionId,
      interviewId: props.interviewSessionId,
      questionId: qid,
      userId: props.userId,
      userAnswer: opt,
      type: props.stage,
      startTime: formattedStartTime,
      submitTime: formattedSubmitTime,
      timeUsed: timeUsed
    }
    
    // 直接调用后端API提交答案
    let response
    try {
      response = await submitAnswer(submitData)
      // 成功提交，记录提交时间
      submitTimes.value[currentIdx] = formattedSubmitTime

    } catch (error) {
      console.error(`答案提交失败 - 题目ID: ${qid}, 错误:`, error)
      // 提交失败，但不中断用户操作
      console.warn("答案已保存在本地，但同步到服务器失败，将在后续自动重试")
    }
    
    // 构造一个答案记录对象
    const answerRecord = {
      id: response?.id || null,
      userId: props.userId,
      questionId: qid,
      interviewId: props.interviewSessionId,
      userAnswer: opt,
      isCorrect: response?.isCorrect || 0,
      timeUsed: timeUsed,
      startTime: formattedStartTime,
      submitTime: formattedSubmitTime,
      type: props.stage
    }
    
    // 更新本地答案和记录
    answers.value[currentIdx] = opt
    answerRecords.value = { ...answerRecords.value, [qid]: answerRecord }
    
    // 触发答案提交事件
    emit('answer-submit', {
      index: currentIdx,
      answer: opt,
      questionId: qid,
      timeUsed: timeUsed,
      userSubmitted: true 
    })
  } catch (error) {
    console.error(`题目 ${currentIndex.value + 1} 答案提交失败:`, error)
    console.error(`答案提交失败: ${error.message || '未知错误'}`)
  } finally {
    answerLoading.value = false
  }
}

// 上一题
function prevQuestion() {
  if (currentIndex.value <= 0) return
  switchQuestion(currentIndex.value - 1)
}

// 下一题
function nextQuestion() {
  if (currentIndex.value >= questions.value.length - 1) return
  switchQuestion(currentIndex.value + 1)
}

// 切换题目 - 优化后的逻辑
async function switchQuestion(newIndex) {
  if (newIndex < 0 || newIndex >= questions.value.length) {
    return // 无效索引，直接返回
  }
  
  // 记录题目切换信息
  const oldIndex = currentIndex.value
  const currentAnswer = answers.value[oldIndex]
  const currentQid = questions.value[oldIndex]?.questionId
  
  // 1. 保存当前题目的答案（如果有）
  if (currentAnswer && currentAnswer.trim() !== '') {
    try {
      // 检查是否是首次回答该题目
      const isFirstAnswer = !answeredQuestions.value[currentQid]
      
      // 获取当前时间
      const now = Date.now()
      let timeUsed = 10 // 默认最少10秒
      let formattedStartTime = ''
      let formattedSubmitTime = formatDateForBackend(new Date(now))
      
      // 如果是首次回答，计算用时
      if (isFirstAnswer) {
        // 如果有记录实际开始时间，计算真实用时
        if (questionRealStartTimes.value[currentQid]) {
          timeUsed = Math.round((now - questionRealStartTimes.value[currentQid]) / 1000)
          
          // 确保用时合理
          if (timeUsed < 1) timeUsed = 1 // 至少1秒
          if (timeUsed > perQuestionCountdown.value) timeUsed = perQuestionCountdown.value // 不超过最大时间
        }
        
        // 根据用时计算开始时间
        const startTime = new Date(now - timeUsed * 1000)
        formattedStartTime = formatDateForBackend(startTime)
        
        // 标记题目为已回答
        answeredQuestions.value[currentQid] = true
      } else {
        // 如果不是首次回答，使用已有的记录中的时间数据
        const existingRecord = answerRecords.value[currentQid]
        if (existingRecord) {
          // 复用已有的时间信息，确保修改答案不改变用时
          timeUsed = existingRecord.timeUsed || 10
          formattedStartTime = existingRecord.startTime || formatDateForBackend(new Date(now - timeUsed * 1000))
        } else {
          // 没有现有记录，使用默认值
          formattedStartTime = formatDateForBackend(new Date(now - timeUsed * 1000))
        }
      }
      

      
      // 构建提交数据
      const submitData = {
        sessionId: props.interviewSessionId,
        interviewId: props.interviewSessionId,
        questionId: currentQid,
        userId: props.userId,
        userAnswer: currentAnswer,
        type: props.stage,
        startTime: formattedStartTime,
        submitTime: formattedSubmitTime,
        timeUsed: timeUsed
      }
      
      // 保存提交时间
      submitTimes.value[oldIndex] = formattedSubmitTime
      
      // 提交答案
      const response = await submitAnswer(submitData)

      
      // 更新答案记录
      const answerRecord = {
        id: response?.id || null,
        userId: props.userId,
        questionId: currentQid,
        interviewId: props.interviewSessionId,
        userAnswer: currentAnswer,
        isCorrect: response?.isCorrect || 0,
        timeUsed: timeUsed,
        startTime: formattedStartTime,
        submitTime: formattedSubmitTime,
        type: props.stage
      }
      
      // 更新答案记录
      answerRecords.value = { ...answerRecords.value, [currentQid]: answerRecord }
    } catch (error) {
      console.error(`切换题目 - 答案提交失败 - 题目ID: ${currentQid}, 错误:`, error)
      // 捕获任何错误，不打断切换流程
    }
  }
  
  // 2. 停止当前计时器
  stopTimer()
  isCountdownPaused.value = true // 标记为暂停状态
  
  // 3. 更新当前题目索引
  currentIndex.value = newIndex
  
  // 4. 确保新题目有开始时间
  const now = new Date()
  const formattedNow = formatDateForBackend(now)
  if (!startTimes.value[newIndex]) {
    startTimes.value[newIndex] = formattedNow
  }
  
  // 5. 先加载新题目的历史答案，确保我们有最新的答案记录
  await loadAnswerRecord(newIndex)
  
  // 6. 获取新题目ID
  const newQid = questions.value[newIndex]?.questionId
  
  // 7. 检查新题目是否已回答
  const isAnswered = answeredQuestions.value[newQid] === true
  
  if (isAnswered) {
    // 如果已回答，不启动计时器，标记为暂停状态
    isCountdownPaused.value = true
  } else {
    // 如果未回答，记录实际开始时间并启动计时器
    questionRealStartTimes.value[newQid] = Date.now()
    setQuestionCountdown(newIndex)
    startTimer()

  }
  
  // 8. 触发题目切换事件
  emit('question-change', {
    index: newIndex,
    question: questions.value[newIndex],
    totalQuestions: questions.value.length
  })
}

// 完成环节
async function completeSection() {
  try {
    // 预加载下一环节(代码操作题)的题目，这样切换时可以直接显示
    try {
      console.log('预加载代码操作题环节题目');
      // 使用父组件的interviewSessionId预加载CODE_TEST环节的题目
      await assignQuestions(props.interviewSessionId, 'CODE_TEST', 3);
      const codeTestResponse = await getSessionQuestions(props.interviewSessionId, 'CODE_TEST');
      // 缓存结果，有效期1分钟
      cacheService.set(`session_questions_${props.interviewSessionId}_CODE_TEST`, codeTestResponse, {ttl: 60 * 1000});
      console.log('代码操作题预加载成功');
    } catch (error) {
      console.warn('预加载代码操作题失败，可能会影响环节切换流畅性:', error);
    }
    
    if (isPlaceholderQuestion.value) {
      emit('section-complete')
      return
    }
    
    // 检查是否所有题目都已回答
    const unansweredQuestions = questions.value.filter((q, idx) => 
      !answers.value[idx] || answers.value[idx].trim() === ''
    )
    
    if (unansweredQuestions.length > 0) {
      console.error(`还有 ${unansweredQuestions.length} 道题目未回答，请完成所有题目后再继续`)
      return
    }
    
    // 记录环节结束时间
    try {
      await completeStage(props.interviewSessionId, props.stage, 'COMPLETE')
    } catch (error) {
      console.error('记录环节结束时间失败:', error)
      // 继续执行，不阻止用户完成环节
    }
    
    // 触发完成事件
    emit('section-complete')
  } catch (error) {
    console.error('完成环节失败:', error)
    console.error('完成环节失败，请重试')
  }
}

// 跳过全部题目（测试用）- 自动选择正确答案并提交
const skipping = ref(false)

async function skipAllQuestions() {
  if (skipping.value || isPlaceholderQuestion.value) return
  skipping.value = true
  stopTimer()

  try {
    console.log('⏭ 测试模式：自动填入正确答案并提交所有题目')

    for (let i = 0; i < questions.value.length; i++) {
      const q = questions.value[i]
      const qid = q.questionId
      const correctAnswer = q.correctAnswer || (q.options && q.options[0]) || 'A'

      // 跳过已回答的题目
      if (answeredQuestions.value[qid]) continue

      // 设置答案
      answers.value[i] = correctAnswer
      answeredQuestions.value[qid] = true

      const now = Date.now()
      const formattedNow = formatDateForBackend(new Date(now))
      const formattedStart = formatDateForBackend(new Date(now - 3000))

      // 提交答案
      try {
        await submitAnswer({
          sessionId: props.interviewSessionId,
          interviewId: props.interviewSessionId,
          questionId: qid,
          userId: props.userId,
          userAnswer: correctAnswer,
          type: props.stage,
          startTime: formattedStart,
          submitTime: formattedNow,
          timeUsed: 3
        })
      } catch (err) {
        console.warn(`跳过模式：题目 ${i + 1} 提交失败:`, err)
      }
    }

    // 跳到最后一题然后完成
    currentIndex.value = questions.value.length - 1
    console.log('⏭ 全部题目已自动作答，完成环节')
    await completeSection()
  } catch (error) {
    console.error('跳过失败:', error)
  } finally {
    skipping.value = false
  }
}

// 提供给父组件的方法
defineExpose({
  loadQuestions,
  completeSection
})

// 处理定时器
function handleTimer() {
  if (countdown.value > 0) {
    countdown.value -= 1
  }
  
  if (countdown.value <= 0) {
    // 停止计时器
    stopTimer()
    
    // 处理倒计时结束
    handleCountdownEnd()
  }
}
</script>

<style scoped>
.basic-qa-section {
  max-width: 100%;
  background-color: #fff;
  padding: 25px; /* 增加整体内边距 */
  padding-top: 15px; /* 顶部内边距调整 */
  box-sizing: border-box;
}

.placeholder-message {
  padding: 30px;
  text-align: center;
}

.progress-bar {
  margin-bottom: 25px; /* 显著增加与题目的间距 */
  margin-top: 0px; /* 移除负边距 */
}

.progress-text {
  font-size: 15px;
  color: #606266;
  margin-bottom: 8px; /* 增加文字与进度条的间距 */
}

.progress-track {
  height: 8px;
  background-color: #f0f2f5;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #409eff;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.question-header {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px; /* 增加题目与选项的间距 */
  margin-top: 7px;
}

.question-number {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 38px; /* 增大序号圆圈 */
  height: 38px; /* 增大序号圆圈 */
  background-color: #409eff;
  color: white;
  border-radius: 50%;
  font-size: 18px; /* 增大字体 */
  font-weight: bold;
  margin-right: 16px; /* 增加与标题的间距 */
  flex-shrink: 0;
}

.question-title {
  flex: 1;
  font-size: 18px; /* 增大标题字体 */
  font-weight: 500;
  color: #303133;
  line-height: 1.5;
  background-color: #f8f9fc;
  padding: 14px 18px; /* 增加内边距 */
  border-radius: 8px; /* 增加圆角 */
  border-left: 4px solid #409eff;
  margin-right: 20px; /* 增加与计时器的间距 */
}

.question-timer {
  color: #f56c6c;
  font-size: 16px; /* 增大计时器字体 */
  white-space: nowrap;
  margin-top: 5px;
  flex-shrink: 0;
}

.question-desc {
  font-size: 16px; /* 增大描述字体 */
  color: #606266;
  line-height: 1.5;
  padding-left: 54px; /* 增加左侧缩进 */
  margin-bottom: 20px;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 16px; /* 增加选项间距 */
  margin-bottom: 25px; /* 增加与按钮区域的间距 */
}

.option-item {
  display: flex;
  align-items: center;
  border: 1px solid #dcdfe6;
  border-radius: 8px; /* 增加圆角 */
  padding: 16px 20px; /* 增加内边距 */
  cursor: pointer;
  transition: all 0.2s;
  background-color: #fff;
}

.option-item:hover {
  border-color: #c6e2ff;
  background-color: #f5faff;
}

.option-item.active {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.option-item.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.option-letter {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 34px; /* 增大选项字母圆圈 */
  height: 34px; /* 增大选项字母圆圈 */
  border: 1px solid #dcdfe6;
  border-radius: 50%;
  margin-right: 16px; /* 增加与选项内容的间距 */
  color: #606266;
  font-size: 16px; /* 增大字体 */
  font-weight: 500;
  flex-shrink: 0;
}

.option-item.active .option-letter {
  border-color: #409eff;
  color: #409eff;
  background-color: #fff;
}

.option-content {
  flex: 1;
  font-size: 16px; /* 增大选项内容字体 */
  color: #303133;
  line-height: 1.5;
  word-break: break-word;
}

.option-item.active .option-content {
  color: #409eff;
  font-weight: 500;
}

.option-radio {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 18px;
  height: 18px;
  border: 1px solid #dcdfe6;
  border-radius: 50%;
  margin-left: 12px;
  flex-shrink: 0;
}

.option-item.active .option-radio {
  border-color: #409eff;
}

.radio-dot {
  width: 10px;
  height: 10px;
  background-color: #409eff;
  border-radius: 50%;
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  margin-top: 15px; /* 增加与选项的间距 */
}

.action-buttons .el-button {
  min-width: 90px;
  font-size: 14px;
}

@media (max-width: 576px) {
  .basic-qa-section {
    padding: 15px;
  }
  
  .question-header {
    flex-direction: column;
  }
  
  .question-timer {
    margin-left: 44px;
    margin-top: 10px;
  }
  
  .question-title {
    margin-right: 0;
    margin-bottom: 10px;
  }
  
  .question-desc {
    padding-left: 15px;
  }
}

@media (min-width: 768px) {
  .basic-qa-section {
    padding: 24px;
  }
  
  .option-item {
    padding: 15px 18px;
  }
  
  .option-letter {
    width: 32px;
    height: 32px;
  }
  
  .action-buttons .el-button {
    min-width: 100px;
    font-size: 15px;
  }
}
.loading-container {
  padding: 20px;
  margin: 20px 0;
}
</style> 