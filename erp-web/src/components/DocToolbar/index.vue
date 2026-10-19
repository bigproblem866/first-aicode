<template>
  <div class="doc-toolbar">
    <div class="doc-toolbar-left">
      <el-tooltip
        v-for="action in visibleActions"
        :key="action.key"
        :content="action.reason || ''"
        :disabled="!showReason(action)"
        placement="top"
      >
        <!--
          tooltip 必须包一层 span：Element Plus 的 disabled 按钮不触发鼠标事件，
          直接写在按钮上时置灰后看不到「为什么不能点」的提示。
        -->
        <span class="doc-toolbar-item">
          <el-button
            size="small"
            :type="action.type"
            :icon="action.icon"
            :disabled="action.disabled"
            :loading="action.loading"
            @click="handleClick(action)"
          >
            {{ action.label }}
          </el-button>
        </span>
      </el-tooltip>
    </div>

    <div class="doc-toolbar-right">
      <slot name="right" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { checkPermi } from '@/utils/permission'

/**
 * 单据操作条（卡片页顶部）。
 *
 * 设计目标：按钮的「有没有 / 能不能点 / 为什么不能点」全部由数据决定，
 * 三个卡片页不再各写一套 v-if，状态规则也更容易对照后端。
 *
 * action 结构：
 * {
 *   key: 'save',                 // 唯一键
 *   label: '保存',
 *   type: 'primary',             // el-button type，可选
 *   icon: Plus,                  // 图标组件，可选
 *   loading: saving,             // 可选
 *   perms: ['pur:order:add'],    // 可选；不传表示不需要权限
 *   visible: true,               // 可选；false 表示该单据类型根本没有这个动作
 *   disabled: false,             // 状态导致不可用 → 常驻置灰
 *   reason: '已审核，不能修改',   // 置灰原因，hover 时显示
 *   onClick: handleSave
 * }
 *
 * 三条规则：
 *   1. 状态不可用 → 按钮保留、置灰、tooltip 说明原因（用户能看到单据能做什么）
 *   2. 业务上不存在 → visible:false 不渲染（例如库存单没有「下推」）
 *   3. 无权限 → 直接不渲染（沿用 checkPermi 的语义）
 */
const props = defineProps({
  actions: { type: Array, default: () => [] }
})

const visibleActions = computed(() =>
  props.actions.filter(
    (action) => action.visible !== false && checkPermi(action.perms)
  )
)

function showReason(action) {
  return !!action.disabled && !!action.reason
}

function handleClick(action) {
  if (action.disabled) return
  if (action.onClick) action.onClick()
}
</script>

<style scoped>
.doc-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  /* 与下方 .bill-card 对齐：同样自适应并居中 */
  max-width: var(--page-max-width);
  margin: 0 auto 12px;
  padding: 8px 10px;
  background-color: #ffffff;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-card);
}

.doc-toolbar-left,
.doc-toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.doc-toolbar-item {
  display: inline-flex;
}
</style>
