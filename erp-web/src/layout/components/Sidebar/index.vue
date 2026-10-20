<template>
  <div class="sidebar-container">
    <el-scrollbar>
      <!--
        ⚠ 这里**不能**加 router 属性（原来有，2026-10 去掉）。

        二级目录（库存管理）现在不是「跳转到功能导航页」，而是「在右侧展开子菜单」。
        它的 index 是合成值 module::/supply/inventory —— 不是一个真实路由，
        交给 el-menu 的 router 模式会直接导航失败。
        所以统一在 @select 里分流：
          - module:: 前缀 → 抛 module-select，由菜单浮层展开右侧卡片
          - 其余（真实路由）→ router.push
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
        @select="handleSelect"
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
import { computed, provide } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SidebarItem from './SidebarItem.vue'
import { usePermissionStore } from '@/store/modules/permission'

/**
 * 导航菜单。
 *
 * 现在渲染在顶栏图标弹出的浮层里（MenuLauncher.vue），不再常驻占位；
 * 所以不再需要折叠态，始终以展开形式显示。
 * 菜单数据完全来自后端（/api/auth/routers），前端不硬编码任何菜单项。
 *
 * 交互：
 *   - 一级目录（供应链）展开/收起，不导航
 *   - 二级目录（库存管理）抛 module-select，由 MenuLauncher 在右侧展开功能卡片
 *   - 三级功能、独立菜单项（用户管理）直接 router.push
 */
const props = defineProps({
  /** 右侧子菜单当前选中的二级目录完整路径，如 /supply/inventory */
  selectedModule: { type: String, default: '' }
})

const emit = defineEmits(['module-select', 'navigate'])

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

const sidebarRouters = computed(() => permissionStore.sidebarRouters)

/** 二级目录的合成 index 前缀：避开真实路由，el-menu 不会拿它去导航 */
const MODULE_INDEX_PREFIX = 'module::'

/**
 * 子组件（SidebarItem）递归多层，事件不会自动冒泡到 Sidebar，
 * 所以「工作台」标签的跳转通过 provide 下发，避免逐层转发。
 */
provide('erpMenuActions', {
  navigate(path) {
    router.push(path)
    emit('navigate')
  }
})

function handleSelect(index) {
  if (index.startsWith(MODULE_INDEX_PREFIX)) {
    emit('module-select', index.slice(MODULE_INDEX_PREFIX.length))
    return
  }
  router.push(index)
  emit('navigate')
}

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
 * 所以停在 /supply/inventory/stock 时，要反过来高亮库存管理这一项。
 */
function findModuleNavIndex(routes, base, depth) {
  for (const item of routes || []) {
    if (item.hidden) continue
    const full = joinPath(base, item.path)
    if (depth === 1 && item.children && item.children.length) {
      if (route.path === full || route.path.startsWith(`${full}/`)) {
        return `${MODULE_INDEX_PREFIX}${full}`
      }
    }
    const found = findModuleNavIndex(item.children, full, depth + 1)
    if (found) return found
  }
  return ''
}

const activeMenu = computed(() => {
  // 右侧卡片展开时，高亮用户刚点开的那个二级目录（可能还没跳转）
  if (props.selectedModule) return `${MODULE_INDEX_PREFIX}${props.selectedModule}`
  return findModuleNavIndex(permissionStore.sidebarRouters, '', 0) || route.path
})
</script>
