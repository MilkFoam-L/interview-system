<template>
  <div class="default-avatar" :style="{ width: size + 'px', height: size + 'px' }">
    {{ initials }}
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  username: {
    type: String,
    default: ''
  },
  size: {
    type: Number,
    default: 36
  },
  color: {
    type: String,
    default: '#6366f1'
  }
});

// 获取用户名首字母作为头像显示
const initials = computed(() => {
  if (!props.username) return '?';
  
  // 如果是中文，取第一个字符
  if (/[\u4e00-\u9fa5]/.test(props.username)) {
    return props.username.charAt(0);
  }
  
  // 如果是英文，取首字母大写
  return props.username.charAt(0).toUpperCase();
});
</script>

<style scoped>
.default-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: v-bind(color);
  color: white;
  border-radius: 50%;
  font-weight: bold;
  font-size: calc(v-bind(size) / 2.5);
  user-select: none;
}
</style> 