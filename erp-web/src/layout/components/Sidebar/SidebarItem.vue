<template>
  <template v-if="!item.hidden">
    <!--
      二级目录（如 供应链 → 库存管理）：**不再展开三级菜单**，点击也不跳转。
      index 是合成值 module::/supply/inventory，由 Sidebar 的 @select 识别，
      转成 module-select 事件，让菜单浮层在右侧展开一张子菜单卡片。
      「工作台」标签单独进入整页功能导航（/module/...），@click.stop 防止误触发展开。
    -->
    <el-menu-item v-if="isModuleLeaf" :index="moduleIndex">
      <el-icon v-if="selfIcon"><component :is="selfIcon" /></el-icon>
      <template #title>
        <span class="menu-text">{{ selfTitle }}</span>
        <span
          class="menu-badge"
          :class="{ 'is-current': workbenchActive }"
          title="进入该目录的功能导航页"
          @click.stop="openWorkbench"
        >工作台</span>
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
import { computed, inject } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

/**
 * 侧边栏菜单项（递归组件）。
 *
 * 层级规则（与参考图一致）：
 *   /supply                一级目录 → 可展开
 *   /supply/inventory      二级目录 → 点击在右侧展开子菜单卡片（不跳转）
 *   /supply/inventory/stock 三级功能 → **不在侧栏出现**，在右侧卡片里列出
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
const router = useRouter()

/** 由 Sidebar provide：统一走导航 + 关闭菜单，递归层数再多也不用逐层转发事件 */
const menuActions = inject('erpMenuActions', null)

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
 * 点击时由菜单浮层在右侧展开功能卡片；整页导航页改由「工作台」标签进入。
 */
const isModuleLeaf = computed(() => depth.value === 2 && !isLeaf.value)

/** 功能导航页地址，如 /module/supply/inventory */
const moduleNavPath = computed(() => `/module${props.basePath}`)

/**
 * 合成 index：不是真实路由，el-menu 的 select 会原样抛出来，
 * Sidebar 看到 module:: 前缀就转为「展开右侧子菜单」。
 */
const moduleIndex = computed(() => `module::${props.basePath}`)

/** 「工作台」标签：进入整页功能导航，同时收起菜单浮层 */
function openWorkbench() {
  if (menuActions) {
    menuActions.navigate(moduleNavPath.value)
  } else {
    router.push(moduleNavPath.value)
  }
}

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
