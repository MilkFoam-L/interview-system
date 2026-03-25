import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path
      },
      '/ws': {
        target: 'ws://localhost:8081',
        ws: true,
        changeOrigin: true,
        rewrite: (path) => path
      }
    },
    hmr: {
      overlay: false // 禁用HMR错误覆盖，以便在控制台中查看错误
    },
    // 添加历史模式回退配置，支持SPA
    historyApiFallback: true
  },
  build: {
    // 启用源码映射，方便调试
    sourcemap: true,
    // 代码拆分策略
    rollupOptions: {
      output: {
        // 将大型文件拆分为更小的块
        manualChunks: {
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus']
        }
      }
    },
    // 提高构建性能
    chunkSizeWarningLimit: 1000, // 提高块大小警告限制
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: false, // 保留console以便调试
        drop_debugger: true
      }
    }
  },
  optimizeDeps: {
    // 预构建依赖项
    include: [
      'vue', 
      'vue-router', 
      'pinia', 
      'element-plus', 
      'axios',
      'element-plus/es/components/message/style/css',
      'element-plus/es/components/notification/style/css',
      'element-plus/es/components/message-box/style/css'
    ]
  }
})
