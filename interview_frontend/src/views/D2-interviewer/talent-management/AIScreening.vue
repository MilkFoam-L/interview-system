<template>
  <div class="page-container">
    <div class="page-header">
      <h1>面试候选人管理</h1>
      <p>查看AI面试者报告，发布面试通知</p>
    </div>
    
    <div class="page-content">
      <el-card class="filter-card">
        <div class="filter-container">
          <el-input 
            v-model="searchQuery" 
            placeholder="搜索候选人姓名/技能/职位" 
            prefix-icon="Search" 
            class="search-input"
          />
          <el-select v-model="positionFilter" placeholder="筛选职位" clearable class="filter-select">
            <el-option label="前端开发工程师" value="前端开发工程师" />
            <el-option label="后端开发工程师" value="后端开发工程师" />
            <el-option label="UI设计师" value="UI设计师" />
            <el-option label="产品经理" value="产品经理" />
          </el-select>
          <el-select v-model="statusFilter" placeholder="筛选状态" clearable class="filter-select">
            <el-option label="待处理" value="待处理" />
            <el-option label="已通过" value="已通过" />
            <el-option label="已拒绝" value="已拒绝" />
          </el-select>
          <div class="filter-buttons">
            <el-button type="primary" @click="onSearch">搜索</el-button>
            <el-button @click="onReset">重置</el-button>
          </div>
        </div>
      </el-card>

      <el-card>
        <div class="table-toolbar">
          <div class="left">
            <el-button type="primary" :loading="loading" @click="batchNotify">批量发布通过通知</el-button>
            <el-button type="danger" :loading="loading" @click="batchReject">批量拒绝</el-button>
          </div>
          <div class="right">
            <span>共 {{ total }} 条</span>
          </div>
        </div>

        <div class="table-wrapper">
          <el-table
            :data="candidateList"
            :loading="loading"
            table-layout="auto"
            @selection-change="handleSelectionChange"
            style="width: 100%"
          >
            <el-table-column type="selection" width="48" />
            <el-table-column prop="name" label="姓名" min-width="120" show-overflow-tooltip />
            <el-table-column prop="position" label="应聘职位" min-width="160" show-overflow-tooltip class-name="position-column" />
            <el-table-column prop="score" label="AI评分" width="100">
              <template #default="scope">
                <el-tag :type="getScoreTagType(scope.row.score)">{{ scope.row.score }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="面试时间" min-width="180" class-name="time-column">
              <template #default="scope">
                {{ formatDateTimeDisplay(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusTagType(scope.row.status)">{{ scope.row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" fixed="right" min-width="200">
              <template #default="scope">
                <div class="action-buttons">
                  <el-button link type="primary" @click="showReport(scope.row)">查看报告</el-button>
                  <el-button link type="primary" @click="notifyCandidate(scope.row)">发布通知</el-button>
                  <el-button link type="danger" @click="rejectCandidate(scope.row)">拒绝</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div class="pagination-container">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="total"
            :page-size="pageSize"
            prev-text="上一页"
            next-text="下一页"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>

      <el-drawer v-model="reportDrawerVisible" title="AI面试报告" :size="drawerSize">
        <div v-if="currentRow">
          <div style="margin-bottom: 8px; font-weight: 600;">{{ currentRow.name }} - {{ currentRow.position }}</div>
          <el-descriptions :column="responsiveColumn" size="small" border>
            <el-descriptions-item label="AI评分">
              <el-tag :type="getScoreTagType(currentRow.score)">{{ currentRow.score }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="面试时间">{{ currentRow.createTime }}</el-descriptions-item>
            <el-descriptions-item label="技能匹配">
              <el-tag v-for="s in currentRow.skills" :key="s" class="skill-tag">{{ s }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusTagType(currentRow.status)">{{ currentRow.status }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          <el-divider />
          <el-empty description="在此渲染 AI 报告详情（概览 / 问答 / 代码 / 行为）" />
        </div>
      </el-drawer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { Search } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { formatDateTimeDisplay } from '@/utils/dateUtils';
import { fetchCandidates, notifyPass as apiNotifyPass, rejectCandidate as apiReject, batchNotify as apiBatchNotify, batchReject as apiBatchReject, getReport } from '@/api/aiScreening';

const searchQuery = ref('');
const positionFilter = ref('');
const statusFilter = ref('');

// 分页
const currentPage = ref(1);
const pageSize = ref(10);
const total = ref(0);

const loading = ref(false);
const selection = ref<any[]>([]);
const reportDrawerVisible = ref(false);
const currentRow = ref<any | null>(null);

const screenWidth = ref(window.innerWidth);

const drawerSize = computed(() => {
  if (screenWidth.value < 768) return '90%';
  if (screenWidth.value < 1024) return '70%';
  return '60%';
});

const responsiveColumn = computed(() => {
  return screenWidth.value < 768 ? 1 : 2;
});

const candidateList = ref<any[]>([]);

const loadData = async () => {
  loading.value = true;
  try {
    const params: any = {
      page: currentPage.value,
      size: pageSize.value
    };
    if (searchQuery.value) params.keyword = searchQuery.value;
    if (positionFilter.value) params.position = positionFilter.value;
    if (statusFilter.value) params.status = statusFilter.value === '已通过' ? 1 : (statusFilter.value === '已拒绝' ? 2 : 0);
    const res: any = await fetchCandidates(params);
    const list = Array.isArray(res && res.list) ? res.list : [];
    candidateList.value = list.map((i: any) => ({
      id: i.applicationId,
      name: i.name,
      position: i.position,
      score: i.score,
      skills: i.skills || [],
      createTime: i.createTime,
      status: i.status
    }));
    total.value = Number((res && res.total) || 0);
  } catch (e) {
    console.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

const handleResize = () => {
  screenWidth.value = window.innerWidth;
};

onMounted(() => {
  window.addEventListener('resize', handleResize);
  loadData();
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});

const onSearch = () => {
  currentPage.value = 1;
  loadData();
};

const onReset = () => {
  searchQuery.value = '';
  positionFilter.value = '';
  statusFilter.value = '';
  currentPage.value = 1;
  pageSize.value = 10;
  loadData();
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  loadData();
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
  loadData();
};

const handleSelectionChange = (rows: any[]) => {
  selection.value = rows;
};

const getScoreTagType = (score: number): string => {
  if (score >= 90) return 'success';
  if (score >= 75) return '';
  if (score >= 60) return 'warning';
  return 'danger';
};

const getStatusTagType = (status: string): string => {
  if (status === '已通过') return 'success';
  if (status === '待处理') return 'info';
  if (status === '已拒绝') return 'danger';
  return '';
};

// 查看报告
const showReport = (row: any) => {
  currentRow.value = row;
  reportDrawerVisible.value = true;
};

const notifyCandidate = async (candidate: any) => {
  const confirmed = await ElMessageBox.confirm(
    `确定向候选人 ${candidate.name} 发布通过通知吗？`,
    '发布通知',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(() => true).catch(() => false);

  if (!confirmed) return;

  loading.value = true;
  try {
    await apiNotifyPass(candidate.id);
    console.log('已成功发布通过通知');
    loadData();
  } finally {
    loading.value = false;
  }
};

const rejectCandidate = async (candidate: any) => {
  const { value: reason } = await ElMessageBox.prompt(
    `请输入拒绝理由（必填，便于复盘）`,
    '拒绝候选人',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '如 技术深度不足/岗位不匹配',
      inputValidator: (v: string) => !!v || '拒绝理由不能为空'
    }
  ).catch(() => ({ value: null } as any));

  if (!reason) return;

  loading.value = true;
  try {
    await apiReject(candidate.id, reason);
    console.log('已拒绝该候选人');
    loadData();
  } finally {
    loading.value = false;
  }
};

const batchNotify = async () => {
  if (!selection.value.length) return console.error('请先选择候选人');
  const confirmed = await ElMessageBox.confirm('确定批量发布通过通知吗？', '批量通知', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'info'
  }).then(() => true).catch(() => false);
  if (!confirmed) return;

  loading.value = true;
  try {
    await apiBatchNotify(selection.value.map((i: any) => i.id));
    console.log('批量发布通知完成');
    loadData();
  } finally {
    loading.value = false;
  }
};

const batchReject = async () => {
  if (!selection.value.length) return console.error('请先选择候选人');
  const { value: reason } = await ElMessageBox.prompt(
    `请输入拒绝理由（必填，便于复盘）`,
    '批量拒绝',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '如 技术深度不足/岗位不匹配',
      inputValidator: (v: string) => !!v || '拒绝理由不能为空'
    }
  ).catch(() => ({ value: null } as any));

  if (!reason) return;

  loading.value = true;
  try {
    await apiBatchReject(selection.value.map((i: any) => i.id), reason);
    console.log('批量拒绝完成');
    loadData();
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 24px;
  margin-bottom: 8px;
}

.page-header p {
  color: #606266;
  font-size: 14px;
}

.page-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 12px;
}

.filter-card {
  margin-bottom: 0;
}

.filter-container {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  width: 300px;
  min-width: 200px;
}

.filter-select {
  width: 180px;
  min-width: 150px;
}

.filter-buttons {
  display: flex;
  gap: 8px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 10px;
}

.table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-start;
}

.skill-tag {
  margin-right: 4px;
}

:deep(.el-table .el-table__row) {
  height: 54px;
}

:deep(.el-table .el-table__cell) {
  padding: 8px 12px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .page-container {
    padding: 12px;
  }
  
  .filter-container {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-input,
  .filter-select {
    width: 100%;
  }
  
  .filter-buttons {
    justify-content: center;
  }
  
  .table-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .table-toolbar .left {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
  }
  
  .table-toolbar .right {
    text-align: center;
  }
  
  .pagination-container {
    justify-content: center;
  }

  :deep(.position-column),
  :deep(.time-column) {
    display: none;
  }
}

@media (max-width: 1024px) {
  .page-container {
    padding: 16px;
  }
  
  .search-input {
    width: 250px;
  }
  
  .filter-select {
    width: 160px;
  }

  :deep(.time-column) {
    display: none;
  }
}

@media (max-width: 480px) {
  .table-toolbar .left {
    flex-direction: column;
  }
  
  .table-toolbar .left .el-button {
    width: 100%;
  }
  
  .action-buttons {
    flex-direction: column;
    gap: 4px;
  }
}
</style> 