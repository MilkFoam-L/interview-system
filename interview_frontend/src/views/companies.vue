<template>
  <div class="companies-container">
    <!-- 渐变背景 -->
    <div class="page-background"></div>

    <!-- 标题与搜索一体化区域 -->
    <div class="unified-search-card">
      <div class="title-content">
        <h1 class="main-title">企业探索</h1>
        <p class="sub-title">发现优质企业，寻找理想工作机会</p>
      </div>
      <div class="section-divider"></div>
      <div class="search-section-elegant">
        <!-- 主搜索框区域 -->
        <div class="search-primary">
          <div class="search-input-wrapper">
            <el-input
                v-model="searchKeyword"
                placeholder="搜索公司名称、行业、规模等..."
                class="search-input-elegant"
                :prefix-icon="Search"
                clearable
                @keyup.enter="handleSearch"
                size="large"
            />
          </div>
        </div>

        <!-- 统一筛选框架 -->
        <div class="unified-search-box">
          <div class="filter-row">
            <div class="filter-group">
              <label class="filter-label">行业类型</label>
              <el-select v-model="selectedIndustry" placeholder="选择行业" class="filter-select-elegant" clearable>
                <el-option label="全部行业" value="" />
                <el-option label="互联网" value="互联网" />
                <el-option label="人工智能" value="人工智能" />
                <el-option label="大数据" value="大数据" />
                <el-option label="物联网" value="物联网" />
              </el-select>
            </div>

            <div class="filter-group">
              <label class="filter-label">公司规模</label>
              <el-select v-model="selectedSize" placeholder="选择规模" class="filter-select-elegant" clearable>
                <el-option label="全部规模" value="" />
                <el-option label="0-20人" value="0-20人" />
                <el-option label="20-99人" value="20-99人" />
                <el-option label="100-499人" value="100-499人" />
                <el-option label="500人以上" value="500人以上" />
                <el-option label="1000-9999人" value="1000-9999人" />
                <el-option label="10000人以上" value="10000人以上" />
              </el-select>
            </div>

            <div class="filter-group">
              <label class="filter-label">融资阶段</label>
              <el-select v-model="selectedFinancing" placeholder="选择阶段" class="filter-select-elegant" clearable>
                <el-option label="全部阶段" value="" />
                <el-option label="未融资" value="未融资" />
                <el-option label="天使轮" value="天使轮" />
                <el-option label="A轮" value="A轮" />
                <el-option label="B轮" value="B轮" />
                <el-option label="C轮" value="C轮" />
                <el-option label="D轮及以上" value="D轮及以上" />
                <el-option label="上市公司" value="上市公司" />
              </el-select>
            </div>
          </div>

          <!-- 操作按钮区域 -->
          <div class="action-buttons-unified">
            <el-button type="primary" @click="handleSearch" :loading="loading" class="search-btn-unified">
              <i class="el-icon-search" style="margin-right: 8px;"></i>搜索企业
            </el-button>
            <el-button @click="resetFilters" class="reset-btn-unified">
              <i class="el-icon-refresh" style="margin-right: 8px;"></i>重置筛选
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 公司列表 -->
    <div class="companies-list-container">
      <div class="companies-list-header">
        <h2>企业列表</h2>
        <div>共找到 {{ total }} 家符合条件的公司</div>
      </div>
      <div class="companies-list" v-loading="loading">
        <template v-if="companies.length > 0">
          <div v-for="company in companies" :key="company.id" class="company-card-beauty"
               :class="{ 'preloaded': companyPreloadCache.has(company.id) }"
               @click="viewCompanyDetail(company)"
               @mouseenter="preloadCompanyDetail(company.id)"
               @mouseleave="cancelCompanyPreload">
            <div class="company-header">
              <div class="company-logo">
                <el-avatar :size="64" :src="company.logoUrl" />
              </div>
              <div class="company-title-section">
                <div class="company-name-row">
                  <h3>{{ company.name }}</h3>
                  <el-tag type="success" class="company-status-tag">在招</el-tag>
                </div>
                <div class="company-location">
                  <el-icon><Location /></el-icon>{{ company.location }}
                </div>
              </div>
            </div>

            <div class="company-info">
              <div class="company-tags">
                <el-tag size="small" effect="plain">{{ company.industry }}</el-tag>
                <el-tag size="small" effect="plain">{{ company.size }}</el-tag>
                <el-tag size="small" effect="plain" type="success">{{ company.financing }}</el-tag>
              </div>
              <p class="company-desc">{{ company.description }}</p>
            </div>

            <div class="company-benefits">
              <h4>企业福利：</h4>
              <div class="benefits-list">
                <el-tag
                    v-for="(value, key) in company.benefits"
                    :key="key"
                    class="benefit-tag"
                    effect="plain"
                    type="info"
                >
                  {{ key }}
                </el-tag>
              </div>
            </div>

            <div class="company-footer">
              <div class="job-count-badge">{{ company.jobCount }}个在招职位</div>
              <div class="company-apply-hint">点击查看详情</div>
            </div>
          </div>
        </template>
        <el-empty v-else description="未找到符合条件的公司" />
      </div>
    </div>

    <!-- 分页 -->
    <div class="companies-pagination">
      <el-pagination
          background
          layout="pager"
          :total="total"
          :page-size="pageSize"
          v-model:current-page="currentPage"
          :pager-count="5"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Search, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const loading = ref(false)

// 预加载相关
const companyPreloadCache = new Map();
const companyPreloadTimers = new Map();
let companyPreloadController = null;
const searchKeyword = ref('')
const selectedIndustry = ref('')
const selectedSize = ref('')
const selectedFinancing = ref('')

const companies = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 8

// 获取公司列表
const fetchCompanies = async () => {
  try {
    loading.value = true

    const params = {
      keyword: searchKeyword.value,
      industry: selectedIndustry.value,
      companySize: selectedSize.value,
      financing: selectedFinancing.value,
      page: currentPage.value - 1,
      size: pageSize,
      sort: 'id,desc'
    }

    const response = await request({
      url: '/api/companies',
      method: 'get',
      params
    })

    if (response && response.content) {
      companies.value = response.content.map(company => ({
        ...company,
        logoUrl: company.logoUrl || 'https://placeholder.com/100',
      }));
      total.value = response.totalElements;
    } else {
      companies.value = [];
      total.value = 0;
    }
  } catch (error) {
    console.error('获取公司列表失败:', error)
    console.error('获取公司列表失败')
    companies.value = [];
    total.value = 0;
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (currentPage.value !== 1) {
    currentPage.value = 1;
  } else {
    fetchCompanies();
  }
};

// 重置筛选条件
const resetFilters = () => {
  searchKeyword.value = ''
  selectedIndustry.value = ''
  selectedSize.value = ''
  selectedFinancing.value = ''
  handleSearch();
}

// 预加载企业详情数据
const preloadCompanyDetail = (companyId) => {
  // 如果已经缓存了，不需要预加载
  if (companyPreloadCache.has(companyId)) {
    return;
  }

  // 设置延迟预加载，避免用户快速移动鼠标时产生过多请求
  const timer = setTimeout(async () => {
    try {
      // 取消之前的请求
      if (companyPreloadController) {
        companyPreloadController.abort();
      }

      companyPreloadController = new AbortController();

      console.log('预加载企业详情:', companyId);
      const response = await request({
        url: `/api/companies/${companyId}`,
        method: 'get',
        signal: companyPreloadController.signal
      });

      if (response) {
        // 缓存预加载的数据
        companyPreloadCache.set(companyId, {
          data: response,
          timestamp: Date.now()
        });
        console.log('预加载完成:', companyId);
      }
    } catch (error) {
      if (error.name !== 'AbortError') {
        console.log('预加载失败:', companyId, error);
      }
    }
  }, 300); // 300ms后开始预加载

  companyPreloadTimers.set(companyId, timer);
};

// 取消预加载
const cancelCompanyPreload = () => {
  // 清除所有预加载定时器
  for (const [companyId, timer] of companyPreloadTimers) {
    clearTimeout(timer);
  }
  companyPreloadTimers.clear();

  // 取消当前预加载请求
  if (companyPreloadController) {
    companyPreloadController.abort();
    companyPreloadController = null;
  }
};

// 清理过期缓存
const cleanupCompanyCache = () => {
  const now = Date.now();
  const maxAge = 5 * 60 * 1000; // 5分钟过期

  for (const [companyId, cache] of companyPreloadCache) {
    if (now - cache.timestamp > maxAge) {
      companyPreloadCache.delete(companyId);
    }
  }
};

// 查看公司详情
const viewCompanyDetail = (company) => {
  // 清理过期缓存
  cleanupCompanyCache();

  // 将预加载的数据传递给详情页
  const preloadedData = companyPreloadCache.get(company.id);

  router.push({
    name: 'company-detail',
    params: { id: company.id },
    state: { preloadedCompanyData: preloadedData?.data }
  });
}

watch(currentPage, fetchCompanies)

// 初始化
onMounted(() => {
  fetchCompanies()
})

// 组件卸载时清理资源
onUnmounted(() => {
  // 清理所有预加载定时器
  cancelCompanyPreload();

  // 清理预加载缓存
  companyPreloadCache.clear();

  // 取消未完成的请求
  if (companyPreloadController) {
    companyPreloadController.abort();
  }

  console.log('企业列表页资源已清理');
})
</script>

<style scoped>
/* 页面整体布局 */
.companies-container {
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 0;
  margin: 0;
  max-width: none;
}

/* 渐变背景 - 莫兰迪色系 */
.page-background {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #A8DADC 0%, #457B9D 50%, #264653 100%);
  z-index: -1;
}

/* 标题与搜索一体化区域 */
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

/* 优雅搜索区域 */
.search-section-elegant {
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-width: 1000px;
  margin: 0 auto;
}

/* 主搜索框区域 */
.search-primary {
  display: flex;
  justify-content: center;
  margin-bottom: 4px;
}

.search-input-wrapper {
  width: 100%;
  max-width: 500px;
  position: relative;
}

.search-input-elegant {
  --el-input-bg-color: rgba(255, 255, 255, 0.95);
  --el-input-border-color: rgba(255, 255, 255, 0.4);
  --el-input-hover-border-color: rgba(255, 255, 255, 0.8);
  --el-input-focus-border-color: rgba(255, 255, 255, 1);
  border-radius: 30px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(10px);
}

/* 统一搜索框架 */
.unified-search-box {
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 20px 24px 16px 24px;
  margin-top: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
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
  transition: all 0.3s ease;
}

:deep(.filter-select-elegant .el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.filter-select-elegant .el-input__wrapper.is-focus) {
  border-color: rgba(255, 255, 255, 1);
  box-shadow: 0 4px 16px rgba(69, 123, 157, 0.2);
}

:deep(.filter-select-elegant .el-input__inner) {
  color: #2c3e50;
  font-weight: 500;
  font-size: 14px;
}

:deep(.filter-select-elegant .el-input__inner::placeholder) {
  color: #95a5a6;
}

:deep(.filter-select-elegant .el-select__caret) {
  color: #457B9D;
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

/* 操作按钮区域 */
.action-buttons-unified {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 4px;
}

.search-btn-unified, .reset-btn-unified {
  padding: 12px 32px;
  border-radius: 25px;
  font-weight: 600;
  font-size: 15px;
  letter-spacing: 0.5px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.search-btn-unified {
  background: linear-gradient(135deg, #457B9D, #264653);
  border: none;
  color: white;
}

.search-btn-unified:hover {
  background: linear-gradient(135deg, #5a8fb3, #2f5463);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
}

.reset-btn-unified {
  background: rgba(255, 255, 255, 0.2);
  border: 2px solid rgba(255, 255, 255, 0.3);
  color: white;
  backdrop-filter: blur(10px);
}

.reset-btn-unified:hover {
  background: rgba(255, 255, 255, 0.3);
  border-color: rgba(255, 255, 255, 0.5);
  transform: translateY(-2px);
}

/* 公司列表容器 */
.companies-list-container {
  margin: 24px;
  padding: 32px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(38, 70, 83, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.companies-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.companies-list-header h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0;
  color: #264653;
  text-shadow: 0 2px 4px rgba(38, 70, 83, 0.1);
}

.companies-list-header div {
  color: #457B9D;
  font-size: 16px;
  font-weight: 500;
}

.companies-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 24px;
  margin-top: 24px;
}

.company-card-beauty {
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

.company-card-beauty:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 20px 60px rgba(69, 123, 157, 0.2);
  border-color: #457B9D;
}

.company-card-beauty::before {
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

.company-card-beauty:hover::before {
  transform: scaleX(1);
}

/* 公司卡片内容样式 */
.company-header {
  margin-bottom: 16px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.company-logo {
  flex-shrink: 0;
}

.company-title-section {
  flex: 1;
  min-width: 0;
}

.company-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.company-name-row h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #264653;
  line-height: 1.2;
  flex: 1;
  min-width: 0;
  word-break: break-word;
}

.company-status-tag {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
}

.company-location {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #6c757d;
  font-weight: 500;
}

.company-location .el-icon {
  font-size: 14px;
  color: #457B9D;
}

.company-info {
  margin-bottom: 20px;
}

.company-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.company-tags .el-tag {
  margin-right: 0;
  font-size: 12px;
  padding: 4px 10px;
  background-color: #f8f9fa;
  border: 1px solid #e9ecef;
  color: #495057;
  border-radius: 6px;
  font-weight: 500;
}

.company-desc {
  margin: 0 0 16px 0;
  color: #495057;
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.company-benefits h4 {
  font-size: 15px;
  color: #264653;
  margin: 0 0 10px 0;
  font-weight: 600;
}

.benefits-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}

.benefit-tag {
  font-size: 11px;
  padding: 3px 8px;
  background-color: #e3f2fd;
  border: 1px solid #bbdefb;
  color: #1976d2;
  border-radius: 4px;
}

.company-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.job-count-badge {
  background: linear-gradient(135deg, #457B9D, #264653);
  color: white;
  padding: 6px 14px;
  border-radius: 15px;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(69, 123, 157, 0.3);
}

.company-apply-hint {
  color: #6c757d;
  font-size: 13px;
  font-style: italic;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.company-card-beauty:hover .company-apply-hint {
  opacity: 1;
}

/* 分页样式（与岗位页一致） */
.companies-pagination {
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

:deep(.companies-pagination .el-pagination) {
  font-weight: 600;
  align-items: center;
  flex-wrap: wrap;
  justify-content: center;
  padding: 0;
  gap: 8px 0;
}

:deep(.companies-pagination .el-pager) {
  display: flex;
  flex-wrap: wrap;
  row-gap: 8px;
}

:deep(.companies-pagination .el-pager li) {
  background: rgba(168, 218, 220, 0.15);
  border: 1px solid rgba(168, 218, 220, 0.3);
  color: #457B9D;
  margin: 0 4px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.companies-pagination .el-pager li:hover) {
  background: #457B9D;
  color: #F1FAEE;
  transform: scale(1.1);
}

:deep(.companies-pagination .el-pager li.is-active) {
  background: #457B9D;
  color: #F1FAEE;
  box-shadow: 0 4px 12px rgba(69, 123, 157, 0.3);
}

/* 预加载完成的视觉指示 */
.company-card-beauty.preloaded::after {
  content: '⚡';
  position: absolute;
  top: 12px;
  right: 12px;
  background: linear-gradient(135deg, #2ecc71, #27ae60);
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  box-shadow: 0 2px 8px rgba(46, 204, 113, 0.3);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { opacity: 0.8; }
  50% { opacity: 1; }
  100% { opacity: 0.8; }
}

/* 动画效果 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

/* 公司卡片动画延迟 */
.company-card-beauty:nth-child(1) { animation-delay: 0.1s; }
.company-card-beauty:nth-child(2) { animation-delay: 0.2s; }
.company-card-beauty:nth-child(3) { animation-delay: 0.3s; }
.company-card-beauty:nth-child(4) { animation-delay: 0.4s; }
.company-card-beauty:nth-child(5) { animation-delay: 0.5s; }
.company-card-beauty:nth-child(6) { animation-delay: 0.6s; }
.company-card-beauty:nth-child(7) { animation-delay: 0.7s; }
.company-card-beauty:nth-child(8) { animation-delay: 0.8s; }

/* 响应式设计优化 */
@media (max-width: 1200px) {
  .companies-list {
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

  .companies-list-container {
    margin: 16px;
    padding: 24px;
  }
}

@media (max-width: 768px) {
  .main-title {
    font-size: 32px;
    letter-spacing: 1px;
  }

  .sub-title {
    font-size: 16px;
  }

  .unified-search-card {
    padding: 32px 20px 24px 20px;
  }

  .filter-row {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .action-buttons-unified {
    flex-direction: column;
    gap: 12px;
  }

  .search-btn-unified,
  .reset-btn-unified {
    width: 100%;
    max-width: 200px;
  }

  .companies-list {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .companies-list-container {
    margin: 12px;
    padding: 16px;
  }

  .companies-list-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 20px;
  }

  .companies-list-header h2 {
    font-size: 24px;
  }

  .company-card-beauty {
    padding: 20px 16px;
  }

  .company-name-row h3 {
    font-size: 18px;
  }

  .companies-pagination {
    margin: 20px 12px;
    padding: 16px;
  }
}
</style> 