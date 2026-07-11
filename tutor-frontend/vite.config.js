import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173, // 前端启动端口
    proxy: {
      // 只要前端请求以 /api 开头，就自动代理到 Spring Boot 后端的 8080 端口上
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})