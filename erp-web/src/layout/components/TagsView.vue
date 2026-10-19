<template>
  <div class="tags-view">
    <el-scrollbar class="tags-scrollbar">
      <!-- 整个标签条都是拖放热区，不需要精确对准某个标签 -->
      <div
        ref="tagsWrapRef"
        class="tags-wrap"
        @dragover.prevent="onDragOver"
        @drop.prevent="onDrop"
      >
        <router-link
          v-for="(tag, index) in visitedViews"
          :key="tag.path"
          :to="tag.fullPath"
          class="tags-item"
          :class="{
            active: isActive(tag),
            'is-dragging': dragIndex === index,
            'drop-before': dropIndex === index && dropIndex < visitedViews.length,
            'drop-after':
              dropIndex === visitedViews.length && index === visitedViews.length - 1
          }"
          :draggable="!tag.affix"
          @dragstart="onDragStart(index, $event)"
          @dragend="onDragEnd"
          @click="onTagClick"
          @contextmenu.prevent="openContextMenu(tag, $event)"
        >
          <span class="title">{{ tag.title }}</span>
          <el-icon
            v-if="!tag.affix"
            class="close-icon"
            draggable="false"
            @click.prevent.stop="closeTag(tag)"
          >
            <Close />
          </el-icon>
        </router-link>
      </div>
    </el-scrollbar>

    <el-dropdown class="tags-actions" trigger="click" @command="handleCommand">
      <el-icon class="more-icon"><ArrowDown /></el-icon>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="closeOthers">关闭其他</el-dropdown-item>
          <el-dropdown-item command="closeAll">关闭全部</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!--
      标签右键菜单。

      必须 Teleport 到 body：标签栏的 el-scrollbar 上有 overflow: hidden，
      菜单如果写在组件内部，会被裁掉，只在标签栏那 40px 高度里露出一条。
    -->
    <Teleport to="body">
      <ul
        v-if="contextMenu.visible"
        ref="menuRef"
        class="tags-context-menu"
        :style="{ left: `${contextMenu.x}px`, top: `${contextMenu.y}px` }"
        @click.stop
      >
        <li
          class="context-menu-item"
          :class="{ 'is-disabled': !canCloseCurrent }"
          @click="handleCloseCurrent"
        >
          关闭当前
        </li>
        <li
          class="context-menu-item"
          :class="{ 'is-disabled': !canCloseOthers }"
          @click="handleCloseOthers"
        >
          关闭其他
        </li>
        <li
          class="context-menu-item"
          :class="{ 'is-disabled': !canCloseAll }"
          @click="handleCloseAll"
        >
          关闭全部
        </li>
      </ul>
    </Teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close, ArrowDown } from '@element-plus/icons-vue'
import { useTagsViewStore } from '@/store/modules/tagsView'

/**
 * 顶部多页签。
 *
 * 只记录「有 title 的、非常量白名单」的页面；login/404/401 这类不进标签栏。
 *
 * 关闭操作有三处入口，共用同一套 handler：
 *   1) 标签上的 ×
 *   2) 标签右键菜单
 *   3) 右上角下拉菜单
 */
const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()

const visitedViews = computed(() => tagsViewStore.visitedViews)

const IGNORE_PATHS = ['/login', '/404', '/401']

/**
 * 当前路由对应的标签路径。
 *
 * 单据卡片页不建自己的标签（meta.noTag），而是用 meta.activeMenu 指回列表页，
 * 所以停在卡片页时高亮的就是列表那个标签 —— 否则标签栏里没有任何高亮项。
 */
const activeTagPath = computed(
  () => (route.meta && route.meta.activeMenu) || route.path
)

function isActive(tag) {
  return tag.path === activeTagPath.value
}

/**
 * 确保固定标签（首页）始终存在且排在最前。
 *
 * 标签不持久化，直接刷新到某个业务页时 visitedViews 里只有当前页；
 * 这里从路由表把 affix 路由补进来（addVisitedView 会把它们插到最前）。
 */
function ensureAffixViews() {
  const affixRoutes = router.getRoutes().filter((r) => r.meta && r.meta.affix && r.name)
  for (const r of affixRoutes) {
    tagsViewStore.addView({
      name: r.name,
      path: r.path,
      fullPath: r.path,
      meta: r.meta
    })
  }
}

function addCurrentView() {
  if (!route.name || IGNORE_PATHS.includes(route.path)) return
  // meta.noTag：功能导航页（启动器）与单据卡片页不占标签位
  if (route.meta && route.meta.noTag) return
  tagsViewStore.addView({
    name: route.name,
    path: route.path,
    fullPath: route.fullPath,
    meta: route.meta
  })
}

/**
 * 关闭单个标签。
 *
 * 关闭的是当前页时，跳「右邻居」而不是数组最后一个 ——
 * 开 5 个标签关中间一个却跳到最右边，体感上是页面乱飞。
 */
function closeTag(tag) {
  const index = visitedViews.value.findIndex((v) => v.path === tag.path)
  if (index === -1) return

  const wasActive = isActive(tag)
  tagsViewStore.delView(tag)

  if (!wasActive) return

  // delView 之后原右邻居正好落到 index 上；它是最后一个就取左邻居
  const list = visitedViews.value
  const next = list[index] || list[index - 1] || null
  router.push(next ? next.fullPath : '/')
}

/** 保留 target 与固定标签（首页），其余关掉；然后停在 target 上 */
function closeOtherTags(target) {
  if (!target) return
  tagsViewStore.delOthersViews(target)
  if (!isActive(target)) router.push(target.fullPath)
}

/** 只保留固定标签（首页）；没有固定标签就回首页 */
function closeAllTags() {
  tagsViewStore.delAllViews()
  const list = visitedViews.value
  const last = list[list.length - 1]
  if (!last) {
    router.push('/')
  } else if (!isActive(last)) {
    router.push(last.fullPath)
  }
}

/* ------------------------------------------------------------------ */
/* 右键菜单                                                            */
/* ------------------------------------------------------------------ */

const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  tag: null
})

const menuRef = ref(null)

/** 固定标签（affix，目前是首页）不允许关闭 */
const canCloseCurrent = computed(() => !!contextMenu.tag && !contextMenu.tag.affix)

/** 「关闭其他」要真的还有别人可关，否则置灰 */
const canCloseOthers = computed(() => {
  const target = contextMenu.tag
  if (!target) return false
  return visitedViews.value.some((v) => !v.affix && v.path !== target.path)
})

const canCloseAll = computed(() => visitedViews.value.some((v) => !v.affix))

async function openContextMenu(tag, event) {
  contextMenu.tag = tag
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.visible = true

  // 等菜单渲染出来再量尺寸；靠近窗口右/下边缘时往回收，避免出屏
  await nextTick()
  const el = menuRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const gap = 8
  const maxX = window.innerWidth - rect.width - gap
  const maxY = window.innerHeight - rect.height - gap
  if (contextMenu.x > maxX) contextMenu.x = Math.max(gap, maxX)
  if (contextMenu.y > maxY) contextMenu.y = Math.max(gap, maxY)
}

function closeContextMenu() {
  contextMenu.visible = false
  contextMenu.tag = null
}

function handleCloseCurrent() {
  if (!canCloseCurrent.value) return
  const tag = contextMenu.tag
  closeContextMenu()
  closeTag(tag)
}

function handleCloseOthers() {
  if (!canCloseOthers.value) return
  const tag = contextMenu.tag
  closeContextMenu()
  closeOtherTags(tag)
}

function handleCloseAll() {
  if (!canCloseAll.value) return
  closeContextMenu()
  closeAllTags()
}

/** 右上角下拉菜单：以「当前页」为保留对象 */
function handleCommand(command) {
  if (command === 'closeOthers') {
    const current = visitedViews.value.find((tag) => tag.path === activeTagPath.value)
    // 当前页是功能导航页时它不在标签栏里（见 IGNORE_ROUTE_NAMES），
    // 没有"自己"可保留，退化成关闭全部，避免点了没反应
    if (!current) {
      closeAllTags()
      return
    }
    closeOtherTags(current)
  } else if (command === 'closeAll') {
    closeAllTags()
  }
}

/* ------------------------------------------------------------------ */
/* 拖动排序（原生 HTML5 DnD，不引入额外依赖）                          */
/* ------------------------------------------------------------------ */

const tagsWrapRef = ref(null)
const dragIndex = ref(-1)
/** 插入位置（0..length），不是目标标签下标 */
const dropIndex = ref(-1)
let lastDragEndAt = 0

function onDragStart(index, event) {
  dragIndex.value = index
  dropIndex.value = -1
  closeContextMenu()
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    // 写入纯文本，避免浏览器把标签当成链接拖到地址栏
    event.dataTransfer.setData('text/plain', visitedViews.value[index].path)
  }
}

/**
 * 在整个标签条上计算插入位置。
 *
 * 以每个标签的**水平中点**为分界线：鼠标在某个中点左侧就插到它前面。
 * 这样落在标签之间的空隙、右侧空白处也能正常排序 —— 不必精确对准某个标签。
 */
function onDragOver(event) {
  if (dragIndex.value === -1 || !tagsWrapRef.value) return
  const items = Array.from(tagsWrapRef.value.querySelectorAll('.tags-item'))
  let index = items.length
  for (let i = 0; i < items.length; i++) {
    const rect = items[i].getBoundingClientRect()
    if (event.clientX < rect.left + rect.width / 2) {
      index = i
      break
    }
  }
  // 首页固定最前：插入位置不能落在固定标签之前
  const affixCount = visitedViews.value.filter((v) => v.affix).length
  dropIndex.value = Math.max(index, affixCount)
}

function onDrop() {
  if (dragIndex.value !== -1 && dropIndex.value !== -1) {
    tagsViewStore.moveView(dragIndex.value, dropIndex.value)
  }
  onDragEnd()
}

function onDragEnd() {
  dragIndex.value = -1
  dropIndex.value = -1
  lastDragEndAt = Date.now()
}

/** 拖拽结束后浏览器偶尔仍会补一个 click，阻止它触发跳转 */
function onTagClick(event) {
  if (Date.now() - lastDragEndAt < 150) {
    event.preventDefault()
    event.stopPropagation()
  }
}

function onKeydown(event) {
  if (event.key === 'Escape') closeContextMenu()
}

onMounted(() => {
  ensureAffixViews()
  addCurrentView()
  window.addEventListener('click', closeContextMenu)
  // capture = true：连内容区内部的滚动也能收到，菜单不会跟着页面飘走
  window.addEventListener('scroll', closeContextMenu, true)
  window.addEventListener('resize', closeContextMenu)
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', closeContextMenu)
  window.removeEventListener('scroll', closeContextMenu, true)
  window.removeEventListener('resize', closeContextMenu)
  window.removeEventListener('keydown', onKeydown)
})

watch(
  () => route.path,
  () => {
    addCurrentView()
    closeContextMenu()
  }
)
</script>

<style scoped>
.tags-view {
  flex-shrink: 0;
  height: var(--tags-view-height);
  display: flex;
  align-items: center;
  padding-left: 10px;
  background-color: #ffffff;
  border-bottom: 1px solid var(--border-color);
}

.tags-scrollbar {
  flex: 1;
  overflow: hidden;
}

.tags-wrap {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 4px;
  white-space: nowrap;
}

.tags-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 10px;
  font-size: 12px;
  color: #6b7280;
  background-color: transparent;
  border-radius: 6px;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    color: #303133;
    background-color: #f5f7fa;
  }

  &.active {
    font-weight: 500;
    color: var(--primary-color);
    background-color: #fbe6e8;
  }
}

/* 拖动排序：被拖的标签半透明，目标位置显示一条主色插入线 */
.tags-item.is-dragging {
  opacity: 0.45;
}

.tags-item.drop-before::before,
.tags-item.drop-after::after {
  content: '';
  position: absolute;
  top: 4px;
  bottom: 4px;
  width: 2px;
  background-color: var(--primary-color);
  border-radius: 1px;
}

.tags-item.drop-before::before {
  left: -3px;
}

.tags-item.drop-after::after {
  right: -3px;
}

.close-icon {
  margin-left: 2px;
  padding: 1px;
  font-size: 11px;
  border-radius: 50%;

  &:hover {
    color: #ffffff;
    background-color: rgba(217, 0, 27, 0.6);
  }
}

.tags-actions {
  padding: 0 12px;
  border-left: 1px solid var(--border-color);
  cursor: pointer;
}

.more-icon {
  color: #6b7280;
}

/* 右键菜单（Teleport 到 body，scoped 属性仍会带在元素上，样式生效） */
.tags-context-menu {
  position: fixed;
  z-index: 3000;
  min-width: 132px;
  margin: 0;
  padding: 4px;
  list-style: none;
  background-color: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.context-menu-item {
  padding: 7px 14px;
  font-size: 13px;
  line-height: 20px;
  color: #303133;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;

  &:hover {
    color: var(--primary-color);
    background-color: #fbe6e8;
  }

  &.is-disabled {
    color: #c0c4cc;
    pointer-events: none;
    background-color: transparent;
  }
}
</style>
