<template>
  <div v-if="position" class="position-detail-container">
    <div class="page-header">
      <div class="header-content">
        <div>
          <h1>{{ position.title }} 详情</h1>
          <p>查看岗位详细信息</p>
        </div>
        <el-button type="primary" size="large" @click="handleEdit">
          <el-icon><Plus /></el-icon>编辑岗位
        </el-button>
      </div>
    </div>

    <div class="detail-content">
      <el-row :gutter="20">
        <el-col :span="16">
          <!-- 基本信息 -->
          <el-card class="detail-card" style="height: 100%;">
            <template #header>
              <div class="card-header">
                <h2>基本信息</h2>
              </div>
            </template>
            <div class="info-section">
              <div class="info-header">
                <h3 class="position-name">{{ position.title }}</h3>
                <div class="position-status-actions">
                  <el-button v-if="position.status === 0" type="success" plain>开放</el-button>
                  <el-button v-else type="danger" plain>关闭</el-button>
                </div>
              </div>
              <div class="position-tags">
                <el-tag size="small" effect="plain">{{ position.category || '-' }}</el-tag>
                <el-tag size="small" type="success" effect="plain">{{ position.categoryType || '-' }}</el-tag>
                <el-tag size="small" type="info" effect="plain">{{ position.location || '-' }}</el-tag>
                <el-tag size="small" type="warning" effect="plain">{{ getWorkTypeText(position.workType) }}</el-tag>
                <el-tag :type="position.status === 0 ? 'primary' : 'danger'" size="small" effect="plain">
                  {{ position.status === 0 ? '开放' : '关闭' }}
                </el-tag>
              </div>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="岗位类别">{{ position.category || '-' }}</el-descriptions-item>
                <el-descriptions-item label="岗位类型">{{ position.categoryType || '-' }}</el-descriptions-item>
                <el-descriptions-item label="工作类型">{{ getWorkTypeText(position.workType) }}</el-descriptions-item>
                <el-descriptions-item label="薪资范围">{{ position.salary || '-' }}</el-descriptions-item>
                <el-descriptions-item label="学历要求">{{ position.education || '-' }}</el-descriptions-item>
                <el-descriptions-item label="工作年限">{{ position.experience || '-' }}</el-descriptions-item>
                <el-descriptions-item label="工作地点">{{ position.location || '-' }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ formatDate(position.updated_at) }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-card>
        </el-col>

        <el-col :span="8">
          <!-- 申请信息 -->
          <el-card class="detail-card" style="height: 100%;">
            <template #header>
              <div class="card-header">
                <h2>申请信息</h2>
              </div>
            </template>
            <div class="apply-section">
              <el-statistic title="收到简历" :value="position.applications || 0" />
              <el-divider />
              <div class="apply-info">
                <p><el-icon><Calendar /></el-icon> 发布时间：{{ formatDate(position.created_at) }}</p>
                <p><el-icon><Timer /></el-icon> 最后更新：{{ formatDate(position.updated_at) }}</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 职位描述 -->
      <el-card class="detail-card">
        <template #header>
          <div class="card-header">
            <h2>职位描述</h2>
          </div>
        </template>
        <div class="description-section">
          <div class="position-summary">
            <h3>岗位简介</h3>
            <p class="summary-text">{{ position.description || '暂无描述' }}</p>
          </div>

          <h3>工作职责</h3>
          <ul class="duty-list">
            <li v-for="(duty, index) in parseDuties(position.duties)" :key="index">{{ duty }}</li>
          </ul>

          <h3>职位要求</h3>
          <ul class="requirement-list">
            <li v-for="(req, index) in parseRequirements(position.requirements)" :key="index">{{ req }}</li>
          </ul>
        </div>
      </el-card>
    </div>

    <PositionForm 
      v-model:visible="isFormVisible"
      :position-data="position"
      @submit="handleFormSubmit"
    />
  </div>
  <div v-else>
    <p>正在加载岗位信息...</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Calendar, Plus, Timer } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import PositionForm from './PositionForm.vue'
import { ElMessage } from 'element-plus'
import { fetchJobDetail, updateJob } from '@/api/jobs'

const route = useRoute()
const router = useRouter()

const position = ref<any>(null)
const isFormVisible = ref(false)

// 工作类型映射
const getWorkTypeText = (workType: number): string => {
  const workTypeMap: Record<number, string> = {
    0: '全职',
    1: '实习',
    2: '兼职'
  }
  return workTypeMap[workType] || '-'
}

// 格式化日期
const formatDate = (dateString: string): string => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 解析职责（假设存储为逗号分隔的字符串或JSON）
const parseDuties = (duties: string): string[] => {
  if (!duties) return ['暂无职责描述']
  
  // 尝试解析JSON
  try {
    const parsed = JSON.parse(duties)
    if (Array.isArray(parsed)) return parsed
  } catch (e) {
    // 如果不是JSON，按逗号分隔
    return duties.split(',').map(item => item.trim()).filter(item => item)
  }
  
  return [duties]
}

// 解析要求（假设存储为逗号分隔的字符串或JSON）
const parseRequirements = (requirements: string): string[] => {
  if (!requirements) return ['暂无具体要求']
  
  // 尝试解析JSON
  try {
    const parsed = JSON.parse(requirements)
    if (Array.isArray(parsed)) return parsed
  } catch (e) {
    // 如果不是JSON，按逗号分隔
    return requirements.split(',').map(item => item.trim()).filter(item => item)
  }
  
  return [requirements]
}

onMounted(async () => {
  const positionId = Number(route.params.id)
  
  try {
    const { data } = await fetchJobDetail(positionId)
    position.value = data
  } catch (error) {
    console.error('获取岗位详情失败:', error)
    ElMessage.error('岗位信息不存在')
    router.push('/interviewer/position-management/list')
  }
})

const handleEdit = () => {
  isFormVisible.value = true
}

const handleFormSubmit = async (formData: any) => {
  try {
    const { data } = await updateJob(formData.id, formData)
    if (data.success) {
      position.value = { ...formData }
      ElMessage.success(data.message || '岗位信息更新成功！')
    } else {
      ElMessage.error(data.message || '更新失败')
    }
  } catch (error) {
    console.error('更新岗位失败:', error)
    ElMessage.error('更新失败，请稍后重试')
  }
}
</script>

<style scoped>
.position-detail-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.page-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
}

.page-header p {
  color: #606266;
  font-size: 14px;
}

.detail-content {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-card {
}

.card-header {
  h2 {
    font-size: 18px;
    margin: 0;
    color: #303133;
  }
}

.info-section {
  .info-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .position-name {
    font-size: 20px;
    color: #303133;
    margin: 0 0 12px 0;
  }

  .position-status-actions {
    display: flex;
    gap: 10px;
  }

  .position-tags {
    margin-bottom: 20px;
    .el-tag {
      margin-right: 8px;
    }
  }
}

.description-section {
  .position-summary {
    margin-bottom: 24px;
    
    .summary-text {
      background: rgba(168, 218, 220, 0.1);
      padding: 16px;
      border-radius: 8px;
      border-left: 4px solid #457B9D;
      color: #606266;
      line-height: 1.6;
      margin: 12px 0;
      font-size: 14px;
    }
  }

  h3 {
    font-size: 16px;
    color: #303133;
    margin: 20px 0 12px;

    &:first-child {
      margin-top: 0;
    }
  }

  .duty-list,
  .requirement-list {
    padding-left: 20px;
    margin: 0;
    
    li {
      color: #606266;
      line-height: 1.8;
      margin-bottom: 8px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}

.apply-section {
  text-align: center;

  .apply-info {
    text-align: left;
    margin: 20px 0;

    p {
      display: flex;
      align-items: center;
      color: #606266;
      margin: 8px 0;

      .el-icon {
        margin-right: 8px;
      }
    }
  }
}
</style> 