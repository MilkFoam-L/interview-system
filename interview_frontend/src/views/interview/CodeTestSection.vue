<template>
  <div class="code-test-section">
    <!-- 处理常规代码题显示 -->
    <template v-if="questions.length > 0 && !isPlaceholderQuestion && questions[currentIndex].inputType === 'code'">
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
          <span v-if="answerRecords[questions[currentIndex].questionId] && answerRecords[questions[currentIndex].questionId].userAnswer">已提交</span>
          <span v-else style="font-size: 18px; font-weight: bold;">{{ formatTime(countdown) }}</span>
        </div>
      </div>
      
      <div v-if="questions[currentIndex].description" class="question-desc">
        {{ questions[currentIndex].description }}
      </div>
      
      <!-- 代码编辑器区域 -->
      <div class="code-editor-area">
        <div class="editor-header">
          <span class="editor-title">代码编辑区</span>
        </div>
        <div class="editor-container">
          <textarea
            v-model="answers[currentIndex]"
            class="code-editor"
            :placeholder="'请在这里编写代码...'"
            :disabled="answerLoading"
          ></textarea>
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
          type="primary"
          :disabled="answerLoading"
          @click="submitCode">
          提交代码
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
    </template>
    
    <!-- 处理占位符题目 -->
    <template v-else-if="isPlaceholderQuestion">
      <div class="placeholder-message">
        <el-result
          icon="info"
          title="暂无可用代码题"
          sub-title="系统根据您的历史作答记录和当前岗位要求，暂时没有找到适合的代码题"
        >
          <template #extra>
            <el-button type="primary" @click="completeSection">继续下一环节</el-button>
          </template>
        </el-result>
      </div>
    </template>
    
    <!-- 处理无题目情况 -->
    <el-empty v-else description="正在加载代码题..." v-loading="loadingQuestions">
      <el-button type="primary" @click="loadQuestions">重新加载题目</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { formatTime, formatDateForBackend } from '../../utils/dateUtils.js'
import { assignQuestions, getSessionQuestions } from '../../api/sessionQuestion.js'
import { submitAnswer, getAnswerRecord } from '../../api/answerRecord.js'
import { completeStage, startStage } from '../../api/stage.js'
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
    default: 'CODE_TEST'
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
const judgeResult = ref(null) // 判题结果

// 计时器相关
const countdown = ref(0)
const timer = ref(null)
const perQuestionCountdown = ref(300) // 代码题默认5分钟
const isCountdownPaused = ref(false) // 倒计时暂停状态

// 计算属性
const hasPrevQuestion = computed(() => currentIndex.value > 0)
const hasNextQuestion = computed(() => currentIndex.value < questions.value.length - 1)

// 监听autoStart变化，当变为true时开始加载题目
watch(() => props.autoStart, (newVal) => {
  if (newVal && questions.value.length === 0) {
    console.log('🎯 CodeTestSection: autoStart变为true，开始加载代码实操题目')
    loadQuestions()
  }
}, { immediate: false })

// 初始化
onMounted(async () => {
  if (props.autoStart) {
    console.log('🎯 CodeTestSection: 组件挂载时autoStart为true，立即加载题目')
    await loadQuestions()
  } else {
    console.log('🎯 CodeTestSection: 组件挂载时autoStart为false，等待autoStart变为true')
  }
})

// 清理
onUnmounted(() => {
  stopTimer()
})

// 加载题目
async function loadQuestions() {
  loadingQuestions.value = true
  
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
  
  try {
    // 记录环节开始时间
    try {
      await startStage(props.interviewSessionId, props.stage);
    } catch (stageError) {
      console.error(`记录${props.stage}环节开始时间失败:`, stageError);
    }
    
    // 代码实操题环节3道题
    const maxQuestionsPerStage = 3
    
    // 缓存键
    const cacheKey = `session_questions_${props.interviewSessionId}_${props.stage}`
    
    // 尝试从缓存获取
    const cachedQuestions = cacheService.get(cacheKey)
    if (cachedQuestions) {
      questions.value = processQuestions(cachedQuestions)
      initializeData()
      return
    }
    
    // 尝试分配题目，但忽略错误（可能是重复分配导致的错误）
    try {
      await assignQuestions(props.interviewSessionId, props.stage, maxQuestionsPerStage);
      console.log(`成功分配${props.stage}环节题目`);
    } catch (assignError) {
      console.warn(`分配${props.stage}环节题目失败，可能是题目已存在:`, assignError);
      // 忽略分配错误，继续尝试获取题目
    }
    
    // 无论分配成功与否，都尝试获取已有题目
    const existingQuestions = await getSessionQuestions(props.interviewSessionId, props.stage)
    
    if (existingQuestions && Array.isArray(existingQuestions) && existingQuestions.length > 0) {
      // 缓存结果
      cacheService.set(cacheKey, existingQuestions, { ttl: 60 * 1000 })
      questions.value = processQuestions(existingQuestions)
      initializeData()
      console.log(`成功加载${questions.value.length}道${props.stage}环节题目`)
    } else {
      console.warn(`未找到${props.stage}环节题目，显示占位符`)
      // 没有题目，创建一个占位符题目
      questions.value = [{
        questionId: 'placeholder',
        content: '暂无可用代码题',
        options: [],
        isPlaceholder: true
      }]
      isPlaceholderQuestion.value = true
      initializeData()
    }
  } catch (error) {
    console.error('加载题目失败:', error)
    console.error('加载题目失败，请刷新页面重试')
    
    // 创建一个占位符题目
    questions.value = [{
      questionId: 'placeholder',
      content: '加载题目失败',
      options: [],
      isPlaceholder: true
    }]
    isPlaceholderQuestion.value = true
    initializeData()
  } finally {
    loadingQuestions.value = false
  }
}

// 处理题目数据
function processQuestions(res) {
  // 代码实操题环节3道题
  const maxQuestionsPerStage = 3
  
  // 检查是否为占位符题目
  if (res && Array.isArray(res) && res.length === 1 && res[0].isPlaceholder) {

    return res; // 直接返回占位符题目，让UI层处理
  }
  
  // 确保res是有效数据
  if (!res) {
    console.error(`题目数据为空，请检查API返回`)
    console.error(`获取题目失败，请刷新页面重试`)
    return []
  }
  
  // 处理数据格式
  let sourceData = res
  if (res.data && Array.isArray(res.data)) {
    sourceData = res.data
  } else if (!Array.isArray(res)) {
    console.error(`题目数据格式不正确:`, res)
    console.error(`题目数据格式不正确，请刷新页面重试`)
    return []
  }
  

  
  // 根据环节过滤题目类型
  let filtered = []
  if (props.stage === 'CODE_TEST') {
    filtered = sourceData.filter(q => q && q.inputType === 'code')

  } else {
    filtered = sourceData
  }
  
  // 确保题目数量不超过最大值
  if (filtered.length > maxQuestionsPerStage) {

    filtered = filtered.slice(0, maxQuestionsPerStage)
  }
  

  return filtered
}

// 初始化数据
function initializeData() {
  // 检查题目数组是否有效
  if (!questions.value || !Array.isArray(questions.value)) {
    questions.value = [];
    isPlaceholderQuestion.value = true;
    return;
  }
  
  // 重置索引和答案数组
  currentIndex.value = 0
  answers.value = new Array(questions.value.length).fill('')
  startTimes.value = new Array(questions.value.length).fill(null)
  submitTimes.value = new Array(questions.value.length).fill(null)
  
  // 检查是否为占位符题目或者没有题目
  isPlaceholderQuestion.value = questions.value.length === 0 || 
                               (questions.value.length === 1 && questions.value[0].isPlaceholder === true);
  
  if (isPlaceholderQuestion.value) {
    return; // 占位符题目不需要设置计时器和加载答案记录
  }
  
  // 题目加载成功
  
  // 设置当前题目的开始时间
  if (questions.value.length > 0) {
    const now = new Date()
    startTimes.value[0] = formatDateForBackend(now)
    
    // 设置倒计时，代码题5分钟
    countdown.value = perQuestionCountdown.value
    
    
    // 先加载可能存在的答案记录
    loadAnswerRecord(0).then(() => {
      // 检查是否已回答
      const qid = questions.value[0].questionId
      const isAnswered = answerRecords.value[qid] && answerRecords.value[qid].userAnswer
      
      if (isAnswered) {
        isCountdownPaused.value = true // 标记为暂停状态
      } else {
        startTimer()
      }
    }).catch(error => {
      startTimer()
    })
  }
}

// 开始计时器
function startTimer() {
  stopTimer() // 确保只有一个计时器在运行
  isCountdownPaused.value = false // 重置暂停状态
  // 确保倒计时有初始值
  if (countdown.value <= 0) {
    countdown.value = perQuestionCountdown.value
  }
  
  timer.value = setInterval(() => {
    if (!isCountdownPaused.value && countdown.value > 0) {
      countdown.value--
    } else if (countdown.value <= 0) {
      // 倒计时结束，停止计时器
      stopTimer()
      
      // 调用倒计时结束处理函数
      handleCountdownEnd()
    }
  }, 1000)
}

// 倒计时结束处理
function handleCountdownEnd() {
  // 获取当前题目ID
  const currentQuestion = questions.value[currentIndex.value]
  if (!currentQuestion) return
  
  const qid = currentQuestion.questionId
  
  // 检查是否已回答，如果已回答则忽略
  if (answerRecords.value[qid] && answerRecords.value[qid].userAnswer) {
    return
  }
  
  // 如果有下一题，自动切换
  if (hasNextQuestion.value) {
    nextQuestion()
  } else {
    console.log('已到达最后一题，请点击"完成"按钮结束本环节')
  }
}

// 加载答案记录
async function loadAnswerRecord(index) {
  if (index < 0 || index >= questions.value.length) return Promise.resolve()
  
  return new Promise(async (resolve, reject) => {
    try {
      const qid = questions.value[index].questionId
      

      
      // 直接从服务器获取，不使用缓存
      const response = await getAnswerRecord(props.userId, qid, props.interviewSessionId, false)
      
      if (response && response.userAnswer) {
        // 更新本地答案
        answers.value[index] = response.userAnswer || ''
        
        // 更新答案记录
        answerRecords.value[qid] = response
        
        // 如果有答案记录，停止计时器
        if (index === currentIndex.value) {
          stopTimer()
          isCountdownPaused.value = true // 标记为暂停状态
        }
        

      } else {

      }
      
      resolve(response)
    } catch (error) {
      console.error(`加载题目 ${index + 1} 的答案记录失败:`, error)
      reject(error)
    }
  })
}

// 为指定题目提交代码（用于异步保存，不影响当前界面状态）
async function submitCodeForQuestion(questionIndex, code) {
  try {
    // 获取题目ID
    const qid = questions.value[questionIndex].questionId
    
    // 检查是否是首次回答该题目
    const isFirstAnswer = !answerRecords.value[qid] || !answerRecords.value[qid].userAnswer
    
    // 获取当前时间
    const now = new Date()
    let timeUsed = 10 // 默认最少10秒
    let formattedStartTime = ''
    let formattedSubmitTime = formatDateForBackend(now)
    
    if (isFirstAnswer) {
      // 首次回答，计算真实用时
      const startTime = new Date(startTimes.value[questionIndex])
      timeUsed = Math.floor((now - startTime) / 1000)
      
      // 确保用时合理
      if (timeUsed < 1) timeUsed = 1 // 至少1秒
      if (timeUsed > perQuestionCountdown.value) timeUsed = perQuestionCountdown.value // 不超过最大时间
      
      formattedStartTime = formatDateForBackend(startTime)
    } else {
      // 如果不是首次回答，使用已有的记录中的时间信息
      const existingRecord = answerRecords.value[qid]
      if (existingRecord) {
        timeUsed = existingRecord.timeUsed || 10
        formattedStartTime = existingRecord.startTime || formatDateForBackend(new Date(now - timeUsed * 1000))
      } else {
        formattedStartTime = formatDateForBackend(new Date(now - timeUsed * 1000))
      }
    }
    
    // 提交答案
    const response = await submitAnswer({
      sessionId: props.interviewSessionId,
      interviewId: props.interviewSessionId,
      questionId: qid,
      userId: props.userId,
      userAnswer: code,
      type: props.stage,
      startTime: formattedStartTime,
      submitTime: formattedSubmitTime,
      timeUsed: timeUsed
    })
    
    submitTimes.value[questionIndex] = formattedSubmitTime
    
    // 更新答案记录
    const answerRecord = {
      id: response?.id || null,
      userId: props.userId,
      questionId: qid,
      interviewId: props.interviewSessionId,
      userAnswer: code,
      isCorrect: response?.isCorrect || 0,
      timeUsed: timeUsed,
      startTime: formattedStartTime,
      submitTime: formattedSubmitTime,
      type: props.stage
    }
    
    // 更新本地记录
    answerRecords.value = { ...answerRecords.value, [qid]: answerRecord }
    
    console.log(`📄 题目 ${questionIndex + 1} 代码已后台保存`)
  } catch (error) {
    console.error(`题目 ${questionIndex + 1} 代码后台保存失败:`, error)
  }
}

// 提交代码
async function submitCode() {
  if (answerLoading.value) return // 防止重复点击
  answerLoading.value = true
  
  try {
    // 立即停止计时器
    stopTimer()
    isCountdownPaused.value = true // 标记为暂停状态 // 确保倒计时显示为0

    
    const currentCode = answers.value[currentIndex.value]
    if (!currentCode.trim()) {
      console.error('请先编写代码再提交')
      answerLoading.value = false
      return
    }
    
    // 获取当前题目ID
    const qid = questions.value[currentIndex.value].questionId
    
    // 检查是否是首次回答该题目
    const isFirstAnswer = !answerRecords.value[qid] || !answerRecords.value[qid].userAnswer
    
    // 获取当前时间
    const now = new Date()
    let timeUsed = 10 // 默认最少10秒
    let formattedStartTime = ''
    let formattedSubmitTime = formatDateForBackend(now)
    
    if (isFirstAnswer) {
      // 首次回答，计算真实用时
      const startTime = new Date(startTimes.value[currentIndex.value])
      timeUsed = Math.floor((now - startTime) / 1000)
      
      // 确保用时合理
      if (timeUsed < 1) timeUsed = 1 // 至少1秒
      if (timeUsed > perQuestionCountdown.value) timeUsed = perQuestionCountdown.value // 不超过最大时间
      
      formattedStartTime = formatDateForBackend(startTime)

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
    const response = await submitAnswer({
      sessionId: props.interviewSessionId,
      interviewId: props.interviewSessionId,
      questionId: qid,
      userId: props.userId,
      userAnswer: currentCode,
      type: props.stage,
      startTime: formattedStartTime,
      submitTime: formattedSubmitTime,
      timeUsed: timeUsed
    })
    
    submitTimes.value[currentIndex.value] = formattedSubmitTime
    
    // 更新答案记录
    const answerRecord = {
      id: response?.id || null,
      userId: props.userId,
      questionId: qid,
      interviewId: props.interviewSessionId,
      userAnswer: currentCode,
      isCorrect: response?.isCorrect || 0,
      timeUsed: timeUsed,
      startTime: formattedStartTime,
      submitTime: formattedSubmitTime,
      type: props.stage
    }
    
    // 更新本地记录
    answerRecords.value = { ...answerRecords.value, [qid]: answerRecord }
    
    // 触发答案提交事件
    emit('answer-submit', {
      index: currentIndex.value,
      answer: currentCode,
      questionId: qid,
      timeUsed: timeUsed,
      userSubmitted: true
    })
    
    // 代码提交成功，仅在控制台显示
    const message = response?.score ? 
      `📄 代码已提交，得分：${response.score}/100` : 
      '📄 代码已成功提交'
    console.log(message)
    
    // 在控制台显示详细得分信息
    console.log(`🔍 后端响应原始数据:`, response)
    if (response?.score !== undefined) {
      console.log(`🎯 题目 ${currentIndex.value + 1} 提交成功！`)
      console.log(`📊 得分：${response.score}/100`)
      console.log(`📊 得分类型：${typeof response.score}`)
      console.log(`📊 得分原始值：`, response.score)
      console.log(`⏱️ 用时：${timeUsed}秒`)
      console.log(`📝 代码长度：${currentCode.length}字符`)
      if (response.isCorrect !== undefined) {
        console.log(`✅ 答案正确性：${response.isCorrect === 1 ? '正确' : '部分正确'}`)
      }
    } else {
      console.log(`📄 题目 ${currentIndex.value + 1} 代码已成功提交`)
      console.log(`⚠️ 响应中没有score字段`)
    }
  } catch (error) {
    console.error(`题目 ${currentIndex.value + 1} 代码提交失败:`, error)
    console.error(`代码提交失败: ${error.message || '未知错误'}`)
  } finally {
    answerLoading.value = false
  }
}

// 超时提交答案函数 - 彻底禁用
async function submitTimeoutAnswer() {
  return;
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

// 切换题目
async function switchQuestion(newIndex) {
  if (newIndex < 0 || newIndex >= questions.value.length) {
    console.error(`无效的题目索引: ${newIndex}，题目总数: ${questions.value.length}`)
    return
  }
  
  // 1. 先停止当前计时器
  stopTimer()
  isCountdownPaused.value = true // 标记为暂停状态
  
  // 2. 立即更新当前题目索引，避免界面卡顿
  const oldIndex = currentIndex.value
  currentIndex.value = newIndex
  
  // 3. 确保新题目有开始时间
  if (!startTimes.value[newIndex]) {
    const now = new Date()
    startTimes.value[newIndex] = formatDateForBackend(now)
  }
  
  // 4. 先加载新题目的历史答案，确保我们有最新的答案记录
  await loadAnswerRecord(newIndex)
  
  // 5. 获取新题目ID
  const newQid = questions.value[newIndex]?.questionId
  
  // 6. 检查新题目是否已回答
  const isAnswered = answerRecords.value[newQid] && answerRecords.value[newQid].userAnswer
  
  if (isAnswered) {
    // 如果已回答，不启动计时器，并将倒计时设为0
    isCountdownPaused.value = true // 标记为暂停状态
    // 确保计时器已停止
    stopTimer()
  } else {
    // 如果未回答，启动计时器
    countdown.value = perQuestionCountdown.value // 重置倒计时为5分钟
    startTimer()
  }
  
  // 7. 触发题目切换事件
  emit('question-change', {
    index: newIndex,
    question: questions.value[newIndex],
    totalQuestions: questions.value.length
  })
  
  // 8. 异步保存之前题目的答案（不阻塞界面）
  const currentAnswer = answers.value[oldIndex]
  if (currentAnswer && currentAnswer.trim()) {
    // 使用Promise异步保存，不等待结果，避免阻塞新题目的输入
    submitCodeForQuestion(oldIndex, currentAnswer).catch(error => {
      console.error('保存上一题答案失败:', error)
    })
  }
}

// 完成环节
async function completeSection() {
  try {
    // 只有在有题目且不是占位符题目的情况下才保存答案
    if (questions.value.length > 0 && !isPlaceholderQuestion.value) {
      // 异步保存当前题目答案，不等待代码判题结果
      const currentAnswer = answers.value[currentIndex.value]
      if (currentAnswer && currentAnswer.trim()) {
        console.log('🚀 异步提交代码，不阻塞面试完成流程')
        // 异步提交代码，不等待结果
        submitCode().catch(error => {
          console.error('代码后台提交失败，但不影响面试完成:', error)
        })
      }
    }
    
    await completeStage(props.interviewSessionId, props.stage, 'COMPLETE')
    
    // 立即触发完成事件，不等待代码判题
    emit('section-complete')
    
    console.log('✅ 代码操作题环节已完成（代码判题在后台继续进行）')
  } catch (error) {
    console.error('完成环节失败:', error)
    console.error(`完成环节失败: ${error.message || '未知错误'}`)
  }
}

// 显示判题结果
function showJudgeResult(response) {
  console.log('判题响应:', response)
  
  // 解析判题结果
  if (response && response.analysis) {
    try {
      // 如果analysis是JSON字符串，解析它
      let analysisData = response.analysis
      if (typeof analysisData === 'string') {
        // 尝试从文本中提取结构化信息
        judgeResult.value = parseAnalysisText(analysisData)
        // 保留原始analysis文本用于详细显示
        judgeResult.value.originalAnalysis = analysisData
      } else {
        judgeResult.value = analysisData
      }
      
      // 设置基本信息
      if (!judgeResult.value.score && response.score) {
        const scoreValue = typeof response.score === 'number' ? response.score : 
                          (typeof response.score === 'string' ? parseFloat(response.score) : 0)
        // 如果score是0-1之间的小数，转换为百分制
        judgeResult.value.score = scoreValue <= 1 ? Math.round(scoreValue * 100) : Math.round(scoreValue)
      }
      
      if (!judgeResult.value.status) {
        judgeResult.value.status = determineStatusFromScore(judgeResult.value.score)
      }
      
    } catch (error) {
      console.error('解析判题结果失败:', error)
      // 创建默认结果
      judgeResult.value = createDefaultResult(response)
    }
  } else {
    // 创建默认结果
    judgeResult.value = createDefaultResult(response)
  }
  
  // 显示消息提示和详细分析
  const message = getResultMessage(judgeResult.value)
  
  // 显示详细分析信息
  if (judgeResult.value.originalAnalysis || judgeResult.value.errorMessage) {
    const detailMessage = judgeResult.value.originalAnalysis || judgeResult.value.errorMessage
    console.log('详细分析:', detailMessage)
    
    // 在控制台显示详细信息
    if (detailMessage.includes('错误信息')) {
      console.error('代码执行错误:', detailMessage)
    }
  }
  
  // 只在控制台显示判分结果，不显示弹窗
  console.log(`[代码判分] ${message}`)
  if (judgeResult.value.score >= 100) {
    console.log('✅ 代码执行完美')
  } else if (judgeResult.value.score >= 60) {
    console.log('⚠️ 代码部分正确')
  } else {
    console.log('❌ 代码需要改进')
  }
}

// 解析分析文本
function parseAnalysisText(analysisText) {
  const result = {
    status: 'SE',
    score: 0,
    passedCount: 0,
    totalCount: 0,
    executionTime: null,
    memoryUsage: null,
    errorMessage: null,
    testCaseResults: []
  }
  
  if (!analysisText) return result
  
  // 提取得分
  const scoreMatch = analysisText.match(/得分[：:]\s*(\d+)/)
  if (scoreMatch) {
    result.score = parseInt(scoreMatch[1])
  }
  
  // 提取通过测试用例数量
  const testCaseMatch = analysisText.match(/通过测试用例[：:]\s*(\d+)\/(\d+)/)
  if (testCaseMatch) {
    result.passedCount = parseInt(testCaseMatch[1])
    result.totalCount = parseInt(testCaseMatch[2])
  }
  
  // 提取状态
  const statusMatch = analysisText.match(/判题结果[：:]\s*([^\n]+)/)
  if (statusMatch) {
    const statusText = statusMatch[1].trim()
    if (statusText.includes('完全正确')) result.status = 'AC'
    else if (statusText.includes('部分正确')) result.status = 'PA'
    else if (statusText.includes('编译错误')) result.status = 'CE'
    else if (statusText.includes('超时')) result.status = 'TLE'
    else if (statusText.includes('运行错误')) result.status = 'RE'
    else if (statusText.includes('答案错误')) result.status = 'WA'
  }
  
  // 提取执行时间
  const timeMatch = analysisText.match(/执行时间[：:]\s*(\d+)ms/)
  if (timeMatch) {
    result.executionTime = parseInt(timeMatch[1])
  }
  
  // 提取内存使用
  const memoryMatch = analysisText.match(/内存使用[：:]\s*(\d+)KB/)
  if (memoryMatch) {
    result.memoryUsage = parseInt(memoryMatch[1])
  }
  
  // 提取错误信息
  const errorMatch = analysisText.match(/错误信息[：:]\s*([^\n]+)/)
  if (errorMatch) {
    result.errorMessage = errorMatch[1].trim()
  }
  
  return result
}

// 滚动到编辑器
function scrollToEditor() {
  const editor = document.querySelector('.code-editor')
  if (editor) {
    editor.scrollIntoView({ behavior: 'smooth' })
    editor.focus()
  }
}

// 提供给父组件的方法
defineExpose({
  loadQuestions,
  completeSection
})

// 停止计时器
function stopTimer() {
  if (timer.value) {
    clearInterval(timer.value)
    timer.value = null

  }
}

// 设置题目倒计时
function setQuestionCountdown(index) {
  // 代码题默认5分钟
  countdown.value = perQuestionCountdown.value

}
</script>

<style scoped>
.code-test-section {
  max-width: 100%;
  background-color: #fff;
  padding: 25px; /* 增加整体内边距 */
  padding-top: 15px; /* 顶部内边距调整 */
  box-sizing: border-box;
}

.progress-bar {
  margin-bottom: 25px; /* 显著增加与题目的间距 */
  margin-top: 0px;
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

.question-timer span {
  font-weight: bold;
}

.question-desc {
  font-size: 16px; /* 增大描述字体 */
  color: #606266;
  line-height: 1.5;
  padding-left: 54px; /* 增加左侧缩进 */
  margin-bottom: 25px; /* 增加底部间距 */
}

/* 代码编辑器区域样式 */
.code-editor-area {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 25px; /* 增加底部间距 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.code-editor-area:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.editor-header {
  background: linear-gradient(to right, #f5f7fa, #f0f2f5);
  padding: 12px 16px;
  border-bottom: 1px solid #dcdfe6;
  display: flex;
  align-items: center;
}

.editor-title {
  font-size: 16px;
  color: #409eff;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.editor-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background-color: #409eff;
  margin-right: 8px;
  border-radius: 2px;
}

.editor-container {
  padding: 0;
  position: relative;
}

.code-editor {
  width: 100%;
  height: 300px; /* 从400px减小到300px */
  padding: 16px;
  font-family: 'Courier New', monospace;
  font-size: 16px;
  line-height: 1.6;
  color: #303133;
  border: none;
  resize: none;
  outline: none;
  background-color: #fafafa;
  transition: background-color 0.3s;
}

.code-editor:focus {
  background-color: #fff;
}

.action-buttons {
  display: flex;
  justify-content: space-between;
  margin-top: 15px; /* 调整与上方内容的间距 */
}

/* 增加按钮样式 */
.action-buttons .el-button {
  min-width: 120px;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px; /* 与其他元素保持一致的圆角 */
  transition: all 0.3s ease;
}

.action-buttons .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.action-buttons .el-button--primary:not(.is-plain) {
  background: linear-gradient(135deg, #409eff, #2c89e0);
  border-color: #409eff;
}

.action-buttons .el-button--success {
  background: linear-gradient(135deg, #67c23a, #4cad29);
  border-color: #67c23a;
}

/* 占位符消息样式 */
.placeholder-message {
  padding: 30px;
  text-align: center;
}

@media (max-width: 768px) {
  .code-test-section {
    padding: 15px;
  }
  
  .question-header {
    flex-direction: column;
  }
  
  .question-timer {
    margin-left: 54px;
    margin-top: 10px;
  }
  
  .question-title {
    margin-right: 0;
    margin-bottom: 10px;
  }
  
  .question-desc {
    padding-left: 15px;
  }
  
  .action-buttons .el-button {
    min-width: 100px;
    padding: 10px 16px;
    font-size: 14px;
  }
  
  .code-editor {
    height: 300px;
    font-size: 14px;
  }
}

@media (min-width: 992px) {
  .code-test-section {
    padding: 30px;
    padding-top: 20px;
  }
  
  .code-editor {
    height: 350px; /* 从450px减小到350px */
  }
}

</style> 