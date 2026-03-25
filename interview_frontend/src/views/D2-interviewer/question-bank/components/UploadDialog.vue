<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    title="批量导入题目"
    width="600px"
    :before-close="handleCancel">
    
    <div class="upload-container">
      <el-alert
        title="导入说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;">
        <template #default>
          <ul style="margin: 0; padding-left: 20px;">
            <li>支持.xlsx格式的Excel文件</li>
            <li>请使用提供的模板格式</li>
            <li>建议单次导入不超过500道题目</li>
            <li>重复的题目将被跳过</li>
          </ul>
        </template>
      </el-alert>
      
      <el-upload
        ref="uploadRef"
        class="upload-demo"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        :file-list="fileList">
        
        <el-icon class="el-icon--upload"><Upload /></el-icon>
        <div class="el-upload__text">
          将Excel文件拖拽到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传.xlsx/.xls格式文件，且不超过10MB
          </div>
        </template>
      </el-upload>
      
      <!-- 上传进度 -->
      <div v-if="uploading" class="upload-progress">
        <el-progress 
          :percentage="uploadProgress" 
          :status="uploadStatus"
          style="margin-top: 20px;" />
        <p class="progress-text">{{ uploadProgressText }}</p>
      </div>
      
      <!-- 上传结果 -->
      <div v-if="uploadResult" class="upload-result">
        <el-alert
          :title="uploadResult.success ? '导入成功' : '导入完成'"
          :type="uploadResult.success ? 'success' : 'warning'"
          :closable="false"
          style="margin-top: 20px;">
          <template #default>
            <div>
              <p>成功导入：{{ uploadResult.successCount }} 道题目</p>
              <p v-if="uploadResult.errorCount > 0">
                失败：{{ uploadResult.errorCount }} 道题目
              </p>
              <div v-if="uploadResult.errors && uploadResult.errors.length > 0">
                <el-divider>错误详情</el-divider>
                <div class="error-list">
                  <div 
                    v-for="(error, index) in uploadResult.errors.slice(0, 10)" 
                    :key="index"
                    class="error-item">
                    第{{ error.row }}行：{{ error.message }}
                  </div>
                  <div v-if="uploadResult.errors.length > 10" class="error-more">
                    还有{{ uploadResult.errors.length - 10 }}个错误...
                  </div>
                </div>
              </div>
            </div>
          </template>
        </el-alert>
      </div>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleUpload"
          :loading="uploading"
          :disabled="!selectedFile || uploading">
          开始导入
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { uploadQuestionsFromExcel } from '../../../../api/interviewer/questions'

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['update:visible', 'success'])

const uploadRef = ref()
const fileList = ref([])
const selectedFile = ref(null)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadStatus = ref('')
const uploadProgressText = ref('')
const uploadResult = ref(null)

function handleFileChange(file) {
  selectedFile.value = file.raw
  fileList.value = [file]
  uploadResult.value = null
  
  // 验证文件
  if (!validateFile(file.raw)) {
    fileList.value = []
    selectedFile.value = null
  }
}

function handleExceed() {
  console.error('只能上传一个文件')
}

function validateFile(file) {
  const allowedTypes = [
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-excel'
  ]
  
  if (!allowedTypes.includes(file.type)) {
    console.error('只能上传Excel格式文件')
    return false
  }
  
  const maxSize = 10 * 1024 * 1024 // 10MB
  if (file.size > maxSize) {
    console.error('文件大小不能超过10MB')
    return false
  }
  
  return true
}

async function handleUpload() {
  if (!selectedFile.value) {
    console.error('请选择要上传的文件')
    return
  }
  
  uploading.value = true
  uploadProgress.value = 0
  uploadStatus.value = ''
  uploadProgressText.value = '正在上传文件...'
  uploadResult.value = null
  
  try {
    // 模拟上传进度
    const progressInterval = setInterval(() => {
      if (uploadProgress.value < 90) {
        uploadProgress.value += Math.random() * 20
        if (uploadProgress.value > 90) {
          uploadProgress.value = 90
        }
      }
    }, 500)
    
    const result = await uploadQuestionsFromExcel(selectedFile.value)
    
    clearInterval(progressInterval)
    uploadProgress.value = 100
    uploadStatus.value = 'success'
    uploadProgressText.value = '导入完成'
    
    uploadResult.value = {
      success: result.errorCount === 0,
      successCount: result.successCount || 0,
      errorCount: result.errorCount || 0,
      errors: result.errors || []
    }
    
    // 通知父组件成功
    if (result.successCount > 0) {
      emit('success', result)
    }
    
  } catch (error) {
    console.error('上传失败:', error)
    uploadProgress.value = 100
    uploadStatus.value = 'exception'
    uploadProgressText.value = '导入失败'
    
    uploadResult.value = {
      success: false,
      successCount: 0,
      errorCount: 1,
      errors: [{ row: 0, message: error.message || '上传失败，请重试' }]
    }
    
    console.error('导入失败：' + (error.message || '未知错误'))
  } finally {
    uploading.value = false
  }
}

function handleCancel() {
  // 重置状态
  fileList.value = []
  selectedFile.value = null
  uploading.value = false
  uploadProgress.value = 0
  uploadStatus.value = ''
  uploadProgressText.value = ''
  uploadResult.value = null
  
  emit('update:visible', false)
}
</script>

<style scoped>
.upload-container {
  padding: 20px 0;
}

.upload-demo {
  width: 100%;
}

.upload-progress {
  margin-top: 20px;
}

.progress-text {
  text-align: center;
  margin-top: 8px;
  color: #606266;
  font-size: 14px;
}

.upload-result {
  margin-top: 20px;
}

.error-list {
  max-height: 200px;
  overflow-y: auto;
  margin-top: 10px;
}

.error-item {
  padding: 4px 0;
  font-size: 13px;
  color: #f56c6c;
  border-bottom: 1px solid #fde2e2;
}

.error-item:last-child {
  border-bottom: none;
}

.error-more {
  padding: 8px 0;
  font-size: 13px;
  color: #909399;
  text-align: center;
  font-style: italic;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-upload-dragger) {
  padding: 40px;
}

:deep(.el-icon--upload) {
  font-size: 67px;
  color: #c0c4cc;
  margin-bottom: 16px;
}

:deep(.el-upload__text) {
  color: #606266;
  font-size: 14px;
}

:deep(.el-upload__text em) {
  color: #409eff;
  font-style: normal;
}
</style>