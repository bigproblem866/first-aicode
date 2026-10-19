import request from '@/utils/request'

/**
 * 认证接口 /api/auth
 * baseURL 已经是 '/api'，所以这里只写 '/auth/...'。
 */

/** 登录，入参 { username, password }，返回 { token } */
export function login(data) {
  return request({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/** 登出，作废服务端登录态 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

/** 获取当前用户信息，返回 { user, roles, permissions } */
export function getInfo() {
  return request({
    url: '/auth/getInfo',
    method: 'get'
  })
}

/** 获取动态路由树 */
export function getRouters() {
  return request({
    url: '/auth/routers',
    method: 'get'
  })
}
