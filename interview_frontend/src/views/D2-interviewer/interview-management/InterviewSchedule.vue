<template>
  <div class="page-container">
    <div class="page-header">
      <h1>面试管理</h1>
      <p>查看已投递候选人，定位候选人面试进度</p>
    </div>
    
    <div class="page-content">
      <el-card class="filter-card">
        <div class="filter-container">
          <el-input 
            v-model="searchQuery" 
            placeholder="搜索候选人姓名" 
            prefix-icon="Search" 
            class="search-input"
          />
          <el-select v-model="jobFilter" placeholder="筛选岗位" clearable class="filter-select">
            <el-option 
              v-for="job in jobOptions" 
              :key="job.id" 
              :label="job.title" 
              :value="job.title" />
          </el-select>
          <el-select v-model="stageFilter" placeholder="筛选阶段" clearable class="filter-select">
            <el-option label="已投递简历" :value="0" />
            <el-option label="待面试" :value="1" />
            <el-option label="已完成面试" :value="2" />
            <el-option label="已通过" :value="3" />
            <el-option label="已经拒绝" :value="4" />
          </el-select>
          <div class="filter-buttons">
            <el-button type="primary" @click="onSearch">搜索</el-button>
            <el-button @click="onReset">重置</el-button>
          </div>
        </div>
      </el-card>

      <el-card>
        <div class="table-toolbar">
          <div class="left">
            <div class="list-title">
              <span class="accent"></span>
              <span class="title-text">候选人面试安排列表</span>
            </div>
          </div>
          <div class="right">
            <span>共 {{ total }} 条</span>
          </div>
        </div>

        <div class="table-wrapper">
          <el-table
            :data="scheduleList"
            :loading="loading"
            table-layout="auto"
            style="width: 100%"
          >
            <el-table-column prop="name" label="姓名" :width="nameColWidth" show-overflow-tooltip />
            <el-table-column prop="position" label="投递岗位" :min-width="positionColMin" show-overflow-tooltip class-name="position-column" />
            <el-table-column prop="createTime" label="投递/面试时间" :min-width="timeColMin" class-name="time-column">
              <template #default="scope">
                {{ formatDateTimeDisplay(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="stageText" label="阶段" :width="stageColWidth">
              <template #default="scope">
                <el-tag :type="getStageTagType(scope.row.stage)">{{ scope.row.stageText }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div class="pagination-container">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            prev-text="上一页"
            next-text="下一页"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { formatDateTimeDisplay } from '@/utils/dateUtils'
import { fetchCandidates } from '@/api/aiScreening'

const searchQuery = ref('')
const jobFilter = ref<string>('')
const stageFilter = ref<number | ''>('')

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const loading = ref(false)
const scheduleList = ref<any[]>([])

const screenWidth = ref(window.innerWidth)

// 自适应列宽
const nameColWidth = computed(() => {
  if (screenWidth.value >= 1680) return 120
  if (screenWidth.value >= 1440) return 110
  return 100
})
const positionColMin = computed(() => {
  if (screenWidth.value >= 1680) return 260
  if (screenWidth.value >= 1440) return 220
  return 170
})
const timeColMin = computed(() => {
  if (screenWidth.value >= 1680) return 220
  if (screenWidth.value >= 1440) return 200
  return 180
})
const stageColWidth = computed(() => {
  if (screenWidth.value >= 1680) return 130
  if (screenWidth.value >= 1440) return 120
  return 110
})

const jobOptions = ref<Array<{ id: number; title: string }>>([])

const loadJobs = async () => {
  try {
    const res: any = await request({ url: '/api/jobs', method: 'get' })
    if (Array.isArray(res)) {
      jobOptions.value = res.map((j: any) => ({ id: j.id, title: j.title || j.name || `岗位${j.id}` }))
    }
  } catch (e) {
  }
}

const stageNumberToText = (stage: number): string => {
  switch (stage) {
    case 0: return '已投递简历'
    case 1: return '待面试'
    case 2: return '已完成面试'
    case 3: return '已通过'
    case 4: return '已经拒绝'
    default: return '未知'
  }
}

const getStageTagType = (stage: number): string => {
  if (stage === 3) return 'success'
  if (stage === 1) return 'warning'
  if (stage === 4) return 'danger'
  if (stage === 0) return 'info'
  if (stage === 2) return ''
  return ''
}

const mapStageToStatus = (stage: number): number => {
  if (stage === 3) return 1
  if (stage === 4) return 2
  return 0
}

const loadData = async () => {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchQuery.value) params.keyword = searchQuery.value
    if (jobFilter.value) params.position = jobFilter.value
    if (stageFilter.value !== '') params.status = mapStageToStatus(stageFilter.value as number)

    const res: any = await fetchCandidates(params)
    const list = Array.isArray(res && res.list) ? res.list : []

    scheduleList.value = list.map((i: any) => {
      const stage = typeof i.stage === 'number' ? i.stage : normalizeStage(i.status || i.stage)
      return {
        id: i.applicationId || i.id,
        name: i.name,
        position: i.position || i.jobTitle,
        createTime: i.createTime || i.interviewTime || i.submitTime,
        stage,
        stageText: stageNumberToText(stage)
      }
    })
    total.value = Number((res && res.total) || 0)
  } catch (e) {
    console.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

function normalizeStage(status: any): number {
  if (typeof status === 'number') return status
  if (typeof status === 'string') {
    const s = status.trim()
    if (['已投递简历', 'SUBMITTED', 'APPLIED', 'PENDING1', '待处理'].includes(s)) return 0
    if (['待面试', 'SCHEDULED', 'PENDING2'].includes(s)) return 1
    if (['已完成面试', 'INTERVIEWED', 'COMPLETED1'].includes(s)) return 2
    if (['已通过', 'PASSED', 'APPROVED'].includes(s)) return 3
    if (['已经拒绝', '已拒绝', 'REJECTED', 'REFUSED', 'DECLINED'].includes(s)) return 4
  }
  return 0
}

const handleResize = () => {
  screenWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadJobs()
  loadData()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

const onSearch = () => {
  currentPage.value = 1
  loadData()
}

const onReset = () => {
  searchQuery.value = ''
  jobFilter.value = ''
  stageFilter.value = ''
  currentPage.value = 1
  pageSize.value = 10
  loadData()
}

const handleCurrentChange = (page: number) => {
  currentPage.value = page
  loadData()
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
}

.page-header p {
  color: #606266;
  font-size: 14px;
}

.page-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 8px;
}

.filter-card {
  margin-bottom: 0;
}

.filter-container {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: 300px;
  min-width: 200px;
}

.filter-select {
  width: 180px;
  min-width: 150px;
}

.filter-buttons {
  display: flex;
  gap: 8px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  flex-wrap: wrap;
  gap: 10px;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

:deep(.el-table .el-table__row) {
  height: 48px;
}

:deep(.el-table .el-table__cell) {
  padding: 6px 10px;
}

.pagination-container {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.list-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.list-title .accent {
  width: 6px;
  height: 18px;
  border-radius: 3px;
  background: linear-gradient(180deg, #409EFF 0%, #67C23A 100%);
}

.list-title .title-text {
  font-weight: 600;
  color: #303133;
}

@media (max-width: 768px) {
  .page-container {
    padding: 12px;
  }
  
  .filter-container {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input,
  .filter-select {
    width: 100%;
  }
  
  .filter-buttons {
    justify-content: center;
  }
  
  .table-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .pagination-container {
    justify-content: center;
  }

  :deep(.position-column),
  :deep(.time-column) {
    display: none;
  }
}

@media (max-width: 1024px) {
  .page-container {
    padding: 16px;
  }
  
  .search-input {
    width: 250px;
  }
  
  .filter-select {
    width: 160px;
  }

  :deep(.time-column) {
    display: none;
  }
}
</style>