/**
 * Token 存取。
 *
 * 只用 localStorage，不引 js-cookie：
 *   - 本系统没有 SSR，也无需让后端读取 cookie，cookie 只会带来 CSRF 与跨域配置的麻烦；
 *   - Token 走 Authorization 请求头，天然不受同源策略限制。
 *
 * 注意：localStorage 的 key 与后端约定无关，改动它只会让已登录用户被踢下线一次。
 */
const TOKEN_KEY = 'erp_token'

/** 读取 Token，未登录返回 null */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

/** 写入 Token */
export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

/** 删除 Token（登出 / 401 时调用） */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}
