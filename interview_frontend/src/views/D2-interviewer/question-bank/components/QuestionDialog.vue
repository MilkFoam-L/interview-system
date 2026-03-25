<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    :title="isEdit ? '编辑题目' : '新增题目'"
    width="800px"
    :before-close="handleCancel">
    
    <el-form 
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px">
      
      <el-form-item label="题目类型" prop="type">
        <el-select v-model="form.type" placeholder="选择题目类型" style="width: 100%">
          <el-option label="基础题" value="basic" />
          <el-option label="编程题" value="code" />
          <el-option label="算法题" value="algorithm" />
          <el-option label="系统设计题" value="system_design" />
          <el-option label="项目经验题" value="project" />
          <el-option label="行为面试题" value="behavioral" />
        </el-select>
      </el-form-item>
      
      <el-form-item label="题目内容" prop="content">
        <el-input 
          v-model="form.content"
          type="textarea"
          :rows="4"
          placeholder="请输入题目内容"
          @focus="userInteracted.content = true" />
      </el-form-item>
      
      <el-form-item v-if="form.type === 'basic'" label="选项" prop="options">
        <div v-for="(option, index) in optionList" :key="index" class="option-item">
          <el-input 
            v-model="option.text"
            :placeholder="`选项 ${String.fromCharCode(65 + index)}`"
            style="margin-bottom: 8px;"
            @focus="userInteracted.options = true">
            <template #prepend>{{ String.fromCharCode(65 + index) }}</template>
            <template #append>
              <el-button 
                @click="removeOption(index)"
                :disabled="optionList.length <= 2"
                size="small"
                type="danger">删除</el-button>
            </template>
          </el-input>
        </div>
        <el-button @click="addOption" size="small" type="primary" style="margin-top: 8px">
          添加选项
        </el-button>
      </el-form-item>
      
      <el-form-item v-if="form.type === 'basic'" label="正确答案" prop="correctAnswer">
        <el-select 
          v-model="form.correctAnswer" 
          placeholder="选择正确答案" 
          style="width: 100%"
          @focus="userInteracted.correctAnswer = true">
          <el-option 
            v-for="(option, index) in optionList" 
            :key="index"
            :label="`${String.fromCharCode(65 + index)}. ${option.text}`"
            :value="String.fromCharCode(65 + index)" />
        </el-select>
      </el-form-item>
      
      <!-- 编程题、算法题的参考答案 -->
      <el-form-item v-if="['code', 'algorithm'].includes(form.type)" label="参考答案" prop="correctAnswer">
        <el-input 
          v-model="form.correctAnswer"
          type="textarea"
          :rows="6"
          placeholder="请输入参考代码或解题思路"
          @focus="userInteracted.correctAnswer = true" />
      </el-form-item>
      
      <!-- 系统设计题的参考答案 -->
      <el-form-item v-if="form.type === 'system_design'" label="设计要点" prop="correctAnswer">
        <el-input 
          v-model="form.correctAnswer"
          type="textarea"
          :rows="8"
          placeholder="请输入系统设计的关键要点、架构图描述、技术选型等"
          @focus="userInteracted.correctAnswer = true" />
      </el-form-item>
      
      <!-- 项目经验题的参考答案 -->
      <el-form-item v-if="form.type === 'project'" label="考察要点" prop="correctAnswer">
        <el-input 
          v-model="form.correctAnswer"
          type="textarea"
          :rows="6"
          placeholder="请输入该项目题目的考察要点、参考回答等"
          @focus="userInteracted.correctAnswer = true" />
      </el-form-item>
      
      <!-- 行为面试题的参考答案 -->
      <el-form-item v-if="form.type === 'behavioral'" label="评分标准" prop="correctAnswer">
        <el-input 
          v-model="form.correctAnswer"
          type="textarea"
          :rows="6"
          placeholder="请输入行为面试题的评分标准、参考回答等"
          @focus="userInteracted.correctAnswer = true" />
      </el-form-item>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="难度" prop="difficulty">
            <el-select v-model="form.difficulty" placeholder="请选择题目难度等级" style="width: 100%">
              <el-option label="简单" value="EASY" />
              <el-option label="中等" value="MEDIUM" />
              <el-option label="困难" value="HARD" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="题目标签" prop="tags">
            <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="大类别" prop="category">
            <el-input 
              v-model="form.category" 
              placeholder="如：前端开发"
              @focus="userInteracted.category = true" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="细分类型" prop="categoryType">
            <el-input 
              v-model="form.categoryType" 
              placeholder="如：Vue.js"
              @focus="userInteracted.categoryType = true" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          {{ isEdit ? '更新' : '创建' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  question: Object,
  isEdit: Boolean
})

const emit = defineEmits(['update:visible', 'submit', 'cancel'])

const formRef = ref()
const submitting = ref(false)
const optionList = ref([
  { text: '' },
  { text: '' }
])

const form = reactive({
  type: 'basic',
  content: '',
  inputType: '',
  options: '',
  correctAnswer: '',
  difficulty: 'MEDIUM',
  tags: '',
  category: '',
  categoryType: ''
})

// 用于跟踪用户是否已经与字段进行过交互
const userInteracted = ref({
  content: false,
  correctAnswer: false,
  category: false,
  categoryType: false,
  options: false
})

const rules = computed(() => {
  const baseRules = {
    type: [{ required: true, message: '请选择题目类型', trigger: 'change' }],
    content: [
      {
        validator: (rule, value, callback) => {
          if (!userInteracted.value.content && !value) {
            callback() // 用户还没有交互过，不显示错误
            return
          }
          if (!value || !value.trim()) {
            callback(new Error('请输入题目内容'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ],
    difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
    category: [
      {
        validator: (rule, value, callback) => {
          if (!userInteracted.value.category && !value) {
            callback()
            return
          }
          if (!value || !value.trim()) {
            callback(new Error('请输入大类别'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ],
    categoryType: [
      {
        validator: (rule, value, callback) => {
          if (!userInteracted.value.categoryType && !value) {
            callback()
            return
          }
          if (!value || !value.trim()) {
            callback(new Error('请输入细分类型'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ]
  }
  
  // 为基础题添加选项和正确答案验证
  if (form.type === 'basic') {
    baseRules.options = [
      {
        validator: (rule, value, callback) => {
          // 只有当用户已经开始编辑选项时才进行验证
          const hasEditedOptions = optionList.value.some(opt => opt.text.trim())
          if (!userInteracted.value.options && !hasEditedOptions) {
            callback() // 如果还没有编辑过选项，不进行验证
            return
          }
          
          const validOptions = optionList.value.filter(opt => opt.text.trim())
          if (validOptions.length < 2) {
            callback(new Error('至少需要2个选项'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ]
    baseRules.correctAnswer = [
      {
        validator: (rule, value, callback) => {
          if (!userInteracted.value.correctAnswer && !value) {
            callback()
            return
          }
          if (!value) {
            callback(new Error('请选择正确答案'))
          } else {
            callback()
          }
        },
        trigger: ['change', 'blur']
      }
    ]
  }
  
  // 为其他类型题目添加参考答案验证
  if (['code', 'algorithm', 'system_design', 'project', 'behavioral'].includes(form.type)) {
    baseRules.correctAnswer = [
      {
        validator: (rule, value, callback) => {
          if (!userInteracted.value.correctAnswer && !value) {
            callback()
            return
          }
          if (!value || !value.trim()) {
            callback(new Error('请输入参考答案'))
          } else {
            callback()
          }
        },
        trigger: 'blur'
      }
    ]
  }
  
  return baseRules
})

// 监听题目类型变化，清除验证状态
watch(() => form.type, () => {
  // 重置相关字段的交互状态
  userInteracted.value.correctAnswer = false
  userInteracted.value.options = false
  
  nextTick(() => {
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  })
})

// 监听对话框显示状态
watch(() => props.visible, (newVal) => {
  if (newVal) {
    // 首先清除表单验证状态
    nextTick(() => {
      formRef.value?.clearValidate()
    })
    
    if (props.isEdit && props.question) {
      // 编辑模式，回填数据
      Object.assign(form, {
        ...props.question,
        tags: props.question.tags || '',
        // 确保难度值是大写格式（数据库存储的是小写）
        difficulty: (props.question.difficulty || 'medium').toUpperCase()
      })
      
      // 处理选项数据（基础题类型可能包含选择题）
      if (form.type === 'basic' && form.options) {
        try {
          const options = JSON.parse(form.options)
          optionList.value = options.map(opt => ({ text: opt }))
        } catch (e) {
          optionList.value = [{ text: '' }, { text: '' }]
        }
      }
    } else {
      // 新增模式，重置表单
      resetForm()
    }
  }
})

// 监听题目类型变化
watch(() => form.type, (newType) => {
  // inputType将在提交时根据实际情况设置
  // 基础题：有选项时为radio，无选项时为text
  // 其他类型：直接使用type值
  
  // 重置相关字段
  if (newType !== 'basic') {
    form.options = ''
    optionList.value = [{ text: '' }, { text: '' }]
    form.correctAnswer = ''
  }
})

function resetForm() {
  Object.assign(form, {
    type: 'basic',
    content: '',
    inputType: '',  // inputType将在提交时设置
    options: '',
    correctAnswer: '',
    difficulty: 'MEDIUM',
    tags: '',
    category: '',
    categoryType: ''
  })
  optionList.value = [{ text: '' }, { text: '' }]
  
  // 重置用户交互状态
  Object.assign(userInteracted.value, {
    content: false,
    correctAnswer: false,
    category: false,
    categoryType: false,
    options: false
  })
}

function addOption() {
  if (optionList.value.length < 6) {
    optionList.value.push({ text: '' })
  } else {
    console.error('最多支持6个选项')
  }
}

function removeOption(index) {
  if (optionList.value.length > 2) {
    optionList.value.splice(index, 1)
  }
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
    
    submitting.value = true
    
    const submitData = { ...form }
    
    // 处理选项数据（基础题可能包含选择题）
    if (form.type === 'basic') {
      const options = optionList.value.filter(opt => opt.text.trim())
      if (options.length >= 2) {
        // 有选项时，验证是否选择了正确答案
        if (!form.correctAnswer) {
          console.error('请选择正确答案')
          submitting.value = false
          return
        }
        submitData.options = JSON.stringify(options.map(opt => opt.text.trim()))
      } else {
        // 没有选项时，清空选项和答案
        submitData.options = ''
        if (optionList.value.some(opt => opt.text.trim())) {
          console.error('至少需要2个选项')
          submitting.value = false
          return
        }
      }
    }
    
    // 设置inputType - 基础题选择题类型设为radio，与现有数据保持一致
    if (form.type === 'basic' && submitData.options) {
      submitData.inputType = 'radio'  // 选择题类型
    } else if (form.type === 'basic') {
      submitData.inputType = 'text'   // 简答题类型
    } else {
      submitData.inputType = form.type
    }
    
    // 设置difficulty为小写格式，与现有数据保持一致
    submitData.difficulty = form.difficulty.toLowerCase()
    
    emit('submit', submitData)
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  emit('update:visible', false)
  emit('cancel')
}
</script>

<style scoped>
.option-item {
  margin-bottom: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}
</style>