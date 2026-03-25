<template>
  <div class="dashboard-main">
    <div class="dashboard-content-wrap">
      <!-- 顶部横幅区 -->
      <div class="dashboard-banner">
        <div class="banner-left">
          <div class="banner-avatar-container">
            <el-avatar
                v-if="userInfo.avatarUrl"
                :size="72"
                :src="userInfo.avatarUrl"
                @error="avatarLoadError"
                class="banner-avatar"
            />
            <DefaultAvatar
                v-else
                :username="displayName"
                :size="72"
                class="banner-avatar"
            />
          </div>
          <div class="banner-info">
            <div class="banner-welcome">欢迎回来，{{ displayName }}！</div>
            <div class="banner-progress">
              <span>简历完善度</span>
              <div class="progress-bar-row">
                <el-progress :percentage="resumePercent" status="success" :show-text="false" style="width: 120px; display: inline-block; margin-left: 12px;" />
                <span class="progress-percent">{{ resumePercent }}%</span>
              </div>
              <el-button type="primary" size="small" class="banner-btn" @click="goToResume">去完善简历</el-button>
            </div>
          </div>
        </div>
        <div class="banner-stats">
          <el-card
              class="stat-card"
              v-for="(stat, index) in statsArr"
              :key="stat.label"
              :style="{ '--index': index }"
              shadow="never"
          >
            <div class="stat-num">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </el-card>
        </div>
        <div class="banner-actions">
          <el-button type="primary" size="default" circle @click="refreshStatistics" title="刷新统计数据">
            <el-icon><Refresh /></el-icon>
          </el-button>
          <el-button type="danger" size="default" circle @click="clearStatistics" title="清空统计数据">
            <el-icon><Delete /></el-icon>
          </el-button>
          <!-- 语音控制按钮 -->
          <el-button
              v-if="isPlayingVoice"
              @click="VoiceGuideService.stopCurrentVoice()"
              type="warning"
              size="default"
              circle
              title="停止语音指引"
          >
            <el-icon><VideoPause /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 主内容区：与顶部横幅区同宽 -->
      <div class="main-content">
        <!-- 中间居中：面试进度流程条 -->
        <div class="progress-center">
          <el-card class="progress-card">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
              <!-- 只在分析报告状态显示继续投递按钮，左对齐 -->
              <div v-if="interviewStore.job && interviewStore.status === 'report'">
                <el-button 
                  type="success" 
                  size="large"
                  @click="continueApplying" 
                  style="font-size: 16px; font-weight: 700; padding: 12px 32px; border-radius: 25px; background: linear-gradient(45deg, #10b981, #34d399); border: none; box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4); transition: all 0.3s ease;">
                  继续投递
                </el-button>
              </div>
              <div v-else></div>
              <el-button v-if="!pendingJob" type="primary" @click="goToApplyJob">去选择岗位</el-button>
            </div>
            <!-- 新增：当前面试岗位/公司信息卡片 -->
            <div v-if="pendingJob" 
                 class="current-job-info" 
                 :style="getJobInfoCardStyle(interviewStore.status)"
                 style="display: flex; align-items: center; border-radius: 12px; padding: 12px 24px; margin-bottom: 18px; font-size: 16px; box-shadow: 0 1px 6px #e0e0e0; gap: 8px; justify-content: flex-start;">
              <el-icon class="current-job-icon"><Calendar /></el-icon>
              <span class="current-job-label">{{ getJobInfoLabel(interviewStore.status) }}：</span>
              <span class="current-job-value">{{ pendingJob?.title || '岗位信息缺失' }}</span>
              <span class="current-job-label">公司：</span>
              <span class="current-job-value">{{ pendingJob?.companyName || '公司信息缺失' }}</span>
              
              <!-- 状态提示 -->
              <el-tag 
                v-if="interviewStore.status === 'report'" 
                type="success" 
                effect="dark"
                style="margin-left: 12px;">
                已完成面试
              </el-tag>
              
              <div style="margin-left: auto; display: flex; gap: 12px; align-items: center; flex-wrap: wrap;">
                <!-- 待面试状态显示进入面试按钮 -->
                <el-button
                    v-if="interviewStore && (interviewStore.status === 'pending' || interviewStore.status === 'pending1' || interviewStore.status === 'pending2')"
                    type="primary"
                    size="large"
                    style="font-size: 16px; border-radius: 20px; padding: 0 28px; height: 40px;"
                    @click="goToInterview"
                    :loading="false"
                >进入面试</el-button>

                <el-button
                    v-if="interviewStore && interviewStore.status === 'report'"
                    type="success"
                    size="large"
                    style="font-size: 16px; border-radius: 20px; padding: 0 28px; height: 40px;"
                    @click="goToAnalysisReport"
                >查看分析报告</el-button>
              </div>
            </div>
            <div class="progress-header">
              <span>面试进度</span>
            </div>
            <el-steps 
              :active="currentStep" 
              align-center 
              finish-status="success" 
              status="success"
              class="progress-steps-horizontal">
              <el-step 
                v-for="(step, idx) in steps" 
                :key="step.title" 
                :title="step.title" 
                :icon="step.icon"
                :status="getStepStatus(idx)">
                <template #description>
                  <div :class="currentStep === idx ? 'step-desc-active' : 'step-desc'">
                    <template v-if="pendingJob">
                      {{ step.desc }}（{{ pendingJob?.title || '岗位信息缺失' }} - {{ pendingJob?.companyName || '公司信息缺失' }}）
                    </template>
                    <template v-else>
                      {{ step.desc }}
                    </template>
                  </div>
                </template>
              </el-step>
            </el-steps>
          </el-card>
        </div>
        <div class="section-gap"></div>
        <!-- 下方横向三栏：日历提醒、历史面试、小贴士 -->
        <div class="dashboard-bottom-row">
          <!-- 面试日历提醒 -->
          <el-card class="remind-card" :style="{ '--card-index': 0 }" shadow="hover">
            <div class="remind-title"><el-icon><Calendar /></el-icon> 即将面试提醒</div>
            <div v-if="pendingJob">
              <div class="remind-job">岗位：{{ pendingJob?.title || '岗位信息缺失' }}</div>
              <div class="remind-company">公司：{{ pendingJob?.companyName || '公司信息缺失' }}</div>
              <div class="remind-info">您可以随时开始AI面试，点击"进入面试"按钮开始</div>
            </div>
            <div v-else style="color: #aaa;">暂无面试安排</div>
          </el-card>
          <!-- 简历投递记录（可展开/收起） -->
          <el-card class="history-card" :style="{ '--card-index': 1 }" shadow="hover">
            <div class="history-title"><el-icon><Document /></el-icon> 简历投递记录</div>
            <el-timeline v-if="historyList.length > 0">
              <el-timeline-item v-for="(item, idx) in showHistoryList" :key="item.id" :timestamp="item.time" :type="getStatusType(item.status)">
                <div class="history-job">{{ item.title }} - {{ item.company }}</div>
                <div class="history-status">状态：{{ item.status }}</div>
                <el-button v-if="item.status === '已完成'" type="info" size="small" @click="goToReport">查看报告</el-button>
              </el-timeline-item>
            </el-timeline>
            <div v-else class="empty-history">
              <el-empty description="暂无简历投递记录" :image-size="80">
                <el-button type="primary" @click="goToApplyJob">去投递简历</el-button>
              </el-empty>
            </div>
            <div v-if="historyList.length > 2" class="history-footer">
              <el-link type="primary" @click="toggleHistory">{{ showAllHistory ? '收起' : '查看更多投递记录' }}</el-link>
            </div>
          </el-card>
          <!-- 面试小贴士/公告 -->
          <el-card class="tips-card" :style="{ '--card-index': 2 }" shadow="hover">
            <div class="tips-header"><el-icon><User /></el-icon> 面试小贴士</div>
            <ul class="tips-list">
              <li>提前准备好面试所需材料和环境。</li>
              <li>保持良好仪表和自信心态。</li>
              <li>面试过程中注意聆听和表达。</li>
              <li>遇到不会的问题，坦诚沟通，展示学习能力。</li>
            </ul>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 新用户引导对话框 -->
    <el-dialog
        v-model="newUserDialogVisible"
        title="完善您的个人资料"
        width="420px"
        :close-on-click-modal="false"
        :show-close="false"
    >
      <div class="welcome-dialog-content">
        <el-icon class="welcome-icon"><User /></el-icon>
        <h2>开始您的求职之旅</h2>
        <p>为了为您提供更好的面试体验和岗位推荐，建议您先完善个人资料。</p>
        <p>完善资料可以帮助您：</p>
        <ul>
          <li>提高面试推荐匹配度</li>
          <li>生成更准确的简历</li>
          <li>个性化您的面试体验</li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="newUserDialogVisible = false">稍后再说</el-button>
        <el-button type="primary" @click="goToProfile">立即前往</el-button>
      </template>
    </el-dialog>

  </div>

  <WelcomeScreen
      :visible="showWelcomeScreen"
      :type="welcomeScreenType"
      :username="displayName"
      :is-playing-voice="isPlayingVoice"
      @continue="handleWelcomeScreenContinue"
      @close="closeWelcomeScreen"
  />
</template>

<script setup>
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue'
import { Upload, Calendar, User, DocumentChecked, DocumentAdd, DocumentRemove, Refresh, Delete, Document, VideoPause } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useInterviewStore } from '@/store/interview'
import axios from 'axios'
import request from '@/utils/request'
import auth from '@/utils/auth'
import DefaultAvatar from '@/components/DefaultAvatar.vue'
import WelcomeScreen from '@/components/WelcomeScreen.vue'
import { getInterviewStatistics, refreshInterviewStatistics, clearInterviewStatistics } from '@/api/statistics'
import { updateInterviewTime } from '@/api/application'
import { getUserApplications } from '@/api/application'
import { fetchTtsAudio } from '@/api/tts'

const router = useRouter()
const interviewStore = useInterviewStore()

// 默认头像
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iIzYzNjZmMSI+PHBhdGggZD0iTTEyIDJDNi40OCAyIDIgNi40OCAyIDEyczQuNDggMTAgMTAgMTAgMTAtNC40OCAxMC0xMFMxNy41MiAyIDEyIDJ6bTAgM2MyLjY3IDAgOC40IDEuMzQgOCA0djNjMCAyLjY2LTUuMzMgNC04IDRzLTgtMS4zNC04LTR2LTNjMC0yLjY2IDUuMzMtNCA4LTR6bTAgMTVjLTIuMzIgMC00LjQ1LS44LTYuMTQtMi4xMkM3LjQ5IDE2LjAyIDkuNTkgMTUgMTIgMTVzNC41MSAxLjAyIDYuMTQgMi44OEM1LjQ1IDE5LjIgMTQuMzIgMjAgMTIgMjB6bTAtN2MtMS4xMSAwLTItLjktMi0yIDAtMS4xMS44OS0yIDItMnMyIC44OSAyIDJjMCAxLjEtLjkgMi0yIDJ6Ii8+PC9zdmc+';

// 用户信息
const userInfo = ref({
  id: null,
  username: '',
  realName: '',
  avatarUrl: '',
  email: '',
  role: '',
  isNewUser: false // 标记是否是新用户
})

// 显示名称优先使用真实姓名
const displayName = computed(() => {
  return userInfo.value.realName || userInfo.value.username || '用户'
})

// 新用户引导对话框
const newUserDialogVisible = ref(false)

// 全屏欢迎界面状态
const showWelcomeScreen = ref(false)
const welcomeScreenType = ref('') // 'newUser' | 'returningUser'

// 语音指引相关状态
const voiceGuideEnabled = ref(true) // 是否启用语音指引
const currentAudio = ref(null) // 当前播放的音频对象
const isPlayingVoice = ref(false) // 是否正在播放语音

// 跳转到个人中心
const goToProfile = () => {
  router.push('/profile')
  newUserDialogVisible.value = false

  // 新用户点击前往完善资料时，标记已经引导过
  markVoiceGuideCompleted('newUserWelcome')
}

// 显示全屏欢迎界面
const showFullScreenWelcome = (type) => {
  welcomeScreenType.value = type
  showWelcomeScreen.value = true
  console.log(`显示${type === 'newUser' ? '新用户' : '老用户'}全屏欢迎界面`)
}

// 关闭全屏欢迎界面
const closeWelcomeScreen = () => {
  showWelcomeScreen.value = false
  welcomeScreenType.value = ''
}

// 处理欢迎界面继续按钮点击
const handleWelcomeScreenContinue = (type) => {
  console.log(`用户点击了${type === 'newUser' ? '新用户' : '老用户'}欢迎界面的继续按钮`)

  // 如果是新用户，在关闭欢迎界面后显示完善资料弹窗
  if (type === 'newUser') {
    setTimeout(() => {
      newUserDialogVisible.value = true
    }, 500) // 稍微延迟，让界面过渡更自然
  }
}

// 语音指引服务类
class VoiceGuideService {
  // 播放语音指引
  static async playVoiceGuide(text, onComplete = null) {
    if (!voiceGuideEnabled.value || isPlayingVoice.value) {
      console.log('语音指引已禁用或正在播放中')
      return Promise.resolve()
    }

    try {
      isPlayingVoice.value = true
      console.log('开始播放语音指引:', text)

      // 调用TTS API获取音频数据
      const audioData = await fetchTtsAudio(text)

      // 创建音频对象并播放
      const audioBlob = new Blob([audioData], { type: 'audio/mp3' })
      const audioUrl = URL.createObjectURL(audioBlob)
      const audio = new Audio(audioUrl)

      currentAudio.value = audio

      // 设置音频事件监听
      audio.onended = () => {
        isPlayingVoice.value = false
        currentAudio.value = null
        URL.revokeObjectURL(audioUrl) // 清理内存
        console.log('语音指引播放完成')
        if (onComplete) onComplete()
      }

      audio.onerror = (error) => {
        console.error('语音播放失败:', error)
        isPlayingVoice.value = false
        currentAudio.value = null
        URL.revokeObjectURL(audioUrl)
      }

      audio.play().catch(error => {
        console.error('音频播放启动失败:', error)
        isPlayingVoice.value = false
        currentAudio.value = null
        URL.revokeObjectURL(audioUrl)
      })

    } catch (error) {
      console.error('语音指引播放出错:', error)
      isPlayingVoice.value = false
      ElMessage.warning('语音指引播放失败，请检查网络连接')
    }
  }

  // 新用户欢迎语音指引
  static async playNewUserWelcome(username, onComplete = null) {
    const welcomeText = `欢迎您注册智能面试系统，${username}！为了让您获得更好的面试体验，建议您先完善个人资料和简历信息。这将帮助我们为您推荐更合适的岗位，并生成更准确的面试问题。让我们一起开始您的求职之旅吧！`

    return this.playVoiceGuide(welcomeText, () => {
      console.log('新用户欢迎语音指引播放完成')
      if (onComplete) onComplete()
    })
  }

  // 新用户完善资料后的指引
  static async playProfileCompleteGuide() {
    const guideText = `恭喜您，个人资料已完善！现在您可以返回首页，浏览并选择心仪的岗位进行投递。我们的AI系统将根据您的资料为您匹配合适的岗位，并在面试中提供智能化的问题和评估。祝您求职顺利！`

    return this.playVoiceGuide(guideText)
  }

  // 老用户欢迎回归语音指引
  static async playReturningUserWelcome(username, onComplete = null) {
    const welcomeBackText = `欢迎回来，${username}！很高兴再次见到您。您可以继续浏览最新的岗位信息，选择感兴趣的职位进行投递，或者查看之前的面试记录和分析报告。我们的AI面试系统已经为您准备就绪，祝您今天求职愉快！`

    return this.playVoiceGuide(welcomeBackText, () => {
      console.log('老用户欢迎语音指引播放完成')
      if (onComplete) onComplete()
    })
  }

  // 停止当前播放的语音
  static stopCurrentVoice() {
    if (currentAudio.value) {
      currentAudio.value.pause()
      currentAudio.value = null
      isPlayingVoice.value = false
      console.log('语音指引已停止')
    }
  }
}

// 检查是否是新的登录会话
const isNewLoginSession = () => {
  if (!userInfo.value.id) {
    console.log('isNewLoginSession: 用户ID不存在，返回false')
    return false
  }

  const loginSessionKey = 'currentLoginSession'
  const currentSession = sessionStorage.getItem(loginSessionKey)
  const userLoginTime = localStorage.getItem(`lastLoginTime_${userInfo.value.id}`)

  console.log('isNewLoginSession 检查:', {
    userId: userInfo.value.id,
    currentSession,
    userLoginTime,
    sessionEqual: currentSession === userLoginTime
  })

  // 如果没有会话标识，认为是新登录
  if (!currentSession) {
    console.log('isNewLoginSession: 没有当前会话标识，认为是新登录')
    return true
  }

  // 如果没有用户登录时间记录，也认为是新登录
  if (!userLoginTime) {
    console.log('isNewLoginSession: 没有用户登录时间记录，认为是新登录')
    return true
  }

  // 检查会话标识是否匹配当前用户的登录时间
  const isNew = currentSession !== userLoginTime
  console.log(`isNewLoginSession: 会话${isNew ? '不' : ''}匹配，${isNew ? '是' : '不是'}新登录`)
  return isNew
}

// 标记当前为新的登录会话
const markNewLoginSession = () => {
  if (!userInfo.value.id) {
    console.warn('markNewLoginSession: 用户ID不存在，无法标记登录会话')
    return
  }

  const now = Date.now().toString()
  const loginSessionKey = 'currentLoginSession'
  const userLoginTimeKey = `lastLoginTime_${userInfo.value.id}`

  localStorage.setItem(userLoginTimeKey, now)
  sessionStorage.setItem(loginSessionKey, now)

  console.log('已标记新的登录会话:', {
    userId: userInfo.value.id,
    timestamp: now,
    userLoginTimeKey,
    loginSessionKey
  })
}

// 检查语音指引是否已播放过
const checkVoiceGuideStatus = (guideType) => {
  const key = `voiceGuide_${guideType}_${userInfo.value.id}`
  return localStorage.getItem(key) === 'completed'
}

// 标记语音指引已完成
const markVoiceGuideCompleted = (guideType) => {
  const key = `voiceGuide_${guideType}_${userInfo.value.id}`
  localStorage.setItem(key, 'completed')
  console.log(`语音指引已标记为完成: ${guideType}`)
}

// 触发新用户语音指引
const triggerNewUserVoiceGuide = async () => {
  console.log('=== 新用户语音指引触发检查开始 ===')

  // 检查是否已经播放过新用户欢迎语音
  if (checkVoiceGuideStatus('newUserWelcome')) {
    console.log('❌ 新用户欢迎语音指引已播放过，跳过')
    return
  }

  console.log('✅ 新用户欢迎语音指引未播放过，开始播放')

  try {
    // 标记新的登录会话
    markNewLoginSession()

    // 立即显示全屏欢迎界面
    showFullScreenWelcome('newUser')
    console.log('✅ 新用户欢迎界面已显示')

    // 极短延迟后播放语音，确保界面渲染完成
    setTimeout(async () => {
      console.log('🎵 开始播放新用户欢迎语音')

      // 立即标记为已完成，确保下次登录时成为老用户（防止用户在播放完成前离开页面）
      markVoiceGuideCompleted('newUserWelcome')
      console.log('✅ 新用户欢迎语音已标记为完成，下次登录将成为老用户')

      await VoiceGuideService.playNewUserWelcome(displayName.value, () => {
        // 语音播放完成的回调，此时用户可以点击继续按钮
        console.log('✅ 新用户语音播放完成，可以继续')
      })
    }, 100) // 仅等待100ms确保界面渲染完成

    console.log('✅ 新用户欢迎语音指引已启动')
    console.log('=== 新用户语音指引触发检查结束 ===')
  } catch (error) {
    console.error('❌ 新用户语音指引播放失败:', error)
  }
}

// 触发老用户语音指引
const triggerReturningUserVoiceGuide = async () => {
  console.log('=== 老用户语音指引触发检查开始 ===')
  console.log('准备为老用户播放欢迎回归语音指引，用户信息:', {
    userId: userInfo.value.id,
    username: userInfo.value.username,
    displayName: displayName.value
  })

  // 首先检查是否是新的登录会话
  const isNewSession = isNewLoginSession()
  if (!isNewSession) {
    console.log('❌ 不是新的登录会话，跳过老用户语音指引')
    return
  }
  console.log('✅ 确认是新的登录会话，继续检查')

  // 检查本次会话是否已经播放过老用户欢迎语音
  const sessionKey = `returningUserWelcome_${userInfo.value.id}_session`
  const hasPlayed = sessionStorage.getItem(sessionKey) === 'completed'
  console.log('会话播放状态检查:', { sessionKey, hasPlayed })

  if (hasPlayed) {
    console.log('❌ 本次会话老用户欢迎语音指引已播放过，跳过')
    return
  }
  console.log('✅ 本次会话未播放过，准备播放语音指引')

  try {
    // 标记新的登录会话
    markNewLoginSession()

    // 立即显示全屏欢迎界面
    showFullScreenWelcome('returningUser')
    console.log('✅ 老用户欢迎界面已显示')

    // 极短延迟后播放语音，确保界面渲染完成
    setTimeout(async () => {
      console.log('🎵 开始播放老用户欢迎语音')
      await VoiceGuideService.playReturningUserWelcome(displayName.value, () => {
        // 语音播放完成的回调
        console.log('✅ 老用户语音播放完成，可以继续')
      })

      // 标记本次会话已播放
      sessionStorage.setItem(sessionKey, 'completed')
      console.log('✅ 已标记老用户语音指引在本次会话完成，sessionKey:', sessionKey)
    }, 100) // 仅等待100ms确保界面渲染完成

    console.log('✅ 老用户欢迎语音指引已启动')
    console.log('=== 老用户语音指引触发检查结束 ===')
  } catch (error) {
    console.error('❌ 老用户语音指引播放失败:', error)
  }
}

const clearInterviewDataForNewUser = () => {
  console.log('🧹 清理新用户的面试相关数据...');
  
  const keysToRemove = [
    'interviewState',
    'interviewStatus', 
    'pendingInterviewJob',
    'interviewSessionId',
    'interviewTime',
    'applicationId',
    'lastReportId'
  ];
  
  keysToRemove.forEach(key => {
    localStorage.removeItem(key);
    console.log(`已清除localStorage键: ${key}`);
  });
  
  // 重置interviewStore状态
  if (interviewStore && typeof interviewStore.resetState === 'function') {
    interviewStore.resetState();
    console.log('已重置interviewStore状态');
  }
  
  console.log('✅ 新用户面试数据清理完成');
}

const checkNewUser = (userId) => {
  if (!userId) return Promise.resolve(false);

  console.log('=== 新用户检查开始 ===');
  console.log('检查用户ID:', userId);

  // 首先检查是否已经播放过新用户欢迎语音
  const hasPlayedNewUserWelcome = checkVoiceGuideStatus('newUserWelcome');
  console.log('新用户欢迎语音播放状态:', hasPlayedNewUserWelcome);

  if (hasPlayedNewUserWelcome) {
    console.log('✅ 用户已播放过新用户欢迎语音，视为老用户');
    console.log('=== 新用户检查结束：老用户 ===');
    return Promise.resolve(false);
  }

  console.log('🔍 未播放过新用户语音，检查用户详细资料...');

  return request({
    url: '/api/users/details',
    method: 'get'
  }).then(detailsResponse => {
    console.log('获取用户详细信息结果:', detailsResponse);

    // 如果没有详细信息或关键字段为空，认为是新用户
    if (!detailsResponse ||
        (!detailsResponse.jobTitle && !detailsResponse.experience && !detailsResponse.education)) {
      console.log('✅ 检测到新用户（无详细资料），需要引导完善资料');
      console.log('=== 新用户检查结束：新用户 ===');

      // 清理新用户的面试相关数据
      clearInterviewDataForNewUser();

      // 立即触发新用户语音指引和全屏欢迎界面
      triggerNewUserVoiceGuide();

      return true;
    }

    console.log('❌ 用户有详细资料但未播放过新用户语音，视为老用户');
    console.log('=== 新用户检查结束：老用户 ===');
    return false;
  }).catch(error => {
    console.error('检查新用户状态失败:', error);
    // 如果API返回404（资料不存在），也认为是新用户
    if (error.response && error.response.status === 404) {
      console.log('✅ 404错误（无详细资料），认为是新用户');
      console.log('=== 新用户检查结束：新用户 ===');

      // 清理新用户的面试相关数据
      clearInterviewDataForNewUser();

      // 立即触发新用户语音指引和全屏欢迎界面
      triggerNewUserVoiceGuide();

      return true;
    }

    console.log('❌ API错误，视为老用户');
    console.log('=== 新用户检查结束：老用户 ===');
    return false;
  });
}

// 头像加载错误时的处理
const avatarLoadError = () => {
  console.error('Dashboard页面头像加载失败:', userInfo.value.avatarUrl);
  // 使用默认头像
  userInfo.value.avatarUrl = '';
}

// 修正头像URL路径
const fixAvatarUrl = (url) => {
  if (!url) return '';

  // 如果已经是完整的URL，则直接返回
  if (url.startsWith('http') || url.startsWith('data:')) {
    return url;
  }

  // 如果是相对路径但不是以/api开头，添加/api前缀
  if (url.startsWith('/avatars/') && !url.startsWith('/api/')) {
    console.log('Dashboard页面修正头像URL路径:', url, ' => ', '/api' + url);
    return '/api' + url;
  }

  return url;
}

// 修改为从后端获取的统计数据
const statsArr = ref([
  { label: '已投递', value: 0 },
  { label: '待面试', value: 0 },
  { label: '已面试', value: 0 },
  { label: '已完成', value: 0 }
]);

const interviewList = ref([])

// 面试列表数据通过后端接口获取

const deliveryHistory = ref([])
// 投递历史数据通过后端接口获取

// 修改historyList，从后端获取用户的投递记录
const historyList = ref([]);
const showAllHistory = ref(false);
const showHistoryList = computed(() => showAllHistory.value ? historyList.value : historyList.value.slice(0, 2));

// 获取岗位详情，以获取公司名称和岗位标题
const fetchJobDetails = (jobId) => {
  console.log('获取岗位详情, jobId:', jobId);
  return request({
    url: `/api/jobs/${jobId}`,
    method: 'get'
  }).then(response => {
    console.log('获取到岗位详情:', response);
    return response;
  }).catch(error => {
    console.error('获取岗位详情失败:', error);
    return null;
  });
};

const fetchUserApplications = () => {
  // 尝试多种方式获取用户ID
  let userId = null;

  // 1. 从auth工具获取
  const authUserId = auth.getUserId();

  // 2. 从localStorage直接获取用户信息
  const userInfoStr = localStorage.getItem('user_info');
  let userInfoObj = null;
  try {
    userInfoObj = userInfoStr ? JSON.parse(userInfoStr) : null;
  } catch (e) {
    // 静默处理
  }

  // 3. 从localStorage中的user获取
  const userStr = localStorage.getItem('user');
  let userObj = null;
  try {
    userObj = userStr ? JSON.parse(userStr) : null;
  } catch (e) {
    // 静默处理
  }

  // 优先使用auth中的ID，然后是user_info，最后是user
  userId = authUserId || (userInfoObj?.id) || (userObj?.id);

  // 如果仍然没有找到用户ID，尝试使用userInfo.value
  if (!userId && userInfo.value && userInfo.value.id) {
    userId = userInfo.value.id;
  }

  if (!userId) {
    ElMessage.warning('请先登录再查看投递记录');
    return Promise.resolve();
  }

  // 确保用户ID是数字类型
  const numericUserId = Number(userId);
  if (isNaN(numericUserId)) {
    ElMessage.warning('用户ID格式不正确');
    return Promise.resolve();
  }

  // 使用getUserApplications函数获取投递记录
  return getUserApplications()
      .then(response => {
        if (response && Array.isArray(response)) {
          // 并发获取岗位详情
          const jobDetailPromises = response.map(app => app.jobId ? fetchJobDetails(app.jobId) : Promise.resolve(null));
          return Promise.all(jobDetailPromises)
              .then(jobDetailsList => {
                // 处理每一条申请记录
                const tempList = response.map((app, idx) => {
                  const jobDetails = jobDetailsList[idx];

                  // 根据状态转换状态描述
                  let statusText;
                  if (typeof app.status === 'number') {
                    switch(app.status) {
                      case 0: statusText = '待审核'; break;
                      case 1: statusText = '已投递'; break;
                      case 2: statusText = '待面试'; break;
                      case 3: statusText = '已面试'; break;
                      case 4: statusText = '已完成'; break;
                      default: statusText = '未知状态'; break;
                    }
                  } else if (typeof app.status === 'string') {
                    // 处理字符串状态
                    switch(app.status.toUpperCase()) {
                      case 'SUBMITTED': statusText = '待审核'; break;
                      case 'PENDING1': statusText = '已投递'; break;
                      case 'PENDING2': statusText = '待面试'; break;
                      case 'ONGOING': statusText = '已面试'; break;
                      case 'COMPLETED': statusText = '已完成'; break;
                      default: statusText = app.status; break;
                    }
                  } else {
                    statusText = '未知状态';
                  }

                  // 格式化日期，优先使用submitTime，然后是interviewTime
                  const appTime = app.submitTime || app.applicationTime || app.interviewTime;
                  const formattedTime = appTime ? new Date(appTime).toLocaleString() : '未知时间';

                  // 返回格式化后的记录
                  return {
                    id: app.id,
                    title: jobDetails?.title || '未知岗位',
                    company: jobDetails?.companyName || '未知公司',
                    time: formattedTime,
                    status: statusText,
                    applicationId: app.id,
                    jobId: app.jobId
                  };
                });

                // 更新历史列表
                historyList.value = tempList;

                // 如果历史列表不为空，刷新统计数据确保一致性
                if (historyList.value.length > 0) {
                  return refreshStatistics().catch(() => {
                    // 静默处理错误
                    return Promise.resolve();
                  });
                }
                return Promise.resolve();
              });
        } else {
          if (response && !Array.isArray(response)) {
            ElMessage.warning('获取投递记录数据格式不正确');
          }
          historyList.value = [];
          return Promise.resolve();
        }
      })
      .catch(error => {
        ElMessage.error('获取投递记录失败，请稍后重试');
        historyList.value = [];
        return Promise.resolve();
      });
};

// 更新getStatusType函数，根据不同状态返回不同类型
const getStatusType = (status) => {
  const statusMap = {
    '待审核': 'info',
    '已投递': 'primary',
    '待面试': 'warning',
    '已面试': 'success',
    '已完成': 'success',
    '通过': 'success',
    '未通过': 'danger'
  };
  return statusMap[status] || 'info';
}

const handleSelectJob = () => {
  router.push('/jobs')
}

// 面试弹窗相关
const interviewDialogVisible = ref(false)
const interviewForm = reactive({})
const interviewFormRef = ref()
const interviewRules = {
  company: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  position: [{ required: true, message: '请输入职位', trigger: 'blur' }],
  date: [{ required: true, message: '请选择时间', trigger: 'change' }],
  location: [{ required: true, message: '请输入地点', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function openInterviewDialog(row) {
  if (row) {
    Object.assign(interviewForm, row)
  } else {
    Object.assign(interviewForm, { company: '', position: '', date: '', location: '', status: '', id: undefined })
  }
  interviewDialogVisible.value = true
}

function submitInterview() {
  interviewFormRef.value.validate((valid) => {
    if (!valid) return
    if (interviewForm.id) {
      // 编辑
      // TODO: 调用后端接口更新面试记录
      ElMessage.success('编辑成功')
    } else {
      // 新增
      // TODO: 调用后端接口新增面试记录
      interviewForm.id = Date.now()
      ElMessage.success('添加成功')
    }
    interviewDialogVisible.value = false
  })
}

// 删除相关
const deleteDialogVisible = ref(false)
let deleteTarget = null
function handleDelete(row) {
  deleteTarget = row
  deleteDialogVisible.value = true
}
function confirmDelete() {
  // TODO: 调用后端接口删除面试记录
  ElMessage.success('删除成功')
  deleteDialogVisible.value = false
}

// 安排面试弹窗相关
const arrangeDialogVisible = ref(false)
const arrangeForm = reactive({ company: '', position: '', date: '', location: '' })
const arrangeFormRef = ref()
function openArrangeDialog(activity) {
  arrangeForm.company = activity.company
  arrangeForm.position = activity.position
  arrangeForm.date = ''
  arrangeForm.location = ''
  arrangeDialogVisible.value = true
}
function submitArrange() {
  // TODO: 调用后端接口安排面试
  ElMessage.success('安排成功')
  arrangeDialogVisible.value = false
}

const pendingJob = computed(() => interviewStore?.job || null)
const interviewStatus = ref('pending') // 'pending' | 'completed'

// 步骤条状态联动
// 新状态码: 0-未投递 1-待面试 2-已面试 3-分析报告
const currentStep = computed(() => {
  if (!interviewStore) {
    console.warn('interviewStore 未初始化，返回默认状态 0');
    return 0;
  }

  console.log('计算进度条状态，当前状态:', interviewStore.status, '当前岗位:', interviewStore.job);

  if (!interviewStore.job) return 0 // 没有岗位，高亮"未投递"状态

  // 根据状态确定高亮步骤
  switch (interviewStore.status) {
    case 'none':
      return 0; // 未投递状态
    case 'submitted':
    case 'applied':
    case 'pending1':
    case 'pending2':
    case 'pending': // 统一为待面试状态
      return 1;
    case 'ongoing': // 进行中，对应已面试
      return 2;
    case 'completed':
      return 2; // 面试已完成，但报告未生成
    case 'report':
      return 3; // 分析报告已生成，高亮到分析报告
    default:
      // 如果状态未知，尝试从localStorage获取
      const storedStatus = localStorage.getItem('interviewStatus');
      console.log('未知状态，从localStorage获取:', storedStatus);

      if (storedStatus === 'none') return 0;
      if (storedStatus === 'pending' || storedStatus === 'pending1' || storedStatus === 'pending2') return 1;
      if (storedStatus === 'ongoing') return 2;
      if (storedStatus === 'completed') return 2;
      if (storedStatus === 'report') return 3;
      if (storedStatus === 'applied' || storedStatus === 'submitted') return 1;

      console.log('未知状态:', interviewStore.status, '默认返回0');
      return 0;
  }
})

const nextInterview = ref(null)
let interviewTimer = null

// 获取面试统计数据
const fetchStatistics = () => {
  console.log('获取面试统计数据...')

  // 获取当前用户ID
  const userId = userInfo.value.id || auth.getUserId();
  if (!userId) {
    console.warn('未找到用户ID，无法获取统计数据');
    ElMessage.warning('请先登录后再查看统计数据');
    return Promise.resolve();
  }

  console.log('使用用户ID获取统计数据:', userId);

  // 使用显式的URL参数传递用户ID
  const timestamp = new Date().getTime();
  const random = Math.floor(Math.random() * 1000);

  return request({
    url: `/api/statistics/interview`,
    method: 'get',
    params: {
      userId: userId,
      _t: timestamp,
      _r: random
    },
    headers: {
      'Cache-Control': 'no-cache, no-store',
      'Pragma': 'no-cache',
      'X-User-ID': userId,
      'userId': userId
    }
  }).then(res => {
    console.log('获取到面试统计数据:', res);

    // 如果获取到数据，更新统计卡片
    if (res) {
      updateStatisticsDisplay(res);
    } else {
      console.warn('未获取到统计数据，使用默认值');
      useDefaultStatistics();
    }

    return res;
  }).catch(error => {
    console.error('API请求失败:', error);
    // 如果API失败，使用默认值
    useDefaultStatistics();
    throw error;
  });
}

// 使用默认统计数据
const useDefaultStatistics = () => {
  statsArr.value = [
    { label: '已投递', value: 0 },
    { label: '待面试', value: 0 },
    { label: '已面试', value: 0 },
    { label: '已完成', value: 0 }
  ];
}

// 更新统计数据显示
const updateStatisticsDisplay = (data) => {
  // 确保数值类型正确
  const submittedCount = Number(data.submittedCount || 0);
  const pendingCount = Number(data.pendingCount || 0);
  const interviewedCount = Number(data.interviewedCount || 0);
  const completedCount = Number(data.completedCount || 0);

  // 更新统计数据
  statsArr.value = [
    { label: '已投递', value: submittedCount },
    { label: '待面试', value: pendingCount },
    { label: '已面试', value: interviewedCount },
    { label: '已完成', value: completedCount }
  ];

  console.log('统计数据已更新:', statsArr.value);
}

// 刷新统计数据
const refreshStatistics = () => {
  console.log('刷新面试统计数据...');

  // 先获取用户ID
  const userId = userInfo.value?.id || auth.getUserId();
  if (!userId) {
    console.error('无法获取用户ID，刷新统计数据失败');
    ElMessage.warning('获取用户ID失败，请先登录');
    return Promise.resolve(null);
  }

  // 直接调用API获取投递记录，用于手动校正统计数据
  return request.get('/api/job-applications/user', {
    params: { userId },
    headers: { 'X-User-ID': String(userId) }
  }).catch(e => {
    console.error('获取投递记录失败:', e);
    return [];
  }).then(applications => {
    console.log('刷新统计前获取到投递记录:', applications);

    // 调用刷新统计数据API
    return refreshInterviewStatistics().then(res => {
      console.log('刷新统计数据结果:', res);

      // 手动确保统计数据与实际记录一致
      let submittedCount = res?.submittedCount || 0;
      if (Array.isArray(applications) && applications.length > 0) {
        // 如果API返回的submittedCount为0但确实有投递记录，手动修正
        if (submittedCount === 0) {
          submittedCount = applications.length;
          console.warn(`发现统计数据不一致，手动修正已投递数量为: ${submittedCount}`);
        }
      }

      if (res) {
        // 更新统计卡片数据，确保正确处理数据类型
        statsArr.value = [
          { label: '已投递', value: submittedCount },
          { label: '待面试', value: Number(res.pendingCount || 0) },
          { label: '已面试', value: Number(res.interviewedCount || 0) },
          { label: '已完成', value: Number(res.completedCount || 0) }
        ];

        console.log('更新后的统计数据:', statsArr.value);

        return res; // 返回结果，方便链式调用
      } else {
        console.error('刷新后未获取到统计数据');
        ElMessage.warning({
          message: '未获取到统计数据',
          duration: 2000
        });

        // 如果没有获取到数据，尝试重新获取
        return fetchStatistics();
      }
    }).catch(error => {
      console.error('刷新统计数据失败:', error);
      ElMessage.error({
        message: '刷新统计数据失败，请稍后再试',
        duration: 3000
      });

      // 发生错误时，尝试重新获取
      setTimeout(() => {
        fetchStatistics();
      }, 1000);

      throw error; // 抛出错误，方便调用者处理
    });
  });
};

// 清空统计数据
const clearStatistics = () => {
  // 先询问用户是否确认清空
  return ElMessageBox.confirm(
      '确定要重置统计数据为0吗？此操作仅影响统计数值显示。',
      '重置确认',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning',
      }
  ).then(() => {
    console.log('开始重置面试统计数据...');

    return clearInterviewStatistics().then(res => {
      console.log('重置统计数据结果:', res);

      if (res && res.success) {
        // 更新统计卡片数据
        statsArr.value = [
          { label: '已投递', value: 0 },
          { label: '待面试', value: 0 },
          { label: '已面试', value: 0 },
          { label: '已完成', value: 0 }
        ];

        console.log('已重置统计数据');
        ElMessage.success('统计数据已重置为0');
      } else {
        console.error('重置统计数据失败:', res ? res.message : '未知错误');
        ElMessage.error(res && res.message ? res.message : '重置统计数据失败');
      }
    }).catch(error => {
      console.error('重置统计数据出错:', error);
      ElMessage.error('重置统计数据失败，请稍后再试');
    });
  }).catch(error => {
    if (error === 'cancel') {
      console.log('用户取消了重置操作');
      return;
    }

    console.error('重置统计数据对话框出错:', error);
  });
};

// 检查面试报告状态
function checkReportStatus() {
  console.log('检查面试报告状态...');
  
  // 检查是否存在报告ID或者其他报告生成的标识
  const lastReportId = localStorage.getItem('lastReportId');
  const currentStatus = interviewStore.status;
  
  console.log('当前状态:', currentStatus, '报告ID:', lastReportId);
  
  // 如果有报告ID且当前状态是completed，则更新为report状态
  if (lastReportId && currentStatus === 'completed') {
    console.log('检测到面试报告已生成，更新状态为report');
    setReportReady();
  }
  
  // 也可以通过API检查是否有报告
  const applicationId = interviewStore.job ? interviewStore.job.applicationId : null;
  if (applicationId && currentStatus === 'completed') {
    // 这里可以添加API调用检查报告是否存在
    // request.get(`/api/interview-reports/check/${applicationId}`)
    //   .then(response => {
    //     if (response && response.exists) {
    //       setReportReady();
    //     }
    //   })
    //   .catch(error => console.log('检查报告状态失败:', error));
  }
}

// 初始化数据
onMounted(() => {
  // 先获取用户信息，使用then链式调用而不是async/await
  fetchUserInfo().then(() => {
    // 确保用户ID存储在auth工具中
    if (userInfo.value.id) {
      const currentUser = auth.getUserInfo() || {};
      if (!currentUser.id || currentUser.id !== userInfo.value.id) {
        auth.setUserInfo({
          ...currentUser,
          id: userInfo.value.id
        });
      }

      // 调试和修复当前的job对象
      debugAndFixJobObject();

      // 在确保有用户ID后强制刷新统计数据
      refreshInterviewStatistics().then(response => {
        if (response) {
                  statsArr.value = [
          { label: '已投递', value: Number(response.submittedCount || 0) },
          { label: '待面试', value: Number(response.pendingCount || 0) },
          { label: '已面试', value: Number(response.interviewedCount || 0) },
          { label: '已完成', value: Number(response.completedCount || 0) }
        ];
        }
      }).catch(error => {
        // 如果刷新失败，回退到普通获取方式
        fetchStatistics();
      });
    } else {
      ElMessage.warning('未获取到用户ID，请先登录');
    }

    // 获取面试会话数据
    fetchNextInterview();
    interviewTimer = setInterval(fetchNextInterview, 60000); // 每60秒刷新一次

    // 获取用户投递记录
    fetchUserApplications();

    // 获取用户详细信息用于计算完善度
    fetchUserDetailInfo();
    
    // 获取用户简历列表用于计算完善度
    fetchUserResumeList();

    // 初始化获取当前申请记录
    if (interviewStore.job) {
      fetchCurrentApplicationRecord();
    }

    // 检查面试报告状态
    checkReportStatus();
  }).catch(error => {
    console.error('初始化数据失败:', error);
    ElMessage.error('获取用户信息失败，请刷新页面重试');
  }).finally(() => {
    // 添加页面可见性监听器
    document.addEventListener('visibilitychange', handleVisibilityChange);

    // 添加键盘导航监听器
    document.addEventListener('keydown', handleKeydown);

    // 添加网络状态监听器
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    // 启动性能监控
    performanceObserver();

    // 添加预加载提示
    if ('serviceWorker' in navigator) {
      console.log('Service Worker 支持检测');
    }

    // 性能监控
    if (performance.mark) {
      performance.mark('dashboard-loaded');
    }

    // 全局错误处理
    window.addEventListener('error', (event) => {
      handleError(event.error, 'global-error');
    });

    window.addEventListener('unhandledrejection', (event) => {
      handleError(event.reason, 'unhandled-promise');
    });
  });
});

// 调试和修复job对象
function debugAndFixJobObject() {
  if (!interviewStore.job) {
    console.log('当前没有job对象');
    return;
  }

  console.log('当前job对象:', interviewStore.job);

  // 确保job.id是数字类型
  if (interviewStore.job.id && typeof interviewStore.job.id === 'string') {
    try {
      const numericId = parseInt(interviewStore.job.id, 10);
      if (!isNaN(numericId)) {
        console.log('修正job.id类型，从字符串:', interviewStore.job.id, '到数字:', numericId);
        interviewStore.updateJob({
          ...interviewStore.job,
          id: numericId
        });
      }
    } catch (e) {
      console.error('转换job.id失败:', e);
    }
  }

  // 如果有applicationId但类型不是数字，修正它
  if (interviewStore.job.applicationId && typeof interviewStore.job.applicationId === 'string') {
    try {
      const numericAppId = parseInt(interviewStore.job.applicationId, 10);
      if (!isNaN(numericAppId)) {
        console.log('修正job.applicationId类型，从字符串:', interviewStore.job.applicationId, '到数字:', numericAppId);
        interviewStore.updateJob({
          ...interviewStore.job,
          applicationId: numericAppId
        });
      }
    } catch (e) {
      console.error('转换job.applicationId失败:', e);
    }
  }
}

// 初始化申请记录ID的函数
function initializeApplicationId() {
  if (!interviewStore.job) return Promise.resolve(null);

  console.log('正在初始化申请记录ID...');
  const jobId = interviewStore.job.id;

  // 使用前面定义的fetchApplicationId函数
  return fetchApplicationId(jobId).then(applicationId => {
    if (applicationId) {
      console.log('获取到申请记录ID:', applicationId);

      // 更新job对象
      interviewStore.updateJob({
        ...interviewStore.job,
        applicationId: applicationId
      });

      return applicationId;
    } else {
      console.warn('未找到与当前岗位匹配的申请记录');
      return null;
    }
  }).catch(error => {
    console.error('初始化申请记录ID时出错:', error);
    return null;
  });
}

// 添加检查申请记录ID的函数
function checkApplicationId() {
  if (!interviewStore.job) return Promise.resolve(null);

  try {
    console.log('正在获取用户投递记录...');

    // 如果已有applicationId，直接返回
    if (interviewStore.job.applicationId) {
      console.log('已有申请记录ID:', interviewStore.job.applicationId);
      return Promise.resolve(interviewStore.job.applicationId);
    }

    // 否则尝试获取
    return initializeApplicationId();
  } catch (error) {
    console.error('检查申请记录ID时出错:', error);
    return Promise.resolve(null);
  }
}

// 获取当前申请记录
function fetchCurrentApplicationRecord() {
  try {
    // 如果没有applicationId，尝试获取
    if (!interviewStore.job.applicationId) {
      initializeApplicationId().then(applicationId => {
        console.log('获取到申请记录ID:', applicationId);
      }).catch(error => {
        console.error('获取申请记录ID失败:', error);
      });
    }
  } catch (error) {
    console.error('获取当前申请记录失败:', error);
  }
}

onUnmounted(() => {
  if (interviewTimer) clearInterval(interviewTimer)

  // 清理语音播放
  VoiceGuideService.stopCurrentVoice()

  // 清理所有事件监听器
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)

  // 清理性能标记
  if (performance.clearMarks) {
    performance.clearMarks('dashboard-loaded')
  }
})

function fetchNextInterview() {
  try {
    console.log('开始获取面试会话数据...')

    // 清空静态数据，使用空数组
    const sessions = []

    console.log('获取到面试会话数据:', sessions)

    if (sessions.length > 0) {
      // 获取最新的一个面试会话
      const latestSession = sessions[0]
      nextInterview.value = {
        id: latestSession.id,
        title: latestSession.sessionName,
        company: latestSession.job ? latestSession.job.companyName : '未知公司',
        date: latestSession.sessionTime,
        status: latestSession.status === 0 ? '进行中' : latestSession.status === 1 ? '已完成' : '已取消'
      }
      console.log('获取到最新面试会话:', nextInterview.value)
    } else {
      console.log('未找到面试会话')
      nextInterview.value = null
    }
  } catch (e) {
    console.error('获取面试会话失败:', e)
    nextInterview.value = null
  }
}

function goToApplyJob() {
  router.push('/jobs')
}

// 继续投递函数
function continueApplying() {
  try {
    console.log('用户点击继续投递，当前面试状态:', interviewStore?.status)
    console.log('当前岗位信息:', interviewStore?.job)
    
    // 确认当前状态为已完成报告状态
    if (interviewStore.status !== 'report') {
      ElMessage.warning('当前状态不支持继续投递，请先完成面试')
      return
    }
    
    // 显示确认对话框
    ElMessageBox.confirm(
      '您已完成当前岗位的面试，是否要继续投递其他岗位？继续投递将清除当前面试记录。',
      '继续投递确认',
      {
        confirmButtonText: '确定投递',
        cancelButtonText: '取消',
        type: 'info',
        distinguishCancelAndClose: true
      }
    ).then(() => {
      console.log('用户确认继续投递')
      
      // 保存完成的面试记录到历史中
      const completedInterview = {
        jobTitle: interviewStore.job?.title,
        companyName: interviewStore.job?.companyName,
        status: '已完成',
        completedTime: new Date().toLocaleString(),
        applicationId: interviewStore.job?.applicationId
      }
      
      console.log('保存完成的面试记录:', completedInterview)
      
      // 重置面试状态，为新的投递做准备
      interviewStore.resetState()
      
      // 确保localStorage中的状态也被清除
      localStorage.removeItem('interviewStatus')
      localStorage.removeItem('pendingInterviewJob')
      localStorage.removeItem('interviewSessionId')
      localStorage.removeItem('interviewJob')
      localStorage.removeItem('lastReportId')
      
      console.log('面试状态已重置，准备跳转到岗位选择页面')
      
      // 提示用户并跳转到岗位选择页面
      ElMessage.success({
        message: '面试记录已保存，正在跳转到岗位选择页面...',
        duration: 2000
      })
      
      // 延迟跳转，让用户看到提示信息
      setTimeout(() => {
        router.push('/jobs').then(() => {
          console.log('✅ 成功跳转到岗位选择页面')
          ElMessage.info('请选择新的岗位进行投递')
        }).catch(error => {
          console.error('❌ 跳转岗位页面失败:', error)
          ElMessage.error('跳转失败，请手动前往岗位页面')
        })
      }, 1000)
      
      // 刷新统计数据
      setTimeout(() => {
        refreshStatistics().catch(error => {
          console.error('刷新统计数据失败:', error)
        })
      }, 2000)
      
    }).catch((action) => {
      if (action === 'cancel') {
        console.log('用户取消继续投递')
        ElMessage.info('已取消继续投递')
      } else {
        console.log('用户关闭继续投递对话框')
      }
    })
    
  } catch (error) {
    console.error('继续投递函数执行出错:', error)
    ElMessage.error('继续投递失败，请刷新页面重试')
  }
}

function goToInterview() {
  try {
    console.log('用户点击进入面试按钮');
    console.log('当前状态:', interviewStore?.status);
    console.log('当前岗位:', interviewStore?.job);

    // 检查store是否可用
    if (!interviewStore) {
      console.error('interviewStore 未初始化');
      ElMessage.error('系统未初始化，请刷新页面重试');
      return;
    }

    // 检查是否有岗位信息
    if (!interviewStore.job) {
      console.error('没有岗位信息，无法进入面试');
      ElMessage.warning('没有找到岗位信息，请先选择岗位');
      return;
    }

    // 检查router是否可用
    if (!router) {
      console.error('router 未初始化');
      ElMessage.error('路由未初始化，请刷新页面重试');
      return;
    }

    console.log('准备跳转到面试页面...');
    router.push('/interview').then(() => {
      console.log('✅ 成功跳转到面试页面');
      ElMessage.success('正在进入面试页面...');
    }).catch(error => {
      console.error('❌ 跳转面试页面失败:', error);
      ElMessage.error(`跳转失败: ${error.message || '未知错误'}`);
    });

  } catch (error) {
    console.error('goToInterview函数执行出错:', error);
    ElMessage.error('进入面试失败，请刷新页面重试');
  }
}

function goToReport() {
  // 更新状态为"分析报告"状态
  interviewStore.setStatus('report');

  // 同步更新后端状态为4(查看分析报告)
  const applicationId = interviewStore.job ? interviewStore.job.applicationId : null;
  if (applicationId) {
    request({
      url: `/api/job-applications/${applicationId}/status`,
      method: 'put',
      data: {
        status: 4, // 状态4表示查看分析报告/已完成
        feedback: `用户已查看分析报告，面试已完成`
      }
    }).then(() => {
      console.log('已更新后端状态为查看分析报告');

      // 刷新统计数据
      refreshStatistics().then(() => {
        console.log('已刷新统计数据，更新已完成面试数量');
      }).catch(error => {
        console.error('刷新统计数据失败:', error);
      });
    }).catch(error => {
      console.error('更新面试状态失败:', error);
    });
  }

  router.push('/report');
}

// 当面试报告生成后，更新状态为报告可查看
function setReportReady() {
  console.log('面试报告已生成，更新状态为report');
  interviewStore.setStatus('report');
  
  // 存储到localStorage以便持久化
  localStorage.setItem('interviewStatus', 'report');
  
  ElMessage.success('面试报告已生成，可点击查看！');
}

// 前往面试分析报告页面
function goToAnalysisReport() {
  // 确认状态为"分析报告"状态
  interviewStore.setStatus('report');

  // 同步更新后端状态为4(查看分析报告)
  const applicationId = interviewStore.job ? interviewStore.job.applicationId : null;
  if (applicationId) {
    request({
      url: `/api/job-applications/${applicationId}/status`,
      method: 'put',
      data: {
        status: 4, // 状态4表示查看分析报告/已完成
        feedback: `用户已查看面试分析报告，面试已完成`
      }
    }).then(() => {
      console.log('已更新后端状态为查看分析报告');

      // 刷新统计数据
      refreshStatistics().then(() => {
        console.log('已刷新统计数据，更新已完成面试数量');
      }).catch(error => {
        console.error('刷新统计数据失败:', error);
      });
    }).catch(error => {
      console.error('更新面试状态失败:', error);
    });
  }

  router.push('/interview-analysis-report');
}

function markCompleted(row) {
  row.status = '已完成'
  // 可同步到后端
  ElMessage.success('已标记为已完成')
}

// 用户详细信息用于计算完善度
const userDetailInfo = ref({
  avatar: '',
  name: '',
  title: '',
  email: '',
  phone: '',
  location: '',
  jobIntention: '',
  experience: '',
  education: '',
  skills: []
})

// 用户简历列表
const userResumeList = ref([])

// 计算简历完善度的函数
function calculateResumeCompleteness() {
  const fields = {
    // 基本信息权重 (总计40%)
    avatar: { weight: 8, value: userDetailInfo.value.avatar },
    name: { weight: 10, value: userDetailInfo.value.name },
    email: { weight: 8, value: userDetailInfo.value.email },
    phone: { weight: 7, value: userDetailInfo.value.phone },
    location: { weight: 7, value: userDetailInfo.value.location },
    
    // 职业信息权重 (总计35%)
    title: { weight: 10, value: userDetailInfo.value.title },
    jobIntention: { weight: 10, value: userDetailInfo.value.jobIntention },
    experience: { weight: 8, value: userDetailInfo.value.experience },
    education: { weight: 7, value: userDetailInfo.value.education },
    
    // 技能权重 (总计15%)
    skills: { weight: 15, value: userDetailInfo.value.skills },
    
    // 简历文件权重 (总计10%)
    resume: { weight: 10, value: userResumeList.value }
  }
  
  let totalWeight = 0
  let completedWeight = 0
  
  Object.entries(fields).forEach(([key, field]) => {
    totalWeight += field.weight
    
    // 判断字段是否已填写
    let isCompleted = false
    if (key === 'skills') {
      isCompleted = Array.isArray(field.value) && field.value.length > 0
    } else if (key === 'resume') {
      isCompleted = Array.isArray(field.value) && field.value.length > 0
    } else {
      isCompleted = field.value && field.value.toString().trim() !== ''
    }
    
    if (isCompleted) {
      completedWeight += field.weight
    }
  })
  
  const percentage = totalWeight > 0 ? Math.round((completedWeight / totalWeight) * 100) : 0
  
  // 记录完善度计算结果
  console.log(`简历完善度: ${percentage}% (${completedWeight}/${totalWeight})`)
  
  return percentage
}

// 动态计算简历完善度
const resumePercent = computed(() => {
  return calculateResumeCompleteness()
})
const steps = [
  { title: '未投递', icon: DocumentRemove, desc: '您尚未投递简历。' },
  { title: '待面试', icon: Calendar, desc: '您的简历已通过AI初步筛选，可以随时开始面试。' },
  { title: '已面试', icon: DocumentChecked, desc: '恭喜您，面试已完成！' },
  { title: '分析报告', icon: User, desc: '分析报告已生成，可点击"继续投递"申请其他岗位！' }
]
function goToResume() {
  router.push('/profile')
}

// 刷新简历完善度
const refreshResumeCompleteness = async () => {
  try {
    console.log('刷新简历完善度...')
    
    // 重新获取用户详细信息和简历列表
    await Promise.all([
      fetchUserDetailInfo(),
      fetchUserResumeList()
    ])
    
    console.log('简历完善度数据已刷新，当前完善度:', resumePercent.value + '%')
  } catch (error) {
    console.error('刷新简历完善度失败:', error)
  }
}

// 暴露刷新方法供其他页面调用
window.refreshDashboardResumeCompleteness = () => {
  console.log('收到简历更新通知，刷新完善度')
  refreshResumeCompleteness()
}

function toggleHistory() { showAllHistory.value = !showAllHistory.value }

// 清除旧的会话状态（当用户切换时）
const clearOldSessionState = (newUserId) => {
  const currentUserId = userInfo.value.id

  if (currentUserId && currentUserId !== newUserId) {
    console.log('检测到用户切换，清除旧的会话状态')
    // 清除旧用户的会话状态
    const oldSessionKeys = [
      `returningUserWelcome_${currentUserId}_session`,
      'currentLoginSession'
    ]

    oldSessionKeys.forEach(key => {
      sessionStorage.removeItem(key)
    })
  }
}

// 获取用户信息
const fetchUserInfo = () => {
  console.log('开始获取用户信息...')

  // 添加重试机制
  let retries = 2;

  return new Promise((resolve, reject) => {
    function tryFetchUser() {
      request.get('/api/users/current', {
        timeout: 5000, // 设置较短的超时时间
        headers: {
          'Cache-Control': 'no-cache',
          'Pragma': 'no-cache'
        }
      }).then(res => {
        if (res) {
          // 清除旧的会话状态（如果用户切换了）
          clearOldSessionState(res.id)

          const user = {
            id: res.id,
            username: res.username,
            realName: res.realName,
            avatarUrl: fixAvatarUrl(res.avatarUrl),
            email: res.email,
            role: res.role
          };

          userInfo.value = user;

          // 检查头像是否存在
          return checkAvatarExists().then(() => {
            // 检查是否是新用户
            return checkNewUser(res.id).then(isNewUser => {
              if (isNewUser) {
                console.log('检测到新用户，将显示全屏欢迎界面和语音指引');
                // 新用户的全屏欢迎界面和语音指引已在checkNewUser中处理
              } else {
                // 老用户，显示全屏欢迎界面并播放欢迎回归语音指引
                console.log('检测到老用户，准备显示全屏欢迎界面');
                triggerReturningUserVoiceGuide();
              }
              resolve(user);
            });
          });
        } else {
          throw new Error('未获取到用户信息');
        }
      }).catch(error => {
        console.warn(`获取用户信息失败，剩余重试次数: ${retries}`, error);
        retries--;

        if (retries < 0) {
          // 尝试从本地存储恢复用户信息
          const localUser = localStorage.getItem('user_info');
          if (localUser) {
            try {
              const parsedUser = JSON.parse(localUser);
              console.log('从本地存储恢复用户信息:', parsedUser);

              // 清除旧的会话状态（如果用户切换了）
              clearOldSessionState(parsedUser.id)

              // 更新到userInfo
              userInfo.value = {
                id: parsedUser.id,
                username: parsedUser.username,
                realName: parsedUser.realName,
                avatarUrl: parsedUser.avatarUrl ? fixAvatarUrl(parsedUser.avatarUrl) : '',
                email: parsedUser.email,
                role: parsedUser.role
              };

              // 从本地存储恢复时也需要检查是否需要语音指引
              console.log('从本地存储恢复用户信息，检查是否需要语音指引')
              return checkNewUser(parsedUser.id).then(isNewUser => {
                if (isNewUser) {
                  console.log('从本地恢复：检测到新用户，将显示全屏欢迎界面和语音指引');
                  // 新用户的全屏欢迎界面和语音指引已在checkNewUser中处理
                } else {
                  // 老用户，显示全屏欢迎界面并播放欢迎回归语音指引
                  console.log('从本地恢复：检测到老用户，准备显示全屏欢迎界面');
                  triggerReturningUserVoiceGuide();
                }
                resolve(userInfo.value);
              }).catch(checkError => {
                console.error('检查用户类型失败，但继续使用本地用户信息:', checkError);
                resolve(userInfo.value);
              });
            } catch (parseError) {
              console.error('解析本地用户信息失败:', parseError);
              reject(error);
            }
          } else {
            reject(error);
          }
        } else {
          // 等待一段时间再重试
          setTimeout(tryFetchUser, 1000);
        }
      });
    }

    tryFetchUser();
  });
}

// 检查头像是否存在
const checkAvatarExists = () => {
  if (!userInfo.value.avatarUrl) return Promise.resolve();

  console.log('Dashboard页面检查头像是否存在:', userInfo.value.avatarUrl);
  return request.get('/api/users/check-avatar').then(res => {
    console.log('头像检查结果:', res);

    if (res && !res.exists) {
      console.warn('头像文件不存在，使用默认头像');
      userInfo.value.avatarUrl = '';
    }
  }).catch(error => {
    console.error('检查头像失败:', error);
    // 接口不存在或其他错误时，不做处理，保留现有头像URL
    return Promise.resolve(); // 确保Promise链继续
  });
}

// 获取用户详细信息用于计算完善度
const fetchUserDetailInfo = async () => {
  try {
    console.log('获取用户详细信息用于计算完善度...')
    
    // 获取基本用户信息
    const basicInfo = await request({
      url: '/api/users/current',
      method: 'get'
    })
    
    if (basicInfo) {
      userDetailInfo.value.avatar = basicInfo.avatarUrl || ''
      userDetailInfo.value.name = basicInfo.realName || basicInfo.username || ''
      userDetailInfo.value.email = basicInfo.email || ''
      userDetailInfo.value.phone = basicInfo.phone || ''
      
      // 获取详细信息
      try {
        const detailInfo = await request({
          url: '/api/users/details',
          method: 'get'
        })
        
        if (detailInfo) {
          userDetailInfo.value.title = detailInfo.jobTitle || ''
          userDetailInfo.value.location = detailInfo.location || ''
          userDetailInfo.value.jobIntention = detailInfo.jobIntention || ''
          userDetailInfo.value.experience = detailInfo.experience || ''
          userDetailInfo.value.education = detailInfo.education || ''
          
          // 处理技能标签
          try {
            if (detailInfo.skills) {
              userDetailInfo.value.skills = JSON.parse(detailInfo.skills)
            } else {
              userDetailInfo.value.skills = []
            }
          } catch (e) {
            console.error('解析技能标签失败:', e)
            userDetailInfo.value.skills = []
          }
        }
      } catch (detailError) {
        console.error('获取用户详细信息失败:', detailError)
        // 如果获取详细信息失败，设置为空值
        userDetailInfo.value.title = ''
        userDetailInfo.value.location = ''
        userDetailInfo.value.jobIntention = ''
        userDetailInfo.value.experience = ''
        userDetailInfo.value.education = ''
        userDetailInfo.value.skills = []
      }
    }
    
    console.log('用户详细信息获取完成:', userDetailInfo.value)
  } catch (error) {
    console.error('获取用户详细信息失败:', error)
  }
}

// 获取用户简历列表
const fetchUserResumeList = async () => {
  try {
    console.log('获取用户简历列表...')
    
    const res = await request({
      url: '/api/resumes',
      method: 'get'
    })
    
    if (res && Array.isArray(res)) {
      userResumeList.value = res.map(item => ({
        id: item.id,
        name: item.name || `简历${item.id}`,
        updateTime: item.updateTime ? new Date(item.updateTime).toLocaleDateString() : '未知',
        type: item.type || 'OTHER'
      }))
      
      console.log('用户简历列表获取完成:', userResumeList.value)
    } else {
      userResumeList.value = []
    }
  } catch (error) {
    console.error('获取简历列表失败:', error)
    userResumeList.value = []
  }
}

// 重置面试状态
function resetInterviewState() {
  interviewStore.resetState()

  // 确保localStorage中的状态也被清除
  localStorage.removeItem('interviewStatus')
  localStorage.removeItem('pendingInterviewJob')
  localStorage.removeItem('interviewSessionId')
  localStorage.removeItem('interviewJob') // 清除可能存在的旧格式数据
  localStorage.removeItem('lastReportId') // 清除报告ID

  console.log('面试状态已重置，localStorage已清除')

  ElMessage.success('已重置面试状态')
  fetchStatistics() // 重新获取统计数据
}

// 根据面试状态获取岗位信息卡片样式
function getJobInfoCardStyle(status) {
  switch(status) {
    case 'report':
      return {
        background: '#f0f9ff', 
        color: '#10b981', 
        border: '1px solid #a7f3d0'
      }
    case 'pending':
    case 'pending1': 
    case 'pending2':
      return {
        background: '#fef3c7', 
        color: '#f59e0b', 
        border: '1px solid #fde68a'
      }
    case 'ongoing':
    case 'completed':
      return {
        background: '#e0f2fe', 
        color: '#0ea5e9', 
        border: '1px solid #bae6fd'
      }
    default:
      return {
        background: '#f8f9ff', 
        color: '#6366f1', 
        border: '1px solid #e0e7ff'
      }
  }
}

// 根据面试状态获取岗位信息标签
function getJobInfoLabel(status) {
  switch(status) {
    case 'report':
      return '已完成面试岗位'
    case 'ongoing':
    case 'completed':
      return '面试中岗位'
    default:
      return '当前面试岗位'
  }
}

// 获取步骤状态
function getStepStatus(stepIndex) {
  const current = currentStep.value
  console.log(`步骤${stepIndex}状态计算: currentStep=${current}`)
  
  if (stepIndex < current) {
    console.log(`步骤${stepIndex}: finish`)
    return 'success' // 已完成的步骤
  } else if (stepIndex === current) {
    console.log(`步骤${stepIndex}: process`)
    return 'process' // 当前步骤
  } else {
    console.log(`步骤${stepIndex}: wait`)
    return 'wait' // 未完成的步骤
  }
}



// 直接创建申请记录
function createJobApplication(jobId) {
  // 确保jobId是整数
  const numericJobId = parseInt(jobId, 10);
  if (isNaN(numericJobId)) {
    console.error('无效的jobId:', jobId);
    return Promise.resolve(null);
  }

  console.log('尝试创建申请记录，岗位ID:', numericJobId);

  // 获取当前用户的简历ID，默认使用最新的简历
  const resumeId = null; // 可以添加逻辑获取用户的resumeId

  // 调用API创建申请记录
  return request({
    url: '/api/job-applications',
    method: 'post',
    data: {
      jobId: numericJobId,
      resumeId: resumeId
    }
  }).then(response => {
    console.log('创建申请记录结果:', response);

    if (response && response.id) {
      ElMessage.success('已自动为您创建投递记录');
      return response.id;
    }

    return null;
  }).catch(error => {
    console.error('创建申请记录失败:', error);

    // 如果是409错误（Conflict），表示用户已经投递过该岗位
    if (error.response && error.response.status === 409) {
      console.log('用户已经投递过该岗位，尝试获取现有申请记录ID');

      // 尝试从用户的所有申请记录中找到匹配的记录
      return request.get('/api/job-applications/user').then(applications => {
        if (Array.isArray(applications)) {
          const matchedApp = applications.find(app => parseInt(app.jobId, 10) === numericJobId);
          if (matchedApp) {
            console.log('找到现有的申请记录:', matchedApp.id);
            return matchedApp.id;
          }
        }
        console.warn('无法找到匹配的申请记录');
        return null;
      }).catch(err => {
        console.error('获取用户申请记录失败:', err);
        return null;
      });
    }

    return null;
  });
}



// 添加一个专门用于获取申请记录ID的函数
function fetchApplicationId(jobId) {
  // 确保jobId是整数类型
  const numericJobId = parseInt(jobId, 10);
  if (isNaN(numericJobId)) {
    console.error('无效的jobId:', jobId);
    return Promise.resolve(null);
  }

  console.log('获取申请记录ID，岗位ID:', numericJobId, '(原始值:', jobId, ')');

  // 查询用户是否已投递该岗位
  return request({
    url: '/api/job-applications/check',
    method: 'get',
    params: { jobId: numericJobId }
  }).then(response => {
    console.log('检查投递状态结果:', response);

    // 检查applied或exists字段，兼容不同的API返回格式
    if (response && (response.applied || response.exists) && response.applicationId) {
      // 已投递过，返回申请记录ID
      console.log('用户已投递该岗位，申请记录ID:', response.applicationId);
      return response.applicationId;
    } else {
      // 未投递过，尝试创建新的申请记录
      console.log('用户未投递该岗位，尝试创建申请记录');
      return createJobApplication(numericJobId);
    }
  }).catch(error => {
    console.error('检查投递状态失败:', error);

    // 如果API不存在或出错，尝试从投递记录中查找
    return request.get('/api/job-applications/user').then(applications => {
      if (Array.isArray(applications)) {
        // 确保比较时类型一致，将jobId转为数字类型
        const matchedApp = applications.find(app => parseInt(app.jobId, 10) === numericJobId);
        if (matchedApp) {
          console.log('从投递记录中找到匹配的申请:', matchedApp.id);
          return matchedApp.id;
        }
      }
      console.warn('未找到与当前岗位匹配的申请记录');
      return null;
    }).catch(() => {
      return null;
    });
  });
}



// 删除这部分残留代码
// ElMessage.success('面试时间已成功设置，状态已更新为待面试');
// timeDialogVisible.value = false;

// 清除所有语音指引相关的会话状态（用于测试或重置）
const clearAllVoiceGuideSession = () => {
  if (!userInfo.value.id) return

  const keysToRemove = [
    `returningUserWelcome_${userInfo.value.id}_session`,
    'currentLoginSession'
  ]

  keysToRemove.forEach(key => {
    sessionStorage.removeItem(key)
  })

  console.log('已清除所有语音指引会话状态')
}

// 强制重置所有语音指引状态（用于调试测试）
const forceResetVoiceGuideStates = () => {
  if (!userInfo.value.id) {
    console.warn('用户未登录，无法重置语音指引状态')
    return
  }

  console.log('🔄 开始强制重置所有语音指引状态...')

  // 清除localStorage中的状态
  localStorage.removeItem(`lastLoginTime_${userInfo.value.id}`)
  localStorage.removeItem(`voiceGuide_newUserWelcome_${userInfo.value.id}`)

  // 清除sessionStorage中的状态
  sessionStorage.removeItem('currentLoginSession')
  sessionStorage.removeItem(`returningUserWelcome_${userInfo.value.id}_session`)

  console.log('✅ 已强制重置所有语音指引状态，下次刷新页面将重新播放语音')
  console.log('📋 请刷新页面测试语音播放功能')
}

// 暴露全局方法供其他页面调用
window.triggerProfileCompleteGuide = () => {
  console.log('收到用户完善资料完成的通知，准备播放指引语音');
  setTimeout(() => {
    VoiceGuideService.playProfileCompleteGuide();
  }, 1000);
};

// 暴露面试报告状态更新方法
window.setInterviewReportReady = () => {
  console.log('收到面试报告生成完成的通知，更新面试状态');
  setReportReady();
};

// 暴露首页统计数据刷新方法
window.refreshDashboardStatistics = () => {
  console.log('收到面试完成通知，刷新首页统计数据');
  refreshStatistics().then(() => {
    console.log('✅ 首页统计数据已刷新');
  }).catch(error => {
    console.error('❌ 刷新首页统计数据失败:', error);
  });
};

// 暴露面试状态管理方法供调试使用
window.interviewStateDebug = {
  // 获取当前状态
  getCurrentState: () => {
    return {
      status: interviewStore?.status,
      job: interviewStore?.job,
      currentStep: currentStep.value,
      pendingJob: pendingJob.value
    };
  },
  
  // 手动设置状态为report（调试用）
  setReportState: () => {
    console.log('手动设置状态为report（调试用）');
    setReportReady();
  },
  
  // 模拟面试报告生成
  simulateReportGenerated: () => {
    console.log('模拟面试报告生成...');
    localStorage.setItem('lastReportId', 'test_report_' + Date.now());
    setReportReady();
  },
  
  // 重置状态
  resetState: () => {
    console.log('重置面试状态（调试用）');
    resetInterviewState();
  },
  
  // 强制清理面试数据（调试用）
  forceClearInterviewData: () => {
    console.log('🧹 强制清理所有面试相关数据（调试用）...');
    clearInterviewDataForNewUser();
    console.log('✅ 面试数据已强制清理完成');
    
    // 刷新页面以确保状态完全重置
    setTimeout(() => {
      console.log('🔄 即将刷新页面以确保状态完全重置');
      window.location.reload();
    }, 1000);
  }
};

// 统计卡片现在只作信息展示，移除点击处理功能

// 页面可见性优化
const handleVisibilityChange = () => {
  if (document.hidden) {
    // 页面隐藏时暂停动画
    document.documentElement.style.setProperty('--animation-play-state', 'paused');
  } else {
    // 页面显示时恢复动画并刷新统计数据
    document.documentElement.style.setProperty('--animation-play-state', 'running');
    
    // 页面重新可见时刷新统计数据（防止从面试页面返回时数据未更新）
    if (userInfo.value.id) {
      console.log('📊 页面重新可见，刷新统计数据');
      setTimeout(() => {
        refreshStatistics().catch(error => {
          console.log('页面可见性刷新统计数据失败:', error);
        });
      }, 500); // 稍微延迟确保页面完全恢复
    }
  }
}

// 键盘导航支持
const handleKeydown = (event) => {
  // Tab键导航优化
  if (event.key === 'Tab') {
    // 确保焦点在可见元素上
    const focusableElements = document.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );

    // 可以添加更多键盘快捷键
    return;
  }

  // 空格键和回车键处理（统计卡片已移除交互功能）
  if (event.key === ' ' || event.key === 'Enter') {
    // 统计卡片现在只作信息展示，不需要激活
  }

  // ESC键关闭模态框等
  if (event.key === 'Escape') {
    // 可以用于关闭打开的对话框
    console.log('ESC键按下');
  }
}

// 错误边界处理
const handleError = (error, context = 'dashboard') => {
  console.error(`${context} 错误:`, error);

  // 错误上报（如果有错误监控服务）
  if (window.errorTracker) {
    window.errorTracker.report(error, { context, page: 'dashboard' });
  }

  // 用户友好的错误提示
  ElMessage.error({
    message: '抱歉，出现了一些问题，请刷新页面重试',
    duration: 5000,
    showClose: true
  });
}

// 网络状态监听
const handleOnline = () => {
  ElMessage.success('网络连接已恢复');
  // 重新获取数据
  fetchStatistics().catch(err => handleError(err, 'network-reconnect'));
}

const handleOffline = () => {
  ElMessage.warning({
    message: '网络连接已断开，部分功能可能受影响',
    duration: 0,
    showClose: true
  });
}

// 性能监控
const performanceObserver = () => {
  if ('PerformanceObserver' in window) {
    try {
      const observer = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        entries.forEach((entry) => {
          if (entry.entryType === 'largest-contentful-paint') {
            console.log('LCP:', entry.startTime);
          }
          if (entry.entryType === 'first-input') {
            console.log('FID:', entry.processingStart - entry.startTime);
          }
        });
      });

      observer.observe({ entryTypes: ['largest-contentful-paint', 'first-input'] });
    } catch (error) {
      console.warn('Performance Observer 不支持:', error);
    }
  }
}

// 暴露语音控制方法
window.voiceGuideControl = {
  stop: () => VoiceGuideService.stopCurrentVoice(),
  isPlaying: () => isPlayingVoice.value,
  playNewUserWelcome: (username) => VoiceGuideService.playNewUserWelcome(username),
  playReturningUserWelcome: (username) => VoiceGuideService.playReturningUserWelcome(username),
  playProfileCompleteGuide: () => VoiceGuideService.playProfileCompleteGuide(),
  clearSession: () => clearAllVoiceGuideSession(), // 用于调试
  forceReset: () => forceResetVoiceGuideStates(), // 强制重置所有状态
  checkStatus: () => {
    // 检查当前状态
    if (!userInfo.value.id) {
      console.log('❌ 用户未登录')
      return
    }

    const userId = userInfo.value.id
    const newUserCompleted = localStorage.getItem(`voiceGuide_newUserWelcome_${userId}`) === 'completed'
    const status = {
      userId,
      username: userInfo.value.username,
      userType: newUserCompleted ? '老用户' : '新用户',
      isNewLoginSession: isNewLoginSession(),
      currentSession: sessionStorage.getItem('currentLoginSession'),
      lastLoginTime: localStorage.getItem(`lastLoginTime_${userId}`),
      returningUserSession: sessionStorage.getItem(`returningUserWelcome_${userId}_session`),
      newUserCompleted: newUserCompleted
    }

    console.table(status)
    console.log('🎭 用户类型:', status.userType)
    console.log('🔄 是否新登录会话:', status.isNewLoginSession)
    return status
  },

  // 模拟新用户状态（用于测试）
  simulateNewUser: () => {
    if (!userInfo.value.id) {
      console.warn('用户未登录，无法模拟新用户状态')
      return
    }

    console.log('🎭 模拟新用户状态...')
    const userId = userInfo.value.id

    // 清除新用户完成标记
    localStorage.removeItem(`voiceGuide_newUserWelcome_${userId}`)
    console.log('✅ 已清除新用户完成标记，用户现在被视为新用户')
    console.log('📋 请刷新页面测试新用户语音播放功能')
  },

  // 检查统计数据状态（调试用）
  checkStatistics: () => {
    console.log('📊 当前统计数据:', statsArr.value)
    console.log('🔍 正在刷新统计数据...')
    return refreshStatistics().then(() => {
      console.log('✅ 统计数据刷新完成:', statsArr.value)
      return statsArr.value
    }).catch(error => {
      console.error('❌ 统计数据刷新失败:', error)
      throw error
    })
  },

  // 手动刷新统计数据
  manualRefresh: () => {
    console.log('🔄 手动刷新统计数据...')
    window.refreshDashboardStatistics()
  },

  // 详细检查统计数据和历史记录
  debugStatistics: async () => {
    console.log('🔍 开始详细检查统计数据和历史记录');
    
    try {
      // 1. 获取历史记录
      await fetchUserApplications();
      console.log('📋 当前历史记录:', historyList.value);
      
      // 2. 手动统计历史记录中的各种状态
      const statusCounts = {
        submitted: 0, pending: 0, interviewed: 0, completed: 0
      };
      
      historyList.value.forEach(item => {
        switch(item.status) {
          case '已投递': statusCounts.submitted++; break;
          case '待面试': statusCounts.pending++; break;
          case '已面试': statusCounts.interviewed++; break;
          case '已完成': statusCounts.completed++; break;
        }
      });
      
      console.log('📊 历史记录统计:', statusCounts);
      
      // 3. 获取API统计数据
      const apiStats = await refreshStatistics();
      console.log('🔗 API统计数据:', apiStats);
      console.log('📈 当前显示统计:', statsArr.value);
      
      // 4. 对比分析
      console.log('🔍 数据对比分析:');
      console.log('历史记录 vs API统计:');
      console.log(`已投递: ${statusCounts.submitted} vs ${statsArr.value[0].value}`);
      console.log(`待面试: ${statusCounts.pending} vs ${statsArr.value[1].value}`);
      console.log(`已面试: ${statusCounts.interviewed} vs ${statsArr.value[2].value}`);
      console.log(`已完成: ${statusCounts.completed} vs ${statsArr.value[3].value}`);
      
      return {
        history: statusCounts,
        api: statsArr.value,
        historyDetails: historyList.value
      };
      
    } catch (error) {
      console.error('❌ 调试统计数据失败:', error);
      return null;
    }
  },

  // 直接检查后端投递记录状态
  checkBackendStatus: async () => {
    console.log('🔍 直接检查后端投递记录状态');
    
    try {
      const response = await request.get('/api/job-applications/user', {
        headers: { 'X-User-ID': String(userInfo.value.id) }
      });
      
      console.log('📋 后端投递记录原始数据:', response);
      
      if (Array.isArray(response)) {
        console.log('📊 后端记录状态统计:');
        const backendCounts = { 1: 0, 2: 0, 3: 0, 4: 0 };
        
        response.forEach((app, index) => {
          console.log(`记录${index + 1}:`, {
            id: app.id,
            jobId: app.jobId,
            status: app.status,
            statusType: typeof app.status,
            submitTime: app.submitTime,
            interviewTime: app.interviewTime
          });
          
          if (typeof app.status === 'number') {
            backendCounts[app.status] = (backendCounts[app.status] || 0) + 1;
          }
        });
        
        console.log('📈 后端状态统计:', {
          '状态1(已投递)': backendCounts[1],
          '状态2(待面试)': backendCounts[2], 
          '状态3(已面试)': backendCounts[3],
          '状态4(已完成)': backendCounts[4]
        });
        
        return response;
      } else {
        console.log('❌ 后端返回数据格式不正确:', response);
        return response;
      }
      
    } catch (error) {
      console.error('❌ 检查后端状态失败:', error);
      return null;
    }
  }
};

</script>

<style scoped>
.dashboard-main {
  width: 100%;
  min-height: 100vh;
  background: transparent;
  padding: 0 0 48px 0;
}
.dashboard-content-wrap {
  max-width: 1440px;
  margin: 0 auto;
  width: 100%;
  padding: 0 20px;
}
.dashboard-banner,
.main-content,
.progress-center,
.dashboard-bottom-row {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
}
.dashboard-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: transparent;
  backdrop-filter: none;
  border: none;
  border-radius: 0;
  box-shadow: none;
  padding: 20px 0;
  margin-bottom: 40px;
  color: #fff;
  position: relative;
  overflow: visible;
  animation: fadeInUp 0.8s ease-out;
  will-change: transform, opacity;
}
.banner-left {
  display: flex;
  align-items: center;
  gap: 28px;
  flex: 1;
  padding: 30px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 20px;
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  margin-top: 0;
  min-width: 460px;
  margin-right: 24px;
  height: 160px;
}

/* 简历完善度区域悬停效果 */
.banner-left:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.35);
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

/* 简历完善度区域微动画 */
.banner-left::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.05), transparent);
  transition: left 0.8s ease;
  z-index: 0;
}

.banner-left:hover::before {
  left: 100%;
}


.banner-avatar-container {
  position: relative;
  width: 80px;
  height: 80px;
  margin-right: 0;
  z-index: 1;
}

.banner-avatar {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  animation: scaleIn 0.6s ease-out 0.2s both;
  will-change: transform;
}
.banner-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  z-index: 1;
  flex: 1;
  min-width: 280px;
}
.banner-welcome {
  font-size: 32px;
  font-weight: 800;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #ffffff 0%, rgba(255, 255, 255, 0.8) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.banner-progress {
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  background: rgba(255, 255, 255, 0.08);
  padding: 12px 18px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  transition: all 0.3s ease;
  min-width: 320px;
  white-space: nowrap;
  flex: 1;
}
.banner-btn {
  margin-left: 12px;
  background: rgba(255, 255, 255, 0.15) !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
  color: #ffffff !important;
  backdrop-filter: blur(15px);
  border-radius: 12px !important;
  padding: 8px 14px !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
  flex-shrink: 0;
  white-space: nowrap;
}
.banner-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  align-items: stretch;
  position: relative;
  min-width: 520px;
  margin-top: 0;
  align-self: center;
  padding: 30px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 20px;
  backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), inset 0 1px 0 rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  overflow: hidden;
  height: 160px;
}

/* 统计卡片容器悬停效果 */
.banner-stats:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.35);
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.3);
}

/* 统计卡片容器微动画 */
.banner-stats::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.05), transparent);
  transition: left 0.8s ease;
  z-index: 0;
}

.banner-stats:hover::before {
  left: 100%;
}
/* 独立统计卡片样式 */
.stat-card {
  min-width: 120px;
  height: 100px;
  border-radius: 16px !important;
  background: rgba(255, 255, 255, 0.08) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15) !important;
  color: #ffffff;
  text-align: center;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.1) !important;
  transition: all 0.3s ease;
  padding: 15px 8px !important;
  position: relative;
  overflow: hidden;
  animation: scaleIn 0.6s ease-out;
  will-change: transform;
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  align-items: center !important;
  z-index: 1;
}

/* 重置Element Plus卡片的默认样式 */
.stat-card :deep(.el-card__body) {
  padding: 0 !important;
  display: flex !important;
  flex-direction: column !important;
  justify-content: center !important;
  align-items: center !important;
  height: 100% !important;
}

/* 卡片悬停效果 */
.stat-card:hover {
  background: rgba(255, 255, 255, 0.15) !important;
  border-color: rgba(255, 255, 255, 0.25) !important;
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.2) !important;
}

/* 卡片微动画 */
.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  transition: left 0.6s ease;
}

.stat-card:hover::before {
  left: 100%;
}
/* 统计卡片只作信息展示，移除交互效果 */
.stat-num {
  font-size: 36px;
  font-weight: 900;
  margin-bottom: 8px;
  color: #ffffff;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  line-height: 1;
  text-align: center;
  width: 100%;
}
.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  text-align: center;
  width: 100%;
  line-height: 1.2;
}
.progress-center {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
  margin-bottom: 60px;
}
.progress-card {
  border-radius: 24px;
  background: #ffffff;
  border: none;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 0 4px 16px rgba(0, 0, 0, 0.04);
  margin-bottom: 60px;
  padding: 40px 40px 32px 40px;
  width: 100%;
  max-width: 1200px;
  position: relative;
  overflow: hidden;
  animation: fadeInUp 1s ease-out 0.3s both;
  will-change: transform, opacity;
}
.progress-header {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 24px;
  color: #5A6B59;
}
.progress-steps-horizontal {
  margin-bottom: 32px;
  width: 100%;
  padding: 0 40px;
}
/* 进度条样式 - 使用更高权重的选择器 */
.progress-steps-horizontal :deep(.el-step__icon) {
  width: 36px !important;
  height: 36px !important;
  font-size: 18px !important;
  background: #ffffff !important;
  border: 2px solid #A8B5A0 !important;
  color: #7A8B79 !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08) !important;
  transition: all 0.3s ease !important;
}

/* 当前进行中的步骤 */
.progress-steps-horizontal :deep(.el-step.is-process .el-step__icon) {
  background: #A8B5A0 !important;
  color: #ffffff !important;
  border: 2px solid #9CAF88 !important;
  box-shadow: 0 4px 16px rgba(168, 181, 160, 0.4) !important;
  animation: pulseStep 1.2s infinite alternate !important;
}

/* 已完成的步骤图标 */
.progress-steps-horizontal :deep(.el-step.is-finish .el-step__icon),
.progress-steps-horizontal :deep(.el-step.is-success .el-step__icon),
.progress-steps-horizontal :deep(.el-step[status="success"] .el-step__icon) {
  background: #10b981 !important;
  color: #ffffff !important;
  border: 2px solid #059669 !important;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4) !important;
}

/* 已完成步骤的连接线 */
.progress-steps-horizontal :deep(.el-step.is-finish .el-step__line),
.progress-steps-horizontal :deep(.el-step.is-success .el-step__line) {
  background: #10b981 !important;
}

.progress-steps-horizontal :deep(.el-step.is-finish .el-step__line-inner),
.progress-steps-horizontal :deep(.el-step.is-success .el-step__line-inner) {
  background: #10b981 !important;
  border-color: #10b981 !important;
}

.progress-steps-horizontal :deep(.el-step.is-finish .el-step__line .el-step__line-inner),
.progress-steps-horizontal :deep(.el-step.is-success .el-step__line .el-step__line-inner) {
  background-color: #10b981 !important;
  border-color: #10b981 !important;
}

/* 额外的样式规则确保覆盖 */
.progress-card .progress-steps-horizontal :deep(.el-step.is-finish .el-step__icon) {
  background: #10b981 !important;
  color: #ffffff !important;
  border-color: #059669 !important;
}

.progress-card .progress-steps-horizontal :deep(.el-step.is-success .el-step__icon) {
  background: #10b981 !important;
  color: #ffffff !important;
  border-color: #059669 !important;
}

/* 强制覆盖Element Plus默认的finish状态样式 */
.el-steps.progress-steps-horizontal :deep(.el-step__icon.is-text) {
  background: #10b981 !important;
  color: #ffffff !important;
  border-color: #059669 !important;
}

/* 使用属性选择器匹配status属性 */
.progress-steps-horizontal :deep(.el-step[status="success"]) .el-step__icon {
  background: #10b981 !important;
  color: #ffffff !important;
  border: 2px solid #059669 !important;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4) !important;
}

/* 全局样式覆盖 */
.dashboard-main .progress-steps-horizontal :deep(.el-step__icon) {
  background: #ffffff;
  border: 2px solid #A8B5A0;
  color: #7A8B79;
}

.dashboard-main .progress-steps-horizontal :deep(.el-step.is-success .el-step__icon) {
  background: #10b981 !important;
  color: #ffffff !important;
  border: 2px solid #059669 !important;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.4) !important;
}
@keyframes pulseStep {
  0% { box-shadow: 0 4px 16px rgba(168, 181, 160, 0.4); }
  100% { box-shadow: 0 8px 24px rgba(168, 181, 160, 0.6); }
}
:deep(.el-step__title) {
  font-size: 16px !important;
  font-weight: 700;
  color: #5A6B59 !important;
}
:deep(.el-step__description) {
  margin-top: 12px;
  margin-bottom: 12px;
}
.step-desc, .step-desc-active {
  display: inline-block;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 12px;
  padding: 14px 24px;
  margin-top: 4px;
  font-size: 14px;
  color: #7A8B79;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  min-width: 180px;
  max-width: 320px;
  line-height: 1.6;
}
.step-desc-active {
  background: #A8B5A0;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 4px 16px rgba(168, 181, 160, 0.3);
  border: 1px solid #9CAF88;
}
.remind-title, .history-title, .tips-header {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #5A6B59;
}
.remind-job, .remind-company, .remind-info {
  font-size: 14px;
  margin-bottom: 8px;
  color: #7A8B79;
}
.remind-info {
  margin-top: 12px;
  color: #6B7D6A;
  font-weight: 500;
  background: #f8f9fa;
  padding: 14px 16px;
  border-radius: 12px;
  border-left: 4px solid #BC9A6A;
  border: 1px solid #e9ecef;
}
.countdown-num {
  color: #BC9A6A;
  font-weight: 700;
}
.history-job {
  font-size: 14px;
  font-weight: 600;
  color: #5A6B59;
}
.history-status {
  font-size: 13px;
  color: #9CAF88;
  margin-bottom: 4px;
}
.tips-list {
  padding-left: 0;
  color: #7A8B79;
  font-size: 14px;
  line-height: 1.8;
  list-style: none;
}
.dashboard-bottom-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  justify-content: center;
  margin: 0 auto 0 auto;
  max-width: 1440px;
  width: 100%;
  padding: 0;
}
.remind-card, .history-card, .tips-card {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 36px 32px 28px 32px;
  border-radius: 20px;
  background: #ffffff;
  border: none;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  color: #6B7D6A;
  position: relative;
  overflow: hidden;
  animation: slideInLeft 0.8s ease-out;
  animation-fill-mode: both;
  will-change: transform, box-shadow;
}
.remind-card:hover, .history-card:hover, .tips-card:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12), 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-4px) scale(1.02);
  background: #ffffff;
  border: 1px solid #e9ecef;
}
@media (max-width: 1200px) {
  .main-content {
    max-width: 100%;
    padding: 0 4px;
  }
  .progress-center, .progress-card {
    max-width: 100%;
    padding: 16px 2px 10px 2px;
  }
  .dashboard-bottom-row {
    flex-direction: column;
    gap: 14px;
    max-width: 100%;
    padding: 0;
  }
  .remind-card, .history-card, .tips-card {
    max-width: 100%;
    min-width: 0;
    padding: 10px 6px 8px 6px;
  }
  .progress-header {
    font-size: 16px;
    margin-bottom: 10px;
  }
  .progress-steps-horizontal {
    padding: 0 2px;
    margin-bottom: 12px;
  }
  .step-desc, .step-desc-active {
    padding: 8px 4px;
    font-size: 13px;
    min-width: 0;
    max-width: 100%;
  }
}
.current-job-info {
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 16px;
  padding: 20px 28px;
  margin-bottom: 24px;
  font-size: 16px;
  color: #6B7D6A;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  gap: 12px;
  position: relative;
  overflow: hidden;
}
.current-job-icon {
  font-size: 22px;
  color: #BC9A6A;
  margin-right: 6px;
}
.current-job-label {
  font-weight: 500;
  color: #7A8B79;
  margin-left: 8px;
}
.current-job-value {
  font-weight: 700;
  color: #5A6B59;
  margin-right: 12px;
}
.progress-card,
.remind-card,
.history-card,
.tips-card {
  width: 100%;
  max-width: none;
  margin: 0;
}
.progress-center,
.dashboard-bottom-row,
.main-content {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
}
.section-gap {
  height: 40px;
  width: 100%;
  background: transparent;
}
.progress-bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: 0;
  flex: 1;
}
.progress-percent {
  font-size: 16px;
  font-weight: 800;
  color: #ffffff;
  margin-left: 12px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  min-width: 45px;
}

.banner-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 0;
  padding: 20px;
  flex-shrink: 0;
  align-self: center;
  width: 80px;
}

.banner-actions .el-button {
  width: 40px !important;
  height: 40px !important;
  margin: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

/* 添加时间选择器提示样式 */
.time-picker-tips {
  background-color: #f0f7ff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 12px;
  border-left: 4px solid #6366f1;
}

.time-picker-tips p {
  margin: 6px 0;
  color: #555;
  display: flex;
  align-items: center;
  gap: 8px;
}

.time-picker-tips .el-icon {
  color: #6366f1;
  font-size: 16px;
}

/* 新用户引导对话框样式 */
.welcome-dialog-content {
  text-align: center;
  padding: 20px 10px;
}

.welcome-icon {
  font-size: 64px;
  color: #6366f1;
  margin-bottom: 16px;
}

.welcome-dialog-content h2 {
  font-size: 24px;
  margin-bottom: 16px;
  color: #333;
}

.welcome-dialog-content p {
  margin: 12px 0;
  color: #555;
  line-height: 1.5;
}

.welcome-dialog-content ul {
  text-align: left;
  padding-left: 20px;
  margin: 16px 0;
}

.welcome-dialog-content li {
  margin: 8px 0;
  color: #666;
}

/* 历史链接样式优化 */
.history-footer {
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e9ecef;
}

.history-footer .el-link {
  color: #A8B5A0 !important;
  font-weight: 500;
}

.history-footer .el-link:hover {
  color: #9CAF88 !important;
}

/* 时间线样式优化 */
:deep(.el-timeline-item__content) {
  color: #6B7D6A !important;
}

:deep(.el-timeline-item__timestamp) {
  color: #9CAF88 !important;
}

:deep(.el-timeline-item__node) {
  background: #A8B5A0 !important;
  border-color: #9CAF88 !important;
}

/* Empty组件样式优化 */
:deep(.el-empty__description p) {
  color: #7A8B79 !important;
}

/* 按钮组优化 */
:deep(.el-button--primary) {
  background: #A8B5A0 !important;
  border: 1px solid #9CAF88 !important;
  color: #ffffff !important;
}

:deep(.el-button--primary:hover) {
  background: #9CAF88 !important;
  transform: translateY(-2px);
}

:deep(.el-button--info) {
  background: #BC9A6A !important;
  border: 1px solid #D2B48C !important;
  color: #ffffff !important;
}

:deep(.el-button--info:hover) {
  background: #D2B48C !important;
}

/* 进度条样式优化 */
:deep(.el-progress-bar__outer) {
  background: rgba(255, 255, 255, 0.15) !important;
  border-radius: 10px !important;
  overflow: hidden;
}

:deep(.el-progress-bar__inner) {
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.9) 0%, rgba(255, 255, 255, 0.7) 100%) !important;
  border-radius: 10px !important;
  position: relative;
}

:deep(.el-progress-bar__inner::after) {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% {
    left: -100%;
    opacity: 0;
  }
  50% {
    opacity: 1;
  }
  100% {
    left: 100%;
    opacity: 0;
  }
}

/* 优化渲染性能的动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 性能友好的悬停动画 */
@keyframes gentlePulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.02);
    opacity: 0.95;
  }
}

/* 添加装饰性背景元素 */
.dashboard-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -20%;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}

.dashboard-banner::after {
  content: '';
  position: absolute;
  bottom: -30%;
  left: -10%;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.05) 0%, transparent 70%);
  border-radius: 50%;
  pointer-events: none;
}



/* 小贴士列表美化 */
.tips-list li {
  position: relative;
  margin-bottom: 12px;
  padding-left: 20px;
}

.tips-list li::before {
  content: '•';
  color: #BC9A6A;
  font-size: 18px;
  position: absolute;
  left: 0;
  top: -2px;
}

.tips-list li:last-child {
  margin-bottom: 0;
}

/* 改进按钮样式 */
:deep(.el-button--large) {
  padding: 14px 32px !important;
  border-radius: 16px !important;
  font-weight: 600 !important;
  backdrop-filter: blur(10px) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

:deep(.el-button--large:hover) {
  transform: translateY(-2px) scale(1.05) !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2) !important;
}

/* 头像悬停效果 */
.banner-avatar {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.banner-avatar:hover {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.3);
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .banner-stats {
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(2, 1fr);
    gap: 16px;
    min-width: auto;
    padding: 16px;
  }
  
  .stat-card {
    min-width: 100px;
    height: auto;
    min-height: 100px;
    padding: 16px 12px !important;
  }
  
  .banner-left {
    padding: 16px;
    gap: 20px;
    min-width: 380px;
    margin-right: 20px;
  }
  
  .banner-progress {
    min-width: 260px;
  }
  
  .banner-info {
    min-width: 220px;
  }
  
  .banner-btn {
    margin-left: 10px !important;
    padding: 6px 12px !important;
  }

  .dashboard-bottom-row {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .dashboard-banner {
    flex-direction: column;
    gap: 24px;
    padding: 32px 24px;
    align-items: stretch;
  }

  .banner-left {
    flex-direction: column;
    text-align: center;
    gap: 20px;
    padding: 20px;
    margin-top: 0;
    min-width: auto;
    margin-right: 0;
  }
  
  .banner-progress {
    justify-content: center;
    align-self: center;
  }
  
  .banner-btn {
    margin-left: 10px !important;
    align-self: center;
  }
  
  .banner-stats {
    margin-top: 0;
    align-self: stretch;
    padding: 16px;
  }
  
  .banner-actions {
    margin-top: 0;
    align-self: stretch;
  }
}

/* 图标颜色统一 */
.remind-title .el-icon,
.history-title .el-icon,
.tips-header .el-icon {
  color: #BC9A6A;
}

/* 加载状态优化 */
.dashboard-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to bottom, #557F93 0%, #BFAFA1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  opacity: 1;
  transition: opacity 0.5s ease-out;
}

.dashboard-loading.fade-out {
  opacity: 0;
  pointer-events: none;
}

/* 焦点管理和可访问性 */
.remind-card:focus,
.history-card:focus,
.tips-card:focus {
  outline: 2px solid #A8B5A0;
  outline-offset: 2px;
}

/* 统计卡片不再需要焦点样式 */
.stat-card {
  outline: none;
}

/* 打印样式优化 */
@media print {
  .dashboard-banner,
  .banner-actions,
  .banner-btn {
    display: none !important;
  }

  .progress-card,
  .remind-card,
  .history-card,
  .tips-card {
    break-inside: avoid;
    box-shadow: none !important;
    border: 1px solid #ccc !important;
  }
}

/* 高对比度模式支持 */
@media (forced-colors: active) {
  .dashboard-banner,
  .progress-card,
  .remind-card,
  .history-card,
  .tips-card {
    border: 2px solid !important;
    box-shadow: none !important;
  }

  .stat-card {
    border: 2px solid !important;
  }
}

/* 减少动画偏好支持 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* 深色模式优化（预留） */
@media (prefers-color-scheme: dark) {
  .progress-card,
  .remind-card,
  .history-card,
  .tips-card {
    background: rgba(255, 255, 255, 0.95) !important;
  }
}

/* 悬停设备优化 */
@media (hover: hover) {
  .stat-card:hover {
    animation: gentlePulse 2s infinite;
  }
}

/* 触摸设备优化 */
@media (hover: none) {
  .stat-card:active {
    transform: scale(0.98);
    transition-duration: 0.1s;
  }
}

/* 完善响应式 */
@media (max-width: 768px) {
  .progress-card,
  .remind-card,
  .history-card,
  .tips-card {
    margin: 0 10px;
    padding: 24px 20px;
  }

  .step-desc,
  .step-desc-active {
    min-width: 120px;
    max-width: 250px;
    padding: 10px 16px;
    font-size: 13px;
  }

  .banner-stats {
    grid-template-columns: repeat(2, 1fr) !important;
    grid-template-rows: repeat(2, 1fr) !important;
    gap: 12px;
    min-width: auto !important;
    padding: 12px !important;
  }
  
  .stat-card {
    min-width: 90px;
    padding: 12px 8px !important;
  }
  
  .banner-left {
    padding: 16px !important;
    gap: 18px !important;
    min-width: auto !important;
    margin-right: 0 !important;
  }
  
  .banner-progress {
    min-width: 220px !important;
    padding: 10px 16px !important;
    gap: 12px !important;
  }
  
  .banner-info {
    min-width: auto !important;
  }
  
  .banner-btn {
    margin-left: 8px !important;
    padding: 6px 10px !important;
    font-size: 14px !important;
  }
  


  .dashboard-banner {
    padding: 24px 20px !important;
  }

  .banner-actions {
    justify-content: center;
    padding: 0 10px;
    margin-top: 0;
    align-self: stretch;
  }
}

/* 超小屏幕优化 */
@media (max-width: 480px) {
  .dashboard-content-wrap {
    padding: 0 10px;
  }

  .banner-stats {
    grid-template-columns: 1fr 1fr !important;
    grid-template-rows: 1fr 1fr !important;
    gap: 8px;
    min-width: auto !important;
    padding: 10px !important;
  }

  .stat-card {
    min-width: 80px;
    height: auto !important;
    min-height: 80px !important;
    padding: 10px 6px !important;
  }
  
  .banner-left {
    padding: 12px !important;
    gap: 14px !important;
    min-width: auto !important;
    margin-right: 0 !important;
  }
  
  .banner-welcome {
    font-size: 24px !important;
  }
  
  .banner-progress {
    min-width: 200px !important;
    font-size: 14px !important;
    padding: 8px 12px !important;
    gap: 10px !important;
  }
  
  .banner-info {
    min-width: auto !important;
  }
  
  .banner-btn {
    margin-left: 6px !important;
    padding: 5px 8px !important;
    font-size: 13px !important;
  }
  


  .stat-num {
    font-size: 24px !important;
  }

  .banner-actions {
    gap: 8px;
    justify-content: center;
    margin-top: 0;
    align-self: stretch;
  }
}

/* 性能优化 */
.dashboard-main {
  contain: layout style paint;
}

.stat-card,
.progress-card,
.remind-card,
.history-card,
.tips-card {
  contain: layout style;
}

/* GPU加速优化 */
.dashboard-banner,
.stat-card,
.progress-card,
.remind-card,
.history-card,
.tips-card {
  transform: translateZ(0);
  backface-visibility: hidden;
}

/* 滚动优化 */
.dashboard-main {
  scroll-behavior: smooth;
}

/* 文本选择优化 */
.stat-num,
.stat-label {
  user-select: none;
}

.remind-info,
.history-job,
.tips-list {
  user-select: text;
}

/* 继续投递按钮增强样式 */
:deep(.el-button--success.el-button--large) {
  background: linear-gradient(45deg, #10b981, #34d399) !important;
  border: none !important;
  color: #ffffff !important;
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  transform: translateZ(0) !important;
  position: relative !important;
  overflow: hidden !important;
}

:deep(.el-button--success.el-button--large:hover) {
  background: linear-gradient(45deg, #059669, #10b981) !important;
  transform: translateY(-3px) scale(1.05) !important;
  box-shadow: 0 12px 30px rgba(16, 185, 129, 0.6) !important;
}

:deep(.el-button--success.el-button--large:active) {
  transform: translateY(-1px) scale(1.02) !important;
  box-shadow: 0 8px 25px rgba(16, 185, 129, 0.5) !important;
}

/* 继续投递按钮闪烁动画 */
:deep(.el-button--success.el-button--large::before) {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.8s ease;
  z-index: 1;
}

:deep(.el-button--success.el-button--large:hover::before) {
  left: 100%;
}

/* 继续投递按钮脉冲动画 */
@keyframes continueButtonPulse {
  0% {
    box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
  }
  50% {
    box-shadow: 0 8px 25px rgba(16, 185, 129, 0.6);
  }
  100% {
    box-shadow: 0 6px 20px rgba(16, 185, 129, 0.4);
  }
}

:deep(.el-button--success.el-button--large) {
  animation: continueButtonPulse 2s infinite ease-in-out;
}


</style>
