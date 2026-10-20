<template>
  <div class="menu-launcher" @mouseenter="openMenu" @mouseleave="scheduleClose">
    <el-tooltip content="菜单" placement="bottom" :show-after="400">
      <button
        class="menu-trigger"
        :class="{ 'is-open': open }"
        type="button"
        aria-label="打开菜单"
        @click.stop="toggle"
      >
        <el-icon><Menu /></el-icon>
      </button>
    </el-tooltip>

    <!--
      菜单浮层（两层）。

      必须 Teleport 到 body：
        1) 外层布局有 overflow: hidden，写在组件内会被裁掉；
        2) 浮层要覆盖在内容区之上，而不是挤压布局。

      两层放在同一个 fixed 容器里（左菜单 + 右子菜单卡片，间距 8px），
      这样鼠标从菜单移到卡片不会触发容器的 mouseleave，浮层不会被误关。

      交互：
        - 点二级目录（库存管理）→ 不跳转，右侧展开该目录的三级功能卡片
        - 点三级功能 → 跳转，两层一起收起
        - 卡片右上的「工作台」→ 进入整页功能导航 /module/<目录>
    -->
    <Teleport to="body">
      <transition name="menu-fade">
        <div
          v-if="open"
          class="menu-layer"
          @mouseenter="cancelClose"
          @mouseleave="scheduleClose"
        >
          <div class="menu-panel">
            <Sidebar
              :selected-module="selectedModule"
              @module-select="handleModuleSelect"
              @navigate="closeMenu"
            />
          </div>

          <transition name="submenu-fade">
            <section v-if="submenu" class="submenu-card">
              <header class="submenu-head">
                <span class="submenu-title">{{ submenu.title }}</span>
                <button
                  class="submenu-more"
                  type="button"
                  title="进入该目录的功能导航页"
                  @click="openWorkbench"
                >
                  工作台
                  <el-icon><ArrowRight /></el-icon>
                </button>
              </header>

              <div class="submenu-body">
                <div
                  v-for="group in submenu.groups"
                  :key="group.title"
                  class="submenu-group"
                >
                  <p v-if="!submenu.hideGroupTitle" class="submenu-group-title">
                    {{ group.title }}
                  </p>
                  <button
                    v-for="item in group.items"
                    :key="item.path"
                    type="button"
                    class="submenu-item"
                    :class="{ 'is-active': route.path === item.path }"
                    @click="goPath(item.path)"
                  >
                    <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                    <span class="submenu-item-text">{{ item.title }}</span>
                  </button>
                </div>
              </div>
            </section>
          </transition>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Menu } from '@element-plus/icons-vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import Sidebar from './Sidebar/index.vue'
import { usePermissionStore } from '@/store/modules/permission'
import { collectMenuFunctions, findMenuByPath, menuTitle } from '@/utils/menu'

/**
 * 顶栏菜单入口。
 *
 * 侧栏不再常驻占位，而是收进顶栏品牌区左侧的一个图标：
 * 悬停（或点击）展开浮层，浮层覆盖在内容上，关闭后内容区恢复全宽。
 *
 * 关闭时机：
 *   - 鼠标移出两层浮层（延迟 150ms，给「从图标移进浮层」留出时间）
 *   - 选中三级功能、点「工作台」
 *   - 按 Esc、点击页面空白、路由变化
 */
const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()

const open = ref(false)

/**
 * 右侧卡片对应的二级目录完整路径（如 /supply/inventory）。
 * 空串表示不展开 —— 关闭菜单时一起清掉，下次打开从干净状态开始。
 */
const selectedModule = ref('')

let closeTimer = null

/**
 * 右侧卡片数据。
 *
 * 直接用已授权的菜单树算出来，不额外维护导航配置：
 * 后端加了菜单这里自动出现，权限被收走这里也自动消失。
 */
const submenu = computed(() => {
  if (!selectedModule.value) return null
  const match = findMenuByPath(permissionStore.sidebarRouters, '', selectedModule.value)
  if (!match) return null

  const children = (match.route.children || []).filter((child) => !child.hidden)
  const functions = collectMenuFunctions(children, match.fullPath)
  if (!functions.length) return null

  const title = menuTitle(match.route)

  // 按 group_name 分区（如 库存查询 / 库存业务）；没有分组的归到目录名下
  const groups = []
  const groupIndex = new Map()
  for (const item of functions) {
    const groupName = item.groupName || title
    if (!groupIndex.has(groupName)) {
      groupIndex.set(groupName, { title: groupName, items: [] })
      groups.push(groupIndex.get(groupName))
    }
    groupIndex.get(groupName).items.push({
      ...item,
      // 图标名来自后端，先校验存在性，避免 <component> 解析失败刷告警
      icon: ElementPlusIconsVue[item.icon] ? item.icon : ''
    })
  }

  return {
    path: match.fullPath,
    title,
    groups,
    // 只有一个分区且标题与目录同名时，再显示分区标题就是重复信息
    hideGroupTitle: groups.length === 1 && groups[0].title === title
  }
})

function openMenu() {
  cancelClose()
  open.value = true
}

function scheduleClose() {
  cancelClose()
  closeTimer = window.setTimeout(closeMenu, 150)
}

function cancelClose() {
  if (closeTimer) {
    window.clearTimeout(closeTimer)
    closeTimer = null
  }
}

/** 收起两层浮层，并清掉右侧卡片状态 */
function closeMenu() {
  cancelClose()
  open.value = false
  selectedModule.value = ''
}

function toggle() {
  if (open.value) {
    closeMenu()
  } else {
    openMenu()
  }
}

/** 点二级目录：只切换右侧卡片，不导航 */
function handleModuleSelect(path) {
  cancelClose()
  selectedModule.value = path
}

function goPath(path) {
  closeMenu()
  router.push(path)
}

function openWorkbench() {
  if (!submenu.value) return
  const target = `/module${submenu.value.path}`
  closeMenu()
  router.push(target)
}

function onKeydown(event) {
  if (event.key === 'Escape') closeMenu()
}

function onWindowClick(event) {
  if (!open.value) return
  const target = event.target
  if (!(target instanceof Element)) return
  if (target.closest('.menu-launcher') || target.closest('.menu-layer')) return
  closeMenu()
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  window.addEventListener('click', onWindowClick)
})

onBeforeUnmount(() => {
  cancelClose()
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('click', onWindowClick)
})

watch(
  () => route.path,
  () => closeMenu()
)
</script>

<style scoped>
.menu-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  font-size: 18px;
  color: #4b5563;
  background-color: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: color 0.15s, background-color 0.15s;
}

.menu-trigger:hover,
.menu-trigger.is-open {
  color: var(--primary-color);
  background-color: var(--el-color-primary-light-9);
}

/* 两层浮层的公共定位容器：左边菜单，右边子菜单，两栏贴齐 */
.menu-layer {
  position: fixed;
  top: var(--header-height);
  bottom: 0;
  left: 0;
  z-index: 2500;
  display: flex;
  align-items: stretch;
  gap: 0;
}

.menu-panel {
  flex-shrink: 0;
  width: var(--sidebar-width);
  height: 100%;
  background-color: #ffffff;
  /* 与右栏之间用一条细线分隔；外阴影交给右栏，避免画到卡片上 */
  border-right: 1px solid var(--border-color);
  overflow: hidden;
}

/* ---------- 右侧：二级目录的子菜单（与一级菜单同宽同高） ---------- */
.submenu-card {
  display: flex;
  flex-direction: column;
  width: var(--sidebar-width);
  height: 100%;
  background-color: #ffffff;
  /* 右侧保留外阴影，与底层内容区拉开距离 */
  box-shadow: 4px 0 16px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.submenu-head {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  /* 行高与一级菜单一致（$sidebar-item-height = 50px） */
  height: 50px;
  padding: 0 12px 0 20px;
  border-bottom: 1px solid var(--border-color);
}

.submenu-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.submenu-more {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 2px;
  padding: 2px 4px;
  font-size: 12px;
  color: #8a94a6;
  background: none;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: color 0.15s, background-color 0.15s;
}

.submenu-more:hover {
  color: var(--primary-color);
  background-color: var(--el-color-primary-light-9);
}

.submenu-body {
  flex: 1;
  min-height: 0;
  padding: 6px 0 10px;
  overflow-y: auto;
}

.submenu-group-title {
  margin: 12px 20px 4px;
  font-size: 12px;
  letter-spacing: 0.04em;
  color: #a0a7b4;
}

/* 行高、字号、左缩进、通栏高亮都与一级菜单保持一致 */
.submenu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 50px;
  padding: 0 20px;
  font-size: 14px;
  color: #4b5563;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.15s, background-color 0.15s;
}

.submenu-item .el-icon {
  color: #8a94a6;
}

.submenu-item:hover {
  color: var(--primary-color);
  background-color: #f5f7fa;
}

.submenu-item:hover .el-icon {
  color: var(--primary-color);
}

.submenu-item.is-active {
  font-weight: 500;
  color: var(--primary-color);
  background-color: var(--el-color-primary-light-9);
}

.submenu-item-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---------- 动画 ---------- */
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}

.submenu-fade-enter-active,
.submenu-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.submenu-fade-enter-from,
.submenu-fade-leave-to {
  opacity: 0;
  transform: translateX(-6px);
}
</style>
