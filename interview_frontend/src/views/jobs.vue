<template>
  <div class="job-selection-container">
    <!-- 渐变背景 -->
    <div class="page-background"></div>

    <!-- 标题与搜索一体化区域 -->
    <div class="unified-search-card">
      <div class="title-content">
        <h1 class="main-title">岗位搜索</h1>
        <p class="sub-title">发现理想职位，开启职业新篇章</p>
      </div>
      <div class="section-divider"></div>
      <div class="search-section-elegant">
        <!-- 主搜索框区域 -->
        <div class="search-primary">
          <div class="search-input-wrapper">
            <el-input
                v-model="searchKeyword"
                placeholder="搜索岗位关键词..."
                class="search-input-elegant"
                :prefix-icon="Search"
                clearable
                @keyup.enter="fetchJobs"
                size="large"
            />
          </div>
        </div>

        <!-- 统一搜索框架 -->
        <div class="unified-search-box">
          <!-- 筛选条件区域 -->
          <div class="filter-section-unified">
            <div class="filter-group">
              <div class="filter-row">
                <div class="filter-item">
                  <label class="filter-label">地点</label>
                  <el-select v-model="selectedLocation" placeholder="选择地点" class="filter-select-elegant" clearable>
                    <el-option label="全部地点" value="" />
                    <el-option label="北京" value="北京" />
                    <el-option label="上海" value="上海" />
                    <el-option label="深圳" value="深圳" />
                    <el-option label="广州" value="广州" />
                    <el-option label="杭州" value="杭州" />
                  </el-select>
                </div>
                <div class="filter-item">
                  <label class="filter-label">学历</label>
                  <el-select v-model="selectedEducation" placeholder="选择学历" class="filter-select-elegant" clearable>
                    <el-option label="全部学历" value="" />
                    <el-option label="大专及以上" value="大专及以上" />
                    <el-option label="本科及以上" value="本科及以上" />
                    <el-option label="硕士及以上" value="硕士及以上" />
                    <el-option label="博士" value="博士" />
                  </el-select>
                </div>
                <div class="filter-item">
                  <label class="filter-label">类型</label>
                  <el-select v-model="selectedWorkType" placeholder="选择类型" class="filter-select-elegant" clearable>
                    <el-option label="全部类型" value="" />
                    <el-option label="全职" value="0" />
                    <el-option label="实习" value="1" />
                  </el-select>
                </div>
                <div class="filter-item">
                  <label class="filter-label">薪资</label>
                  <el-select v-model="selectedSalary" placeholder="选择薪资" class="filter-select-elegant" clearable>
                    <el-option label="不限" value="" />
                    <el-option label="10k以下" value="10k以下" />
                    <el-option label="10k-20k" value="10k-20k" />
                    <el-option label="20k-30k" value="20k-30k" />
                    <el-option label="30k以上" value="30k以上" />
                  </el-select>
                </div>
              </div>
            </div>
          </div>

          <!-- 分隔线 -->
          <div class="section-separator"></div>

          <!-- 分类区域 -->
          <div class="category-section-unified">
            <div class="category-wrapper">
              <label class="category-label-unified">岗位分类</label>
              <el-radio-group v-model="selectedCategory" class="category-radio-unified">
                <el-radio-button label="">全部</el-radio-button>
                <el-radio-button label="ai">人工智能</el-radio-button>
                <el-radio-button label="bigdata">大数据</el-radio-button>
                <el-radio-button label="iot">物联网</el-radio-button>
                <el-radio-button label="system">智能系统</el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <!-- 分隔线 -->
          <div class="section-separator"></div>

          <!-- 操作按钮区域 -->
          <div class="action-buttons-unified">
            <el-button type="primary" @click="fetchJobs" :loading="loading" class="search-btn-unified">
              <i class="el-icon-search" style="margin-right: 8px;"></i>搜索岗位
            </el-button>
            <el-button @click="resetFilters" class="reset-btn-unified">
              <i class="el-icon-refresh" style="margin-right: 8px;"></i>重置筛选
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 岗位列表 -->
    <div class="job-list-container">
      <div class="job-list-header">
        <h2>岗位列表</h2>
        <div>共找到 {{ jobs.length }} 个符合条件的岗位</div>
      </div>
      <div class="job-list" v-loading="loading">
        <template v-if="pagedJobs.length > 0">
          <div v-for="job in pagedJobs" :key="job.id" class="job-card-beauty" @click="navigateToJobDetail(job)">
            <div class="job-header">
              <div class="job-title-row">
                <h2>{{ job.title }}</h2>
                <div class="job-tags-right">
                  <el-tag :type="job.categoryType" class="job-category-tag">{{ job.category }}</el-tag>
                  <el-tag
                      v-if="getApplicationStatus(job.id).applied"
                      :type="getApplicationStatus(job.id).type"
                      class="job-application-tag"
                      size="default"
                      effect="dark"
                  >
                    {{ getApplicationStatus(job.id).text }}
                  </el-tag>
                </div>
              </div>
              <div class="job-salary">{{ job.salary }}</div>
            </div>
            <div class="job-info">
              <div class="job-tags">
                <el-tag size="small" effect="plain">{{ job.location }}</el-tag>
                <el-tag size="small" effect="plain">{{ job.experience }}</el-tag>
                <el-tag size="small" effect="plain">{{ job.education }}</el-tag>
                <el-tag v-if="job.workType !== null" size="small" effect="plain">{{ job.workType === 0 ? '全职' : '实习' }}</el-tag>
              </div>
            </div>
            <div class="job-requirements">
              <h3>岗位要求：</h3>
              <ul>
                <li v-for="(requirement, index) in job.requirements" :key="index">
                  {{ requirement }}
                </li>
              </ul>
            </div>
            <div class="company-info">
              <el-avatar :size="48" :src="job.company.logoUrl" />
              <div class="company-detail">
                <h3>{{ job.company.name }}</h3>
                <p>{{ job.company.slogan }}</p>
              </div>
            </div>
            <div class="job-footer">
              <div class="job-apply-hint">点击查看详情</div>
            </div>
          </div>
        </template>
        <el-empty v-else description="未找到符合条件的岗位" />
      </div>
    </div>
    <div class="job-pagination">
      <el-pagination
          background
          layout="prev, pager, next"
          :total="jobs.length"
          :page-size="pageSize"
          v-model:current-page="currentPage"
          :pager-count="5"/>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { useInterviewStore } from '@/store/interview'
import { createApplication, getUserApplications } from '@/api/application'

const router = useRouter()
const searchKeyword = ref('')
const selectedLocation = ref('')
const selectedEducation = ref('')
const selectedSalary = ref('')
const selectedCategory = ref('')
const selectedWorkType = ref('')

const jobs = ref([])
const userApplications = ref([]) // 存储用户投递记录
const userId = ref(1) // 实际应用中应从用户状态获取
const currentPage = ref(1)

// 响应式窗口大小
const windowWidth = ref(window.innerWidth)

// 动态计算每页显示的岗位数
const pageSize = computed(() => {
  // 使用响应式的窗口宽度
  const screenWidth = windowWidth.value
  let itemsPerRow = 1
  
  if (screenWidth >= 1200) {
    const containerWidth = screenWidth - 120 // 减去页面边距
    itemsPerRow = Math.floor(containerWidth / 404) // 380px + 24px gap
  } else if (screenWidth >= 768) {
    // 中等屏幕：固定2列
    itemsPerRow = 2
  } else {
    // 小屏幕：1列
    itemsPerRow = 1
  }
  
  // 至少为1，最多为4
  itemsPerRow = Math.max(1, Math.min(4, itemsPerRow))
  
  // 自适应行数，最多8条
  const maxItems = 8
  const rowsNeeded = Math.ceil(maxItems / itemsPerRow)
  
  // 返回实际能显示的数量，但不超过8条
  return Math.min(itemsPerRow * rowsNeeded, maxItems)
})

const pagedJobs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return jobs.value.slice(start, start + pageSize.value)
})
// 检查岗位是否已投递
const isJobApplied = (jobId) => {
  return userApplications.value.some(app => app.jobId === jobId)
}

// 获取岗位的投递状态信息
const getApplicationStatus = (jobId) => {
  return {
    applied: isJobApplied(jobId),
    text: isJobApplied(jobId) ? '已投递' : '未投递',
    type: isJobApplied(jobId) ? 'warning' : 'primary'
  }
}

const interviewStore = useInterviewStore()

// 添加加载状态
const loading = ref(false);

// 筛选函数，对应后端的筛选参数
const selectedCategoryValue = computed(() => {
  const categoryMap = {
    'ai': '人工智能',
    'bigdata': '大数据',
    'iot': '物联网',
    'system': '智能系统'
  };
  return categoryMap[selectedCategory.value] || '';
});

// 获取用户投递记录
const fetchUserApplications = async () => {
  try {
    console.log('开始获取用户投递记录...');
    const applications = await getUserApplications();
    console.log('用户投递记录:', applications);

    if (Array.isArray(applications)) {
      userApplications.value = applications;
    } else {
      userApplications.value = [];
    }
  } catch (error) {
    console.error('获取用户投递记录失败:', error);
    userApplications.value = [];
  }
};
// 获取岗位数据
const fetchJobs = async () => {
  try {
    console.log('开始获取岗位数据...');
    loading.value = true;

    // 同时获取岗位数据和用户投递记录
    await Promise.all([
      fetchJobsData(),
      fetchUserApplications()
    ]);
  } catch (error) {
    console.error('获取数据失败:', error);
  } finally {
    loading.value = false;
  }
}

// 获取岗位数据的具体实现
const fetchJobsData = async () => {
  // 构建查询参数，过滤掉空值
  const params = {};
  if (searchKeyword.value) params.keyword = searchKeyword.value;
  if (selectedLocation.value) params.location = selectedLocation.value;
  if (selectedEducation.value) params.education = selectedEducation.value;
  if (selectedSalary.value) params.salary = selectedSalary.value;
  if (selectedWorkType.value) params.workType = selectedWorkType.value;

  const categoryValue = selectedCategoryValue.value;
  if (categoryValue) {
    params.category = categoryValue;
  }

  console.log('查询参数:', params);

  const response = await request({
    url: '/api/jobs',
    method: 'get',
    params
  });
  console.log('获取到岗位数据:', response);

  if (response && Array.isArray(response)) {
    jobs.value = response.map(job => {
      let requirements = [];
      if (job.requirements && typeof job.requirements === 'string') {
        // 如果包含逗号，按逗号分割
        if (job.requirements.includes(',')) {
          requirements = job.requirements.split(',').map(item => item.trim()).filter(Boolean);
        } else {
          // 否则按换行符分割
          requirements = job.requirements.split('\n').filter(line => line.trim() !== '');
        }
      }

      return {
        ...job,
        requirements: requirements,
        company: job.company || { name: '未知公司', logoUrl: '/default-company-logo.png' },
        categoryType: getCategoryType(job.category)
      };
    });

    if (jobs.value.length === 0) {
      ElMessage.info('没有找到符合条件的岗位');
    } else {
      console.log(`成功加载了${jobs.value.length}个岗位`);
    }
  } else {
    console.error('获取到的岗位数据格式不正确:', response);
    jobs.value = [];
    console.error('获取岗位数据格式不正确');
  }
}

// 根据分类返回对应的类型，用于标签颜色
const getCategoryType = (category) => {
  const categoryMap = {
    '人工智能': 'success',
    '大数据': 'primary',
    '物联网': 'warning',
    '智能系统': 'danger'
  };
  return categoryMap[category] || 'default';
}

// 当筛选条件变化时重新获取数据
watch([searchKeyword, selectedLocation, selectedEducation, selectedSalary, selectedCategory, selectedWorkType], () => {
  fetchJobs()
})

// 响应式窗口大小变化处理
const handleResize = () => {
  windowWidth.value = window.innerWidth
  // 当窗口大小变化导致pageSize变化时，重置到第一页
  currentPage.value = 1
}

// 初始化时获取数据
onMounted(() => {
  fetchJobs()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

function navigateToJobDetail(job) {
  router.push({ name: 'job-detail', params: { id: job.id } });
}

// 重置筛选条件
function resetFilters() {
  searchKeyword.value = '';
  selectedLocation.value = '';
  selectedEducation.value = '';
  selectedSalary.value = '';
  selectedCategory.value = '';
  selectedWorkType.value = '';
  fetchJobs();
}
</script>

<style scoped>
/* 页面整体布局 */
.job-selection-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 0;
  margin: 0;
  max-width: none;
}

.page-background {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #A8DADC 0%, #457B9D 50%, #264653 100%);
  z-index: -1;
}

.unified-search-card {
  position: relative;
  margin: 0;
  padding: 50px 50px 40px 50px;
  text-align: center;
  color: white;
  animation: fadeInUp 0.8s ease-out;
}

.title-content {
  position: relative;
  z-index: 2;
  margin-bottom: 24px;
}

.main-title {
  font-size: 42px;
  font-weight: 800;
  margin: 0 0 12px 0;
  color: #ffffff;
  text-shadow: 0 4px 16px rgba(0, 0, 0, 0.5), 0 2px 8px rgba(0, 0, 0, 0.4);
  letter-spacing: 2px;
}

.sub-title {
  font-size: 18px;
  margin: 0;
  color: rgba(255, 255, 255, 0.95);
  font-weight: 500;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.4), 0 1px 4px rgba(0, 0, 0, 0.3);
  letter-spacing: 1px;
}

.section-divider {
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #ffffff 0%, rgba(255, 255, 255, 0.6) 50%, rgba(255, 255, 255, 0.2) 100%);
  margin: 0 auto 28px auto;
  border-radius: 2px;
  position: relative;
  z-index: 2;
  box-shadow: 0 2px 6px rgba(255, 255, 255, 0.3);
}

.search-section-elegant {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1000px;
  margin: 0 auto;
}

.search-primary {
  display: flex;
  justify-content: center;
  margin-bottom: 4px;
}

.search-input-wrapper {
  width: 100%;
  max-width: 600px;
}

.search-input-elegant {
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

:deep(.search-input-elegant.el-input--large .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.98);
  border: 2px solid rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(15px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  border-radius: 18px;
  padding: 10px 16px;
  height: 42px;
}

:deep(.search-input-elegant .el-input__inner) {
  color: #2c3e50;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.5;
}

:deep(.search-input-elegant .el-input__inner::placeholder) {
  color: #95a5a6;
}

/* 下拉菜单样式优化 */
:deep(.el-select-dropdown) {
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(168, 218, 220, 0.3);
  border-radius: 16px;
  backdrop-filter: blur(20px);
  box-shadow: 0 12px 48px rgba(38, 70, 83, 0.15);
}

:deep(.el-select-dropdown .el-select-dropdown__item) {
  color: #2c3e50;
  font-weight: 500;
  padding: 12px 16px;
  border-radius: 8px;
  margin: 4px 8px;
  transition: all 0.2s ease;
}

:deep(.el-select-dropdown .el-select-dropdown__item:hover) {
  background: rgba(168, 218, 220, 0.15);
  color: #457B9D;
}

:deep(.el-select-dropdown .el-select-dropdown__item.selected) {
  background: #457B9D;
  color: #F1FAEE;
  font-weight: 600;
}

.unified-search-box {
  background: none;
  border-radius: 0;
  padding: 16px 0;
  backdrop-filter: none;
  border: none;
  box-shadow: none;
}

.filter-section-unified {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 16px;
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.filter-group {
  width: 100%;
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  align-items: end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 4px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

.filter-select-elegant {
  border-radius: 12px;
}

:deep(.filter-select-elegant .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  height: 36px;
}

:deep(.filter-select-elegant .el-input__inner) {
  color: #2c3e50;
  font-weight: 500;
  font-size: 14px;
}

:deep(.filter-select-elegant .el-input__inner::placeholder) {
  color: #95a5a6;
}

.section-separator {
  height: 1px;
  background: linear-gradient(90deg,
  transparent 0%,
  rgba(255, 255, 255, 0.3) 20%,
  rgba(255, 255, 255, 0.6) 50%,
  rgba(255, 255, 255, 0.3) 80%,
  transparent 100%
  );
  margin: 16px 0;
}

.category-section-unified {
  padding: 0;
  background: none;
  border: none;
  border-radius: 0;
}

.category-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.category-label-unified {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
  margin: 0;
}

.category-radio-unified {
  background: rgba(255, 255, 255, 0.1);
  padding: 4px;
  border-radius: 14px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

:deep(.category-radio-unified .el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
  border-radius: 10px;
  margin: 0 2px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-weight: 500;
  font-size: 13px;
  padding: 6px 14px;
  backdrop-filter: blur(8px);
}

:deep(.category-radio-unified .el-radio-button__inner:hover) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.35);
  transform: translateY(-1px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
}

:deep(.category-radio-unified .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #F1FAEE 0%, #E9F2F5 100%);
  color: #264653;
  border-color: #F1FAEE;
  box-shadow: 0 3px 12px rgba(241, 250, 238, 0.4);
  transform: translateY(-1px);
  font-weight: 600;
}

/* 操作按钮区域（在统一框架内） */
.action-buttons-unified {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin: 0;
}

.search-btn-unified {
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border: none !important;
  color: #F1FAEE !important;
  border-radius: 16px !important;
  padding: 10px 28px !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 4px 16px rgba(38, 70, 83, 0.35) !important;
  letter-spacing: 0.5px;
  height: 38px !important;
  min-width: 120px;
}

.search-btn-unified:hover {
  transform: translateY(-3px) scale(1.03) !important;
  box-shadow: 0 10px 30px rgba(38, 70, 83, 0.45) !important;
  background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important;
}

.reset-btn-unified {
  background: rgba(255, 255, 255, 0.12) !important;
  border: 1px solid rgba(255, 255, 255, 0.3) !important;
  color: rgba(255, 255, 255, 0.95) !important;
  border-radius: 16px !important;
  padding: 10px 24px !important;
  font-weight: 500 !important;
  font-size: 14px !important;
  transition: all 0.3s ease !important;
  backdrop-filter: blur(12px);
  height: 38px !important;
  min-width: 100px;
}

.reset-btn-unified:hover {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.5) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2) !important;
}


/* 岗位列表容器 */
.job-list-container {
  margin: 20px 20px 20px 20px;
  padding: 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(25px);
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.8s ease-out 0.4s both;
}

.job-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 2px solid rgba(69, 123, 157, 0.2);
}

.job-list-header h2 {
  font-size: 28px;
  color: #457B9D;
  margin: 0;
  font-weight: 700;
}

.job-list-header div {
  color: #666;
  font-size: 16px;
  font-weight: 500;
}

.job-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 24px;
  margin-top: 24px;
}

.job-card-beauty {
  border-radius: 24px;
  padding: 28px 24px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 15px;
  position: relative;
  cursor: pointer;
  border: 1px solid rgba(168, 218, 220, 0.3);
  box-shadow: 0 4px 20px rgba(38, 70, 83, 0.1);
  overflow: hidden;
  animation: scaleIn 0.6s ease-out;
  animation-fill-mode: both;
}

.job-card-beauty:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 20px 60px rgba(69, 123, 157, 0.2);
  border-color: #457B9D;
}

.job-card-beauty::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #457B9D, #264653);
  transform: scaleX(0);
  transition: transform 0.3s ease;
  transform-origin: left;
}

.job-card-beauty:hover::before {
  transform: scaleX(1);
}
/* 岗位卡片内容样式 */
.job-header {
  margin-bottom: 16px;
}

.job-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 12px;
}

.job-title-row h2 {
  margin: 0;
  font-size: 22px;
  color: #264653;
  font-weight: 700;
  flex: 1;
  line-height: 1.3;
}

.job-tags-right {
  display: flex;
  gap: 6px;
  align-items: center;
}

.job-category-tag {
  font-size: 12px;
  font-weight: 600;
  border-radius: 12px;
  padding: 4px 12px;
  flex-shrink: 0;
}

.job-application-tag {
  font-size: 14px !important;
  font-weight: 700 !important;
  padding: 4px 12px !important;
  border-radius: 6px !important;
  border: 2px solid !important;
  letter-spacing: 0.5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
}

.job-application-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

/* 自定义已投递（黄色）标签样式 */
.job-application-tag.el-tag--warning.el-tag--dark {
  background-color: #f39c12 !important;
  border-color: #f39c12 !important;
  color: #ffffff !important;
}

/* 自定义未投递（蓝色）标签样式 */
.job-application-tag.el-tag--primary.el-tag--dark {
  background-color: #3498db !important;
  border-color: #3498db !important;
  color: #ffffff !important;
}

.job-salary {
  color: #e74c3c;
  font-size: 20px;
  font-weight: 800;
  background: linear-gradient(135deg, #e74c3c, #f39c12);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.job-info {
  margin-bottom: 16px;
}

.job-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.job-tags .el-tag {
  font-size: 12px;
  border-radius: 12px;
  padding: 4px 12px;
  background: rgba(168, 218, 220, 0.15);
  border: 1px solid rgba(168, 218, 220, 0.3);
  color: #457B9D;
  font-weight: 500;
}

.job-requirements {
  margin: 16px 0;
  background: rgba(168, 218, 220, 0.08);
  padding: 16px;
  border-radius: 16px;
  border-left: 4px solid #457B9D;
}

.job-requirements h3 {
  font-size: 16px;
  color: #457B9D;
  margin: 0 0 12px 0;
  font-weight: 700;
}

.job-requirements ul {
  margin: 0;
  padding-left: 20px;
}

.job-requirements li {
  color: #555;
  margin-bottom: 6px;
  font-size: 14px;
  line-height: 1.6;
}

.company-info {
  display: flex;
  align-items: center;
  padding: 16px 0 0 0;
  border-top: 2px solid rgba(168, 218, 220, 0.2);
  gap: 16px;
  margin-bottom: 12px;
}

.company-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.company-detail h3 {
  margin: 0;
  font-size: 16px;
  color: #264653;
  font-weight: 600;
}

.company-detail p {
  margin: 0;
  font-size: 14px;
  color: #7f8c8d;
  line-height: 1.4;
}

.job-footer {
  display: flex;
  justify-content: center;
  margin-top: 12px;
}

.job-apply-hint {
  font-size: 14px;
  color: #457B9D;
  font-weight: 500;
  padding: 8px 16px;
  background: rgba(168, 218, 220, 0.15);
  border-radius: 20px;
  border: 1px solid rgba(168, 218, 220, 0.3);
  transition: all 0.3s ease;
}

.job-card-beauty:hover .job-apply-hint {
  background: #457B9D;
  color: #F1FAEE;
  transform: scale(1.05);
}
/* 分页样式 */
.job-pagination {
  margin: 16px 20px 36px 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(25px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
  animation: fadeInUp 0.8s ease-out 0.6s both;
}

:deep(.job-pagination .el-pagination) {
  font-weight: 600;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
  padding: 0;
  gap: 8px 0;
}

:deep(.job-pagination .el-pager) {
  display: flex;
  flex-wrap: wrap; /* 多页时分页数字可换行 */
  row-gap: 8px;
}

:deep(.job-pagination .el-pager li) {
  background: rgba(168, 218, 220, 0.15);
  border: 1px solid rgba(168, 218, 220, 0.3);
  color: #457B9D;
  margin: 0 4px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.job-pagination .el-pager li:hover) {
  background: #457B9D;
  color: #F1FAEE;
  transform: scale(1.1);
}

:deep(.job-pagination .el-pager li.is-active) {
  background: #457B9D;
  color: #F1FAEE;
  box-shadow: 0 4px 12px rgba(69, 123, 157, 0.3);
}

/* 动画定义 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(40px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 为岗位卡片添加延迟动画 */
.job-card-beauty:nth-child(1) { animation-delay: 0.1s; }
.job-card-beauty:nth-child(2) { animation-delay: 0.2s; }
.job-card-beauty:nth-child(3) { animation-delay: 0.3s; }
.job-card-beauty:nth-child(4) { animation-delay: 0.4s; }
.job-card-beauty:nth-child(5) { animation-delay: 0.5s; }
.job-card-beauty:nth-child(6) { animation-delay: 0.6s; }

/* 响应式设计优化 */
@media (max-width: 1200px) {
  .job-list {
    grid-template-columns: repeat(2, 1fr);
    gap: 20px;
  }

  .unified-search-card {
    padding: 40px 32px 32px 32px;
  }

  .search-section-elegant {
    gap: 20px;
    max-width: none;
  }

  .filter-row {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 12px;
  }

  .unified-search-box {
    padding: 14px 0;
  }

  .section-separator {
    margin: 14px 0;
  }

  .job-list-container {
    margin: 16px;
    padding: 24px;
  }
}

@media (max-width: 768px) {
  .main-title {
    font-size: 32px;
    letter-spacing: 1.5px;
  }

  .sub-title {
    font-size: 16px;
    letter-spacing: 0.5px;
  }

  .unified-search-card {
    padding: 32px 20px 28px 20px;
  }

  .title-content {
    margin-bottom: 20px;
  }

  .section-divider {
    margin-bottom: 20px;
    width: 50px;
  }

  .search-section-elegant {
    gap: 16px;
  }

  .search-input-wrapper {
    max-width: none;
  }

  .filter-section-unified {
    padding: 16px;
  }

  .filter-row {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .unified-search-box {
    padding: 12px 0;
  }

  .section-separator {
    margin: 12px 0;
  }

  .category-wrapper {
    gap: 10px;
  }

  .category-radio-unified {
    overflow-x: auto;
    white-space: nowrap;
    padding: 4px 8px;
  }

  .action-buttons-unified {
    gap: 12px;
    flex-direction: column;
    align-items: center;
  }

  .search-btn-unified,
  .reset-btn-unified {
    width: 100%;
    max-width: 200px;
  }

  .job-list {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .job-list-container {
    margin: 12px;
    padding: 16px;
  }

  .job-list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 20px;
  }

  .job-list-header h2 {
    font-size: 24px;
  }

  .job-card-beauty {
    padding: 20px 16px;
  }

  .job-title-row h2 {
    font-size: 20px;
  }

  .job-pagination {
    margin: 20px 12px;
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .main-title {
    font-size: 28px;
    letter-spacing: 1px;
  }

  .sub-title {
    font-size: 14px;
    letter-spacing: 0.5px;
  }

  .unified-search-card {
    padding: 28px 16px 24px 16px;
  }

  .section-divider {
    width: 40px;
    margin-bottom: 16px;
  }

  .title-content {
    margin-bottom: 16px;
  }

  .search-section-elegant {
    gap: 10px;
  }

  .filter-section-unified {
    padding: 12px;
  }

  .filter-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .filter-item {
    gap: 4px;
  }

  .filter-label {
    font-size: 12px;
  }

  .unified-search-box {
    padding: 8px 0;
  }

  .section-separator {
    margin: 10px 0;
  }

  .category-label-unified {
    font-size: 13px;
  }

  .category-wrapper {
    gap: 8px;
  }

  :deep(.category-radio-unified .el-radio-button__inner) {
    padding: 5px 10px !important;
    font-size: 12px !important;
  }

  .search-btn-unified,
  .reset-btn-unified {
    font-size: 13px !important;
    padding: 8px 20px !important;
    height: 34px !important;
  }

  .job-card-beauty {
    padding: 16px 12px;
    gap: 12px;
  }

  .job-title-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>