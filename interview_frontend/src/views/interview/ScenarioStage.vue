<template>
  <div class="scenario-stage">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="3" animated />
      <div class="loading-text">加载面试场景中...</div>
    </div>

    <div v-else-if="completed" class="completion-container">
      <el-result icon="success" title="场景问答已完成" sub-title="您已完成所有场景模拟问题" />
      <el-button type="primary" @click="$emit('stage-complete')">进入下一环节</el-button>
    </div>

    <div v-else class="main-container">
      <!-- 场景背景和对话区域合并 -->
      <div v-if="scenarioContext" class="scenario-context">
        <div class="context-header">
          <el-icon><InfoFilled /></el-icon> 场景背景：
          <span class="context-text">{{ scenarioContext }}</span>
        </div>
      </div>

      <div class="chat-header">
        <div class="chat-progress">对话进度: {{ currentIndex + 1 }}/{{ questions.length }}</div>
      </div>

      <!-- 对话区域 -->
      <div class="chat-container" ref="chatContainer">
        <!-- 所有问答展示为连续的聊天流 -->
        <template v-for="item in chatMessages" :key="item.id || `${item.role}-${item.roundNo}-${item.content.slice(0, 20)}`">
          <!-- 面试官消息 -->
          <div v-if="item.role === 'interviewer'" class="message-row interviewer-row" 
               :class="{'fade-in': !isReplayingHistory}">
            <div class="avatar interviewer-avatar">
              <img src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" alt="面试官" />
            </div>
            <div class="message interviewer" :class="{'follow-up-question': item.isFollowUp}">
              {{ item.content }}
            </div>
          </div>
          
          <!-- 应聘者消息 -->
          <div v-else class="message-row candidate-row"
               :class="{'fade-in': !isReplayingHistory}">
            <div class="avatar candidate-avatar">
              <img :src="userAvatar" alt="应聘者" onerror="this.src='https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
            </div>
            <div class="message candidate">
              {{ item.content }}
            </div>
          </div>
        </template>
        
        <!-- 面试官正在显示的当前问题 -->
        <div class="message-row interviewer-row" v-if="displayedText && !isReplayingHistory">
          <div class="avatar interviewer-avatar">
            <img src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" alt="面试官" />
          </div>
          <div class="message interviewer">
            {{ displayedText }}
          </div>
        </div>
        
        <!-- 面试官加载中效果 -->
        <div class="message-row interviewer-row" v-if="isTyping">
          <div class="avatar interviewer-avatar">
            <img src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" alt="面试官" />
          </div>
          <div class="message interviewer typing">
            <span class="dot" v-for="n in 3" :key="n"></span>
          </div>
        </div>
      </div>

      <!-- 回答区域 -->
      <div class="input-area" v-if="!completed">
        <form @submit.prevent="handleSubmit" class="input-form">
        <div class="input-content">
          <!-- 答题倒计时 -->
          <div class="answer-timer" :class="{'disabled': showThinkingCountdown}">
            <div class="timer-label">答题时间：</div>
            <div class="timer-progress">
              <el-progress 
                :percentage="answerTimePercentage" 
                :stroke-width="8" 
                :show-text="false"
                :color="answerTimerColor">
              </el-progress>
            </div>
            <div class="timer-text" :class="{'timer-warning': answerTimer <= 60}">
              {{ formatTimer(answerTimer) }}
            </div>
          </div>

          <!-- 语音输入控制区 -->
          <div class="voice-control" :class="{'disabled': showThinkingCountdown && !recording}">
            <el-button type="primary" :icon="Microphone" :disabled="showThinkingCountdown || recording" @click="startVoice" class="voice-button">
              {{ recording ? '录音中...' : '语音输入' }}
            </el-button>
            <el-button type="danger" :icon="VideoPause" :disabled="!recording" @click="stopVoice">结束录音</el-button>
            <span v-if="recording" class="recording-indicator">
              <span class="pulse-dot"></span> 录音中
            </span>
            

          </div>
          
          <el-input
            v-model="answer"
            type="textarea"
            :rows="4"
            :maxlength="2000"
            show-word-limit
            resize="none"
            :disabled="showThinkingCountdown"
            placeholder="请输入您的回答，或使用语音输入..."
            @keydown.enter.ctrl="handleSubmit"
          />
          
          <div class="actions" :class="{'disabled': showThinkingCountdown}">
            <el-button plain @click="answer = ''" :disabled="showThinkingCountdown || !answer || isSending">清空</el-button>
            <el-button type="primary" :loading="isSending" :disabled="showThinkingCountdown" @click="handleSubmit">
              {{ currentIndex < questions.length - 1 ? '提交回答' : '提交并完成' }}
            </el-button>
          </div>

          <!-- 30秒思考倒计时遮罩层 -->
          <div v-if="showThinkingCountdown" class="thinking-overlay">
            <div class="thinking-content">
              <div class="countdown-circle">
                <svg width="120" height="120" viewBox="0 0 120 120">
                  <circle cx="60" cy="60" r="54" fill="none" stroke="#e0e0e0" stroke-width="4" />
                  <circle cx="60" cy="60" r="54" fill="none" stroke="#409EFF" stroke-width="4"
                    stroke-dasharray="339.292" :stroke-dashoffset="thinkingCircleOffset" />
                </svg>
                <div class="countdown-number">{{ thinkingCountdown }}</div>
              </div>
              <div class="thinking-title">思考时间</div>
              <div class="thinking-desc">请仔细思考问题，倒计时结束后将自动开始录音</div>
            </div>
          </div>
        </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick, onBeforeUnmount, shallowRef } from 'vue';
import { startScenario, fetchQuestions, submitAnswer, generateFollowUp, completeStage } from '@/api/scenario';
import { ElMessage } from 'element-plus';
import { fetchTtsAudio } from '@/api/tts';
// 实时语音转写
import { connectScenarioSpeech } from '@/api/scenarioSpeech';
// 导入所需图标
import { InfoFilled, Microphone, VideoPause, ChatDotRound } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/user';

// 原生防抖函数实现
function debounce(func, wait) {
  let timeout;
  const debounced = function (...args) {
    const later = () => {
      timeout = null;
      func.apply(this, args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
  
  debounced.cancel = function () {
    clearTimeout(timeout);
    timeout = null;
  };
  
  return debounced;
}

const props = defineProps({
  sessionId: { type: Number, required: true },
});

// 发出事件
const emit = defineEmits(['stage-complete']);

const userStore = useUserStore();
const userAvatar = computed(() => userStore.userAvatar || '');

const loading = ref(true);
const questions = ref([]);
const currentIndex = ref(0);
const answer = ref('');
const completed = ref(false);
const audioUrl = ref('');
let audioPlayer = null;
const displayedText = ref('');
let typingTimer = null;
const chatContainer = ref(null);
const isSending = ref(false);
const isTyping = ref(false);
const scenarioContext = ref(''); // 场景背景描述
const isGeneratingFollowUp = ref(false); // 是否正在生成追问
const chatMessages = shallowRef([]); // 聊天消息流（使用shallowRef优化性能）
const isReplayingHistory = ref(false); // 是否正在重播历史

// 思考倒计时相关状态
const showThinkingCountdown = ref(false); // 是否显示思考倒计时
const thinkingCountdown = ref(30);        // 默认30秒思考时间
const isThinkingActive = ref(false);      // 思考倒计时是否已激活（显示但不一定在倒计时）
const thinkingCircleOffset = computed(() => {
  return 339.292 * (1 - thinkingCountdown.value / 30);
});
let thinkingTimer = null;

// 回答时间控制
const answerTimer = ref(300); // 5分钟 = 300秒
const answerTimePercentage = computed(() => {
  return (1 - answerTimer.value / 300) * 100;
});
const answerTimerColor = computed(() => {
  if (answerTimer.value <= 60) return '#F56C6C'; // 剩余1分钟内为红色
  if (answerTimer.value <= 120) return '#E6A23C'; // 剩余2分钟内为橙色
  return '#67C23A'; // 其他时间为绿色
});
let answerInterval = null;

// 语音录制相关状态
const recording = ref(false);
const voiceTimer = ref(300); // 5分钟 = 300秒
let voiceInterval = null;
let ws = null;
let audioContext = null;
let scriptNode = null;
let mediaStream = null;
// 保存每次开始录音时的基准内容，用于追加新的转写结果
let baseContent = '';

// 格式化时间为分:秒格式
function formatTimer(seconds) {
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${minutes}:${secs.toString().padStart(2, '0')}`;
}

// 显示思考倒计时遮罩，但不开始计时
function prepareThinkingCountdown() {
  showThinkingCountdown.value = true;
  isThinkingActive.value = true;
  thinkingCountdown.value = 30;
}

// 开始思考倒计时
function startThinkingCountdown() {
  if (!isThinkingActive.value) return;
  
  if (thinkingTimer) clearInterval(thinkingTimer);
  thinkingTimer = setInterval(() => {
    thinkingCountdown.value--;
    if (thinkingCountdown.value <= 0) {
      clearInterval(thinkingTimer);
      endThinkingCountdown();
    }
  }, 1000);
}

// 结束思考倒计时，自动开始语音输入
function endThinkingCountdown() {
  showThinkingCountdown.value = false;
  isThinkingActive.value = false;
  startAnswerTimer();
  nextTick(() => {
    startVoice(); // 自动开始语音输入
  });
}

// 开始答题计时
function startAnswerTimer() {
  answerTimer.value = 300; // 重置为5分钟
  
  if (answerInterval) clearInterval(answerInterval);
  answerInterval = setInterval(() => {
    answerTimer.value--;
    if (answerTimer.value <= 0) {
      clearInterval(answerInterval);
      handleTimeUp();
    }
  }, 1000);
}

// 答题时间用尽处理
function handleTimeUp() {
  if (answer.value.trim()) {
    console.log('答题时间已到，系统将自动提交您的回答');
    handleSubmit();
  } else {
    console.log('答题时间已到，系统将自动进入下一题');
    moveToNextQuestion();
  }
}

// 移动到下一题
function moveToNextQuestion() {
  stopVoice();
  if (answerInterval) clearInterval(answerInterval);
  
  // 如果有当前显示的问题文本，添加到聊天记录中
  if (displayedText.value) {
    chatMessages.value.push({
      role: 'interviewer',
      content: displayedText.value,
      isFollowUp: displayedText.isFollowUp === true,
      roundNo: currentQuestion.value?.roundNo
    });
    displayedText.value = '';
    displayedText.isFollowUp = false;
  }
  
  // 清空输入框和重置基准内容，为新问题做准备
  answer.value = '';
  baseContent = '';
  
  if (currentIndex.value < questions.value.length - 1) {
    isTyping.value = true;
    currentIndex.value += 1;
  } else {
    // 在完成前检查并标记所有问题为已完成
    try {
      completeStage(props.sessionId);
    } catch (e) {
      console.error('标记完成失败', e);
    }
    completed.value = true;
    emit('stage-complete');
  }
}

function startVoice() {
  if (recording.value) return;
  if (!currentQuestion.value || !currentQuestion.value.roundNo) {
    console.error('请先获取到题目后再开始语音');
    return;
  }
  recording.value = true;

  // 清空输入框，确保每次语音录制都从空白开始
  // 这样可以避免重复填充历史文本的问题
  answer.value = '';
  baseContent = '';

  // 建立 WebSocket，sessionId 格式: scenarioId-questionNo
  const questionNo = currentQuestion.value.roundNo;
  const sessionKey = `${props.sessionId}-${questionNo}`;
  ws = connectScenarioSpeech(props.sessionId, questionNo, handleSubtitle, (err) => {
    console.error(err);
    stopVoice();
  });

  ws.onopen = () => {
    // 取得麦克风音频
    navigator.mediaDevices.getUserMedia({ audio: true }).then((stream) => {
      mediaStream = stream;
      audioContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 });
      const source = audioContext.createMediaStreamSource(stream);
      // 缓冲区 1024，提升实时性
      scriptNode = audioContext.createScriptProcessor(1024, 1, 1);
      scriptNode.onaudioprocess = (e) => {
        const input = e.inputBuffer.getChannelData(0);
        const buffer = new ArrayBuffer(input.length * 2);
        const view = new DataView(buffer);
        for (let i = 0; i < input.length; i++) {
          let s = Math.max(-1, Math.min(1, input[i]));
          view.setInt16(i * 2, s < 0 ? s * 0x8000 : s * 0x7fff, true);
        }
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.send(JSON.stringify({ type: 'audio', sessionId: sessionKey, audioData: btoa(String.fromCharCode(...new Uint8Array(buffer))) }));
        }
      };
      source.connect(scriptNode);
      scriptNode.connect(audioContext.destination);
    }).catch((err) => {
      console.error('麦克风权限获取失败: ' + err.message);
      stopVoice();
    });
  };
}

// 优化语音转写更新，使用防抖减少频繁DOM操作
const debouncedScrollToBottom = debounce(() => {
  scrollToBottom();
}, 100);

function handleSubtitle(text) {
  if (!text) return;
  
  // 后端返回的是本次录音会话的完整累积转写结果
  // 由于我们在开始录音时已经清空了输入框，直接使用转写结果即可
  answer.value = text;
  
  // 使用防抖优化滚动性能
  debouncedScrollToBottom();
}

function stopVoice() {
  if (!recording.value) return;
  recording.value = false;

  if (ws && ws.readyState === WebSocket.OPEN) {
    const questionNo = currentQuestion.value.roundNo;
    const sessionKey = `${props.sessionId}-${questionNo}`;
    ws.send(JSON.stringify({ type: 'end', sessionId: sessionKey }));
    ws.close();
  }
  if (audioContext) {
    audioContext.close();
    audioContext = null;
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach((t) => t.stop());
    mediaStream = null;
  }
  // prevPartial 相关代码已移除，不再需要重置
}

// 优化定时器管理和内存清理
function clearTimers() {
  if (thinkingTimer) {
    clearInterval(thinkingTimer);
    thinkingTimer = null;
  }
  if (answerInterval) {
    clearInterval(answerInterval);
    answerInterval = null;
  }
  if (typingTimer) {
    clearInterval(typingTimer);
    typingTimer = null;
  }
  if (voiceInterval) {
    clearInterval(voiceInterval);
    voiceInterval = null;
  }
  
  // 清理语音相关资源
  stopVoice();
  
  // 清理音频播放器
  if (audioPlayer) {
    audioPlayer.pause();
    audioPlayer = null;
  }
  
  // 清理音频URL
  if (audioUrl.value) {
    URL.revokeObjectURL(audioUrl.value);
    audioUrl.value = '';
  }
  
  // 取消防抖函数
  if (debouncedScrollToBottom.cancel) {
    debouncedScrollToBottom.cancel();
  }
}

// 组件销毁时的清理工作
onBeforeUnmount(() => {
  clearTimers();
  
  // 重置状态
  recording.value = false;
  isTyping.value = false;
  showThinkingCountdown.value = false;
  isThinkingActive.value = false;
  
  // 清理历史数据
  baseContent = '';
  
  console.log('[ScenarioStage] 组件已清理');
});

// 在用户提交回答或跳过后也停止录音
watch(currentIndex, () => {
  stopVoice();
  // 切换问题时重置基准内容，确保新问题从空白开始
  baseContent = '';
});

const currentQuestion = computed(() => questions.value[currentIndex.value] || {});

const initStage = async () => {
  try {
    const startList = await startScenario(props.sessionId);

    if (Array.isArray(startList) && startList.length) {
      questions.value = startList;
      // 获取场景背景
      if (startList[0] && startList[0].scenarioContext) {
        scenarioContext.value = startList[0].scenarioContext;
      }
    } else {
      // 若首次接口失败或已生成过题目，则重新拉取列表
      const list = await fetchQuestions(props.sessionId);
      questions.value = Array.isArray(list) ? list : [];
      // 尝试获取场景背景
      if (list && list[0] && list[0].scenarioContext) {
        scenarioContext.value = list[0].scenarioContext;
      }
    }
    
    // 初始化聊天消息流
    isReplayingHistory.value = true;
    updateChatMessages();
    isReplayingHistory.value = false;
  } catch (e) {
    console.error('初始化场景问答失败');
  } finally {
    loading.value = false;
  }
};

// 解析问题文本，将问题和追问分开，并添加标记以便更好地区分
const parseQuestionParts = (questionText) => {
  if (!questionText) return [''];
  
  const parts = questionText.split('\n\n追问：');
  return parts.map((part, index) => {
    if (index === 0) {
      // 原始问题
      return part.trim();
    } else {
      // 追问需添加标记
      return `${part.trim()}`;
    }
  });
};

// 解析回答文本，将多次回答分开，不再添加序号标记
const parseAnswerParts = (answerText) => {
  if (!answerText) return [''];
  
  if (answerText.includes('\n\n应聘者回答：')) {
    const parts = answerText.split('\n\n应聘者回答：');
    return parts.map(part => part.trim());
  }
  
  return [answerText];
};

// 优化聊天消息更新，减少不必要的重新渲染
const updateChatMessages = () => {
  const newMessages = [];
  
  // 处理所有已完成的问答(除了当前问题)
  for (let i = 0; i < questions.value.length; i++) {
    const qa = questions.value[i];
    
    // 跳过未开始的问题
    if (i > currentIndex.value) continue;
    
    // 跳过当前问题(当前正在显示的问题由displayedText负责显示)
    if (i === currentIndex.value && !qa.answer) continue;
    
    // 添加问题（原始问题和所有追问）
    const questionParts = parseQuestionParts(qa.question);
    questionParts.forEach((part, partIndex) => {
      newMessages.push({
        role: 'interviewer',
        content: part,
        isFollowUp: partIndex > 0,
        roundNo: qa.roundNo,
        id: `q-${qa.roundNo}-${partIndex}` // 添加唯一ID优化渲染
      });
      
      // 如果有对应的回答，添加回答
      if (qa.answer) {
        const answerParts = parseAnswerParts(qa.answer);
        if (partIndex < answerParts.length) {
          newMessages.push({
            role: 'candidate',
            content: answerParts[partIndex],
            roundNo: qa.roundNo,
            id: `a-${qa.roundNo}-${partIndex}` // 添加唯一ID优化渲染
          });
        }
      }
    });
  }
  
  // 只有当消息确实发生变化时才更新
  if (JSON.stringify(newMessages) !== JSON.stringify(chatMessages.value)) {
    chatMessages.value = newMessages;
  }
};

// 生成并显示追问
const handleFollowUp = async () => {
  if (isGeneratingFollowUp.value || !currentQuestion.value || !currentQuestion.value.answer) return;
  
  // 先检查并添加当前显示的问题到聊天记录
  if (displayedText.value) {
    chatMessages.value.push({
      role: 'interviewer',
      content: displayedText.value,
      isFollowUp: displayedText.isFollowUp === true,
      roundNo: currentQuestion.value.roundNo
    });
    displayedText.value = '';
    displayedText.isFollowUp = false;
  }
  
  isGeneratingFollowUp.value = true;
  isTyping.value = true;
  
  console.log('系统正在评估回答并生成追问...');
  
  try {
    const response = await generateFollowUp(props.sessionId, currentQuestion.value.roundNo);
    
    // 检查是否得到了有效的追问
    if (!response || !response.question || response.question === currentQuestion.value.question) {
      // 如果没有新追问生成（回答质量不足或无需深入），直接进入下一题
      console.log('系统已完成当前方向评估，进入下一个问题');
      moveToNextQuestion();
      return;
    }
    
    // 更新本地问题以展示追问
    const updatedQuestion = questions.value.find(q => q.roundNo === currentQuestion.value.roundNo);
    if (updatedQuestion) {
      updatedQuestion.question = response.question;
      
      // 对于当前问题，播放TTS并显示追问
      if (currentQuestion.value.roundNo === questions.value[currentIndex.value]?.roundNo) {
        // 获取最后一个追问
        const questionParts = parseQuestionParts(response.question);
        const latestFollowUp = questionParts[questionParts.length - 1];
        
        // 不立即添加到聊天记录，而是先通过loadTts进行逐字显示
        // 在追问之前添加一个短暂的停顿，提高用户体验
        setTimeout(() => {
          // 播放追问TTS并逐字显示，标记为追问
          loadTts({ 
            question: latestFollowUp,
            isFollowUp: true  // 标记为追问
          });
        }, 500);
      }
    }
    
    scrollToBottom();
  } catch (e) {
    console.error('生成追问失败', e);
    console.error('生成追问失败，将进入下一题');
    moveToNextQuestion();
  } finally {
    isGeneratingFollowUp.value = false;
    isTyping.value = false;
  }
};

// 检查是否应该继续追问还是进入下一题
const shouldContinueWithFollowUp = (questionRoundNo) => {
  // 获取该问题的完整文本
  const question = questions.value.find(q => q.roundNo === questionRoundNo);
  if (!question) return false;
  
  // 统计已有追问次数
  const followUpCount = (question.question.match(/\n\n追问：/g) || []).length;
  
  // 根据问题维度设置不同的追问次数限制
  const maxFollowUps = getMaxFollowUpsByDimension(question.dimension);
  
  return followUpCount < maxFollowUps;
};

// 根据维度获取最大追问次数
const getMaxFollowUpsByDimension = (dimension) => {
  // 这些维度最多只能追问1次
  const singleFollowUpDimensions = [
    '团队协作能力',
    '职业发展规划', 
    '工作态度与价值观'
  ];
  
  // 如果是限制追问的维度，返回1，否则返回2
  return singleFollowUpDimensions.includes(dimension) ? 1 : 2;
};

const handleSubmit = async (event) => {
  // 防止任何默认行为，确保不会触发页面刷新
  if (event) {
    event.preventDefault();
    event.stopPropagation();
  }
  
  if (isSending.value) return;
  if (!answer.value.trim()) {
    console.error('请先输入回答');
    return;
  }
  const userAns = answer.value.trim();
  isSending.value = true;
  try {
    // 首先，如果有逐字显示的问题，添加到聊天记录
    if (displayedText.value) {
      // 添加当前问题到聊天消息流，并正确处理追问状态
      chatMessages.value.push({
        role: 'interviewer',
        content: displayedText.value,
        isFollowUp: displayedText.isFollowUp === true, // 使用保存的追问标记
        roundNo: currentQuestion.value.roundNo
      });
      // 清空显示文本，以免被重复添加
      displayedText.value = '';
      displayedText.isFollowUp = false;
    }
    
    const response = await submitAnswer(props.sessionId, currentQuestion.value.roundNo, userAns);

    // 更新本地数据
    questions.value[currentIndex.value].answer = response.answer;
    questions.value[currentIndex.value].answerTime = Date.now(); // 记录回答时间
    
    // 添加用户回答到聊天消息流
    chatMessages.value.push({
      role: 'candidate',
      content: userAns,
      roundNo: currentQuestion.value.roundNo
    });

    answer.value = '';
    // 重置基准内容，确保下次录音从空白开始
    baseContent = '';
    
    // 强制滚动到最新消息
    scrollToBottom(true);
    
    // 判断是否是最后一题
    if (currentIndex.value >= questions.value.length - 1) {
      moveToNextQuestion();
    } else {
      // 判断是否需要继续追问当前方向
      if (shouldContinueWithFollowUp(currentQuestion.value.roundNo)) {
        // 自动生成追问
        await handleFollowUp();
      } else {
        // 已达到追问上限，移动到下一题
        moveToNextQuestion();
      }
    }
  } catch (e) {
    console.error('提交失败');
  }
  finally {
    isSending.value = false;
  }
};

const loadTts = async (q) => {
  if (!q || !q.question) return;
  try {
    // 在新题目加载时准备思考倒计时（显示但不开始计时）
    prepareThinkingCountdown();
    
    // 记录问题显示时间
    if (questions.value[currentIndex.value]) {
      questions.value[currentIndex.value].questionTime = Date.now();
    }
    
    // 判断是否是追问
    const isFollowUp = q.isFollowUp === true;
    
    const arrayBuffer = await fetchTtsAudio(q.question);
    const blob = new Blob([arrayBuffer], { type: 'audio/mpeg' });
    if (audioPlayer) {
      audioPlayer.pause();
      URL.revokeObjectURL(audioUrl.value);
    }
    audioUrl.value = URL.createObjectURL(blob);
    audioPlayer = new Audio(audioUrl.value);
    audioPlayer.onloadedmetadata = () => {
      const dur = isFinite(audioPlayer.duration) ? audioPlayer.duration : 2; // fallback 2s
      // 传入追问标记，以便在逐字效果完成后正确处理
      startTypingEffect(q.question, dur, isFollowUp);
      isTyping.value = false;
    };
    
    audioPlayer.onended = () => {
      // 播放结束后，开始思考倒计时
      startThinkingCountdown();
    };
    
    await audioPlayer.play().catch(() => {
      // 如果自动播放失败，也启动思考倒计时
      startThinkingCountdown();
    });
    scrollToBottom(true);
  } catch (err) {
    console.error('TTS 播放失败', err);
    // 如果TTS失败，也启动思考倒计时
    startThinkingCountdown();
  }
};

const startTypingEffect = (text, durationSec, isFollowUp = false) => {
  displayedText.value = '';
  if (!text) return;
  // 计算每个字符间隔，最小 20ms，防止过快
  const interval = Math.max((durationSec * 1000) / text.length, 20);
  let idx = 0;
  if (typingTimer) clearInterval(typingTimer);
  typingTimer = setInterval(() => {
    displayedText.value += text[idx++];
    if (idx >= text.length) {
      clearInterval(typingTimer);
      // 注意：逐字效果结束后，不立即清除displayedText，保持显示
      // 也不立即添加到chatMessages，而是等到回答提交后或切换问题时添加
      
      // 保存当前显示文本是否为追问的状态，供后续处理使用
      displayedText.isFollowUp = isFollowUp;
      
      // 如果没有音频或音频播放失败，开始思考倒计时
      if (!audioPlayer || audioPlayer.error) {
        startThinkingCountdown();
      }
    }
    // 每次更新都强制滚动到底部
    scrollToBottom(true);
  }, interval);
};

// 优化滚动行为 - 面试场景下总是滚动到最新消息
const scrollToBottom = (force = false) => {
  nextTick(() => {
    if (chatContainer.value) {
      const container = chatContainer.value;
      const isAtBottom = container.scrollTop + container.clientHeight >= container.scrollHeight - 10;
      
      // 面试场景下，新消息总是自动滚动到底部，确保最新内容可见
      if (force || isAtBottom || recording.value || isTyping.value || displayedText.value) {
        container.scrollTo({
          top: container.scrollHeight,
          behavior: 'smooth'
        });
      }
    }
  });
};

watch(currentQuestion, (q) => {
  clearTimers(); // 清理之前的所有计时器
  loadTts(q);
  // 更新聊天消息
  updateChatMessages();
}, { immediate: true });

// 优化watch监听器，减少不必要的更新
watch([currentIndex, () => questions.value], () => {
  updateChatMessages();
  scrollToBottom(true);
}, { deep: false });

// 单独监听answer变化，使用防抖优化
watch(() => answer.value, debounce(() => {
  // 只在必要时触发滚动
  if (recording.value) {
    scrollToBottom(true);
  }
}, 300));

// 监听聊天消息变化，自动滚动到底部
watch(chatMessages, () => {
  scrollToBottom(true);
}, { deep: true });

onMounted(initStage);
</script>

<style scoped>
.scenario-stage { 
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.loading-container {
  padding: 40px;
  text-align: center;
}

.loading-text {
  margin-top: 16px;
  color: #909399;
}

.completion-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

/* 主容器 - 简化为单一容器 */
.main-container {
  background: #fff;
  border-radius: 8px;
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  position: relative; /* 为思考倒计时遮罩层提供定位 */
}

/* 思考倒计时遮罩层 - 只覆盖输入区域 */
.thinking-overlay {
  position: absolute;
  top: auto;
  left: 0;
  right: 0;
  bottom: 0;
  height: auto;
  min-height: 240px;
  background-color: #f9f9f9;
  border-top: 1px solid #ebeef5;
  z-index: 10;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 20px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
  border-radius: 0 0 8px 8px;
}

.thinking-content {
  text-align: center;
}

.countdown-circle {
  position: relative;
  margin: 0 auto;
  width: 120px;
  height: 120px;
}

.countdown-number {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  font-weight: bold;
  color: #409EFF;
}

.thinking-title {
  font-size: 20px;
  margin: 16px 0 8px;
  font-weight: bold;
  color: #409EFF;
}

.thinking-desc {
  color: #606266;
  font-size: 14px;
  max-width: 300px;
}

/* 场景背景 - 简化样式 */
.scenario-context {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.context-header {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.context-header .el-icon {
  color: #409EFF;
  margin-right: 6px;
}

.context-text {
  color: #606266;
  font-weight: normal;
}

.chat-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.chat-progress {
  font-size: 14px;
  color: #606266;
  background: #f2f6fc;
  padding: 2px 8px;
  border-radius: 12px;
}

/* 对话区 - 修改为固定高度 */
.chat-container {
  height: 400px; /* 固定高度 */
  overflow-y: auto;
  padding: 0 8px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.message-row {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.interviewer-row {
  flex-direction: row-reverse;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.interviewer-avatar {
  margin-left: 8px;
}

.candidate-avatar {
  margin-right: 8px;
}

/* 消息气泡 */
.message {
  line-height: 1.6;
  word-break: break-word;
  position: relative;
  padding: 10px 14px;
  max-width: 70%;
}

.interviewer {
  background-color: #e1f3ff;
  color: #333;
  border-radius: 10px 2px 10px 10px;
}

.interviewer .follow-up-question {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #a0cfff;
  color: #1e5eaa;
  font-weight: 500;
  position: relative;
}

/* 追问和回答标记的样式 */
.message [class*="【追问"] {
  color: #1e5eaa;
  font-weight: 500;
  padding-left: 5px;
  border-left: 3px solid #409EFF;
}

.message [class*="【回答追问"] {
  color: #2c9678;
  font-weight: 500;
  padding-left: 5px;
  border-left: 3px solid #67C23A;
}

.candidate {
  background-color: #f0fff0;
  color: #333;
  border-radius: 2px 10px 10px 10px;
  margin-right: auto;
  border-left: 2px solid #67c23a;
}

/* 答题计时器 */
.answer-timer {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 12px;
  background-color: #f8f8f8;
  border-radius: 4px;
}

.timer-label {
  margin-right: 8px;
  font-weight: bold;
  color: #606266;
}

.timer-progress {
  flex: 1;
  margin-right: 12px;
}

.timer-text {
  font-family: monospace;
  font-size: 16px;
  color: #67C23A;
}

.timer-warning {
  color: #F56C6C;
  font-weight: bold;
}

/* 输入区域 */
.input-area {
  border-top: 1px solid #f0f0f0;
  padding-top: 16px;
}

.input-form {
  width: 100%;
  margin: 0;
  padding: 0;
}

.input-content {
  position: relative;
  padding-top: 0;
}

/* 禁用状态 */
.disabled {
  pointer-events: none;
  opacity: 0.6;
}

/* 思考倒计时遮罩层 - 精确匹配截图样式 */
.thinking-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(249, 249, 249, 0.95);
  z-index: 10;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 20px;
  border-radius: 4px;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.05);
}

.voice-control {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.recording-indicator {
  display: flex;
  align-items: center;
  margin-left: 8px;
  color: #F56C6C;
  font-size: 14px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background-color: #F56C6C;
  border-radius: 50%;
  margin-right: 8px;
  display: inline-block;
  animation: pulse 1s infinite;
}

.actions {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
}

/* 打字动画 */
.typing {
  padding: 8px 12px;
  display: inline-flex;
  align-items: center;
  min-width: 40px;
}

.typing .dot {
  width: 6px;
  height: 6px;
  margin: 0 2px;
  border-radius: 50%;
  background: #8ac3ff;
  animation: blink 1.2s infinite ease-in-out;
}

.typing .dot:nth-child(2) { animation-delay: 0.2s; }
.typing .dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes pulse {
  0% { transform: scale(0.8); opacity: 0.6; }
  50% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(0.8); opacity: 0.6; }
}

@keyframes blink { 
  0%, 80%, 100% { transform: scale(0); opacity: 0.4; } 
  40% { transform: scale(1); opacity: 1; } 
}

/* 添加平滑的进入动画 */
.fade-in {
  animation: fadeIn 0.5s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 优化滚动性能 */
.chat-container {
  will-change: scroll-position;
  scrollbar-width: thin;
  scrollbar-color: rgba(144, 147, 153, 0.3) transparent;
}

.chat-container::-webkit-scrollbar {
  width: 6px;
}

.chat-container::-webkit-scrollbar-track {
  background: transparent;
}

.chat-container::-webkit-scrollbar-thumb {
  background-color: rgba(144, 147, 153, 0.3);
  border-radius: 3px;
  transition: background-color 0.3s ease;
}

.chat-container::-webkit-scrollbar-thumb:hover {
  background-color: rgba(144, 147, 153, 0.5);
}

/* 平滑的消息过渡 */
.message {
  transition: all 0.3s ease;
}

.message:hover {
  transform: translateX(2px);
}

/* 平滑的输入区域过渡 */
.input-area {
  transition: all 0.3s ease;
}

.disabled {
  transition: all 0.3s ease;
}

/* 优化思考倒计时动画 */
.thinking-overlay {
  animation: fadeInOverlay 0.3s ease-in-out;
}

@keyframes fadeInOverlay {
  from {
    opacity: 0;
    backdrop-filter: blur(0px);
  }
  to {
    opacity: 1;
    backdrop-filter: blur(2px);
  }
}

/* 优化进度条动画 */
.el-progress {
  transition: all 0.3s ease;
}

/* 平滑的按钮状态过渡 */
.el-button {
  transition: all 0.3s ease !important;
}

/* 语音录制指示器动画优化 */
.recording-indicator {
  animation: slideIn 0.3s ease-in-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@media (max-width: 768px) {
  .scenario-stage {
    padding: 8px;
  }
  
  .message {
    max-width: 85%;
  }
  
  .avatar {
    width: 28px;
    height: 28px;
  }
  
  /* 移动端聊天框高度调整 */
  .chat-container {
    height: 300px;
  }
}
</style>