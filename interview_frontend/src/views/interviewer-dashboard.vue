<template>
  <div class="under-development">
    <el-result
      icon="info"
      title="功能开发中"
      sub-title="面试官功能正在开发中，敬请期待..."
    >
      <template #extra>
        <el-button type="primary" @click="handleLogout">退出登录</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import auth from '@/utils/auth'

const router = useRouter()

const handleLogout = () => {
  // 使用统一的auth工具退出登录
  auth.logout('已退出登录')
}
</script>

<style scoped>
.under-development {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f5f5;
}
</style> 