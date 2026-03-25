<template>
  <div class="job-detail-container">
    <!-- 页面过渡容器 -->
    <transition name="page-fade" mode="out-in">
      <div v-if="jobDetail" key="content" class="content-container">
        <el-row :gutter="24">
          <!-- 左侧岗位详情 -->
          <el-col :span="16">
            <el-card class="job-detail-card">
              <template #header>
                <div class="card-header">
                  <div class="title-section">
                    <h1>{{ jobDetail.title }}</h1>
                    <p class="job-description">{{ jobDetail.description }}</p>
                  </div>
                  <div class="header-actions">
                    <el-tag :type="getCategoryType(jobDetail.category)">{{ jobDetail.category }}</el-tag>
                  </div>
                </div>
              </template>
              <div class="job-content">
                <div class="job-salary">{{ jobDetail.salary }}</div>
                <div class="job-tags">
                  <el-tag size="small" effect="plain">{{ jobDetail.location }}</el-tag>
                  <el-tag size="small" effect="plain">{{ jobDetail.experience }}</el-tag>
                  <el-tag size="small" effect="plain">{{ jobDetail.education }}</el-tag>
                  <el-tag v-if="jobDetail.workType !== null" size="small" effect="plain">{{ jobDetail.workType === 0 ? '全职' : '实习' }}</el-tag>
                </div>
                <h3>岗位职责：</h3>
                <ul class="requirements-list">
                  <li v-for="(item, index) in parsedDuties" :key="`duty-${index}`">{{ item }}</li>
                </ul>
                <h3>任职要求：</h3>
                <ul class="requirements-list">
                  <li v-for="(item, index) in parsedRequirements" :key="`req-${index}`">{{ item }}</li>
                </ul>
                <div
                    v-if="jobDetail.company"
                    @click="navigateToCompany(jobDetail.company.id)"
                    class="company-info clickable">
                  <el-avatar :size="60" :src="jobDetail.company.logoUrl || '/default-company-logo.png'" />
                  <div class="company-detail">
                    <h3>{{ jobDetail.company.name }}</h3>
                    <p>{{ jobDetail.company.slogan }}</p>
                  </div>
                </div>
              </div>
              <template #footer>
                <div class="card-footer">
                  <el-button type="primary" size="large" @click="goBackToJobs">返回</el-button>
                  <el-button type="primary" size="large" @click="applyInterview">投递简历</el-button>
                </div>
              </template>
            </el-card>
          </el-col>

          <!-- 右侧相关岗位 -->
          <el-col :span="8">
            <el-card class="related-jobs-card">
              <template #header>
                <div class="card-header">
                  <span>相关岗位</span>
                </div>
              </template>
              <div v-if="relatedJobs.length > 0" class="related-jobs-list">
                <div v-for="job in relatedJobs" :key="job.id" class="related-job-item" @click="navigateToJob(job.id)">
                  <div class="related-job-title">{{ job.title }}</div>
                  <div class="related-job-salary">{{ job.salary }}</div>
                  <div class="related-job-location">{{ job.location }}</div>
                </div>
              </div>
              <el-empty v-else description="暂无相关岗位" size="small" />
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 错误状态 -->
      <div v-else key="error" class="error-container">
        <el-row :gutter="24">
          <el-col :span="24">
            <el-empty description="岗位信息加载失败或不存在" />
          </el-col>
        </el-row>
      </div>
    </transition>

    <!-- 简历选择弹窗 -->
    <el-dialog
        v-model="resumeSelectVisible"
        title=""
        width="600px"
        class="resume-dialog"
        :show-close="false"
    >
      <!-- 自定义头部 -->
      <div class="dialog-header">
        <div class="header-content">
          <div class="header-icon">
            <i class="el-icon-document"></i>
          </div>
          <div class="header-text">
            <h3>选择简历</h3>
            <p>请选择要投递的简历</p>
          </div>
        </div>
        <el-button
            type="text"
            @click="resumeSelectVisible = false"
            class="close-btn"
        >
          <i class="el-icon-close"></i>
        </el-button>
      </div>

      <!-- 简历列表 -->
      <div class="dialog-content">
        <div v-if="resumeList.length" class="resume-list">
          <div
              v-for="resume in resumeList"
              :key="resume.id"
              class="resume-card"
              :class="{ 'selected': selectedResumeId === resume.id }"
              @click="selectedResumeId = resume.id"
          >
            <div class="resume-card-header">
              <div class="resume-icon">
                <i class="el-icon-document-copy"></i>
              </div>
              <div class="resume-info">
                <h4 class="resume-name">{{ resume.name }}</h4>
                <div class="resume-meta">
                  <span class="resume-type">
                    <i class="el-icon-collection-tag"></i>
                    {{ resume.type }}
                  </span>
                  <span class="resume-time">
                    <i class="el-icon-time"></i>
                    更新时间：{{ resume.updateTime ? new Date(resume.updateTime).toLocaleDateString() : '未知' }}
                  </span>
                </div>
              </div>
            </div>
            <div class="resume-card-action">
              <div class="radio-indicator" :class="{ 'active': selectedResumeId === resume.id }">
                <i class="el-icon-check" v-if="selectedResumeId === resume.id"></i>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="empty-resume-state">
          <div class="empty-icon">
            <i class="el-icon-document-delete"></i>
          </div>
          <h4>暂无简历</h4>
          <p>请先在个人中心上传或生成简历</p>
          <el-button type="primary" size="small">
            <i class="el-icon-plus"></i>
            去添加简历
          </el-button>
        </div>
      </div>

      <!-- 底部按钮 -->
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="resumeSelectVisible = false" class="cancel-btn">
            取消
          </el-button>
          <el-button
              type="primary"
              :disabled="!selectedResumeId"
              @click="confirmResumeSelect"
              class="confirm-btn"
          >
            <i class="el-icon-s-promotion"></i>
            确认投递
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { ElMessage } from 'element-plus';
import { useInterviewStore } from '@/store/interview';

const route = useRoute();
const router = useRouter();
const interviewStore = useInterviewStore();
const jobId = ref(route.params.id);

const jobDetail = ref(null);
const relatedJobs = ref([]);

// 重置页面状态
const resetPageState = () => {
  jobDetail.value = null;
  relatedJobs.value = [];
};

const fetchJobDetail = async () => {
  try {
    console.log('正在获取岗位详情，ID:', jobId.value);

    const response = await request({
      url: `/api/jobs/${jobId.value}`,
      method: 'get',
    });
    console.log('获取到的岗位详情:', response);

    if (response) {
      jobDetail.value = {
        ...response,
        company: response.company || { name: '未知公司', logoUrl: '/default-company-logo.png' },
        requirements: parseRequirements(response.requirements),
        duties: parseContent(response.duties),
        description: response.description || '',
        companyLogo: response.companyLogo || '/default-company-logo.png',
        companyName: response.companyName || '未知公司',
        companyDesc: response.companyDesc || '暂无描述',
      };

      // 获取相关岗位（异步，不阻塞主内容显示）
      fetchRelatedJobs(jobDetail.value.category);
    } else {
      console.error('未找到该岗位信息');
      jobDetail.value = null;
    }
  } catch (error) {
    // 过滤掉用户主动取消的请求错误，避免不必要的错误提示
    if (error.name !== 'AbortError') {
      console.error('获取岗位详情失败:', error);
      console.error('获取岗位详情失败: ' + (error.response?.data?.message || error.message));
    }
    jobDetail.value = null;
  }
};

const fetchRelatedJobs = async (category) => {
  try {
    // 稍微延迟加载相关岗位，让主内容先显示
    await new Promise(resolve => setTimeout(resolve, 100));

    const response = await request({
      url: '/api/jobs',
      method: 'get',
      params: { category: category, pageSize: 5 } // 获取同类别的5个岗位作为推荐
    });
    if (response && Array.isArray(response)) {
      // 过滤掉当前岗位
      const filteredJobs = response.filter(job => job.id.toString() !== jobId.value);

      // 使用动画显示相关岗位
      relatedJobs.value = filteredJobs;
    }
  } catch (error) {
    console.error('获取相关岗位失败:', error);
    // 即使获取失败也不影响主内容显示
    relatedJobs.value = [];
  }
};

const parseRequirements = (requirements) => {
  if (!requirements) return [];
  if (Array.isArray(requirements)) return requirements;
  if (typeof requirements === 'string') {
    return requirements.split(/\\n|,/g).map(item => item.trim()).filter(Boolean);
  }
  return [];
};

// 解析内容的方法
const parseContent = (content) => {
  if (!content) return [];
  if (Array.isArray(content)) return content;
  if (typeof content === 'string') {
    // 首先尝试按换行符分割
    const byNewline = content.split('\n').map(item => item.trim()).filter(Boolean);
    if (byNewline.length > 1) return byNewline;

    // 如果没有换行符，则按逗号分割
    return content.split(/[,，]/).map(item => item.trim()).filter(Boolean);
  }
  return [];
};

// 计算属性
const parsedDuties = computed(() => {
  if (!jobDetail.value?.duties) return [];
  return parseContent(jobDetail.value.duties);
});

const parsedRequirements = computed(() => {
  if (!jobDetail.value?.requirements) return [];
  return parseContent(jobDetail.value.requirements);
});

const getCategoryType = (category) => {
  const categoryMap = {
    '人工智能': 'success',
    '大数据': 'primary',
    '物联网': 'warning',
    '智能系统': 'danger',
  };
  return categoryMap[category] || 'default';
};

// 简历选择相关
const resumeSelectVisible = ref(false)
const resumeList = ref([])
const selectedResumeId = ref(null)

async function applyInterview() {
  await fetchResumeList()
  resumeSelectVisible.value = true
}

async function fetchResumeList() {
  try {
    const response = await request({
      url: '/api/resumes',
      method: 'get'
    });
    if (response && Array.isArray(response)) {
      resumeList.value = response.map(resume => ({
        id: resume.id,
        name: resume.name,
        type: resume.type,
        updateTime: resume.updateTime
      }));
      if (resumeList.value.length > 0) {
        selectedResumeId.value = resumeList.value[0].id; // 默认选中第一个简历
      }
    } else {
      console.error('获取简历列表失败');
    }
  } catch (error) {
    console.error('获取简历列表失败: ' + (error.response?.data?.message || error.message));
  }
}

async function confirmResumeSelect() {
  if (!selectedResumeId.value) {
    console.error('请选择一个简历')
    return
  }
  try {
    const { applyJobWithResume } = await import('@/api/application')
    const response = await applyJobWithResume({
      jobId: jobId.value,
      resumeId: selectedResumeId.value
    })
    if (response) {
      const pendingJobData = {
        id: jobDetail.value.id,
        title: jobDetail.value.title,
        companyName: jobDetail.value.company.name,
        salary: jobDetail.value.salary,
        location: jobDetail.value.location,
        category: jobDetail.value.category
      };
      interviewStore.setStatus('pending1');
      interviewStore.setJob(pendingJobData);
      localStorage.setItem('interviewStatus', 'pending1');
      localStorage.setItem('pendingInterviewJob', JSON.stringify(pendingJobData));
      console.log('简历已成功投递');
      resumeSelectVisible.value = false;
      router.push('/');
    } else {
      throw new Error('投递简历失败');
    }
  } catch (error) {
    console.error(error);
    if (error.response && error.response.status === 409) {
      console.error('您已投递过该岗位，请勿重复投递');
    } else {
      console.error('投递简历失败: ' + (error.response?.data?.message || error.message));
    }
  }
}

function navigateToJob(id) {
  // 如果是当前页面，不需要跳转
  if (id === jobId.value) {
    return;
  }

  // 使用replace而不是push，避免历史记录堆积
  router.push({ name: 'job-detail', params: { id } });
}

function goBackToJobs() {
  router.push({ name: 'jobs' });
}

const navigateToCompany = (companyId) => {
  if (companyId) {
    router.push({ name: 'company-detail', params: { id: companyId } });
  }
};

onMounted(() => {
  fetchJobDetail();
});

// 监听路由变化，实现平滑切换
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    console.log('路由切换:', oldId, '->', newId);

    // 立即重置页面状态
    resetPageState();
    jobId.value = newId;

    // 使用nextTick确保DOM更新后再获取数据
    nextTick(() => {
      fetchJobDetail();
    });
  }
}, {
  immediate: false // 不立即执行，因为onMounted已经处理了初始加载
});

</script>

<style scoped>
.job-detail-container {
  padding: 32px;
  max-width: 1600px;
  margin: 0 auto;
  min-height: calc(100vh - 128px); /* Adjust based on your layout's header/footer */
}

.el-row {
  margin: 0 !important;
}

.job-detail-card, .related-jobs-card {
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  margin: 0 12px;
  background-color: #ffffff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafafa;
  border-radius: 12px 12px 0 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-actions .el-tag {
  border-radius: 6px;
  font-size: 12px;
  padding: 4px 10px;
  font-weight: 500;
}

.card-header h1 {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.3;
  color: #2c3e50;
}

.title-section {
  flex: 1;
}

.job-description {
  margin: 8px 0 0 0;
  font-size: 15px;
  color: #5a6c7d;
  line-height: 1.5;
  font-weight: 400;
}

.job-content {
  padding: 24px;
  background-color: #ffffff;
}

.job-salary {
  font-size: 22px;
  color: #e74c3c;
  font-weight: 600;
  margin: 0 0 20px 0;
  padding: 12px 16px;
  background: linear-gradient(135deg, #fff5f5 0%, #ffe6e6 100%);
  border-radius: 8px;
  border-left: 4px solid #e74c3c;
  display: inline-block;
}

.job-tags {
  margin-bottom: 32px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.job-tags .el-tag {
  margin-right: 0;
  font-size: 13px;
  padding: 6px 12px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  color: #495057;
  border-radius: 6px;
}

h3 {
  font-size: 17px;
  color: #2c3e50;
  margin: 32px 0 16px 0;
  font-weight: 600;
  padding-bottom: 8px;
  border-bottom: 2px solid #3498db;
  display: inline-block;
}

.requirements-list {
  padding-left: 0;
  list-style: none;
  margin: 0 0 24px 0;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
}

.requirements-list li {
  margin-bottom: 12px;
  line-height: 1.6;
  color: #495057;
  position: relative;
  padding-left: 20px;
  font-size: 14px;
}

.requirements-list li:before {
  content: '▸';
  position: absolute;
  left: 0;
  color: #3498db;
  font-weight: bold;
}

.requirements-list li:last-child {
  margin-bottom: 0;
}

.company-info {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  background-color: #f8f9fa;
  cursor: pointer;
  margin-top: 8px;
  transition: background-color 0.2s ease;
}

.company-info:hover {
  background-color: #e9ecef;
  border-color: #dee2e6;
}

.company-detail h3 {
  margin: 0 0 6px 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: none;
  padding-bottom: 0;
  display: block;
}

.company-detail p {
  margin: 0;
  color: #6c757d;
  font-size: 13px;
  line-height: 1.4;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
  background-color: #fafafa;
  border-radius: 0 0 12px 12px;
}

.related-jobs-card {
  height: fit-content;
}

.related-jobs-card .card-header span {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.related-jobs-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 500px;
  overflow-y: auto;
  padding: 16px 20px 20px;
}

.related-job-item {
  cursor: pointer;
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  background-color: #ffffff;
  transition: background-color 0.2s ease;
}

.related-job-item:hover {
  background-color: #f8f9fa;
  border-color: #dee2e6;
}

.related-job-title {
  font-weight: 600;
  font-size: 15px;
  color: #2c3e50;
  margin-bottom: 6px;
}

.related-job-salary {
  color: #e74c3c;
  margin: 4px 0;
  font-size: 14px;
  font-weight: 600;
}

.related-job-location {
  color: #6c757d;
  font-size: 13px;
  margin: 0;
}

/* 弹窗样式优化 */
.resume-dialog {
  border-radius: 16px;
}

.resume-dialog .el-dialog {
  border-radius: 16px;
  overflow: hidden;
}

.resume-dialog .el-dialog__header {
  display: none;
}

.resume-dialog .el-dialog__body {
  padding: 0;
}

.resume-dialog .el-dialog__footer {
  padding: 0;
  border-top: 1px solid #f0f0f0;
}

/* 弹窗头部 */
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 24px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
}

.header-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #3498db, #2980b9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
}

.header-text h3 {
  margin: 0 0 4px 0;
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
  border-bottom: none;
  padding-bottom: 0;
  display: block;
}

.header-text p {
  margin: 0;
  font-size: 14px;
  color: #6c757d;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  background-color: #ffffff;
  color: #6c757d;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background-color: #f8f9fa;
  border-color: #dee2e6;
}

/* 弹窗内容区域 */
.dialog-content {
  padding: 24px;
  max-height: 400px;
  overflow-y: auto;
}

/* 简历列表 */
.resume-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resume-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border: 2px solid #e9ecef;
  border-radius: 12px;
  background-color: #ffffff;
  cursor: pointer;
  transition: all 0.3s ease;
}

.resume-card:hover {
  border-color: #3498db;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.1);
}

.resume-card.selected {
  border-color: #3498db;
  background-color: #f8fbff;
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.15);
}

.resume-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.resume-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ecf0f1, #bdc3c7);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7f8c8d;
  font-size: 16px;
}

.resume-card.selected .resume-icon {
  background: linear-gradient(135deg, #3498db, #2980b9);
  color: white;
}

.resume-info {
  flex: 1;
}

.resume-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.resume-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.resume-type,
.resume-time {
  font-size: 13px;
  color: #6c757d;
  display: flex;
  align-items: center;
  gap: 4px;
}

.resume-type i,
.resume-time i {
  font-size: 12px;
}

.resume-card-action {
  margin-left: 12px;
}

.radio-indicator {
  width: 24px;
  height: 24px;
  border: 2px solid #dee2e6;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.radio-indicator.active {
  border-color: #3498db;
  background-color: #3498db;
  color: white;
}

.radio-indicator i {
  font-size: 12px;
}

/* 空状态 */
.empty-resume-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ecf0f1, #bdc3c7);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #95a5a6;
  font-size: 32px;
}

.empty-resume-state h4 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #2c3e50;
}

.empty-resume-state p {
  margin: 0 0 24px 0;
  color: #6c757d;
  font-size: 14px;
}

/* 底部按钮区域 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  background-color: #f8f9fa;
}

.cancel-btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  background-color: #ffffff;
  color: #6c757d;
  font-weight: 500;
}

.cancel-btn:hover {
  background-color: #f8f9fa;
  border-color: #dee2e6;
}

.confirm-btn {
  padding: 10px 24px;
  border-radius: 8px;
  background: linear-gradient(135deg, #3498db, #2980b9);
  border: none;
  color: white;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.confirm-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #2980b9, #1f5f8b);
  box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
}

.confirm-btn:disabled {
  background: linear-gradient(135deg, #bdc3c7, #95a5a6);
  cursor: not-allowed;
}

/* 滚动条样式 */
.dialog-content::-webkit-scrollbar {
  width: 6px;
}

.dialog-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.dialog-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.dialog-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 页面过渡动画 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: all 0.3s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}



/* 内容容器 */
.content-container {
  animation: content-fade-in 0.4s ease-out;
}

@keyframes content-fade-in {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 错误状态 */
.error-container {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}



/* 响应式优化 */
@media (max-width: 768px) {
  .page-fade-enter-from,
  .page-fade-leave-to {
    transform: translateX(100%);
  }
}
</style> 