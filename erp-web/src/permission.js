import { ElMessage } from 'element-plus'
import router, { anyRoute } from '@/router'
import { getToken } from '@/utils/auth'
import useUserStore from '@/store/modules/user'
import { usePermissionStore } from '@/store/modules/permission'

/** 免登录白名单 */
const whiteList = ['/login', '/404', '/401']

/**
 * 全局前置守卫。
 *
 * 有 Token 但没有角色信息 → 说明是刚登录或刚刷新，先拉用户信息、再拉动态路由并注入，
 * 然后用 next({ ...to, replace: true }) 重新走一遍导航。
 *
 * 为什么要 next({ ...to, replace: true }) 而不是 next()：
 * addRoute 是在本次导航过程中发生的，当前这次导航匹配的还是旧路由表（命中兜底 404）。
 * 用 replace 重新导航一次，vue-router 才会用新路由表重新匹配，
 * 同时 replace 避免在历史记录里留下一条中间态。
 */
router.beforeEach(async (to, from, next) => {
  const hasToken = getToken()

  if (!hasToken) {
    if (whiteList.includes(to.path)) {
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
    }
    return
  }

  // 已登录还去登录页 → 直接回首页
  if (to.path === '/login') {
    next({ path: '/' })
    return
  }

  const userStore = useUserStore()

  // 用 infoLoaded 而不是 roles.length：没有分配角色的用户 roles 为空，
  // 但那也代表「已经加载完成」，否则会陷入无限重定向（点登录像没反应）
  if (userStore.infoLoaded) {
    next()
    return
  }

  try {
    await userStore.getInfo()
    // 没分配角色的用户仍允许登录（否则会卡在登录页且没有任何提示），
    // 但明确告诉用户为什么看不到菜单。
    // duration: 0 → 不自动消失；showClose: true → 用户自己点 × 关闭。
    if (!userStore.roles.length) {
      ElMessage.warning({
        message: '当前账号未分配角色，只能访问首页，请联系管理员',
        duration: 0,
        showClose: true
      })
    }
    const accessRoutes = await usePermissionStore().generateRoutes()
    accessRoutes.forEach((route) => router.addRoute(route))
    router.addRoute(anyRoute)
    next({ ...to, replace: true })
  } catch (error) {
    // Token 失效 / 拉取用户信息失败：清掉本地登录态回到登录页
    await userStore.logout()
    next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }
})

router.afterEach(() => {
  // 预留：这里可以关掉全局 loading（本工程未引入 NProgress）
})
