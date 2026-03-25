<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="题目使用统计"
    width="900px"
    :before-close="handleClose">
    
    <div v-loading="loading" class="statistics-container">
      <!-- 总体统计 -->
      <div class="overview-section">
        <h3>总体概况</h3>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-value">{{ overview.totalQuestions }}</div>
                <div class="stat-label">题目总数</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-value active">{{ overview.activeQuestions }}</div>
                <div class="stat-label">启用题目</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-value used">{{ overview.usedQuestions }}</div>
                <div class="stat-label">已使用题目</div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-value usage">{{ overview.totalUsage }}</div>
                <div class="stat-label">总使用次数</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      
      <!-- 分类统计 -->
      <div class="category-section">
        <h3>分类统计</h3>
        <el-tabs v-model="activeTab">
          <el-tab-pane label="按大类别" name="category">
            <div class="chart-container">
              <div ref="categoryChartRef" style="width: 100%; height: 300px;"></div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="按细分类型" name="categoryType">
            <div class="chart-container">
              <div ref="typeChartRef" style="width: 100%; height: 300px;"></div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="按题目类型" name="questionType">
            <div class="chart-container">
              <div ref="questionTypeChartRef" style="width: 100%; height: 300px;"></div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
      
      <!-- 使用排行 -->
      <div class="ranking-section">
        <h3>使用排行榜</h3>
        <el-table :data="topUsedQuestions" style="width: 100%">
          <el-table-column prop="rank" label="排名" width="80" />
          <el-table-column prop="content" label="题目内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getTypeTagType(row.type)">{{ getTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="category" label="大类别" width="120" />
          <el-table-column prop="categoryType" label="细分类型" width="120" />
          <el-table-column prop="usageCount" label="使用次数" width="100" />
        </el-table>
      </div>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="refreshData">刷新数据</el-button>
        <el-button @click="handleClose">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getQuestionStatistics } from '../../../../api/interviewer/questions'

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['update:visible'])

const loading = ref(false)
const activeTab = ref('category')

// 图表实例
const categoryChartRef = ref()
const typeChartRef = ref()
const questionTypeChartRef = ref()
let categoryChart = null
let typeChart = null
let questionTypeChart = null

// 统计数据
const overview = reactive({
  totalQuestions: 0,
  activeQuestions: 0,
  usedQuestions: 0,
  totalUsage: 0
})

const categoryStats = ref([])
const typeStats = ref([])
const questionTypeStats = ref([])
const topUsedQuestions = ref([])

// 监听对话框显示
watch(() => props.visible, (newVal) => {
  if (newVal) {
    loadStatistics()
  }
})

// 监听标签切换
watch(activeTab, (newTab) => {
  if (props.visible) {
    nextTick(() => {
      initCharts()
    })
  }
})

async function loadStatistics() {
  loading.value = true
  try {
    const data = await getQuestionStatistics()
    
    // 更新概况数据
    Object.assign(overview, {
      totalQuestions: data.overview?.totalQuestions || 0,
      activeQuestions: data.overview?.activeQuestions || 0,
      usedQuestions: data.overview?.usedQuestions || 0,
      totalUsage: data.overview?.totalUsage || 0
    })
    
    // 更新分类统计
    categoryStats.value = data.categoryStats || []
    typeStats.value = data.typeStats || []
    questionTypeStats.value = data.questionTypeStats || []
    
    // 更新排行榜
    const ranking = data.topUsedQuestions || []
    topUsedQuestions.value = ranking.map((item, index) => ({
      ...item,
      rank: index + 1
    }))
    
    // 初始化图表
    nextTick(() => {
      initCharts()
    })
    
  } catch (error) {
    console.error('加载统计数据失败:', error)
    console.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

function initCharts() {
  // 这里使用简单的模拟图表，实际项目中可以使用 ECharts 等图表库
  // 由于这是演示代码，我们创建简单的文本显示
  
  if (activeTab.value === 'category' && categoryChartRef.value) {
    initCategoryChart()
  } else if (activeTab.value === 'categoryType' && typeChartRef.value) {
    initTypeChart()
  } else if (activeTab.value === 'questionType' && questionTypeChartRef.value) {
    initQuestionTypeChart()
  }
}

function initCategoryChart() {
  if (categoryChartRef.value) {
    categoryChartRef.value.innerHTML = generateChartHTML(categoryStats.value, '大类别')
  }
}

function initTypeChart() {
  if (typeChartRef.value) {
    typeChartRef.value.innerHTML = generateChartHTML(typeStats.value, '细分类型')
  }
}

function initQuestionTypeChart() {
  if (questionTypeChartRef.value) {
    questionTypeChartRef.value.innerHTML = generateChartHTML(questionTypeStats.value, '题目类型')
  }
}

function generateChartHTML(data, title) {
  if (!data || data.length === 0) {
    return `<div style="text-align: center; padding: 50px; color: #909399;">暂无${title}统计数据</div>`
  }
  
  const maxCount = Math.max(...data.map(item => item.count))
  
  return `
    <div style="padding: 20px;">
      <div style="margin-bottom: 20px; font-weight: 600; color: #303133;">${title}分布</div>
      ${data.map(item => `
        <div style="margin-bottom: 12px;">
          <div style="display: flex; justify-content: space-between; margin-bottom: 4px;">
            <span style="color: #303133;">${item.name}</span>
            <span style="color: #606266;">${item.count}个 (${((item.count / (data.reduce((sum, d) => sum + d.count, 0)) * 100)).toFixed(1)}%)</span>
          </div>
          <div style="background-color: #f0f2f5; height: 8px; border-radius: 4px; overflow: hidden;">
            <div style="width: ${(item.count / maxCount * 100)}%; height: 100%; background-color: #409eff; transition: width 0.3s ease;"></div>
          </div>
        </div>
      `).join('')}
    </div>
  `
}

function refreshData() {
  loadStatistics()
}

function handleClose() {
  emit('update:visible', false)
}

// 暴露方法供父组件调用
defineExpose({
  refreshData
})

function getTypeLabel(type) {
  const typeMap = {
    'basic': '基础题',
    'code': '编程题',
    'algorithm': '算法题',
    'system_design': '系统设计题',
    'project': '项目经验题',
    'behavioral': '行为面试题'
  }
  return typeMap[type] || type
}

function getTypeTagType(type) {
  const typeMap = {
    'basic': 'primary',
    'code': 'success',
    'algorithm': 'warning',
    'system_design': 'danger',
    'project': 'info',
    'behavioral': 'primary'
  }
  return typeMap[type] || 'default'
}

// 清理资源
onUnmounted(() => {
  if (categoryChart) categoryChart.dispose()
  if (typeChart) typeChart.dispose()
  if (questionTypeChart) questionTypeChart.dispose()
})
</script>

<style scoped>
.statistics-container {
  padding: 20px 0;
}

.overview-section,
.category-section,
.ranking-section {
  margin-bottom: 32px;
}

.overview-section h3,
.category-section h3,
.ranking-section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}

.stat-card {
  text-align: center;
}

.stat-item {
  padding: 12px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.stat-value.active {
  color: #67c23a;
}

.stat-value.used {
  color: #409eff;
}

.stat-value.usage {
  color: #e6a23c;
}

.stat-label {
  font-size: 14px;
  color: #606266;
}

.chart-container {
  min-height: 300px;
  background-color: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-card__body) {
  padding: 16px;
}

:deep(.el-tabs__content) {
  padding: 0;
}
</style>