import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getInfo as getInfoApi } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'

/**
 * 当前登录用户。
 *
 * token 存 localStorage（刷新页面不丢），userInfo/roles/permissions 只放内存：
 * 它们每次刷新都由 permission.js 守卫重新调 getInfo 拉取，不必持久化，
 * 也避免了菜单权限改了、前端却还拿着旧缓存的情况。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: {},
    roles: [],
    permissions: [],
    /**
     * 是否已经成功拉过用户信息。
     *
     * 注意不能用 roles.length > 0 判断：新建用户如果没有分配角色，
     * roles 就是空数组，守卫会误以为「还没加载」，于是 getInfo → generateRoutes
     * 无限循环，最终路由导航被浏览器中断 —— 表现就是「点登录没反应」。
     */
    infoLoaded: false
  }),

  getters: {
    /** 是否已经拉过用户信息（守卫用它判断要不要走 getInfo + generateRoutes） */
    isInfoLoaded: (state) => state.infoLoaded,
    nickName: (state) => state.userInfo.nickName || state.userInfo.username || '未登录',
    avatar: (state) => state.userInfo.avatar || ''
  },

  actions: {
    /**
     * 登录。入参 { username, password }，成功返回的 data 形如 { token }。
     */
    async login(loginForm) {
      const data = await loginApi(loginForm)
      const token = data && data.token
      if (!token) {
        throw new Error('登录接口未返回 token')
      }
      this.token = token
      setToken(token)
      return data
    },

    /**
     * 拉取当前用户信息，data 形如 { user, roles, permissions }。
     */
    async getInfo() {
      const data = await getInfoApi()
      const user = (data && data.user) || {}
      this.userInfo = user
      this.roles = (data && data.roles) || []
      this.permissions = (data && data.permissions) || []
      // 即使 roles 为空也算「已加载」，否则守卫会无限循环
      this.infoLoaded = true
      return data
    },

    /**
     * 登出：先通知服务端作废登录态，无论成功失败都清理本地状态。
     */
    async logout() {
      try {
        if (this.token) {
          await logoutApi()
        }
      } catch (e) {
        // 服务端已失效 / 网络不通都不影响本地登出，忽略即可
      } finally {
        this.resetState()
      }
    },

    /** 清空本地登录态（登出、401、切换账号时调用） */
    resetState() {
      this.token = ''
      this.userInfo = {}
      this.roles = []
      this.permissions = []
      this.infoLoaded = false
      removeToken()
    }
  }
})

export default useUserStore
