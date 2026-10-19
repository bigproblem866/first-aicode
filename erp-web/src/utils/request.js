import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from './auth'

/**
 * Axios 实例。
 *
 * baseURL 固定为 '/api'：后端 Controller 上写的是全路径（含 /api），
 * Vite 代理不做任何 rewrite，所以这里拼出来正好是 /api/xxx。
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/** 401 只提示一次：多个请求同时过期时避免弹出十几个弹窗 */
let expiredMessageShown = false

function redirectToLogin() {
  // 动态 import 路由，避免 request -> router -> layout -> store -> api -> request 的循环依赖
  import('@/router').then(({ default: router }) => {
    const current = router.currentRoute.value
    if (current.path !== '/login') {
      router
        .push({ path: '/login', query: { redirect: current.fullPath } })
        .catch(() => {})
    }
  })
}

// ---------------------------------------------------------------------------
// 请求拦截：带上 Token
// ---------------------------------------------------------------------------
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------------------------------------------------------------------------
// 响应拦截：区分「HTTP 层错误」与「业务层失败」
// ---------------------------------------------------------------------------
service.interceptors.response.use(
  (response) => {
    // 文件下载等非 JSON 响应（Blob / ArrayBuffer）直接原样返回，不做包装拆解
    const responseType = response.config && response.config.responseType
    if (responseType === 'blob' || responseType === 'arraybuffer') {
      return response
    }

    const res = response.data

    // 后端约定：业务失败也是 HTTP 200，靠 body.code 判断
    if (res == null || typeof res !== 'object') {
      return res
    }
    if (res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
    // 成功：把外层 { code, msg, data } 拆掉，页面里直接拿业务数据，不用再 .data
    return res.data
  },
  (error) => {
    const status = error.response && error.response.status
    const body = error.response && error.response.data

    if (status === 401) {
      // 未登录 / Token 过期：清 Token 并回登录页
      removeToken()
      if (!expiredMessageShown) {
        expiredMessageShown = true
        ElMessage.error('登录已过期，请重新登录')
        setTimeout(() => {
          expiredMessageShown = false
        }, 1500)
      }
      redirectToLogin()
    } else if (status === 403) {
      // 已登录但无权限：只弹提示，绝不清理 Token、绝不跳登录页
      ElMessage.error((body && body.msg) || '没有权限访问该资源')
    } else {
      const msg =
        (body && body.msg) ||
        error.message ||
        (status ? `请求失败（HTTP ${status}）` : '网络异常，请检查后端服务是否已启动')
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default service
