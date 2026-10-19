<template>
  <div class="app-container">
    <PageHeader :title="moduleTitle" description="选择一个功能进入" />

    <el-empty v-if="!moduleRoute" description="未找到该模块" />

    <div v-else class="nav-sections">
      <section v-for="group in groups" :key="group.key" class="nav-section">
        <h3 class="nav-section-title">{{ group.title }}</h3>
        <div class="nav-links">
          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            class="nav-link"
          >
            {{ item.title }}
          </router-link>
        </div>
      </section>

      <el-empty v-if="!groups.length" description="该模块下还没有可访问的功能" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/store/modules/permission'

/**
 * 功能导航页（/module/:key）。
 *
 * 布局参考金蝶 AI 套件：按「二级目录」分区块，每块一个标题 + 多列链接网格。
 * 数据完全来自后端下发的菜单树，不额外维护一份导航配置 ——
 * 加了菜单这里自动出现，权限删了这里也自动消失。
 *
 * 例如 供应链(/supply)：
 *   库存管理  → 仓库管理 / 库存查询 / 入库单 / 出库单 / 库存流水
 *   采购管理  → 采购订单 / 采购收货
 */
const route = useRoute()
const permissionStore = usePermissionStore()

function joinPath(base, path) {
  if (!path) return base || '/'
  if (/^https?:\/\//.test(path)) return path
  if (path.startsWith('/')) return path
  if (!base) return `/${path}`
  return `${base}/${path}`.replace(/\/{2,}/g, '/')
}

function collectLeaves(routes, base) {
  const out = []
  for (const item of routes || []) {
    if (item.hidden) continue
    const full = joinPath(base, item.path)
    const children = (item.children || []).filter((c) => !c.hidden)
    if (children.length) {
      out.push(...collectLeaves(children, full))
    } else if (item.meta && item.meta.title) {
      out.push({ path: full, title: item.meta.title })
    }
  }
  return out
}

/**
 * 在整棵已授权路由树里按完整路径查找（支持 /supply/inventory 这种多级 key）。
 *
 * ⚠ 必须同时返回 fullPath：二级目录的 path 是**相对值**（inventory / purchase），
 *   只返回节点本身的话，调用方拿它当根路径会拼出 /purchase/order 这种错误地址。
 */
function findRouteByPath(routes, base, target) {
  for (const item of routes || []) {
    if (item.hidden) continue
    const full = joinPath(base, item.path)
    if (full === target) return { route: item, fullPath: full }
    const found = findRouteByPath(item.children, full, target)
    if (found) return found
  }
  return null
}

const moduleMatch = computed(() => {
  const key = String(route.params.key || '').replace(/^\//, '')
  return findRouteByPath(permissionStore.sidebarRouters, '', `/${key}`)
})

const moduleRoute = computed(() => (moduleMatch.value ? moduleMatch.value.route : null))

/** 模块的完整路径，例如 /supply/purchase（不是相对的 purchase） */
const moduleBase = computed(() => (moduleMatch.value ? moduleMatch.value.fullPath : ''))

const moduleTitle = computed(
  () => (moduleRoute.value && moduleRoute.value.meta && moduleRoute.value.meta.title) || '功能导航'
)

const groups = computed(() => {
  const current = moduleRoute.value
  if (!current) return []

  // 用查找时算出的完整路径，避免相对 path 拼错（见 findRouteByPath 的注释）
  const base = moduleBase.value

  const directoryGroups = []
  // 叶子功能按 group_name 归组（Map 保留菜单顺序）
  const functionGroups = new Map()

  for (const child of (current.children || []).filter((c) => !c.hidden)) {
    const full = joinPath(base, child.path)
    const children = (child.children || []).filter((c) => !c.hidden)
    if (children.length) {
      // 目录：按目录本身分区（如 供应链 → 库存管理 / 采购管理）
      directoryGroups.push({
        key: child.path,
        title: (child.meta && child.meta.title) || '',
        items: collectLeaves(children, full)
      })
    } else if (child.meta && child.meta.title) {
      // 功能：按 group_name 分区；为空时回退到所属目录名
      const groupName = child.groupName || moduleTitle.value
      if (!functionGroups.has(groupName)) functionGroups.set(groupName, [])
      functionGroups.get(groupName).push({ path: full, title: child.meta.title })
    }
  }

  const functionSections = Array.from(functionGroups.entries()).map(
    ([title, items]) => ({ key: `group:${title}`, title, items })
  )

  return [...functionSections, ...directoryGroups]
})
</script>

<style scoped>
.nav-sections {
  padding: 24px;
  background-color: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-card);
}

.nav-section {
  margin-bottom: 28px;
}

.nav-section:last-child {
  margin-bottom: 0;
}

.nav-section-title {
  position: relative;
  margin: 0 0 12px;
  padding-left: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

/* 分区标题前的短竖条（参考图里的主色标记） */
.nav-section-title::before {
  content: '';
  position: absolute;
  top: 3px;
  bottom: 3px;
  left: 0;
  width: 3px;
  background-color: var(--primary-color);
  border-radius: 2px;
}

.nav-links {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px 24px;
}

.nav-link {
  padding: 6px 0;
  font-size: 13px;
  color: #4b5563;
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-link:hover {
  color: var(--primary-color);
}

@media (max-width: 1200px) {
  .nav-links {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .nav-links {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
