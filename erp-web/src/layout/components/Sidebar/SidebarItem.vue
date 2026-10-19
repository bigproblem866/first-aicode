<template>
  <template v-if="!item.hidden">
    <!--
      二级目录（如 供应链 → 库存管理）：**不再展开三级菜单**，
      而是渲染成一个可点的菜单项，点击进入它的功能导航页（右下内容区展示）。
      这对应参考图：侧栏只到「库存管理」这一级，下级功能在右侧页面里列出。
    -->
    <el-menu-item v-if="isModuleLeaf" :index="moduleNavPath">
      <el-icon v-if="selfIcon"><component :is="selfIcon" /></el-icon>
      <template #title>
        <span class="menu-text">{{ selfTitle }}</span>
        <span class="menu-badge" :class="{ 'is-current': workbenchActive }">工作台</span>
      </template>
    </el-menu-item>

    <!-- 叶子功能（如 用户管理 / 物料管理）：直接跳转到对应页面 -->
    <el-menu-item v-else-if="renderAsLeaf" :index="leafPath">
      <el-icon v-if="leafIcon"><component :is="leafIcon" /></el-icon>
      <template #title>{{ leafTitle }}</template>
    </el-menu-item>

    <!-- 一级目录（如 供应链）：可展开，子级递归 -->
    <el-sub-menu v-else :index="menuIndex">
      <template #title>
        <el-icon v-if="selfIcon"><component :is="selfIcon" /></el-icon>
        <span>{{ selfTitle }}</span>
      </template>
      <SidebarItem
        v-for="child in showingChildren"
        :key="child.path"
        :item="child"
        :base-path="resolvePath(child.path)"
      />
    </el-sub-menu>
  </template>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

/**
 * 侧边栏菜单项（递归组件）。
 *
 * 层级规则（与参考图一致）：
 *   /supply                一级目录 → 可展开
 *   /supply/inventory      二级目录 → 渲染成菜单项，点击进入功能导航页
 *   /supply/inventory/stock 三级功能 → **不在侧栏出现**，由功能导航页列出
 *   /system/user           直接挂在模块下的功能 → 普通菜单项
 *
 * 组件通过文件名自引用，因此不需要 import 自己。
 * basePath 是「当前项自身的完整路径」，父子拼路径全靠它。
 */
const props = defineProps({
  item: { type: Object, required: true },
  basePath: { type: String, default: '' }
})

const route = useRoute()

/** 拼绝对路径：子菜单的 path 是相对的，必须拼到父级上才能被 vue-router 命中 */
function resolvePath(routePath) {
  if (!routePath) return props.basePath
  if (/^https?:\/\//.test(routePath)) return routePath
  if (routePath.startsWith('/')) return routePath
  if (!props.basePath) return `/${routePath}`
  return `${props.basePath}/${routePath}`.replace(/\/{2,}/g, '/')
}

/** 图标名来自后端菜单的 icon 字段；校验一下是不是真的存在，避免全局组件解析失败告警 */
function iconOf(route) {
  const name = route && route.meta && route.meta.icon
  return name && ElementPlusIconsVue[name] ? name : ''
}

const showingChildren = computed(() =>
  (props.item.children || []).filter((child) => !child.hidden)
)

const isLeaf = computed(() => showingChildren.value.length === 0)

/** 目录层级：/supply → 1，/supply/inventory → 2 */
const depth = computed(() => (props.basePath || '').split('/').filter(Boolean).length)

/**
 * 二级目录：有子菜单，但不在侧栏展开。
 * 点击进入 `/module/<目录路径>`，在内容区展示它的功能导航。
 */
const isModuleLeaf = computed(() => depth.value === 2 && !isLeaf.value)

/** 功能导航页地址，如 /module/supply/inventory */
const moduleNavPath = computed(() => `/module${props.basePath}`)

/** 当前是否停在该目录内（导航页本身，或它下面的某个功能页） */
const workbenchActive = computed(
  () =>
    route.path === moduleNavPath.value ||
    route.path.startsWith(`${props.basePath}/`)
)

const singleChild = computed(() =>
  showingChildren.value.length === 1 ? showingChildren.value[0] : null
)

/** 是否直接渲染成菜单项：本身没有子菜单，或只有一个子菜单且父级未要求强制展开 */
const renderAsLeaf = computed(() => {
  if (isLeaf.value) return true
  return !!singleChild.value && !props.item.alwaysShow
})

/** 叶子项指向的路径：单子菜单时用子菜单的路径，自身为叶子时就是 basePath */
const leafPath = computed(() => {
  if (isLeaf.value) return props.basePath
  return resolvePath(singleChild.value.path)
})

const leafRoute = computed(() => (isLeaf.value ? props.item : singleChild.value))

const leafTitle = computed(
  () => (leafRoute.value && leafRoute.value.meta && leafRoute.value.meta.title) || ''
)
const leafIcon = computed(() => iconOf(leafRoute.value))

const selfTitle = computed(() => (props.item.meta && props.item.meta.title) || '')
const selfIcon = computed(() => iconOf(props.item))

const menuIndex = computed(() => props.basePath)
</script>
