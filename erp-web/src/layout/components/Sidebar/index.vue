<template>
  <div class="sidebar-container">
    <el-scrollbar>
      <!--
        ⚠ router 属性不能省。

        el-menu 默认【不跳转】：点击菜单项只是把它高亮，不会做任何路由导航 ——
        现象就是"点菜单没反应，页面内容不换"。要让它跳转有两条路：
          1) 加 router 属性，Element Plus 会用 el-menu-item 的 index 去 router.push
          2) 自己监听 @select 事件手动 router.push

        这里用第 1 条。能直接用是因为 el-menu-item 的 index 传的就是完整路由路径
        （见 SidebarItem.vue 的 leafPath，拼出来是 /system/user 这种），
        正好就是 router.push 需要的东西。
      -->
      <!--
        这三个颜色 props 不要加回来：
        background-color / text-color / active-text-color 会被 Element 写成
        --el-menu-bg-color 等 CSS 变量注入菜单，直接压过 sidebar.scss 里的浅色方案，
        现象是"样式改了但侧栏还是深色/文字还是浅灰"。
        配色统一在 assets/styles/sidebar.scss 维护，只留一个来源。
      -->
      <el-menu
        :default-active="activeMenu"
        :unique-opened="false"
        mode="vertical"
        router
      >
        <SidebarItem
          v-for="route in sidebarRouters"
          :key="route.path"
          :item="route"
          :base-path="route.path"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SidebarItem from './SidebarItem.vue'
import { usePermissionStore } from '@/store/modules/permission'

/**
 * 导航菜单。
 *
 * 现在渲染在顶栏图标弹出的浮层里（MenuLauncher.vue），不再常驻占位；
 * 所以不再需要折叠态，始终以展开形式显示。
 * 菜单数据完全来自后端（/api/auth/routers），前端不硬编码任何菜单项。
 */
const route = useRoute()
const permissionStore = usePermissionStore()

const sidebarRouters = computed(() => permissionStore.sidebarRouters)

function joinPath(base, path) {
  if (!path) return base || '/'
  if (/^https?:\/\//.test(path)) return path
  if (path.startsWith('/')) return path
  if (!base) return `/${path}`
  return `${base}/${path}`.replace(/\/{2,}/g, '/')
}

/**
 * 当前路由属于哪个二级目录，就高亮哪个二级目录的菜单项。
 *
 * 侧栏只渲染到二级目录（库存管理 / 采购管理），三级功能不在侧栏出现；
 * 所以停在 /supply/inventory/stock 时，要反过来高亮 /module/supply/inventory。
 */
function findModuleNavPath(routes, base, depth) {
  for (const item of routes || []) {
    if (item.hidden) continue
    const full = joinPath(base, item.path)
    if (depth === 1 && item.children && item.children.length) {
      if (route.path === full || route.path.startsWith(`${full}/`)) {
        return `/module${full}`
      }
    }
    const found = findModuleNavPath(item.children, full, depth + 1)
    if (found) return found
  }
  return ''
}

const activeMenu = computed(
  () => findModuleNavPath(permissionStore.sidebarRouters, '', 0) || route.path
)
</script>
