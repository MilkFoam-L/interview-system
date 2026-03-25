<template>
  <div class="recruitment-analysis">
    <div class="page-header">
            <div class="header-left">
        <h1 class="main-title">招聘数据分析</h1>
        <p class="sub-title">全面洞察招聘流程，优化人才获取策略</p>
    </div>
            <div class="header-actions">
        <el-select 
          v-model="timeRange" 
          size="default" 
          class="custom-select"
          style="width: 140px"
        >
                  <el-option label="最近7天" value="7days" />
                  <el-option label="最近30天" value="30days" />
                  <el-option label="最近90天" value="90days" />
                  <el-option label="本年度" value="thisYear" />
                </el-select>
        <el-button 
          type="primary" 
          :icon="RefreshRight" 
          @click="refreshData"
          class="refresh-btn"
        >
          刷新数据
        </el-button>
              </div>
                </div>

    <div class="main-content">
      <div class="main-area">
        <div class="kpi-cards-row">
          <div class="kpi-card">
            <div class="kpi-icon-wrapper total">
              <el-icon class="kpi-icon"><Document /></el-icon>
                </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ stats.totalApplications }}</div>
              <div class="kpi-label">总投递简历数</div>
              <div class="kpi-trend up">
                <el-icon><TrendCharts /></el-icon>
                <span>+{{ stats.applicationGrowth }}%</span>
                </div>
                </div>
                </div>
          
          <div class="kpi-card">
            <div class="kpi-icon-wrapper jobs">
              <el-icon class="kpi-icon"><Briefcase /></el-icon>
              </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ stats.publishedJobs }}</div>
              <div class="kpi-label">已发布岗位数量</div>
              <div class="kpi-trend stable">
                <el-icon><Minus /></el-icon>
                <span>{{ stats.jobsGrowth }}%</span>
            </div>
              </div>
              </div>
          
          <div class="kpi-card">
            <div class="kpi-icon-wrapper interviewed">
              <el-icon class="kpi-icon"><User /></el-icon>
              </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ stats.totalInterviewed }}</div>
              <div class="kpi-label">已面试总人数</div>
              <div class="kpi-trend up">
                <el-icon><TrendCharts /></el-icon>
                <span>+{{ stats.interviewGrowth }}%</span>
                  </div>
                </div>
                  </div>
          
          <div class="kpi-card">
            <div class="kpi-icon-wrapper weekly">
              <el-icon class="kpi-icon"><Calendar /></el-icon>
                </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ stats.weeklyApplications }}</div>
              <div class="kpi-label">本周新增投递简历数</div>
              <div class="kpi-trend up">
                <el-icon><TrendCharts /></el-icon>
                <span>+{{ stats.weeklyGrowth }}%</span>
                  </div>
                </div>
                  </div>
                </div>

        <div class="chart-container timeline-chart">
          <div class="chart-header">
            <h3>简历投递数量分析</h3>
                         <div class="chart-controls">
               <el-radio-group v-model="chartDisplayType" size="small" class="custom-radio-group">
                 <el-radio-button label="all">全部数据</el-radio-button>
                 <el-radio-button label="top3">热门岗位Top3</el-radio-button>
               </el-radio-group>
                  </div>
                </div>
          <div ref="timelineChartRef" class="chart-content"></div>
              </div>

        <div class="bottom-charts-row">
          <div class="chart-container funnel-chart">
            <div class="chart-header">
              <h3>招聘漏斗分析</h3>
              <el-dropdown @command="handleFunnelPeriodChange" trigger="click">
                <div class="filter-btn chart-filter-btn">
                  <span>{{ getFunnelPeriodLabel(funnelPeriod) }}</span>
                  <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </div>
                <template #dropdown>
                  <el-dropdown-menu class="custom-chart-dropdown">
                    <el-dropdown-item command="total">总</el-dropdown-item>
                    <el-dropdown-item command="year">年</el-dropdown-item>
                    <el-dropdown-item command="quarter">季</el-dropdown-item>
                    <el-dropdown-item command="month">月</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              </div>
            <div ref="funnelChartRef" class="chart-content"></div>
              </div>

          <div class="chart-container heat-chart">
            <div class="chart-header">
              <h3>岗位招聘热度</h3>
              <el-dropdown @command="handleHeatPeriodChange" trigger="click">
                <div class="filter-btn chart-filter-btn">
                  <span>{{ getHeatPeriodLabel(heatPeriod) }}</span>
                  <el-icon class="arrow-icon"><ArrowDown /></el-icon>
              </div>
                <template #dropdown>
                  <el-dropdown-menu class="custom-chart-dropdown">
                    <el-dropdown-item command="total">总</el-dropdown-item>
                    <el-dropdown-item command="year">年</el-dropdown-item>
                    <el-dropdown-item command="quarter">季</el-dropdown-item>
                    <el-dropdown-item command="month">月</el-dropdown-item>
                  </el-dropdown-menu>
            </template>
              </el-dropdown>
                    </div>
            <div ref="heatChartRef" class="chart-content"></div>
                    </div>
                    </div>
                  </div>

      <div class="sidebar">
         <div class="sidebar-card">
           <div class="sidebar-header">
             <el-icon class="sidebar-icon"><Calendar /></el-icon>
             <span>近期面试数据</span>
                </div>
           <div class="sidebar-content">
             <div class="recent-stat">
               <div class="stat-content">
                 <div class="stat-icon jobs">
                   <el-icon><Briefcase /></el-icon>
                    </div>
                 <div class="stat-label">本季度发布岗位数</div>
                    </div>
               <div class="stat-number">{{ recentStats.interviews }}</div>
                    </div>
             <div class="recent-stat">
               <div class="stat-content">
                 <div class="stat-icon interviews">
                   <el-icon><User /></el-icon>
                  </div>
                 <div class="stat-label">本季度面试人数</div>
                </div>
               <div class="stat-number">{{ recentStats.passed }}</div>
                    </div>
                    </div>
                    </div>

        <div class="sidebar-card">
          <div class="sidebar-header">
            <el-icon class="sidebar-icon"><TrendCharts /></el-icon>
            <span>热门投递岗位</span>
                  </div>
          <div class="sidebar-content">
            <div class="job-list">
              <div 
                v-for="(job, index) in recentJobs" 
                :key="job.id"
                class="job-item"
                @click="navigateToJobDetail(job.id)"
              >
                <div class="job-content">
                  <div class="job-rank" :class="`rank-${index + 1}`">
                    <span class="rank-number">{{ index + 1 }}</span>
                </div>
                  <div class="job-info">
                    <div class="job-title">{{ job.title }}</div>
                    <div class="job-count">{{ job.applicationCount }} 人投递</div>
                    </div>
                    </div>
                <div class="job-action">
                  <el-icon class="job-arrow"><ArrowRight /></el-icon>
                    </div>
                  </div>
                </div>
              </div>
            </div>

        <div class="sidebar-card warning-card">
          <div class="sidebar-header">
            <el-icon class="sidebar-icon warning"><Warning /></el-icon>
            <span>待完善信息</span>
                </div>
          <div class="sidebar-content">
            <div class="warning-content">
              <p>您的公司信息还不够完善，完善企业信息有助于提升候选人信任度</p>
              <el-button 
                type="warning" 
                size="small" 
                @click="navigateToCompanyCenter"
                class="complete-btn"
              >
                前往完善
              </el-button>
              </div>
                  </div>
                  </div>
                  </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  RefreshRight, Document, Briefcase, User, Calendar,
  TrendCharts, Minus, ArrowRight, Warning
} from '@element-plus/icons-vue'

const router = useRouter()

const timeRange = ref('30days')
const chartDisplayType = ref('all')
const funnelPeriod = ref('total')
const heatPeriod = ref('total')

const timelineChartRef = ref<HTMLElement>()
const funnelChartRef = ref<HTMLElement>()
const heatChartRef = ref<HTMLElement>()

let timelineChart: echarts.ECharts | null = null
let funnelChart: echarts.ECharts | null = null
let heatChart: echarts.ECharts | null = null

// KPI 统计数据
const stats = reactive({
  totalApplications: 1285,
  applicationGrowth: 12.5,
  publishedJobs: 45,
  jobsGrowth: 0,
  totalInterviewed: 432,
  interviewGrowth: 8.3,
  weeklyApplications: 186,
  weeklyGrowth: 15.2
})

// 近期统计数据
const recentStats = reactive({
  interviews: 89,
  passed: 34,
  passRate: 38.2
})

// 近期热门岗位
const recentJobs = ref([
  { id: 1, title: '前端开发工程师', applicationCount: 89 },
  { id: 2, title: '后端开发工程师', applicationCount: 76 },
  { id: 3, title: 'UI设计师', applicationCount: 65 }
])

// 进度条颜色配置
const progressColors = [
  { color: '#67C23A', percentage: 80 },
  { color: '#E6A23C', percentage: 60 },
  { color: '#F56C6C', percentage: 40 },
  { color: '#909399', percentage: 20 }
]

// 格式化百分比
const formatPercentage = (percentage: number) => {
  return `${percentage}%`
}

// 初始化时间线图表
const initTimelineChart = () => {
  if (!timelineChartRef.value) return
  
  timelineChart = echarts.init(timelineChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#457B9D',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      }
    },
    legend: {
      data: chartDisplayType.value === 'all' ? ['总投递数'] : ['前端开发', '后端开发', 'UI设计'],
      top: 20,
      textStyle: {
        color: '#64748b'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['1月', '2月', '3月', '4月', '5月', '6月'],
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      },
      axisLabel: {
        color: '#64748b'
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: '#e2e8f0'
        }
      },
      axisLabel: {
        color: '#64748b'
      },
      splitLine: {
        lineStyle: {
          color: '#f1f5f9'
        }
      }
    },
    series: chartDisplayType.value === 'all' ? [{
      name: '总投递数',
      type: 'line',
      smooth: true,
      lineStyle: {
        color: '#457B9D',
        width: 3
      },
      itemStyle: {
        color: '#457B9D'
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(69, 123, 157, 0.3)' },
          { offset: 1, color: 'rgba(69, 123, 157, 0.05)' }
        ])
      },
      data: [120, 180, 140, 220, 200, 250]
    }] : [
      {
        name: '前端开发',
        type: 'line',
        smooth: true,
        lineStyle: { color: '#457B9D', width: 2 },
        itemStyle: { color: '#457B9D' },
        data: [45, 67, 52, 89, 76, 95]
      },
      {
        name: '后端开发',
        type: 'line',
        smooth: true,
        lineStyle: { color: '#E74C3C', width: 2 },
        itemStyle: { color: '#E74C3C' },
        data: [38, 55, 44, 76, 62, 78]
      },
      {
        name: 'UI设计',
        type: 'line',
        smooth: true,
        lineStyle: { color: '#F39C12', width: 2 },
        itemStyle: { color: '#F39C12' },
        data: [28, 42, 33, 65, 48, 58]
      }
    ]
  }
  
  timelineChart.setOption(option)
}

// 初始化漏斗图表
const initFunnelChart = () => {
  if (!funnelChartRef.value) return
  
  funnelChart = echarts.init(funnelChartRef.value)
  
  const funnelData = [
    { name: '简历投递', value: 356, color: '#AFC7D9' },
    { name: '已预约面试时间', value: 290, color: '#CFE3D2' },
    { name: '面试完成', value: 205, color: '#F1C9BB' },
    { name: '已发送通知', value: 126, color: '#D9E1E8' }
  ]
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: function(params) {
        const data = funnelData[params.dataIndex]
        return `${data.name}<br/>人数: ${data.value}`
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#457B9D',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      }
    },
    series: [{
      type: 'custom',
      coordinateSystem: 'none',
      renderItem: function(params, api) {
        const categoryIndex = api.value(0)
        const data = funnelData[categoryIndex]
        const total = funnelData.length

        const containerW = api.getWidth()
        const containerH = api.getHeight()
        const paddingTop = 30
        const paddingBottom = 30
        const availableH = containerH - paddingTop - paddingBottom
        const stageH = Math.min(70, availableH / total - 8)
        const gap = 8
        const totalStageH = total * stageH + (total - 1) * gap
        const startY = paddingTop + (availableH - totalStageH) / 2
        
        const y = startY + categoryIndex * (stageH + gap)

        const maxW = containerW * 0.88
        const minW = containerW * 0.28
        const widthRatio = 1 - (categoryIndex / (total - 1)) * 0.6
        const stageW = maxW * widthRatio
        const x = (containerW - stageW) / 2
        
        return {
          type: 'group',
          children: [
            // 漏斗阶段主体矩形
            {
              type: 'rect',
              shape: {
                x: x,
                y: y,
                width: stageW,
                height: stageH,
                r: 8
              },
              style: {
                fill: data.color,
                opacity: 0.95,
                stroke: '#ffffff',
                lineWidth: 2,
                shadowColor: 'rgba(0, 0, 0, 0.1)',
                shadowBlur: 8,
                shadowOffsetY: 4
              }
            },
            // 内部高光效果
            {
              type: 'rect',
              shape: {
                x: x + 2,
                y: y + 2,
                width: stageW - 4,
                height: stageH / 3,
                r: 6
              },
              style: {
                fill: 'rgba(255, 255, 255, 0.15)',
                opacity: 1
              }
            },
            // 阶段名称
            {
              type: 'text',
              style: {
                x: x + 18,
                y: y + stageH / 2 - 6,
                text: data.name,
                textFill: '#2d3748',
                fontSize: 15,
                fontWeight: '600',
                textVerticalAlign: 'middle',
                textShadowColor: 'rgba(255, 255, 255, 0.5)',
                textShadowBlur: 1,
                textShadowOffsetY: 1
              }
            },
            // 数值
            {
              type: 'text',
              style: {
                x: x + stageW - 18,
                y: y + stageH / 2 + 6,
                text: String(data.value),
                textFill: '#1a202c',
                fontSize: 18,
                fontWeight: '700',
                textAlign: 'right',
                textVerticalAlign: 'middle',
                textShadowColor: 'rgba(255, 255, 255, 0.6)',
                textShadowBlur: 1,
                textShadowOffsetY: 1
              }
            },
            // 左侧装饰线
            {
              type: 'rect',
              shape: {
                x: x,
                y: y + stageH * 0.2,
                width: 4,
                height: stageH * 0.6,
                r: 2
              },
              style: {
                fill: 'rgba(45, 55, 72, 0.3)',
                opacity: 1
              }
            }
          ]
        }
      },
      data: funnelData.map((item, index) => [index, item.value])
    }]
  }
  
  funnelChart.setOption(option)
}

// 初始化热度图表
const initHeatChart = () => {
  if (!heatChartRef.value) return
  
  heatChart = echarts.init(heatChartRef.value)
  
  const positionData = [
    { name: '前端开发工程师', resumes: 89, interviews: 32, positions: 3, percentage: 89, color: '#AFC7D9' },
    { name: '后端开发工程师', resumes: 76, interviews: 28, positions: 4, percentage: 76, color: '#B8D6D0' },
    { name: 'UI设计师', resumes: 65, interviews: 24, positions: 2, percentage: 65, color: '#F1C9BB' },
    { name: '产品经理', resumes: 52, interviews: 18, positions: 2, percentage: 52, color: '#D9E1E8' }
  ]
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: function(params) {
        const data = positionData[params.dataIndex]
        return `${data.name}<br/>简历数: ${data.resumes}<br/>面试数: ${data.interviews}<br/>在招: ${data.positions}个`
      },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#457B9D',
      borderWidth: 1,
      textStyle: {
        color: '#333'
      }
    },
    grid: {
      left: 0,
      right: 0,
      top: 0,
      bottom: 0
    },
    series: [{
      type: 'custom',
      coordinateSystem: 'none',
      renderItem: function(params, api) {
        const categoryIndex = api.value(0)
        const data = positionData[categoryIndex]
        const paddingTop = 32
        const paddingBottom = 32
        const rowH = (api.getHeight() - paddingTop - paddingBottom) / positionData.length
        const yStart = paddingTop + categoryIndex * rowH
        const contentHeight = rowH
        
        return {
          type: 'group',
          children: [
            // 职位名称
            {
              type: 'text',
              style: {
                x: 24,
                y: yStart + contentHeight * 0.28,
                text: data.name,
                textFill: '#1e293b',
                fontSize: 18,
                fontWeight: 'bold'
              }
            },
            // 简历数
            {
              type: 'text',
              style: {
                x: 24,
                y: yStart + contentHeight * 0.52,
                text: `简历数：${data.resumes}`,
                textFill: '#64748b',
                fontSize: 14,
                fontWeight: '500'
              }
            },
            // 面试数
            {
              type: 'text',
              style: {
                x: 140,
                y: yStart + contentHeight * 0.52,
                text: `面试数：${data.interviews}`,
                textFill: '#64748b',
                fontSize: 14,
                fontWeight: '500'
              }
            },
            // 在招岗位
            {
              type: 'text',
              style: {
                x: 260,
                y: yStart + contentHeight * 0.52,
                text: `在招：${data.positions}个`,
                textFill: '#64748b',
                fontSize: 14,
                fontWeight: '500'
              }
            },
            // 进度条背景
            {
              type: 'rect',
              shape: {
                x: 24,
                y: yStart + contentHeight * 0.72,
                width: api.getWidth() - 120,
                height: 12,
                r: 6
              },
              style: {
                fill: '#f1f5f9',
                opacity: 1
              }
            },
            // 进度条
            {
              type: 'rect',
              shape: {
                x: 24,
                y: yStart + contentHeight * 0.72,
                width: (api.getWidth() - 120) * (data.percentage / 100),
                height: 12,
                r: 6
              },
              style: {
                fill: data.color,
                opacity: 0.9
              }
            },
            // 百分比数值
            {
              type: 'text',
              style: {
                x: api.getWidth() - 36,
                y: yStart + contentHeight * 0.42,
                text: data.percentage.toString(),
                textFill: '#111827',
                fontSize: 28,
                fontWeight: 'bold',
                textAlign: 'right'
              }
            }
          ]
        }
      },
      data: positionData.map((item, index) => [index, item.percentage])
    }]
  }
  
  heatChart.setOption(option)
}

// 刷新数据
const refreshData = async () => {
  try {
    const { refreshRecruitmentAnalysis } = await import('@/api/statistics')
    await refreshRecruitmentAnalysis()
    await loadAll()
    console.log('数据已刷新')
  } catch (e) {
    console.error('刷新失败', e)
    console.error('刷新失败')
  }
}

// 导航到岗位详情
const navigateToJobDetail = (jobId: number) => {
  router.push(`/job-detail/${jobId}`)
}

// 导航到企业中心
const navigateToCompanyCenter = () => {
  router.push('/interviewer/system-settings/company-info')
}

// 处理漏斗图周期变化
const handleFunnelPeriodChange = (command: string) => {
  funnelPeriod.value = command
  loadFunnel()
}

// 处理热度图周期变化
const handleHeatPeriodChange = (command: string) => {
  heatPeriod.value = command
  loadHeat()
}

// 刷新按钮调用


// 获取漏斗图周期标签
const getFunnelPeriodLabel = (period: string) => {
  const labels = {
    total: '总',
    year: '年',
    quarter: '季',
    month: '月'
  }
  return labels[period] || '总'
}

// 获取热度图周期标签
const getHeatPeriodLabel = (period: string) => {
  const labels = {
    total: '总',
    year: '年',
    quarter: '季',
    month: '月'
  }
  return labels[period] || '总'
}

// 窗口大小变化处理
const handleResize = () => {
  timelineChart?.resize()
  funnelChart?.resize()
  heatChart?.resize()
}

// 监听图表类型变化
const updateTimelineChart = () => {
  if (timelineChart) {
    initTimelineChart()
  }
}

const timelineData = ref({ labels: [], series: [] as Array<{ name: string; data: number[] }> })
const funnelDataState = ref<Array<{ name: string; value: number; color: string }>>([])
const heatItems = ref<Array<{ name: string; resumes: number; interviews: number; positions: number; percentage: number; color: string }>>([])

async function loadKpis() {
  try {
    const { getRecruitmentKpis } = await import('@/api/statistics')
    const data = await getRecruitmentKpis(timeRange.value)
    stats.totalApplications = Number(data.totalApplications || 0)
    stats.applicationGrowth = Number(data.applicationGrowth || 0)
    stats.publishedJobs = Number(data.publishedJobs || 0)
    stats.jobsGrowth = Number(data.jobsGrowth || 0)
    stats.totalInterviewed = Number(data.totalInterviewed || 0)
    stats.interviewGrowth = Number(data.interviewGrowth || 0)
    stats.weeklyApplications = Number(data.weeklyApplications || 0)
    stats.weeklyGrowth = Number(data.weeklyGrowth || 0)
  } catch (e) {
    console.error('加载KPI失败', e)
  }
}

function buildTimelineOption(state: { labels: string[]; series: Array<{ name: string; data: number[] }> }) {
  const base = {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#457B9D', borderWidth: 1, textStyle: { color: '#333' } },
    legend: { data: state.series.map(s => s.name), top: 20, textStyle: { color: '#64748b' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: state.labels, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b' } },
    yAxis: { type: 'value', axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: state.series.map((s, idx) => ({
      name: s.name,
      type: 'line', smooth: true,
      lineStyle: { width: 2 }, itemStyle: {},
      areaStyle: idx === 0 ? { color: new (echarts as any).graphic.LinearGradient(0, 0, 0, 1, [ { offset: 0, color: 'rgba(69, 123, 157, 0.3)' }, { offset: 1, color: 'rgba(69, 123, 157, 0.05)' } ]) } : undefined,
      data: s.data
    }))
  }
  return base
}

async function loadTimeline() {
  try {
    const { getRecruitmentTimeline } = await import('@/api/statistics')
    const data = await getRecruitmentTimeline(timeRange.value, chartDisplayType.value)
    timelineData.value.labels = Array.isArray(data.labels) ? data.labels : []
    timelineData.value.series = Array.isArray(data.series) ? data.series : []
    if (timelineChart) {
      timelineChart.setOption(buildTimelineOption(timelineData.value), true)
    }
  } catch (e) {
    console.error('加载时间线失败', e)
  }
}

function buildFunnelOption(items: Array<{ name: string; value: number; color: string }>) {
  const fd = items
  return {
    tooltip: { trigger: 'item', formatter: (params: any) => {
      const d = fd[params.dataIndex]
      return `${d.name}<br/>人数: ${d.value}`
    }, backgroundColor: 'rgba(255, 255, 255, 0.95)', borderColor: '#457B9D', borderWidth: 1, textStyle: { color: '#333' } },
    series: [{
      type: 'custom', coordinateSystem: 'none',
      renderItem: function(params: any, api: any) {
        const i = api.value(0)
        const data = fd[i]
        const total = fd.length
        const containerW = api.getWidth(); const containerH = api.getHeight();
        const paddingTop = 30; const paddingBottom = 30; const availableH = containerH - paddingTop - paddingBottom
        const stageH = Math.min(70, availableH / total - 8); const gap = 8; const totalStageH = total * stageH + (total - 1) * gap
        const startY = paddingTop + (availableH - totalStageH) / 2
        const y = startY + i * (stageH + gap)
        const maxW = containerW * 0.88; const widthRatio = 1 - (i / (total - 1)) * 0.6
        const stageW = maxW * widthRatio; const x = (containerW - stageW) / 2
        return { type: 'group', children: [
          { type: 'rect', shape: { x, y, width: stageW, height: stageH, r: 8 }, style: { fill: data.color, opacity: 0.95, stroke: '#ffffff', lineWidth: 2, shadowColor: 'rgba(0,0,0,0.1)', shadowBlur: 8, shadowOffsetY: 4 } },
          { type: 'rect', shape: { x: x + 2, y: y + 2, width: stageW - 4, height: stageH / 3, r: 6 }, style: { fill: 'rgba(255,255,255,0.15)', opacity: 1 } },
          { type: 'text', style: { x: x + 18, y: y + stageH / 2 - 6, text: data.name, textFill: '#2d3748', fontSize: 15, fontWeight: '600', textVerticalAlign: 'middle', textShadowColor: 'rgba(255,255,255,0.5)', textShadowBlur: 1, textShadowOffsetY: 1 } },
          { type: 'text', style: { x: x + stageW - 18, y: y + stageH / 2 + 6, text: String(data.value), textFill: '#1a202c', fontSize: 18, fontWeight: '700', textAlign: 'right', textVerticalAlign: 'middle', textShadowColor: 'rgba(255,255,255,0.6)', textShadowBlur: 1, textShadowOffsetY: 1 } },
          { type: 'rect', shape: { x, y: y + stageH * 0.2, width: 4, height: stageH * 0.6, r: 2 }, style: { fill: 'rgba(45,55,72,0.3)', opacity: 1 } }
        ] }
      },
      data: fd.map((_, idx) => [idx, fd[idx].value])
    }]
  }
}

async function loadFunnel() {
  try {
    const { getRecruitmentFunnel } = await import('@/api/statistics')
    const data = await getRecruitmentFunnel(funnelPeriod.value)
    const palette = ['#AFC7D9', '#CFE3D2', '#F1C9BB', '#D9E1E8']
    const items = [
      { name: '简历投递', value: Number(data.applied || 0), color: palette[0] },
      { name: '已预约面试时间', value: Number(data.scheduled || 0), color: palette[1] },
      { name: '面试完成', value: Number(data.completed || 0), color: palette[2] },
      { name: '已发送通知', value: Number(data.notified || 0), color: palette[3] }
    ]
    funnelDataState.value = items
    if (funnelChart) {
      funnelChart.setOption(buildFunnelOption(items), true)
    }
  } catch (e) {
    console.error('加载漏斗失败', e)
  }
}

function buildHeatOption(items: Array<{ name: string; resumes: number; interviews: number; positions: number; percentage: number; color: string }>) {
  const positionData = items
  return {
    tooltip: { trigger: 'item', formatter: (params: any) => {
      const d = positionData[params.dataIndex]
      return `${d.name}<br/>简历数: ${d.resumes}<br/>面试数: ${d.interviews}<br/>在招: ${d.positions}个`
    }, backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#457B9D', borderWidth: 1, textStyle: { color: '#333' } },
    grid: { left: 0, right: 0, top: 0, bottom: 0 },
    series: [{
      type: 'custom', coordinateSystem: 'none',
      renderItem: function(params: any, api: any) {
        const i = api.value(0)
        const data = positionData[i]
        const paddingTop = 32; const paddingBottom = 32
        const rowH = (api.getHeight() - paddingTop - paddingBottom) / positionData.length
        const yStart = paddingTop + i * rowH
        const contentHeight = rowH
        return { type: 'group', children: [
          { type: 'text', style: { x: 24, y: yStart + contentHeight * 0.28, text: data.name, textFill: '#1e293b', fontSize: 18, fontWeight: 'bold' } },
          { type: 'text', style: { x: 24, y: yStart + contentHeight * 0.52, text: `简历数：${data.resumes}`, textFill: '#64748b', fontSize: 14, fontWeight: '500' } },
          { type: 'text', style: { x: 140, y: yStart + contentHeight * 0.52, text: `面试数：${data.interviews}`, textFill: '#64748b', fontSize: 14, fontWeight: '500' } },
          { type: 'text', style: { x: 260, y: yStart + contentHeight * 0.52, text: `在招：${data.positions}个`, textFill: '#64748b', fontSize: 14, fontWeight: '500' } },
          { type: 'rect', shape: { x: 24, y: yStart + contentHeight * 0.72, width: api.getWidth() - 120, height: 12, r: 6 }, style: { fill: '#f1f5f9', opacity: 1 } },
          { type: 'rect', shape: { x: 24, y: yStart + contentHeight * 0.72, width: (api.getWidth() - 120) * (data.percentage / 100), height: 12, r: 6 }, style: { fill: data.color, opacity: 0.9 } },
          { type: 'text', style: { x: api.getWidth() - 36, y: yStart + contentHeight * 0.42, text: String(data.percentage), textFill: '#111827', fontSize: 28, fontWeight: 'bold', textAlign: 'right' } }
        ] }
      },
      data: positionData.map((_, idx) => [idx, positionData[idx].percentage])
    }]
  }
}

async function loadHeat() {
  try {
    const { getRecruitmentHeat } = await import('@/api/statistics')
    const data = await getRecruitmentHeat(heatPeriod.value, 10)
    const palette = ['#AFC7D9', '#B8D6D0', '#F1C9BB', '#D9E1E8']
    const items = Array.isArray(data.items) ? data.items : []
    heatItems.value = items.map((it: any, idx: number) => ({
      name: it.title || it.name,
      resumes: Number(it.resumes || 0),
      interviews: Number(it.interviews || 0),
      positions: Number(it.positions || 0),
      percentage: Number(it.percentage || 0),
      color: palette[idx % palette.length]
    }))
    if (heatChart) {
      heatChart.setOption(buildHeatOption(heatItems.value), true)
    }
  } catch (e) {
    console.error('加载热度失败', e)
  }
}

async function loadSidebar() {
  try {
    const { getRecruitmentSidebar } = await import('@/api/statistics')
    const data = await getRecruitmentSidebar('quarter', 3, timeRange.value)
    if (data && data.recentStats) {
      recentStats.interviews = Number(data.recentStats.interviews || 0)
      recentStats.passed = Number(data.recentStats.passed || 0)
    }
    if (Array.isArray(data.recentJobs)) {
      recentJobs.value = data.recentJobs.map((j: any) => ({ id: j.id, title: j.title, applicationCount: Number(j.applicationCount || 0) }))
    }
  } catch (e) {
    console.error('加载侧边栏失败', e)
  }
}

async function loadAll() {
  await Promise.all([
    loadKpis(), loadTimeline(), loadFunnel(), loadHeat(), loadSidebar()
  ])
}

onMounted(async () => {
  await nextTick()
  initTimelineChart()
  initFunnelChart()
  initHeatChart()
  window.addEventListener('resize', handleResize)
  await loadAll()
})

onUnmounted(() => {
  timelineChart?.dispose()
  funnelChart?.dispose()
  heatChart?.dispose()
  window.removeEventListener('resize', handleResize)
})

// 监听图表显示类型变化
import { watch } from 'vue'
watch(chartDisplayType, () => { loadTimeline() })
watch(timeRange, () => { loadKpis(); loadTimeline(); loadSidebar() })


</script>

<style scoped>
.recruitment-analysis {
  padding: 0 40px 40px 40px;
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

.page-header {
  margin-bottom: 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 40px 0 32px 0;
  position: relative;
}

.header-left {
  display: flex;
  flex-direction: column;
  padding-right: 20px;
}

.main-title {
  font-size: 38px !important;
  font-weight: 800;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  background-clip: text;
  -webkit-background-clip: text;
  color: #ffffff !important;
  margin: 0;
  letter-spacing: -0.5px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3), 0 1px 4px rgba(0, 0, 0, 0.2);
}

.sub-title {
  margin: 16px 0 0 0;
  color: rgba(255, 255, 255, 0.9) !important;
  font-size: 16px !important;
  font-weight: 500;
  line-height: 1.5;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

.header-actions {
  display: flex;
  gap: 16px;
  align-items: center;
}

.refresh-btn {
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border: none !important;
  color: #F1FAEE !important;
  border-radius: 16px !important;
  padding: 8px 16px !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 2px 8px rgba(38, 70, 83, 0.25) !important;
}

.refresh-btn:hover {
  transform: translateY(-1px) scale(1.02) !important;
  box-shadow: 0 4px 12px rgba(38, 70, 83, 0.35) !important;
  background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important;
}

.chart-filter-btn {
  padding: 6px 12px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid rgba(129, 152, 170, 0.2);
  color: #5a6c7d;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(0, 0, 0, 0.02);
  min-width: 60px;
  justify-content: center;
}

.chart-filter-btn:hover {
  background: linear-gradient(135deg, #5a6c7d 0%, #4a5763 100%);
  color: #fff;
  border-color: transparent;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(90, 108, 125, 0.25), 0 2px 4px rgba(74, 87, 99, 0.1);
}

.chart-filter-btn .arrow-icon {
  transition: transform 0.3s ease;
  font-size: 12px;
}

.chart-filter-btn:hover .arrow-icon {
  transform: rotate(180deg);
}

:deep(.custom-chart-dropdown .el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  font-size: 13px;
  padding: 8px 16px;
  transition: all 0.2s ease;
  color: #5a6c7d;
  font-weight: 500;
}

:deep(.custom-chart-dropdown .el-dropdown-menu__item:hover) {
  background-color: rgba(90, 108, 125, 0.08);
  color: #5a6c7d;
}

:deep(.custom-chart-dropdown .el-dropdown-menu) {
  border-radius: 12px;
  border: 1px solid rgba(129, 152, 170, 0.15);
  box-shadow: 0 8px 32px rgba(90, 108, 125, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

:deep(.custom-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.15) !important;
  border: 1px solid rgba(255, 255, 255, 0.25) !important;
  border-radius: 20px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1), inset 0 1px 0 rgba(255, 255, 255, 0.2) !important;
  backdrop-filter: blur(10px) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

:deep(.custom-select .el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.25) !important;
  border-color: rgba(255, 255, 255, 0.4) !important;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.3) !important;
  transform: translateY(-1px) !important;
}

:deep(.custom-select .el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.3) !important;
  border-color: rgba(255, 255, 255, 0.5) !important;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.2), 0 8px 24px rgba(0, 0, 0, 0.2) !important;
}

:deep(.custom-select .el-input__inner) {
  color: rgba(255, 255, 255, 0.95) !important;
  font-weight: 500 !important;
  font-size: 14px !important;
}

:deep(.custom-select .el-input__suffix) {
  color: rgba(255, 255, 255, 0.8) !important;
}

:deep(.el-select__dropdown) {
  background: rgba(255, 255, 255, 0.95) !important;
  border: 1px solid rgba(69, 123, 157, 0.2) !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08) !important;
  backdrop-filter: blur(16px) !important;
  padding: 8px !important;
}

:deep(.el-select__dropdown .el-select-dropdown__item) {
  border-radius: 8px !important;
  margin: 2px 0 !important;
  color: #334155 !important;
  font-weight: 500 !important;
  transition: all 0.2s ease !important;
  padding: 8px 16px !important;
}

:deep(.el-select__dropdown .el-select-dropdown__item:hover) {
  background: linear-gradient(135deg, rgba(69, 123, 157, 0.1), rgba(69, 123, 157, 0.05)) !important;
  color: #457B9D !important;
  transform: translateX(2px) !important;
}

:deep(.el-select__dropdown .el-select-dropdown__item.is-selected) {
  background: linear-gradient(135deg, #457B9D, #264653) !important;
  color: #ffffff !important;
  font-weight: 600 !important;
}

:deep(.custom-radio-group .el-radio-button__inner) {
  background: rgba(248, 250, 252, 0.95) !important;
  border: 1px solid rgba(226, 232, 240, 0.8) !important;
  border-radius: 18px !important;
  color: #64748b !important;
  font-weight: 500 !important;
  font-size: 13px !important;
  padding: 8px 16px !important;
  margin: 0 4px !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04), inset 0 1px 0 rgba(255, 255, 255, 0.8) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

:deep(.custom-radio-group .el-radio-button__inner:hover) {
  background: #ffffff !important;
  border-color: rgba(69, 123, 157, 0.3) !important;
  color: #457B9D !important;
  box-shadow: 0 4px 12px rgba(69, 123, 157, 0.1), inset 0 1px 0 rgba(255, 255, 255, 1) !important;
  transform: translateY(-1px) !important;
}

:deep(.custom-radio-group .el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border-color: transparent !important;
  color: #ffffff !important;
  font-weight: 600 !important;
  box-shadow: 0 4px 16px rgba(69, 123, 157, 0.25), 0 2px 4px rgba(38, 70, 83, 0.1) !important;
  transform: translateY(-1px) !important;
}

:deep(.custom-radio-group .el-radio-button.is-active .el-radio-button__inner:hover) {
  background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important;
}

.main-content {
  display: flex;
  gap: 32px;
  align-items: flex-start;
  margin-top: 24px;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sidebar {
  width: 360px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  align-self: flex-start;
}

.kpi-cards-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-top: 0;
  margin-bottom: 8px;
}

.kpi-card {
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%);
  border-radius: 20px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.02);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: slideInUp 0.6s ease-out both;
  position: relative;
  overflow: hidden;
}

.kpi-card:hover {
  box-shadow: 0 12px 32px rgba(69, 123, 157, 0.15), 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-4px);
  border-color: rgba(69, 123, 157, 0.3);
}

.kpi-card:nth-child(1) { animation-delay: 0.1s; }
.kpi-card:nth-child(2) { animation-delay: 0.2s; }
.kpi-card:nth-child(3) { animation-delay: 0.3s; }
.kpi-card:nth-child(4) { animation-delay: 0.4s; }

.kpi-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.kpi-icon-wrapper.total {
  background: linear-gradient(135deg, rgba(69, 123, 157, 0.15), rgba(69, 123, 157, 0.1));
  color: #457B9D;
}

.kpi-icon-wrapper.jobs {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(16, 185, 129, 0.1));
  color: #10b981;
}

.kpi-icon-wrapper.interviewed {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.15), rgba(245, 158, 11, 0.1));
  color: #f59e0b;
}

.kpi-icon-wrapper.weekly {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15), rgba(139, 92, 246, 0.1));
  color: #8b5cf6;
}

.kpi-icon {
  font-size: 24px;
}

.kpi-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.kpi-value {
  font-size: 32px;
  font-weight: 800;
  color: #1e293b;
  line-height: 1;
}

.kpi-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 600;
  margin-top: 4px;
}

.kpi-trend.up {
  color: #059669;
}

.kpi-trend.stable {
  color: #6b7280;
}

.chart-container {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 4px 28px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  animation: slideInUp 0.6s ease-out 0.3s both;
}

.timeline-chart {
  height: 400px;
}

.bottom-charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.funnel-chart,
.heat-chart {
  height: 480px;
}

.chart-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: #f8fafc;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.chart-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.chart-content {
  height: calc(100% - 81px);
  min-height: 300px;
  padding: 0;
  position: relative;
}

.sidebar-card {
  background: #ffffff;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  animation: slideInUp 0.6s ease-out 0.5s both;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  margin-top: 0;
}

.sidebar-card:nth-child(1) .recent-stat:first-child .stat-label {
  color: #4A6B85;
}

.sidebar-card:nth-child(1) .recent-stat:first-child .stat-icon.jobs {
  background: linear-gradient(135deg, rgba(74, 107, 133, 0.18), rgba(74, 107, 133, 0.12));
  color: #4A6B85;
  border: 1px solid rgba(74, 107, 133, 0.3);
}

.sidebar-card:nth-child(1) .recent-stat:last-child .stat-label {
  color: #3A5F4A;
}

.sidebar-card:nth-child(1) .recent-stat:last-child .stat-icon.interviews {
  background: linear-gradient(135deg, rgba(58, 95, 74, 0.18), rgba(58, 95, 74, 0.12));
  color: #3A5F4A;
  border: 1px solid rgba(58, 95, 74, 0.3);
}

.sidebar-card:nth-child(2) .job-item:nth-child(1) .job-title {
  color: #8B5A5A;
}

.sidebar-card:nth-child(2) .job-item:nth-child(1) .job-count {
  color: #8B5A5A;
}

.sidebar-card:nth-child(2) .job-item:nth-child(2) .job-title {
  color: #6B5B7B;
}

.sidebar-card:nth-child(2) .job-item:nth-child(2) .job-count {
  color: #6B5B7B;
}

.sidebar-card:nth-child(2) .job-item:nth-child(3) .job-title {
  color: #7A5B4A;
}

.sidebar-card:nth-child(2) .job-item:nth-child(3) .job-count {
  color: #7A5B4A;
}

.sidebar-card:hover {
  box-shadow: 0 8px 24px rgba(69, 123, 157, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  transform: translateY(-2px);
}

.warning-card {
  border-color: rgba(175, 199, 217, 0.2);
  background: #ffffff;
  box-shadow: 0 4px 20px rgba(175, 199, 217, 0.15), 0 1px 3px rgba(0, 0, 0, 0.02);
}

.sidebar-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: #f8fafc;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.sidebar-icon {
  font-size: 18px;
  color: #5a6c7d;
}

.sidebar-icon.warning {
  color: #5a6c7d;
}

.sidebar-content {
  padding: 24px;
}

.recent-stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(90, 108, 125, 0.05) 0%, rgba(90, 108, 125, 0.02) 100%);
  border-radius: 16px;
  border: 1px solid rgba(90, 108, 125, 0.1);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.recent-stat::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(180deg, #5a6c7d 0%, #8198aa 100%);
  border-radius: 0 2px 2px 0;
}

.recent-stat:hover {
  background: linear-gradient(135deg, rgba(90, 108, 125, 0.08) 0%, rgba(90, 108, 125, 0.04) 100%);
  border-color: rgba(90, 108, 125, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(90, 108, 125, 0.12), 0 2px 4px rgba(0, 0, 0, 0.06);
}



.stat-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.stat-icon::before {
  content: '';
  position: absolute;
  inset: 0;
  background: inherit;
  opacity: 0.1;
  border-radius: inherit;
}

.stat-icon.jobs {
  background: linear-gradient(135deg, rgba(129, 152, 170, 0.15), rgba(129, 152, 170, 0.1));
  color: #8198aa;
  border: 1px solid rgba(129, 152, 170, 0.2);
}

.stat-icon.interviews {
  background: linear-gradient(135deg, rgba(90, 108, 125, 0.15), rgba(90, 108, 125, 0.1));
  color: #5a6c7d;
  border: 1px solid rgba(90, 108, 125, 0.2);
}

.stat-label {
  font-size: 15px;
  color: #1e293b;
  font-weight: 600;
  line-height: 1.4;
}

.stat-number {
  font-size: 32px;
  font-weight: 800;
  color: #5a6c7d;
  text-align: right;
  min-width: 80px;
  position: relative;
  text-shadow: 0 1px 2px rgba(90, 108, 125, 0.1);
}

.sidebar-card:nth-child(1) .recent-stat:first-child .stat-number {
  color: #4A6B85;
  text-shadow: 0 1px 2px rgba(74, 107, 133, 0.2);
}

.sidebar-card:nth-child(1) .recent-stat:last-child .stat-number {
  color: #3A5F4A;
  text-shadow: 0 1px 2px rgba(58, 95, 74, 0.2);
}

.recent-stat:first-child .stat-number {
  color: #8198aa;
  text-shadow: 0 1px 2px rgba(129, 152, 170, 0.1);
}

.stat-number::after {
  content: '';
  position: absolute;
  right: -8px;
  top: 50%;
  transform: translateY(-50%);
  width: 2px;
  height: 20px;
  background: linear-gradient(180deg, currentColor 0%, transparent 100%);
  opacity: 0.3;
  border-radius: 1px;
}



.job-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}


.job-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, rgba(129, 152, 170, 0.06) 0%, rgba(129, 152, 170, 0.03) 100%);
  border-radius: 14px;
  border: 1px solid rgba(129, 152, 170, 0.12);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.job-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: linear-gradient(180deg, #8198aa 0%, #5a6c7d 100%);
  border-radius: 0 2px 2px 0;
  transition: width 0.3s ease;
}

.job-item:hover {
  background: linear-gradient(135deg, rgba(129, 152, 170, 0.1) 0%, rgba(129, 152, 170, 0.06) 100%);
  border-color: rgba(129, 152, 170, 0.2);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(129, 152, 170, 0.15), 0 2px 6px rgba(0, 0, 0, 0.06);
}

.job-item:hover::before {
  width: 5px;
}

.job-item:last-child {
  margin-bottom: 0;
}

.job-content {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
}

.job-rank {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  flex-shrink: 0;
  position: relative;
}

.sidebar-card:nth-child(2) .job-item:nth-child(1) .job-rank .rank-number {
  color: #8B5A5A;
}

.sidebar-card:nth-child(2) .job-item:nth-child(2) .job-rank .rank-number {
  color: #6B5B7B;
}

.sidebar-card:nth-child(2) .job-item:nth-child(3) .job-rank .rank-number {
  color: #7A5B4A;
}

.job-rank.rank-1 {
  background: linear-gradient(135deg, rgba(142, 169, 185, 0.2), rgba(142, 169, 185, 0.15));
  color: #5a7186;
  border: 1px solid rgba(142, 169, 185, 0.25);
}

.job-rank.rank-2 {
  background: linear-gradient(135deg, rgba(129, 152, 170, 0.18), rgba(129, 152, 170, 0.13));
  color: #5a6c7d;
  border: 1px solid rgba(129, 152, 170, 0.22);
}

.job-rank.rank-3 {
  background: linear-gradient(135deg, rgba(116, 135, 154, 0.16), rgba(116, 135, 154, 0.11));
  color: #586673;
  border: 1px solid rgba(116, 135, 154, 0.2);
}

.rank-number {
  position: relative;
  z-index: 1;
}

.job-info {
  flex: 1;
}

.job-title {
  font-size: 15px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 6px;
  line-height: 1.3;
}

.job-count {
  font-size: 13px;
  color: #5a6c7d;
  font-weight: 500;
}

.sidebar-card:nth-child(2) .job-item:nth-child(1) .job-arrow {
  color: #8B5A5A;
}

.sidebar-card:nth-child(2) .job-item:nth-child(2) .job-arrow {
  color: #6B5B7B;
}

.sidebar-card:nth-child(2) .job-item:nth-child(3) .job-arrow {
  color: #7A5B4A;
}

.job-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(129, 152, 170, 0.1);
  transition: all 0.3s ease;
}

.job-arrow {
  color: #8198aa;
  font-size: 14px;
  transition: all 0.3s ease;
}

.job-item:hover .job-action {
  background: rgba(129, 152, 170, 0.2);
  transform: scale(1.1);
}

.job-item:hover .job-arrow {
  color: #5a6c7d;
  transform: translateX(2px);
}

.warning-content {
  text-align: center;
}

.warning-content p {
  margin: 0 0 20px 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.complete-btn {
  background: linear-gradient(135deg, #74879a 0%, #5a6c7d 100%) !important;
  border: none !important;
  color: #ffffff !important;
  border-radius: 16px !important;
  padding: 8px 20px !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 2px 8px rgba(116, 135, 154, 0.25) !important;
}

.complete-btn:hover {
  transform: translateY(-1px) scale(1.02) !important;
  box-shadow: 0 4px 12px rgba(116, 135, 154, 0.35) !important;
  background: linear-gradient(135deg, #5a6c7d 0%, #4a5763 100%) !important;
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .kpi-cards-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1100px) {
  .recruitment-analysis {
    padding: 20px;
  }

  .main-content {
    flex-direction: column;
    gap: 24px;
  }

  .sidebar {
    width: 100%;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    display: grid;
    gap: 20px;
  }

  .kpi-cards-row {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .bottom-charts-row {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .main-title {
    font-size: 36px !important;
  }

  .sub-title {
    font-size: 18px !important;
  }
}

@media (max-width: 640px) {
  .recruitment-analysis {
    padding: 16px;
  }

  .kpi-cards-row {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .sidebar {
    grid-template-columns: 1fr;
  }

  .kpi-card {
    padding: 20px;
  }

  .kpi-value {
    font-size: 24px;
  }

  .chart-header {
    padding: 16px 20px;
  }

  .sidebar-content {
    padding: 20px;
  }
}
</style> 