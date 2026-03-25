<template>
  <div class="position-list-container">
    <!-- 标题 -->
    <div class="page-header-optimized">
      <div class="header-content">
        <div class="title-section">
          <h1 class="main-title">岗位管理</h1>
          <p class="sub-title">编辑查看所有岗位，管理岗位开放状态</p>
        </div>
      </div>
    </div>

    <div class="main-content">
      <!-- 左侧筛选区域 -->
      <div class="filter-section">
        <el-card class="filter-card">
          <template #header>
            <div class="filter-header">
              <span class="filter-title">筛选</span>
              <el-button text @click="resetFilters" class="clear-btn">清除</el-button>
            </div>
          </template>
          
          <div class="filter-content">
            <h3>岗位分类</h3>
            <el-tree
              :data="departments"
              :props="{ label: 'name' }"
              @node-click="handleDepartmentSelect"
            />

            <h3>工作类型</h3>
            <el-checkbox-group v-model="selectedJobTypes" @change="handleFilterChange">
              <el-checkbox :label="'全职'">全职</el-checkbox>
              <el-checkbox :label="'实习'">实习</el-checkbox>
              <el-checkbox :label="'兼职'">兼职</el-checkbox>
            </el-checkbox-group>

            <h3>工作地点</h3>
            <el-select v-model="selectedLocation" placeholder="选择城市" style="width: 100%" @change="handleFilterChange">
              <el-option label="广州" value="广州" />
              <el-option label="北京" value="北京" />
              <el-option label="上海" value="上海" />
              <el-option label="深圳" value="深圳" />
            </el-select>
          </div>
        </el-card>
      </div>

      <!-- 右侧列表区域 -->
      <div class="list-section">
        <el-card class="search-card">
          <div class="search-bar-container">
            <div class="search-bar">
              <el-input
                v-model="searchKeyword"
                placeholder="请输入岗位名称、关键词进行搜索"
                :prefix-icon="Search"
                class="search-input"
                @keyup.enter.native="handleSearch"
              >
                <template #append>
                  <el-button type="primary" class="search-btn" @click="handleSearch">搜索</el-button>
                </template>
              </el-input>
            </div>
            <el-button type="primary" size="large" @click="handleAddPosition" class="add-btn-optimized">
              <el-icon><Plus /></el-icon>新增岗位
            </el-button>
          </div>
        </el-card>

        <!-- 岗位列表 -->
        <el-card class="list-card">
          <div class="position-list-optimized">
            <div v-for="position in positions" :key="position.id" class="position-item-row">
              <div class="position-info-left">
                <div class="position-header-row">
                  <h2 class="position-title-large">{{ position.title }}</h2>
                </div>
                <div class="position-meta-row">
                  <el-tag size="small" effect="plain" class="meta-tag">{{ position.category || '-' }}</el-tag>
                  <el-tag size="small" type="success" effect="plain" class="meta-tag">{{ position.categoryType || '-' }}</el-tag>
                  <el-tag size="small" type="info" effect="plain" class="meta-tag">{{ position.location || '-' }}</el-tag>
                  <el-tag :type="position.status === 0 ? 'primary' : 'danger'" size="small" effect="plain" class="meta-tag">{{ position.status === 0 ? '开放' : '关闭' }}</el-tag>
                </div>
                <div class="position-desc-optimized">{{ position.description }}</div>
              </div>

              <div class="position-actions-right">
                <el-button @click="handleViewDetail(position.id)" class="detail-btn-optimized">详情</el-button>
                <el-button @click="handleEdit(position.id)" class="edit-btn-optimized">修改</el-button>
              </div>
            </div>
          </div>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 30, 50]"
              layout="prev, pager, next"
              :pager-count="5"
              background
              class="pagination-optimized"
              @current-change="handlePageChange"
              @size-change="handleSizeChange"
            />
          </div>
        </el-card>
      </div>
    </div>

    <PositionForm 
      v-model:visible="isFormVisible"
      :position-data="currentPosition"
      :readonly="isReadOnly"
      @submit="handleFormSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
// import { useRouter } from 'vue-router'
import PositionForm from './PositionForm.vue'
import { ElMessage } from 'element-plus'
import { fetchMyJobsPage, fetchMyDepartments, createJob, updateJob, deleteJob } from '@/api/jobs'

// const router = useRouter()

const departments = ref([])
const positions = ref([])

const isFormVisible = ref(false)
const currentPosition = ref(null)
const isReadOnly = ref(false)

// 状态管理
const searchKeyword = ref('')
const selectedJobTypes = ref<string[]>([])
const selectedLocation = ref('')
const selectedCategory = ref<string | null>(null)
const selectedCategoryType = ref<string | null>(null)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const workTypeMap: Record<string, number> = {
  '全职': 0,
  '实习': 1,
  '兼职': 2
}

const loadDepartments = async () => {
  try {
    const data = await fetchMyDepartments()
    departments.value = Array.isArray(data) ? data : []
  } catch (e) {
    departments.value = []
  }
}

const loadPositions = async () => {
  try {
    const params: any = {
      page: currentPage.value - 1,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      location: selectedLocation.value || undefined,
      category: selectedCategory.value || undefined,
      categoryType: selectedCategoryType.value || undefined
    }

    if (selectedJobTypes.value.length > 0) {
      const mapped = workTypeMap[selectedJobTypes.value[0]]
      if (typeof mapped === 'number') params.workType = mapped
    }

    const page: any = await fetchMyJobsPage(params)
    positions.value = page?.content || []
    total.value = page?.totalElements || 0
  } catch (e) {
    positions.value = []
    total.value = 0
  }
}

const resetFilters = () => {
  selectedJobTypes.value = []
  selectedLocation.value = ''
  selectedCategory.value = null
  selectedCategoryType.value = null
  searchKeyword.value = ''
  currentPage.value = 1
  loadPositions()
}

const handleDepartmentSelect = (data: any) => {
  if (data && data.name) {
    if (Array.isArray(data.children)) {
      selectedCategory.value = data.name
      selectedCategoryType.value = null
    } else {
      selectedCategoryType.value = data.name
      const parent = departments.value.find((d:any)=>Array.isArray(d.children)&&d.children.some((c:any)=>c.name===data.name))
      selectedCategory.value = parent ? parent.name : selectedCategory.value
    }
  }
  currentPage.value = 1
  loadPositions()
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadPositions()
}

const handleSearch = () => {
  currentPage.value = 1
  loadPositions()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadPositions()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadPositions()
}

const handleAddPosition = () => {
  currentPosition.value = null
  isReadOnly.value = false
  isFormVisible.value = true
}

const handleViewDetail = (id: number) => {
  const positionToView = positions.value.find((p:any) => p.id === id)
  currentPosition.value = positionToView ? { ...positionToView } : null
  isReadOnly.value = true
  isFormVisible.value = true
}

const handleEdit = (id: number) => {
  const positionToEdit = positions.value.find((p:any) => p.id === id)
  currentPosition.value = positionToEdit ? { ...positionToEdit } : null
  isReadOnly.value = false
  isFormVisible.value = true
}

const handleFormSubmit = async (formData: any) => {
  try {
    if (formData.id) {
      // 更新岗位
      const { data } = await updateJob(formData.id, formData)
      if (data.success) {
        ElMessage.success(data.message || '岗位更新成功！')
      } else {
        ElMessage.error(data.message || '更新失败')
        return
      }
    } else {
      // 创建岗位
      const { data } = await createJob(formData)
      if (data.success) {
        ElMessage.success(data.message || '岗位创建成功！')
      } else {
        ElMessage.error(data.message || '创建失败')
        return
      }
    }
    
    // 刷新列表
    loadPositions()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  }
}

onMounted(() => {
  loadDepartments()
  loadPositions()
})
</script>

<style scoped>
.position-list-container {
  padding: 20px;
}

.page-header-optimized {
  margin-bottom: 24px;
  padding: 32px 0;
  background: transparent;
}

.header-content {
  display: flex;
  justify-content: center;
  align-items: center;
}

.title-section {
  text-align: center;
}

.main-title {
  font-size: 40px;
  margin-bottom: 8px;
  color: #ffffff;
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.sub-title {
  color: rgba(255, 255, 255, 0.9);
  font-size: 16px;
  font-weight: 400;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.add-btn-optimized {
  flex-shrink: 0;
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border: none !important;
  color: #F1FAEE !important;
  border-radius: 20px !important;
  padding: 12px 24px !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 4px 16px rgba(38, 70, 83, 0.35) !important;
  letter-spacing: 0.5px;

  &:hover {
    transform: translateY(-3px) scale(1.03) !important;
    box-shadow: 0 10px 30px rgba(38, 70, 83, 0.45) !important;
    background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important;
  }
}

.main-content {
  display: flex;
  gap: 20px;
  min-height: calc(100vh - 180px);
}

.filter-section {
  width: 260px;
  flex-shrink: 0;
}

.filter-card {
  height: 100%;
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-content {
  h3 {
    margin: 18px 0 10px;
    font-size: 13px;
    color: #264653;
    font-weight: 600;
    letter-spacing: 0.3px;
    position: relative;
    padding-left: 10px;
  }

  h3::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 4px;
    height: 14px;
    border-radius: 2px;
    background: linear-gradient(135deg, #457B9D 0%, #264653 100%);
  }
}

.list-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.search-card {
  padding: 16px;
}

.search-bar-container {
  display: flex;
  gap: 16px;
  align-items: center;
}

.search-bar {
  flex: 1;
}

.search-input {
  width: 100%;
}

.search-btn {
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border: none !important;
  color: #F1FAEE !important;
  border-radius: 0 4px 4px 0 !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 2px 8px rgba(38, 70, 83, 0.25) !important;
  
  &:hover {
    background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important;
    transform: translateY(-1px) !important;
    box-shadow: 0 4px 12px rgba(38, 70, 83, 0.35) !important;
  }
}

.list-card {
  flex: 1;
}

.position-list-optimized {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 20px;
}

.position-item-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 24px;
  background-color: #fff;
  border: 1px solid rgba(168, 218, 220, 0.3);
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 20px rgba(38, 70, 83, 0.08);
  position: relative;
  overflow: hidden;
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 12px 40px rgba(69, 123, 157, 0.15);
    border-color: #457B9D;
  }
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, #457B9D, #264653);
    transform: scaleX(0);
    transition: transform 0.3s ease;
    transform-origin: left;
  }
  
  &:hover::before {
    transform: scaleX(1);
  }
}

.position-info-left {
  flex: 1;
}

.position-header-row {
  margin-bottom: 8px;
}

.position-title-large {
  font-size: 20px;
  color: #264653;
  margin: 0 0 8px 0;
  font-weight: 700;
  line-height: 1.3;
}

.position-meta-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  
  .meta-tag {
    font-size: 12px;
    border-radius: 12px;
    padding: 4px 12px;
    background: rgba(168, 218, 220, 0.15);
    border: 1px solid rgba(168, 218, 220, 0.3);
    color: #457B9D;
    font-weight: 500;
  }
}

.position-desc-optimized {
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
  margin-top: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.position-actions-right {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-top: 8px;
}

.detail-btn-optimized {
  padding: 8px 20px;
  font-size: 14px;
  border-radius: 20px;
  border: 1px solid #457B9D;
  color: #457B9D;
  background-color: rgba(168, 218, 220, 0.1);
  font-weight: 500;
  transition: all 0.3s ease;

  &:hover {
    background-color: #457B9D;
    border-color: #457B9D;
    color: #F1FAEE;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(69, 123, 157, 0.3);
  }
}

.edit-btn-optimized {
  padding: 8px 20px;
  font-size: 14px;
  border-radius: 20px;
  border: 1px solid #264653;
  color: #264653;
  background-color: rgba(38, 70, 83, 0.1);
  font-weight: 500;
  transition: all 0.3s ease;

  &:hover {
    background-color: #264653;
    border-color: #264653;
    color: #F1FAEE;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(38, 70, 83, 0.3);
  }
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(25px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.pagination-optimized {
  font-weight: 600;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
  padding: 0;
  gap: 8px 0;
}

:deep(.pagination-optimized .el-pager) {
  display: flex;
  flex-wrap: wrap;
  row-gap: 8px;
}

:deep(.pagination-optimized .el-pager li) {
  background: rgba(168, 218, 220, 0.15);
  border: 1px solid rgba(168, 218, 220, 0.3);
  color: #457B9D;
  margin: 0 4px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.pagination-optimized .el-pager li:hover) {
  background: #457B9D;
  color: #F1FAEE;
  transform: scale(1.1);
}

:deep(.pagination-optimized .el-pager li.is-active) {
  background: #457B9D;
  color: #F1FAEE;
  box-shadow: 0 4px 12px rgba(69, 123, 157, 0.3);
}

:deep(.pagination-optimized .btn-prev),
:deep(.pagination-optimized .btn-next) {
  background: rgba(168, 218, 220, 0.15);
  border: 1px solid rgba(168, 218, 220, 0.3);
  color: #457B9D;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.pagination-optimized .btn-prev:hover),
:deep(.pagination-optimized .btn-next:hover) {
  background: #457B9D;
  color: #F1FAEE;
  transform: scale(1.1);
}

.filter-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.clear-btn {
  color: #909399;
  font-size: 14px;
  &:hover {
    color: #606266;
  }
}
</style>