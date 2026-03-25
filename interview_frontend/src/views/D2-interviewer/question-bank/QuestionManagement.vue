<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1>题目管理</h1>
      <p>增删改查题目，支持批量导入</p>
    </div>
    
    <!-- 搜索和操作栏 -->
    <div class="search-toolbar">
      <el-card>
        <el-row :gutter="20" align="middle">
          <el-col :span="4">
            <el-select v-model="searchForm.category" placeholder="选择大类" clearable @change="handleSearch">
              <el-option v-for="cat in categories.categories" :key="cat" :label="cat" :value="cat" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="searchForm.categoryType" placeholder="选择细分类型" clearable @change="handleSearch">
              <el-option v-for="type in categories.categoryTypes" :key="type" :label="type" :value="type" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="searchForm.type" placeholder="选择题目类型" clearable @change="handleSearch">
              <el-option label="基础题" value="basic" />
              <el-option label="编程题" value="code" />
              <el-option label="算法题" value="algorithm" />
              <el-option label="系统设计题" value="system_design" />
              <el-option label="项目经验题" value="project" />
              <el-option label="行为面试题" value="behavioral" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="searchForm.difficulty" placeholder="选择难度" clearable @change="handleSearch">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-input 
              v-model="searchForm.keyword" 
              placeholder="搜索题目内容或标签"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
              clearable
              class="search-input-optimized" />
          </el-col>
          <el-col :span="1">
            <el-button @click="handleSearch" class="search-btn-optimized">搜索</el-button>
          </el-col>
          <el-col :span="1">
            <el-button @click="resetSearch" class="reset-btn-optimized">重置</el-button>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 题目列表 -->
    <div class="table-container">
      <el-card>
        <!-- 操作按钮栏 -->
        <div class="action-toolbar-inner">
          <el-row justify="space-between">
            <el-col :span="12">
              <el-button type="primary" @click="showCreateDialog">
                <el-icon><Plus /></el-icon>
                新增题目
              </el-button>
              <el-button @click="showUploadDialog">
                <el-icon><Upload /></el-icon>
                批量导入
              </el-button>
              <el-button @click="downloadTemplate">
                <el-icon><Download /></el-icon>
                下载模板
              </el-button>
              <el-button 
                type="danger" 
                :disabled="selectedQuestions.length === 0"
                @click="handleBatchDelete">
                <el-icon><Delete /></el-icon>
                批量删除
              </el-button>
            </el-col>
            <el-col :span="12" style="text-align: right;">
              <el-button @click="showStatistics">
                <el-icon><DataAnalysis /></el-icon>
                使用统计
              </el-button>
            </el-col>
          </el-row>
        </div>
        <el-table 
          v-loading="loading"
          :data="questions" 
          @selection-change="handleSelectionChange"
          :default-sort="{ prop: 'id', order: 'ascending' }"
          style="width: 100%">
          
          <el-table-column type="selection" width="55" />
          
          <el-table-column prop="id" label="ID" width="80" sortable />
          
          <el-table-column prop="content" label="题目内容" min-width="200" show-overflow-tooltip />
          
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getTypeTagType(row.type)">{{ getTypeLabel(row.type) }}</el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="difficulty" label="难度" width="100">
            <template #default="{ row }">
              <el-tag :type="getDifficultyTagType(row.difficulty)">{{ getDifficultyLabel(row.difficulty) }}</el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="category" label="大类" width="120" show-overflow-tooltip />
          
          <el-table-column prop="categoryType" label="细分类型" width="120" show-overflow-tooltip />
          
          <el-table-column prop="usageCount" label="使用次数" width="100" />
          
          <el-table-column prop="isActive" label="状态" width="120">
            <template #default="{ row }">
              <el-switch 
                v-model="row.isActive" 
                @change="handleToggleActive(row)"
                active-text="启用"
                inactive-text="禁用"
                inline-prompt />
            </template>
          </el-table-column>
          
          <el-table-column prop="createTime" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="showQuestionDetail(row)">查看</el-button>
              <el-button size="small" type="primary" @click="showEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange" />
        </div>
      </el-card>
    </div>

    <!-- 题目创建/编辑对话框 -->
    <QuestionDialog 
      v-model:visible="dialogVisible"
      :question="currentQuestion"
      :is-edit="isEdit"
      @submit="handleQuestionSubmit"
      @cancel="handleDialogCancel" />

    <!-- 批量上传对话框 -->
    <UploadDialog 
      v-model:visible="uploadDialogVisible"
      @success="handleUploadSuccess" />

    <!-- 题目详情对话框 -->
    <QuestionDetailDialog 
      v-model:visible="detailDialogVisible"
      :question="detailQuestion" />

    <!-- 统计对话框 -->
    <StatisticsDialog 
      ref="statisticsDialogRef"
      v-model:visible="statisticsDialogVisible" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Delete, DataAnalysis } from '@element-plus/icons-vue'
import { 
  getQuestions, 
  createQuestion, 
  updateQuestion, 
  deleteQuestion, 
  batchDeleteQuestions,
  getQuestionCategories,
  getQuestionDetailById,
  toggleQuestionActive,
  downloadExcelTemplate
} from '../../../api/interviewer/questions'
import QuestionDialog from './components/QuestionDialog.vue'
import UploadDialog from './components/UploadDialog.vue'
import QuestionDetailDialog from './components/QuestionDetailDialog.vue'
import StatisticsDialog from './components/StatisticsDialog.vue'

// 响应式数据
const loading = ref(false)
const questions = ref([])
const selectedQuestions = ref([])
const categories = ref({
  categories: [],
  categoryTypes: []
})

// 搜索表单
const searchForm = reactive({
  category: '',
  categoryType: '',
  type: '',
  difficulty: '',
  keyword: ''
})

// 分页
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// 对话框状态
const dialogVisible = ref(false)
const uploadDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const statisticsDialogVisible = ref(false)
const isEdit = ref(false)
const currentQuestion = ref(null)
const detailQuestion = ref(null)
const statisticsDialogRef = ref(null)

onMounted(() => {
  loadCategories()
  loadQuestions()
})

// 加载类别选项
async function loadCategories() {
  try {
    const result = await getQuestionCategories()
    categories.value = result
  } catch (error) {
    console.error('加载类别失败:', error)
  }
}

// 加载题目列表
async function loadQuestions() {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1, // 后端从0开始
      size: pagination.size,
      sort: 'id,asc', // 添加默认按ID升序排序
      ...searchForm
    }
    
    const result = await getQuestions(params)
    questions.value = result.content || []
    pagination.total = result.totalElements || 0
  } catch (error) {
    console.error('加载题目失败:', error)
    console.error('加载题目失败')
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadQuestions()
}

// 重置
function resetSearch() {
  Object.assign(searchForm, {
    category: '',
    categoryType: '',
    type: '',
    difficulty: '',
    keyword: ''
  })
  handleSearch()
}

// 分页处理
function handlePageChange(page) {
  pagination.page = page
  loadQuestions()
}

function handleSizeChange(size) {
  pagination.size = size
  pagination.page = 1
  loadQuestions()
}

// 选择处理
function handleSelectionChange(selection) {
  selectedQuestions.value = selection
}

// 新增题目
function showCreateDialog() {
  isEdit.value = false
  currentQuestion.value = null
  dialogVisible.value = true
}

// 编辑题目
function showEditDialog(question) {
  isEdit.value = true
  currentQuestion.value = { ...question }
  dialogVisible.value = true
}

// 题目提交处理
async function handleQuestionSubmit(questionData) {
  try {
    if (isEdit.value) {
      await updateQuestion(currentQuestion.value.id, questionData)
      console.log('更新题目成功')
    } else {
      await createQuestion(questionData)
      console.log('创建题目成功')
    }
    
    dialogVisible.value = false
    loadQuestions()

    if (statisticsDialogVisible.value) {
      nextTick(() => {
        if (statisticsDialogRef.value) {
          statisticsDialogRef.value.refreshData()
        }
      })
    }
  } catch (error) {
    console.error('题目操作失败:', error)
    console.error(isEdit.value ? '更新题目失败' : '创建题目失败')
  }
}

// 对话框取消
function handleDialogCancel() {
  dialogVisible.value = false
  currentQuestion.value = null
}

// 删除题目
async function handleDelete(question) {
  try {
    await ElMessageBox.confirm(
      `确定要删除题目"${question.content.substring(0, 50)}..."吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await deleteQuestion(question.id)
    console.log('删除成功')
    loadQuestions()

    if (statisticsDialogVisible.value) {
      nextTick(() => {
        if (statisticsDialogRef.value) {
          statisticsDialogRef.value.refreshData()
        }
      })
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      console.error('删除失败')
    }
  }
}

// 批量删除
async function handleBatchDelete() {
  if (selectedQuestions.value.length === 0) {
    console.error('请选择要删除的题目')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedQuestions.value.length} 个题目吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    const questionIds = selectedQuestions.value.map(q => q.id)
    const result = await batchDeleteQuestions(questionIds)
    
    console.log(`成功删除 ${result.successCount} 个题目`)
    selectedQuestions.value = []
    loadQuestions()

    if (statisticsDialogVisible.value) {
      nextTick(() => {
        if (statisticsDialogRef.value) {
          statisticsDialogRef.value.refreshData()
        }
      })
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      console.error('批量删除失败')
    }
  }
}

// 切换题目状态
async function handleToggleActive(question) {
  try {
    await toggleQuestionActive(question.id)
    console.log(question.isActive ? '题目已启用' : '题目已禁用')
  } catch (error) {
    console.error('切换状态失败:', error)
    console.error('切换状态失败')
    // 回滚状态
    question.isActive = !question.isActive
  }
}

// 显示上传对话框
function showUploadDialog() {
  uploadDialogVisible.value = true
}

// 上传成功处理
function handleUploadSuccess(result) {
  console.log(`成功导入 ${result.successCount} 个题目`)
  uploadDialogVisible.value = false
  loadQuestions()
  
  // 如果统计对话框是打开状态，自动刷新统计数据
  if (statisticsDialogVisible.value) {
    nextTick(() => {
      if (statisticsDialogRef.value) {
        statisticsDialogRef.value.refreshData()
      }
    })
  }
}

// 下载模板
async function downloadTemplate() {
  try {
    console.log('开始下载模板...')
    const blob = await downloadExcelTemplate()
    console.log('API响应:', blob)
    console.log('数据类型:', typeof blob, blob instanceof Blob)
    
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'question_template.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    console.log('模板下载成功')
  } catch (error) {
    console.error('下载模板失败:', error)
    console.error('错误详情:', error.response?.data)
    console.error(`下载模板失败：${error.message || '未知错误'}`)
  }
}

// 显示题目详情
async function showQuestionDetail(question) {
  try {
    const detailData = await getQuestionDetailById(question.id)
    detailQuestion.value = detailData
    detailDialogVisible.value = true
  } catch (error) {
    console.error('获取题目详情失败:', error)
    console.error('获取题目详情失败')
  }
}

// 显示统计
function showStatistics() {
  statisticsDialogVisible.value = true
}

// 工具函数
function formatDateTime(dateTime) {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN')
}

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

function getDifficultyLabel(difficulty) {
  const difficultyMap = {
    'EASY': '简单',
    'MEDIUM': '中等',
    'HARD': '困难',
    'easy': '简单',
    'medium': '中等',
    'hard': '困难'
  }
  return difficultyMap[difficulty] || difficulty
}

function getDifficultyTagType(difficulty) {
  const difficultyMap = {
    'EASY': 'success',
    'MEDIUM': 'warning',
    'HARD': 'danger',
    'easy': 'success',
    'medium': 'warning',
    'hard': 'danger'
  }
  return difficultyMap[difficulty] || 'default'
}
</script>

<style scoped>
.page-container {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #303133;
}

.page-header p {
  color: #606266;
  font-size: 14px;
  margin: 0;
}

.search-toolbar,
.table-container {
  margin-bottom: 0;
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
}

:deep(.el-card__body) {
  padding: 20px;
}

:deep(.el-table .el-table__cell) {
  padding: 12px 0;
}

:deep(.el-pagination) {
  justify-content: flex-end;
}

.action-toolbar-inner {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f2f5;
}

.action-toolbar-inner :deep(.el-button) {
  margin-right: 12px;
}

.action-toolbar-inner :deep(.el-button:last-child) {
  margin-right: 0;
}

.page-header {
  margin-bottom: 0;
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
  margin-top: 20px;
}

.search-input-optimized {
  width: 100%;
}

.search-input-optimized :deep(.el-input__wrapper) {
  border-radius: 20px;
  border: 1px solid rgba(168, 218, 220, 0.5);
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(69, 123, 157, 0.1);
}

.search-input-optimized :deep(.el-input__wrapper:hover) {
  border-color: #457B9D;
  box-shadow: 0 4px 12px rgba(69, 123, 157, 0.2);
}

.search-input-optimized :deep(.el-input__wrapper.is-focus) {
  border-color: #457B9D;
  box-shadow: 0 0 0 2px rgba(69, 123, 157, 0.2);
}

.search-btn-optimized {
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border: none !important;
  color: #F1FAEE !important;
  border-radius: 30px !important;
  font-weight: 600 !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 2px 8px rgba(38, 70, 83, 0.25) !important;
  padding: 10px 20px !important;
  width: 100% !important;
  
  &:hover {
    background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important;
    transform: translateY(-2px) scale(1.05) !important;
    box-shadow: 0 6px 16px rgba(38, 70, 83, 0.4) !important;
  }
}

.reset-btn-optimized {
  background: rgba(168, 218, 220, 0.15) !important;
  border: 1px solid rgba(168, 218, 220, 0.3) !important;
  color: #457B9D !important;
  border-radius: 30px !important;
  font-weight: 500 !important;
  transition: all 0.3s ease !important;
  padding: 10px 20px !important;
  width: 100% !important;
  
  &:hover {
    background: #457B9D !important;
    border-color: #457B9D !important;
    color: #F1FAEE !important;
    transform: translateY(-2px) scale(1.05) !important;
    box-shadow: 0 4px 12px rgba(69, 123, 157, 0.3) !important;
  }
}
</style>