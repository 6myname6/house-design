import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发时把 /api 和 /files 代理到后端 8080，解决跨域
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/files': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})