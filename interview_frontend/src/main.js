import './assets/base.css'
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import * as auth from './utils/auth'

// 创建应用实例
const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 创建pinia
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 在应用挂载前加载用户信息
import { useUserStore } from './store/user'

app.mount('#app')
