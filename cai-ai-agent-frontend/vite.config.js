import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端服务地址（Spring Boot 默认 8123）
const BACKEND_TARGET = process.env.VITE_BACKEND_TARGET || 'http://localhost:8123'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    open: false,
    strictPort: false,
    // 某些编辑器的“原子保存”会瞬时生成临时目录，文件监听器偶发 EBUSY 导致进程退出。
    // 依赖 HMR 时可把下面这行去掉（默认就是开启监听）。
    watch: process.env.VITE_NO_WATCH === '1' ? null : undefined,
    // 关键：EventSource 只能 GET，且是长连接。
    // 开发环境统一通过 /api 前缀代理到后端，避免浏览器跨域(CORS)与长连接被中断的问题。
    proxy: {
      '/api': {
        target: BACKEND_TARGET,
        changeOrigin: true,
        // SSE 必须关闭代理缓冲，否则内容会被攒着一次性返回，没有打字机效果
        ws: false,
        configure(proxy) {
          proxy.on('proxyRes', (proxyRes) => {
            if (String(proxyRes.headers['content-type'] || '').includes('text/event-stream')) {
              proxyRes.headers['cache-control'] = 'no-cache, no-transform'
            }
          })
        },
      },
    },
  },
})
