<template>
  <el-dialog 
    v-model="visible"
    title="代码题测试用例配置"
    width="800px"
    :before-close="handleClose">
    
    <div class="code-question-config">
      <el-form :model="form" label-width="120px">
        <el-form-item label="题目内容">
          <el-input 
            v-model="form.content" 
            type="textarea" 
            :rows="3"
            placeholder="请输入代码题题目描述"
            readonly />
        </el-form-item>
        
        <el-form-item label="编程语言">
          <el-checkbox-group v-model="form.supportedLanguages">
            <el-checkbox label="java">Java</el-checkbox>
            <el-checkbox label="python">Python</el-checkbox>
            <el-checkbox label="cpp">C++</el-checkbox>
            <el-checkbox label="c">C</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        
        <el-form-item label="测试用例">
          <div class="test-cases-container">
            <div 
              v-for="(testCase, index) in form.testCases" 
              :key="index"
              class="test-case-item">
              <div class="test-case-header">
                <span>测试用例 {{ index + 1 }}</span>
                <el-button 
                  type="danger" 
                  size="small" 
                  @click="removeTestCase(index)"
                  :disabled="form.testCases.length <= 1">
                  删除
                </el-button>
              </div>
              
              <div class="test-case-content">
                <div class="input-section">
                  <label>输入:</label>
                  <el-input 
                    v-model="testCase.input" 
                    type="textarea" 
                    :rows="2"
                    placeholder="测试输入数据" />
                </div>
                
                <div class="output-section">
                  <label>期望输出:</label>
                  <el-input 
                    v-model="testCase.expectedOutput" 
                    type="textarea" 
                    :rows="2"
                    placeholder="期望的输出结果" />
                </div>
                
                <div class="description-section">
                  <label>描述:</label>
                  <el-input 
                    v-model="testCase.description" 
                    placeholder="测试用例描述（可选）" />
                </div>
              </div>
            </div>
            
            <el-button 
              type="primary" 
              @click="addTestCase"
              style="width: 100%; margin-top: 10px;">
              + 添加测试用例
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item label="示例代码">
          <el-input 
            v-model="form.sampleCode" 
            type="textarea" 
            :rows="6"
            placeholder="可以提供一个示例代码框架（可选）" />
        </el-form-item>
      </el-form>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSave">保存配置</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  question: Object
})

const emit = defineEmits(['update:visible', 'save'])

// 表单数据
const form = ref({
  content: '',
  supportedLanguages: ['java'],
  testCases: [
    {
      input: '',
      expectedOutput: '',
      description: ''
    }
  ],
  sampleCode: ''
})

// 监听题目变化
watch(() => props.question, (newQuestion) => {
  if (newQuestion) {
    form.value.content = newQuestion.content || ''
    
    // 解析已有的测试用例
    if (newQuestion.correctAnswer) {
      try {
        const testCases = JSON.parse(newQuestion.correctAnswer)
        if (Array.isArray(testCases) && testCases.length > 0) {
          form.value.testCases = testCases
        }
      } catch (e) {
        console.warn('解析测试用例失败:', e)
      }
    }
  }
}, { immediate: true })

// 添加测试用例
function addTestCase() {
  form.value.testCases.push({
    input: '',
    expectedOutput: '',
    description: ''
  })
}

// 删除测试用例
function removeTestCase(index) {
  if (form.value.testCases.length > 1) {
    form.value.testCases.splice(index, 1)
  }
}

// 保存配置
function handleSave() {
  // 验证测试用例
  const validTestCases = form.value.testCases.filter(tc => 
    tc.input.trim() !== '' || tc.expectedOutput.trim() !== ''
  )
  
  if (validTestCases.length === 0) {
    console.error('请至少配置一个有效的测试用例')
    return
  }
  
  // 构建保存数据
  const saveData = {
    ...props.question,
    correctAnswer: JSON.stringify(form.value.testCases),
    // 可以在description字段存储其他信息
    description: JSON.stringify({
      supportedLanguages: form.value.supportedLanguages,
      sampleCode: form.value.sampleCode
    })
  }
  
  emit('save', saveData)
  handleClose()
}

// 关闭对话框
function handleClose() {
  emit('update:visible', false)
}
</script>

<style scoped>
.code-question-config {
  padding: 20px 0;
}

.test-cases-container {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 15px;
  background-color: #fafafa;
}

.test-case-item {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 15px;
  margin-bottom: 15px;
  background-color: #fff;
}

.test-case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 500;
}

.test-case-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.input-section,
.output-section,
.description-section {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.input-section label,
.output-section label,
.description-section label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

:deep(.el-checkbox-group) {
  display: flex;
  gap: 20px;
}
</style>
