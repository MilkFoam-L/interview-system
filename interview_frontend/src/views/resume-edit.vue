<template>
  <div class="resume-edit-container">
    <el-card class="resume-edit-card">
      <template #header>
        <div class="card-header">
          <span>编辑简历</span>
          <el-button type="primary" @click="generateResume" :loading="generating">生成简历</el-button>
        </div>
      </template>
      
      <el-form :model="resumeForm" label-width="120px">
        <!-- 基本信息 -->
        <el-divider>基本信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                action="/api/resume/upload-photo"
                :show-file-list="false"
                :on-success="handleAvatarSuccess"
                :before-upload="beforeAvatarUpload"
                :headers="{ 'X-Requested-With': 'XMLHttpRequest' }"
              >
                <img v-if="resumeForm.avatar" :src="resumeForm.avatar" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="姓名">
              <el-input v-model="resumeForm.name" placeholder="请输入姓名"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="年龄">
              <el-input-number v-model="resumeForm.age" :min="16" :max="100"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="电话">
              <el-input v-model="resumeForm.phone" placeholder="请输入联系电话"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="resumeForm.email" placeholder="请输入邮箱"/>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 教育经历 -->
        <el-divider>教育经历</el-divider>
        <div v-for="(edu, index) in resumeForm.education" :key="index" class="education-item">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item :label="'学校名称' + (index + 1)">
                <el-input v-model="edu.school" placeholder="请输入学校名称"/>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="'专业'">
                <el-input v-model="edu.major" placeholder="请输入专业"/>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="'学历'">
                <el-select v-model="edu.degree" placeholder="请选择">
                  <el-option label="专科" value="专科"/>
                  <el-option label="本科" value="本科"/>
                  <el-option label="硕士" value="硕士"/>
                  <el-option label="博士" value="博士"/>
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="16">
              <el-form-item :label="'在校时间'">
                <el-date-picker
                  v-model="edu.timeRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始时间"
                  end-placeholder="结束时间"
                  format="YYYY-MM"
                  value-format="YYYY-MM"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="text-right">
              <el-button type="danger" @click="removeEducation(index)" icon="Delete">删除</el-button>
            </el-col>
          </el-row>
        </div>
        <el-button type="primary" @click="addEducation" icon="Plus">添加教育经历</el-button>

        <!-- 工作经历 -->
        <el-divider>工作经历</el-divider>
        <div v-for="(work, index) in resumeForm.workExperience" :key="index" class="work-item">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item :label="'公司名称' + (index + 1)">
                <el-input v-model="work.company" placeholder="请输入公司名称"/>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="'职位'">
                <el-input v-model="work.position" placeholder="请输入职位"/>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item :label="'在职时间'">
                <el-date-picker
                  v-model="work.timeRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始时间"
                  end-placeholder="结束时间"
                  format="YYYY-MM"
                  value-format="YYYY-MM"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24">
              <el-form-item :label="'工作内容'">
                <el-input
                  v-model="work.description"
                  type="textarea"
                  :rows="3"
                  placeholder="请描述您的工作内容和成就"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="24" class="text-right">
              <el-button type="danger" @click="removeWork(index)" icon="Delete">删除</el-button>
            </el-col>
          </el-row>
        </div>
        <el-button type="primary" @click="addWork" icon="Plus">添加工作经历</el-button>

        <!-- 技能特长 -->
        <el-divider>技能特长</el-divider>
        <el-form-item label="技能描述">
          <el-input
            v-model="resumeForm.skills"
            type="textarea"
            :rows="4"
            placeholder="请描述您的技能特长，例如：编程语言、框架、工具等"
          />
        </el-form-item>

        <!-- 自我评价 -->
        <el-divider>自我评价</el-divider>
        <el-form-item label="自我评价">
          <el-input
            v-model="resumeForm.selfEvaluation"
            type="textarea"
            :rows="4"
            placeholder="请进行自我评价"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 生成结果对话框 -->
    <el-dialog
      v-model="resultDialogVisible"
      title="生成的简历"
      width="70%"
      :close-on-click-modal="false"
    >
      <div v-loading="generating">
        <div v-if="generatedResume" v-html="generatedResume" class="generated-resume"></div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resultDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="saveResume">
            保存简历
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/utils/request'

const resumeForm = reactive({
  avatar: '',
  name: '',
  age: 25,
  phone: '',
  email: '',
  education: [{
    school: '',
    major: '',
    degree: '',
    timeRange: []
  }],
  workExperience: [{
    company: '',
    position: '',
    timeRange: [],
    description: ''
  }],
  skills: '',
  selfEvaluation: ''
})

const generating = ref(false)
const resultDialogVisible = ref(false)
const generatedResume = ref('')

// 添加教育经历
const addEducation = () => {
  resumeForm.education.push({
    school: '',
    major: '',
    degree: '',
    timeRange: []
  })
}

// 删除教育经历
const removeEducation = (index) => {
  resumeForm.education.splice(index, 1)
}

// 添加工作经历
const addWork = () => {
  resumeForm.workExperience.push({
    company: '',
    position: '',
    timeRange: [],
    description: ''
  })
}

// 删除工作经历
const removeWork = (index) => {
  resumeForm.workExperience.splice(index, 1)
}

// 添加头像上传相关方法
const handleAvatarSuccess = (response) => {
  resumeForm.avatar = response.url
  console.log('头像上传成功')
}

const beforeAvatarUpload = (file) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isJpgOrPng) {
    console.error('头像只能是 JPG 或 PNG 格式!')
    return false
  }
  if (!isLt2M) {
    console.error('头像大小不能超过 2MB!')
    return false
  }
  return true
}

// 生成简历
const generateResume = async () => {
  try {
    generating.value = true
    const response = await request.post('/api/resume/generate', {
      ...resumeForm,
      avatar: resumeForm.avatar
    })
    generatedResume.value = response.data.content
    resultDialogVisible.value = true
  } catch (error) {
    console.error(error.message || '生成简历失败')
  } finally {
    generating.value = false
  }
}

// 保存简历
const saveResume = async () => {
  try {
    await request.post('/api/resume/save', {
      originalData: JSON.stringify(resumeForm),
      generatedContent: generatedResume.value
    })
    console.log('简历保存成功')
    resultDialogVisible.value = false
  } catch (error) {
    console.error('保存失败：' + error.message)
  }
}
</script>

<style scoped>
.resume-edit-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.resume-edit-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.education-item, .work-item {
  margin-bottom: 20px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.text-right {
  text-align: right;
}

.generated-resume {
  padding: 20px;
  white-space: pre-wrap;
}

.generated-resume :deep(.resume-header) {
  text-align: center;
  margin-bottom: 20px;
}

.generated-resume :deep(.resume-avatar) {
  width: 120px;
  height: 120px;
  border-radius: 60px;
  object-fit: cover;
  border: 2px solid #409eff;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.generated-resume :deep(.resume-content) {
  line-height: 1.6;
  font-size: 14px;
}

:deep(.el-divider__text) {
  font-size: 16px;
  font-weight: bold;
}

/* 头像上传相关样式 */
.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.avatar-uploader:hover {
  border-color: #409eff;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
  line-height: 120px;
}

.avatar {
  width: 120px;
  height: 120px;
  display: block;
  object-fit: cover;
}
</style> 