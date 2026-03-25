<template>
  <div class="introduction-analysis-chart">
    <div class="chart-header">
      <h3 class="chart-title">
        <el-icon><DocumentChecked /></el-icon>
        自我介绍分析
      </h3>
      <div class="overall-score" v-if="analysisData && !analysisData.fallback">
        <span class="score-label">综合得分</span>
        <span class="score-value" :class="getScoreClass(analysisData.overallScore)">
          {{ analysisData.overallScore }}/10
        </span>
      </div>
      <div class="overall-score" v-else-if="analysisData && analysisData.fallback">
        <span class="score-label">综合得分</span>
        <span class="score-value analyzing">
          {{ analysisData.overallScore }}
        </span>
      </div>
    </div>
    
    <!-- 左右布局：得分和分析结果 -->
    <div class="content-layout" v-if="analysisData && !analysisData.fallback">
      <!-- 左侧：得分展示 -->
      <div class="scores-column" v-if="hasValidScores">
        <div class="scores-list">
          <div class="score-item">
            <span class="score-label">🗣️ 语言流畅度</span>
            <span class="score-value" :class="getScoreClass(analysisData.fluencyScore)">{{ (analysisData.fluencyScore || 0).toFixed(1) }}</span>
          </div>
          <div class="score-item">
            <span class="score-label">💭 表达能力</span>
            <span class="score-value" :class="getScoreClass(analysisData.expressionScore)">{{ (analysisData.expressionScore || 0).toFixed(1) }}</span>
          </div>
          <div class="score-item">
            <span class="score-label">📝 内容质量</span>
            <span class="score-value" :class="getScoreClass(analysisData.contentScore)">{{ (analysisData.contentScore || 0).toFixed(1) }}</span>
          </div>
        </div>
      </div>

      <!-- 左侧：状态显示（无得分时） -->
      <div class="scores-column status-column" v-if="!hasValidScores">
        <div class="status-message">
          <el-icon class="status-icon"><Loading /></el-icon>
          <span class="status-text">正在分析...</span>
        </div>
      </div>

      <!-- 右侧：AI分析结果 -->
      <div class="analysis-column" v-if="analysisData.analysisResult">
        <div class="analysis-header">
          <el-icon class="analysis-icon"><DocumentChecked /></el-icon>
          <h4>AI智能分析结果</h4>
        </div>
        <div class="analysis-content-box">
          <div class="analysis-text" v-html="formatAnalysisResultAsHtml(analysisData.analysisResult)">
          </div>
        </div>
      </div>

      <!-- 右侧：无分析结果时的占位 -->
      <div class="analysis-column placeholder-column" v-else>
        <div class="placeholder-message">
          <el-icon class="placeholder-icon"><Loading /></el-icon>
          <span>正在生成AI分析结果...</span>
        </div>
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-loading 
        element-loading-text="正在分析自我介绍..."
        element-loading-spinner="el-icon-loading"
        element-loading-background="rgba(0, 0, 0, 0.8)"
      />
    </div>
    
    <!-- 无数据状态 -->
    <div v-else-if="!analysisData" class="no-data">
      <el-empty description="暂无自我介绍分析数据">
        <div style="margin-top: 16px;">
          <el-button type="primary" @click="fetchAnalysisData(0)" :disabled="!props.sessionId">
            重新获取 {{ !props.sessionId ? '(无会话ID)' : '' }}
          </el-button>
          <el-button type="warning" @click="testApiCall" style="margin-left: 12px;">
            测试API (ID:448)
          </el-button>
        </div>
        <div style="margin-top: 12px; color: #666; font-size: 12px;">
          当前会话ID: {{ props.sessionId || '未传入' }}
        </div>
      </el-empty>
    </div>
    
    <!-- 降级状态 -->
    <div v-else-if="analysisData && analysisData.fallback" class="fallback-data">
      <div class="fallback-content">
        <el-icon class="fallback-icon"><Loading /></el-icon>
        <h4>AI分析进行中</h4>
        <p>{{ analysisData.analysisResult }}</p>
        <div style="margin-top: 16px;">
          <el-button type="primary" @click="fetchAnalysisData(0)">
            <el-icon><Refresh /></el-icon>
            重新获取
          </el-button>
          <el-button type="warning" @click="testApiCall" style="margin-left: 12px;">测试API</el-button>
        </div>
        <div style="margin-top: 12px; font-size: 12px; color: #666;">
          当前sessionId: {{ props.sessionId || '未传入' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { DocumentChecked, Loading, Refresh } from '@element-plus/icons-vue'
import { getIntroductionAnalysis } from '@/api/introductionAnalysis'
import { marked } from 'marked'

// 配置marked选项
marked.setOptions({
  breaks: true, // 支持软换行
  gfm: true     // 支持GitHub Flavored Markdown
})

const props = defineProps({
  sessionId: {
    type: [Number, String],
    required: true
  }
})

// 响应式数据
const analysisData = ref(null)
const loading = ref(false)

// 获取分析数据（支持重试机制）
const fetchAnalysisData = async (retryCount = 0) => {
  if (!props.sessionId) return
  
  const maxRetries = 3
  loading.value = true
  
  try {
    console.log(`获取自我介绍分析数据，会话ID: ${props.sessionId}，重试次数: ${retryCount}`)
    
    const response = await getIntroductionAnalysis(props.sessionId)
    
    console.log('API响应数据:', response)
    
    if (response && response.success && response.data) {
      // 处理BigDecimal类型的数据，确保转换为Number
      const rawData = response.data
      
      analysisData.value = {
        id: rawData.id,
        sessionId: rawData.sessionId,
        introductionText: rawData.introductionText,
        duration: rawData.duration,
        fluencyScore: rawData.fluencyScore ? parseFloat(rawData.fluencyScore) : 0,
        expressionScore: rawData.expressionScore ? parseFloat(rawData.expressionScore) : 0,
        contentScore: rawData.contentScore ? parseFloat(rawData.contentScore) : 0,
        overallScore: rawData.overallScore ? parseFloat(rawData.overallScore) : 0,
        analysisResult: rawData.analysisResult,
        createTime: rawData.createTime,
        updateTime: rawData.updateTime
      }
      
      console.log('✅ 自我介绍分析数据获取成功:', analysisData.value)
      console.log('✅ 处理后的分数数据:', {
        fluencyScore: analysisData.value.fluencyScore,
        expressionScore: analysisData.value.expressionScore,
        contentScore: analysisData.value.contentScore,
        overallScore: analysisData.value.overallScore
      })
    } else {
      console.warn('⚠️ 自我介绍分析数据格式不正确:', response)
      
      // 如果数据不存在且还有重试次数，则重试
      if (retryCount < maxRetries) {
        console.log(`数据可能还在处理中，${2000 * (retryCount + 1)}ms后重试...`)
        setTimeout(() => {
          fetchAnalysisData(retryCount + 1)
        }, 2000 * (retryCount + 1)) // 递增延迟重试
        return
      } else {
        // 所有重试都失败，显示占位数据
        console.log('所有重试失败，显示占位提示')
        showFallbackData()
      }
    }
  } catch (error) {
    console.error('❌ 获取自我介绍分析数据失败:', error)
    
    // 如果是超时错误且还有重试次数，则重试
    if (error.code === 'ECONNABORTED' && retryCount < maxRetries) {
      console.log(`API超时，${3000 * (retryCount + 1)}ms后重试...`)
      setTimeout(() => {
        fetchAnalysisData(retryCount + 1)
      }, 3000 * (retryCount + 1))
      return
    } else if (retryCount >= maxRetries) {
      console.log('重试次数已达上限，显示占位提示')
      showFallbackData()
    }
  } finally {
    // 只在最终失败或成功时关闭loading
    if (retryCount >= maxRetries || analysisData.value) {
      loading.value = false
    }
  }
}

// 显示降级数据
const showFallbackData = () => {
  analysisData.value = {
    overallScore: '分析中',
    fluencyScore: '...',
    expressionScore: '...',
    contentScore: '...',
    analysisResult: '正在分析您的自我介绍，请稍候...\n\n如果长时间未显示结果，可能是因为AI分析需要较长时间处理，建议您稍后刷新页面查看。',
    fallback: true
  }
  loading.value = false
}



// 获取得分等级样式类
const getScoreClass = (score) => {
  if (score >= 8) return 'excellent'
  if (score >= 6) return 'good'
  if (score >= 4) return 'average'
  return 'poor'
}

// 检查是否有有效分数
const hasValidScores = computed(() => {
  if (!analysisData.value) return false
  console.log('检查有效分数:', {
    fluencyScore: analysisData.value.fluencyScore,
    expressionScore: analysisData.value.expressionScore,
    contentScore: analysisData.value.contentScore,
    overallScore: analysisData.value.overallScore
  })
  return (analysisData.value.fluencyScore && analysisData.value.fluencyScore > 0) ||
         (analysisData.value.expressionScore && analysisData.value.expressionScore > 0) ||
         (analysisData.value.contentScore && analysisData.value.contentScore > 0) ||
         (analysisData.value.overallScore && analysisData.value.overallScore > 0)
})

// 格式化分析结果文本
const formatAnalysisResult = (result) => {
  if (!result) return '暂无分析结果'
  // 如果是JSON格式的数据，尝试解析
  if (result.startsWith('{')) {
    try {
      const parsed = JSON.parse(result)
      return parsed.analysis || parsed.result || result
    } catch (e) {
      return result
    }
  }
  return result
}

// 格式化分析结果为HTML（支持Markdown）
const formatAnalysisResultAsHtml = (result) => {
  if (!result) return '<p>暂无分析结果</p>'
  
  let textToFormat = result
  
  // 如果是JSON格式的数据，尝试解析
  if (result.startsWith('{')) {
    try {
      const parsed = JSON.parse(result)
      textToFormat = parsed.analysis || parsed.result || result
    } catch (e) {
      textToFormat = result
    }
  }
  
  try {
    // 使用marked将Markdown转换为HTML
    const htmlContent = marked.parse(textToFormat)
    return htmlContent
  } catch (e) {
    console.error('Markdown解析失败:', e)
    // 如果Markdown解析失败，返回普通文本的HTML
    return `<p>${textToFormat.replace(/\n/g, '<br>')}</p>`
  }
}

// 测试API调用
const testApiCall = async () => {
  try {
    // 使用数据库中确实存在数据的会话ID进行测试
    const testSessionId = 448 // 从数据库看到的最新ID，有完整分析数据
    
    console.log('🧪 开始API测试')
    console.log('- 当前props.sessionId:', props.sessionId) 
    console.log('- 使用测试会话ID:', testSessionId)
    
    const response = await getIntroductionAnalysis(testSessionId)
    
    console.log('🧪 API测试响应:', response)
    
    if (response && response.success && response.data) {
      console.log('🧪 ✅ API测试成功！数据解析:')
      console.log('- sessionId:', response.data.sessionId)
      console.log('- fluencyScore:', response.data.fluencyScore, typeof response.data.fluencyScore)
      console.log('- expressionScore:', response.data.expressionScore, typeof response.data.expressionScore)
      console.log('- contentScore:', response.data.contentScore, typeof response.data.contentScore)
      console.log('- overallScore:', response.data.overallScore, typeof response.data.overallScore)
      console.log('- analysisResult length:', response.data.analysisResult?.length)
      console.log('- analysisResult preview:', response.data.analysisResult?.substring(0, 100) + '...')
      
      alert(`✅ API测试成功！\n会话ID: ${response.data.sessionId}\n分数数据：\n- 语言流畅度: ${response.data.fluencyScore}\n- 表达能力: ${response.data.expressionScore}\n- 内容质量: ${response.data.contentScore}\n- 综合评分: ${response.data.overallScore}\n- 分析结果长度: ${response.data.analysisResult?.length || 0}字符`)
      
      // 直接使用测试数据填充组件（用于调试）
      if (confirm('是否使用测试数据填充当前组件？')) {
        const rawData = response.data
        analysisData.value = {
          id: rawData.id,
          sessionId: rawData.sessionId,
          introductionText: rawData.introductionText,
          duration: rawData.duration,
          fluencyScore: rawData.fluencyScore ? parseFloat(rawData.fluencyScore) : 0,
          expressionScore: rawData.expressionScore ? parseFloat(rawData.expressionScore) : 0,
          contentScore: rawData.contentScore ? parseFloat(rawData.contentScore) : 0,
          overallScore: rawData.overallScore ? parseFloat(rawData.overallScore) : 0,
          analysisResult: rawData.analysisResult,
          createTime: rawData.createTime,
          updateTime: rawData.updateTime
        }
        
        console.log('🧪 测试数据已填充到组件:', analysisData.value)
      }
      
    } else {
      console.log('🧪 ❌ API返回数据格式异常:', response)
      alert(`❌ API测试失败\n响应格式异常:\n${JSON.stringify(response, null, 2)}`)
    }
    
  } catch (error) {
    console.error('🧪 ❌ API测试异常:', error)
    alert(`❌ API测试异常:\n${error.message}\n\n请检查：\n1. 后端服务是否启动\n2. API地址是否正确\n3. 网络连接是否正常`)
  }
}

// 监听sessionId变化
watch(() => props.sessionId, (newSessionId) => {
  if (newSessionId) {
    fetchAnalysisData()
  }
}, { immediate: true })

// 组件挂载
onMounted(() => {
  if (props.sessionId) {
    fetchAnalysisData()
  }
})
</script>

<style scoped>
.introduction-analysis-chart {
  border-radius: 12px;
  padding: 18px 18px 8px 18px;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  height: fit-content;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.chart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.overall-score {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-label {
  font-size: 16px;
  color: #606266;
  font-weight: 500;
}

.score-value {
  font-size: 26px;
  font-weight: 700;
  padding: 4px 12px;
  border-radius: 6px;
}

.score-value.excellent {
  color: #67c23a;
}

.score-value.good {
  color: #409eff;
}

.score-value.average {
  color: #e6a23c;
}

.score-value.poor {
  color: #f56c6c;
}

/* 左右布局容器 */
.content-layout {
  display: flex;
  gap: 16px;
  align-items: stretch;
  flex: 1;
  margin-top: 12px;
}

/* 左侧得分列 */
.scores-column {
  flex: 0 0 160px;
  min-width: 160px;
  display: flex;
  flex-direction: column;
}

.scores-list {
  display: flex;
  flex-direction: column;
  justify-content: space-around;
  flex: 1;
  padding: 24px 16px;
}

.score-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 8px;
  transition: all 0.3s ease;
  flex: 1;
}

.score-item:hover {
  transform: translateY(-1px);
}

.score-label {
  font-size: 16px;
  color: #606266;
  font-weight: 500;
  text-align: center;
  line-height: 1.3;
}

.score-value {
  font-size: 26px;
  font-weight: 700;
  padding: 2px 4px;
  border-radius: 4px;
  min-width: 40px;
  text-align: center;
}

.score-value.excellent {
  color: #67c23a;
}

.score-value.good {
  color: #409eff;
}

.score-value.average {
  color: #e6a23c;
}

.score-value.poor {
  color: #f56c6c;
}

/* 右侧分析结果列 */
.analysis-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

/* 状态显示列 */
.status-column {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 16px;
}

.status-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 16px;
  text-align: center;
}

.status-icon {
  font-size: 20px;
  color: #409eff;
  animation: spin 2s linear infinite;
}

.status-text {
  font-weight: 500;
}

/* 占位列 */
.placeholder-column {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 20px;
}

.placeholder-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 16px;
  text-align: center;
}

.placeholder-icon {
  font-size: 20px;
  color: #409eff;
  animation: spin 2s linear infinite;
}


.analysis-content-box {
  padding: 12px 16px 20px 16px;
  flex: 1;
  overflow-y: auto;
  display: flex;
  align-items: flex-start;
  max-height: 280px;
}

.analysis-text {
  line-height: 1.6;
  color: #2c3e50;
  font-size: 14px;
  word-wrap: break-word;
  margin: 0;
  font-weight: 400;
}

/* Markdown内容样式 */
.analysis-text :deep(h1),
.analysis-text :deep(h2),
.analysis-text :deep(h3),
.analysis-text :deep(h4),
.analysis-text :deep(h5),
.analysis-text :deep(h6) {
  color: #1e293b;
  margin: 1em 0 0.5em 0;
  font-weight: 600;
}

.analysis-text :deep(h1) { font-size: 1.5em; }
.analysis-text :deep(h2) { font-size: 1.3em; }
.analysis-text :deep(h3) { font-size: 1.1em; }

.analysis-text :deep(p) {
  margin: 0.8em 0;
  line-height: 1.6;
}

.analysis-text :deep(ul),
.analysis-text :deep(ol) {
  margin: 0.8em 0;
  padding-left: 1.2em;
}

.analysis-text :deep(li) {
  margin: 0.3em 0;
  line-height: 1.5;
}

.analysis-text :deep(strong) {
  font-weight: 600;
  color: #1e293b;
}

.analysis-text :deep(em) {
  font-style: italic;
  color: #4b5563;
}

.analysis-text :deep(code) {
  background-color: #f3f4f6;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
}

.analysis-text :deep(blockquote) {
  border-left: 3px solid #A8B5A0;
  margin: 1em 0;
  padding-left: 1em;
  color: #6b7280;
  font-style: italic;
}

.analysis-text :deep(hr) {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 1.5em 0;
}

.analysis-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.analysis-icon {
  color: #409eff;
  font-size: 18px;
}

.analysis-header h4 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}









.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.no-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.fallback-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
  color: #606266;
}

.fallback-content {
  text-align: center;
  max-width: 400px;
}

.fallback-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 16px;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.fallback-content h4 {
  margin: 16px 0;
  font-size: 18px;
  color: #303133;
}

.fallback-content p {
  margin: 16px 0;
  line-height: 1.6;
}

.score-value.analyzing {
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
}

@media (max-width: 768px) {
  .chart-header {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
    margin-bottom: 12px;
    padding-bottom: 8px;
  }
  
  .content-layout {
    flex-direction: column;
    gap: 12px;
  }
  
  .scores-column {
    flex: none;
    min-width: 0;
  }
  
  .scores-list {
    flex-direction: row;
    justify-content: space-around;
    padding: 10px;
    gap: 8px;
  }
  
  .score-item {
    padding: 8px 4px;
    flex: 1;
    min-width: 80px;
  }
  
  .score-label {
    font-size: 10px;
  }
  
  .score-value {
    font-size: 14px;
  }
  
  .analysis-content-box {
    min-height: 120px;
    max-height: 200px;
    padding: 10px;
  }
  
  .analysis-text {
    font-size: 12px;
    line-height: 1.4;
  }
  
  .placeholder-column,
  .status-column {
    padding: 16px;
    height: auto;
    min-height: 100px;
  }
  
  .introduction-analysis-chart {
    padding: 16px;
    height: auto;
  }
}
</style>

