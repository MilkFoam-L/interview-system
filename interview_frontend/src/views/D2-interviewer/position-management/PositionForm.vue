<template>
  <el-dialog
    :model-value="visible"
    :title="readonly ? '岗位详情' : (isEditMode ? '编辑岗位' : '新增岗位')"
    width="60%"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-position="top"
      label-width="100px"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="岗位名称" prop="title">
            <el-input v-model="formData.title" placeholder="请输入岗位名称" :disabled="readonly" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="薪资范围" prop="salary">
            <el-input v-model="formData.salary" placeholder="如：15k-25k" :disabled="readonly" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="岗位类别" prop="category">
            <el-select v-model="formData.category" placeholder="请选择岗位类别" :disabled="readonly">
              <el-option label="人工智能" value="人工智能" />
              <el-option label="大数据" value="大数据" />
              <el-option label="物联网" value="物联网" />
              <el-option label="智能系统" value="智能系统" />
              <el-option label="IT/互联网" value="IT/互联网" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="岗位类型" prop="categoryType">
            <el-input v-model="formData.categoryType" placeholder="如：前端开发、算法工程师" :disabled="readonly" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="工作类型" prop="workType">
            <el-select v-model="formData.workType" placeholder="请选择工作类型" :disabled="readonly">
              <el-option label="全职" :value="0" />
              <el-option label="实习" :value="1" />
              <el-option label="兼职" :value="2" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="工作地点" prop="location">
            <el-select v-model="formData.location" placeholder="请选择工作地点" :disabled="readonly">
              <el-option label="广州" value="广州" />
              <el-option label="北京" value="北京" />
              <el-option label="上海" value="上海" />
              <el-option label="深圳" value="深圳" />
              <el-option label="杭州" value="杭州" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="工作年限" prop="experience">
            <el-input v-model="formData.experience" placeholder="如：1-3年或不限" :disabled="readonly" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="学历要求" prop="education">
            <el-input v-model="formData.education" placeholder="如：本科及以上" :disabled="readonly" />
          </el-form-item>
        </el-col>
         <el-col :span="8">
          <el-form-item label="岗位状态" prop="status">
            <el-radio-group v-model="formData.status" :disabled="readonly">
              <el-radio :label="0">开放</el-radio>
              <el-radio :label="1">关闭</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-form-item label="岗位简介" prop="description">
        <el-input
          type="textarea"
          :rows="3"
          v-model="formData.description"
          placeholder="一句话介绍岗位，将展示在列表页"
          :disabled="readonly"
        />
      </el-form-item>

      <el-form-item label="工作职责" prop="duties">
        <div v-for="(duty, index) in formData.duties" :key="index" class="dynamic-input">
          <el-input v-model="formData.duties[index]" placeholder="请输入工作职责" :disabled="readonly" />
          <el-button v-if="!readonly" @click.prevent="removeDuty(index)" circle class="btn-ink-outline">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button v-if="!readonly" @click="addDuty" plain class="btn-ink-plain">添加职责</el-button>
      </el-form-item>

      <el-form-item label="职位要求" prop="requirements">
        <div v-for="(req, index) in formData.requirements" :key="index" class="dynamic-input">
          <el-input v-model="formData.requirements[index]" placeholder="请输入职位要求" :disabled="readonly" />
          <el-button v-if="!readonly" @click.prevent="removeRequirement(index)" circle class="btn-ink-outline">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button v-if="!readonly" @click="addRequirement" plain class="btn-ink-plain">添加要求</el-button>
      </el-form-item>

    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <template v-if="readonly">
          <el-button @click="handleClose">返回</el-button>
        </template>
        <template v-else>
          <el-button @click="handleClose">取消</el-button>
          <el-button class="btn-ink" @click="submitForm">保存</el-button>
        </template>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

interface PositionFormModel {
  id: number | null
  title: string
  category: string
  categoryType: string
  workType: number
  location: string
  salary: string
  education: string
  experience: string
  status: number
  description: string
  duties: string[]
  requirements: string[]
}

const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  positionData: {
    type: Object,
    default: null
  },
  readonly: {
    type: Boolean,
    default: false
  }
}) as { visible: boolean; positionData: Partial<PositionFormModel> | null; readonly: boolean }

const emit = defineEmits(['update:visible', 'submit'])

const formRef = ref<FormInstance>()

const getInitialFormData = (): PositionFormModel => ({
  id: null,
  title: '',
  category: '',
  categoryType: '',
  workType: 0,
  location: '',
  salary: '',
  education: '',
  experience: '',
  status: 0,
  description: '',
  duties: [''],
  requirements: ['']
})

const formData = ref<PositionFormModel>(getInitialFormData())
const isEditMode = computed(() => !!props.positionData)

// 解析字符串为数组（用于duties和requirements）
const parseStringToArray = (str: string): string[] => {
  if (!str) return ['']
  
  try {
    const parsed = JSON.parse(str)
    if (Array.isArray(parsed)) return parsed.length > 0 ? parsed : ['']
  } catch (e) {
    // 如果不是JSON，按逗号分隔
    const arr = str.split(',').map(item => item.trim()).filter(item => item)
    return arr.length > 0 ? arr : ['']
  }
  
  return [str]
}

// 将数组转换为字符串（用于提交数据）
const parseArrayToString = (arr: string[]): string => {
  const filtered = arr.filter(item => item && item.trim())
  return filtered.length > 0 ? JSON.stringify(filtered) : ''
}

watch(() => props.positionData, (newVal) => {
  if (newVal) {
    const data = { ...getInitialFormData(), ...(newVal as PositionFormModel) }
    
    // 特殊处理duties和requirements字段
    if (typeof newVal.duties === 'string') {
      data.duties = parseStringToArray(newVal.duties)
    }
    if (typeof newVal.requirements === 'string') {
      data.requirements = parseStringToArray(newVal.requirements)
    }
    
    formData.value = data
  } else {
    formData.value = getInitialFormData()
  }
}, { immediate: true, deep: true })

const formRules = ref<FormRules>({
  title: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择岗位类别', trigger: 'change' }],
  categoryType: [{ required: true, message: '请输入岗位类型', trigger: 'blur' }],
  workType: [{ required: true, message: '请选择工作类型', trigger: 'change' }],
  location: [{ required: true, message: '请选择工作地点', trigger: 'change' }],
  status: [{ required: true, message: '请选择岗位状态', trigger: 'change' }],
})

const removeDuty = (index: number) => {
  if (formData.value.duties.length > 1) {
    formData.value.duties.splice(index, 1)
  }
}

const addDuty = () => {
  formData.value.duties.push('')
}

const removeRequirement = (index: number) => {
  if (formData.value.requirements.length > 1) {
    formData.value.requirements.splice(index, 1)
  }
}

const addRequirement = () => {
  formData.value.requirements.push('')
}

const handleClose = () => {
  emit('update:visible', false)
}

const submitForm = () => {
  formRef.value?.validate((valid) => {
    if (valid) {
      // 转换duties和requirements为字符串格式
      const submitData = {
        ...formData.value,
        duties: parseArrayToString(formData.value.duties),
        requirements: parseArrayToString(formData.value.requirements)
      }
      
      emit('submit', submitData)
      handleClose()
    }
  })
}
</script>

<style scoped>
.dynamic-input {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  width: 100%;
}
.dynamic-input .el-input {
  margin-right: 10px;
}

.btn-ink {
  background-color: #264653 !important;
  border-color: #264653 !important;
  color: #F1FAEE !important;
}

.btn-ink:hover {
  filter: brightness(1.05);
}

.btn-ink-plain {
  color: #264653 !important;
  border-color: #264653 !important;
}

.btn-ink-plain:hover {
  background: rgba(38, 70, 83, 0.08) !important;
}

.btn-ink-outline {
  color: #264653 !important;
  border-color: #264653 !important;
}

.btn-ink-outline:hover {
  background: rgba(38, 70, 83, 0.08) !important;
}
</style> 