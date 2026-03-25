<template>
  <div class="interview-analysis-report">
    <!-- 返回按钮 -->
    <el-button class="back-btn" type="info" plain @click="goBack" :icon="ArrowLeft">
      返回
    </el-button>
    <!-- 顶部统计区 -->
    <div class="report-header">
      <div class="header-main">
        <!-- 左侧标题和信息区 -->
        <div class="header-left">
          <div class="title-section">
            <h1 class="main-title">面试分析报告</h1>
            <p class="sub-title">Interview Analysis Report</p>
          </div>
          <div class="info-row">
            <div class="info-item">
              <div class="info-icon-wrapper">
                <el-icon class="info-icon"><OfficeBuilding /></el-icon>
              </div>
              <div class="info-content">
                <span class="info-label">公司</span>
                <span class="info-value">{{ reportData.companyName }}</span>
              </div>
            </div>
            <div class="info-divider"></div>
            <div class="info-item">
              <div class="info-icon-wrapper">
                <el-icon class="info-icon"><Briefcase /></el-icon>
              </div>
              <div class="info-content">
                <span class="info-label">职位</span>
                <span class="info-value">{{ reportData.positionName }}</span>
              </div>
            </div>
            <div class="info-divider"></div>
            <div class="info-item">
              <div class="info-icon-wrapper">
                <el-icon class="info-icon"><Timer /></el-icon>
              </div>
              <div class="info-content">
                <span class="info-label">面试时间</span>
                <span class="info-value">{{ formatDateTime(reportData.interviewTime) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧评分和操作区 -->
        <div class="header-right">
          <div class="score-section">
            <div class="score-label">综合评分</div>
            <div class="score-display">
              <span class="current-score">{{ reportData.totalScore }}</span>
              <span class="total-score">/100</span>
            </div>
            <div class="score-desc">Overall Score</div>
          </div>
          <el-button
              type="primary"
              @click="exportReport"
              :icon="Download"
              class="export-btn"
              size="large"
          >
            导出报告
          </el-button>
        </div>
      </div>
    </div>

    <!-- 核心图表区 -->
    <div class="report-content">
      <!-- 第一行卡片 -->
      <div class="chart-row">
        <el-card class="chart-card medium-chart expression-chart expression-wide">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="card-icon"><UserFilled /></el-icon>
                <span>表情分析</span>
              </div>
            </div>
          </template>
          <div class="chart-container" ref="expressionChartRef"></div>
        </el-card>

        <el-card class="chart-card medium-chart star-chart star-narrow">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="card-icon"><PieChart /></el-icon>
                <span>STAR结构完整度</span>
                <el-tooltip content="情境(Situation)、任务(Task)、行动(Action)、结果(Result)">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
            </div>
          </template>
          <div class="chart-container" ref="starChartRef"></div>
        </el-card>
      </div>

      <!-- 第二行卡片 -->
      <div class="chart-row">
        <el-card class="chart-card medium-chart answer-quality-chart">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="card-icon"><DocumentChecked /></el-icon>
                <span>问题回答质量分析</span>
              </div>
            </div>
          </template>
          <div class="chart-container" ref="answerQualityChartRef"></div>
        </el-card>
        <el-card class="chart-card medium-chart tech-analysis-chart">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="card-icon"><DocumentChecked /></el-icon>
                <span>技术能力分析</span>
              </div>
            </div>
          </template>
          <div class="tech-analysis-container">
            <!-- 综合得分标题 -->
            <div class="overall-score-header">
              <h3 class="score-title">
                <el-icon class="score-icon"><Trophy /></el-icon>
                综合技术得分：<span class="score-highlight">{{ techAnalysisData.overallScore }}分</span>
              </h3>
            </div>

            <!-- 分项得分分析 -->
            <div class="score-breakdown">
              <div class="score-item" v-if="techAnalysisData.basicQaAnalysis">
                <div class="score-item-header">
                  <el-icon class="item-icon"><DocumentChecked /></el-icon>
                  <span class="item-title">基础知识掌握</span>
                  <span class="item-score">{{ getBasicQaScore() }}分</span>
                </div>
                <div class="score-item-description">
                  {{ getBasicQaAnalysis() }}
                </div>
              </div>

              <div class="score-item" v-if="techAnalysisData.codeAnalysis">
                <div class="score-item-header">
                  <el-icon class="item-icon"><Monitor /></el-icon>
                  <span class="item-title">编程实战能力</span>
                  <span class="item-score">{{ getCodeScore() }}分</span>
                </div>
                <div class="score-item-description">
                  {{ getCodeAnalysisDescription() }}
                </div>
              </div>

              <div class="score-item" v-if="!techAnalysisData.basicQaAnalysis && !techAnalysisData.codeAnalysis">
                <div class="evaluation-pending">
                  <el-icon class="pending-icon"><Loading /></el-icon>
                  <span class="pending-text">技术评估正在进行中...</span>
                </div>
              </div>
            </div>

            <!-- AI 总体评估 -->
            <div class="ai-summary-section" v-if="techAnalysisData.summary">
              <div class="summary-header">
                <el-icon class="summary-icon"><MagicStick /></el-icon>
                <span class="summary-title">答题情况</span>
              </div>
              <div class="summary-content">
                <p class="summary-text">{{ formatTechSummary(techAnalysisData.summary) }}</p>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 第三行卡片 -->
      <div class="chart-row">
        <div class="chart-card medium-chart introduction-chart">
          <IntroductionAnalysisChart :sessionId="sessionId" />
        </div>
        <el-card class="chart-card medium-chart ability-chart">
          <template #header>
            <div class="card-header">
              <div class="header-title">
              <el-icon class="card-icon"><PieChart /></el-icon>
              <span>能力评估矩阵</span>
              </div>
              <div class="header-actions">
                <el-button 
                  v-if="!isAbilityMatrixGenerated && !isGeneratingAbilityMatrix"
                  type="primary" 
                  size="small" 
                  @click="handleGenerateAbilityMatrix"
                  :icon="MagicStick"
                >
                  AI分析
                </el-button>
                <el-button 
                  v-if="isGeneratingAbilityMatrix"
                  type="primary" 
                  size="small" 
                  loading
                  disabled
                >
                  分析中...
                </el-button>
                <el-button 
                  v-if="isAbilityMatrixGenerated && !isGeneratingAbilityMatrix"
                  type="success" 
                  size="small"
                  @click="handleGenerateAbilityMatrix"
                  :icon="MagicStick"
                >
                  再次分析
                </el-button>
              </div>
            </div>
          </template>
          <div class="chart-container" ref="abilityChartRef"></div>
        </el-card>
      </div>

      <!-- 第四行：AI综合分析简报 -->
      <div class="chart-row full-width-row">
        <!-- AI综合分析简报 -->
        <el-card class="analysis-card full-width-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
              <el-icon class="card-icon"><MagicStick /></el-icon>
              <span>AI综合分析简报</span>
              </div>
              <div class="header-actions">
                <el-button 
                  v-if="!isComprehensiveAnalysisGenerated && !isGeneratingComprehensiveAnalysis"
                  type="primary" 
                  size="small" 
                  @click="handleGenerateComprehensiveAnalysis"
                  :icon="MagicStick"
                >
                  AI分析
                </el-button>
                <el-button 
                  v-if="isGeneratingComprehensiveAnalysis"
                  type="primary" 
                  size="small" 
                  loading
                  disabled
                >
                  分析中...
                </el-button>
                                    <el-button 
                      v-if="isComprehensiveAnalysisGenerated && !isGeneratingComprehensiveAnalysis"
                      type="success" 
                      size="small"
                      @click="handleGenerateComprehensiveAnalysis"
                      :icon="MagicStick"
                    >
                      再次分析
                    </el-button>
              </div>
            </div>
          </template>
          <div class="analysis-content">
            <div v-if="!isComprehensiveAnalysisGenerated && !isGeneratingComprehensiveAnalysis" class="analysis-placeholder">
              <div class="placeholder-content">
                <el-icon class="placeholder-icon"><MagicStick /></el-icon>
                <p class="placeholder-text">点击"AI分析"按钮开始生成综合分析简报</p>
                <p class="placeholder-desc">系统将基于您的面试表现生成详细的综合评估报告</p>
              </div>
            </div>
            
            <div v-else-if="isGeneratingComprehensiveAnalysis" class="analysis-loading">
              <div class="loading-content">
                <el-icon class="loading-icon"><Loading /></el-icon>
                <p class="loading-text">AI正在分析您的面试表现...</p>
                <p class="loading-desc">请稍候，这可能需要30-60秒</p>
              </div>
            </div>
            
            <div v-else-if="isComprehensiveAnalysisGenerated" class="analysis-sections">
            <div class="analysis-section">
              <h3><el-icon class="section-icon"><Trophy /></el-icon>综合表现评估</h3>
              <div class="analysis-summary">
                  <p class="summary-text" v-if="reportData.comprehensiveAnalysis?.summary">
                    {{ reportData.comprehensiveAnalysis.summary }}
                  </p>
                  <div v-else class="no-data">
                    <p>暂无综合分析数据</p>
                  </div>
                  <div class="key-metrics" v-if="reportData.comprehensiveAnalysis?.metrics">
                    <div 
                      v-for="(metric, index) in reportData.comprehensiveAnalysis.metrics" 
                      :key="'metric-'+index"
                      class="metric-item"
                    >
                      <span class="metric-label">{{ metric.label }}</span>
                      <span class="metric-value">{{ metric.value }}分</span>
                  </div>
                  </div>
                  <div v-else class="no-data">
                    <p>暂无评分数据</p>
                </div>
              </div>
            </div>
            <div class="analysis-section">
              <h3><el-icon class="section-icon"><DocumentChecked /></el-icon>面试亮点</h3>
                <ul class="highlight-list" v-if="reportData.advantages && reportData.advantages.length > 0">
                <li v-for="(advantage, index) in reportData.advantages" :key="'adv-'+index">
                  <el-icon class="highlight-icon"><SuccessFilled /></el-icon>
                  <span>{{ advantage }}</span>
                </li>
              </ul>
                <div v-else class="no-data">
                  <p>暂无亮点数据</p>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 第五行：个性化建议 -->
      <div class="chart-row full-width-row">
        <!-- 个性化建议 -->
        <el-card class="suggestion-card full-width-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
              <el-icon class="card-icon"><Tools /></el-icon>
              <span>个性化建议</span>
              </div>
              <div class="header-actions">
                <el-button 
                  v-if="!isPersonalizedSuggestionsGenerated && !isGeneratingPersonalizedSuggestions"
                  type="primary" 
                  size="small" 
                  @click="handleGeneratePersonalizedSuggestions"
                  :icon="MagicStick"
                >
                  AI分析
                </el-button>
                <el-button 
                  v-if="isGeneratingPersonalizedSuggestions"
                  type="primary" 
                  size="small" 
                  loading
                  disabled
                >
                  分析中...
                </el-button>
                <el-button 
                  v-if="isPersonalizedSuggestionsGenerated && !isGeneratingPersonalizedSuggestions"
                  type="success" 
                  size="small"
                  @click="handleGeneratePersonalizedSuggestions"
                  :icon="MagicStick"
                >
                  再次分析
                </el-button>
              </div>
            </div>
          </template>
          <div class="suggestion-content">
            <!-- AI分析生成的个性化建议 -->
            <div v-if="isPersonalizedSuggestionsGenerated && personalizedSuggestionsData" class="ai-suggestions">
              
              <!-- 技能改进建议 -->
              <div v-if="personalizedSuggestionsData.skillImprovements" class="suggestion-section">
                <h3><el-icon class="section-icon"><MagicStick /></el-icon>技能提升建议</h3>
                <div class="skill-improvements">
                  <div 
                    v-for="(skill, index) in personalizedSuggestionsData.skillImprovements" 
                    :key="'skill-' + index"
                    class="skill-item"
                  >
                    <h4 class="skill-category">{{ skill.category }}</h4>
                    <div class="skill-content">
                      <div class="skill-level">
                        <strong>当前水平：</strong>{{ skill.currentLevel }}
                      </div>
                      <div class="skill-target">
                        <strong>目标水平：</strong>{{ skill.targetLevel }}
                      </div>
                      <div class="skill-suggestions">
                        <strong>具体建议：</strong>
                        <ul>
                          <li v-for="(suggestion, sIndex) in skill.suggestions" :key="'suggestion-' + sIndex">
                            {{ suggestion }}
                </li>
              </ul>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 沟通表达改进 -->
              <div v-if="personalizedSuggestionsData.communicationImprovements" class="suggestion-section">
                <h3><el-icon class="section-icon"><ChatDotRound /></el-icon>沟通表达改进</h3>
                <div class="communication-improvements">
                  <div v-if="personalizedSuggestionsData.communicationImprovements.strengths" class="comm-strengths">
                    <h4>✅ 优势</h4>
                    <ul>
                      <li v-for="(strength, index) in personalizedSuggestionsData.communicationImprovements.strengths" :key="'strength-' + index">
                        {{ strength }}
                      </li>
                    </ul>
                  </div>
                  <div v-if="personalizedSuggestionsData.communicationImprovements.weaknesses" class="comm-weaknesses">
                    <h4>⚠️ 待改进点</h4>
                    <ul>
                      <li v-for="(weakness, index) in personalizedSuggestionsData.communicationImprovements.weaknesses" :key="'weakness-' + index">
                        {{ weakness }}
                      </li>
                    </ul>
                  </div>
                  <div v-if="personalizedSuggestionsData.communicationImprovements.actionPlan" class="comm-actions">
                    <h4>🎯 行动计划</h4>
                    <ul>
                      <li v-for="(action, index) in personalizedSuggestionsData.communicationImprovements.actionPlan" :key="'action-' + index">
                        {{ action }}
                      </li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- 学习路径 -->
              <div v-if="personalizedSuggestionsData.learningPath" class="suggestion-section">
                <h3><el-icon class="section-icon"><Reading /></el-icon>学习路径规划</h3>
                <div class="learning-path">
                  <div 
                    v-for="(phase, phaseKey) in personalizedSuggestionsData.learningPath" 
                    :key="phaseKey"
                    class="phase-item"
                  >
                    <h4 class="phase-title">
                      阶段{{ phaseKey.replace('phase', '') }}：{{ phase.focus }}
                    </h4>
                    <div class="phase-content">
                      <div class="phase-duration">
                        <strong>时间安排：</strong>{{ phase.duration }}
                      </div>
                      <div class="phase-goals">
                        <strong>主要目标：</strong>
                        <ul>
                          <li v-for="(goal, index) in phase.goals" :key="'goal-' + index">
                            {{ goal }}
                          </li>
                        </ul>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 实践项目建议 -->
              <div v-if="personalizedSuggestionsData.practiceProjects" class="suggestion-section">
                <h3><el-icon class="section-icon"><FolderOpened /></el-icon>实践项目推荐</h3>
                <div class="practice-projects">
                  <div 
                    v-for="(project, index) in personalizedSuggestionsData.practiceProjects" 
                    :key="'project-' + index"
                    class="project-item"
                  >
                    <h4 class="project-name">{{ project.name }}</h4>
                    <div class="project-content">
                      <div class="project-description">{{ project.description }}</div>
                      <div class="project-meta">
                        <span class="project-difficulty">难度：{{ project.difficulty }}</span>
                        <span class="project-duration">时长：{{ project.duration }}</span>
                      </div>
                      <div class="project-skills">
                        <strong>技能要点：</strong>
                        <el-tag 
                          v-for="skill in project.skills" 
                          :key="skill" 
                          size="small" 
                          type="info"
                          class="skill-tag"
                        >
                          {{ skill }}
                        </el-tag>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 面试技巧 -->
              <div v-if="personalizedSuggestionsData.interviewTips" class="suggestion-section">
                <h3><el-icon class="section-icon"><Microphone /></el-icon>面试技巧提升</h3>
                <div class="interview-tips">
                  <div v-if="personalizedSuggestionsData.interviewTips.preparation" class="tips-category">
                    <h4>📋 面试准备</h4>
                    <ul>
                      <li v-for="(tip, index) in personalizedSuggestionsData.interviewTips.preparation" :key="'prep-' + index">
                        {{ tip }}
                      </li>
                    </ul>
                  </div>
                  <div v-if="personalizedSuggestionsData.interviewTips.presentation" class="tips-category">
                    <h4>🎤 表达技巧</h4>
                    <ul>
                      <li v-for="(tip, index) in personalizedSuggestionsData.interviewTips.presentation" :key="'pres-' + index">
                        {{ tip }}
                      </li>
                    </ul>
                  </div>
                  <div v-if="personalizedSuggestionsData.interviewTips.technical" class="tips-category">
                    <h4>💻 技术回答</h4>
                    <ul>
                      <li v-for="(tip, index) in personalizedSuggestionsData.interviewTips.technical" :key="'tech-' + index">
                        {{ tip }}
                      </li>
                    </ul>
                  </div>
                </div>
              </div>

              <!-- 职业发展建议 -->
              <div v-if="personalizedSuggestionsData.careerAdvice" class="suggestion-section">
                <h3><el-icon class="section-icon"><TrendCharts /></el-icon>职业发展建议</h3>
                <div class="career-advice">
                  <div v-if="personalizedSuggestionsData.careerAdvice.currentPosition" class="career-current">
                    <h4>📍 当前定位</h4>
                    <p>{{ personalizedSuggestionsData.careerAdvice.currentPosition }}</p>
                  </div>
                  <div v-if="personalizedSuggestionsData.careerAdvice.nextSteps" class="career-next">
                    <h4>🎯 下一步行动</h4>
                    <ul>
                      <li v-for="(step, index) in personalizedSuggestionsData.careerAdvice.nextSteps" :key="'step-' + index">
                        {{ step }}
                      </li>
                    </ul>
                  </div>
                  <div v-if="personalizedSuggestionsData.careerAdvice.longTermGoal" class="career-goal">
                    <h4>🚀 长期目标</h4>
                    <p>{{ personalizedSuggestionsData.careerAdvice.longTermGoal }}</p>
                  </div>
                  <div v-if="personalizedSuggestionsData.careerAdvice.industryTrends" class="career-trends">
                    <h4>📈 行业趋势关注</h4>
                    <ul>
                      <li v-for="(trend, index) in personalizedSuggestionsData.careerAdvice.industryTrends" :key="'trend-' + index">
                        {{ trend }}
                      </li>
                    </ul>
                  </div>
                </div>
              </div>

            </div>
            
            <!-- 分析中状态 -->
            <div v-if="isGeneratingPersonalizedSuggestions" class="analysis-loading">
              <el-icon class="loading-icon"><Loading /></el-icon>
              <p class="loading-title">AI正在分析面试表现</p>
              <p class="loading-subtitle">基于您的整体表现生成个性化建议，预计需要30-60秒...</p>
            </div>
            
            <!-- 未分析状态 -->
            <div v-if="!isPersonalizedSuggestionsGenerated && !isGeneratingPersonalizedSuggestions" class="analysis-placeholder">
              <el-icon class="placeholder-icon"><MagicStick /></el-icon>
              <div class="placeholder-content">
                <p class="placeholder-title">获取AI个性化建议</p>
                <p class="placeholder-description">
                  基于您的整体面试表现，AI将为您生成：
                </p>
                <ul class="feature-list">
                  <li><el-icon><Star /></el-icon>技能改进方向分析</li>
                  <li><el-icon><Star /></el-icon>面试技巧优化建议</li>
                  <li><el-icon><Star /></el-icon>职业发展路径指导</li>
                  <li><el-icon><Star /></el-icon>针对性学习计划</li>
                </ul>
                <p class="placeholder-action">点击上方"AI分析"按钮开始生成</p>
              </div>
            </div>
          </div>
        </el-card>
            </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import {
  Download,
  Timer,
  QuestionFilled,
  SuccessFilled,
  InfoFilled,
  ArrowLeft,
  Histogram,
  OfficeBuilding,
  Briefcase,
  StarFilled,
  Star,
  UserFilled,
  Filter,
  DocumentChecked,
  PieChart,
  MagicStick,
  Trophy,
  Tools,
  Loading,
  ChatDotRound,
  Reading,
  FolderOpened,
  Microphone,
  TrendCharts,
  Monitor
} from '@element-plus/icons-vue'
import { getStarScores, getAnswerQuality, getReportBySessionId, getTechAnalysis, getAbilityMatrix, generateAbilityMatrix, checkAbilityMatrixStatus, getComprehensiveAnalysis, generateComprehensiveAnalysis, checkComprehensiveAnalysisStatus, generatePersonalizedSuggestions, getPersonalizedSuggestions, checkPersonalizedSuggestionsStatus } from '@/api/comprehensiveReport'
import { getExpressionSummary } from '@/api/faceExpression'
import IntroductionAnalysisChart from '@/components/IntroductionAnalysisChart.vue'

const route = useRoute()
const router = useRouter()

// 安全获取token的函数
const getAuthToken = () => {
  try {
    return localStorage?.getItem('token') || ''
  } catch (error) {
    console.error('无法访问localStorage获取token:', error)
    return ''
  }
}

const goBack = () => {
  router.back()
}

// 面试会话ID（从路由获取）
const sessionId = ref(null)

// 表情分析数据
const expressionData = ref([0, 0, 0, 0]) // [惊讶, 紧张, 思考, 微笑]

// 技术能力分析数据
const techAnalysisData = ref({
  overallScore: 0,
  summary: '',
  basicQaAnalysis: null,
  codeAnalysis: null
})

// 能力评估矩阵状态
const isAbilityMatrixGenerated = ref(false)
const isGeneratingAbilityMatrix = ref(false)

// 综合分析简报状态
const isComprehensiveAnalysisGenerated = ref(false)
const isGeneratingComprehensiveAnalysis = ref(false)

// 个性化建议状态
const isPersonalizedSuggestionsGenerated = ref(false)
const isGeneratingPersonalizedSuggestions = ref(false)
const personalizedSuggestions = ref('')
const personalizedSuggestionsData = ref(null)

// 获取会话ID的方法
const getSessionId = () => {
  const localStorageId = (() => {
    try {
      return localStorage?.getItem('interviewSessionId')
    } catch (error) {
      console.error('无法访问localStorage:', error)
      return null
    }
  })()
  
  const id = route.params.id || route.query.sessionId || localStorageId
  
  if (id) {
    sessionId.value = Number(id)
  } else {
    console.warn('未找到面试会话ID')
  }
  
  return sessionId.value
}

// 获取技术能力分析数据
const fetchTechAnalysis = async () => {
  try {
    const currentSessionId = sessionId.value || getSessionId()
    if (!currentSessionId) {
      console.warn('未找到面试会话ID')
      return
    }

    const response = await getTechAnalysis(currentSessionId)
    console.log('技术分析API响应:', response)
    
    if (response && response.code === 0 && response.data) {
      techAnalysisData.value = {
        overallScore: response.data.overallScore || 0,
        summary: response.data.summary || '暂无分析数据',
        basicQaAnalysis: response.data.basicQaAnalysis,
        codeAnalysis: response.data.codeAnalysis
      }
    } else {
      console.warn('技术分析数据格式不正确或获取失败:', response)
      techAnalysisData.value = {
        overallScore: 0,
        summary: '暂无分析数据',
        basicQaAnalysis: null,
        codeAnalysis: null
      }
    }
  } catch (error) {
    console.error('获取技术分析数据失败:', error)
    techAnalysisData.value = {
      overallScore: 0,
      summary: '获取分析数据失败',
      basicQaAnalysis: null,
      codeAnalysis: null
    }
  }
}

// 获取表情分析数据
const fetchExpressionData = async () => {
  try {
    const currentSessionId = sessionId.value || getSessionId()
    if (!currentSessionId) {
      console.warn('未找到面试会话ID')
      return
    }

    const response = await getExpressionSummary(currentSessionId)
    console.log('表情分析API响应:', response)
    
    if (response && response.success && response.expressionDistribution) {
      const distribution = response.expressionDistribution
      
      // 映射后端数据到前端显示的四种表情类型
      expressionData.value = [
        distribution['惊讶'] || 0,    // 惊讶 (后端: surprised_count)
        distribution['悲伤'] || 0,    // 紧张 (后端: sad_count，前端显示为紧张)
        distribution['正常'] || 0,    // 思考 (后端: neutral_count，前端显示为思考)
        distribution['高兴'] || 0     // 微笑 (后端: happy_count，前端显示为微笑)
      ]
      
      // 重新初始化图表
      await nextTick()
      initExpressionChart()
    } else {
      console.warn('表情分析数据格式不正确或获取失败:', response)
      // 如果数据格式不正确，使用默认数据
      expressionData.value = [0, 0, 0, 0]
    }
  } catch (error) {
    console.error('获取表情分析数据失败:', error)
    // 如果获取失败，使用默认数据
    expressionData.value = [0, 0, 0, 0]
  }
}

// 图表引用
const expressionChartRef = ref(null)
const starChartRef = ref(null)
const answerQualityChartRef = ref(null)
const abilityChartRef = ref(null)

// 报告数据
const reportData = ref({
  id: 1,
  companyName: '科技创新公司',
  positionName: '高级前端开发工程师',
  interviewTime: '2024-07-15T14:30:00',
  duration: 65, // 分钟
  totalScore: 86,
  starRating: 4.3,
  expressionData: {
    timeAxis: ['14:30', '14:35', '14:40', '14:45', '14:50', '14:55', '15:00', '15:05', '15:10', '15:15', '15:20', '15:25', '15:30', '15:35'],
    neutral: [75, 80, 82, 78, 75, 70, 65, 68, 72, 75, 78, 80, 75, 72],
    happy: [15, 10, 8, 12, 15, 18, 22, 20, 18, 15, 12, 10, 15, 18],
    thinking: [8, 8, 8, 8, 8, 10, 10, 10, 8, 8, 8, 8, 8, 8],
    nervous: [2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 2]
  },
  starAnalysis: {
    situation: 92,
    task: 85,
    action: 76,
    result: 68
  },
  answerQuality: [
    {name: '技术问题', score: 88},
    {name: '项目经验', score: 82},
    {name: '团队协作', score: 75},
    {name: '沟通表达', score: 85},
    {name: '解决方案', score: 78}
  ],
  abilityMatrix: [], // 空数组，等待AI分析生成数据
  advantages: [], // 空数组，等待AI分析生成数据
  improvements: [], // 空数组，等待AI分析生成数据
  comprehensiveAnalysis: null // 综合分析简报数据
})

// 初始化各个图表
const initCharts = () => {
  nextTick(() => {
    initExpressionChart()
    initStarChart()
    initAnswerQualityChart()
    initAbilityChart()
  })
}

// 表情分析横向柱状图
const initExpressionChart = () => {
  const chartDom = expressionChartRef.value
  if (!chartDom) return

  // 检查是否已有图表实例，如果有则销毁
  const existingChart = echarts.getInstanceByDom(chartDom)
  if (existingChart) {
    existingChart.dispose()
  }

  const chart = echarts.init(chartDom)
  
  // 表情分类和对应的次数数据
  const expressionCategories = ['惊讶', '紧张', '思考', '微笑']
  const expressionCounts = expressionData.value // 使用从API获取的真实数据

  // 定义颜色映射（深色柔和主题色调）
  const colorMap = {
    '惊讶': '#C4A55A',  // 深黄色
    '紧张': '#5F8A4A',  // 深绿色
    '思考': '#5B82B3',  // 深蓝色
    '微笑': '#D1849C'   // 深粉色
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'rgba(102, 126, 234, 0.2)',
      borderWidth: 1,
      borderRadius: 12,
      textStyle: {
        color: '#2d3748',
        fontSize: 13,
        fontWeight: 500
      },
      padding: [16, 20],
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(102, 126, 234, 0.1)',
          width: 'auto'
        }
      },
      formatter: function(params) {
        const param = params[0]
        const color = colorMap[param.name]
        const dot = `<span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%; background: ${color}; margin-right: 8px;"></span>`
        return `<div style="font-weight: 600; color: #1a202c;">${dot}${param.name}: <span style="font-weight: 600;">${param.value}次</span></div>`
      },
      extraCssText: 'box-shadow: 0 8px 32px rgba(102, 126, 234, 0.15); backdrop-filter: blur(10px);'
    },
    grid: {
      left: '8%',
      right: '8%',
      bottom: '5%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      min: 0,
      max: 6,
      interval: 1,
      axisLine: {
        lineStyle: {
          color: 'rgba(102, 126, 234, 0.15)',
          width: 2
        }
      },
      axisLabel: {
        color: '#718096',
        fontSize: 14,
        fontWeight: 500,
        formatter: '{value}次',
        margin: 12
      },
      axisTick: {
        show: true,
        lineStyle: {
          color: 'rgba(102, 126, 234, 0.2)',
          width: 1
        },
        length: 6
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(102, 126, 234, 0.06)',
          width: 1,
          type: 'solid'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: expressionCategories,
      axisLine: {
        show: false
      },
      axisLabel: {
        color: '#4a5568',
        fontSize: 14,
        fontWeight: 600,
        margin: 12
      },
      axisTick: {
        show: false
      }
    },
    series: [
      {
        name: '表情次数',
        type: 'bar',
        barWidth: '60%',
        data: expressionCategories.map((category, index) => ({
          name: category,
          value: expressionCounts[index],
          itemStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 1, y2: 0,
              colorStops: [
                { offset: 0, color: colorMap[category] },
                { offset: 1, color: colorMap[category] + 'CC' } // 添加透明度
              ]
            },
            borderRadius: [0, 6, 6, 0],
            shadowColor: colorMap[category] + '40',
            shadowBlur: 8,
            shadowOffsetX: 2
          }
        })),
        emphasis: {
          itemStyle: {
            shadowBlur: 12,
            shadowOffsetX: 4,
            scale: 1.02
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c}次',
          color: '#4a5568',
          fontWeight: 600,
          fontSize: 13
        }
      }
    ],
    animationDuration: 1200,
    animationEasing: 'cubicOut',
    animationDelay: function (idx) {
      return idx * 150
    }
  }

  chart.setOption(option)

  // 添加鼠标悬停效果
  chart.on('mouseover', 'series', function(params) {
    chart.dispatchAction({
      type: 'highlight',
      dataIndex: params.dataIndex
    })
  })

  chart.on('mouseout', 'series', function(params) {
    chart.dispatchAction({
      type: 'downplay',
      dataIndex: params.dataIndex
    })
  })

  window.addEventListener('resize', () => chart.resize())
}

// STAR环形图
const initStarChart = () => {
  const chartDom = starChartRef.value
  if (!chartDom) return

  // 检查是否已有图表实例，如果有则销毁
  const existingChart = echarts.getInstanceByDom(chartDom)
  if (existingChart) {
    existingChart.dispose()
  }

  const chart = echarts.init(chartDom)
  const starData = reportData.value.starAnalysis

  const option = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.98)',
      borderColor: 'rgba(102, 126, 234, 0.2)',
      borderWidth: 1,
      borderRadius: 12,
      textStyle: {
        color: '#2d3748',
        fontSize: 13,
        fontWeight: 500
      },
      padding: [16, 20],
      formatter: function(params) {
        return `<div style="font-weight: 600; margin-bottom: 4px;">${params.name}</div><div style="color: #667eea; font-weight: 600;">${params.value}分</div>`
      },
      extraCssText: 'box-shadow: 0 8px 32px rgba(102, 126, 234, 0.15); backdrop-filter: blur(10px);'
    },
    legend: {
      show: false
    },
    series: [
      {
        name: 'STAR分析',
        type: 'pie',
        radius: ['35%', '65%'],
        center: ['50%', '55%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
          borderColor: '#ffffff',
          borderWidth: 3,
          shadowColor: 'rgba(0, 0, 0, 0.1)',
          shadowBlur: 6
        },
        label: {
          show: true,
          position: 'outside',
          formatter: function(params) {
            return `{name|${params.name}}\n{value|${params.value}分}`
          },
          rich: {
            name: {
              fontSize: 13,
              fontWeight: 600,
              color: '#4a5568',
              padding: [0, 0, 4, 0]
            },
            value: {
              fontSize: 14,
              fontWeight: 700,
              color: '#2d3748'
            }
          }
        },
        labelLine: {
          show: true,
          length: 15,
          length2: 8,
          lineStyle: {
            width: 2,
            color: '#e2e8f0'
          }
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 12,
            shadowOffsetX: 0,
            shadowOffsetY: 2,
            shadowColor: 'rgba(0, 0, 0, 0.2)',
            scale: 1.05
          },
          label: {
            fontSize: 16,
            fontWeight: 'bold'
          }
        },
        animationType: 'scale',
        animationEasing: 'elasticOut',
        animationDelay: function (idx) {
          return Math.random() * 200
        },
        data: [
          { value: starData.situation, name: '情境(S)', itemStyle: { color: '#6B9AC4' }},
          { value: starData.task, name: '任务(T)', itemStyle: { color: '#F2B366' }},
          { value: starData.action, name: '行动(A)', itemStyle: { color: '#D4A5A5' }},
          { value: starData.result, name: '结果(R)', itemStyle: { color: '#7AADAD' }}
        ]
      }
    ]
  }

  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

// 问题回答质量分析图
const initAnswerQualityChart = () => {
  const chartDom = answerQualityChartRef.value
  if (!chartDom) return

  // 检查是否已有图表实例，如果有则销毁
  const existingChart = echarts.getInstanceByDom(chartDom)
  if (existingChart) {
    existingChart.dispose()
  }

  const chart = echarts.init(chartDom)
  const data = reportData.value.answerQuality

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
        shadowStyle: {
          color: 'rgba(58,122,254,0.04)'
        }
      },
      formatter: function(params) {
        return `${params[0].name}: ${params[0].value}分`
      }
    },
    grid: {
      left: '5%',
      right: '8%',
      bottom: '5%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      max: 100,
      axisLabel: {
        formatter: '{value}分',
        color: '#7A869A'
      },
      splitLine: {
        lineStyle: {
          type: 'dashed',
          color: '#E5EAF3'
        }
      },
      axisLine: {
        lineStyle: {
          color: '#B0B8C9'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: data.map(item => item.name),
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        margin: 10,
        fontSize: 14,
        color: '#7A869A'
      }
    },
    series: [
      {
        name: '得分',
        type: 'bar',
        barWidth: '60%',
        data: data.map((item, index) => {
          // 找到最高分
          const maxScore = Math.max(...data.map(d => d.score))
          // 判断当前项是否为最高分
          const isMaxScore = item.score === maxScore

          // 定义优先级顺序
          const priorityOrder = ['解决方案', '沟通表达', '团队协作', '项目经验', '技术问题']

          // 找到所有最高分项
          const maxScoreItems = data.filter(d => d.score === maxScore)

          // 如果有多个最高分，按优先级顺序找到排在最前面的
          let highestPriorityItem = null
          if (maxScoreItems.length > 1) {
            for (const priorityName of priorityOrder) {
              const found = maxScoreItems.find(item => item.name === priorityName)
              if (found) {
                highestPriorityItem = found
                break
              }
            }
          } else {
            highestPriorityItem = maxScoreItems[0]
          }

          // 判断当前项是否是应该显示为淡粉色的项
          const isHighestPriority = isMaxScore && item.name === highestPriorityItem?.name

          // 设置颜色：最高优先级的最高分为淡粉色，其他为淡蓝色
          const color = isHighestPriority ?
              ['#E9B3A3', '#D4998A'] :  // 淡粉色渐变
              ['#B9D7E1', '#A4C4D1']    // 淡蓝色渐变

          return {
            value: item.score,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                { offset: 0, color: color[0] },
                { offset: 1, color: color[1] }
              ])
            },
            label: {
              show: true,
              position: 'right',
              formatter: `${item.score}分`,
              color: isHighestPriority ? '#D4998A' : '#A4C4D1',  // 粉色 : 蓝色
              fontWeight: 600
            }
          }
        })
      }
    ]
  }

  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

// 能力评估雷达图
const initAbilityChart = () => {
  const chartDom = abilityChartRef.value
  if (!chartDom) return

  // 检查是否已有图表实例，如果有则销毁
  const existingChart = echarts.getInstanceByDom(chartDom)
  if (existingChart) {
    existingChart.dispose()
  }

  const chart = echarts.init(chartDom)
  const abilityData = reportData.value.abilityMatrix

  // 检查是否有能力评估矩阵数据
  if (!abilityData || !Array.isArray(abilityData) || abilityData.length === 0) {
    // 没有数据时显示提示信息
    const emptyOption = {
      title: {
        text: '未进行分析',
        subtext: '可点击右上按钮进行分析',
        left: 'center',
        top: 'middle',
        textStyle: {
          fontSize: 18,
          color: '#909399',
          fontWeight: 'normal'
        },
        subtextStyle: {
          fontSize: 14,
          color: '#C0C4CC',
          fontWeight: 'normal'
        }
      },
      graphic: {
        type: 'image',
        style: {
          image: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTMyIDJMMzggMTRIMjZMMzIgMloiIGZpbGw9IiNEQ0RGRTYiLz4KPHN2ZyB3aWR0aD0iNjQiIGhlaWdodD0iNjQiIHZpZXdCb3g9IjAgMCA2NCA2NCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMjgiIHN0cm9rZT0iI0RDREZFNiIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtZGFzaGFycmF5PSI0IDQiLz4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMjAiIHN0cm9rZT0iI0RDREZFNiIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtZGFzaGFycmF5PSI0IDQiLz4KPGNpcmNsZSBjeD0iMzIiIGN5PSIzMiIgcj0iMTIiIHN0cm9rZT0iI0RDREZFNiIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtZGFzaGFycmF5PSI0IDQiLz4KPGxpbmUgeDE9IjMyIiB5MT0iNCIgeDI9IjMyIiB5Mj0iMTIiIHN0cm9rZT0iI0RDREZFNiIgc3Ryb2tlLXdpZHRoPSIyIi8+CjxsaW5lIHgxPSI1NSIgeTE9IjMyIiB4Mj0iNjAiIHkyPSIzMiIgc3Ryb2tlPSIjRENERkU2IiBzdHJva2Utd2lkdGg9IjIiLz4KPGxpbmUgeDE9IjMyIiB5MT0iNjAiIHgyPSIzMiIgeTI9IjUyIiBzdHJva2U9IiNEQ0RGRTYiIHN0cm9rZS13aWR0aD0iMiIvPgo8bGluZSB4MT0iNCIgeTE9IjMyIiB4Mj0iOSIgeTI9IjMyIiBzdHJva2U9IiNEQ0RGRTYiIHN0cm9rZS13aWR0aD0iMiIvPgo8L3N2Zz4K',
          width: 64,
          height: 64,
          x: chartDom.clientWidth / 2 - 32,
          y: chartDom.clientHeight / 2 - 80
        }
      }
    }
    chart.setOption(emptyOption)
    window.addEventListener('resize', () => chart.resize())
    return
  }

  // 有数据时显示正常的雷达图
  const option = {
    tooltip: {},
    radar: {
      indicator: abilityData.map(item => ({
        name: item.name,
        min: 0,
        max: 100
      })),
      shape: 'circle',
      radius: '70%',
      splitNumber: 5, // 从4改为5，避免alignTicks导致的刻度不可读问题
      scale: false, // 禁用自动缩放，确保使用设置的min/max值
      axisLine: {
        lineStyle: {
          color: '#6B9AC4'
        }
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(107,154,196,0.2)'
        }
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(107,154,196,0.06)', 'rgba(255,214,102,0.06)']
        }
      },
      name: {
        textStyle: {
          color: '#6B9AC4',
          fontSize: 14
        }
      }
    },
    series: [
      {
        name: '能力评估',
        type: 'radar',
        data: [
          {
            value: abilityData.map(item => item.value),
            name: '能力得分',
            areaStyle: {
              color: new echarts.graphic.RadialGradient(0.5, 0.5, 1, [
                {
                  color: 'rgba(107,154,196,0.35)',
                  offset: 0
                },
                {
                  color: 'rgba(255,214,102,0.18)',
                  offset: 1
                }
              ])
            },
            symbol: 'circle',
            symbolSize: 8,
            lineStyle: {
              width: 2,
              color: '#6B9AC4'
            }
          }
        ]
      }
    ]
  }

  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize())
}

// 导出报告
const exportReport = () => {
  console.log('报告导出功能正在开发中')
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '未知'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化技术分析摘要
const formatTechSummary = (summary) => {
  if (!summary || summary.trim() === '') {
    console.log('技术分析摘要为空，等待数据加载...')
    return '暂无分析数据'
  }
  
  // 尝试从 getCodeAnalysisDescription 获取简洁描述
  const codeDescription = getCodeAnalysisDescription()
  
  // 如果没有代码分析数据，尝试从原始摘要中提取符合格式的文字
  if (codeDescription === '暂无代码题完成记录' || codeDescription === '暂无编程能力测评数据') {
    // 直接匹配符合格式的文字：代码实操分析：共完成X道代码题，成功通过Y道，平均得分Z分。
    const formatMatch = summary.match(/代码实操分析：共完成(\d+)道代码题，成功通过(\d+)道，平均得分(\d+)分。/)
    if (formatMatch) {
      return formatMatch[0]  // 直接返回匹配到的完整文字
    }
    
    // 如果没有找到标准格式，返回通用描述
    return '代码实操分析：面试已完成代码编程测试，具体得分情况请参考详细报告。'
  }
  
  return codeDescription
}

// 获取基础问答得分
const getBasicQaScore = () => {
  if (techAnalysisData.value.basicQaAnalysis && techAnalysisData.value.basicQaAnalysis.technicalScore) {
    return techAnalysisData.value.basicQaAnalysis.technicalScore
  }
  return 0
}

// 获取代码能力得分
const getCodeScore = () => {
  if (techAnalysisData.value.codeAnalysis && techAnalysisData.value.codeAnalysis.averageScore) {
    return techAnalysisData.value.codeAnalysis.averageScore
  }
  return 0
}

// 获取基础知识分析描述
const getBasicQaAnalysis = () => {
  if (!techAnalysisData.value.basicQaAnalysis) {
    return '暂无基础知识测评数据'
  }
  
  const analysis = techAnalysisData.value.basicQaAnalysis
  const score = analysis.technicalScore || 0
  
  if (score >= 90) {
    return '基础知识掌握扎实，理论功底深厚，具备良好的专业素养'
  } else if (score >= 80) {
    return '基础知识掌握良好，理论基础较为牢固，有一定的专业深度'
  } else if (score >= 70) {
    return '基础知识掌握一般，建议加强理论学习，提升专业基础'
  } else if (score >= 60) {
    return '基础知识有所欠缺，需要系统性地补强理论基础'
  } else {
    return '基础知识掌握不足，建议重点加强基础理论学习'
  }
}

// 获取代码能力分析描述
const getCodeAnalysisDescription = () => {
  if (!techAnalysisData.value.codeAnalysis) {
    return '暂无编程能力测评数据'
  }
  
  const analysis = techAnalysisData.value.codeAnalysis
  const score = Math.round(analysis.averageScore || 0)
  const totalProblems = analysis.totalProblems || 0
  const solvedProblems = analysis.solvedProblems || 0
  
  if (totalProblems > 0) {
    return `代码实操分析：共完成${totalProblems}道代码题，成功通过${solvedProblems}道，平均得分${score}分。`
  }
  
  return '暂无代码题完成记录'
}

// 获取能力评估矩阵数据
const fetchAbilityMatrixData = async (autoGenerate = true) => {
  try {
    const currentSessionId = sessionId.value || getSessionId()
    if (!currentSessionId) {
      console.warn('未找到面试会话ID')
      return
    }

    console.log('获取能力评估矩阵数据，会话ID:', currentSessionId)

    // 先检查是否已生成
    const statusRes = await checkAbilityMatrixStatus(currentSessionId)
    console.log('能力矩阵状态检查结果:', statusRes)

    if (statusRes.code === 0 && statusRes.data) {
      isAbilityMatrixGenerated.value = statusRes.data.isGenerated
      
      // 如果未生成且不自动生成，直接返回
      if (!statusRes.data.isGenerated && !autoGenerate) {
        console.log('能力矩阵尚未生成，等待用户手动触发')
        return
      }
      
      // 如果未生成且允许自动生成，尝试生成
      if (!statusRes.data.isGenerated && autoGenerate) {
        console.log('能力矩阵尚未生成，尝试自动生成...')
        
        try {
          isGeneratingAbilityMatrix.value = true
          const generateRes = await generateAbilityMatrix(currentSessionId)
          console.log('能力矩阵生成结果:', generateRes)
          
          if (generateRes.code !== 0) {
            console.warn('能力矩阵生成失败:', generateRes.msg)
            console.error('能力评估矩阵生成失败，请稍后重试')
            return
          }
          
          isAbilityMatrixGenerated.value = true
          console.log('能力评估矩阵自动生成完成', { sessionId: currentSessionId, timestamp: new Date().toISOString() })
        } catch (generateError) {
          console.error('生成能力矩阵时出错:', generateError)
          console.error('生成能力矩阵失败')
          return
        } finally {
          isGeneratingAbilityMatrix.value = false
        }
      }
    }

    // 获取能力矩阵数据
    const matrixRes = await getAbilityMatrix(currentSessionId)
    console.log('能力矩阵数据获取结果:', matrixRes)
    
    if (matrixRes.code === 0 && matrixRes.data && matrixRes.data.ability_matrix_data) {
      const matrixData = matrixRes.data.ability_matrix_data
      
      if (matrixData.abilityMatrix && Array.isArray(matrixData.abilityMatrix)) {
        // 使用真实数据更新reportData
        reportData.value.abilityMatrix = matrixData.abilityMatrix
        console.log('✅ 能力评估矩阵数据更新成功:', reportData.value.abilityMatrix)
        
        // 重新初始化能力评估图表
        await nextTick()
        initAbilityChart()
        
        isAbilityMatrixGenerated.value = true
      } else {
        console.warn('能力矩阵数据格式不正确:', matrixData)
      }
    } else {
      console.warn('获取能力矩阵数据失败或数据为空')
    }
  } catch (error) {
    console.error('获取能力评估矩阵数据失败:', error)
    isGeneratingAbilityMatrix.value = false
  }
}

// 获取综合分析简报数据
const fetchComprehensiveAnalysisData = async (autoGenerate = true) => {
  try {
    const currentSessionId = sessionId.value || getSessionId()
    if (!currentSessionId) {
      console.warn('未找到面试会话ID')
      return
    }

    console.log('获取综合分析简报数据，会话ID:', currentSessionId)

    // 先检查是否已生成
    const statusRes = await checkComprehensiveAnalysisStatus(currentSessionId)
    console.log('综合分析简报状态检查结果:', statusRes)

    if (statusRes.code === 0 && statusRes.data) {
      isComprehensiveAnalysisGenerated.value = statusRes.data.isGenerated
      
      // 如果未生成且不自动生成，直接返回
      if (!statusRes.data.isGenerated && !autoGenerate) {
        console.log('综合分析简报尚未生成，等待用户手动触发')
        return
      }
      
      // 如果未生成且允许自动生成，尝试生成
      if (!statusRes.data.isGenerated && autoGenerate) {
        console.log('综合分析简报尚未生成，尝试自动生成...')
        
        try {
          isGeneratingComprehensiveAnalysis.value = true
          const generateRes = await generateComprehensiveAnalysis(currentSessionId)
          console.log('综合分析简报生成结果:', generateRes)
          
          if (generateRes.code !== 0) {
            console.warn('综合分析简报生成失败:', generateRes.msg)
            console.error('综合分析简报生成失败，请稍后重试')
            return
          }
          
          isComprehensiveAnalysisGenerated.value = true
          console.log('综合分析简报自动生成完成', { sessionId: currentSessionId, timestamp: new Date().toISOString() })
        } catch (generateError) {
          console.error('生成综合分析简报时出错:', generateError)
          console.error('生成综合分析简报失败')
          return
        } finally {
          isGeneratingComprehensiveAnalysis.value = false
        }
      }
    }

    // 获取综合分析简报数据
    const analysisRes = await getComprehensiveAnalysis(currentSessionId)
    console.log('综合分析简报数据获取结果:', analysisRes)
    console.log('API返回的完整数据结构:', JSON.stringify(analysisRes, null, 2))
    
    if (analysisRes.code === 0 && analysisRes.data) {
      const analysisData = analysisRes.data
      console.log('解析前的analysisData:', JSON.stringify(analysisData, null, 2))
      
      // 更新报告数据中的综合分析相关字段
      if (analysisData.advantages && Array.isArray(analysisData.advantages)) {
        reportData.value.advantages = analysisData.advantages
        console.log('✅ 更新advantages:', analysisData.advantages)
      } else {
        console.log('❌ advantages数据不存在或格式错误:', analysisData.advantages)
      }
      
      if (analysisData.improvements && Array.isArray(analysisData.improvements)) {
        reportData.value.improvements = analysisData.improvements
        console.log('✅ 更新improvements:', analysisData.improvements)
      } else {
        console.log('❌ improvements数据不存在或格式错误:', analysisData.improvements)
      }
      
      // 处理综合分析数据，适配后端返回的字段名
      if (analysisData.comprehensiveAnalysis) {
        reportData.value.comprehensiveAnalysis = analysisData.comprehensiveAnalysis
        console.log('✅ 更新comprehensiveAnalysis:', analysisData.comprehensiveAnalysis)
      } else if (analysisData.overallAssessment || analysisData.detailedAnalysis) {
        // 后端返回的字段名是overallAssessment和detailedAnalysis，需要适配
        // 构建metrics数组用于显示分数
        const metrics = []
        if (analysisData.technicalScore) {
          metrics.push({ label: '技术能力', value: analysisData.technicalScore })
        }
        if (analysisData.communicationScore) {
          metrics.push({ label: '沟通表达', value: analysisData.communicationScore })
        }
        if (analysisData.learningScore) {
          metrics.push({ label: '学习能力', value: analysisData.learningScore })
        }
        
        reportData.value.comprehensiveAnalysis = {
          summary: analysisData.overallAssessment || analysisData.detailedAnalysis || '',
          detailedAnalysis: analysisData.detailedAnalysis || '',
          metrics: metrics,
          scores: {
            technicalScore: analysisData.technicalScore || 0,
            communicationScore: analysisData.communicationScore || 0,
            learningScore: analysisData.learningScore || 0
          }
        }
        console.log('✅ 从后端字段构建comprehensiveAnalysis:', reportData.value.comprehensiveAnalysis)
      } else if (analysisData.summary || analysisData.metrics) {
        // 如果数据直接在根级别（兼容旧版本）
        reportData.value.comprehensiveAnalysis = {
          summary: analysisData.summary || '',
          metrics: analysisData.metrics || []
        }
        console.log('✅ 从根级别构建comprehensiveAnalysis:', reportData.value.comprehensiveAnalysis)
      } else if (analysisData.analysis) {
        // 如果数据在analysis字段下
        reportData.value.comprehensiveAnalysis = analysisData.analysis
        console.log('✅ 从analysis字段更新comprehensiveAnalysis:', analysisData.analysis)
      } else {
        console.log('❌ comprehensiveAnalysis数据不存在:', analysisData)
        // 尝试将整个analysisData作为comprehensiveAnalysis
        if (Object.keys(analysisData).length > 0) {
          reportData.value.comprehensiveAnalysis = analysisData
          console.log('🔄 尝试使用整个analysisData作为comprehensiveAnalysis:', analysisData)
        }
      }
      
      console.log('✅ 综合分析简报数据更新成功:', {
        advantages: reportData.value.advantages,
        improvements: reportData.value.improvements,
        comprehensiveAnalysis: reportData.value.comprehensiveAnalysis
      })
      
      isComprehensiveAnalysisGenerated.value = true
    } else {
      console.warn('获取综合分析简报数据失败或数据为空')
    }
  } catch (error) {
    console.error('获取综合分析简报数据失败:', error)
    isGeneratingComprehensiveAnalysis.value = false
  }
}

// 处理手动生成能力评估矩阵
const handleGenerateAbilityMatrix = async () => {
  try {
    const currentSessionId = sessionId.value || getSessionId()
    if (!currentSessionId) {
      console.error('未找到面试会话ID')
      return
    }

    console.log('用户手动触发能力矩阵生成，会话ID:', currentSessionId)
    
    // 显示确认对话框
    const isRegenerate = isAbilityMatrixGenerated.value
    const result = await ElMessageBox.confirm(
      isRegenerate 
        ? '将重新生成六维能力评估矩阵，这会覆盖现有的分析结果，分析过程可能需要30-60秒，是否继续？'
        : '即将基于您的面试表现生成六维能力评估矩阵，分析过程可能需要30-60秒，是否继续？',
      isRegenerate ? '重新生成能力评估矩阵' : '生成能力评估矩阵',
      {
        confirmButtonText: isRegenerate ? '重新分析' : '开始分析',
        cancelButtonText: '取消',
        type: isRegenerate ? 'warning' : 'info',
        icon: markRaw(MagicStick)
      }
    ).catch(() => {
      // 用户取消
      return false
    })

    if (!result) {
      return
    }

    isGeneratingAbilityMatrix.value = true
    
    // 后台输出生成进度信息
    console.log('开始AI分析面试表现...', { sessionId: currentSessionId, timestamp: new Date().toISOString() })
    
    const loadingMessage = ElMessage({
      message: 'AI分析中...',
      type: 'info',
      duration: 0, // 不自动关闭
      showClose: false
    })

    try {
      const generateRes = await generateAbilityMatrix(currentSessionId, isRegenerate)
      console.log('手动生成能力矩阵结果:', generateRes)
      
      loadingMessage.close()
      
      if (generateRes.code === 0) {
        console.log('手动生成能力评估矩阵成功', { sessionId: currentSessionId, timestamp: new Date().toISOString() })
        console.log('分析完成')
        
        // 重新获取数据并更新图表
        await fetchAbilityMatrixData(false) // 不再自动生成
        
        isAbilityMatrixGenerated.value = true
      } else {
        console.error(`生成失败：${generateRes.msg || '未知错误'}`)
      }
    } catch (error) {
      loadingMessage.close()
      console.error('手动生成能力矩阵失败:', error)
      console.error('生成失败：网络错误或服务暂不可用')
    } finally {
      isGeneratingAbilityMatrix.value = false
    }
  } catch (error) {
    console.error('处理能力矩阵生成失败:', error)
    isGeneratingAbilityMatrix.value = false
  }
}

// 处理手动生成综合分析简报
const handleGenerateComprehensiveAnalysis = async () => {
  try {
    const currentSessionId = sessionId.value || getSessionId()
    if (!currentSessionId) {
      console.error('未找到面试会话ID')
      return
    }

    console.log('用户手动触发综合分析简报生成，会话ID:', currentSessionId)
    
    // 显示确认对话框
    const isRegenerate = isComprehensiveAnalysisGenerated.value
    const result = await ElMessageBox.confirm(
      isRegenerate 
        ? '将重新生成AI综合分析简报，这会覆盖现有的分析结果，包括综合表现评估、面试亮点和改进建议，分析过程可能需要30-60秒，是否继续？'
        : '即将基于您的面试表现生成AI综合分析简报，包括综合表现评估、面试亮点和改进建议，分析过程可能需要30-60秒，是否继续？',
      isRegenerate ? '重新生成综合分析简报' : '生成综合分析简报',
      {
        confirmButtonText: isRegenerate ? '重新分析' : '开始分析',
        cancelButtonText: '取消',
        type: isRegenerate ? 'warning' : 'info',
        icon: markRaw(MagicStick)
      }
    ).catch(() => {
      // 用户取消
      return false
    })

    if (!result) {
      return
    }

    isGeneratingComprehensiveAnalysis.value = true
    
    // 后台输出生成进度信息
    console.log('开始AI综合分析面试表现...', { sessionId: currentSessionId, timestamp: new Date().toISOString() })
    
    const loadingMessage = ElMessage({
      message: 'AI综合分析中...',
      type: 'info',
      duration: 0, // 不自动关闭
      showClose: false
    })

    try {
      const generateRes = await generateComprehensiveAnalysis(currentSessionId, isRegenerate)
      console.log('手动生成综合分析简报结果:', generateRes)
      
      loadingMessage.close()
      
      if (generateRes.code === 0) {
        console.log('手动生成综合分析简报成功', { sessionId: currentSessionId, timestamp: new Date().toISOString() })
        console.log('综合分析完成')
        
        // 重新获取数据并更新显示
        await fetchComprehensiveAnalysisData()
        
        isComprehensiveAnalysisGenerated.value = true
      } else {
        console.error(`生成失败：${generateRes.msg || '未知错误'}`)
      }
    } catch (error) {
      loadingMessage.close()
      console.error('手动生成综合分析简报失败:', error)
      console.error('生成失败：网络错误或服务暂不可用')
    } finally {
      isGeneratingComprehensiveAnalysis.value = false
    }
  } catch (error) {
    console.error('处理综合分析简报生成失败:', error)
    isGeneratingComprehensiveAnalysis.value = false
  }
}

// 处理手动生成个性化建议
const handleGeneratePersonalizedSuggestions = async () => {
  try {
    const currentSessionId = sessionId.value || getSessionId()
    if (!currentSessionId) {
      console.error('未找到面试会话ID')
      return
    }

    console.log('用户手动触发个性化建议生成，会话ID:', currentSessionId)
    
    // 显示确认对话框
    const isRegenerate = isPersonalizedSuggestionsGenerated.value
    const result = await ElMessageBox.confirm(
      isRegenerate 
        ? '将重新生成AI个性化建议，这会覆盖现有的建议内容，基于您的整体面试表现提供更精准的改进方向，分析过程可能需要30-60秒，是否继续？'
        : '即将基于您的整体面试表现生成AI个性化建议，包括技能改进方向、面试技巧优化和职业发展建议，分析过程可能需要30-60秒，是否继续？',
      isRegenerate ? '重新生成个性化建议' : '生成个性化建议',
      {
        confirmButtonText: isRegenerate ? '重新分析' : '开始分析',
        cancelButtonText: '取消',
        type: isRegenerate ? 'warning' : 'info',
        icon: markRaw(MagicStick)
      }
    ).catch(() => {
      // 用户取消
      return false
    })

    if (!result) {
      return
    }

    isGeneratingPersonalizedSuggestions.value = true
    
    // 后台输出生成进度信息
    console.log('开始AI生成个性化建议...', { sessionId: currentSessionId, timestamp: new Date().toISOString() })
    
    const loadingMessage = ElMessage({
      message: 'AI正在分析您的面试表现，生成个性化建议...',
      type: 'info',
      duration: 0, // 不自动关闭
      showClose: false
    })

    try {
      // 调用后端API生成个性化建议
      const result = await generatePersonalizedSuggestions(currentSessionId, isRegenerate)
      
      loadingMessage.close()
      
      if (result.code === 0) {
        console.log('手动生成个性化建议成功', { sessionId: currentSessionId, timestamp: new Date().toISOString() })
        console.log('个性化建议生成完成')
        
        // 获取生成的个性化建议
        await fetchPersonalizedSuggestions(currentSessionId)
        isPersonalizedSuggestionsGenerated.value = true
      } else {
        console.error(`生成失败：${result.msg || '未知错误'}`)
      }
    } catch (error) {
      loadingMessage.close()
      console.error('手动生成个性化建议失败:', error)
      console.error('生成失败：网络错误或服务暂不可用')
    } finally {
      isGeneratingPersonalizedSuggestions.value = false
    }
  } catch (error) {
    console.error('处理个性化建议生成失败:', error)
    isGeneratingPersonalizedSuggestions.value = false
  }
}

// 获取个性化建议数据
const fetchPersonalizedSuggestions = async (currentSessionId) => {
  try {
    const result = await getPersonalizedSuggestions(currentSessionId)
    
    if (result.code === 0 && result.data) {
        console.log('获取个性化建议原始数据:', result.data)
        
        // 解析个性化建议数据
        try {
          let dataToProcess = result.data
          
          // 如果数据是对象，直接使用
          if (typeof result.data === 'object' && result.data !== null) {
            personalizedSuggestionsData.value = result.data
            personalizedSuggestions.value = '数据已解析'
          } 
          // 如果数据是字符串，尝试解析为JSON
          else if (typeof result.data === 'string') {
            // 检查是否是markdown格式的JSON（以```json开头，以```结尾）
            let jsonString = result.data
            if (jsonString.startsWith('```json') && jsonString.endsWith('```')) {
              // 提取JSON部分
              jsonString = jsonString.replace(/^```json\s*\n/, '').replace(/\n\s*```$/, '')
            } else if (jsonString.startsWith('```') && jsonString.endsWith('```')) {
              // 处理其他格式的代码块
              jsonString = jsonString.replace(/^```[a-zA-Z]*\s*\n/, '').replace(/\n\s*```$/, '')
            }
            
            const parsedData = JSON.parse(jsonString)
            personalizedSuggestionsData.value = parsedData
            personalizedSuggestions.value = '数据已解析'
          }
          
          console.log('个性化建议数据解析成功:', personalizedSuggestionsData.value)
          console.log('isPersonalizedSuggestionsGenerated状态:', isPersonalizedSuggestionsGenerated.value)
          console.log('personalizedSuggestionsData状态:', !!personalizedSuggestionsData.value)
        } catch (parseError) {
          console.error('解析个性化建议数据失败:', parseError)
          // 如果解析失败，fallback到原来的逻辑
          personalizedSuggestions.value = typeof result.data === 'string' ? result.data : JSON.stringify(result.data, null, 2)
          personalizedSuggestionsData.value = null
        }
      } else {
        console.warn('获取个性化建议失败:', result.msg)
        personalizedSuggestions.value = ''
        personalizedSuggestionsData.value = null
      }
  } catch (error) {
    console.error('获取个性化建议异常:', error)
    personalizedSuggestions.value = ''
    personalizedSuggestionsData.value = null
  }
}

// 检查个性化建议状态并获取数据
const checkAndFetchPersonalizedSuggestions = async (currentSessionId) => {
  try {
    console.log('检查个性化建议状态，当前session_id:', currentSessionId)
    // 检查个性化建议是否已生成
    const statusResult = await checkPersonalizedSuggestionsStatus(currentSessionId)
    
    if (statusResult.code === 0 && statusResult.data && statusResult.data.isGenerated === true) {
      // 已生成，获取数据
      console.log('检测到个性化建议已生成，准备获取数据...')
      isPersonalizedSuggestionsGenerated.value = true
      await fetchPersonalizedSuggestions(currentSessionId)
      console.log('获取数据后的状态:', {
        isGenerated: isPersonalizedSuggestionsGenerated.value,
        hasData: !!personalizedSuggestionsData.value
      })
    } else if (statusResult.code === 0 && statusResult.data && statusResult.data.isGenerated === false) {
      // 未生成
      console.log('个性化建议尚未生成')
      isPersonalizedSuggestionsGenerated.value = false
      personalizedSuggestions.value = ''
      personalizedSuggestionsData.value = null
    } else {
      console.warn('检查个性化建议状态失败')
      isPersonalizedSuggestionsGenerated.value = false
    }
  } catch (error) {
    console.error('检查个性化建议状态异常:', error)
    isPersonalizedSuggestionsGenerated.value = false
  }
}

// 更新面试状态为report
const updateInterviewStatusToReport = async (sessionId, reportId) => {
  try {

    
    // 存储报告ID到localStorage
    if (reportId) {
      localStorage.setItem('lastReportId', reportId)

    }
    
    // 更新面试状态
    localStorage.setItem('interviewStatus', 'report')
    
    // 同步更新后端申请状态为4(已完成)
    try {
      const response = await fetch('/api/users/current', {
        method: 'GET',
        headers: {
          'Authorization': getAuthToken(),
          'Content-Type': 'application/json'
        }
      });
      if (response.ok) {
        const userData = await response.json();
        
        // 查找对应的申请记录并更新状态
        const appResponse = await fetch(`/api/job-applications/user?userId=${userData.id}`, {
          method: 'GET',
          headers: {
            'Authorization': getAuthToken(),
            'Content-Type': 'application/json'
          }
        });
        if (appResponse.ok) {
          const applications = await appResponse.json();
          // 找到对应会话的申请记录
          const matchedApp = applications.find(app => 
            app.interviewSessionId === currentSessionId || app.sessionId === currentSessionId
          );
          
          if (matchedApp && matchedApp.status < 4) {
            await fetch(`/api/job-applications/${matchedApp.id}/status`, {
              method: 'PUT',
              headers: {
                'Authorization': getAuthToken(),
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                status: 4,
                feedback: `用户已查看分析报告，面试完成，报告ID: ${reportId}`
              })
            });

          }
        }
      }
    } catch (error) {

    }
    
    // 通知首页更新状态（如果首页已加载）
    if (window.setInterviewReportReady) {

      window.setInterviewReportReady()
    }
    
    // 后台刷新统计数据
    console.log('刷新首页统计数据...', { timestamp: new Date().toISOString() })
    if (window.refreshDashboardStatistics) {
      window.refreshDashboardStatistics()
    }
    
    // 如果有面试store，也更新store状态
    if (window.interviewStore && window.interviewStore.setStatus) {
      window.interviewStore.setStatus('report')

    }
    
  } catch (error) {
    console.error('更新面试状态失败:', error)
  }
}

// 生命周期钩子
onMounted(async () => {
  // 初始化会话ID
  const currentSessionId = getSessionId()
  console.log('页面初始化，当前session_id:', currentSessionId)


  // 获取表情分析数据
  await fetchExpressionData()

  // 获取技术能力分析数据
  await fetchTechAnalysis()

  // 获取报告基本信息
  try {
    const reportRes = await getReportBySessionId(currentSessionId)
    if (reportRes.code === 0 && reportRes.data) {
      const report = reportRes.data
      // 同步报告基本信息
      reportData.value.companyName = report.companyName || '未知公司'
      reportData.value.positionName = report.positionName || '未知职位'
      reportData.value.interviewTime = report.interviewTime || report.startTime
      reportData.value.duration = report.duration || 0
      reportData.value.totalScore = Number(report.totalScore) || 0
      reportData.value.starRating = Number(report.starRating) || 0
      
      // 报告成功加载，更新面试状态为'report'
      await updateInterviewStatusToReport(currentSessionId, report.id)
    }
  } catch (e) {
    console.error('获取报告基本信息失败', e)
  }

  // 获取STAR结构数据
  try {
    const starRes = await getStarScores(currentSessionId)
    if (starRes.code === 0 && starRes.data) {
      reportData.value.starAnalysis = {
        situation: starRes.data.star_situation_score || 0,
        task: starRes.data.star_task_score || 0,
        action: starRes.data.star_action_score || 0,
        result: starRes.data.star_result_score || 0
      }
    }
  } catch (e) {
    console.error('获取STAR结构失败', e)
  }

  // 获取问题回答质量分析
  try {
    const answerRes = await getAnswerQuality(currentSessionId)
    if (answerRes.code === 0 && answerRes.data) {
      // 转换为前端需要的格式
      const mapping = [
        { key: 'tech_score', name: '技术问题' },
        { key: 'project_score', name: '项目经验' },
        { key: 'team_score', name: '团队协作' },
        { key: 'plan_score', name: '沟通表达' },
        { key: 'attitude_score', name: '解决方案' }
      ]
      reportData.value.answerQuality = mapping.map(item => ({
        name: item.name,
        score: answerRes.data[item.key] || 0
      }))
    }
  } catch (e) {
    console.error('获取问题回答质量分析失败', e)
  }

  // 获取能力评估矩阵数据（不自动生成，让用户手动触发）
  await fetchAbilityMatrixData(false)

  // 获取综合分析简报数据（不自动生成，让用户手动触发）
  await fetchComprehensiveAnalysisData(false)

  // 检查个性化建议状态并获取数据
  await checkAndFetchPersonalizedSuggestions(currentSessionId)

  // 初始化图表
  initCharts()
})
</script>

<style scoped>
.interview-analysis-report {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.back-btn {
  margin-bottom: 24px;
  font-size: 15px;
  color: #457B9D;
  border: 1px solid #A8DADC;
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  border-radius: 12px;
  padding: 10px 20px;
  font-weight: 600;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(69, 123, 157, 0.1);
}

.back-btn:hover {
  background: linear-gradient(135deg, #457B9D 0%, #A8DADC 100%);
  color: #ffffff;
  border-color: transparent;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(69, 123, 157, 0.2);
}

/* 顶部统计区 */
.report-header {
  margin-bottom: 36px;
  background: linear-gradient(135deg, #f0f7ff 0%, #f9fcff 100%);
  border-radius: 20px;
  padding: 36px 44px;
  box-shadow: 0 8px 32px rgba(69, 123, 157, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(168, 218, 220, 0.3);
  backdrop-filter: blur(10px);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.report-header:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(69, 123, 157, 0.12), 0 4px 16px rgba(0, 0, 0, 0.06);
  border-color: rgba(168, 218, 220, 0.4);
}

.header-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  width: 100%;
  gap: 40px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
}

.title-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.main-title {
  font-size: 40px;
  font-weight: 800;
  background: linear-gradient(135deg, #264653 0%, #457B9D 100%);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  margin: 0;
  letter-spacing: -1px;
  line-height: 1.1;
  animation: slideInDown 0.8s ease-out 0.2s both;
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

.sub-title {
  font-size: 16px;
  color: #7a869a;
  margin: 0;
  font-weight: 500;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 16px 0;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.info-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: rgba(69, 123, 157, 0.1);
  border-radius: 8px;
  flex-shrink: 0;
}

.info-icon {
  color: #457B9D;
  font-size: 16px;
}

.info-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #7a869a;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.info-value {
  font-size: 16px;
  font-weight: 600;
  color: #264653;
  line-height: 1.2;
}

.info-divider {
  width: 1px;
  height: 32px;
  background: linear-gradient(to bottom, transparent, #e0e6f3, transparent);
  flex-shrink: 0;
}

.header-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 20px;
  min-width: 200px;
}

.score-section {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  text-align: right;
}

.score-label {
  font-size: 14px;
  color: #7a869a;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.score-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.current-score {
  font-size: 48px;
  font-weight: 800;
  color: #264653;
  line-height: 1;
  letter-spacing: -1px;
}

.total-score {
  font-size: 18px;
  color: #7a869a;
  font-weight: 600;
}

.score-desc {
  font-size: 12px;
  color: #7a869a;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-top: 4px;
}

.export-btn {
  min-width: 120px;
  height: 36px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #457B9D 0%, #A8DADC 100%);
  border: none;
  border-radius: 8px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-shadow: 0 2px 8px rgba(69, 123, 157, 0.25);
  transition: all 0.3s ease;
}

.export-btn:hover {
  background: linear-gradient(135deg, #264653 0%, #457B9D 100%);
  box-shadow: 0 6px 20px rgba(69, 123, 157, 0.4);
  transform: translateY(-2px);
}

.export-btn:active {
  transform: translateY(0px);
  box-shadow: 0 2px 12px rgba(69, 123, 157, 0.3);
}

/* 核心图表区 */
.report-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chart-row {
  display: flex;
  gap: 28px;
  margin-bottom: 0;
  animation: slideInUp 0.6s ease-out both;
}

.chart-row.half-width {
  max-width: 50%;
}

.chart-row:nth-child(1) {
  animation-delay: 0.3s;
}

.chart-row:nth-child(2) {
  animation-delay: 0.4s;
}

.chart-row:nth-child(3) {
  animation-delay: 0.5s;
}

.chart-row:nth-child(4) {
  animation-delay: 0.6s;
}

.chart-row:nth-child(5) {
  animation-delay: 0.7s;
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

.chart-card {
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 6px 24px rgba(69, 123, 157, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.8);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
}

.chart-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 48px rgba(69, 123, 157, 0.12), 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: rgba(69, 123, 157, 0.2);
}

.large-chart {
  flex: 2;
}

.medium-chart {
  flex: 1;
}

/* 确保第一行三个卡片能够正确布局 */
.chart-row .medium-chart {
  min-width: 0; /* 防止flex item收缩 */
}

/* 第一行卡片宽度比例设置 - 考虑28px gap的影响 */
.expression-wide {
  flex: 0 0 calc(60% - 16.8px); /* 表情分析卡片占60%宽度，减去gap的60%部分 */
  max-width: calc(60% - 16.8px);
}

.star-narrow {
  flex: 0 0 calc(40% - 11.2px); /* STAR结构完整度卡片占40%宽度，减去gap的40%部分 */
  max-width: calc(40% - 11.2px);
}



.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 19px;
  font-weight: 700;
  color: #264653;
  height: 44px;
  padding: 0;
  letter-spacing: -0.3px;
}

.card-header .header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-start;
}

.card-header .header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header .header-actions .el-button {
  font-weight: 600;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(69, 123, 157, 0.2);
  transition: all 0.3s ease;
}

.card-header .header-actions .el-button:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(69, 123, 157, 0.3);
}

.card-header .header-actions .el-tag {
  font-weight: 500;
  border-radius: 6px;
  padding: 4px 8px;
}

.card-icon {
  color: #457B9D;
  font-size: 22px;
}

.chart-container {
  height: 350px;
  width: 100%;
}

.expression-chart .chart-container {
  height: 400px;
}

.star-chart .chart-container {
  height: 380px;
}

/* 自我介绍分析卡片样式 */
.introduction-chart {
  display: flex;
  flex-direction: column;
}

.introduction-chart .introduction-analysis-chart {
  height: 350px;
}

/* 技术能力分析卡片样式 */
.tech-analysis-chart .tech-analysis-container {
  height: 350px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

/* 综合得分标题样式 */
.overall-score-header {
  margin-bottom: 8px;
}

.score-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.score-icon {
  color: #f39c12;
  font-size: 18px;
}

.score-highlight {
  color: #457B9D;
  font-weight: 700;
  font-size: 18px;
}

/* 分项得分分析样式 */
.score-breakdown {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
}

.score-item {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 12px;
  transition: all 0.2s ease;
}

.score-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.score-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.item-icon {
  color: #457B9D;
  font-size: 16px;
}

.item-title {
  font-weight: 600;
  color: #333;
  font-size: 14px;
  flex: 1;
}

.item-score {
  font-weight: 700;
  color: #457B9D;
  font-size: 14px;
  background: rgba(69, 123, 157, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}

.score-item-description {
  color: #666;
  font-size: 13px;
  line-height: 1.5;
  margin-left: 24px;
}

/* 评估中状态样式 */
.evaluation-pending {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  text-align: center;
}

.pending-icon {
  color: #6c757d;
  font-size: 18px;
}

.pending-text {
  color: #6c757d;
  font-size: 14px;
}

/* AI 总体评估样式 */
.ai-summary-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #dee2e6;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.summary-icon {
  color: #457B9D;
  font-size: 16px;
}

.summary-title {
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.summary-content {
  min-height: 40px;
  max-height: 100px;
  overflow-y: auto;
  padding: 8px 0;
}

.summary-text {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  word-wrap: break-word;
}

/* 全宽度行布局 */
.full-width-row {
  width: 100%;
}

.full-width-card {
  width: 100%;
  flex: none;
}

.analysis-card {
  flex: 1;
  border-radius: 20px;
  box-shadow: 0 6px 24px rgba(69, 123, 157, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.analysis-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(69, 123, 157, 0.12), 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: rgba(69, 123, 157, 0.2);
}

.suggestion-card {
  flex: 1;
  border-radius: 20px;
  box-shadow: 0 6px 24px rgba(69, 123, 157, 0.08), 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.suggestion-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(69, 123, 157, 0.12), 0 4px 16px rgba(0, 0, 0, 0.08);
  border-color: rgba(69, 123, 157, 0.2);
}

.analysis-content {
  padding: 12px;
}

.analysis-section {
  margin-bottom: 24px;
}

.analysis-section:last-child {
  margin-bottom: 0;
}

.analysis-section h3 {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  border-bottom: 2px solid #eaeaea;
  padding-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.analysis-summary {
  margin-bottom: 16px;
}

.summary-text {
  font-size: 15px;
  line-height: 1.6;
  color: #555;
  margin-bottom: 16px;
  text-align: justify;
}

.key-metrics {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.metric-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fcff 0%, #f0f7ff 100%);
  border-radius: 16px;
  border: 1px solid rgba(168, 218, 220, 0.3);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 8px rgba(69, 123, 157, 0.05);
}

.metric-item:hover {
  transform: translateY(-2px);
  background: linear-gradient(135deg, #ffffff 0%, #f8fcff 100%);
  border-color: rgba(69, 123, 157, 0.2);
  box-shadow: 0 4px 16px rgba(69, 123, 157, 0.08);
}

/* 分析内容状态样式 */
.analysis-placeholder,
.analysis-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
  padding: 40px 20px;
}

.placeholder-content,
.loading-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  text-align: center;
}

.placeholder-icon,
.loading-icon {
  font-size: 48px;
  color: #A8DADC;
  margin-bottom: 8px;
}

.loading-icon {
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.placeholder-text,
.loading-text {
  font-size: 18px;
  font-weight: 600;
  color: #457B9D;
  margin: 0;
}

.placeholder-desc,
.loading-desc {
  font-size: 14px;
  color: #7a869a;
  margin: 0;
  max-width: 300px;
  line-height: 1.5;
}

.analysis-sections {
  padding: 0;
}

.no-data {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #7a869a;
  font-size: 14px;
  font-style: italic;
}

.metric-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.metric-value {
  font-size: 18px;
  font-weight: 700;
  color: #457B9D;
}

.highlight-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.highlight-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 14px;
  line-height: 1.5;
}

.highlight-icon {
  color: #457B9D;
  font-size: 16px;
  margin-top: 2px;
}

.suggestion-content {
  padding: 12px;
}

.suggestion-section {
  margin-bottom: 16px;
}

.suggestion-section h3 {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  border-bottom: 2px solid #eaeaea;
  padding-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.improvement-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.improvement-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
  line-height: 1.4;
}

.improvement-icon {
  color: #FFD666;
  font-size: 14px;
  margin-top: 2px;
}

.section-icon {
  color: #A8DADC;
  font-size: 16px;
}

/* AI个性化建议样式 */
.ai-suggestions {
  margin-bottom: 20px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fcff 0%, #f0f7ff 100%);
  border-radius: 12px;
  border: 1px solid rgba(69, 123, 157, 0.1);
}

.ai-suggestion-content {
  margin-top: 8px;
}

.suggestion-text {
  font-size: 14px;
  line-height: 1.6;
  color: #444;
  margin: 0;
  text-align: justify;
  background: rgba(255, 255, 255, 0.8);
  padding: 12px;
  border-radius: 8px;
  border-left: 4px solid #457B9D;
}

.suggestion-section.with-ai-suggestions {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(69, 123, 157, 0.1);
}

/* 分析状态样式 */
.analysis-placeholder,
.analysis-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  color: #666;
  background: linear-gradient(135deg, #fafbfc 0%, #f8f9fa 100%);
  border-radius: 12px;
  border: 2px dashed rgba(69, 123, 157, 0.2);
  margin: 20px 0;
}

.placeholder-icon,
.loading-icon {
  font-size: 48px;
  color: #A8DADC;
  margin-bottom: 12px;
}

.loading-icon {
  animation: loading-spin 1.5s linear infinite;
}

@keyframes loading-spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.analysis-placeholder span,
.analysis-loading span {
  font-size: 14px;
  line-height: 1.5;
  max-width: 300px;
}

/* 个性化建议卡片的分析状态样式 */
.suggestion-card .analysis-placeholder {
  background: linear-gradient(135deg, #f8fcff 0%, #f0f7ff 100%);
  border: 2px dashed rgba(69, 123, 157, 0.3);
  border-radius: 16px;
  padding: 48px 32px;
  text-align: center;
}

.suggestion-card .analysis-placeholder .placeholder-icon {
  font-size: 48px;
  color: #457B9D;
  margin-bottom: 20px;
  opacity: 0.8;
}

.suggestion-card .analysis-placeholder .placeholder-content {
  max-width: 400px;
  margin: 0 auto;
}

.suggestion-card .analysis-placeholder .placeholder-title {
  font-size: 20px;
  font-weight: 600;
  color: #264653;
  margin-bottom: 12px;
  line-height: 1.3;
}

.suggestion-card .analysis-placeholder .placeholder-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
  line-height: 1.5;
}

.suggestion-card .analysis-placeholder .feature-list {
  list-style: none;
  padding: 0;
  margin: 0 0 24px 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.suggestion-card .analysis-placeholder .feature-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #457B9D;
  font-weight: 500;
  padding: 8px 12px;
  background: rgba(69, 123, 157, 0.05);
  border-radius: 8px;
  transition: all 0.3s ease;
}

.suggestion-card .analysis-placeholder .feature-list li:hover {
  background: rgba(69, 123, 157, 0.1);
  transform: translateY(-1px);
}

.suggestion-card .analysis-placeholder .feature-list li .el-icon {
  color: #A8DADC;
  font-size: 14px;
}

.suggestion-card .analysis-placeholder .placeholder-action {
  font-size: 14px;
  color: #666;
  font-weight: 500;
  margin: 0;
  padding: 16px 20px;
  background: rgba(69, 123, 157, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(69, 123, 157, 0.15);
}

/* 个性化建议卡片的分析中状态样式 */
.suggestion-card .analysis-loading {
  background: linear-gradient(135deg, #fff8f0 0%, #fff4e6 100%);
  border: 2px solid rgba(255, 159, 64, 0.2);
  border-radius: 16px;
  padding: 48px 32px;
  text-align: center;
}

.suggestion-card .analysis-loading .loading-icon {
  font-size: 36px;
  color: #ff9f40;
  margin-bottom: 16px;
  animation: spin 1.5s linear infinite;
}

.suggestion-card .analysis-loading .loading-title {
  font-size: 18px;
  font-weight: 600;
  color: #264653;
  margin-bottom: 8px;
  line-height: 1.3;
}

.suggestion-card .analysis-loading .loading-subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
  line-height: 1.5;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

@media (max-width: 1200px) {
  .chart-row {
    flex-direction: column;
  }

  /* 移动设备上恢复等宽布局 */
  .expression-wide,
  .star-narrow {
    flex: 1;
    max-width: 100%;
  }

  .report-header {
    padding: 24px 28px;
  }

  .header-main {
    flex-direction: column;
    align-items: stretch;
    gap: 24px;
  }

  .header-left {
    width: 100%;
    gap: 16px;
  }

  .main-title {
    font-size: 28px;
    letter-spacing: -0.6px;
  }

  .sub-title {
    font-size: 14px;
  }

  .header-right {
    width: 100%;
    align-items: stretch;
    gap: 16px;
    min-width: auto;
  }

  .info-row {
    flex-direction: column;
    gap: 16px;
    padding: 12px 0;
  }

  .info-item {
    width: 100%;
    justify-content: flex-start;
    padding: 12px 16px;
    background: rgba(255, 255, 255, 0.6);
    border-radius: 12px;
    border: 1px solid rgba(69, 123, 157, 0.1);
  }

  .info-divider {
    display: none;
  }

  .score-section {
    width: 100%;
    align-items: center;
    text-align: center;
    padding: 20px;
    background: rgba(255, 255, 255, 0.6);
    border-radius: 12px;
    border: 1px solid rgba(69, 123, 157, 0.1);
  }

  .score-display {
    align-items: center;
    justify-content: center;
  }

  .current-score {
    font-size: 36px;
  }

  .total-score {
    font-size: 16px;
  }

  .export-btn {
    width: 100%;
    height: 48px;
    font-size: 16px;
    min-width: auto;
  }

  .full-width-row {
    flex-direction: column;
  }

  .key-metrics {
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .report-header {
    padding: 20px 24px;
  }

  .main-title {
    font-size: 24px;
    letter-spacing: -0.4px;
  }

  .sub-title {
    font-size: 13px;
  }

  .info-icon-wrapper {
    width: 32px;
    height: 32px;
  }

  .info-icon {
    font-size: 14px;
  }

  .info-value {
    font-size: 15px;
  }

  .current-score {
    font-size: 32px;
  }

  .total-score {
    font-size: 14px;
  }

  .export-btn {
    height: 36px;
    font-size: 14px;
    min-width: 100px;
  }
}

/* 个性化建议样式 */
.ai-suggestions {
  padding: 20px 0;
}

.suggestion-section {
  margin-bottom: 30px;
}

.suggestion-section h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.suggestion-section .section-icon {
  color: #457b9d;
}

/* 技能改进样式 */
.skill-improvements .skill-item {
  margin-bottom: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #457b9d;
}

.skill-category {
  font-size: 16px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 10px;
}

.skill-content > div {
  margin-bottom: 8px;
}

.skill-suggestions ul {
  margin: 5px 0 0 20px;
  padding: 0;
}

.skill-suggestions li {
  margin-bottom: 5px;
  line-height: 1.5;
}

/* 沟通改进样式 */
.communication-improvements > div {
  margin-bottom: 15px;
  padding: 12px;
  border-radius: 6px;
}

.comm-strengths {
  background: #e8f5e8;
  border-left: 4px solid #28a745;
}

.comm-weaknesses {
  background: #fff3cd;
  border-left: 4px solid #ffc107;
}

.comm-actions {
  background: #e3f2fd;
  border-left: 4px solid #2196f3;
}

.communication-improvements h4 {
  margin-bottom: 8px;
  font-size: 14px;
}

.communication-improvements ul {
  margin: 5px 0 0 20px;
  padding: 0;
}

.communication-improvements li {
  margin-bottom: 5px;
  line-height: 1.5;
}

/* 学习路径样式 */
.learning-path .phase-item {
  margin-bottom: 20px;
  padding: 15px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 8px;
  border: 1px solid #e1e5e9;
}

.phase-title {
  font-size: 16px;
  font-weight: bold;
  color: #2c3e50;
  margin-bottom: 10px;
}

.phase-content > div {
  margin-bottom: 8px;
}

.phase-goals ul {
  margin: 5px 0 0 20px;
  padding: 0;
}

.phase-goals li {
  margin-bottom: 5px;
  line-height: 1.5;
}

/* 实践项目样式 */
.practice-projects .project-item {
  margin-bottom: 20px;
  padding: 15px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e1e5e9;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.project-name {
  font-size: 16px;
  font-weight: bold;
  color: #457b9d;
  margin-bottom: 10px;
}

.project-description {
  margin-bottom: 10px;
  line-height: 1.5;
  color: #555;
}

.project-meta {
  margin-bottom: 10px;
  font-size: 14px;
}

.project-meta span {
  margin-right: 15px;
  color: #666;
}

.project-skills {
  margin-top: 10px;
}

.skill-tag {
  margin-right: 8px;
  margin-bottom: 5px;
}

/* 面试技巧样式 */
.interview-tips .tips-category {
  margin-bottom: 15px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.interview-tips h4 {
  margin-bottom: 8px;
  font-size: 14px;
  color: #2c3e50;
}

.interview-tips ul {
  margin: 5px 0 0 20px;
  padding: 0;
}

.interview-tips li {
  margin-bottom: 5px;
  line-height: 1.5;
}

/* 职业发展样式 */
.career-advice > div {
  margin-bottom: 15px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.career-advice h4 {
  margin-bottom: 8px;
  font-size: 14px;
  color: #2c3e50;
}

.career-advice p {
  margin: 0;
  line-height: 1.5;
}

.career-advice ul {
  margin: 5px 0 0 20px;
  padding: 0;
}

.career-advice li {
  margin-bottom: 5px;
  line-height: 1.5;
}
</style> 