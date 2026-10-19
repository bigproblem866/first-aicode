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
      菜单浮层。

      必须 Teleport 到 body：
        1) 外层布局有 overflow: hidden，写在组件内会被裁掉；
        2) 浮层要覆盖在内容区之上，而不是挤压布局。
      起点固定在顶栏下方（top = 52px），左边贴齐，标准的下拉浮层位置。
    -->
    <Teleport to="body">
      <transition name="menu-fade">
        <div
          v-if="open"
          class="menu-panel"
          @mouseenter="cancelClose"
          @mouseleave="scheduleClose"
          @click="handlePanelClick"
        >
          <Sidebar />
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Menu } from '@element-plus/icons-vue'
import Sidebar from './Sidebar/index.vue'

/**
 * 顶栏菜单入口。
 *
 * 侧栏不再常驻占位，而是收进顶栏品牌区左侧的一个图标：
 * 悬停（或点击）展开浮层，浮层覆盖在内容上，关闭后内容区恢复全宽。
 *
 * 关闭时机：
 *   - 鼠标移出触发区/浮层（延迟 150ms，给「从图标移进浮层」留出时间）
 *   - 点击浮层里的可导航菜单项
 *   - 按 Esc、点击页面空白、路由变化
 */
const route = useRoute()
const open = ref(false)

let closeTimer = null

function openMenu() {
  cancelClose()
  open.value = true
}

function scheduleClose() {
  cancelClose()
  closeTimer = window.setTimeout(() => {
    open.value = false
  }, 150)
}

function cancelClose() {
  if (closeTimer) {
    window.clearTimeout(closeTimer)
    closeTimer = null
  }
}

function toggle() {
  if (open.value) {
    open.value = false
  } else {
    openMenu()
  }
}

/** 点中可导航的菜单项就收起；点一级目录的展开箭头时保持打开 */
function handlePanelClick(event) {
  const target = event.target
  if (target instanceof Element && target.closest('.el-menu-item')) {
    open.value = false
  }
}

function onKeydown(event) {
  if (event.key === 'Escape') open.value = false
}

function onWindowClick(event) {
  if (!open.value) return
  const target = event.target
  if (!(target instanceof Element)) return
  if (target.closest('.menu-launcher') || target.closest('.menu-panel')) return
  open.value = false
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
  () => {
    open.value = false
  }
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

.menu-panel {
  position: fixed;
  top: var(--header-height);
  bottom: 0;
  left: 0;
  z-index: 2500;
  width: var(--sidebar-width);
  background-color: #ffffff;
  box-shadow: 4px 0 16px rgba(15, 23, 42, 0.12);
  overflow: hidden;
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateX(-8px);
}
</style>
