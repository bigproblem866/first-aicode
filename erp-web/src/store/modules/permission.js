import { defineStore } from 'pinia'
import Layout from '@/layout/index.vue'
import ParentView from '@/layout/components/ParentView/index.vue'
import { getRouters } from '@/api/auth'
import { constantRoutes } from '@/router'

/**
 * 动态路由。
 *
 * 后端 GET /api/auth/routers 返回的是一棵 RouterVo 树，其中 component 是字符串：
 *   'Layout'                → 主框架
 *   'ParentView'            → 多级目录的中间层（只是一个 <router-view/> 容器）
 *   'system/user/index'     → src/views/system/user/index.vue
 *
 * import.meta.glob 会在构建期把 src/views 下所有 .vue 收集成 { 路径: 加载函数 }，
 * 于是「字符串 → 组件」就变成一次查表。查不到时降级到 404 页，
 * 避免动态路由白屏且没有任何线索。
 */
const viewModules = import.meta.glob('../../views/**/*.vue')

/** 把后端 component 字符串解析成组件 */
function resolveComponent(component) {
  if (component === 'Layout') return Layout
  if (component === 'ParentView') return ParentView
  if (!component) return undefined

  const key = `../../views/${component}.vue`
  const matched = viewModules[key]
  if (!matched) {
    // 后端菜单 component 写错 / 视图文件缺失时，最容易排查的方式是控制台报出来
    console.warn(`[permission] 未找到视图组件：${key}（请检查 sys_menu.component 与 views 目录是否一致）`)
    return viewModules['../../views/error/404.vue']
  }
  return matched
}

/** 递归转换路由树 */
function filterAsyncRouter(routes) {
  return (routes || []).map((route) => {
    const item = { ...route }
    delete item.children

    const comp = resolveComponent(route.component)
    if (comp) {
      item.component = comp
    } else {
      delete item.component
    }

    if (route.children && route.children.length > 0) {
      item.children = filterAsyncRouter(route.children)
    }

    // 后端可能下发 redirect: 'noRedirect'（表示目录不跳转），vue-router 不认这个值
    if (item.redirect === 'noRedirect') {
      delete item.redirect
    }

    return item
  })
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    /** 转换后、已 addRoute 的路由 */
    addRoutes: [],
    /** 全部路由（常量 + 动态），面包屑等地方用 */
    routes: [],
    /** 侧边栏渲染用的路由 */
    sidebarRouters: []
  }),

  actions: {
    /**
     * 拉取并转换动态路由。返回可直接 router.addRoute() 的数组。
     */
    async generateRoutes() {
      const data = await getRouters()
      const accessedRoutes = filterAsyncRouter(data)
      this.addRoutes = accessedRoutes
      // 侧边栏 = 常量路由（首页）+ 后端下发的业务路由。
      // 只放后端路由的话侧边栏会没有「首页」入口；
      // login/404/401 靠 hidden 标记被 SidebarItem 自动跳过。
      this.sidebarRouters = [...constantRoutes, ...accessedRoutes]
      this.routes = [...constantRoutes, ...accessedRoutes]
      return accessedRoutes
    },

    /** 登出时重置 */
    resetRoutes() {
      this.addRoutes = []
      this.routes = []
      this.sidebarRouters = []
    }
  }
})

export default usePermissionStore
