<template>
  <div class="profile-container">
    <div class="profile-content-wrap">
      <!-- 上部分：个人资料与基本信息并排 -->
      <div class="profile-top-section">
        <!-- 个人资料卡片 -->
        <div class="profile-sidebar">
          <div class="profile-card-beauty">
            <div class="profile-content-wrapper">
              <div class="profile-avatar-wrap">
                <div class="avatar-container">
                  <el-avatar :size="120" :src="userInfo.avatar" @error="handleAvatarError" class="profile-avatar" />
                  <div class="avatar-glow"></div>
                </div>
              </div>
              <div class="profile-name-title">
                <div class="profile-name">{{ userInfo.name || '未设置姓名' }}</div>
                <div class="profile-title">{{ userInfo.title || '设置职位' }}</div>
              </div>
              <div class="profile-contact">
                <div class="contact-item">
                  <el-icon class="contact-icon"><Message /></el-icon>
                  <span>{{ userInfo.email || '添加邮箱' }}</span>
                </div>
                <div class="contact-item">
                  <el-icon class="contact-icon"><Phone /></el-icon>
                  <span>{{ userInfo.phone || '添加电话' }}</span>
                </div>
                <div class="contact-item">
                  <el-icon class="contact-icon"><Location /></el-icon>
                  <span>{{ userInfo.location || '添加地区' }}</span>
                </div>
              </div>
            </div>
            <el-button type="primary" class="edit-btn" round @click="openEditProfile">
              <el-icon><Edit /></el-icon>
              编辑资料
            </el-button>
          </div>
        </div>

        <!-- 基本信息卡片 -->
        <div class="profile-basic-info">
          <div class="profile-info-card">
            <div class="card-header">
              <div class="header-title">
                <el-icon class="header-icon"><User /></el-icon>
                <span>基本信息</span>
              </div>
              <el-button link type="primary" @click="openEditBaseInfo" class="edit-link">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
            </div>
            <div class="info-grid">
              <div class="info-item" style="--item-index: 0">
                <div class="info-header">
                  <div class="info-icon">
                    <el-icon><Avatar /></el-icon>
                  </div>
                  <div class="info-label">姓名</div>
                </div>
                <div class="info-value" :class="{ 'empty-state': !userInfo.name }">
                  {{ userInfo.name || '点击编辑添加姓名' }}
                </div>
              </div>
              <div class="info-item" style="--item-index: 1">
                <div class="info-header">
                  <div class="info-icon">
                    <el-icon><Suitcase /></el-icon>
                  </div>
                  <div class="info-label">求职意向</div>
                </div>
                <div class="info-value" :class="{ 'empty-state': !userInfo.jobIntention }">
                  {{ userInfo.jobIntention || '点击编辑添加求职意向' }}
                </div>
              </div>
              <div class="info-item" style="--item-index: 2">
                <div class="info-header">
                  <div class="info-icon">
                    <el-icon><TrendCharts /></el-icon>
                  </div>
                  <div class="info-label">工作经验</div>
                </div>
                <div class="info-value" :class="{ 'empty-state': !userInfo.experience }">
                  {{ userInfo.experience || '点击编辑添加工作经验' }}
                </div>
              </div>
              <div class="info-item" style="--item-index: 3">
                <div class="info-header">
                  <div class="info-icon">
                    <el-icon><School /></el-icon>
                  </div>
                  <div class="info-label">教育背景</div>
                </div>
                <div class="info-value" :class="{ 'empty-state': !userInfo.education }">
                  {{ userInfo.education || '点击编辑添加教育背景' }}
                </div>
              </div>
              <div class="info-item skills-item" style="--item-index: 4">
                <div class="info-header">
                  <div class="info-icon">
                    <el-icon><Medal /></el-icon>
                  </div>
                  <div class="info-label">技能标签</div>
                </div>
                <div class="skills-container">
                  <div v-if="userInfo.skills && userInfo.skills.length > 0" class="skills-list">
                    <el-tag
                        v-for="skill in userInfo.skills"
                        :key="skill"
                        class="skill-tag"
                        closable
                        @close="removeSkill(skill)"
                        effect="plain"
                    >
                      {{ skill }}
                    </el-tag>
                  </div>
                  <div v-else class="skills-empty-state">
                    <span class="empty-hint">暂无技能标签，点击下方按钮添加</span>
                  </div>
                  <div class="skill-actions">
                    <el-input
                        v-if="addSkillInputVisible"
                        v-model="addSkillInput"
                        size="small"
                        class="add-skill-input"
                        @keyup.enter="confirmAddSkill"
                        @blur="confirmAddSkill"
                        placeholder="输入技能名称"
                    />
                    <el-button
                        v-else
                        size="small"
                        @click="addSkillInputVisible = true"
                        class="add-skill-btn"
                        type="primary"
                        plain
                    >
                      <el-icon><Plus /></el-icon>
                      添加技能
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 下部分：简历信息 -->
      <div class="profile-resume-section">
        <div class="resume-card-beauty">
          <div class="card-header">
            <div class="header-title">
              <el-icon class="header-icon"><Document /></el-icon>
              <span>我的简历</span>
            </div>
            <div class="resume-actions">
              <el-button type="primary" @click="$router.push('/resume-edit')" size="small">
                <el-icon><Star /></el-icon>
                智能生成简历
              </el-button>
              <el-upload :show-file-list="false" :before-upload="beforeUploadResume">
                <el-button type="primary" plain size="small">
                  <el-icon><Upload /></el-icon>
                  上传新简历
                </el-button>
              </el-upload>
            </div>
          </div>
          <div class="resume-list-beauty">
            <div v-for="(resume, index) in resumeList" :key="resume.id" class="resume-item-beauty" :style="{ '--index': index }">
              <div class="resume-info-beauty">
                <div class="resume-icon">
                  <el-icon :size="32"><Document /></el-icon>
                </div>
                <div class="resume-detail-beauty">
                  <div class="resume-name">{{ resume.name }}</div>
                  <div class="resume-meta">更新时间：{{ resume.updateTime }}</div>
                </div>
                <el-tag :type="resume.type === 'PDF' ? 'danger' : 'primary'" size="small" class="resume-type-tag">
                  {{ resume.type }}
                </el-tag>
              </div>
              <div class="resume-actions-beauty">
                <el-button link type="primary" @click="handlePreview(resume)" size="small">
                  <el-icon><View /></el-icon>
                  预览
                </el-button>
                <el-button link type="primary" @click="handleDownload(resume)" size="small">
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
                <el-button link type="primary" @click="openEditResume(resume)" size="small">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button link type="danger" @click="handleDeleteResume(resume)" size="small">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
            <div v-if="resumeList.length === 0" class="no-resume-tip">
              <el-empty description="暂无简历" :image-size="80">
                <el-button type="primary" @click="$router.push('/resume-edit')" size="large">
                  <el-icon><Plus /></el-icon>
                  立即创建
                </el-button>
              </el-empty>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editProfileVisible" title="编辑个人资料" width="420px">
      <el-form :model="editProfileForm" label-width="80px">
        <el-form-item label="头像">
          <el-upload
              class="avatar-uploader"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleAvatarSelect"
              accept="image/jpeg,image/png"
          >
            <img v-if="editProfileForm.avatar" :src="editProfileForm.avatar" class="avatar-preview" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div v-if="selectedAvatarFile" class="avatar-upload-info">
            <span>已选择: {{ selectedAvatarFile.name }}</span>
            <el-button
                type="primary"
                size="small"
                @click="uploadAvatar"
                :loading="uploading"
                style="margin-left: 10px;"
            >
              上传
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="editProfileForm.name" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="editProfileForm.title" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editProfileForm.email" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="editProfileForm.phone" />
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="editProfileForm.location" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editProfileVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
    <!-- 编辑基本信息弹窗 -->
    <el-dialog v-model="editBaseInfoVisible" title="编辑基本信息" width="420px">
      <el-form :model="editBaseInfoForm" label-width="80px">
        <el-form-item label="求职意向">
          <el-input v-model="editBaseInfoForm.jobIntention"></el-input>
        </el-form-item>
        <el-form-item label="工作经验">
          <el-input v-model="editBaseInfoForm.experience"></el-input>
        </el-form-item>
        <el-form-item label="教育背景">
          <el-input v-model="editBaseInfoForm.education"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editBaseInfoVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBaseInfo">保存</el-button>
      </template>
    </el-dialog>
    <!-- 编辑简历弹窗 -->
    <el-dialog v-model="editResumeVisible" title="编辑简历名称" width="340px">
      <el-form :model="editResumeForm" label-width="80px">
        <el-form-item label="简历名称">
          <el-input v-model="editResumeForm.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editResumeVisible = false">取消</el-button>
        <el-button type="primary" @click="saveResumeName">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { Message, Phone, Location, Document, Plus, Edit, User, Star, Upload, View, Download, Delete, Avatar, Suitcase, TrendCharts, School, Medal } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import auth from '@/utils/auth'

// 用户基本信息
const userInfo = ref({
  avatar: '',
  name: '',
  title: '',
  email: '',
  phone: '',
  location: '',
  jobIntention: '',
  experience: '',
  education: '',
  skills: []
})

// 简历列表
const resumeList = ref([])



// 获取用户基本信息
const fetchUserInfo = async () => {
  try {
    // 获取当前用户基本信息
    const basicInfo = await request({
      url: '/api/users/current',
      method: 'get'
    })

    console.log('获取到用户基本信息:', basicInfo)

    if (basicInfo) {
      // 从users表获取基本信息
      userInfo.value.avatar = basicInfo.avatarUrl || ''
      userInfo.value.name = basicInfo.realName || basicInfo.username || ''
      userInfo.value.email = basicInfo.email || ''
      userInfo.value.phone = basicInfo.phone || ''

      // 获取用户详细信息
      try {
        const detailInfo = await request({
          url: '/api/users/details',
          method: 'get'
        })

        console.log('获取到用户详细信息:', detailInfo)

        if (detailInfo) {
          userInfo.value.title = detailInfo.jobTitle || ''
          userInfo.value.jobIntention = detailInfo.jobIntention || ''
          userInfo.value.experience = detailInfo.experience || ''
          userInfo.value.education = detailInfo.education || ''
          userInfo.value.location = detailInfo.location || ''

          // 处理技能标签，转换为数组
          try {
            if (detailInfo.skills) {
              userInfo.value.skills = JSON.parse(detailInfo.skills)
            } else {
              userInfo.value.skills = []
            }
          } catch (e) {
            console.error('解析技能标签失败:', e)
            userInfo.value.skills = []
          }
        }
      } catch (detailError) {
        console.error('获取用户详细信息失败:', detailError)
        // 如果是404错误，表示用户详情记录不存在，设置默认空值
        userInfo.value.title = ''
        userInfo.value.jobIntention = ''
        userInfo.value.experience = ''
        userInfo.value.education = ''
        userInfo.value.location = ''
        userInfo.value.skills = []
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    console.error('获取用户信息失败')
  }
}

// 获取用户简历列表
const fetchResumeList = async () => {
  try {
    const res = await request({
      url: '/api/resumes',
      method: 'get'
    })

    console.log('获取到用户简历列表:', res)

    if (res && Array.isArray(res)) {
      resumeList.value = res.map(item => ({
        id: item.id,
        name: item.name || `简历${item.id}`,
        updateTime: item.updateTime ? new Date(item.updateTime).toLocaleDateString() : '未知',
        type: item.type || 'OTHER'
      }))
    }
  } catch (error) {
    console.error('获取简历列表失败:', error)
    console.error('获取简历列表失败')
  }
}

// 头像加载错误处理
const handleAvatarError = () => {
  userInfo.value.avatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
}

onMounted(() => {
  // 页面加载时获取用户信息和简历列表
  fetchUserInfo()
  fetchResumeList()
})

// 编辑资料弹窗
const editProfileVisible = ref(false)
const editProfileForm = reactive({
  avatar: '',
  name: '',
  title: '',
  email: '',
  phone: '',
  location: ''
})

// 头像上传相关状态
const selectedAvatarFile = ref(null)
const uploading = ref(false)

// 打开编辑资料弹窗时初始化表单
const openEditProfile = () => {
  editProfileForm.avatar = userInfo.value.avatar
  editProfileForm.name = userInfo.value.name
  editProfileForm.title = userInfo.value.title
  editProfileForm.email = userInfo.value.email
  editProfileForm.phone = userInfo.value.phone
  editProfileForm.location = userInfo.value.location
  editProfileVisible.value = true
}

// 处理头像文件选择
const handleAvatarSelect = (uploadFile) => {
  console.log('用户选择头像文件:', uploadFile)

  // 验证文件类型和大小
  const isJPG = uploadFile.raw.type === 'image/jpeg' || uploadFile.raw.type === 'image/png'
  const isLt2M = uploadFile.raw.size / 1024 / 1024 < 2

  if (!isJPG) {
    console.error('上传头像图片只能是 JPG/PNG 格式!')
    return
  }
  if (!isLt2M) {
    console.error('上传头像图片大小不能超过 2MB!')
    return
  }

  selectedAvatarFile.value = uploadFile.raw
  console.log('头像文件选择成功，待上传')
}

// 手动上传头像
const uploadAvatar = async () => {
  if (!selectedAvatarFile.value) {
    console.error('请先选择头像文件')
    return
  }

  try {
    uploading.value = true
    console.log('开始上传头像...')

    // 创建FormData
    const formData = new FormData()
    formData.append('file', selectedAvatarFile.value)

    // 获取用户ID和token
    const userId = userInfo.value.id || auth.getUserId()
    const token = auth.getToken()

    console.log('上传参数:', {
      userId,
      hasToken: !!token,
      fileName: selectedAvatarFile.value.name,
      fileSize: selectedAvatarFile.value.size
    })

    // 使用request工具上传
    const response = await request({
      url: '/api/users/upload-avatar',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data',
        'X-User-ID': String(userId),
        ...(token && { 'Authorization': `Bearer ${token}` })
      },
      timeout: 30000
    })

    console.log('头像上传成功，返回数据:', response)

    if (response && response.avatarUrl) {
      editProfileForm.avatar = response.avatarUrl
      console.log('头像上传成功')
      selectedAvatarFile.value = null // 清除已选择的文件
    } else {
      console.error('头像上传响应格式错误:', response)
      console.error('头像上传失败，响应格式错误')
    }

  } catch (error) {
    console.error('头像上传失败:', error)
    let errorMessage = '头像上传失败'

    if (error.response && error.response.data && error.response.data.error) {
      errorMessage += ': ' + error.response.data.error
    } else if (error.message) {
      errorMessage += ': ' + error.message
    }

    console.error(errorMessage)
  } finally {
    uploading.value = false
  }
}



// 保存个人资料
const saveProfile = async () => {
  try {
    // 如果头像有更新，先更新头像
    if (editProfileForm.avatar && editProfileForm.avatar !== userInfo.value.avatar) {
      console.log('头像已更新，保存新头像URL:', editProfileForm.avatar)
      userInfo.value.avatar = editProfileForm.avatar
    }

    // 统一保存基本信息和详细信息
    await request({
      url: '/api/users/update-profile',
      method: 'post',
      data: {
        basicInfo: {
          realName: editProfileForm.name,
          email: editProfileForm.email,
          phone: editProfileForm.phone
        },
        detailInfo: {
          jobTitle: editProfileForm.title,
          location: editProfileForm.location
        }
      }
    })

    // 更新本地显示
    userInfo.value.name = editProfileForm.name
    userInfo.value.title = editProfileForm.title
    userInfo.value.email = editProfileForm.email
    userInfo.value.phone = editProfileForm.phone
    userInfo.value.location = editProfileForm.location

    console.log('保存成功')
    editProfileVisible.value = false

    // 通知首页刷新简历完善度
    if (window.refreshDashboardResumeCompleteness) {
      setTimeout(() => {
        window.refreshDashboardResumeCompleteness()
      }, 500)
    }

    // 检查是否是新用户完善资料，如果是则触发语音指引
    const isNewUserCompleting = checkIfNewUserCompleting()
    if (isNewUserCompleting) {
      console.log('检测到新用户完善资料，触发语音指引')
      // 延迟触发语音指引，确保保存成功消息显示后再播放
      setTimeout(() => {
        if (window.triggerProfileCompleteGuide) {
          window.triggerProfileCompleteGuide()
        }
      }, 1500)
    }
  } catch (error) {
    console.error('保存个人资料失败:', error)
    console.error('保存失败，请稍后重试')
  }
}

// 检查是否是新用户完善资料
const checkIfNewUserCompleting = () => {
  // 检查用户是否是新用户（可以通过localStorage中的标记或用户资料完整度判断）
  const userId = userInfo.value.id || userInfo.value.userId
  if (!userId) return false

  // 检查是否有新用户标记且尚未完善资料
  const newUserKey = `voiceGuide_newUserWelcome_${userId}`
  const hasNewUserWelcome = localStorage.getItem(newUserKey) === 'completed'

  // 检查是否还未触发过完善资料后的语音指引
  const profileCompleteKey = `voiceGuide_profileComplete_${userId}`
  const hasProfileCompleteGuide = localStorage.getItem(profileCompleteKey) === 'completed'

  // 如果是新用户且还没有播放过完善资料后的语音指引
  if (hasNewUserWelcome && !hasProfileCompleteGuide) {
    // 标记已经播放过完善资料后的语音指引
    localStorage.setItem(profileCompleteKey, 'completed')
    return true
  }

  return false
}

// 编辑基本信息弹窗
const editBaseInfoVisible = ref(false)
const editBaseInfoForm = reactive({
  jobIntention: '',
  experience: '',
  education: ''
})

// 打开编辑基本信息弹窗时初始化表单
const openEditBaseInfo = () => {
  editBaseInfoForm.jobIntention = userInfo.value.jobIntention
  editBaseInfoForm.experience = userInfo.value.experience
  editBaseInfoForm.education = userInfo.value.education
  editBaseInfoVisible.value = true
}

// 保存基本信息
const saveBaseInfo = async () => {
  try {
    // 保存详细信息到user_details表
    await request({
      url: '/api/users/update-profile',
      method: 'post',
      data: {
        detailInfo: {
          jobIntention: editBaseInfoForm.jobIntention,
          experience: editBaseInfoForm.experience,
          education: editBaseInfoForm.education
        }
      }
    })

    // 更新本地显示
    userInfo.value.jobIntention = editBaseInfoForm.jobIntention
    userInfo.value.experience = editBaseInfoForm.experience
    userInfo.value.education = editBaseInfoForm.education

    console.log('保存成功')
    editBaseInfoVisible.value = false
    
    // 通知首页刷新简历完善度
    if (window.refreshDashboardResumeCompleteness) {
      setTimeout(() => {
        window.refreshDashboardResumeCompleteness()
      }, 500)
    }
  } catch (error) {
    console.error('保存基本信息失败:', error)
    console.error('保存失败，请稍后重试')
  }
}

// 技能标签编辑
const addSkillInputVisible = ref(false)
const addSkillInput = ref('')

// 添加技能
const confirmAddSkill = async () => {
  const val = addSkillInput.value.trim()
  if (val && !userInfo.value.skills.includes(val)) {
    try {
      // 添加技能到数据库
      const updatedSkills = [...userInfo.value.skills, val]
      await request({
        url: '/api/users/update-profile',
        method: 'post',
        data: {
          detailInfo: {
            skills: JSON.stringify(updatedSkills)
          }
        }
      })

      // 更新本地显示
      userInfo.value.skills.push(val)
      console.log('添加技能成功')
      
      // 通知首页刷新简历完善度
      if (window.refreshDashboardResumeCompleteness) {
        setTimeout(() => {
          window.refreshDashboardResumeCompleteness()
        }, 500)
      }
    } catch (error) {
      console.error('添加技能失败:', error)
      console.error('添加技能失败')
    }
  }

  addSkillInputVisible.value = false
  addSkillInput.value = ''
}

// 删除技能
const removeSkill = async (skill) => {
  try {
    // 从数据库中删除技能
    const updatedSkills = userInfo.value.skills.filter(s => s !== skill)
    await request({
      url: '/api/users/update-profile',
      method: 'post',
      data: {
        detailInfo: {
          skills: JSON.stringify(updatedSkills)
        }
      }
    })

    // 更新本地显示
    userInfo.value.skills = updatedSkills
    console.log('删除技能成功')
    
    // 通知首页刷新简历完善度
    if (window.refreshDashboardResumeCompleteness) {
      setTimeout(() => {
        window.refreshDashboardResumeCompleteness()
      }, 500)
    }
  } catch (error) {
    console.error('删除技能失败:', error)
    console.error('删除技能失败')
  }
}

// 简历相关
async function beforeUploadResume(file) {
  // 验证文件
  if (!file) {
    console.error('请选择要上传的文件')
    return false
  }

  // 验证文件大小 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    console.error('文件大小不能超过10MB')
    return false
  }

  // 验证文件类型
  const allowedTypes = ['application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  if (!allowedTypes.includes(file.type)) {
    console.error('只支持PDF、DOC、DOCX格式的简历文件')
    return false
  }

  console.log('开始上传简历文件:', file.name, '大小:', file.size, '类型:', file.type)

  // 检查用户状态
  try {
    console.log('检查用户登录状态...')
    const statusRes = await request({
      url: '/api/user-status/check',
      method: 'get'
    })
    console.log('用户状态检查结果:', statusRes)

    if (!statusRes.isLoggedIn) {
      console.error('登录已过期，请重新登录')
      setTimeout(() => {
        window.location.href = '/login'
      }, 2000)
      return false
    }
  } catch (statusError) {
    console.error('检查用户状态失败:', statusError)
    console.error('无法验证登录状态，将尝试继续上传')
  }

  // 上传简历到后端
  const formData = new FormData()
  formData.append('file', file)

  request({
    url: '/api/resumes/upload',
    method: 'post',
    data: formData
    // 不要手动设置Content-Type，让浏览器自动设置multipart/form-data和boundary
  }).then(res => {
    console.log('简历上传响应:', res)

    if (res && res.success && res.id) {
      console.log(res.message || '简历上传成功')
      fetchResumeList()
      
      // 通知首页刷新简历完善度
      if (window.refreshDashboardResumeCompleteness) {
        setTimeout(() => {
          window.refreshDashboardResumeCompleteness()
        }, 1000) // 延迟稍长，确保简历列表更新完成
      }
    } else {
      const errorMsg = res?.error || res?.message || '上传失败，请重试'
      console.error('上传响应错误:', res)
      console.error(errorMsg)
    }
  }).catch(error => {
    console.error('上传简历失败:', error)
    console.error('错误详情:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      headers: error.response?.headers
    })

    let errorMessage = '上传失败，请重试'

    if (error.response) {
      const status = error.response.status
      const data = error.response.data

      console.log('服务器返回的错误数据:', data)

      switch (status) {
        case 401:
          errorMessage = data?.error || '登录已过期，请重新登录'
          setTimeout(() => {
            window.location.href = '/login'
          }, 2000)
          break
        case 400:
          errorMessage = data?.error || data?.message || '请求参数错误'
          console.error('400错误详情:', data)
          break
        case 413:
          errorMessage = '文件过大，请选择较小的文件'
          break
        case 500:
          errorMessage = data?.error || data?.message || '服务器错误，请稍后重试'
          break
        default:
          errorMessage = data?.error || data?.message || `上传失败(${status})`
      }
    } else if (error.request) {
      errorMessage = '网络连接失败，请检查网络后重试'
    } else {
      errorMessage = error.message || '未知错误'
    }

    console.error('最终错误消息:', errorMessage)
    console.error(errorMessage)
  })

  return false // 阻止自动上传
}

function handlePreview(resume) {
  // 预览简历
  window.open(`/api/resumes/${resume.id}/preview`, '_blank')
}

function handleDownload(resume) {
  // 下载简历
  window.location.href = `/api/resumes/${resume.id}/download`
}

const editResumeVisible = ref(false)
const editResumeForm = reactive({ id: '', name: '' })

function openEditResume(resume) {
  editResumeForm.id = resume.id
  editResumeForm.name = resume.name
  editResumeVisible.value = true
}

async function saveResumeName() {
  try {
    // 保存简历名称到后端
    await request({
      url: `/api/resumes/${editResumeForm.id}/name`,
      method: 'put',
      data: {
        name: editResumeForm.name
      }
    })

    // 更新本地显示
    const idx = resumeList.value.findIndex(r => r.id === editResumeForm.id)
    if (idx !== -1) resumeList.value[idx].name = editResumeForm.name

    console.log('保存成功')
    editResumeVisible.value = false
  } catch (error) {
    console.error('保存简历名称失败:', error)
    console.error('保存失败')
  }
}

async function handleDeleteResume(resume) {
  try {
    // 删除简历
    await request({
      url: `/api/resumes/${resume.id}`,
      method: 'delete'
    })

    // 更新本地显示
    resumeList.value = resumeList.value.filter(r => r.id !== resume.id)
    console.log('删除成功')
    
    // 通知首页刷新简历完善度
    if (window.refreshDashboardResumeCompleteness) {
      setTimeout(() => {
        window.refreshDashboardResumeCompleteness()
      }, 500)
    }
  } catch (error) {
    console.error('删除简历失败:', error)
    console.error('删除失败')
  }
}
</script>

<style scoped>
/* 全局容器 */
.profile-container {
  width: 100%;
  min-height: 100vh;
  background: transparent;
  position: relative;
  overflow-x: hidden;
}



/* 内容包装器 */
.profile-content-wrap {
  max-width: 1400px;
  margin: 0 auto;
  padding: 40px 40px 60px;
  position: relative;
  z-index: 1;
}

/* 上部分布局：个人资料与基本信息并排 */
.profile-top-section {
  display: grid;
  grid-template-columns: 400px 1fr;
  gap: 32px;
  align-items: stretch;
  margin-bottom: 32px;
}

/* 下部分布局：简历信息 */
.profile-resume-section {
  width: 100%;
}

/* 个人资料卡片 */
.profile-sidebar {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

/* 基本信息卡片容器 */
.profile-basic-info {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.profile-card-beauty {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 28px;
  padding: 24px;
  text-align: center;
  box-shadow:
      0 20px 60px rgba(168, 181, 160, 0.08),
      0 8px 32px rgba(0, 0, 0, 0.04),
      inset 0 1px 0 rgba(255, 255, 255, 0.4);
  position: relative;
  overflow: hidden;
  animation: cardSlideIn 0.8s ease-out;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.profile-card-beauty:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow:
      0 32px 80px rgba(0, 0, 0, 0.15),
      0 16px 48px rgba(0, 0, 0, 0.08);
}

.profile-card-beauty::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #A8B5A0, #BC9A6A, #A8B5A0);
  border-radius: 28px 28px 0 0;
}

/* 个人资料内容包装器 */
.profile-content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  text-align: center;
}

/* 头像容器 */
.profile-avatar-wrap {
  margin-bottom: 16px;
}

.avatar-container {
  position: relative;
  display: inline-block;
}

.profile-avatar {
  border: 4px solid rgba(255, 255, 255, 0.8);
  box-shadow:
      0 12px 40px rgba(0, 0, 0, 0.15),
      0 4px 16px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  animation: avatarGlow 3s ease-in-out infinite alternate;
}

.avatar-glow {
  position: absolute;
  top: -10px;
  left: -10px;
  right: -10px;
  bottom: -10px;
  background: linear-gradient(45deg, #A8B5A0, #BC9A6A);
  border-radius: 50%;
  opacity: 0.3;
  filter: blur(20px);
  z-index: -1;
  animation: pulseGlow 2s ease-in-out infinite;
}

/* 姓名和职位 */
.profile-name-title {
  margin-bottom: 16px;
}

.profile-name {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.profile-title {
  font-size: 16px;
  color: #7f8c8d;
  font-weight: 500;
  opacity: 0.8;
}

/* 联系信息 */
.profile-contact {
  margin-bottom: 16px;
}

.contact-item {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px 20px;
  margin-bottom: 8px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
}

.contact-item:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: translateX(8px);
}

.contact-icon {
  color: #BC9A6A;
  margin-right: 12px;
  font-size: 18px;
}

.contact-item span {
  color: #556571;
  font-size: 14px;
  font-weight: 500;
}

/* 编辑按钮 */
.edit-btn {
  width: 100%;
  height: 48px;
  border-radius: 24px;
  background: linear-gradient(135deg, #A8B5A0 0%, #9CAF88 100%) !important;
  border: none !important;
  color: #ffffff !important;
  font-weight: 600 !important;
  font-size: 16px !important;
  box-shadow: 0 8px 24px rgba(168, 181, 160, 0.3) !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  position: relative;
  overflow: hidden;
  margin-top: 16px;
  flex-shrink: 0;
}

.edit-btn:hover {
  transform: translateY(-2px) !important;
  box-shadow: 0 12px 32px rgba(168, 181, 160, 0.4) !important;
}

.edit-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.6s ease;
}

.edit-btn:hover::before {
  left: 100%;
}

/* 内容动画 */
.profile-basic-info {
  animation: contentFadeIn 1s ease-out 0.3s both;
}

.profile-resume-section {
  animation: contentFadeIn 1s ease-out 0.5s both;
}

/* 信息卡片 */
.profile-info-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(248, 250, 252, 0.95) 100%);
  backdrop-filter: blur(25px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 28px;
  padding: 24px;
  box-shadow:
      0 20px 60px rgba(168, 181, 160, 0.08),
      0 8px 32px rgba(0, 0, 0, 0.04),
      inset 0 1px 0 rgba(255, 255, 255, 0.4);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.profile-info-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #A8B5A0 0%, #BC9A6A 50%, #A8B5A0 100%);
  border-radius: 28px 28px 0 0;
}

.resume-card-beauty {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  padding: 32px;
  box-shadow:
      0 12px 40px rgba(0, 0, 0, 0.08),
      0 4px 16px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.profile-info-card:hover,
.resume-card-beauty:hover {
  transform: translateY(-4px);
  box-shadow:
      0 20px 60px rgba(0, 0, 0, 0.12),
      0 8px 24px rgba(0, 0, 0, 0.08);
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid transparent;
  background: linear-gradient(to right, rgba(168, 181, 160, 0.1), rgba(168, 181, 160, 0.05), rgba(168, 181, 160, 0.1))
  left bottom / 100% 2px no-repeat;
  position: relative;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 21px;
  font-weight: 700;
  color: #2c3e50;
  background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-icon {
  color: #BC9A6A;
  font-size: 26px;
  filter: drop-shadow(0 2px 4px rgba(188, 154, 106, 0.2));
}

.edit-link {
  display: inline-flex !important;
  align-items: center !important;
  gap: 6px !important;
  padding: 8px 16px !important;
  background: linear-gradient(135deg, rgba(168, 181, 160, 0.1) 0%, rgba(156, 175, 136, 0.1) 100%) !important;
  border: 1px solid rgba(168, 181, 160, 0.3) !important;
  border-radius: 20px !important;
  color: #A8B5A0 !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  text-decoration: none !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  backdrop-filter: blur(10px) !important;
  box-shadow: 0 2px 8px rgba(168, 181, 160, 0.1) !important;
}

.edit-link:hover {
  background: linear-gradient(135deg, rgba(168, 181, 160, 0.2) 0%, rgba(156, 175, 136, 0.2) 100%) !important;
  border-color: rgba(168, 181, 160, 0.5) !important;
  color: #8A9B7A !important;
  transform: translateY(-1px) scale(1.02) !important;
  box-shadow: 0 4px 16px rgba(168, 181, 160, 0.2) !important;
}

.edit-link .el-icon {
  font-size: 14px !important;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
  flex: 1;
  align-content: start;
}

.info-item {
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.9) 0%, rgba(255, 255, 255, 0.8) 100%);
  border: 1px solid rgba(226, 232, 240, 0.6);
  border-radius: 18px;
  padding: 16px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  backdrop-filter: blur(10px);
  animation: infoItemSlideIn 0.6s ease-out;
  animation-delay: calc(var(--item-index, 0) * 0.1s);
  animation-fill-mode: both;
}

.info-item:hover {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.9) 100%);
  border-color: rgba(168, 181, 160, 0.4);
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 16px 40px rgba(168, 181, 160, 0.15), 0 8px 24px rgba(0, 0, 0, 0.08);
}

.info-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #A8B5A0 0%, #BC9A6A 50%, #A8B5A0 100%);
  border-radius: 0 18px 18px 0;
}

.info-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, rgba(168, 181, 160, 0.15) 0%, rgba(156, 175, 136, 0.15) 100%);
  border: 1px solid rgba(168, 181, 160, 0.3);
  border-radius: 10px;
  color: #A8B5A0;
  font-size: 16px;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.info-item:hover .info-icon {
  background: linear-gradient(135deg, rgba(168, 181, 160, 0.25) 0%, rgba(156, 175, 136, 0.25) 100%);
  border-color: rgba(168, 181, 160, 0.5);
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(168, 181, 160, 0.2);
  animation: iconBounce 0.6s ease-in-out;
}

.info-label {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  margin: 0;
}

.info-value {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  min-height: 20px;
  line-height: 1.4;
  word-break: break-word;
}

.info-value.empty-state {
  color: #9ca3af;
  font-style: italic;
  font-weight: 400;
  opacity: 0.8;
}

/* 技能标签区域 */
.skills-item {
  grid-column: 1 / -1;
}

.skills-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skills-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.skills-empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 50px;
  background: rgba(248, 250, 252, 0.5);
  border: 2px dashed rgba(168, 181, 160, 0.3);
  border-radius: 14px;
  padding: 16px;
}

.empty-hint {
  color: #9ca3af;
  font-size: 14px;
  font-style: italic;
  text-align: center;
}

.skill-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.skill-tag {
  background: transparent !important;
  border: 1px solid rgba(0, 0, 0, 0.2) !important;
  color: #4a5568 !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  padding: 10px 18px !important;
  border-radius: 24px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
  position: relative !important;
  overflow: hidden !important;
}

.skill-tag:hover {
  background: rgba(0, 0, 0, 0.05) !important;
  border-color: rgba(0, 0, 0, 0.3) !important;
  color: #2d3748 !important;
  transform: translateY(-2px) scale(1.02) !important;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1), 0 4px 8px rgba(0, 0, 0, 0.05) !important;
}

.add-skill-btn {
  background: linear-gradient(135deg, #A8B5A0 0%, #9CAF88 100%) !important;
  border: 1px solid #A8B5A0 !important;
  color: #ffffff !important;
  font-weight: 600 !important;
  font-size: 13px !important;
  padding: 10px 18px !important;
  border-radius: 24px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  position: relative !important;
  overflow: hidden !important;
}

.add-skill-btn:hover {
  background: linear-gradient(135deg, #9CAF88 0%, #8A9B7A 100%) !important;
  border-color: #9CAF88 !important;
  color: #ffffff !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 6px 16px rgba(168, 181, 160, 0.2) !important;
}

.add-skill-input {
  width: 140px !important;
  border-radius: 24px !important;
  font-size: 13px !important;
  font-weight: 500 !important;
}

.add-skill-input :deep(.el-input__wrapper) {
  border-radius: 24px !important;
  border: 1px solid rgba(0, 0, 0, 0.2) !important;
  background: rgba(255, 255, 255, 0.9) !important;
  backdrop-filter: blur(10px) !important;
  transition: all 0.3s ease !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
}

.add-skill-input :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 0, 0, 0.3) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
}

.add-skill-input :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(0, 0, 0, 0.4) !important;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15) !important;
}

.add-skill-input :deep(.el-input__inner) {
  color: #4a5568 !important;
  font-size: 13px !important;
  font-weight: 500 !important;
}

.add-skill-input :deep(.el-input__inner::placeholder) {
  color: #9ca3af !important;
  font-weight: 400 !important;
}

/* 简历操作区域 */
.resume-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.resume-actions .el-button {
  border-radius: 16px !important;
  font-weight: 500 !important;
  transition: all 0.3s ease !important;
}

/* 简历列表 */
.resume-list-beauty {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.resume-item-beauty {
  background: rgba(248, 250, 252, 0.6);
  border: 1px solid rgba(226, 232, 240, 0.5);
  border-radius: 20px;
  padding: 24px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  animation: resumeSlideIn 0.6s ease-out;
  animation-fill-mode: both;
}

.resume-item-beauty:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(168, 181, 160, 0.3);
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
}

.resume-item-beauty::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #A8B5A0, #BC9A6A);
  border-radius: 20px 20px 0 0;
}

.resume-info-beauty {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 16px;
}

.resume-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: rgba(168, 181, 160, 0.1);
  border-radius: 16px;
  color: #A8B5A0;
  transition: all 0.3s ease;
}

.resume-item-beauty:hover .resume-icon {
  background: rgba(168, 181, 160, 0.2);
  transform: scale(1.1);
}

.resume-detail-beauty {
  flex: 1;
}

.resume-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.resume-meta {
  font-size: 14px;
  color: #64748b;
}

.resume-type-tag {
  border-radius: 12px !important;
  font-weight: 500 !important;
}

.resume-actions-beauty {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.resume-actions-beauty .el-button {
  border-radius: 12px !important;
  transition: all 0.3s ease !important;
  background: transparent !important;
  border: 1px solid rgba(0, 0, 0, 0.2) !important;
  color: #4a5568 !important;
  font-weight: 500 !important;
}

.resume-actions-beauty .el-button:hover {
  background: rgba(0, 0, 0, 0.05) !important;
  border-color: rgba(0, 0, 0, 0.3) !important;
  color: #2d3748 !important;
  transform: translateY(-1px) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
}

.resume-actions-beauty .el-button--danger {
  color: #e53e3e !important;
  border-color: rgba(229, 62, 62, 0.3) !important;
}

.resume-actions-beauty .el-button--danger:hover {
  background: rgba(229, 62, 62, 0.05) !important;
  border-color: rgba(229, 62, 62, 0.5) !important;
  color: #c53030 !important;
}

/* 空状态 */
.no-resume-tip {
  text-align: center;
  padding: 40px 20px;
  background: rgba(248, 250, 252, 0.5);
  border: 2px dashed rgba(168, 181, 160, 0.3);
  border-radius: 20px;
  margin: 20px 0;
}

/* 对话框样式优化 */
:deep(.el-dialog) {
  border-radius: 20px !important;
  overflow: hidden;
  backdrop-filter: blur(20px);
}

:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #A8B5A0 0%, #9CAF88 100%);
  color: white;
  padding: 20px 24px;
}

:deep(.el-dialog__title) {
  color: white !important;
  font-weight: 600;
}

:deep(.el-dialog__body) {
  padding: 24px;
  background: rgba(255, 255, 255, 0.95);
}

/* 头像上传样式 */
.avatar-uploader {
  text-align: center;
  width: 100%;
}

.avatar-uploader :deep(.el-upload) {
  cursor: pointer;
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  border: 2px dashed rgba(168, 181, 160, 0.4);
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  background: rgba(248, 250, 252, 0.5);
}

.avatar-uploader :deep(.el-upload:hover) {
  border-color: #A8B5A0;
  background: rgba(248, 250, 252, 0.8);
}

.avatar-uploader-icon {
  font-size: 32px;
  color: #A8B5A0;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 16px;
  object-fit: cover;
}

.avatar-upload-info {
  margin-top: 12px;
  font-size: 14px;
  color: #64748b;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
}

/* 按钮样式优化 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #A8B5A0 0%, #9CAF88 100%) !important;
  border: none !important;
  border-radius: 12px !important;
  font-weight: 500 !important;
  color: #ffffff !important;
  transition: all 0.3s ease !important;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px) !important;
  box-shadow: 0 8px 24px rgba(168, 181, 160, 0.3) !important;
  color: #ffffff !important;
}

/* plain类型的primary按钮样式 */
:deep(.el-button--primary.is-plain) {
  background: linear-gradient(135deg, #A8B5A0 0%, #9CAF88 100%) !important;
  border: 1px solid #A8B5A0 !important;
  color: #ffffff !important;
}

:deep(.el-button--primary.is-plain:hover) {
  background: linear-gradient(135deg, #9CAF88 0%, #8A9B7A 100%) !important;
  border-color: #9CAF88 !important;
  color: #ffffff !important;
}

/* 动画定义 */

@keyframes cardSlideIn {
  from {
    opacity: 0;
    transform: translateX(-50px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes contentFadeIn {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes resumeSlideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes avatarGlow {
  0% { box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15), 0 4px 16px rgba(0, 0, 0, 0.1); }
  100% { box-shadow: 0 16px 50px rgba(168, 181, 160, 0.2), 0 8px 24px rgba(168, 181, 160, 0.1); }
}

@keyframes pulseGlow {
  0%, 100% { opacity: 0.3; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.05); }
}

@keyframes infoItemSlideIn {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes iconBounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-4px);
  }
  60% {
    transform: translateY(-2px);
  }
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .profile-top-section {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .profile-content-wrap {
    padding: 20px 20px 40px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-content-wrap {
    padding: 20px 16px 30px;
  }

  .profile-card-beauty,
  .profile-info-card,
  .resume-card-beauty {
    padding: 24px 20px;
  }

  .resume-actions {
    flex-direction: column;
    gap: 8px;
  }

  .resume-actions-beauty {
    justify-content: center;
  }
}

/* 减少动画偏好支持 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* 高对比度模式支持 */
@media (forced-colors: active) {
  .profile-card-beauty,
  .profile-info-card,
  .resume-card-beauty,
  .resume-item-beauty {
    border: 2px solid !important;
    box-shadow: none !important;
  }
}
</style> 