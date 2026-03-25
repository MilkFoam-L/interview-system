<template>
  <div class="company-detail-container">
    <!-- 页面过渡容器 -->
    <transition name="page-fade" mode="out-in" appear>
      <el-card v-if="company" key="content" class="company-detail-card content-container">
        <template #header>
          <div class="card-header">
            <div class="header-main">
              <el-avatar :size="80" :src="company.logoUrl || defaultLogo" />
              <div class="header-info">
                <h1>{{ company.name }}</h1>
                <p class="slogan">{{ company.slogan }}</p>
                <div class="header-tags">
                  <el-tag effect="plain">{{ company.industry }}</el-tag>
                  <el-tag effect="plain" type="info">{{ company.size }}</el-tag>
                  <el-tag effect="plain" type="success">{{ company.financing }}</el-tag>
                </div>
              </div>
            </div>
            <div class="header-actions">
              <el-button type="primary" @click="goBack" :icon="ArrowLeft">返回</el-button>
            </div>
          </div>
        </template>

        <div class="detail-content">
          <el-row :gutter="32">
            <el-col :span="16">
              <el-card class="info-section-card" shadow="never">
                <template #header>
                  <div class="section-header">
                    <el-icon><OfficeBuilding /></el-icon>
                    <span>公司简介</span>
                  </div>
                </template>
                <p class="description">{{ company.description }}</p>
              </el-card>

              <el-card class="info-section-card" shadow="never">
                <template #header>
                  <div class="section-header">
                    <el-icon><MagicStick /></el-icon>
                    <span>公司文化与产品</span>
                  </div>
                </template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="公司文化">{{ company.culture }}</el-descriptions-item>
                  <el-descriptions-item label="产品介绍">{{ company.productDescription }}</el-descriptions-item>
                </el-descriptions>
              </el-card>

              <el-card class="info-section-card" shadow="never">
                <template #header>
                  <div class="section-header">
                    <el-icon><Present /></el-icon>
                    <span>福利待遇</span>
                  </div>
                </template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item
                      v-for="(value, key) in company.benefits"
                      :key="key"
                      :label="key">
                    {{ value }}
                  </el-descriptions-item>
                  <el-descriptions-item label="工作时间">{{ company.workTime }}</el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>

            <el-col :span="8">
              <el-card class="info-section-card" shadow="never">
                <template #header>
                  <div class="section-header">
                    <el-icon><Tickets /></el-icon>
                    <span>基本信息</span>
                  </div>
                </template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="法定全称">{{ company.legalName }}</el-descriptions-item>
                  <el-descriptions-item label="成立日期">{{ company.establishedDate }}</el-descriptions-item>
                  <el-descriptions-item label="公司性质">{{ company.type }}</el-descriptions-item>
                  <el-descriptions-item label="融资阶段">{{ company.financing }}</el-descriptions-item>
                  <el-descriptions-item label="上市状态">
                    <el-tag :type="company.listedStatus ? 'success' : 'info'" size="small">
                      {{ company.listedStatus ? '已上市' : '未上市' }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="company.listedStatus" label="股票代码">{{ company.stockCode }}</el-descriptions-item>
                </el-descriptions>
              </el-card>

              <el-card class="info-section-card" shadow="never">
                <template #header>
                  <div class="section-header">
                    <el-icon><Link /></el-icon>
                    <span>联系方式</span>
                  </div>
                </template>
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="公司地址">{{ company.location }}</el-descriptions-item>
                  <el-descriptions-item label="联系邮箱">{{ company.email }}</el-descriptions-item>
                  <el-descriptions-item label="联系电话">{{ company.phone }}</el-descriptions-item>
                  <el-descriptions-item label="公司官网">
                    <a :href="company.websiteUrl" target="_blank" class="company-website">{{ company.websiteUrl }}</a>
                  </el-descriptions-item>
                </el-descriptions>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-card>

      <!-- 错误状态 -->
      <el-empty v-else key="error" description="未找到公司信息" />
    </transition>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { ElMessage } from 'element-plus';
import { OfficeBuilding, MagicStick, Present, Tickets, Link, ArrowLeft } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const company = ref(null);
const loading = ref(false);
const defaultLogo = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';
const companyId = ref(route.params.id);

// 检查是否有预加载的数据
const preloadedCompanyData = ref(history.state?.preloadedCompanyData || null);

const goBack = () => {
  router.go(-1);
};

// 重置页面状态
const resetPageState = () => {
  company.value = null;
};

const fetchCompanyDetail = async () => {
  try {
    console.log('正在获取企业详情，ID:', companyId.value);

    let response;

    // 优先使用预加载的数据
    if (preloadedCompanyData.value) {
      console.log('使用预加载数据');
      response = preloadedCompanyData.value;
      preloadedCompanyData.value = null; // 清除预加载数据，避免重复使用

      // 使用预加载数据时，只需要很短的延迟用于动画过渡
      await new Promise(resolve => setTimeout(resolve, 50));
    } else {
      // 没有预加载数据，正常请求
      response = await request({
        url: `/api/companies/${companyId.value}`,
        method: 'get',
      });
    }

    console.log('获取到的企业详情:', response);

    if (response) {
      company.value = response;
    } else {
      console.error('公司信息加载失败');
      company.value = null;
    }
  } catch (error) {
    // 过滤掉用户主动取消的请求错误，避免不必要的错误提示
    if (error.name !== 'AbortError') {
      console.error('获取公司详情失败:', error);
      console.error('获取公司详情失败，请稍后重试');
    }
    company.value = null;
  }
};

onMounted(() => {
  fetchCompanyDetail();
});

// 监听路由变化，实现平滑切换
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    console.log('路由切换:', oldId, '->', newId);

    // 检查新的预加载数据
    preloadedCompanyData.value = history.state?.preloadedCompanyData || null;

    // 立即重置页面状态
    resetPageState();
    companyId.value = newId;

    // 使用nextTick确保DOM更新后再获取数据
    nextTick(() => {
      fetchCompanyDetail();
    });
  }
}, {
  immediate: false // 不立即执行，因为onMounted已经处理了初始加载
});
</script>

<style scoped>
.company-detail-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}
.company-detail-card {
  border-radius: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-main {
  display: flex;
  align-items: center;
  gap: 24px;
}
.header-info h1 {
  font-size: 28px;
  margin: 0;
  font-weight: 600;
}
.header-info .slogan {
  font-size: 16px;
  color: #606266;
  margin: 8px 0;
}
.header-tags {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}
.info-section-card {
  margin-bottom: 24px;
}
.description {
  line-height: 1.8;
  color: #606266;
}
.company-website {
  color: #409eff;
  text-decoration: none;
}
.company-website:hover {
  text-decoration: underline;
}
.welfare-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.welfare-tag {
  font-size: 14px;
}
.header-actions {
  display: flex;
  align-items: center;
}

.header-actions .el-button {
  background: linear-gradient(135deg, #457B9D 0%, #264653 100%) !important;
  border: none !important;
  color: #F1FAEE !important;
  border-radius: 16px !important;
  padding: 10px 20px !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1) !important;
  box-shadow: 0 4px 16px rgba(38, 70, 83, 0.35) !important;
  letter-spacing: 0.5px;
}

.header-actions .el-button:hover {
  transform: translateY(-2px) scale(1.02) !important;
  box-shadow: 0 8px 24px rgba(38, 70, 83, 0.45) !important;
  background: linear-gradient(135deg, #264653 0%, #1D3E47 100%) !important;
}

.header-actions .el-button:active {
  transform: translateY(0) scale(1) !important;
  box-shadow: 0 2px 8px rgba(38, 70, 83, 0.3) !important;
}

/* 页面过渡动画 - 更加流畅 */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(15px) scale(0.98);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(1.02);
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

/* 预加载内容的快速显示动画 */
.content-container.preloaded {
  animation: content-fade-in-fast 0.2s ease-out;
}

@keyframes content-fade-in-fast {
  from {
    opacity: 0;
    transform: translateY(3px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式优化 */
@media (max-width: 768px) {
  .company-detail-container {
    padding: 16px;
  }

  .page-fade-enter-from,
  .page-fade-leave-to {
    transform: translateX(100%);
  }

  .header-main {
    flex-direction: column;
    text-align: center;
    gap: 16px;
  }

  .card-header {
    flex-direction: column;
    gap: 16px;
  }
}
</style> 