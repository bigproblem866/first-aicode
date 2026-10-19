import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import crypto from 'node:crypto'
import { fileURLToPath, URL } from 'node:url'

/**
 * Node < 20.12 兼容补丁（本机是 Node 20.11.1）。
 *
 * @vitejs/plugin-vue 6.x 内部用 crypto.hash(算法, 数据, 编码) 计算 SFC 的缓存 key，
 * 而 crypto.hash 是 Node 20.12 才有的 API。在 20.11 上会直接抛
 * "crypto.hash is not a function" 导致 build 挂掉。
 *
 * plugin-vue 是以默认导入方式拿到 crypto 对象、并在编译时才调用 hash 的，
 * 所以在这里给同一个模块对象补上该方法即可（模块对象在 Node 里是同一个引用）。
 * 等本机 Node 升到 20.12+ 后，这段代码会自动跳过，可以删掉。
 */
if (typeof crypto.hash !== 'function') {
  crypto.hash = (algorithm, data, outputEncoding) =>
    crypto.createHash(algorithm).update(data).digest(outputEncoding)
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // '@' 指向 src，页面里统一用 '@/xxx' 引入
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
    extensions: ['.js', '.json', '.vue']
  },
  server: {
    port: 3000,
    host: true,
    open: false,
    proxy: {
      // 后端接口路径本身就带 /api（Controller 上写全路径），
      // 所以这里只做转发，绝对不要写 rewrite，否则会 404。
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 2000
  }
})
