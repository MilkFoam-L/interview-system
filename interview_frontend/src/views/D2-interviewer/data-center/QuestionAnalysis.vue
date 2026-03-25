<template>
  <div class="qa-analysis">
    <div class="page-header">
      <div class="header-left">
        <h1 class="main-title">题库数据分析</h1>
        <p class="sub-title">题库规模、结构与质量分析，辅助优化命题与筛选策略</p>
      </div>
      <div class="header-actions">
        <el-button 
          type="primary" 
          @click="onRefresh"
          class="refresh-btn"
        >
          刷新数据
        </el-button>
      </div>
    </div>

    <div class="main-content">
      <div class="main-area">
        <!-- 顶部 -->
        <div class="kpi-cards-row">
          <div class="kpi-card">
            <div class="kpi-icon-wrapper total">
              <el-icon class="kpi-icon"><Document /></el-icon>
            </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ kpis.totalQuestions }}</div>
              <div class="kpi-label">总题量</div>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon-wrapper active">
              <el-icon class="kpi-icon"><Collection /></el-icon>
            </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ kpis.activeQuestions }}</div>
              <div class="kpi-label">在用题量</div>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon-wrapper used">
              <el-icon class="kpi-icon"><TrendCharts /></el-icon>
            </div>
            <div class="kpi-content">
              <div class="kpi-value">{{ formatPercent(kpis.coverageRate) }}</div>
              <div class="kpi-label">使用覆盖率</div>
            </div>
          </div>
        </div>

        <!-- 中部图表 -->
        <div class="chart-container accuracy-chart">
          <div class="chart-header">
            <h3>题型正确率 / 错误率</h3>
          </div>
          <div ref="accuracyChartRef" class="chart-content"></div>
        </div>

        <!-- 底部 -->
        <div class="bottom-charts-row">
          <div class="chart-container category-chart">
            <div class="chart-header">
              <h3>分类占比</h3>
              <el-radio-group v-model="categoryMode" size="small" class="custom-radio-group">
                <el-radio-button label="major">大类</el-radio-button>
                <el-radio-button label="sub">细分</el-radio-button>
              </el-radio-group>
            </div>
            <div ref="categoryChartRef" class="chart-content"></div>
          </div>
          <div class="chart-container type-chart">
            <div class="chart-header">
              <h3>题型占比</h3>
            </div>
            <div ref="typeChartRef" class="chart-content"></div>
          </div>
        </div>
      </div>

      <!-- 右侧 -->
      <div class="sidebar">
        <div class="sidebar-card">
          <div class="sidebar-header">
            <el-icon class="sidebar-icon"><PriceTag /></el-icon>
            <span>题目难度分布</span>
          </div>
          <div class="sidebar-content">
            <div ref="difficultyRingsRef" class="difficulty-rings"></div>
            <div class="legend-row">
              <div class="legend-item"><span class="dot simple"></span><span class="name">简单</span></div>
              <div class="legend-item"><span class="dot medium"></span><span class="name">中等</span></div>
              <div class="legend-item"><span class="dot hard"></span><span class="name">困难</span></div>
            </div>
          </div>
        </div>

        <div class="sidebar-card notice-card">
          <div class="sidebar-header">
            <el-icon class="sidebar-icon"><Warning /></el-icon>
            <span>题库更新提示</span>
          </div>
          <div class="sidebar-content update-box">
            <div class="days-highlight">
              <span class="num">{{ updateInfo.daysSinceUpdate }}</span>
              <span class="unit">天</span>
            </div>
            <div class="update-text">题库已久未更新，建议补充新题以提升区分度</div>
            <el-button class="link-btn" type="primary" @click="goToQuestionManagement">
              前往题目管理
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Document, Collection, TrendCharts, PriceTag, Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { 
  getQuestionKpis,
  getTypeAccuracy,
  getQuestionTypeDistribution,
  getCategoryDistribution,
  getDifficultyDistribution,
  getQuestionBankUpdateInfo,
  refreshQuestionAnalysis
} from '@/api/questionAnalysis'

const router = useRouter()

const kpis = reactive({ totalQuestions: 0, activeQuestions: 0, usedQuestions: 0, coverageRate: 0 })
const updateInfo = reactive({ daysSinceUpdate: 0, lastUpdatedAt: '' })

const categoryMode = ref<'major' | 'sub'>('major')

const accuracyChartRef = ref<HTMLElement>()
const typeChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()
const difficultyRingsRef = ref<HTMLElement>()

let accuracyChart: echarts.ECharts | null = null
let typeChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null
let difficultyRingsChart: echarts.ECharts | null = null

function formatPercent(n: number) { return `${Number(n || 0).toFixed(2)}%` }

function toCNType(name: string) {
  const map: Record<string, string> = {
    code: '编程题', basic: '基础题', scenario: '场景题', choice: '选择题', text: '基础题'
  }
  const key = String(name || '').toLowerCase()
  return map[key] || name
}

function buildTypeAccuracyHorizontalOption(items: Array<{ type: string; correctRate: number; wrongRate: number }>) {
  const ordered = ['basic', 'choice', 'code']
  const mapped = ordered.map(k => {
    const it = items.find(x => (x.type || '').toLowerCase() === k) || { type: k, correctRate: 0, wrongRate: 0 }
    return { name: toCNType(k), correct: Number(it.correctRate || 0), wrong: Number(it.wrongRate || (100 - Number(it.correctRate || 0))) }
  })
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: (ps: any) => {
      const n = ps[0]?.axisValue || ''
      const correct = ps.find((p: any) => p.seriesName === '正确率')?.data ?? 0
      const wrong = ps.find((p: any) => p.seriesName === '错误率')?.data ?? 0
      return `${n}<br/>正确率：${correct}%<br/>错误率：${wrong}%`
    } },
    legend: { data: ['正确率', '错误率'], top: 16, textStyle: { color: '#64748b' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '20%', containLabel: true },
    xAxis: { type: 'value', max: 100, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b', formatter: '{value}%' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    yAxis: { type: 'category', data: mapped.map(m => m.name), axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#64748b' } },
    series: [
      { name: '正确率', type: 'bar', data: mapped.map(m => m.correct), barWidth: 14, itemStyle: { color: '#457B9D', borderRadius: [0,6,6,0] } },
      { name: '错误率', type: 'bar', data: mapped.map(m => m.wrong), barWidth: 14, itemStyle: { color: '#CFE3D2', borderRadius: [0,6,6,0] } }
    ]
  }
}

function buildTypePieOption(items: Array<{ name: string; value: number }>) {
  const data = items.map(it => ({ name: toCNType(it.name), value: it.value }))
  return {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 12, top: 16, textStyle: { color: '#64748b' } },
    series: [
      { name: '题型', type: 'pie', radius: ['48%', '72%'], center: ['40%', '50%'], itemStyle: { borderColor: '#fff', borderWidth: 2 }, label: { show: true, formatter: '{b}: {d}%' }, data: data.map((it, idx) => ({ name: it.name, value: it.value, itemStyle: { color: ['#AFC7D9', '#B8D6D0', '#F1C9BB', '#D9E1E8'][idx % 4] } })) }
    ]
  }
}

function buildCategoryDonutOption(items: Array<{ name: string; value: number }>) {
  return {
    tooltip: { trigger: 'item' },
    legend: { show: false },
    series: [
      { name: '分类', type: 'pie', radius: ['48%', '72%'], center: ['50%', '50%'], itemStyle: { borderColor: '#fff', borderWidth: 2 }, label: { show: true, formatter: '{b}: {d}%' }, data: items.map((it, idx) => ({ name: it.name, value: it.value, itemStyle: { color: ['#457B9D', '#AFC7D9', '#CFE3D2', '#B8D6D0', '#F1C9BB', '#D9E1E8'][idx % 6] } })) }
    ]
  }
}

function buildDifficultyRingsOption(d: { simple: number; medium: number; hard: number }) {
  const rings = [
    { name: '简单', value: d.simple, radius: '85%', color: '#7FB069', width: 12 },
    { name: '中等', value: d.medium, radius: '65%', color: '#AFC7D9', width: 12 },
    { name: '困难', value: d.hard, radius: '45%', color: '#457B9D', width: 12 }
  ]
  return {
    tooltip: { trigger: 'item', formatter: (p: any) => `${p.seriesName}: ${p.value}%` },
    series: rings.map(r => ({
      type: 'gauge', name: r.name, min: 0, max: 100, startAngle: 90, endAngle: -270,
      radius: r.radius, center: ['50%', '50%'],
      pointer: { show: false },
      progress: { show: true, roundCap: true, width: r.width, itemStyle: { color: r.color } },
      axisLine: { lineStyle: { width: r.width, color: [[1, '#edf2f7']] } },
      splitLine: { show: false }, axisTick: { show: false }, axisLabel: { show: false },
      detail: { show: false },
      data: [{ value: Math.max(0, Math.min(100, r.value)) }]
    }))
  }
}

async function loadKpis() {
  const data = await getQuestionKpis()
  kpis.totalQuestions = data.totalQuestions
  kpis.activeQuestions = data.activeQuestions
  kpis.usedQuestions = data.usedQuestions
  kpis.coverageRate = data.coverageRate
}

async function loadTypeAccuracy() {
  const items = await getTypeAccuracy()
  if (accuracyChart) accuracyChart.setOption(buildTypeAccuracyHorizontalOption(items), true)
}

async function loadTypeDistribution() {
  const items = await getQuestionTypeDistribution()
  if (typeChart) typeChart.setOption(buildTypePieOption(items), true)
}

async function loadCategory() {
  const items = await getCategoryDistribution(categoryMode.value, 12)
  if (categoryChart) categoryChart.setOption(buildCategoryDonutOption(items), true)
}

async function loadDifficulty() {
  const data = await getDifficultyDistribution()
  if (difficultyRingsChart) difficultyRingsChart.setOption(buildDifficultyRingsOption(data), true)
}

async function loadUpdateInfo() {
  const info = await getQuestionBankUpdateInfo()
  updateInfo.daysSinceUpdate = info.daysSinceUpdate
  updateInfo.lastUpdatedAt = info.lastUpdatedAt
}

function goToQuestionManagement() { router.push('/interviewer/question-bank/management') }

function handleResize() { accuracyChart?.resize(); typeChart?.resize(); categoryChart?.resize(); difficultyRingsChart?.resize() }

async function onRefresh() {
  try { await refreshQuestionAnalysis() } catch {}
  await Promise.all([loadKpis(), loadTypeAccuracy(), loadTypeDistribution(), loadCategory(), loadDifficulty(), loadUpdateInfo()])
  console.log('已刷新')
}

onMounted(async () => {
  await nextTick()
  if (accuracyChartRef.value) accuracyChart = echarts.init(accuracyChartRef.value)
  if (typeChartRef.value) typeChart = echarts.init(typeChartRef.value)
  if (categoryChartRef.value) categoryChart = echarts.init(categoryChartRef.value)
  if (difficultyRingsRef.value) difficultyRingsChart = echarts.init(difficultyRingsRef.value)
  window.addEventListener('resize', handleResize)
  await Promise.all([loadKpis(), loadTypeAccuracy(), loadTypeDistribution(), loadCategory(), loadDifficulty(), loadUpdateInfo()])
})

onUnmounted(() => {
  accuracyChart?.dispose(); typeChart?.dispose(); categoryChart?.dispose(); difficultyRingsChart?.dispose()
  window.removeEventListener('resize', handleResize)
})

watch(categoryMode, () => { loadCategory() })
</script>

<style scoped>
.qa-analysis {
  padding: 0 40px 40px 40px;
  min-height: 100vh;
  background: transparent;
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.page-header { margin-bottom: 32px; display: flex; justify-content: space-between; align-items: center; padding: 40px 0 32px 0; position: relative; }
.header-left { display: flex; flex-direction: column; padding-right: 20px; }
.main-title { font-size: 38px !important; font-weight: 800; background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%); background-clip: text; -webkit-background-clip: text; color: #ffffff !important; margin: 0; letter-spacing: -0.5px; text-shadow: 0 2px 8px rgba(0,0,0,0.3), 0 1px 4px rgba(0,0,0,0.2); }
.sub-title { margin: 16px 0 0 0; color: rgba(255, 255, 255, 0.9) !important; font-size: 16px !important; font-weight: 500; line-height: 1.5; text-shadow: 0 1px 4px rgba(0,0,0,0.3); }
.header-actions { display: flex; gap: 16px; align-items: center; }
.refresh-btn { background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important; border: none !important; color: #F1FAEE !important; border-radius: 16px !important; padding: 8px 16px !important; font-weight: 600 !important; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important; box-shadow: 0 2px 8px rgba(38,70,83,0.25) !important; }
.refresh-btn:hover { transform: translateY(-1px) scale(1.02) !important; box-shadow: 0 4px 12px rgba(38,70,83,0.35) !important; background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important; }

.main-content { display: flex; gap: 28px; align-items: flex-start; margin-top: 24px; }
.main-area { flex: 1; display: flex; flex-direction: column; gap: 20px; }
.sidebar { width: 360px; display: flex; flex-direction: column; gap: 20px; }

.kpi-cards-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.kpi-card { background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%); border-radius: 20px; padding: 22px; display: flex; align-items: center; gap: 18px; border: 1px solid rgba(226,232,240,0.8); box-shadow: 0 4px 20px rgba(0,0,0,0.06); }
.kpi-icon-wrapper { width: 56px; height: 56px; border-radius: 16px; display: flex; align-items: center; justify-content: center; }
.kpi-icon-wrapper.total { background: linear-gradient(135deg, rgba(69,123,157,0.18), rgba(69,123,157,0.12)); color: #457B9D; }
.kpi-icon-wrapper.active { background: linear-gradient(135deg, rgba(175,199,217,0.2), rgba(175,199,217,0.12)); color: #4A6B85; }
.kpi-icon-wrapper.used { background: linear-gradient(135deg, rgba(139,92,246,0.18), rgba(139,92,246,0.12)); color: #8B5CF6; }
.kpi-icon { font-size: 24px; }
.kpi-content { flex: 1; display: flex; flex-direction: column; gap: 4px; }
.kpi-value { font-size: 32px; font-weight: 800; color: #1e293b; line-height: 1; }
.kpi-label { font-size: 14px; color: #64748b; font-weight: 500; }

.chart-container { background: #ffffff; border-radius: 20px; border: 1px solid rgba(226,232,240,0.8); box-shadow: 0 4px 28px rgba(0,0,0,0.08); overflow: hidden; }
.chart-header { padding: 20px 24px; border-bottom: 1px solid rgba(226,232,240,0.6); background: #f8fafc; display: flex; justify-content: space-between; align-items: center; }
.chart-header h3 { margin: 0; font-size: 18px; font-weight: 700; color: #1e293b; }
.chart-content { height: 360px; }

.bottom-charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.type-chart .chart-content, .category-chart .chart-content { height: 360px; }

.sidebar-card { background: #ffffff; border-radius: 20px; border: 1px solid rgba(226,232,240,0.8); box-shadow: 0 4px 20px rgba(0,0,0,0.06); overflow: hidden; }
.sidebar-header { padding: 20px 24px; border-bottom: 1px solid rgba(226,232,240,0.6); background: #f8fafc; display: flex; align-items: center; gap: 12px; font-size: 16px; font-weight: 600; color: #1e293b; }
.sidebar-icon { font-size: 18px; color: #5a6c7d; }
.sidebar-content { padding: 24px; }
.difficulty-rings { height: 260px; }

.notice-card .update-box { text-align: center; display: flex; flex-direction: column; align-items: center; gap: 12px; }
.days-highlight { display: inline-flex; align-items: baseline; gap: 6px; background: linear-gradient(135deg, rgba(69,123,157,0.12), rgba(69,123,157,0.06)); border: 1px solid rgba(69,123,157,0.25); border-radius: 14px; padding: 10px 16px; box-shadow: inset 0 1px 0 rgba(255,255,255,0.5); }
.days-highlight .num { font-size: 36px; font-weight: 900; color: #457B9D; line-height: 1; }
.days-highlight .unit { font-size: 14px; font-weight: 700; color: #4A6B85; }
.update-text { color: #64748b; font-size: 14px; }
.link-btn { border-radius: 14px !important; font-weight: 700 !important; background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important; color: #fff !important; }

.accuracy-chart .chart-content { height: 420px; }

.legend-row { display: flex; justify-content: center; gap: 16px; margin-top: 10px; }
.legend-item { display: inline-flex; align-items: center; gap: 6px; color: #64748b; font-size: 13px; font-weight: 600; }
.legend-item .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.legend-item .dot.hard { background: #457B9D; }
.legend-item .dot.medium { background: #AFC7D9; }
.legend-item .dot.simple { background: #7FB069; }

:deep(.custom-radio-group .el-radio-button__inner) {
  background: rgba(248, 250, 252, 0.95) !important;
  border: 1px solid rgba(226, 232, 240, 0.8) !important;
  border-radius: 18px !important;
  color: #64748b !important; font-weight: 500 !important; font-size: 13px !important; padding: 8px 16px !important; margin: 0 4px !important;
}
:deep(.custom-radio-group .el-radio-button.is-active .el-radio-button__inner) {
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border-color: transparent !important; color: #ffffff !important; font-weight: 600 !important;
}

@media (max-width: 1200px) {
  .kpi-cards-row { grid-template-columns: 1fr; }
  .bottom-charts-row { grid-template-columns: 1fr; }
  .sidebar { width: 100%; }
  .main-content { flex-direction: column; }
}
</style>