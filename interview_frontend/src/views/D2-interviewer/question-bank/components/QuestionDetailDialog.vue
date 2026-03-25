<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="题目详情"
    width="700px">
    
    <div v-if="question" class="question-detail">
      <!-- 基本信息 -->
      <div class="section">
        <h3>基本信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="题目ID">
            {{ question.id }}
          </el-descriptions-item>
          <el-descriptions-item label="题目类型">
            <el-tag :type="getTypeTagType(question.type)">
              {{ getTypeLabel(question.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag :type="getDifficultyTagType(question.difficulty)">
              {{ getDifficultyLabel(question.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="question.isActive ? 'success' : 'danger'">
              {{ question.isActive ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="大类别">
            {{ question.category || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="细分类型">
            {{ question.categoryType || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="使用次数">
            {{ question.usageCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="标签">
            <div v-if="question.tags">
              <el-tag 
                v-for="tag in getTagList(question.tags)" 
                :key="tag" 
                size="small" 
                style="margin-right: 8px;">
                {{ tag }}
              </el-tag>
            </div>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <!-- 题目内容 -->
      <div class="section">
        <h3>题目内容</h3>
        <div class="content-box">
          <pre class="question-content">{{ question.content }}</pre>
        </div>
      </div>
      
      <!-- 选项（基础题中的选择题） -->
      <div v-if="question.type === 'basic' && question.options" class="section">
        <h3>选项</h3>
        <div class="options-list">
          <div 
            v-for="(option, index) in getOptionList(question.options)" 
            :key="index"
            class="option-item"
            :class="{ 'correct-option': isCorrectOption(index, question.correctAnswer) }">
            <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
            <span class="option-text">{{ option }}</span>
            <el-tag 
              v-if="isCorrectOption(index, question.correctAnswer)" 
              type="success" 
              size="small"
              class="correct-answer-badge">
              ✓ 正确答案
            </el-tag>
          </div>
        </div>
      </div>
      
      <!-- 参考答案 -->
      <div v-if="question.correctAnswer && (question.type !== 'basic' || !question.options)" class="section">
        <h3>参考答案</h3>
        <div class="content-box">
          <pre class="answer-content">{{ question.correctAnswer }}</pre>
        </div>
      </div>
      
      <!-- 时间信息 -->
      <div class="section">
        <h3>时间信息</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(question.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(question.updateTime) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="question.createdByName || question.createdBy" label="创建者">
            {{ question.createdByName || `ID: ${question.createdBy}` }}
          </el-descriptions-item>
          <el-descriptions-item v-if="question.updatedByName || question.updatedBy" label="更新者">
            {{ question.updatedByName || `ID: ${question.updatedBy}` }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
const props = defineProps({
  visible: Boolean,
  question: Object
})

const emit = defineEmits(['update:visible'])

function handleClose() {
  emit('update:visible', false)
}

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

function getTagList(tags) {
  if (!tags) return []
  return tags.split(',').map(tag => tag.trim()).filter(tag => tag)
}

function getOptionList(options) {
  if (!options) return []
  try {
    return JSON.parse(options)
  } catch (e) {
    return []
  }
}

function isCorrectOption(index, correctAnswer) {
  if (!correctAnswer) return false
  const optionLetter = String.fromCharCode(65 + index)
  
  // 首先尝试按选项字母匹配
  if (optionLetter === correctAnswer.toUpperCase()) {
    return true
  }
  
  // 如果不匹配，尝试按选项内容匹配
  const options = getOptionList(props.question.options)
  if (options && options[index]) {
    const optionText = options[index].toString().toLowerCase()
    const correctText = correctAnswer.toLowerCase()
    return optionText.includes(correctText) || correctText.includes(optionText)
  }
  
  return false
}
</script>

<style scoped>
.question-detail {
  max-height: 70vh;
  overflow-y: auto;
}

.section {
  margin-bottom: 24px;
}

.section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 8px;
}

.content-box {
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 4px;
  padding: 16px;
}

.question-content,
.answer-content {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
}

.options-list {
  space-y: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 8px;
  background-color: #fafafa;
}

.option-item.correct-option {
  background-color: #f0f9ff;
  border: 1px solid #67c23a;
  border-left: 4px solid #67c23a;
}





.option-label {
  font-weight: 600;
  color: #606266;
  margin-right: 12px;
  min-width: 20px;
}

.option-text {
  flex: 1;
  color: #303133;
}

.dialog-footer {
  display: flex;
  justify-content: center;
}

:deep(.el-descriptions__label) {
  font-weight: 500;
}

:deep(.el-descriptions__content) {
  color: #303133;
}

.correct-answer-badge {
  margin-left: 12px;
}
</style>