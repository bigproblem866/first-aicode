<template>
  <div class="global-search">
    <div class="gs-field">
      <el-input
        v-model="keyword"
        size="small"
        placeholder="请输入应用/业务对象"
        :prefix-icon="Search"
        clearable
        @keyup.enter="jumpFirst"
        @focus="focused = true"
        @blur="onBlur"
      />

      <div v-if="keyword && results.length" class="gs-panel">
        <div
          v-for="item in results"
          :key="item.path"
          class="gs-item"
          @mousedown.prevent="jump(item.path)"
        >
          <span class="gs-item-title">{{ item.title }}</span>
          <span class="gs-item-parent">{{ item.parentTitle }}</span>
        </div>
      </div>
      <div v-else-if="keyword && focused" class="gs-panel gs-empty">
        没有匹配的功能
      </div>
    </div>

    <div class="gs-actions">
      <el-tooltip content="当前模块的功能导航" placement="bottom">
        <el-icon class="gs-icon" @click="goModuleNav"><Grid /></el-icon>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Grid, Search } from '@element-plus/icons-vue'
import { usePermissionStore } from '@/store/modules/permission'

/**
 * 全局搜索栏（顶栏下方独立一行）。
 *
 * 布局对齐金蝶 AI 套件：整宽白条，左侧是大搜索框，右侧是辅助动作。
 * 搜索的是**已下发的菜单**（本地过滤，不发请求），回车或点击直接跳转。
 * 右侧的「功能导航」进入当前模块的功能目录页（/module/:key）。
 */
const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

const keyword = ref('')
const focused = ref(false)

function joinPath(base, path) {
  if (!path) return base || '/'
  if (/^https?:\/\//.test(path)) return path
  if (path.startsWith('/')) return path
  if (!base) return `/${path}`
  return `${base}/${path}`.replace(/\/{2,}/g, '/')
}

function collectLeaves(routes, base = '', parentTitle = '') {
  const out = []
  for (const item of routes || []) {
    if (item.hidden) continue
    const full = joinPath(base, item.path)
    const title = (item.meta && item.meta.title) || ''
    const children = (item.children || []).filter((c) => !c.hidden)
    if (children.length) {
      out.push(...collectLeaves(children, full, title || parentTitle))
    } else if (title) {
      out.push({ path: full, title, parentTitle })
    }
  }
  return out
}

const allLeaves = computed(() => collectLeaves(permissionStore.sidebarRouters))

const results = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return []
  return allLeaves.value
    .filter((item) => item.title.toLowerCase().includes(kw))
    .slice(0, 8)
})

/** 当前所在的顶级模块（用于「功能导航」按钮） */
const currentModuleKey = computed(() => {
  const top = permissionStore.sidebarRouters.find((r) => !r.hidden && r.meta && r.meta.title)
  if (!top) return ''
  const matched = permissionStore.sidebarRouters.find((r) => {
    if (r.hidden || !r.meta || !r.meta.title) return false
    const base = r.path && r.path.startsWith('/') ? r.path : `/${r.path || ''}`
    return route.path === base || route.path.startsWith(`${base}/`)
  })
  const key = matched ? matched.path : ''
  return String(key).replace(/^\//, '')
})

function jump(path) {
  keyword.value = ''
  focused.value = false
  router.push(path)
}

function jumpFirst() {
  if (results.value.length) jump(results.value[0].path)
}

function goModuleNav() {
  if (currentModuleKey.value) router.push(`/module/${currentModuleKey.value}`)
}

/** 面板用 mousedown 触发跳转，这里延迟收起，避免 blur 先于点击把面板关掉 */
function onBlur() {
  window.setTimeout(() => {
    focused.value = false
  }, 120)
}
</script>

<style scoped>
/* 已并入顶栏右侧：不再有独立的一行高度/背景/边框 */
.global-search {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.gs-field {
  position: relative;
  width: 240px;
}

/* 顶栏是白底，给输入框一点浅灰填充，边界更清楚 */
.global-search :deep(.el-input__wrapper) {
  background-color: #f5f7fa;
  box-shadow: none;
}

.global-search :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
  box-shadow: 0 0 0 1px var(--primary-color) inset;
}

.gs-panel {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 3000;
  padding: 6px;
  background-color: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-card);
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.1);
}

.gs-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  font-size: 13px;
  color: #303133;
  border-radius: var(--radius-control);
  cursor: pointer;

  &:hover {
    color: var(--primary-color);
    background-color: #fdf4f5;
  }
}

.gs-item-parent {
  font-size: 12px;
  color: #9aa3b2;
}

.gs-empty {
  padding: 12px;
  font-size: 13px;
  color: #9aa3b2;
  text-align: center;
}

.gs-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.gs-icon {
  font-size: 16px;
  color: #6b7280;
  cursor: pointer;

  &:hover {
    color: var(--primary-color);
  }
}
</style>
