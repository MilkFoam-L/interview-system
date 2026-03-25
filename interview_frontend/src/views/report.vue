<template>
  <div class="interview-report-list">
    <!-- 顶部标题和功能区 -->
    <div class="report-header">
      <div class="header-left">
        <h2 class="main-title">
          <el-icon class="title-icon"><DataLine /></el-icon>
          面试报告中心
        </h2>
        <p class="sub-title">查看您的所有面试分析报告及数据统计</p>
      </div>
      <div class="header-actions">
      </div>
    </div>

    <!-- 主体内容区 -->
    <div class="report-main-container">
      <!-- 左侧主要列表 -->
      <div class="report-list-container">
        <el-card class="report-list-card" shadow="hover">
          <!-- 排序选项 -->
          <div class="filter-row">
            <div class="filter-item">
              <span class="filter-label">排序：</span>
              <div class="custom-radio-group">
                <div
                    class="custom-radio-btn"
                    :class="{ 'active': sortBy === 'newest' }"
                    @click="sortBy = 'newest'; handleSort()"
                >
                  <el-icon><Timer /></el-icon>
                  <span>最新优先</span>
                </div>
                <div
                    class="custom-radio-btn"
                    :class="{ 'active': sortBy === 'score' }"
                    @click="sortBy = 'score'; handleSort()"
                >
                  <el-icon><StarFilled /></el-icon>
                  <span>评分优先</span>
                </div>
              </div>
            </div>
            <div class="filter-actions">
              <div class="search-container">
                <el-input
                    v-model="searchQuery"
                    placeholder="搜索公司/职位"
                    class="search-input"
                    clearable
                    prefix-icon="Search"
                    @input="handleSearch"
                />
              </div>
              <el-dropdown @command="handleFilterCommand" trigger="click">
                <div class="filter-btn">
                  <el-icon><Filter /></el-icon>
                  <span>筛选</span>
                  <el-icon class="arrow-icon"><ArrowDown /></el-icon>
                </div>
                <template #dropdown>
                  <el-dropdown-menu class="custom-dropdown">
                    <el-dropdown-item command="all">
                      <el-icon><List /></el-icon>
                      <span>全部报告</span>
                    </el-dropdown-item>
                    <el-dropdown-item command="highScore">
                      <el-icon><StarFilled /></el-icon>
                      <span>高分报告 (80+)</span>
                    </el-dropdown-item>
                    <el-dropdown-item command="recent">
                      <el-icon><Calendar /></el-icon>
                      <span>最近一周</span>
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>

          <!-- 报告列表 -->
          <el-empty v-if="reportList.length === 0" description="暂无面试报告" />
          <div v-else class="report-items">
            <div
                v-for="report in reportList"
                :key="report.id"
                class="report-item"
                :class="{ 'report-item-active': selectedReportId === report.id }"
                @click="handleReportClick(report)"
            >
              <div class="report-item-main">
                <div class="report-info">
                  <div class="report-title">
                    {{ report.positionName || '未知职位' }}
                    <el-tag size="small" effect="plain" v-if="isRecentReport(report)" class="recent-tag">最新</el-tag>
                  </div>
                  <div class="report-company">{{ report.companyName || '未知公司' }}</div>
                  <div class="report-meta">
                    <el-icon><Timer /></el-icon>
                    <span>{{ formatDateTime(report.interviewTime) }}</span>
                    <el-divider direction="vertical" />
                    <el-icon><Clock /></el-icon>
                    <span>{{ report.duration }} 分钟</span>
                  </div>
                </div>
              </div>
              <div class="report-item-score">
                <div class="score-circle" :class="getScoreClass(report.totalScore)">
                  {{ Math.round(report.totalScore) }}
                </div>
                <div class="score-stars">
                  <el-rate
                      v-model="report.starRating"
                      disabled
                      :colors="['#FFD666', '#FFD666', '#FFD666']"
                      :max="5"
                      :allow-half="true"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                layout="pager"
                :total="totalReports"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                background
            />
          </div>
        </el-card>
      </div>

      <!-- 右侧统计信息 -->
      <div class="report-stats-container">
        <el-card class="stats-card summary-card" shadow="hover">
          <div class="stats-header">
            <el-icon class="stats-icon"><PieChart /></el-icon>
            <span>总体统计</span>
          </div>
          <div class="stats-content">
            <div class="stat-item">
              <span class="stat-label">总面试次数</span>
              <span class="stat-value">{{ stats.totalInterviews }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">平均得分</span>
              <span class="stat-value">{{ Math.round(stats.averageScore) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">
                高分面试
                <el-tooltip content="得分在80分及以上的面试次数" placement="top">
                  <el-icon class="info-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </span>
              <span class="stat-value">{{ stats.highScoreCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">面试总时长</span>
              <span class="stat-value">{{ formatTotalDuration(stats.totalDuration) }}</span>
            </div>
          </div>
        </el-card>

        <el-card class="stats-card recent-card" shadow="hover">
          <div class="stats-header">
            <el-icon class="stats-icon"><Calendar /></el-icon>
            <span>近期动态</span>
          </div>
          <div v-if="stats.lastInterview" class="stats-content">
            <div class="stat-item">
              <span class="stat-label">最近面试</span>
              <span class="stat-value date-value">{{ formatDate(stats.lastInterview.interviewTime) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">公司</span>
              <span class="stat-value text-ellipsis">{{ stats.lastInterview.companyName || '未知' }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">职位</span>
              <span class="stat-value text-ellipsis">{{ stats.lastInterview.positionName || '未知' }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">得分</span>
              <span class="stat-value score-value" :class="getScoreClass(stats.lastInterview.totalScore)">
                {{ Math.round(stats.lastInterview.totalScore) }}分
              </span>
            </div>
            <el-button
                v-if="stats.lastInterview"
                type="primary"
                size="small"
                class="view-recent-btn"
                @click="handleReportClick(stats.lastInterview)"
            >查看报告</el-button>
          </div>
          <el-empty v-else description="暂无面试记录" :image-size="80" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  DataLine, Search, Refresh, ArrowDown, Timer,
  Clock, PieChart, Calendar, TrendCharts, StarFilled, Filter, List, QuestionFilled
} from '@element-plus/icons-vue'
import { getReportList, getUserStats } from '@/api/comprehensiveReport'
import { getUserId } from '@/utils/auth'

const router = useRouter()

// 分页和搜索相关
const currentPage = ref(1)
const pageSize = ref(5)
const totalReports = ref(0)
const searchQuery = ref('')
const dateRange = ref([])
const sortBy = ref('newest')
const selectedReportId = ref(null)

// 面试报告列表
const reportList = ref([])

// 统计数据
const stats = reactive({
  totalInterviews: 0,
  averageScore: 0,
  highScoreCount: 0,
  totalDuration: 0,
  lastInterview: null
})

// 日期快捷选项
const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    },
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    },
  },
  {
    text: '最近三个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    },
  },
]

// 获取当前用户ID
const currentUserId = getUserId()

// 初始化加载报告列表
const loadReportList = async () => {
  try {
    if (!currentUserId) {
      console.error('未获取到用户信息，请重新登录')
      return
    }

    // 构建请求参数
    const params = {
      userId: currentUserId,
      page: currentPage.value - 1,
      size: pageSize.value,
      sortBy: sortBy.value
    }

    // 添加搜索关键词
    if (searchQuery.value && searchQuery.value.trim()) {
      params.searchKeyword = searchQuery.value.trim()
    }

    // 添加日期范围筛选
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0].toISOString()
      params.endTime = dateRange.value[1].toISOString()
    }

    // 添加筛选类型
    if (currentFilterType.value) {
      params.filterType = currentFilterType.value
    }

    console.log('请求参数:', params)

    const response = await getReportList(params)

    if (response.code === 0) {
      const data = response.data
      reportList.value = data.content || []
      totalReports.value = data.totalElements || 0

      console.log('获取到报告列表:', reportList.value)
    } else {
      console.error(response.msg || '获取报告列表失败')
      reportList.value = []
    }

    // 同时加载统计数据
    await loadUserStats()

  } catch (error) {
    console.error('获取报告列表失败:', error)
    console.error('获取报告列表失败，请稍后重试')
    reportList.value = []
  }
}

// 加载用户统计数据
const loadUserStats = async () => {
  try {
    if (!currentUserId) {
      return
    }

    const response = await getUserStats(currentUserId)

    if (response.code === 0) {
      const data = response.data
      stats.totalInterviews = data.totalInterviews || 0
      stats.averageScore = data.averageScore || 0
      stats.highScoreCount = data.highScoreCount || 0
      stats.totalDuration = data.totalDuration || 0
      stats.lastInterview = data.lastInterview || null

      console.log('获取到统计数据:', data)
    } else {
      console.error('获取统计数据失败:', response.msg)
    }

  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

// 点击报告跳转到详情页
const handleReportClick = (report) => {
  selectedReportId.value = report.id
  router.push({
    path: '/interview-analysis-report',
    query: {
      sessionId: report.sessionId,
      reportId: report.id
    }
  })
}

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1
  loadReportList()
}

// 处理日期变化
const handleDateChange = () => {
  currentPage.value = 1
  loadReportList()
}

// 处理排序
const handleSort = () => {
  loadReportList()
}

// 当前筛选类型
const currentFilterType = ref('')

// 处理筛选命令
const handleFilterCommand = (command) => {
  if (command === 'all') {
    dateRange.value = []
    searchQuery.value = ''
    currentFilterType.value = ''
  } else if (command === 'highScore') {
    searchQuery.value = ''
    dateRange.value = []
    currentFilterType.value = 'highScore'
  } else if (command === 'recent') {
    const end = new Date()
    const start = new Date()
    start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
    dateRange.value = [start, end]
    currentFilterType.value = 'recent'
  }

  currentPage.value = 1
  loadReportList()
}

// 处理页码变化
const handleCurrentChange = (page) => {
  currentPage.value = page
  loadReportList()
}

// 处理每页条数变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadReportList()
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

// 格式化日期
const formatShortDate = (dateTime) => {
  if (!dateTime) return '未知'
  const date = new Date(dateTime)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 格式化日期
const formatDate = (dateTime) => {
  if (!dateTime) return '未知'
  const date = new Date(dateTime)
  return date.toLocaleDateString('zh-CN')
}

// 格式化总时长
const formatTotalDuration = (minutes) => {
  if (!minutes) return '0分钟'
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  const remainingMinutes = minutes % 60
  return `${hours}小时${remainingMinutes > 0 ? ` ${remainingMinutes}分钟` : ''}`
}

// 判断是否是最新的报告
const isRecentReport = (report) => {
  if (!reportList.value || reportList.value.length === 0) return false

  // 找到最新的报告
  const latestReport = reportList.value.reduce((latest, current) => {
    const latestTime = new Date(latest.interviewTime).getTime()
    const currentTime = new Date(current.interviewTime).getTime()
    return currentTime > latestTime ? current : latest
  })

  // 判断当前报告是否是最新的
  return report.id === latestReport.id
}

// 获取得分对应的CSS类
const getScoreClass = (score) => {
  if (score >= 90) return 'score-excellent'
  if (score >= 80) return 'score-good'
  if (score >= 70) return 'score-fair'
  return 'score-poor'
}

// 页面加载时获取报告列表
onMounted(() => {
  console.log('当前用户ID:', currentUserId)
  if (currentUserId) {
    loadReportList()
  } else {
    console.error('未获取到用户信息，请重新登录')
  }
})
</script>

<style scoped>
.interview-report-list {
  padding: 32px;
  min-height: 100vh;
  background: transparent;
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

@keyframes bounceIn {
  0% {
    opacity: 0;
    transform: scale(0.3);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
  70% {
    transform: scale(0.9);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

/* 顶部区域 - 优化布局和美感 */
.report-header {
  margin-bottom: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 24px 0;
  position: relative;
}

.report-header::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.8), rgba(255, 255, 255, 0.6), rgba(255, 255, 255, 0.4));
  border-radius: 1px;
  opacity: 0.8;
  box-shadow: 0 1px 4px rgba(255, 255, 255, 0.3);
}

.header-left {
  display: flex;
  flex-direction: column;
  padding-right: 12px;
}

.main-title {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  background-clip: text;
  -webkit-background-clip: text;
  color: #ffffff;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  letter-spacing: -0.5px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3), 0 1px 4px rgba(0, 0, 0, 0.2);
}

.title-icon {
  color: #ffffff;
  font-size: 32px;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.4));
  animation: bounceIn 0.8s ease-out 0.2s both;
}

.sub-title {
  margin: 12px 0 0 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  font-weight: 500;
  line-height: 1.5;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

/* 搜索框样式优化 */
.search-container {
  position: relative;
  margin-right: 12px;
}

.search-input {
  width: 200px;
}

:deep(.search-input .el-input__wrapper) {
  border-radius: 28px;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.02);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.search-input .el-input__wrapper:hover) {
  box-shadow: 0 4px 20px rgba(69, 123, 157, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  border-color: rgba(69, 123, 157, 0.3);
  transform: translateY(-1px);
}

:deep(.search-input .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(69, 123, 157, 0.4), 0 4px 20px rgba(69, 123, 157, 0.15);
  border-color: transparent;
  transform: translateY(-2px);
}

:deep(.search-input .el-input__inner) {
  color: #333;
  font-weight: 500;
}

:deep(.search-input .el-input__prefix-icon) {
  color: #457B9D;
}

/* 主体内容布局 */
.report-main-container {
  display: flex;
  gap: 32px;
}

.report-list-container {
  flex: 1;
}

.report-stats-container {
  width: 360px;
}

/* 列表卡片 */
.report-list-card {
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: #ffffff;
  box-shadow: 0 4px 28px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.02);
  animation: slideInUp 0.6s ease-out 0.1s both;
}

/* 排序筛选区优化 */
.filter-row {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid #e2e8f0;
  background: linear-gradient(135deg, #f8fafc 0%, #ffffff 100%);
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.04);
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.filter-label {
  font-weight: 600;
  color: #475569;
  font-size: 16px;
}

/* 自定义单选按钮组 */
.custom-radio-group {
  display: flex;
  gap: 12px;
}

.custom-radio-btn {
  padding: 10px 18px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(0, 0, 0, 0.02);
}

.custom-radio-btn:hover {
  border-color: #457B9D;
  color: #457B9D;
  background: #f8fafc;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(69, 123, 157, 0.15), 0 2px 4px rgba(0, 0, 0, 0.08);
}

.custom-radio-btn.active {
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%);
  color: #fff;
  border: 1px solid transparent;
  box-shadow: 0 4px 16px rgba(69, 123, 157, 0.25), 0 2px 4px rgba(38, 70, 83, 0.1);
  transform: translateY(-2px);
}

/* 筛选按钮样式优化 */
.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-btn {
  padding: 10px 18px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #457B9D;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(0, 0, 0, 0.02);
}

.filter-btn:hover {
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%);
  color: #fff;
  border-color: transparent;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(69, 123, 157, 0.25), 0 2px 4px rgba(38, 70, 83, 0.1);
}

.arrow-icon {
  transition: transform 0.3s ease;
  font-size: 12px;
}

.filter-btn:hover .arrow-icon {
  transform: rotate(180deg);
}

/* 自定义下拉菜单样式 */
:deep(.custom-dropdown .el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  padding: 10px 20px;
  transition: all 0.2s ease;
}

:deep(.custom-dropdown .el-dropdown-menu__item:hover) {
  background-color: #f0f7ff;
  color: #457B9D;
}

:deep(.custom-dropdown .el-dropdown-menu__item .el-icon) {
  color: #457B9D;
}

/* 报告列表项 - 优化样式 */
.report-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.report-item {
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  border-radius: 20px;
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.02);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  border: 1px solid rgba(226, 232, 240, 0.8);
  margin-bottom: 0;
  overflow: hidden;
}

.report-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #457B9D 0%, #264653 50%, #06b6d4 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.report-item:hover {
  box-shadow: 0 12px 32px rgba(69, 123, 157, 0.15), 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-6px);
  border-color: rgba(69, 123, 157, 0.3);
  background: #ffffff;
}

.report-item:hover::before {
  opacity: 1;
}

.report-item-active {
  border-color: rgba(69, 123, 157, 0.6);
  background: linear-gradient(135deg, rgba(69, 123, 157, 0.04) 0%, rgba(38, 70, 83, 0.02) 100%);
  box-shadow: 0 8px 24px rgba(69, 123, 157, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.report-item-active::before {
  opacity: 1;
}

.report-item-main {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.report-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.report-title {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 12px;
  line-height: 1.3;
  letter-spacing: -0.3px;
}

.recent-tag {
  margin-left: 8px;
  animation: pulse 2s infinite;
  background: linear-gradient(135deg, #10b981, #059669);
  color: white;
  border: none;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.9;
  }
}

.report-company {
  font-size: 16px;
  color: #64748b;
  margin-top: 4px;
  font-weight: 500;
}

.report-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
  color: #94a3b8;
  margin-top: 8px;
}

.report-meta .el-icon {
  font-size: 15px;
}

.report-item-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-width: 80px;
}

.score-circle {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  background-color: #f8fafc;
  color: #475569;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08), inset 0 1px 0 rgba(255, 255, 255, 0.6);
  border: 2px solid transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.score-circle::before {
  content: '';
  position: absolute;
  top: 2px;
  right: 2px;
  bottom: 2px;
  left: 2px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(69, 123, 157, 0.1), rgba(38, 70, 83, 0.1));
  border: 1px solid rgba(69, 123, 157, 0.1);
}

.score-excellent {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.15) 0%, rgba(34, 197, 94, 0.12) 100%);
  color: #059669;
  border-color: rgba(34, 197, 94, 0.2);
  box-shadow: 0 4px 20px rgba(34, 197, 94, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.score-good {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(59, 130, 246, 0.12) 100%);
  color: #2563eb;
  border-color: rgba(59, 130, 246, 0.2);
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.score-fair {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15) 0%, rgba(245, 158, 11, 0.12) 100%);
  color: #d97706;
  border-color: rgba(245, 158, 11, 0.2);
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.score-poor {
  background: linear-gradient(135deg, rgba(239, 68, 68, 0.15) 0%, rgba(239, 68, 68, 0.12) 100%);
  color: #dc2626;
  border-color: rgba(239, 68, 68, 0.2);
  box-shadow: 0 4px 20px rgba(239, 68, 68, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.pagination-container {
  margin: 16px 0 0 0;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  animation: slideInUp 0.6s ease-out 0.2s both;
}

:deep(.pagination-container .el-pagination) {
  font-weight: 600;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
  padding: 0;
  gap: 8px 0;
}

:deep(.pagination-container .el-pager) {
  display: flex;
  flex-wrap: wrap;
  row-gap: 8px;
}

:deep(.pagination-container .el-pager li) {
  background: rgba(168, 218, 220, 0.15);
  border: 1px solid rgba(168, 218, 220, 0.3);
  color: #457B9D;
  margin: 0 4px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.pagination-container .el-pager li:hover) {
  background: #457B9D;
  color: #F1FAEE;
  transform: scale(1.1);
}

:deep(.pagination-container .el-pager li.is-active) {
  background: #457B9D;
  color: #F1FAEE;
  box-shadow: 0 4px 12px rgba(69, 123, 157, 0.3);
}

/* 统计卡片 - 简洁样式 */
.stats-card {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}



.stats-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: #f8fafc;
}

/* 移除template #header默认样式 */
:deep(.el-card__header) {
  display: none;
}

.stats-icon {
  color: #457B9D;
  font-size: 18px;
}

.stats-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
}

.stat-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-icon {
  font-size: 14px;
  color: #94a3b8;
  cursor: help;
  transition: color 0.2s ease;
}

.info-icon:hover {
  color: #457B9D;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.score-value {
  font-size: 16px;
}

.date-value {
  font-size: 14px;
}

.text-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.view-recent-btn {
  margin-top: 16px;
  align-self: flex-end;
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border: none !important;
  color: #F1FAEE !important;
  border-radius: 16px !important;
  padding: 8px 16px !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 2px 8px rgba(38, 70, 83, 0.25) !important;
  letter-spacing: 0.3px;
}

.view-recent-btn:hover {
  transform: translateY(-1px) scale(1.02) !important;
  box-shadow: 0 4px 12px rgba(38, 70, 83, 0.35) !important;
  background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important;
}

.view-recent-btn:active {
  transform: translateY(0) scale(1) !important;
  box-shadow: 0 1px 4px rgba(38, 70, 83, 0.2) !important;
}

@media (max-width: 1100px) {
  .interview-report-list {
    padding: 20px;
  }

  .report-main-container {
    flex-direction: column-reverse;
    gap: 24px;
  }

  .report-stats-container {
    width: 100%;
    display: flex;
    gap: 20px;
  }

  .stats-card {
    flex: 1;
    margin-bottom: 0;
  }

  .report-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
    padding: 20px 0;
    margin-bottom: 24px;
  }

  .main-title {
    font-size: 28px;
  }

  .sub-title {
    font-size: 15px;
  }

  .header-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .filter-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
    padding: 20px;
  }

  .filter-actions {
    align-self: flex-end;
    width: 100%;
    justify-content: flex-end;
  }

  .custom-radio-group {
    flex-wrap: wrap;
    gap: 8px;
  }

  .search-container {
    margin-right: 0;
  }

  .search-input {
    width: 100%;
    max-width: 280px;
  }

  .report-item {
    padding: 20px;
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    text-align: left;
  }

  .report-item-main {
    width: 100%;
  }

  .report-item-score {
    align-self: flex-end;
    min-width: auto;
  }
}

@media (max-width: 640px) {
  .interview-report-list {
    padding: 16px;
  }

  .report-stats-container {
    flex-direction: column;
    gap: 16px;
  }

  .stats-card {
    flex: none;
  }

  .custom-radio-btn, .filter-btn {
    padding: 8px 14px;
    font-size: 13px;
  }

  .score-circle {
    width: 64px;
    height: 64px;
    font-size: 20px;
  }
}
</style> 