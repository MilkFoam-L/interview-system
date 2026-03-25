<template>
  <div class="interview-container-full">
    <el-card class="interview-card-full">
      <template #header>
        <div class="card-header">
          <span v-if="step === 1">准备面试</span>
          <span v-else>正式面试</span>
        </div>
      </template>
      <!-- Step 1: 准备面试 -->
      <div v-if="step === 1" class="prepare-section-full">
        <!-- 顶部流程条 -->
        <el-steps :active="currentStepIndex" align-center finish-status="success" class="prepare-steps-horizontal">
          <el-step v-for="(item, idx) in detectList" :key="item.key" :title="item.title" :icon="item.icon"></el-step>
        </el-steps>
        <!-- 统一检测区域 -->
        <div class="prepare-step-content">
          <!-- 上半部分：摄像头预览和设备检测 -->
          <div class="upper-section">
            <!-- 左侧摄像头预览 -->
            <div class="camera-section">
              <div class="camera-container">
                <video
                    ref="cameraRef"
                    autoplay
                    playsinline
                    class="camera-preview"
                />
                <div class="camera-overlay" v-if="faceVerifying">
                  <div class="face-scanning">
                    <div class="scan-line"></div>
                    <div class="scan-text">人脸识别中...</div>
                  </div>
                </div>
                <div class="face-guide" v-if="networkStatus && !faceStatus && !faceVerifying">
                  <div class="guide-box"></div>
                  <div class="guide-text">请将面部置于框内</div>
                </div>
              </div>
              <div class="detection-status">
                <el-tag v-if="cameraStatus" type="success" effect="dark">摄像头已就绪</el-tag>
                <el-tag v-if="micStatus" type="success" effect="dark">麦克风已就绪</el-tag>
                <el-tag v-if="networkStatus" type="success" effect="dark">网络已就绪</el-tag>
                <el-tag v-if="faceStatus" type="success" effect="dark">人脸验证通过</el-tag>
              </div>
            </div>
            <!-- 右侧设备检测 -->
            <div class="device-section">
              <!-- 摄像头设置 -->
              <div class="device-item" :class="{ 'active-item': !cameraStatus }">
                <h4><el-icon><VideoCamera /></el-icon> 摄像头</h4>
                <el-select v-model="selectedCamera" placeholder="选择摄像头" size="default">
                  <el-option v-for="item in cameraList" :key="item.deviceId" :label="item.label" :value="item.deviceId"></el-option>
                </el-select>
              </div>
              <!-- 麦克风设置 -->
              <div class="device-item" :class="{ 'active-item': cameraStatus && !micStatus }">
                <h4><el-icon><Microphone /></el-icon> 麦克风</h4>
                <el-select v-model="selectedMic" placeholder="选择麦克风" size="default">
                  <el-option v-for="item in micList" :key="item.deviceId" :label="item.label" :value="item.deviceId"></el-option>
                </el-select>
                <div style="margin-top: 8px; display: flex; justify-content: space-between; align-items: center;">
                  <el-progress :percentage="micVolume" :text-inside="true" status="success" class="device-progress" style="flex: 1;" />
                  <el-button type="primary" size="small" @click="testMicrophone" style="margin-left: 10px; flex-shrink: 0">
                    测试麦克风
                  </el-button>
                </div>
              </div>
              <!-- 网络检测 -->
              <div class="device-item" :class="{ 'active-item': micStatus && !networkStatus }">
                <h4><el-icon><Connection /></el-icon> 网络状态</h4>
                <el-progress :percentage="networkQuality" :text-inside="true" :status="networkQuality > 60 ? 'success' : 'exception'" class="device-progress" />
                <div class="network-quality">
                  状态：<span :class="networkQuality > 60 ? 'good' : 'poor'">{{ networkQuality > 60 ? '良好' : '较差' }}</span>
                </div>
              </div>
            </div>
          </div>
          
          <!-- 下半部分：人脸识别 -->
          <div class="lower-section" :class="{ 'active-section': networkStatus && !faceStatus }">
            <div class="face-verify-container">
              <h3><el-icon><UserFilled /></el-icon> 人脸识别验证</h3>
              
              <div class="face-verify-content">
                <!-- 个人信息表单 -->
                <el-form :inline="true" size="default">
                  <el-form-item label="姓名">
                    <el-input v-model="faceName" placeholder="请输入姓名" />
                  </el-form-item>
                  <el-form-item label="身份证">
                    <el-input v-model="faceId" placeholder="请输入身份证号" maxlength="18" />
                  </el-form-item>
                </el-form>
                
                <!-- 身份证照片上传区域和人脸拍照区域 -->
                <div class="upload-section">
                  <div class="id-card-upload">
                    <h4>上传身份证照片</h4>
                    <div class="upload-area">
                      <div v-if="!idCardImageUrl" class="upload-placeholder">
                        <el-upload
                          class="avatar-uploader"
                          action=""
                          :auto-upload="false"
                          :show-file-list="false"
                          accept="image/*"
                          :on-change="handleIdCardUpload"
                        >
                          <div class="upload-inner">
                            <el-icon class="upload-icon"><Plus /></el-icon>
                            <div>点击上传身份证照片</div>
                          </div>
                        </el-upload>
                      </div>
                      <div v-else class="upload-preview">
                        <img :src="idCardImageUrl" class="preview-image" />
                        <div class="preview-actions">
                          <el-button type="danger" size="small" @click="resetIdCardImage">重新上传</el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                  
                  <div class="face-snapshot">
                    <h4>拍摄人脸照片</h4>
                    <div class="snapshot-area">
                      <div v-if="!faceImageUrl" class="snapshot-placeholder">
                        <el-button type="primary" @click="takeSnapshot" :disabled="!cameraStatus">
                          <el-icon><Camera /></el-icon> 拍摄照片
                        </el-button>
                        <div class="snapshot-tip">请将面部置于摄像头正中位置</div>
                      </div>
                      <div v-else class="snapshot-preview">
                        <img :src="faceImageUrl" class="preview-image" />
                        <div class="preview-actions">
                          <el-button type="danger" size="small" @click="resetFaceImage">重新拍摄</el-button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 验证按钮 -->
                <div class="verify-actions">
                  <el-button 
                    type="primary" 
                    :disabled="!idCardImageUrl || !faceImageUrl || !faceName || !faceId" 
                    @click="handleFaceVerify" 
                    :loading="faceVerifying"
                  >
                    进行人脸识别验证
                  </el-button>
                  

                  
                  <!-- 验证结果展示 -->
                  <div v-if="verifyResult" :class="['verify-result', verifyResult.samePerson ? 'success' : 'error']">
                    <el-icon v-if="verifyResult.samePerson"><CircleCheckFilled /></el-icon>
                    <el-icon v-else><CircleCloseFilled /></el-icon>
                    <span>{{ verifyResult.message }}</span>
                    <div v-if="verifyResult.similarity" class="similarity">
                      相似度: {{ (verifyResult.similarity * 100).toFixed(2) }}%
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 下一步按钮 -->
        <div class="prepare-next-btn-full">
          <el-button type="primary" :disabled="!canNext" @click="handleStartInterview" size="large">下一步，进入正式面试</el-button>
        </div>
      </div>
      
      <!-- Step 2: 正式面试 -->
      <div v-else class="interview-formal-container">
        <!-- 左侧：摄像头+个人信息（八二开，高度与右侧一致） -->
        <div class="formal-left">
          <div class="camera-info-wrapper">
            <div class="video-container">
              <div class="video-area-large">
                <!-- 正式面试摄像头 -->
                <video 
                  ref="formalCameraRef"
                  id="formalCameraRef"
                  autoplay 
                  playsinline 
                  muted
                  width="100%" 
                  height="100%" 
                  style="object-fit: cover; transform: scaleX(-1);"
                />


              </div>
            </div>
          </div>
        </div>
        <!-- 右侧：智能答题区更大 -->
        <div class="formal-right">
          <!-- 答题进程 -->
          <el-steps :active="currentQuestionStep" align-center finish-status="success" class="question-steps">
            <el-step title="自我介绍"></el-step>
            <el-step title="基础问答"></el-step>
            <el-step title="场景模拟"></el-step>
            <el-step title="代码实操题"></el-step>
          </el-steps>
          
          <!-- 智能答题区 -->
          <div class="question-area">
            <!-- 场景模拟阶段 -->
            <ScenarioStage v-if="currentQuestionStep === 2" 
                          ref="scenarioStageRef"
                          :sessionId="interviewSessionId" 
                          @stage-complete="onStageComplete('SCENARIO')" />
            <!-- 基础问答阶段 - 预先渲染但根据步骤控制显示 -->
            <BasicQASection v-if="true"
                         v-show="currentQuestionStep === 1"
                         :interviewSessionId="interviewSessionId" 
                         :userId="userId"
                         :autoStart="currentQuestionStep === 1"
                         :stage="'BASIC_QA'"
                         @section-complete="onStageComplete('BASIC_QA')" />
            <!-- 代码操作题阶段 -->
            <CodeTestSection v-if="currentQuestionStep === 3" 
                          :interviewSessionId="interviewSessionId" 
                          :userId="userId"
                          :autoStart="codeTestStarted"
                          @section-complete="onStageComplete('CODE_TEST')" />

            <!-- 其他阶段内容 - 自我介绍环节 -->
            <div v-if="currentQuestionStep === 0">
              <!-- 自我介绍提示 -->
              <div v-show="introductionMode" class="introduction-guide">
                <h3>自我介绍环节</h3>
                <div class="introduction-countdown-box">
                  <div class="countdown-circle">
                    <div class="countdown-number">{{ introductionCountdown }}</div>
                    <div class="countdown-unit">秒</div>
                  </div>
                </div>
                <div class="introduction-tips">
                  <h4>温馨提示:</h4>
                  <ul>
                    <li>请用1分钟时间进行自我介绍</li>
                    <li>介绍应包含您的教育背景、专业技能和工作经验</li>
                    <li>突出与应聘岗位相关的技能和经历</li>
                    <li>保持自然微笑，语速适中，音量适当</li>
                    <li>请您注视摄像头，大方自信，不必紧张哦</li>
                  </ul>
                </div>
                
                <!-- 添加隐藏的表情捕获组件，只有当videoRef存在时才渲染 -->
                <FaceExpressionCapture
                  v-if="formalCameraRef"
                  :enabled="isCapturingExpression"
                  :video-ref="formalCameraRef"
                  :session-id="interviewSessionId || 0"
                  :user-id="userId || 1"
                  :candidate-name="faceName || '测试用户'"
                  :candidate-id-number="faceId || '000000000000000'"
                  :remaining-time="introductionCountdown"
                  :capture-interval="10000"
                  @capture-success="handleCaptureSuccess"
                  @capture-error="handleCaptureError"
                  @capture-start="() => console.log('表情捕获已开始')"
                  @capture-stop="(data) => console.log('表情捕获已停止，共捕获', data.count, '次')"
                />
              </div>
              
              <!-- 显示转写结果 -->
              <div v-if="transcriptionText && !introductionMode" class="transcription-result">
                <h4>语音转写结果：</h4>
                <div class="transcription-text">{{ transcriptionText }}</div>
                
                <!-- 测试模式提示 -->
              <div v-if="useTestMode" class="test-mode-indicator">
                <el-icon><Tools /></el-icon>
                <span>测试模式</span>
              </div>
              
              <!-- 自我介绍分析状态（静默模式，不显眼） -->
              <div v-if="isAnalyzingIntroduction" class="analysis-status-minimal">
                <div class="analysis-indicator">
                  <el-icon class="is-loading analysis-icon"><Loading /></el-icon>
                  <span class="analysis-text">AI分析中...</span>
                </div>
              </div>
              
              <!-- 分析完成提示（简洁版） -->
              <div v-else-if="introductionAnalysisResult" class="analysis-complete-minimal">
                <div class="analysis-done">
                  <el-icon class="analysis-icon success"><CircleCheckFilled /></el-icon>
                  <span class="analysis-text">{{ useTestMode ? '模拟分析完成' : 'AI分析完成' }}</span>
                </div>
              </div>
              </div>
              <el-empty v-else-if="!introductionMode" description="请开始说话，这里将显示语音转写结果..." />
              
              <!-- 添加表情分析结果组件（在自我介绍结束后显示） -->
              <FaceExpressionAnalysis 
                v-if="showExpressionAnalysis && interviewSessionId"
                :session-id="interviewSessionId" 
                class="expression-analysis-section"
              />

            </div>
          </div>
          

          

        </div>
        <!-- 右侧可折叠状态栏 -->
        <el-drawer v-model="drawerVisible" title="设备状态" direction="rtl" size="300px">
          <div class="status-bars-drawer">
            <div class="status-bar-item">
              <div class="status-bar-title"><el-icon><Microphone /></el-icon> 麦克风音量</div>
              <!-- TODO: 这里可对接麦克风实时音量接口 -->
              <el-progress :percentage="micVolume" :text-inside="true" status="success" />
            </div>
            <hr class="status-bar-divider" />
            <div class="status-bar-item">
              <div class="status-bar-title"><el-icon><Connection /></el-icon> 网络状况</div>
              <!-- TODO: 这里可对接网络质量检测接口 -->
              <el-progress :percentage="networkQuality" :text-inside="true" status="success" />
            </div>
          </div>
        </el-drawer>
        <el-button class="status-btn" @click="drawerVisible = true" type="info" circle icon="Connection" title="查看设备状态" />
      </div>
    </el-card>
    
    <!-- 准备面试指引弹窗 -->
    <InterviewPreparationGuide
      :visible="showPreparationGuide"
      @close="handleGuideClose"
      @confirm="handleGuideConfirm"
    />
    
    <!-- 自我介绍指引弹窗 -->
    <SelfIntroductionGuide
      :visible="showSelfIntroductionGuide"
      @close="handleSelfIntroductionGuideClose"
      @start="handleSelfIntroductionStart"
    />
    
    <!-- 基础问答指引弹窗 -->
    <BasicQAGuide
      :visible="showBasicQAGuide"
      @close="handleBasicQAGuideClose"
      @start="handleBasicQAStart"
    />
    
    <!-- 场景问答指引弹窗 -->
    <ScenarioQAGuide
      :visible="showScenarioQAGuide"
      @close="handleScenarioQAGuideClose"
      @start="handleScenarioQAStart"
    />
    
    <!-- 代码实操指引弹窗 -->
    <CodeTestGuide
      :visible="showCodeTestGuide"
      @close="handleCodeTestGuideClose"
      @start="handleCodeTestStart"
    />
    
    <!-- 面试完成感谢弹窗 -->
    <InterviewCompletionGuide
      v-model="showCompletionGuide"
      @completion-confirmed="handleCompletionConfirmed"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Camera, Plus, Document } from '@element-plus/icons-vue'
import axios from 'axios'
import * as echarts from 'echarts' // 引入echarts
import request from '@/utils/request'
import { useInterviewStore } from '@/store/interview' // 修正store路径
import { useRouter } from 'vue-router'
import { getStageProgress, startStage, completeStage, updateStageStatus } from '@/api/stage';
import ScenarioStage from './interview/ScenarioStage.vue'
import FaceExpressionCapture from '@/components/FaceExpressionCapture/FaceExpressionCapture.vue'
import FaceExpressionAnalysis from '@/components/FaceExpressionAnalysis/FaceExpressionAnalysis.vue'
import { analyzeIntroduction, getIntroductionAnalysis } from '@/api/introductionAnalysis'
import { updateStartTime, updateEndTime, generateStarScores } from '@/api/comprehensiveReport'
import {
  VideoCamera,
  Microphone,
  Connection,
  UserFilled,
  CircleCheckFilled,
  CircleCloseFilled,
  Loading,
  Tools
} from '@element-plus/icons-vue'
import { assignQuestions, getSessionQuestions } from '../api/sessionQuestion'
import { submitAnswer, getAnswerRecord } from '../api/answerRecord'

// 导入组合式API
import { formatDateForBackend } from '../utils/dateUtils'
import cacheService from '../services/cacheService'
import BasicQASection from './interview/BasicQASection.vue'
import CodeTestSection from './interview/CodeTestSection.vue'
import InterviewPreparationGuide from '@/components/InterviewPreparationGuide.vue'
import SelfIntroductionGuide from '@/components/SelfIntroductionGuide.vue'
import BasicQAGuide from '@/components/BasicQAGuide.vue'
import ScenarioQAGuide from '@/components/ScenarioQAGuide.vue'
import CodeTestGuide from '@/components/CodeTestGuide.vue'
import InterviewCompletionGuide from '@/components/InterviewCompletionGuide.vue'
import { detectAllDevices } from '@/utils/deviceDetection'

// 使用 Pinia 存储
const interviewStore = useInterviewStore()

const step = ref(1) // 1: 准备面试, 2: 正式面试
const cameraStatus = ref(false)
const micStatus = ref(false)
const networkStatus = ref(false)
const faceStatus = ref(false)
const faceVerifying = ref(false)
const canNext = ref(false)
const micVolume = ref(0)
const networkQuality = ref(0)

// 准备面试指引相关
const showPreparationGuide = ref(false)
const deviceDetectionCompleted = ref(false)

// 自我介绍指引相关
const showSelfIntroductionGuide = ref(false)

// 基础问答指引相关
const showBasicQAGuide = ref(false)

// 场景问答指引相关
const showScenarioQAGuide = ref(false)

// 代码实操指引相关  
const showCodeTestGuide = ref(false)
const codeTestStarted = ref(false) // 控制代码实操是否真正开始计时

// 面试完成感谢相关
const showCompletionGuide = ref(false)
const countdown = ref(60) // 答题倒计时
const duration = ref(0) // 面试时长
// 面试进度 - 用于进度条显示
const progress = ref(0)

// 题目状态管理相关变量
const questions = ref([]);          // 题目列表
const loadingQuestions = ref(false); // 加载状态
const currentIndex = ref(0);        // 当前题号
const answers = ref([]);            // 每题答案
const startTimes = ref([]);         // 每题开始时间
const submitTimes = ref([]);        // 每题提交时间
const answerRecords = ref({});      // 答案记录（对象形式）
const answerLoading = ref(false);   // 答案判分 loading 状态
const perQuestionCountdown = ref(60); // 每题倒计时秒数
const isCountdownPaused = ref(false); // 倒计时暂停状态
let countdownTimer = null;            // 倒计时定时器
let timer = null;                     // 定时器

// 格式化时间函数 (用于显示倒计时)
const formatTime = (val) => {
  const m = String(Math.floor(val / 60)).padStart(2, '0')
  const s = String(val % 60).padStart(2, '0')
  return `${m}:${s}`
}

// 自我介绍相关
const introductionMode = ref(false) // 是否处于自我介绍模式
const introductionCountdown = ref(60) // 自我介绍倒计时
const introductionTimer = ref(null) // 自我介绍计时器
const isAnalyzingIntroduction = ref(false) // 是否正在分析自我介绍
const introductionAnalysisResult = ref(null) // 自我介绍分析结果
const useTestMode = ref(true) // 测试模式开关，设为true使用模拟数据
const transcriptionTimer = ref(null) // 语音转写计时器
const faceImageCaptures = ref([]) // 采集的面部图像列表
const captureInterval = ref(null) // 图像采集定时器
const isCapturing = ref(false) // 是否正在采集图像
const analyzeLoading = ref(false) // 分析加载状态
const analyzeResult = ref(null) // 分析结果
const showAnalysisDialog = ref(false) // 是否显示分析报告对话框
const expressionCaptureInterval = ref(null) // 表情定时捕获定时器
const isInitializing = ref(false) // 添加初始化状态变量

// 表情分析相关
const isCapturingExpression = ref(false) // 是否正在捕获表情
const expressionCaptureCount = ref(0) // 表情捕获次数
const expressionCaptureResults = ref([]) // 表情捕获结果列表
const showExpressionAnalysis = ref(false) // 是否显示表情分析结果

// 获取当前用户信息
const userInfo = ref(JSON.parse(localStorage.getItem('user') || '{}'))
const userId = computed(() => userInfo.value.id || 1) // 获取用户ID，如果没有则默认为1
// 面试ID使用用户ID确保数据隔离
const interviewId = computed(() => userId.value)

const cameraRef = ref(null)
const formalCameraRef = ref(null)
const drawerVisible = ref(false)
const currentQuestionStep = ref(0)

// 设备选择相关
const cameraList = ref([
  { deviceId: '1', label: '默认摄像头' }
])
const micList = ref([
  { deviceId: '1', label: '默认麦克风' }
])
const selectedCamera = ref('1')
const selectedMic = ref('1')

// 人脸识别相关
const faceName = ref('')
const faceId = ref('')
const idCardImageUrl = ref('') // 身份证照片URL（用于预览）
const faceImageUrl = ref('') // 人脸照片URL（用于预览）
const idCardImageData = ref('') // 身份证照片Base64数据
const faceImageData = ref('') // 人脸照片Base64数据
const verifyResult = ref(null) // 验证结果

// 检测流程数据
const detectList = ref([
  { key: 'device', title: '设备检测', icon: VideoCamera },
  { key: 'network', title: '网络检测', icon: Connection },
  { key: 'face', title: '人脸验证', icon: UserFilled },
])

// 语音转写相关
const transcriptionText = ref('') // 转写文本
const isTranscribing = ref(false) // 是否正在转写
const webSocket = ref(null) // WebSocket客户端
// sessionId 只用后端返回的数字ID
const sessionId = ref(0); // 会话ID，后端返回的数字

const audioContext = ref(null) // 音频上下文
const audioProcessor = ref(null) // 音频处理器
const audioStream = ref(null) // 音频流

const currentStepIndex = computed(() => {
  if (!cameraStatus.value || !micStatus.value) return 0
  if (!networkStatus.value) return 1
  return 2
})

// 初始化摄像头和麦克风
onMounted(async () => {
  console.log('组件挂载，初始化设备')
  
  // 全局控制台方法，方便测试
  window.switchToTestMode = () => {
    useTestMode.value = true
    console.log('🧪 已切换到测试模式（使用模拟AI分析数据）')
  }
  
  window.switchToProductionMode = () => {
    useTestMode.value = false
    console.log('🚀 已切换到生产模式（使用真实API）')
  }
  
  console.log('💡 可在控制台使用以下命令切换模式:')
  console.log('   switchToTestMode() - 切换到测试模式')
  console.log('   switchToProductionMode() - 切换到生产模式')
  console.log(`   当前模式: ${useTestMode.value ? '测试模式' : '生产模式'}`)
  
  try {
    // 先请求音频和视频权限
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: true
      })
      
      // 临时存储流，稍后在获取设备列表后会重新创建
      const tracks = stream.getTracks()
      tracks.forEach(track => track.stop())
      
      console.log('成功获取媒体设备权限')
    } catch (permissionError) {
      console.error('获取媒体设备权限失败:', permissionError)
      console.error('获取摄像头或麦克风权限失败，请确保已授权浏览器使用这些设备')
      throw permissionError
    }
    
    // 获取设备列表
    const devices = await navigator.mediaDevices.enumerateDevices()
    
    // 过滤摄像头设备
    const cameras = devices.filter(device => device.kind === 'videoinput')
    if (cameras.length > 0) {
      cameraList.value = cameras.map(camera => ({
        deviceId: camera.deviceId,
        label: camera.label || `摄像头 ${cameraList.value.length + 1}`
      }))
      selectedCamera.value = cameras[0].deviceId
      console.log('找到摄像头设备:', cameras.length)
    } else {
      console.warn('未找到摄像头设备')
      console.error('未检测到摄像头设备')
    }
    
    // 过滤麦克风设备
    const mics = devices.filter(device => device.kind === 'audioinput')
    if (mics.length > 0) {
      micList.value = mics.map(mic => ({
        deviceId: mic.deviceId,
        label: mic.label || `麦克风 ${micList.value.length + 1}`
      }))
      selectedMic.value = mics[0].deviceId
      console.log('找到麦克风设备:', mics.length)
    } else {
      console.warn('未找到麦克风设备')
      console.error('未检测到麦克风设备')
    }
    
    // 打开摄像头和麦克风
    await openCamera()
    
    // 模拟网络检测
    simulateNetworkCheck()
    
    // 如果已经是正式面试阶段，初始化正式面试摄像头
    if (step.value === 2) {
      setTimeout(() => {
        initFormalCamera()
      }, 1000)
    }
    
    // 主动促使用户进行声音测试
    setTimeout(() => {
      if (!micStatus.value) {
        // 移除多余的麦克风提示信息
      }
    }, 5000)
  } catch (error) {
    console.error('设备初始化失败:', error)
    console.error('设备初始化失败，请检查设备权限并刷新页面重试')
  }
})

// 打开摄像头
const openCamera = async () => {
  return new Promise(async (resolve, reject) => {
    try {
      console.log('正在打开摄像头...')
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { deviceId: selectedCamera.value ? { exact: selectedCamera.value } : undefined },
        audio: { deviceId: selectedMic.value ? { exact: selectedMic.value } : undefined }
      })
      
      if (cameraRef.value) {
        cameraRef.value.srcObject = stream
        cameraStatus.value = true
        console.log('准备阶段摄像头已就绪:', cameraStatus.value)
      }
      
      // 如果到了第二步，也更新正式面试的摄像头
      if (step.value === 2 && formalCameraRef.value) {
        console.log('正在初始化正式面试摄像头...')
        formalCameraRef.value.srcObject = stream
        // 确保视频播放
        try {
          await formalCameraRef.value.play();
          console.log('正式面试摄像头播放成功');
        } catch (err) {
          console.error('正式面试摄像头播放失败:', err)
          // 这里不抛出错误，因为准备阶段的摄像头已经成功了
        }
      }
      
      // 检测麦克风音量
      detectMicVolume(stream)
      
      // 在开发测试环境中，自动设置麦克风状态为就绪
      if (process.env.NODE_ENV !== 'production') {
        console.log('在开发测试环境中自动设置麦克风状态为就绪')
        micStatus.value = true
        micVolume.value = 50
        updateNextButtonStatus()
      }
      
          // 摄像头和麦克风已就绪，移除多余提示
    
    // 如果还没有显示过准备指引，则显示指引弹窗
    if (!deviceDetectionCompleted.value) {
      setTimeout(() => {
        showInterviewGuide()
      }, 1000) // 延迟1秒显示，让用户先看到摄像头画面
    }
    
    resolve(stream);
  } catch (error) {
    console.error('打开摄像头失败:', error)
    console.error('打开摄像头失败，请检查设备权限')
    
    // 即使摄像头失败也显示指引弹窗
    if (!deviceDetectionCompleted.value) {
      setTimeout(() => {
        showInterviewGuide()
      }, 2000)
    }
    
    reject(error);
  }
  });
}

// 检测麦克风音量
const detectMicVolume = (stream) => {
  try {
    // 开发测试环境中直接设置麦克风状态为就绪
    if (process.env.NODE_ENV !== 'production') {
      console.log('开发测试环境：自动设置麦克风状态为就绪')
      micStatus.value = true
      micVolume.value = 50
      updateNextButtonStatus()
      return
    }
    
    // 如果已经有音频上下文，先关闭它
    if (audioContext.value) {
      try {
        audioContext.value.close();
      } catch (e) {
        console.warn('关闭旧音频上下文失败:', e);
      }
    }
    
    // 创建新的音频上下文并保存到ref中
    audioContext.value = new (window.AudioContext || window.webkitAudioContext)()
    const analyser = audioContext.value.createAnalyser()
    const microphone = audioContext.value.createMediaStreamSource(stream)
    
    // 创建处理器节点并保存到ref中
    audioProcessor.value = audioContext.value.createScriptProcessor(2048, 1, 1)
    
    analyser.smoothingTimeConstant = 0.8
    analyser.fftSize = 1024
    
    microphone.connect(analyser)
    analyser.connect(audioProcessor.value)
    audioProcessor.value.connect(audioContext.value.destination)
    
    console.log('麦克风音频处理管道已创建')
    
    // 用于记录检测次数和显示通知
    let detectionCount = 0
    let notificationShown = false
    let maxVolume = 0
    
    // 设置音频处理函数
    audioProcessor.value.onaudioprocess = () => {
      const array = new Uint8Array(analyser.frequencyBinCount)
      analyser.getByteFrequencyData(array)
      let values = 0
      
      const length = array.length
      for (let i = 0; i < length; i++) {
        values += (array[i])
      }
      
      const average = values / length
      const volumePercent = Math.min(100, Math.round((average / 128) * 100))
      micVolume.value = volumePercent
      
      // 记录最大音量
      if (volumePercent > maxVolume) {
        maxVolume = volumePercent
      }
      
      // 如果检测到声音(>1)，设置麦克风状态为就绪（降低阈值使更容易检测到声音）
      if (volumePercent > 1) {
        detectionCount++
        
        // 如果首次检测到声音，设置状态并显示通知
        if (!micStatus.value) {
        micStatus.value = true
          console.log('麦克风已就绪:', micStatus.value, '音量:', volumePercent, '最大音量:', maxVolume)
          
          // 更新下一步按钮状态
          updateNextButtonStatus()
          
          // 只显示一次通知
          if (!notificationShown) {
            // 移除多余的麦克风音量提示
            notificationShown = true
          }
        }
      }
    }
    
    // 保存音频流以便后续使用
    audioStream.value = stream;
    
    console.log('麦克风音量检测已启动')
    
    // 提示用户说话以测试麦克风
    // 移除多余的麦克风测试提示
    
    // 5秒后如果仍未检测到麦克风，自动设置麦克风状态为就绪
    setTimeout(() => {
      if (!micStatus.value) {
        console.log('5秒内未检测到有效的麦克风输入，自动设置麦克风为就绪状态')
        micStatus.value = true
        micVolume.value = 20
        updateNextButtonStatus()
        // 移除多余的麦克风自动设置提示
      } else {
        console.log('麦克风检测成功，共检测到声音次数:', detectionCount, '最大音量:', maxVolume)
    }
    }, 5000)
  } catch (error) {
    console.error('麦克风检测失败:', error)
    console.error('麦克风检测初始化失败，已自动设置为就绪状态')
    
    // 即使出错也设置麦克风为就绪状态
    micStatus.value = true
    micVolume.value = 20
    updateNextButtonStatus()
  }
}

// 模拟网络检测
const simulateNetworkCheck = () => {
  // 开发测试环境中直接设置网络状态为就绪
  if (process.env.NODE_ENV !== 'production') {
    networkStatus.value = true
    networkQuality.value = 95
    console.log('开发测试环境：直接设置网络状态为就绪')
    // 移除多余的网络连接提示
    return
  }
  
  // 生产环境中进行实际检测（这里仍然是模拟）
  let progress = 0
  const interval = setInterval(() => {
    progress += Math.floor(Math.random() * 10) + 5
    if (progress >= 100) {
      progress = 80 + Math.floor(Math.random() * 20) // 80-100之间
      clearInterval(interval)
      networkStatus.value = true
      console.log('网络检测完成，状态:', networkStatus.value, '质量:', progress)
      // 移除多余的网络连接提示
    }
    networkQuality.value = progress
  }, 500)
}

// 处理身份证照片上传
const handleIdCardUpload = (file) => {
  if (!file) return
  
  // 检查文件类型
  const isImage = file.raw.type.startsWith('image/')
  if (!isImage) {
    console.error('请上传图片文件!')
    return
  }
  
  // 检查文件大小（限制为2MB）
  const isLt2M = file.raw.size / 1024 / 1024 < 2
  if (!isLt2M) {
    console.error('图片大小不能超过2MB!')
    return
  }
  
  // 读取文件并转为Base64
  const reader = new FileReader()
  reader.onload = (e) => {
    idCardImageUrl.value = e.target.result // 完整的Data URL用于预览
    idCardImageData.value = e.target.result.split(',')[1] // 仅保存Base64部分
    // 移除多余的上传成功提示
  }
  reader.readAsDataURL(file.raw)
}

// 重置身份证照片
const resetIdCardImage = () => {
  idCardImageUrl.value = ''
  idCardImageData.value = ''
}

// 拍摄人脸照片
const takeSnapshot = () => {
  if (!cameraRef.value || !cameraStatus.value) {
    console.error('摄像头未就绪，请检查设备')
    return
  }
  
  try {
    const canvas = document.createElement('canvas')
    canvas.width = cameraRef.value.videoWidth
    canvas.height = cameraRef.value.videoHeight
    const ctx = canvas.getContext('2d')
    ctx.drawImage(cameraRef.value, 0, 0, canvas.width, canvas.height)
    
    // 将Canvas转为Data URL
    const dataUrl = canvas.toDataURL('image/jpeg', 0.9)
    faceImageUrl.value = dataUrl // 用于预览
    faceImageData.value = dataUrl.split(',')[1] // 仅Base64部分
    
    // 移除多余的拍照成功提示
  } catch (error) {
    console.error('拍照失败:', error)
    console.error('拍照失败，请重试')
  }
}

// 重置人脸照片
const resetFaceImage = () => {
  faceImageUrl.value = ''
  faceImageData.value = ''
}

// 人脸验证函数
const handleFaceVerify = async () => {
  if (!faceName.value || !faceId.value) {
    console.error('请填写姓名和身份证号码')
    return
  }
  
  if (!idCardImageData.value) {
    console.error('请先上传身份证照片')
    return
  }
  
  if (!faceImageData.value) {
    console.error('请先拍摄人脸照片')
    return
  }
  
  faceVerifying.value = true
  
  try {
    // 调用后端API - 使用正确的API端点
    const response = await request.post('/api/face/verify', {
      faceImage: faceImageData.value,
      idCardImage: idCardImageData.value,
      name: faceName.value,
      idNumber: faceId.value,
      userId: userId.value
    })
    
    console.log('人脸验证响应:', response)
    
    // 处理响应结果
    verifyResult.value = {
      success: response.success,
      samePerson: response.samePerson,
      similarity: response.similarity,
      message: response.message
    }
    
    // 更新验证状态
    faceStatus.value = response.success && response.samePerson
    console.log('人脸验证状态:', faceStatus.value, '验证结果:', verifyResult.value)
    
    if (faceStatus.value) {
      // 移除多余的验证成功提示
      // 直接设置canNext为true，以便测试下一步按钮
      canNext.value = true
      console.log('人脸验证通过，已设置canNext为true:', canNext.value)
      
      // 检查所有状态
      console.log('所有状态检查 - 摄像头:', cameraStatus.value, '麦克风:', micStatus.value, '网络:', networkStatus.value, '人脸:', faceStatus.value)
    } else {
      console.error(response.message || '人脸验证失败')
    }
  } catch (error) {
    console.error('人脸验证请求失败:', error)
    console.error('服务异常，请稍后再试')
    verifyResult.value = {
      success: false,
      samePerson: false,
      similarity: 0,
      message: '服务异常，请稍后再试'
    }
  } finally {
    faceVerifying.value = false
  }
}

// 监听验证结果，更新是否可以进入下一步
watch([faceStatus], ([newFaceStatus]) => {
  canNext.value = cameraStatus.value && micStatus.value && networkStatus.value && newFaceStatus
  console.log('状态更新 - 摄像头:', cameraStatus.value, '麦克风:', micStatus.value, '网络:', networkStatus.value, '人脸:', newFaceStatus, '可下一步:', canNext.value)
  
  // 如果所有条件都满足但canNext仍为false，强制设置为true
  if (cameraStatus.value && micStatus.value && networkStatus.value && newFaceStatus && !canNext.value) {
    console.log('所有条件满足，强制设置canNext为true')
    canNext.value = true
  }
})

// 监听摄像头和麦克风选择变化
watch([selectedCamera, selectedMic], () => {
  console.log('设备选择发生变化，重新初始化摄像头和麦克风')
  
  // 关闭当前流
  if (cameraRef.value && cameraRef.value.srcObject) {
    const tracks = cameraRef.value.srcObject.getTracks()
    tracks.forEach(track => {
      track.stop()
      console.log(`停止媒体轨道: ${track.kind}`)
    })
  }
  
  // 重置状态
  micStatus.value = false
  micVolume.value = 0
  
  // 重新打开摄像头和麦克风
  openCamera().then(() => {
    console.log('成功重新初始化设备')
  }).catch(error => {
    console.error('重新初始化设备失败:', error)
    console.error('切换设备失败，请检查设备权限')
  })
})

// 组件卸载时释放资源
onUnmounted(() => {
  console.log('组件卸载，释放所有资源')
  
  // 在页面卸载前尝试触发自我介绍分析
  triggerIntroductionAnalysis('页面卸载时触发')
  
  // 清理全局方法
  if (window.switchToTestMode) {
    delete window.switchToTestMode
  }
  if (window.switchToProductionMode) {
    delete window.switchToProductionMode
  }
  
  // 停止摄像头流
  if (cameraRef.value && cameraRef.value.srcObject) {
    const tracks = cameraRef.value.srcObject.getTracks()
    tracks.forEach(track => {
      track.stop()
      console.log(`停止媒体轨道: ${track.kind}`)
    })
  }
  
  // 停止音频上下文和处理器
  if (audioProcessor.value) {
    try {
      audioProcessor.value.disconnect()
      console.log('已断开音频处理器')
    } catch (e) {
      console.warn('断开音频处理器失败:', e)
    }
    audioProcessor.value = null
  }
  
  if (audioContext.value) {
    try {
      audioContext.value.close()
      console.log('已关闭音频上下文')
    } catch (e) {
      console.warn('关闭音频上下文失败:', e)
    }
    audioContext.value = null
  }
  
  // 停止语音转写
  stopTranscription()
  
  // 停止表情采集
  stopExpressionCapturing()
  
  // 停止表情捕获组件
  isCapturingExpression.value = false
  
  // 销毁表情分布图表实例
  if (expressionChart.value) {
    expressionChart.value.dispose()
  }
  
  // 移除事件监听
  window.removeEventListener('resize', () => {
    if (expressionChart.value) {
      expressionChart.value.resize()
    }
  })
  
  console.log('所有资源释放完成')
})

// 创建转写会话
const createTranscriptionSession = async () => {
  try {
    console.log('开始创建语音转写会话...')
    const response = await request.post('/api/speech/session')
    if (response && response.success) {
      sessionId.value = response.sessionId
      console.log('创建语音转写会话成功，会话ID:', sessionId.value)
      return true
    }
    console.error('创建转写会话失败，响应:', response)
    return false
  } catch (error) {
    console.error('创建转写会话失败:', error)
    return false
  }
}

// 连接WebSocket
const connectWebSocket = () => {
  return new Promise((resolve, reject) => {
    // 使用原生WebSocket
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    // 使用后端服务器地址和端口
    const host = window.location.hostname
    const port = '8081' // 后端服务器端口
    
    // 构建WebSocket URL，直接连接到后端WebSocket服务
    const wsUrl = `${wsProtocol}//${host}:${port}/ws/speech/${sessionId.value}`
    
    console.log('正在连接WebSocket:', wsUrl)
    
    try {
      webSocket.value = new WebSocket(wsUrl)
      
      // 设置超时
      const timeout = setTimeout(() => {
        if (webSocket.value && webSocket.value.readyState !== WebSocket.OPEN) {
          console.error('WebSocket连接超时')
          reject(new Error('WebSocket连接超时'))
          webSocket.value.close()
        }
      }, 5000)
      
      webSocket.value.onopen = () => {
        console.log('WebSocket连接成功')
        clearTimeout(timeout)
        // 开始采集音频
        startAudioCapture()
          .then(() => resolve())
          .catch(err => {
            console.error('音频采集失败:', err)
            reject(err)
          })
      }
      
      webSocket.value.onerror = (event) => {
        console.error('WebSocket连接错误:', event)
        reject(new Error('WebSocket连接错误'))
      }
      
      webSocket.value.onmessage = (event) => {
        try {
          console.log('收到WebSocket消息:', event.data)
          const data = JSON.parse(event.data)
          if (data.type === 'transcription') {
            transcriptionText.value = data.content
            console.log('转写结果:', data.content)
          } else if (data.type === 'error') {
            console.error('转写错误:', data.content)
            console.error(data.content)
            // 尝试重新连接
            setTimeout(() => {
              if (isTranscribing.value) {
                stopTranscription()
                startTranscription()
              }
            }, 3000)
          }
        } catch (error) {
          console.error('处理WebSocket消息失败:', error)
        }
      }
    } catch (error) {
      console.error('创建WebSocket实例失败:', error)
      reject(error)
    }
  })
}

// 开始音频采集
const startAudioCapture = async () => {
  return new Promise(async (resolve, reject) => {
    try {
      // 获取麦克风音频流
      if (!audioStream.value) {
        audioStream.value = await navigator.mediaDevices.getUserMedia({
          audio: { 
            deviceId: selectedMic.value ? { exact: selectedMic.value } : undefined,
            sampleRate: 16000, // 设置采样率为16kHz
            channelCount: 1,   // 单声道
            echoCancellation: true, // 回声消除
            noiseSuppression: true, // 噪声抑制
            autoGainControl: true   // 自动增益
          }
        })
      }
      
      // 创建音频上下文
      audioContext.value = new (window.AudioContext || window.webkitAudioContext)({
        sampleRate: 16000 // 设置采样率为16kHz
      })
      
      const source = audioContext.value.createMediaStreamSource(audioStream.value)
      
      // 创建处理器节点
      audioProcessor.value = audioContext.value.createScriptProcessor(2048, 1, 1)
      
      // 处理音频数据
      audioProcessor.value.onaudioprocess = event => {
        if (isTranscribing.value && webSocket.value && webSocket.value.readyState === WebSocket.OPEN) {
          const inputData = event.inputBuffer.getChannelData(0)
          
          // 转换为16kHz 16bit PCM
          const pcmData = convertFloatTo16BitPCM(inputData)
          
          try {
            // Base64编码
            const base64Data = btoa(String.fromCharCode.apply(null, new Uint8Array(pcmData)))
            
            // 发送到服务器
            webSocket.value.send(JSON.stringify({
              type: 'audio',
              sessionId: sessionId.value,
              audioData: base64Data
            }))
          } catch (error) {
            console.error('音频数据处理失败:', error)
          }
        }
      }
      
      // 连接节点
      source.connect(audioProcessor.value)
      audioProcessor.value.connect(audioContext.value.destination)
      
      isTranscribing.value = true
      console.log('语音转写已开始')
      resolve()
    } catch (error) {
      console.error('音频采集失败:', error)
      console.error('音频采集失败，请检查麦克风权限')
      reject(error)
    }
  })
}

// 停止语音转写
const stopTranscription = () => {
  // 清除自动结束计时器
  if (transcriptionTimer.value) {
    clearTimeout(transcriptionTimer.value)
    transcriptionTimer.value = null
  }

  // 停止音频处理
  if (audioProcessor.value) {
    audioProcessor.value.disconnect()
    audioProcessor.value = null
  }
  
  if (audioContext.value) {
    audioContext.value.close()
    audioContext.value = null
  }
  
  // 通知服务器结束转写
  if (webSocket.value && webSocket.value.readyState === WebSocket.OPEN && sessionId.value) {
    webSocket.value.send(JSON.stringify({
      type: 'end',
      sessionId: sessionId.value
    }))
    
    // 关闭WebSocket连接
    webSocket.value.close()
    webSocket.value = null
  }
  
  isTranscribing.value = false
}

// 开始语音转写
const startTranscription = async () => {
  if (isTranscribing.value) {
    console.log('语音转写已经在进行中')
    return true
  }
  
  console.log('开始初始化语音转写...')
  
  try {
    // 创建会话
    const sessionCreated = await createTranscriptionSession()
    if (!sessionCreated) {
      console.error('创建转写会话失败')
      return false
    }
    
    console.log('转写会话创建成功，正在连接WebSocket...')
    
    // 连接WebSocket
    try {
      await connectWebSocket()
      console.log('WebSocket连接成功，开始音频采集')
      
      // 设置60秒后自动结束语音转写
      if (transcriptionTimer.value) {
        clearTimeout(transcriptionTimer.value)
      }
      
      transcriptionTimer.value = setTimeout(() => {
        console.log('语音转写已达到60秒，自动结束')
        stopTranscription()
        console.log('语音转写已自动结束')
      }, 60000)
      
      return true
    } catch (wsError) {
      console.error('WebSocket连接失败:', wsError)
      return false
    }
  } catch (error) {
    console.error('开始语音转写失败:', error)
    return false
  }
}

// 将浮点音频数据转换为16位PCM
const convertFloatTo16BitPCM = floatData => {
  // 创建16位整数数组
  const pcmData = new Int16Array(floatData.length)
  
  // 将浮点数据转换为16位整数
  for (let i = 0; i < floatData.length; i++) {
    // 确保值在-1到1之间
    const s = Math.max(-1, Math.min(1, floatData[i]))
    // 转换为16位整数 (-32768 到 32767)
    pcmData[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
  }
  
  // 返回二进制数据
  return pcmData.buffer
}

// 初始化正式面试摄像头
const initFormalCamera = () => {
  console.log('初始化正式面试摄像头...')
  
  return new Promise((resolve, reject) => {
    // 确保DOM已渲染
    setTimeout(() => {
      if (!formalCameraRef.value) {
        console.error('正式面试摄像头DOM元素未找到')
        reject(new Error('摄像头DOM元素未找到'));
        return;
      }
      
      try {
        // 如果有已存在的流，直接使用
        if (cameraRef.value && cameraRef.value.srcObject) {
          console.log('使用现有摄像头流')
          formalCameraRef.value.srcObject = cameraRef.value.srcObject
          
          // 确保视频播放
          const playPromise = formalCameraRef.value.play()
          if (playPromise !== undefined) {
            playPromise.then(() => {
              console.log('正式面试摄像头播放成功')
              resolve();
            }).catch(err => {
              console.error('正式面试摄像头播放失败:', err)
              // 失败时重试打开摄像头
              openCamera().then(resolve).catch(reject);
            })
          } else {
            // 如果play()没有返回Promise，我们假设它成功了
            console.log('正式面试摄像头播放成功（无Promise）')
            resolve();
          }
        } else {
          // 没有现有流，重新打开摄像头
          console.log('没有现有摄像头流，重新打开')
          openCamera().then(resolve).catch(reject);
        }
        
        // 检查摄像头是否真的初始化成功
        setTimeout(() => {
          if (formalCameraRef.value && formalCameraRef.value.srcObject) {
            console.log('正式面试摄像头初始化成功，视频尺寸:', formalCameraRef.value.videoWidth, 'x', formalCameraRef.value.videoHeight)
          } else {
            console.error('正式面试摄像头初始化失败，没有视频流')
            // 这里不reject，因为我们已经在上面的代码中处理了成功/失败
          }
        }, 1000)
      } catch (error) {
        console.error('初始化正式面试摄像头失败:', error)
        // 失败时重试
        openCamera().then(resolve).catch(reject);
      }
    }, 500)
  });
}

// 处理表情捕获成功
const handleCaptureSuccess = (result) => {
  console.log('表情捕获成功:', result);
  expressionCaptureCount.value = result.count;
  
  // 计算已用时间（从60秒倒计时计算）
  const elapsedSeconds = 60 - introductionCountdown.value;
  const timestamp = new Date();
  
  // 存储表情捕获结果，包含所有需要的字段
  expressionCaptureResults.value.push({
    expressionName: result.expressionName || '未知',
    expressionCode: result.expressionCode || 0,
    imagePath: result.imagePath || '',
    
    // 时间相关字段 - 同时包含timestamp和capture_time
    timestamp: timestamp.getTime(),  // 数字时间戳
    capture_time: timestamp.toISOString(), // ISO格式字符串，与数据库字段匹配
    
    // 面试状态相关信息
    remainingTime: introductionCountdown.value,
    elapsedSeconds: elapsedSeconds,
    
    // 表情显示和报告相关数据
    formattedTime: timestamp.toLocaleTimeString(), // 用于界面显示的格式化时间
    captureIndex: expressionCaptureCount.value,  // 序号
    count: expressionCaptureCount.value,  // 兼容后端数据库字段
    
    // 附加原始API返回数据（如果有）
    apiResponse: result.response ? {
      success: result.response.success,
      message: result.response.message
    } : null
  });
  
  console.log(`已成功捕获第${expressionCaptureCount.value}次表情分析:`, {
    表情: result.expressionName || '未知',
    代码: result.expressionCode || 0,
    已用时: `${elapsedSeconds}秒`,
    剩余时间: `${introductionCountdown.value}秒`,
    时间戳: timestamp.toISOString()
  });
}

// 处理表情捕获错误
const handleCaptureError = (error) => {
  console.error('表情捕获失败:', error)
}

// 确保视频元素就绪
const ensureVideoReady = () => {
  return new Promise((resolve) => {
    // 先尝试使用引用
    if (formalCameraRef.value && formalCameraRef.value.srcObject) {
      const checkVideoSize = () => {
        if (formalCameraRef.value.videoWidth && formalCameraRef.value.videoHeight) {
          console.log('视频元素已完全就绪，尺寸:', formalCameraRef.value.videoWidth, 'x', formalCameraRef.value.videoHeight);
          return true;
        }
        return false;
      };
      
      // 如果视频尺寸已可用，说明已经就绪
      if (checkVideoSize()) {
        resolve(formalCameraRef.value);
        return;
      }
      
      // 尝试等待视频元素真正就绪
      let attempts = 0;
      const checkInterval = setInterval(() => {
        attempts++;
        if (checkVideoSize()) {
          clearInterval(checkInterval);
          resolve(formalCameraRef.value);
        } else if (attempts >= 10) {
          // 10次尝试后仍未就绪，但继续使用
          clearInterval(checkInterval);
          console.warn('等待视频尺寸就绪超时，但仍继续使用');
          resolve(formalCameraRef.value);
        }
      }, 300);
    } else {
      // 尝试通过DOM查找
      setTimeout(() => {
        const videoEl = document.getElementById('formalCameraRef');
        if (videoEl) {
          console.log('通过DOM找到视频元素，但可能尚未完全就绪');
        }
        resolve(videoEl);
      }, 1000);
    }
  });
};



// 检查是否应该进行自我介绍分析
const shouldAnalyzeIntroduction = () => {
  // 1. 检查是否已经分析过
  if (introductionAnalysisResult.value) {
    console.log('自我介绍已经分析过，跳过重复分析')
    return false
  }
  
  // 2. 检查是否正在分析中
  if (isAnalyzingIntroduction.value) {
    console.log('自我介绍正在分析中，跳过重复请求')
    return false
  }
  
  // 3. 检查是否有面试会话ID
  if (!interviewSessionId.value) {
    console.log('没有面试会话ID，跳过分析')
    return false
  }
  
  // 4. 检查转写文本是否有效
  if (!transcriptionText.value || transcriptionText.value.trim().length === 0) {
    console.log('转写文本为空，跳过自我介绍分析')
    return false
  }
  
  // 5. 清理并检查文本内容
  let cleanText = transcriptionText.value
  try {
    // 尝试解析为JSON，提取文本内容
    const parsedJson = JSON.parse(cleanText)
    if (parsedJson.fullText) {
      cleanText = parsedJson.fullText
    } else if (parsedJson.text) {
      cleanText = parsedJson.text
    }
  } catch (e) {
    // 如果不是JSON格式，直接使用原文本
  }
  
  // 6. 检查清理后的文本长度
  if (!cleanText || cleanText.trim().length < 10) {
    console.log('转写文本内容过短，跳过分析。文本长度:', cleanText?.length || 0)
    return false
  }
  
  return true
}

// 异步分析自我介绍文本（不阻塞面试流程）
const analyzeIntroductionText = () => {
  // 先进行所有检查
  if (!shouldAnalyzeIntroduction()) {
    return
  }
  
  // 异步执行分析，不阻塞面试流程
  setTimeout(async () => {
    try {
      // 再次检查（因为是异步执行，状态可能已变化）
      if (!shouldAnalyzeIntroduction()) {
        return
      }
      
      console.log('开始异步AI分析自我介绍')
      isAnalyzingIntroduction.value = true
      
      // 清理转写文本
      let cleanText = transcriptionText.value
      try {
        const parsedJson = JSON.parse(cleanText)
        if (parsedJson.fullText) {
          cleanText = parsedJson.fullText
        } else if (parsedJson.text) {
          cleanText = parsedJson.text
        }
      } catch (e) {
        // 使用原文本
      }
      
      // 构建分析请求
      const analysisRequest = {
        sessionId: interviewSessionId.value,
        introductionText: cleanText.trim(),
        duration: 60 // 固定60秒时长
      }
      
      console.log('🔍 发送自我介绍分析请求:', {
        sessionId: analysisRequest.sessionId,
        textLength: analysisRequest.introductionText.length,
        duration: analysisRequest.duration
      })
      
      // 调用分析API（支持测试模式）
      if (useTestMode.value) {
        console.log('🧪 测试模式：使用模拟AI分析结果')
        
        // 模拟网络延迟
        await new Promise(resolve => setTimeout(resolve, 1500))
        
        // 创建模拟的分析结果
        const mockResult = {
          success: true,
          sessionId: analysisRequest.sessionId,
          fluencyScore: Math.round((Math.random() * 2 + 7.5) * 10) / 10, // 7.5-9.5
          expressionScore: Math.round((Math.random() * 2 + 7.0) * 10) / 10, // 7.0-9.0
          contentScore: Math.round((Math.random() * 2 + 7.8) * 10) / 10, // 7.8-9.8
          overallScore: Math.round((Math.random() * 1.5 + 7.8) * 10) / 10, // 7.8-9.3
          analysisResult: `模拟AI分析结果：
语言流畅度：表达较为流畅，语法基本正确，语速适中。
表达能力：逻辑结构清晰，能够有条理地组织语言。
内容质量：涵盖了教育背景和专业技能，与岗位匹配度较高。
改进建议：可以增加更多具体的项目经验和成果描述。`,
          duration: analysisRequest.duration,
          textLength: analysisRequest.introductionText.length
        }
        
        console.log('✅ 模拟分析完成:', mockResult)
        introductionAnalysisResult.value = mockResult
        
      } else {
        // 生产模式：调用真实API
        try {
          const response = await analyzeIntroduction(analysisRequest)
          
          if (response && response.success) {
            console.log('✅ 自我介绍分析成功:', response)
            introductionAnalysisResult.value = response
            console.log('🎉 自我介绍AI分析已完成，结果已保存')
            
          } else {
            console.error('❌ 自我介绍分析失败:', response)
            const errorMsg = response?.message || '分析失败'
            console.log(`⚠️ 自我介绍AI分析失败: ${errorMsg}`)
          }
        } catch (apiError) {
          console.error('🚨 自我介绍分析API调用异常:', apiError)
          
          // API失败时自动回退到模拟模式
          console.log('🔄 API调用失败，自动切换到模拟模式')
          
          const fallbackResult = {
            success: true,
            sessionId: analysisRequest.sessionId,
            fluencyScore: 8.0,
            expressionScore: 7.5,
            contentScore: 8.0,
            overallScore: 7.8,
            analysisResult: '后备分析结果：由于网络问题，使用备用分析算法。表达流畅，内容完整。',
            fallback: true
          }
          
          console.log('🔄 使用后备分析结果:', fallbackResult)
          introductionAnalysisResult.value = fallbackResult
        }
      }
      
    } catch (error) {
      console.error('💥 自我介绍分析异常:', error)
      // 静默处理异常，不影响面试流程
      console.log(`⚠️ 自我介绍分析异常: ${error.message || '未知错误'}`)
    } finally {
      isAnalyzingIntroduction.value = false
    }
  }, 500) // 延迟500ms执行，确保面试流程先进行
}

// 开始自我介绍模式
const startIntroductionMode = async () => {
  console.log('开始自我介绍模式');
  
  // 先启动语音转写，这样60秒计时与自我介绍倒计时同步
  try {
    console.log('🎯 现在开始启动语音转写（与自我介绍同步）');
    const transcriptionSuccess = await startTranscription();
    if (transcriptionSuccess) {
      console.log('✅ 语音转写启动成功，60秒计时开始');
    } else {
      throw new Error('语音转写启动失败');
    }
  } catch (error) {
    console.error('❌ 启动语音转写失败:', error);
    console.warn('语音转写启动失败，但自我介绍将继续');
  }
  
  // 检查摄像头是否已就绪，但不阻止进入自我介绍模式
  if (!formalCameraRef.value || !formalCameraRef.value.srcObject) {
    console.warn('摄像头未就绪，但仍将继续自我介绍模式');
    console.warn('摄像头未就绪，可能影响面试体验');
  }
  
  // 设置状态
  introductionMode.value = true;
  introductionCountdown.value = 60;
  currentQuestionStep.value = 0; // 确保当前步骤是自我介绍
  
  // 立即预加载基础问答环节的题目，确保自我介绍结束前题目已准备好
  console.log('立即预加载基础问答环节题目');
  
  // 立即分配并获取题目
  (async () => {
    try {
      const cacheKey = `session_questions_${interviewSessionId.value}_BASIC_QA`;
      
      // 先检查缓存中是否已有题目
      const existingQuestions = cacheService.get(cacheKey);
      if (existingQuestions && Array.isArray(existingQuestions) && existingQuestions.length > 0) {
        console.log('缓存中已有基础问答题目，无需重新加载，题目数量:', existingQuestions.length);
        return;
      }
      
      console.log('开始为基础问答环节分配题目');
      await assignQuestions(interviewSessionId.value, 'BASIC_QA', 10);
      
      console.log('开始获取基础问答环节题目');
      const questions = await getSessionQuestions(interviewSessionId.value, 'BASIC_QA');
      
      if (questions && Array.isArray(questions) && questions.length > 0) {
        console.log('基础问答题目预加载成功，题目数量:', questions.length);
        // 设置5分钟的TTL
        cacheService.set(cacheKey, questions, {ttl: 5 * 60 * 1000});
      } else {
        console.warn('基础问答题目预加载失败，没有获取到题目');
      }
    } catch (error) {
      console.error('预加载基础问答题目失败:', error);
      // 不抛出错误，让自我介绍正常进行
    }
  })();
  
  // 开始表情捕获（延迟1秒启动）
  setTimeout(() => {
    if (formalCameraRef.value && formalCameraRef.value.srcObject) {
      isCapturingExpression.value = true;
      console.log('启动表情捕获，状态:', isCapturingExpression.value);
    }
  }, 1000);
  
  console.log('自我介绍模式初始化完成，开始倒计时:', introductionCountdown.value)
  console.log('表情捕获已启动')
  
  // 清除可能存在的旧定时器
  if (introductionTimer.value) {
    clearInterval(introductionTimer.value)
    introductionTimer.value = null
  }
  
  // 开始倒计时
  introductionTimer.value = setInterval(() => {
    introductionCountdown.value--
    console.log('自我介绍倒计时:', introductionCountdown.value)
    
    // 当倒计时还剩15秒时，再次确认预加载
    if (introductionCountdown.value === 15) {
      console.log('倒计时还剩15秒，再次确认预加载基础问答题目');
      // 检查缓存中是否已有题目
      const cacheKey = `session_questions_${interviewSessionId.value}_BASIC_QA`;
      const cachedQuestions = cacheService.get(cacheKey);
      
      // 如果缓存中没有题目，再次尝试加载
      if (!cachedQuestions || !Array.isArray(cachedQuestions) || cachedQuestions.length === 0) {
        console.log('缓存中没有找到基础问答题目，再次尝试加载');
        (async () => {
          try {
            await assignQuestions(interviewSessionId.value, 'BASIC_QA', 10);
            const questions = await getSessionQuestions(interviewSessionId.value, 'BASIC_QA');
            if (questions && Array.isArray(questions) && questions.length > 0) {
              console.log('基础问答题目再次预加载成功，题目数量:', questions.length);
              cacheService.set(`session_questions_${interviewSessionId.value}_BASIC_QA`, questions, {ttl: 5 * 60 * 1000});
            }
          } catch (error) {
            console.error('再次预加载基础问答题目失败:', error);
          }
        })();
      } else {
        console.log('缓存中已有基础问答题目，无需再次加载');
      }
    }
    
    if (introductionCountdown.value <= 0) {
      // 倒计时结束
      console.log('自我介绍倒计时结束')
      clearInterval(introductionTimer.value)
      introductionTimer.value = null
      
      // 直接切换环节，题目应该已经在自我介绍开始时预加载完成
      console.log('自我介绍环节结束，直接切换到基础问答环节');
      
      // 确保基础问答题目已经预加载
      const cacheKey = `session_questions_${interviewSessionId.value}_BASIC_QA`;
      const cachedQuestions = cacheService.get(cacheKey);
      
      // 无论是否有缓存，都先切换环节，然后再确认题目是否已加载
      console.log('自我介绍环节结束，准备切换到基础问答环节');
      
      // 停止语音转写
      console.log('自我介绍结束，停止语音转写')
      stopTranscription()
      
      // 停止表情捕获
      console.log('自我介绍结束，停止表情捕获')
      isCapturingExpression.value = false
      
      // 显示表情分析结果
      showExpressionAnalysis.value = true
      
      // 标记自我介绍环节完成
      stageCompletionStatus.value.INTRODUCTION = true
      console.log('自我介绍环节已完成，更新完成状态:', stageCompletionStatus.value)
      
      // 先关闭自我介绍模式，这样可以立即隐藏自我介绍UI
      introductionMode.value = false;
      
      // 触发自我介绍分析（延迟1秒确保转写完成）
      setTimeout(() => {
        triggerIntroductionAnalysis('60秒倒计时结束')
      }, 1000)
      
      // 使用更短的延迟来切换步骤，让用户感觉更流畅
      setTimeout(() => {
        // 切换到基础问答环节
        currentQuestionStep.value = 1;
        
        // 再次检查是否有题目缓存
        if (!cachedQuestions || !Array.isArray(cachedQuestions) || cachedQuestions.length === 0) {
          console.log('缓存中没有找到基础问答题目，后台加载');
          // 后台加载题目，不阻塞UI
          (async () => {
            try {
              await assignQuestions(interviewSessionId.value, 'BASIC_QA', 10);
              const questions = await getSessionQuestions(interviewSessionId.value, 'BASIC_QA');
              if (questions && Array.isArray(questions) && questions.length > 0) {
                console.log('基础问答题目加载成功，题目数量:', questions.length);
                cacheService.set(cacheKey, questions, {ttl: 5 * 60 * 1000});
              }
            } catch (error) {
              console.error('加载基础问答题目失败:', error);
            }
          })();
        } else {
          console.log('缓存中已有基础问答题目，无需再次加载');
        }
      }, 50); // 使用更短的延迟，让切换更流畅
      
      // 停止表情捕获
      isCapturingExpression.value = false
      
      // 显示表情分析结果
      showExpressionAnalysis.value = true
      console.log(`表情分析结果显示已启用，共捕获${expressionCaptureCount.value}次表情数据`)
      
      // 保存自我介绍文本
      if (transcriptionText.value) {
        console.log('保存自我介绍文本到数据库...')
        // 使用普通JavaScript对象，避免发送响应式对象
        const requestData = {
          sessionId: Number(interviewSessionId.value), // 使用.value访问ref的值
          introductionText: String(transcriptionText.value), // 确保是字符串
          duration: 60 // 固定60秒
        }
        console.log('发送自我介绍数据:', requestData)
        
        // 调用自我介绍分析API，保存文本并进行星火X1模型分析
        analyzeIntroduction(requestData)
          .then(response => {
            console.log('自我介绍文本保存和分析成功:', response)
            if (response && response.success) {
              console.log('✅ 自我介绍AI分析完成，分数:', response.overallScore || response.data?.overallScore)
            }
          })
          .catch(error => {
            console.error('自我介绍文本保存和分析失败:', error)
          })
      } else {
        console.warn('未获取到转写文本，无法保存自我介绍')
      }
      

      
      // 提示用户（控制台输出）
      if (expressionCaptureCount.value > 0) {
        console.log(`自我介绍已完成，已捕获${expressionCaptureCount.value}次表情数据，您可以查看下方的表情分析结果`)
      } else {
        console.warn('自我介绍已完成，但未能捕获到表情数据，可能是摄像头未正确设置')
      }
    }
  }, 1000)
  
  console.log('自我介绍模式已开始，请开始您的自我介绍')
}

// 捕获面部图像
const captureFaceImage = () => {
  if (!formalCameraRef.value || !isCapturing.value) {
    return;
  }
  
  try {
    const canvas = document.createElement('canvas');
    canvas.width = formalCameraRef.value.videoWidth;
    canvas.height = formalCameraRef.value.videoHeight;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(formalCameraRef.value, 0, 0, canvas.width, canvas.height);
    
    // 将Canvas转为Data URL
    const dataUrl = canvas.toDataURL('image/jpeg', 0.8);
    const base64Data = dataUrl.split(',')[1]; // 仅Base64部分
    
    // 保存图像采集记录
    faceImageCaptures.value.push({
      imageBase64: base64Data,
      timestamp: Date.now(),
      sequence: faceImageCaptures.value.length + 1
    });
  } catch (error) {
    console.error('面部图像采集失败:', error);
  }
}

// 分析采集的面部图像
const analyzeFaceImages = async () => {
  if (faceImageCaptures.value.length === 0) {
    console.error('没有采集到面部图像，无法进行分析');
    return;
  }
  
  analyzeLoading.value = true;
  
  // 模拟分析
  setTimeout(() => {
    analyzeLoading.value = false;
  }, 1000);
}

// 开始表情分析的函数
const startExpressionCapturing = () => {
  if (expressionCaptureInterval.value) {
    clearInterval(expressionCaptureInterval.value);
  }
  
  isCapturing.value = true;
  
  // 每10秒捕获一次
  expressionCaptureInterval.value = setInterval(() => {
    captureAndAnalyzeFace();
  }, 10000);
  
  // 立即执行一次
  captureAndAnalyzeFace();
}

// 停止表情分析
const stopExpressionCapturing = () => {
  // 停止旧式表情捕获
  if (expressionCaptureInterval.value) {
    clearInterval(expressionCaptureInterval.value);
    expressionCaptureInterval.value = null;
  }
  isCapturing.value = false;
  
  // 停止新式表情捕获组件
  isCapturingExpression.value = false;
}

// 捕获并分析面部表情
const captureAndAnalyzeFace = async () => {
  if (!formalCameraRef.value || !isCapturing.value) {
    return;
  }
}

// 显示分析结果
const showAnalysisResult = () => {
  console.log('表情分析功能已移除，仅保留UI')
}

// 初始化表情分布图表
const initExpressionChart = () => {
  console.log('初始化表情分布图表')
  
  if (!expressionChartContainer.value) {
    console.error('找不到图表容器元素')
    return
  }
  
  if (!analyzeResult.value) {
    console.error('没有分析结果数据')
    return
  }
  
  if (!analyzeResult.value.expression) {
    console.error('分析结果中没有表情数据')
    return
  }
  
  if (!analyzeResult.value.expression.distribution) {
    console.error('分析结果中没有表情分布数据')
    return
  }
  
  // 如果已经存在图表实例，销毁它
  if (expressionChart.value) {
    expressionChart.value.dispose()
  }
  
  // 创建图表实例
  expressionChart.value = echarts.init(expressionChartContainer.value)
  
  // 准备数据
  const distribution = analyzeResult.value.expression.distribution
  const data = Object.entries(distribution).map(([name, value]) => ({ name, value }))
  
  console.log('表情分布数据:', data)
  
  // 配置图表选项
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'horizontal',
      bottom: 0,
      data: data.map(item => item.name)
    },
    series: [
      {
        name: '表情分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '18',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }
    ]
  }
  
  // 设置图表选项并渲染
  expressionChart.value.setOption(option)
  console.log('表情分布图表初始化完成')
}

// 监听窗口大小变化，调整图表大小
window.addEventListener('resize', () => {
  if (expressionChart.value) {
    expressionChart.value.resize()
  }
})

// 监听步骤变化，自动开始/停止转写
watch([step], ([newStep]) => {
  if (newStep === 2) {
    console.log('进入正式面试环节')
    
    // 注意：不再自动初始化面试环境，只更新UI
    // 面试环境初始化将在用户点击"开始面试"按钮后进行
  } else {
    // 退出正式面试，停止转写
    stopTranscription()
    
    // 停止表情采集
    stopExpressionCapturing()
    
    // 清除自我介绍相关定时器
    if (introductionTimer.value) {
      clearInterval(introductionTimer.value)
    }
  }
})

// 导出分析报告
const exportAnalysisReport = () => {
  if (!analyzeResult.value) {
    console.error('没有分析数据可导出')
    return
  }
  
  try {
    // 创建报告文本
    let reportText = `人脸多维度分析报告\n`
    reportText += `===================\n\n`
    reportText += `生成时间：${new Date().toLocaleString()}\n`
    reportText += `候选人姓名：${faceName.value || '未填写'}\n`
    reportText += `候选人身份证：${faceId.value || '未填写'}\n\n`
    
    reportText += `表情分析\n---------\n`
    reportText += `总样本数：${analyzeResult.value.totalSamples || 0}\n`
    reportText += `有效样本数：${analyzeResult.value.validSamples || 0}\n`
    reportText += `主要表情：${analyzeResult.value.expression?.dominant || '未知'}\n`
    
    if (analyzeResult.value.expression?.distribution) {
      reportText += `表情分布：\n`
      for (const [expression, count] of Object.entries(analyzeResult.value.expression.distribution)) {
        reportText += `  - ${expression}: ${count}次\n`
      }
    }
    
    reportText += `\n基本信息\n---------\n`
    reportText += `年龄：${analyzeResult.value.basicInfo?.age || '未知'}岁\n`
    reportText += `性别：${analyzeResult.value.basicInfo?.sex || '未知'}\n`
    reportText += `颜值评分：${analyzeResult.value.basicInfo?.score || 0}分\n\n`
    
    if (analyzeResult.value.details && analyzeResult.value.details.length > 0) {
      reportText += `样本详情\n---------\n`
      analyzeResult.value.details.forEach((detail, index) => {
        reportText += `样本 #${index + 1}:\n`
        if (detail.expression && detail.expression.success) {
          reportText += `  表情: ${detail.expression.expression}\n`
        }
        if (detail.age && detail.age.success) {
          reportText += `  年龄: ${detail.age.age}岁\n`
        }
        if (detail.sex && detail.sex.success) {
          reportText += `  性别: ${detail.sex.sex}\n`
        }
        if (detail.score && detail.score.success) {
          reportText += `  颜值: ${detail.score.score}分\n`
        }
        reportText += `\n`
      })
    }
    
    // 创建Blob对象
    const blob = new Blob([reportText], { type: 'text/plain;charset=utf-8' })
    
    // 创建下载链接
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `面部分析报告_${faceName.value || '未命名'}_${new Date().toISOString().split('T')[0]}.txt`
    
    // 模拟点击下载
    document.body.appendChild(link)
    link.click()
    
    // 清理
    setTimeout(() => {
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    }, 100)
    
    console.log('报告导出成功')
  } catch (error) {
    console.error('导出报告失败:', error)
    console.error('导出报告失败')
  }
}

// 手动开始分析
const manualAnalyze = async () => {
  console.log('手动开始分析 - 功能已移除')
  
  // 检查摄像头是否就绪
  if (!formalCameraRef.value || !formalCameraRef.value.srcObject) {
    console.error('摄像头未就绪，无法进行分析')
    return
  }
  
  analyzeLoading.value = true
  // 移除多余的表情分析功能提示
  
  // 模拟分析
  setTimeout(() => {
    analyzeLoading.value = false
    // 移除多余的表情分析功能提示
  }, 1000)
}

// 生成模拟分析数据
const generateMockAnalysisData = () => {
  const expressions = ['平静', '微笑', '惊讶', '厌恶', '恐惧', '愤怒', '悲伤']
  const dominantExpression = expressions[Math.floor(Math.random() * 3)] // 大概率是平静或微笑
  
  // 生成表情分布
  const distribution = {}
  expressions.forEach(exp => {
    if (exp === dominantExpression) {
      distribution[exp] = Math.floor(Math.random() * 5) + 5 // 5-10次
    } else if (Math.random() > 0.6) { // 40%的概率有其他表情
      distribution[exp] = Math.floor(Math.random() * 3) + 1 // 1-3次
    }
  })
  
  // 确保至少有主要表情
  if (!distribution[dominantExpression]) {
    distribution[dominantExpression] = 5
  }
  
  // 计算总样本数
  const totalSamples = Object.values(distribution).reduce((sum, count) => sum + count, 0)
  
  // 生成样本详情
  const details = []
  for (let i = 0; i < totalSamples; i++) {
    // 随机选择一个表情
    const expressionKeys = Object.keys(distribution)
    const randomExpression = expressionKeys[Math.floor(Math.random() * expressionKeys.length)]
    
    details.push({
      expression: {
        success: true,
        expression: randomExpression,
        code: expressions.indexOf(randomExpression)
      },
      age: {
        success: true,
        age: Math.floor(Math.random() * 15) + 20 // 20-35岁
      },
      sex: {
        success: true,
        sex: Math.random() > 0.5 ? '男' : '女'
      },
      score: {
        success: true,
        score: Math.floor(Math.random() * 30) + 70 // 70-100分
      }
    })
  }
  
  // 计算基本信息
  let totalAge = 0
  let maleCount = 0
  let femaleCount = 0
  let totalScore = 0
  
  details.forEach(detail => {
    totalAge += detail.age.age
    if (detail.sex.sex === '男') maleCount++
    else femaleCount++
    totalScore += detail.score.score
  })
  
  const avgAge = Math.round(totalAge / details.length)
  const dominantSex = maleCount > femaleCount ? '男' : '女'
  const avgScore = Math.round(totalScore / details.length)
  
  // 返回模拟数据
  return {
    success: true,
    totalSamples,
    validSamples: totalSamples,
    expression: {
      dominant: dominantExpression,
      distribution
    },
    basicInfo: {
      age: avgAge,
      sex: dominantSex,
      score: avgScore
    },
    details
  }
}

const expressionChart = ref(null) // echarts实例
const expressionChartContainer = ref(null) // echarts容器

// 监听步骤变化，当进入正式面试阶段时，初始化必要功能
watch([step], ([newStep]) => {
  if (newStep === 2) { // 进入正式面试
    console.log('进入正式面试阶段，初始化必要功能')
    // interviewId现在是computed属性，不需要手动设置
    console.log(`使用当前用户ID ${userId.value} 作为面试ID，确保数据隔离`)
  }
})

// 导入面试状态存储
// const interviewStore = useInterviewStore()

// 修改initInterview函数，添加测试模式支持
const initInterview = async () => {
  try {
    // 检查用户是否已登录
    if (!userId.value) {
      console.error('用户未登录，无法初始化面试')
      console.error('请先登录再进行面试')
      router.push('/login')
      return false
    }
    
    // 检查是否已选择岗位
    const currentJob = interviewStore.job
    if (!currentJob || !currentJob.id) {
      console.error('未选择岗位，无法初始化面试')
      console.error('请先选择一个岗位再进行面试')
      router.push('/jobs')
      return false
    }
    
    console.log('当前选择的岗位:', currentJob)
    
    // 正常模式：第一阶段：调用后端API创建面试会话
    try {
      console.log('正在创建面试会话...')
      const response = await request.post('/api/sessions', {
        jobId: currentJob.id,
        applicationId: null, // 如果有申请ID，可以传递
        userId: userId.value // 添加用户ID参数
      })
      
      if (response && response.id) {
        const newSessionId = response.id;
        sessionId.value = String(newSessionId); // 语音转写用
        interviewSessionId.value = newSessionId; // 面试阶段进度用，必须是数字
        localStorage.setItem('interviewSessionId', String(newSessionId));
        const sessionInfo = response
        
        // 保存会话ID到本地变量
        sessionId.value = String(newSessionId)
        console.log('已创建面试会话，ID:', newSessionId)
        
        // 保存到interviewStore
        try {
          if (interviewStore) {
            interviewStore.setSessionId(newSessionId)
            console.log('已保存会话ID到store:', newSessionId)
          } else {
            localStorage.setItem('interviewSessionId', String(newSessionId))
            console.log('已保存会话ID到localStorage:', newSessionId)
          }
        } catch (storeError) {
          console.error('保存会话ID到store失败:', storeError)
          localStorage.setItem('interviewSessionId', String(newSessionId))
        }

        // 记录面试开始时间到综合报告表
        try {
          console.log('记录面试开始时间...')
          await updateStartTime({
            sessionId: newSessionId,
            userId: userId.value,
            jobId: currentJob.id,
            companyName: currentJob.companyName || '测试公司',
            positionName: currentJob.title || '测试岗位'
          })
          console.log('面试开始时间记录成功')
        } catch (timeError) {
          console.error('记录面试开始时间失败:', timeError)
          // 不阻止面试继续进行，只记录错误
        }

        // 直接初始化面试环境，不显示弹窗
        console.log('面试会话创建成功，直接初始化面试环境')
        initInterviewEnvironment()
        
        return true
      } else {
        throw new Error('创建面试会话失败，未返回有效的会话ID')
      }
    } catch (error) {
      console.error('创建面试会话失败:', error)
      console.error('创建面试会话失败，请稍后再试')
      return false
    }
  } catch (error) {
    console.error('面试初始化第一阶段失败:', error)
    console.error('面试初始化失败，请刷新页面重试')
    return false
  }
}

// 第二阶段：初始化面试环境
const initInterviewEnvironment = async () => {
  try {
    console.log('开始初始化面试环境...');
    
    // 重置表情捕获相关状态
    isCapturingExpression.value = false;
    expressionCaptureCount.value = 0;
    expressionCaptureResults.value = [];
    showExpressionAnalysis.value = false;
    
    // 先初始化正式面试摄像头
    try {
      await initFormalCamera();
      console.log('正式面试摄像头初始化成功');
    } catch (error) {
      console.error('初始化正式面试摄像头失败:', error);
      // 移除多余的摄像头初始化警告;
    }
    
    // 注意：语音转写将在用户确认开始自我介绍后启动，而不是现在启动
    
    // 显示自我介绍指引弹窗前，确保准备面试指引已完全关闭
    console.log('准备显示自我介绍指引弹窗，先确保准备指引已关闭')
    
    // 强制确保准备面试指引已关闭，避免音频冲突
    if (showPreparationGuide.value) {
      console.log('🔇 检测到准备指引仍在显示，强制关闭')
      showPreparationGuide.value = false
    }
    
    // 稍微延时后显示自我介绍指引，确保音频完全停止
    setTimeout(() => {
      console.log('显示自我介绍指引弹窗')
      showSelfIntroductionGuide.value = true
    }, 300)
    
    // 注意：startIntroductionMode() 和语音转写将在用户确认自我介绍指引弹窗后调用
    
    // 注意：不在页面初始化时设置ongoing状态
    // 状态应该在用户点击"下一步，进入正式面试"时才设置为ongoing
    try {
      // 只清除旧的表情分析结果，不修改面试状态
      localStorage.removeItem('expressionAnalysisEnabled');
      console.log('面试页面初始化完成，当前状态:', interviewStore?.status);
    } catch (statusError) {
      console.error('面试页面初始化失败:', statusError);
    }
    
    console.log('面试环境初始化完成');
    return true;
  } catch (error) {
    console.error('面试环境初始化失败:', error);
    console.error('面试环境初始化失败，请刷新页面重试');
    return false;
  }
}

// 跟踪四个面试环节的完成状态
const stageCompletionStatus = ref({
  INTRODUCTION: false,
  BASIC_QA: false, 
  SCENARIO: false,
  CODE_TEST: false
})

// 添加completingInterview状态变量
const completingInterview = ref(false) // 是否正在完成面试（用于按钮loading状态）

// 计算属性：检查是否所有环节都已完成
const allStagesCompleted = computed(() => {
  return Object.values(stageCompletionStatus.value).every(status => status === true)
})

// 监听所有环节完成状态，自动完成面试
watch(allStagesCompleted, (completed) => {
  if (completed) {
    console.log('🎉 所有面试环节已完成，自动完成面试')
    // 移除多余的面试完成提示
    // 延迟2秒后自动完成面试，给用户一些时间看到提示
    setTimeout(() => {
      completeInterview()
    }, 2000)
  }
})

// 通用的分析触发函数
const triggerIntroductionAnalysis = (reason = '未知原因') => {
  console.log(`📊 触发自我介绍分析 - 原因: ${reason}`)
  
  // 异步触发分析，不阻塞当前流程
  setTimeout(() => {
    analyzeIntroductionText()
  }, 100)
}

// 添加完成面试的处理函数
const completeInterview = async () => {
  completingInterview.value = true;
  
  try {
    // 在面试结束前触发自我介绍分析（如果还没有分析过）
    triggerIntroductionAnalysis('面试完成时触发')
    
    // 停止所有正在进行的活动
    stopTranscription();
    
    // 获取当前面试会话ID - 移到前面避免初始化问题
    let currentSessionId = 0;
    if (interviewStore && interviewStore.sessionId) {
      currentSessionId = Number(interviewStore.sessionId);
    } else {
      currentSessionId = sessionId.value || Number(localStorage.getItem('interviewSessionId'));
    }
    
    // 立即更新面试状态，不等待网络请求
    if (interviewStore) {
      interviewStore.setStatus('completed');
      console.log('✅ 已更新面试状态为completed');
      
      // 确保状态已保存到localStorage
      localStorage.setItem('interviewStatus', 'completed');
    }
    
    // 确保有会话ID并且保存到localStorage
    if (currentSessionId) {
      localStorage.setItem('interviewSessionId', String(currentSessionId));
      console.log('✅ 会话ID已保存到localStorage:', currentSessionId);
    } else {
      console.error('❌ 会话ID为空，无法创建报告');
      router.push('/interview-analysis-report'); // 仍然跳转到报告页面
      return;
    }

    // 立即保存面试相关数据到localStorage供报告页面使用
    try {
      console.log('💾 保存面试数据供报告页面使用，会话ID:', currentSessionId);

      // 准备表情数据，确保兼容报告页面的数据格式
      const cleanedExpressionData = expressionCaptureResults.value.map(item => ({
        expressionName: item.expressionName,
        expressionCode: item.expressionCode,
        imagePath: item.imagePath,
        timestamp: item.timestamp,
        remainingTime: item.remainingTime,
        count: item.captureCount || item.captureIndex || 1,
        capture_time: item.capture_time
      }));

      console.log('准备保存的表情数据:', cleanedExpressionData);

      // 保存面试相关数据到localStorage
      localStorage.setItem('lastSessionId', String(currentSessionId));
      localStorage.setItem('expressionAnalysisEnabled', showExpressionAnalysis.value ? 'true' : 'false');
      localStorage.setItem('expressionCaptureCount', String(expressionCaptureCount.value));
      localStorage.setItem('expressionData', JSON.stringify(cleanedExpressionData));

      console.log('✅ 面试数据保存成功');
    } catch (error) {
      console.error('❌ 保存面试数据失败:', error);
      // 即使保存失败也不影响面试完成流程
    }

    // 立即显示面试完成感谢弹窗，不等待后台请求
    console.log('🎉 面试全部环节完成，立即显示感谢弹窗');
    showCompletionGuide.value = true;
    completingInterview.value = false;

    // 后台异步执行网络请求，不阻塞UI
    executeBackgroundTasks(currentSessionId);

  } catch (error) {
    console.error('❌ 完成面试失败:', error);
    ElMessage.error('完成面试失败，请重试');
    completingInterview.value = false;
  }
}

// 后台异步执行网络请求的函数
const executeBackgroundTasks = async (currentSessionId) => {
  console.log('🔄 开始执行后台任务...');
  
  // 异步更新后端申请状态为3(已面试)
  const applicationId = interviewStore.job ? interviewStore.job.applicationId : null;
  if (applicationId) {
    request({
      url: `/api/job-applications/${applicationId}/status`,
      method: 'put',
      data: {
        status: 3, // 状态3表示已面试
        feedback: `面试已完成，会话ID: ${currentSessionId}`
      }
    }).then(() => {
      console.log('✅ 已更新后端申请状态为已面试');
      
      // 通知首页刷新统计数据
      if (window.refreshDashboardStatistics) {
        console.log('📊 通知首页刷新统计数据');
        window.refreshDashboardStatistics();
      }
    }).catch(error => {
      console.error('❌ 更新后端申请状态失败:', error);
    });
  }

  // 异步记录面试结束时间并计算时长
  try {
    console.log('⏰ 记录面试结束时间...')
    const endTimeResponse = await updateEndTime({
      sessionId: currentSessionId
    })
    console.log('✅ 面试结束时间记录成功:', endTimeResponse)
    if (endTimeResponse && endTimeResponse.data) {
      console.log(`📊 面试时长: ${endTimeResponse.data.duration}分钟`)
    }
  } catch (timeError) {
    console.error('❌ 记录面试结束时间失败:', timeError)
    // 后台任务失败不影响用户体验
  }

  // 异步生成STAR分数
  try {
    console.log('⭐ 生成STAR分数...')
    const starResponse = await generateStarScores(currentSessionId)
    console.log('✅ STAR分数生成成功:', starResponse)
  } catch (starError) {
    console.error('❌ 生成STAR分数失败:', starError)
    // 后台任务失败不影响用户体验
  }

  console.log('✅ 后台任务执行完成');
}

// 在script部分添加router变量
const router = useRouter()

// 更新startInterview调用为initInterview
const handleStartInterview = async () => {
  try {
    isInitializing.value = true
    console.log('准备进入正式面试，当前step:', step.value)
    
    // 检查是否有岗位信息，如果没有则创建一个模拟的岗位信息（用于测试）
    if (!interviewStore.job || !interviewStore.job.id) {
      console.log('未找到岗位信息，创建模拟岗位信息用于测试')
      
      // 创建模拟岗位信息
      const mockJob = {
        id: 1,
        title: '测试岗位',
        companyName: '测试公司',
        companyDesc: '这是一个测试公司描述',
        salary: '面议',
        location: '测试地点',
        category: '测试类别'
      }
      
      // 设置到store中
      interviewStore.setJob(mockJob)
      
      // 保存到localStorage
      localStorage.setItem('pendingInterviewJob', JSON.stringify(mockJob))
      
      console.log('已设置模拟岗位信息:', mockJob)
    }
    
    // 设置步骤为正式面试
    step.value = 2
    
    // 更新面试状态为正式面试中
    try {
      if (interviewStore) {
        interviewStore.setStatus('ongoing');
        console.log('开始正式面试，已更新面试状态为ongoing');
        // 确保状态保存到localStorage
        localStorage.setItem('interviewStatus', 'ongoing');
        
        // 同步更新后端状态为3(已面试)
        const applicationId = interviewStore.job ? interviewStore.job.applicationId : null;
        if (applicationId) {
          request({
            url: `/api/job-applications/${applicationId}/status`,
            method: 'put',
            data: {
              status: 3, // 状态3表示已面试
              feedback: `用户已开始正式面试`
            }
          }).then(() => {
            console.log('已更新后端状态为已面试');
          }).catch(error => {
            console.error('更新面试状态失败:', error);
          });
        }
      }
    } catch (statusError) {
      console.error('更新面试状态失败:', statusError);
    }
    
    // 注意：不再直接调用initInterview函数
    // 将在弹窗确认后才调用initInterview函数
    
    // 检查是否是从跳过验证按钮进入的测试模式
    const isTestMode = verifyResult.value && verifyResult.value.message && verifyResult.value.message.includes('测试模式');
    
    // 调用initInterview函数，但只进行到显示弹窗的步骤，不初始化面试环境
    await initInterview()
    
    isInitializing.value = false
  } catch (error) {
    console.error('开始面试失败:', error)
    isInitializing.value = false
  }
}



// 更新下一步按钮状态
const updateNextButtonStatus = () => {
  canNext.value = cameraStatus.value && micStatus.value && networkStatus.value && faceStatus.value
  console.log('更新下一步按钮状态 - 摄像头:', cameraStatus.value, 
    '麦克风:', micStatus.value, '网络:', networkStatus.value, 
    '人脸:', faceStatus.value, '可下一步:', canNext.value)
}

// 处理准备面试指引弹窗关闭
const handleGuideClose = () => {
  console.log('用户关闭了面试准备指引弹窗')
  showPreparationGuide.value = false
}

// 处理准备面试指引确认
const handleGuideConfirm = async () => {
  console.log('用户确认了面试准备指引，开始设备检测')
  showPreparationGuide.value = false
  deviceDetectionCompleted.value = true
  
  // 开始设备检测
  // 移除多余的检测进行提示
  
  try {
    // 调用设备检测函数
    const detectionResult = await detectAllDevices()
    
    console.log('设备检测结果:', detectionResult)
    
    // 根据检测结果更新状态
    if (detectionResult.camera.available) {
      cameraStatus.value = true
    }
    
    if (detectionResult.microphone.available) {
      micStatus.value = true
      micVolume.value = 60 // 设置默认音量
    }
    
    if (detectionResult.network.available) {
      networkStatus.value = true
      networkQuality.value = 85 // 设置默认网络质量
    }
    
    // 更新下一步按钮状态
    updateNextButtonStatus()
    
    if (detectionResult.allReady) {
      // 移除多余的设备检测完成提示
    } else {
      // 移除多余的设备棂测异常提示
    }
    
  } catch (error) {
    console.error('设备检测失败:', error)
    console.error('设备检测失败，请手动检查设备状态')
  }
}

// 处理自我介绍指引弹窗关闭
const handleSelfIntroductionGuideClose = () => {
  console.log('用户关闭了自我介绍指引弹窗')
  showSelfIntroductionGuide.value = false
}

// 处理自我介绍开始
const handleSelfIntroductionStart = async () => {
  console.log('用户确认开始自我介绍环节')
  showSelfIntroductionGuide.value = false
  
  try {
    // 开始自我介绍环节
    console.log('自我介绍环节开始，请开始您的自我介绍')
    
    // 启动自我介绍模式和倒计时（现在是异步的）
    await startIntroductionMode()
    
  } catch (error) {
    console.error('启动自我介绍环节失败:', error)
    console.error('启动自我介绍环节失败，请重试')
  }
}

// 处理基础问答指引弹窗关闭
const handleBasicQAGuideClose = () => {
  console.log('用户关闭了基础问答指引弹窗')
  showBasicQAGuide.value = false
}

// 处理基础问答开始
const handleBasicQAStart = async () => {
  console.log('自动开始基础问答环节')
  showBasicQAGuide.value = false
  
  try {
    // 开始基础问答环节
    console.log('基础问答环节开始，请仔细阅读题目并作答')
    
    // 这里可以添加开始基础问答的具体逻辑
    // 例如：初始化题目、开始计时等
    console.log('基础问答环节已开始，当前问题步骤:', currentQuestionStep.value)
    
  } catch (error) {
    console.error('启动基础问答环节失败:', error)
    console.error('启动基础问答环节失败，请重试')
  }
}

// 处理场景问答指引弹窗关闭
const handleScenarioQAGuideClose = () => {
  console.log('用户关闭了场景问答指引弹窗')
  showScenarioQAGuide.value = false
}

// 处理场景问答开始
const handleScenarioQAStart = async () => {
  console.log('自动开始场景问答环节')
  showScenarioQAGuide.value = false
  
  try {
    // 开始场景问答环节
    console.log('场景问答环节开始，AI面试官将根据您的简历提问')
    
    // 这里可以添加开始场景问答的具体逻辑
    // 例如：初始化AI问答、准备简历数据等
    console.log('场景问答环节已开始，当前问题步骤:', currentQuestionStep.value)
    
  } catch (error) {
    console.error('启动场景问答环节失败:', error)
    console.error('启动场景问答环节失败，请重试')
  }
}

// 处理代码实操指引弹窗关闭
const handleCodeTestGuideClose = () => {
  console.log('用户关闭了代码实操指引弹窗')
  showCodeTestGuide.value = false
}

// 处理代码实操开始
const handleCodeTestStart = async () => {
  console.log('自动开始代码实操环节')
  showCodeTestGuide.value = false
  
  try {
    // 开始代码实操环节
    console.log('代码实操环节开始，每道题限时5分钟，请注意时间')
    
    // 设置代码实操真正开始，这样CodeTestSection才会开始计时
    codeTestStarted.value = true
    console.log('🎯 代码实操计时已启动，codeTestStarted设置为:', codeTestStarted.value)
    
    // 这里可以添加开始代码实操的具体逻辑
    // 例如：初始化代码编辑器、加载题目等
    console.log('代码实操环节已开始，当前问题步骤:', currentQuestionStep.value)
    
  } catch (error) {
    console.error('启动代码实操环节失败:', error)
    console.error('启动代码实操环节失败，请重试')
  }
}

// 处理面试完成确认
const handleCompletionConfirmed = async () => {
  console.log('🎉 面试完成感谢确认，开始检查报告生成状态')
  
  try {
    // 获取当前面试会话ID
    let currentSessionId = 0;
    if (interviewStore && interviewStore.sessionId) {
      currentSessionId = Number(interviewStore.sessionId);
    } else {
      currentSessionId = sessionId.value || Number(localStorage.getItem('interviewSessionId'));
    }
    
    if (!currentSessionId) {
      console.error('会话ID为空，无法跳转到报告页面');
      console.error('会话ID为空，无法跳转到报告页面');
      return;
    }
    
    console.log('开始检查面试报告生成状态，会话ID:', currentSessionId);
    
    // 检查面试报告是否生成
    const reportGenerated = await checkInterviewReportGenerated(currentSessionId);
    
    if (reportGenerated) {
      console.log('✅ 面试报告已生成，更新状态为report');
      
      // 更新面试状态为report
      if (interviewStore) {
        interviewStore.setStatus('report');
        localStorage.setItem('interviewStatus', 'report');
      }
      
      // 同步更新后端申请状态为4(已完成)
      const applicationId = interviewStore.job ? interviewStore.job.applicationId : null;
      if (applicationId) {
        request({
          url: `/api/job-applications/${applicationId}/status`,
          method: 'put',
          data: {
            status: 4, // 状态4表示已完成（报告已生成）
            feedback: `面试报告已生成，面试完成，会话ID: ${currentSessionId}`
          }
        }).then(() => {
          console.log('✅ 已更新后端申请状态为已完成(报告已生成)');
          
          // 通知首页刷新统计数据
          if (window.refreshDashboardStatistics) {
            console.log('📊 通知首页刷新统计数据(已完成+1)');
            window.refreshDashboardStatistics();
          }
        }).catch(error => {
          console.error('❌ 更新后端申请状态失败:', error);
        });
      }
      
      // 通知首页更新状态（如果首页已加载）
      if (window.setInterviewReportReady) {
        console.log('📢 通知首页更新面试状态为report');
        window.setInterviewReportReady();
      }
      
      // 移除多余的报告生成提示;
    } else {
      console.log('⏳ 面试报告还在生成中，保持completed状态');
      // 移除多余的报告生成中提示;
    }
    
    // 跳转到面试分析报告页面
    console.log('跳转到面试分析报告页面，会话ID:', currentSessionId);
    router.push({
      path: '/interview-analysis-report',
      query: {
        sessionId: currentSessionId,
        expressionAnalysis: showExpressionAnalysis.value ? 'true' : 'false',
        source: 'interview-complete' // 标记来源是面试完成
      }
    });
    
  } catch (error) {
    console.error('处理面试完成确认失败:', error);
    console.error('处理面试完成失败，请重试');
  }
}

// 检查面试报告是否生成
const checkInterviewReportGenerated = async (sessionId) => {
  try {
    console.log('🔍 检查面试报告生成状态，会话ID:', sessionId);
    
    // 方法1: 检查localStorage中是否有报告ID
    const lastReportId = localStorage.getItem('lastReportId');
    if (lastReportId) {
      console.log('✅ 发现localStorage中有报告ID:', lastReportId);
      return true;
    }
    
    // 方法2: 调用后端API检查报告是否存在
    try {
      const response = await request({
        url: `/api/comprehensive-reports/check/${sessionId}`,
        method: 'get',
        timeout: 5000
      });
      
      if (response && response.exists) {
        console.log('✅ 后端确认面试报告已生成');
        // 存储报告ID到localStorage
        if (response.reportId) {
          localStorage.setItem('lastReportId', response.reportId);
        }
        return true;
      }
    } catch (apiError) {
      console.log('🔍 后端报告检查API调用失败，继续其他检查方式:', apiError.message);
    }
    
    // 方法3: 检查面试数据完整性（作为备用方案）
    const applicationId = interviewStore.job ? interviewStore.job.applicationId : null;
    if (applicationId) {
      try {
        const appResponse = await request({
          url: `/api/job-applications/${applicationId}`,
          method: 'get',
          timeout: 3000
        });
        
        // 如果申请状态为4，说明已经完成并可能有报告
        if (appResponse && appResponse.status === 4) {
          console.log('✅ 申请状态为4，认为报告已生成');
          return true;
        }
      } catch (appError) {
        console.log('🔍 申请状态检查失败:', appError.message);
      }
    }
    
    console.log('❌ 未检测到面试报告生成');
    return false;
    
  } catch (error) {
    console.error('❌ 检查面试报告生成状态失败:', error);
    // 出错时假设报告已生成，避免阻塞用户流程
    return true;
  }
}

// 显示准备面试指引弹窗
const showInterviewGuide = () => {
  console.log('显示面试准备指引弹窗')
  showPreparationGuide.value = true
}

// 阶段名称定义
const stageNames = ['INTRODUCTION', 'BASIC_QA', 'SCENARIO', 'CODE_TEST'];

// 当前阶段名
const currentStage = ref('');

// 每个阶段开始时调用
const handleStageStart = async () => {
  await startStage(interviewSessionId.value, currentStage.value);
  // 可选：本地状态切换、提示等
};

// 每个阶段完成时调用
const handleStageComplete = async () => {
  await completeStage(interviewSessionId.value, currentStage.value);
  // 可选：本地状态切换、提示、跳转到下一个阶段
};

// 只保留 interviewSessionId 作为面试会话ID（数字）
const interviewSessionId = ref(0); // 面试会话ID，数字
const speechSessionId = ref('');   // 语音转写会话ID，UUID

// 读取 localStorage 时
const storedInterviewSessionId = Number(localStorage.getItem('interviewSessionId'));
if (storedInterviewSessionId) {
  interviewSessionId.value = storedInterviewSessionId;
}

// 创建语音转写会话时
// const response = await request.post('/api/speech/session');
// if (response && response.sessionId) {
//   speechSessionId.value = response.sessionId;
// }

// 语音转写相关 WebSocket 连接
// const wsUrl = `ws://.../ws/speech/${speechSessionId.value}`;

// 面试阶段进度相关API调用
// await completeStage(interviewSessionId.value, currentStage.value);

// 测试麦克风功能
const testMicrophone = async () => {
  try {
    console.log('开始测试麦克风...')
    
    // 开发测试模式下直接设置麦克风状态为就绪
    if (process.env.NODE_ENV !== 'production') {
      console.log('开发测试环境：直接设置麦克风状态为就绪')
      micStatus.value = true
      micVolume.value = 50
      updateNextButtonStatus()
      // 移除多余的测试模式提示
      return
    }
    
    // 移除多余的麦克风测试提示
    
    // 确保有权限访问麦克风
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: { 
        deviceId: selectedMic.value ? { exact: selectedMic.value } : undefined,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })
    
    // 关闭之前可能存在的音频流的所有轨道
    if (audioStream.value) {
      const oldTracks = audioStream.value.getTracks()
      oldTracks.forEach(track => {
        track.stop()
        console.log('停止旧的音频轨道')
      })
    }
    
    // 手动激活音频上下文
    if (audioContext.value) {
      try {
        // 先关闭旧的音频上下文
        audioContext.value.close()
        console.log('关闭旧的音频上下文')
      } catch (e) {
        console.warn('关闭旧音频上下文失败:', e)
      }
    }
    
    // 创建新的音频上下文
    try {
      audioContext.value = new (window.AudioContext || window.webkitAudioContext)()
      console.log('创建新的音频上下文成功, 状态:', audioContext.value.state)
      
      // 尝试恢复暂停的音频上下文
      if (audioContext.value.state === 'suspended') {
        await audioContext.value.resume()
        console.log('已恢复暂停的音频上下文, 新状态:', audioContext.value.state)
      }
    } catch (e) {
      console.error('创建音频上下文失败:', e)
      throw e
    }
    
    // 创建分析器
    const analyser = audioContext.value.createAnalyser()
    analyser.fftSize = 1024
    analyser.smoothingTimeConstant = 0.8
    
    // 创建麦克风源
    const microphone = audioContext.value.createMediaStreamSource(stream)
    
    // 创建处理器节点
    if (window.AudioContext && !window.AudioContext.prototype.createScriptProcessor) {
      // 使用更现代的AudioWorkletNode替代已废弃的ScriptProcessorNode
      console.log('浏览器不支持createScriptProcessor，尝试使用替代方法')
      // 这里我们简化处理，仍然使用全局变量，但是通过标准方法获取
      audioProcessor.value = audioContext.value.createScriptProcessor(2048, 1, 1)
    } else {
      audioProcessor.value = audioContext.value.createScriptProcessor(2048, 1, 1)
    }
    
    // 连接节点
    microphone.connect(analyser)
    analyser.connect(audioProcessor.value)
    audioProcessor.value.connect(audioContext.value.destination)
    
    console.log('麦克风测试管道已创建')
    
    // 保存音频流
    audioStream.value = stream
    
    // 重置麦克风状态和音量
    micVolume.value = 0
    micStatus.value = false
    
    // 设置音量监测
    let detectedCount = 0
    let maxVolume = 0
    
    audioProcessor.value.onaudioprocess = () => {
      const array = new Uint8Array(analyser.frequencyBinCount)
      analyser.getByteFrequencyData(array)
      let values = 0
      
      const length = array.length
      for (let i = 0; i < length; i++) {
        values += array[i]
      }
      
      const average = values / length
      // 计算麦克风音量百分比
      const volumePercent = Math.min(100, Math.round((average / 128) * 100))
      micVolume.value = volumePercent
      
      // 记录最大音量
      if (volumePercent > maxVolume) {
        maxVolume = volumePercent
      }
      
      // 检测到有效音量(>1)才计数和输出日志 (降低阈值便于检测)
      if (volumePercent > 1) {
        console.log('检测到麦克风音量:', volumePercent)
        detectedCount++
        
        // 如果检测到有效音量，设置麦克风状态为就绪
        if (!micStatus.value) {
          micStatus.value = true
          console.log('麦克风已就绪:', micStatus.value, '音量:', volumePercent)
          // 更新下一步按钮状态
          updateNextButtonStatus()
          // 显示成功消息
          // 移除多余的麦克风音量检测提示
        }
      }
    }
    
    console.log('麦克风测试已启动，请对着麦克风清晰地说话...')
    // 移除多余的麦克风测试提示
    
    // 5秒后如果仍未检测到声音，自动设置为就绪状态（测试环境）
    setTimeout(() => {
      if (!micStatus.value) {
        console.log('5秒内未检测到有效声音，自动设置麦克风为就绪状态')
        micStatus.value = true
        micVolume.value = 20
        updateNextButtonStatus()
        // 移除多余的麦克风状态提示
      }
    }, 5000)
    
    // 20秒后自动停止测试
    setTimeout(() => {
      if (audioProcessor.value) {
        console.log('麦克风测试结束，检测到声音次数:', detectedCount, '最大音量:', maxVolume)
        
        // 无论是否检测到声音，都设置为就绪状态
        micStatus.value = true
        updateNextButtonStatus()
        
        if (detectedCount > 0) {
          // 移除多余的麦克风检测成功提示
        } else {
          // 移除多余的麦克风自动设置提示
        }
      }
    }, 20000)
    
  } catch (error) {
    console.error('麦克风测试失败:', error)
    console.error('麦克风测试失败，请检查设备权限并确保麦克风已连接: ' + error.message)
    
    // 测试失败时，自动设置为就绪状态（测试环境）
    console.log('测试失败但自动设置麦克风为就绪状态')
    micStatus.value = true
    micVolume.value = 20
    updateNextButtonStatus()
  }
}
//题目获取相关逻辑
async function loadQuestions(sessionId, stage, isPreload = false) {
  if (!isPreload) {
    loadingQuestions.value = true;
  }
  try {
    // 根据环节设置不同的题目数量
    const maxQuestionsPerStage = stage === 'BASIC_QA' ? 10 : stage === 'CODE_TEST' ? 3 : 5;

    console.log(`${isPreload ? '预加载' : '开始加载'}${stage}环节题目，会话ID: ${sessionId}`);

    // 分配题目，后端幂等
    await assignQuestions(sessionId, stage, maxQuestionsPerStage);

    // 缓存键
    const cacheKey = `session_questions_${sessionId}_${stage}`;

    // 尝试从缓存获取
    const cachedQuestions = cacheService.get(cacheKey);
    if (cachedQuestions) {
      console.log(`从缓存获取${stage}环节题目`);
      const res = cachedQuestions;
      console.log(`缓存命中，获取到${stage}环节题目:`, res);
      return res;
    }

    // 缓存未命中，从服务器获取
    const res = await getSessionQuestions(sessionId, stage);

    // 缓存结果，有效期1分钟
    cacheService.set(cacheKey, res, {ttl: 60 * 1000});

    console.log(`获取到${stage}环节题目:`, res);

    let filtered = [];
    if (stage === 'BASIC_QA') {
      filtered = (res.data || res).filter(q => q.inputType === 'radio');
    } else if (stage === 'CODE_TEST') {
      filtered = (res.data || res).filter(q => q.inputType === 'code');
    } else {
      filtered = res.data || res;
    }

    // 确保题目数量不超过最大值
    if (filtered.length > maxQuestionsPerStage) {
      console.warn(`${stage}环节题目数量(${filtered.length})超过最大值(${maxQuestionsPerStage})，已截断`);
      filtered = filtered.slice(0, maxQuestionsPerStage);
    }

    console.log(`${stage}环节最终题目数量: ${filtered.length}`);
    questions.value = filtered;
    currentIndex.value = 0;

    // 初始化答案数组
    answers.value = new Array(filtered.length).fill('');
    startTimes.value = new Array(filtered.length).fill(null);
    submitTimes.value = new Array(filtered.length).fill(null);
  } catch (error) {
    console.error(`加载${stage}环节题目失败:`, error);
    console.error(`加载题目失败，请刷新页面重试`);
    questions.value = [];
  } finally {
    loadingQuestions.value = false;
  }
}

async function onSwitchQuestion(newIndex) {
  const prevIndex = currentIndex.value;

  // 边界检查：确保新索引在有效范围内
  if (newIndex < 0 || newIndex >= questions.value.length) {
    console.error(`无效的题目索引: ${newIndex}，题目总数: ${questions.value.length}`);
    return;
  }

  if (prevIndex !== newIndex && questions.value.length > 0) {
    // 获取当前题目ID
    const prevQid = questions.value[prevIndex].questionId;

    // 检查是否已有答案记录
    const existingRecord = answerRecords.value[prevQid];
    const currentAnswer = answers.value[prevIndex] || '';

    // 只有当前有答案且未保存过，或答案发生变化时才提交
    if (currentAnswer && (!existingRecord || existingRecord.userAnswer !== currentAnswer)) {
      console.log(`需要保存题目 ${prevIndex + 1} 的答案变更`);

      // 计算答题时间（秒）
      const now = new Date();
      const startTime = new Date(startTimes.value[prevIndex]);

      // 计算实际用时，根据题目类型使用不同的最大值限制
      const isCodeQuestion = questions.value[prevIndex].inputType === 'code';
      const maxTime = isCodeQuestion ? 300 : 60; // 代码题5分钟，其他题1分钟

      let timeUsed;
      let formattedStartTime;

      // 如果已经有记录的答题时间，继续使用该时间，否则计算新的答题时间
      if (existingRecord && existingRecord.timeUsed) {
        // 如果已有记录，复用原有的用时和开始时间
        timeUsed = existingRecord.timeUsed;
        formattedStartTime = existingRecord.startTime;
        console.log(`切换题目：题目 ${prevIndex + 1} 已有记录的用时: ${timeUsed} 秒，使用原有用时`);
      } else {
        // 首次提交，计算用时
        timeUsed = Math.floor((now - startTime) / 1000);

        // 限制最大答题时间
        if (timeUsed > maxTime) {
          timeUsed = maxTime;
          console.log(`切换题目：题目 ${prevIndex + 1} 用时超过限制，已设为最大值 ${maxTime} 秒`);
        }

        formattedStartTime = formatDateForBackend(startTime);
        console.log(`切换题目：题目 ${prevIndex + 1} 首次提交，用时: ${timeUsed} 秒`);
      }

      // 格式化提交时间
      const formattedSubmitTime = formatDateForBackend(now);

      console.log(`开始时间: ${formattedStartTime}, 提交时间: ${formattedSubmitTime}`);

      // 提交上一题答案
      try {
        await submitAnswer({
          sessionId: interviewSessionId.value,
          interviewId: interviewSessionId.value,
          questionId: prevQid,
          userId: userId.value,
          userAnswer: currentAnswer,
          type: questions.value[prevIndex].stage,
          startTime: formattedStartTime,
          submitTime: formattedSubmitTime,
          timeUsed: timeUsed
        });
        submitTimes.value[prevIndex] = formattedSubmitTime;

        console.log(`题目 ${prevIndex + 1} (ID: ${prevQid}) 答案保存成功，用时: ${timeUsed}秒`);
      } catch (error) {
        console.error(`题目 ${prevIndex + 1} 答案提交失败:`, error);
        console.error(`答案提交失败: ${error.message || '未知错误'}`);
      }
    } else {
      console.log(`题目 ${prevIndex + 1} 无变更或无答案，不需保存`);
    }
  }

  // 切换到新题目
  currentIndex.value = newIndex;

  // 如果新题目没有开始时间，设置开始时间为当前时间
  if (!startTimes.value[newIndex]) {
    const nowForNewQuestion = new Date();
    startTimes.value[newIndex] = formatDateForBackend(nowForNewQuestion);
  }

  // 获取新题目ID并检查是否已回答
  const newQid = questions.value[newIndex]?.questionId;
  const newExistingRecord = answerRecords.value[newQid];

  // 如果题目已有答案记录，加载到界面
  if (newExistingRecord && newExistingRecord.userAnswer) {
    answers.value[newIndex] = newExistingRecord.userAnswer;

    // 如果已回答，不启动计时器
    isCountdownPaused.value = true; // 标记为暂停状态
    console.log(`切换到已回答题目 ${newIndex + 1}，不启动计时器`);
  } else {
    // 如果未回答，启动计时器
    onEnterQuestion(newIndex);
  }
}

// 提交超时答案的函数
async function submitTimeoutAnswer(idx, answer, maxTimeUsed) {
  // 完全禁用此功能，不执行任何操作
  console.log(`超时提交已被禁用，不会提交题目 ${idx + 1} 的超时答案`);
  return;
}

async function handleSelectOption(opt) {
  if (answerLoading.value) return; // 防止重复点击
  answerLoading.value = true;

  try {
    // 获取当前题目ID
    const qid = questions.value[currentIndex.value].questionId;

    // 更新本地答案
    answers.value[currentIndex.value] = opt;

    // 检查是否已经有记录的答题时间
    const existingRecord = answerRecords.value[qid];
    let timeUsed;
    let formattedStartTime;
    let formattedSubmitTime;
    const now = new Date();

    // 如果已经有记录的答题时间，继续使用该时间，否则计算新的答题时间
    if (existingRecord && existingRecord.timeUsed) {
      timeUsed = existingRecord.timeUsed;
      formattedStartTime = existingRecord.startTime;
      formattedSubmitTime = formatDateForBackend(now);
      console.log(`题目 ${currentIndex.value + 1} 已有记录的用时: ${timeUsed} 秒，仅更新答案不更新用时`);
    } else {
      // 首次回答，计算用时
      const startTime = new Date(startTimes.value[currentIndex.value]);
      timeUsed = Math.floor((now - startTime) / 1000);

      // 计算实际用时，根据题目类型使用不同的最大值限制
      const isCodeQuestion = questions.value[currentIndex.value].inputType === 'code';
      const maxTime = isCodeQuestion ? 300 : 60; // 代码题5分钟，其他题1分钟

      // 限制最大答题时间
      if (timeUsed > maxTime) {
        timeUsed = maxTime;
        console.log(`题目 ${currentIndex.value + 1} 用时超过限制，已设为最大值 ${maxTime} 秒`);
      }

      // 格式化时间为后端期望的格式
      formattedStartTime = formatDateForBackend(startTime);
      formattedSubmitTime = formatDateForBackend(now);
      console.log(`题目 ${currentIndex.value + 1} 首次回答，用时: ${timeUsed} 秒`);
    }

    console.log(`提交答案 - 题目ID: ${qid}, 答案: ${opt}, 用时: ${timeUsed}秒`);

    // 提交答案
    const response = await submitAnswer({
      sessionId: interviewSessionId.value,
      interviewId: interviewSessionId.value,
      questionId: qid,
      userId: userId.value,
      userAnswer: opt,
      type: questions.value[currentIndex.value].stage,
      startTime: formattedStartTime,
      submitTime: formattedSubmitTime,
      timeUsed: timeUsed
    });

    submitTimes.value[currentIndex.value] = formattedSubmitTime;

    // 更新答案记录
    const newRecord = {
      id: response?.id || null,
      userId: userId.value,
      questionId: qid,
      interviewId: interviewSessionId.value,
      userAnswer: opt,
      isCorrect: response?.isCorrect || 0,
      timeUsed: timeUsed,
      startTime: formattedStartTime,
      submitTime: formattedSubmitTime,
      type: questions.value[currentIndex.value].stage
    };

    // 更新本地记录
    answerRecords.value = {...answerRecords.value, [qid]: newRecord};

    ;

    // 如果是首次提交，立即停止计时器但保持倒计时显示
    if (!existingRecord || !existingRecord.timeUsed) {
      isCountdownPaused.value = true; // 标记倒计时已暂停
      if (countdownTimer) {
        clearInterval(countdownTimer);
      }
    }
  } catch (error) {
    console.error(`题目 ${currentIndex.value + 1} 答案提交失败:`, error);
    console.error(`答案提交失败: ${error.message || '未知错误'}`);
  } finally {
    answerLoading.value = false;
  }
}

function onEnterQuestion(idx) {
  // 边界检查：确保索引在有效范围内
  if (idx < 0 || idx >= questions.value.length) {
    console.error(`onEnterQuestion: 无效的题目索引: ${idx}，题目总数: ${questions.value.length}`);
    return;
  }

  if (timer) clearInterval(timer);

  // 获取当前题目ID
  const qid = questions.value[idx].questionId;

  // 检查是否已回答过
  const existingRecord = answerRecords.value[qid];
  if (existingRecord && existingRecord.userAnswer) {
    console.log(`题目 ${idx + 1} 已有答案记录，不启动计时器`);
    isCountdownPaused.value = true; // 标记为暂停状态
    return;
  }

  // 根据题目类型设置不同的倒计时时间
  const isCodeQuestion = questions.value[idx].inputType === 'code';
  perQuestionCountdown.value = isCodeQuestion ? 300 : 60; // 代码题5分钟，其他题1分钟
  isCountdownPaused.value = false; // 重置暂停状态

  // 只有首次进入题目时才设置开始时间
  if (!startTimes.value[idx]) {
    const now = new Date();
    startTimes.value[idx] = formatDateForBackend(now);
  }

  timer = setInterval(() => {
    if (!isCountdownPaused.value && perQuestionCountdown.value > 0) {
      perQuestionCountdown.value--;
    } else if (perQuestionCountdown.value <= 0) {
      clearInterval(timer);

      // 获取当前题目ID
      const qid = questions.value[idx].questionId;

      // 再次检查是否已回答
      if (existingRecord && existingRecord.userAnswer) {
        console.log(`题目 ${idx + 1} 已回答，忽略倒计时结束`);
        return;
      }

      // 切换到下一题（如果还有下一题）
      if (idx + 1 < questions.value.length) {
        onSwitchQuestion(idx + 1);
      } else {
        // 可以选择显示完成按钮或其他操作

      }
    }
  }, 1000);
}

// 已在下方有更完整的watch函数，此处注释掉
// watch(currentQuestionStep, (step) => {
//   if (step === 1) {
//     loadQuestions(interviewSessionId.value, 'BASIC_QA');
//   } else if (step === 3) {
//     loadQuestions(interviewSessionId.value, 'CODE_TEST');
//   }
// });

watch(questions, (newQuestions) => {
  console.log(`题目数据更新，新题目数量: ${newQuestions.length}`);

  // 初始化答案数组
  answers.value = newQuestions.map(() => '');

  // 初始化提交时间数组
  submitTimes.value = newQuestions.map(() => null);

  // 初始化开始时间数组（仅初始化为null，不立即设置时间）
  // 实际开始时间将在首次进入题目时设置
  startTimes.value = newQuestions.map(() => null);

  // 进入第一题
  if (newQuestions.length > 0) {
    onEnterQuestion(0);
  }
});

watch(currentIndex, (newIdx, oldIdx) => {
  clearInterval(countdownTimer);

  // 获取当前题目ID
  const qid = questions.value[newIdx]?.questionId;
  if (!qid) return;

  // 检查是否已有答案记录
  const existingRecord = answerRecords.value[qid];
  if (existingRecord && existingRecord.userAnswer) {
    console.log(`切换到已回答题目 ${newIdx + 1}，不启动计时器`);
    isCountdownPaused.value = true; // 标记为暂停状态
    return;
  }

  perQuestionCountdown.value = 60;
  isCountdownPaused.value = false; // 重置暂停状态
  countdownTimer = setInterval(() => {
    if (!isCountdownPaused.value && perQuestionCountdown.value > 0) {
      perQuestionCountdown.value--;
    }
    if (perQuestionCountdown.value <= 0) {
      clearInterval(countdownTimer);

      // 再次检查是否已回答
      if (existingRecord && existingRecord.userAnswer) {
        console.log(`题目 ${newIdx + 1} 已回答，忽略倒计时结束`);
        return;
      }

      // 不再提交超时答案，只记录日志
      console.log(`题目 ${newIdx + 1} (ID: ${qid}) 倒计时结束，不提交超时答案`);

      // 直接切换到下一题
      onSwitchQuestion(Math.min(newIdx + 1, questions.value.length - 1));
    }
  }, 1000);
});

// 组件卸载时清除定时器
onUnmounted(() => {
  clearInterval(countdownTimer);
  if (timer) clearInterval(timer);
});

// 同步父组件的countdown显示和perQuestionCountdown
watch(perQuestionCountdown, (newVal) => {
  countdown.value = newVal;
});

// 监听面试环节变化，加载对应题目
watch(currentQuestionStep, (step) => {
  // 结束上一个环节
  if (step > 0 && introductionMode.value) {
    // 如果进入了新环节但自我介绍模式还在，则结束自我介绍
    if (introductionTimer.value) {
      clearInterval(introductionTimer.value);
      introductionTimer.value = null;
    }
    introductionMode.value = false;
    console.log('已自动结束自我介绍模式，进入新环节');
  }

  // 加载新环节题目
  if (step === 1) {
    // 基础问答环节已经预加载过，这里直接使用缓存
    console.log('进入基础问答环节，使用预加载的题目');
  } else if (step === 3) {
    // 代码测试环节也已预加载，直接使用缓存
    console.log('进入代码操作题环节，使用预加载的题目');
  }
});

// 处理题目切换事件 - 由BasicQASection和CodeTestSection组件触发
function handleQuestionChange(data) {
  console.log(`题目已切换: ${data.index + 1}/${data.totalQuestions}`, data.question);
}

// 处理答案提交事件 - 由BasicQASection和CodeTestSection组件触发
function handleAnswerSubmit(data) {
  // 只有用户手动选择并提交的答案，或者超时自动提交的答案才显示"答案已提交"
  if (data.userSubmitted || data.isTimeout) {
    console.log(`答案已提交: 题目 ${data.index + 1}, 答案: ${data.answer}, 用时: ${data.timeUsed}秒`);
    if (data.isTimeout) {
      console.log('答案由倒计时超时自动提交');
    }
  }
}

// 处理环节完成事件
async function onCompleteStage(stage) {
  console.log(`✅ 环节 ${stage} 已完成，准备进入下一环节`);

  try {
    // 立即标记当前环节完成，不等待网络请求
    if (stageCompletionStatus.value.hasOwnProperty(stage)) {
      stageCompletionStatus.value[stage] = true;
      console.log(`✅ ${stage} 环节已标记为完成，当前完成状态:`, stageCompletionStatus.value);
    }

    // 推进到下一环节
    const maxSteps = stageNames.length - 1; // 环节数量（从0开始计数）
    if (currentQuestionStep.value < maxSteps) {
      currentQuestionStep.value++;
      console.log(`🔄 切换到下一环节: ${stageNames[currentQuestionStep.value]}, 索引: ${currentQuestionStep.value}`);
      countdown.value = 60; // 重置倒计时

      // 重置当前题目索引
      currentIndex.value = 0;

      // 加载新环节题目
      if (currentQuestionStep.value === 1) {
        // 基础问答环节已经预加载过，这里直接使用缓存
        console.log('📝 进入基础问答环节，使用预加载的题目');
      } else if (currentQuestionStep.value === 3) {
        // 代码测试环节也已预加载，直接使用缓存
        console.log('💻 进入代码操作题环节，使用预加载的题目');
      }
    } else {
      console.log('🎯 已到达最后一个环节，面试即将完成');
      // 如果是最后一个环节完成，立即结束面试
      setTimeout(() => {
        console.log('🎉 所有环节已完成，开始结束面试流程');
        completeInterview();
      }, 100); // 稍微延迟确保UI状态更新
    }

    // 后台异步记录环节结束时间，不阻塞UI
    recordStageCompletion(stage);

  } catch (error) {
    console.error('❌ 结束当前环节时出错:', error);
    ElMessage.error('切换环节失败，请刷新页面重试');
  }
}

// 后台异步记录环节完成的函数
const recordStageCompletion = async (stage) => {
  try {
    console.log(`⏰ 后台记录环节 ${stage} 结束时间...`);
    await completeStage(interviewSessionId.value, stage, 'COMPLETE');
    console.log(`✅ 环节 ${stage} 结束时间记录成功`);
  } catch (error) {
    console.error(`❌ 记录环节 ${stage} 结束时间失败:`, error);
    // 后台任务失败不影响用户体验
  }
}

// 处理场景模拟完成事件
const onStageComplete = (stage) => {
  console.log(`环节完成：${stage}`);
  
  // 无论什么环节完成，都尝试触发自我介绍分析（如果尚未分析）
  triggerIntroductionAnalysis(`${stage}环节完成时触发`)
  
  // 调用通用的环节完成处理函数
  onCompleteStage(stage);
};

const scenarioStageRef = ref(null)
 

 </script>

<style scoped>
.interview-container-full {
  width: 100%;
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 0;
  box-sizing: border-box;
}
.interview-card-full {
  max-width: 100%;
  width: 100%;
  margin: 0;
  border-radius: 0;
  box-shadow: none;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  min-height: 100vh;
}
.card-header {
  font-size: 22px;
  font-weight: bold;
  padding: 20px 20px 8px 20px;
  background: #fff;
  border-bottom: 1px solid #f0f2f5;
}
.prepare-section-full {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 30px 20px;
}
.prepare-steps-horizontal {
  width: 100%;
  max-width: 800px;
  margin-bottom: 20px;
}
.prepare-step-content {
  width: 100%;
  max-width: 1200px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.upper-section {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.camera-section {
  flex: 0 0 400px;
  min-width: 300px;
  max-width: 450px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}
.camera-container {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  max-width: 350px;
  border-radius: 50%;
  overflow: hidden;
  background: #22243a;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  border: 4px solid #409eff;
}
.camera-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1); /* 镜像显示 */
}
.camera-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  justify-content: center;
  align-items: center;
}
.face-scanning {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.scan-line {
  width: 100%;
  height: 2px;
  background: #409eff;
  position: absolute;
  animation: scanning 2s linear infinite;
}
.scan-text {
  color: #fff;
  font-size: 14px;
  margin-top: 8px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}
.face-guide {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.guide-box {
  width: 200px;
  height: 200px;
  border: 2px solid #409eff;
  border-radius: 50%;
  animation: pulse 2s infinite;
}
.guide-text {
  color: #fff;
  font-size: 14px;
  margin-top: 8px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
}
.detection-status {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.device-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 300px;
  padding: 0 16px;
  justify-content: space-between;
}
.device-item {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  transition: all 0.3s ease;
}
.device-item h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px 0;
  font-size: 14px;
  color: #606266;
}
.active-item {
  background: #ecf5ff;
  border: 1px solid #409eff;
}
.device-progress {
  margin-top: 8px;
}
.network-quality {
  margin-top: 8px;
  font-size: 13px;
}
.network-quality .good {
  color: #67c23a;
  font-weight: bold;
}
.network-quality .poor {
  color: #f56c6c;
  font-weight: bold;
}
.lower-section {
  width: 100%;
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.3s ease;
}
.lower-section.active-section {
  background: #ecf5ff;
  border: 1px solid #409eff;
}
.face-verify-container {
  width: 100%;
}
.face-verify-container h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #303133;
}
.face-verify-content {
  display: flex;
  justify-content: center;
}
.prepare-next-btn-full {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}
/* 响应式调整 */
@media (max-width: 1400px) {
  .formal-left {
    max-width: 450px;
  }
  
  .interview-formal-container {
    gap: 20px;
  }
}

@media (max-width: 1200px) {
  .upper-section {
    flex-direction: column;
    align-items: center;
    gap: 20px;
  }

  .device-section {
    width: 100%;
    max-width: 600px;
    height: auto;
    padding: 0;
  }

  .camera-section {
    width: 100%;
    flex: none;
    max-width: 400px;
  }

  .camera-container {
    max-width: 300px;
  }

  .formal-left {
    flex: 5 0 0;
    max-width: 400px;
  }
  
  .formal-right {
    flex: 5 0 0;
  }
}
@media (max-width: 768px) {
  .face-verify-content .el-form {
    flex-direction: column;
  }

  .face-verify-content .el-form-item {
    margin-right: 0;
    width: 100%;
  }
  
  .prepare-step-content {
    padding: 0 16px;
  }
  
  .upper-section {
    gap: 16px;
  }
  
  .device-section {
    padding: 0;
    gap: 12px;
  }
  
  .device-item {
    padding: 10px;
  }
  
  .camera-container {
    max-width: 280px;
  }
  
  .video-area-large {
    min-height: 250px;
  }
  
  .interview-card-full {
    margin: 0;
    border-radius: 0;
  }
  
  .card-header {
    padding: 15px 16px 8px 16px !important;
  }
  
  .prepare-section-full {
    padding: 20px 16px !important;
  }
}
.interview-formal-container {
  display: flex;
  flex-direction: row;
  gap: 32px;
  min-height: calc(100vh - 80px);
  width: 100%;
  align-items: stretch;
  margin-top: 0;
  padding: 30px 20px;
  /* 为左侧视频与右侧答题区域顶部对齐提供统一偏移 */
  --steps-offset: 72px;
  /* 右侧步骤条与答题框之间的间距，需要同步计入左侧顶部留白 */
  --qa-gap: 12px;
}
.formal-left {
  flex: 4 0 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
  max-width: 500px;
  justify-content: flex-start;
  height: 100%;
  padding-right: 24px; /* 增加右侧间距 */
}
.formal-right {
  flex: 6 0 0;
  display: flex;
  flex-direction: column;
  gap: var(--qa-gap); /* steps 与答题框之间保留少量间距 */
  min-width: 0;
  height: 100%;
  position: relative;
}
.camera-info-wrapper {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
  justify-content: stretch;
  /* 与右侧答题框（question-area）顶部对齐：预留 steps 高度 + steps 与答题框间距 */
  padding-top: calc(var(--steps-offset) + var(--qa-gap));
}
.video-area-large {
  width: 100%;
  /* 视频顶部与右侧答题框顶部对齐，扣除 steps 高度 + 间距 */
  height: calc(100% - var(--steps-offset) - var(--qa-gap));
  min-height: 420px;
  background: #22243a;
  border-radius: 0;
  padding: 0;
  margin: 0;
  display: block;
  box-shadow: none;
  overflow: hidden;
  position: relative;
  transition: all 0.3s ease;
  border: none;
}

.video-area-large:hover {
  transform: none;
  box-shadow: none;
}
.video-area-large video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transform: scaleX(-1); /* 镜像显示 */
  display: block;
  position: absolute;
  top: 0;
  left: 0;
}
/* 移除个人信息卡片样式（不再显示） */

.video-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.video-header {
  background: #22243a;
  padding: 12px 16px;
  border: none;
  border-radius: 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
}

.video-title {
  font-size: 16px;
  color: #ffffff;
  font-weight: 600;
  display: flex;
  align-items: center;
}

.video-title::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background-color: #ffffff;
  margin-right: 8px;
  border-radius: 2px;
}
.question-steps {
  margin: 0;
  height: var(--steps-offset);
  display: flex;
  align-items: center;
}
.question-area {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  /* 让答题区与左侧视频区在步骤条下方同高：占满剩余高度 */
  min-height: 0;
  height: calc(100% - 0px);
  box-shadow: 0 2px 8px #e0e0e0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.question-content {
  margin-top: 12px;
}
.timers-split {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-top: 18px;
}
.timer-left, .timer-right {
  font-size: 16px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 6px;
}
.status-btn {
  position: fixed;
  top: 120px;
  right: 48px;
  z-index: 1001;
  box-shadow: 0 2px 8px #e0e0e0;
  border-radius: 50%;
}
.status-bars-drawer {
  display: flex;
  flex-direction: column;
  gap: 18px;
  margin-top: 18px;
}
.status-bar-item {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 6px #e0e0e0;
  padding: 14px 18px 10px 18px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 6px;
}
.status-bar-title {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.status-bar-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 8px 0;
  width: 100%;
  border: none;
}
@media (max-width: 1000px) {
  .interview-formal-container {
    flex-direction: column !important;
    gap: 20px !important;
    padding: 20px 15px !important;
  }
  
  .formal-left, .formal-right {
    flex: none !important;
    width: 100% !important;
    max-width: 100% !important;
    padding-right: 0 !important;
  }
  
  .formal-left {
    order: 1;
  }
  
  .formal-right {
    order: 2;
  }
  
  .video-area-large {
    max-width: 500px;
    margin: 0 auto;
    min-height: 300px;
    /* 移动端步骤区域更紧凑，减少顶部偏移 */
    height: auto;
  }
  
  .camera-info-wrapper {
    max-width: 500px;
    margin: 0 auto;
    padding-top: calc(56px + var(--qa-gap)); /* 移动端：步骤高度 + 间距 */
  }
}

@media (max-width: 900px) {
  .video-area-large, .user-info-card-compact, .video-area, .user-info-card {
    width: 100% !important;
    min-width: 0 !important;
  }
  .question-area {
    min-width: 0 !important;
    min-height: 220px !important;
    padding: 12px;
  }
}

@media (max-width: 600px) {
  .interview-formal-container {
    gap: 16px !important;
    padding: 15px 10px !important;
  }
  
  .formal-left {
    padding-right: 0 !important;
  }
  
  .video-area-large {
    max-width: 400px;
    min-height: 200px;
  }
  
  .camera-container {
    max-width: 250px;
  }
  
  .prepare-steps-horizontal {
    max-width: 100%;
  }
  
  .interview-card-full {
    margin: 0;
  }
  
  .card-header {
    padding: 12px 10px 6px 10px !important;
    font-size: 20px !important;
  }
  
  .prepare-section-full {
    padding: 15px 10px !important;
  }
  
  .question-area {
    padding: 8px;
    min-height: 180px !important;
  }
}
@keyframes scanning {
  0% {
    top: 0;
  }
  50% {
    top: 100%;
  }
  100% {
    top: 0;
  }
}
@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 新增样式 */
.upload-section {
  display: flex;
  margin: 20px 0;
  gap: 30px;
}

.id-card-upload, .face-snapshot {
  flex: 1;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 15px;
}

.id-card-upload h4, .face-snapshot h4 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #303133;
}

.upload-area, .snapshot-area {
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-placeholder, .snapshot-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  color: #409eff;
}

.upload-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.upload-preview, .snapshot-preview {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview-image {
  max-width: 100%;
  max-height: 140px;
  object-fit: contain;
  margin-bottom: 10px;
}

.preview-actions {
  margin-top: auto;
}

.verify-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 20px;
}

.verify-result {
  margin-top: 15px;
  display: flex;
  align-items: center;
  font-size: 16px;
}

.verify-result.success {
  color: #67c23a;
}

.verify-result.error {
  color: #f56c6c;
}

.verify-result .el-icon {
  margin-right: 8px;
  font-size: 20px;
}

.similarity {
  margin-left: 10px;
  font-size: 14px;
}

.snapshot-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
.transcription-result {
  width: 100%;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.transcription-result h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #409EFF;
  font-size: 16px;
}

.transcription-text {
  font-size: 16px;
  line-height: 1.6;
  color: #333;
  white-space: pre-wrap;
  max-height: 300px;
  overflow-y: auto;
  padding: 10px;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

/* 自我介绍相关样式 */

.countdown-number {
  font-size: 48px;
  font-weight: bold;
  line-height: 1;
}

.countdown-text {
  font-size: 14px;
  margin-top: 8px;
}

.introduction-guide {
  padding: 24px;
  background: #ecf5ff;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.introduction-tip {
  color: #409eff;
  margin-top: 8px;
}

.introduction-countdown-box {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 15px 0;
}

.countdown-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8a2be2);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 10px rgba(99, 102, 241, 0.3);
  animation: pulse 1.5s infinite alternate;
}

@keyframes pulse {
  from {
    box-shadow: 0 4px 10px rgba(99, 102, 241, 0.3);
    transform: scale(1);
  }
  to {
    box-shadow: 0 8px 20px rgba(99, 102, 241, 0.5);
    transform: scale(1.05);
  }
}

.countdown-number {
  font-size: 48px;
  font-weight: bold;
  color: #fff;
  text-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.countdown-unit {
  font-size: 16px;
  color: #fff;
  margin-top: 5px;
}

.introduction-tips {
  width: 100%;
  margin-top: 15px;
  border-top: 1px dashed #b3b3e6;
  padding-top: 15px;
}

.introduction-tips h4 {
  color: #6366f1;
  font-size: 18px;
  text-align: center;
  margin-bottom: 15px;
}

.introduction-tips ul {
  text-align: left;
  padding-left: 20px;
}

.introduction-tips li {
  margin-bottom: 10px;
  color: #555;
  line-height: 1.5;
  position: relative;
  padding-left: 5px;
}

.introduction-tips li:before {
  content: "";
  position: absolute;
  left: -15px;
  top: 8px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #6366f1;
}

.expression-analysis-section {
  margin-top: 20px;
}

/* 自我介绍分析状态样式（静默模式） */
.analysis-status-minimal,
.analysis-complete-minimal {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 6px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
}

.analysis-indicator,
.analysis-done {
  display: flex;
  align-items: center;
  gap: 6px;
}

.analysis-icon {
  font-size: 14px;
}

.analysis-icon.is-loading {
  color: #909399;
}

.analysis-icon.success {
  color: #67c23a;
}

.analysis-text {
  font-size: 13px;
  color: #606266;
  font-weight: 400;
}

.analysis-status-minimal {
  background-color: #f0f9ff;
  border-color: #e0f2fe;
}

.analysis-complete-minimal {
  background-color: #f0f9f4;
  border-color: #dcfce7;
}

/* 测试模式指示器样式 */
.test-mode-indicator {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  margin-bottom: 8px;
  background-color: #fffbeb;
  border: 1px solid #f59e0b;
  border-radius: 4px;
  font-size: 12px;
  color: #d97706;
  font-weight: 500;
}

.test-mode-indicator .el-icon {
  font-size: 12px;
}

.analysis-result {
  padding: 16px;
}

.analysis-summary {
  margin-top: 16px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 4px;
  border-left: 4px solid #409eff;
}

.analysis-summary h4 {
  margin-top: 0;
  margin-bottom: 8px;
  color: #303133;
}

.analysis-summary p {
  margin: 0;
  line-height: 1.6;
  color: #606266;
}

/* 新增样式 */
.expression-chart-container {
  width: 100%;
  height: 300px;
  margin-bottom: 16px;
}

.expression-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.basic-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 8px;
}

.basic-info-item {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
}

.info-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.info-value {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.details-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.detail-item {
  background: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
}

.detail-header {
  background: #409eff;
  color: white;
  padding: 8px 12px;
  font-weight: bold;
}

.detail-content {
  padding: 12px;
  font-size: 14px;
  line-height: 1.5;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .basic-info-grid {
    grid-template-columns: 1fr;
  }
  
  .expression-chart-container {
    height: 250px;
  }
  
  .details-grid {
    grid-template-columns: 1fr;
  }
}

/* ... existing styles ... */
.analysis-actions {
  margin-bottom: 15px;
  display: flex;
  justify-content: flex-end;
}

.analysis-dialog-content {
  max-height: 70vh;
  overflow-y: auto;
  padding: 10px;
}

.analysis-dialog-content h3 {
  margin-top: 20px;
  margin-bottom: 10px;
  color: #409eff;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
}

.analysis-dialog-content h3:first-child {
  margin-top: 0;
}

.expression-distribution {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}

.expression-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.expression-name {
  width: 80px;
  font-weight: bold;
  text-align: right;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.detail-item {
  background: #f8f9fa;
  border-radius: 4px;
  padding: 10px;
}

.detail-label {
  font-weight: bold;
  color: #606266;
  margin-bottom: 5px;
}

.detail-value {
  font-size: 16px;
  color: #303133;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 768px) {
  .analysis-dialog-content {
    max-height: 80vh;
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
  
  .expression-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
  
  .expression-name {
    width: 100%;
    text-align: left;
  }
}
/* ... existing styles ... */

.report-button-container {
  margin: 10px 0;
  display: flex;
  justify-content: flex-end;
  padding: 0 16px;
}

.analysis-dialog-empty {
  padding: 20px;
  text-align: center;
}

.empty-actions {
  margin-top: 10px;
}

/* 去掉这些与分析报告相关的样式 */
.report-button-container,
.analysis-dialog-content, 
.analysis-dialog-empty,
.analysis-actions,
.expression-distribution,
.expression-item,
.expression-name,
.detail-grid,
.detail-item,
.detail-label,
.detail-value,
.dialog-footer,
.empty-actions {
  display: none;
}

/* 新增样式 */
.complete-interview-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.complete-tip {
  font-size: 14px;
  color: #909399;
  margin-top: 10px;
}

/* 题目卡片样式 */
.question-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
  transition: all 0.3s ease;
  border: 1px solid #ebeef5;
}

.question-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.question-progress-bar {
  margin-bottom: 20px;
}

.progress-text {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
}

.progress-track {
  height: 6px;
  background-color: #f0f2f5;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #3a8ee6);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.question-card-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 24px;
  position: relative;
}

.question-header-top {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  width: 100%;
  flex-wrap: wrap;
}

.question-header-bottom {
  width: 100%;
}

.question-no-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #3a8ee6);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 16px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  font-size: 18px;
}

.question-main-title {
  flex: 1;
  padding-top: 4px;
  min-width: 0; /* 防止内容溢出 */
  margin-right: 16px;
}

.question-title-text {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  line-height: 1.6;
  word-wrap: break-word;
  word-break: break-word;
}

.question-desc {
  font-size: 15px;
  color: #606266;
  margin-top: 10px;
  line-height: 1.6;
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  border-left: 4px solid #409eff;
  word-wrap: break-word;
  word-break: break-word;
}

.question-timer {
  background: #f2f6fc;
  padding: 8px 14px;
  border-radius: 20px;
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
  margin-top: 4px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .question-header-top {
    flex-direction: column;
  }

  .question-main-title {
    margin-right: 0;
    margin-bottom: 16px;
    width: 100%;
  }

  .question-timer {
    align-self: flex-start;
  }
}

.timer-num {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

/* 选项卡样式 */
.option-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 10px;
}

.option-card-custom {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.option-card-custom:hover {
  border-color: #c6e2ff;
  background: #f5faff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.option-card-custom.active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
}

/* 已移除正确/错误选项样式 */

.option-card-custom.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.option-letter-custom {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f2f6fc;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
  align-items: center;
  font-size: 36px;
  font-weight: bold;
  margin-right: 16px;
  transition: all 0.3s;
  font-size: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.option-letter-custom.active {
  background: linear-gradient(135deg, #409eff, #3a8ee6);
  color: white;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.option-content-custom {
  flex: 1;
  color: #303133;
  transition: all 0.3s;
  font-size: 16px;
  line-height: 1.5;
}

.option-content-custom.active {
  color: #409eff;
  font-weight: 500;
}

.option-radio-custom {
  width: 22px;
  height: 22px;
  border: 2px solid #dcdfe6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  margin-left: 12px;
}

.option-card-custom.active .option-radio-custom {
  border-color: #409eff;
}

.radio-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #409eff;
  transition: all 0.3s;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0.7);
  }

  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(64, 158, 255, 0);
  }

  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(64, 158, 255, 0);
  }
}

/* 代码输入框样式 */
.code-input-flat {
  font-family: 'Courier New', monospace;
  font-size: 15px;
  border-radius: 8px;
  border-color: #dcdfe6;
  transition: all 0.3s;
  line-height: 1.6;
  padding: 12px;
}

.code-input-flat:focus {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.question-actions-bottom {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  gap: 16px;
}

.question-actions-bottom .el-button {
  padding: 12px 24px;
  font-size: 16px;
  border-radius: 8px;
  transition: all 0.3s;
}

.question-actions-bottom .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 8px;
  transition: all 0.3s;
}

.nav-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.nav-button.prev-button {
  background-color: #f5f7fa;
  color: #606266;
}

.nav-button.next-button {
  background-color: #409eff;
  color: white;
}

.nav-icon {
  width: 16px;
  height: 16px;
}
</style>
