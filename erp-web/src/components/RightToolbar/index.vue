<template>
  <div class="right-toolbar">
    <el-tooltip content="搜索" placement="top">
      <el-button circle :icon="Search" @click="toggleSearch" />
    </el-tooltip>
    <el-tooltip content="刷新" placement="top">
      <el-button circle :icon="Refresh" @click="refresh" />
    </el-tooltip>
  </div>
</template>

<script setup>
import { Search, Refresh } from '@element-plus/icons-vue'

/**
 * 表格右上角的工具按钮：显隐搜索栏、刷新列表。
 *
 *   <RightToolbar v-model:showSearch="showSearch" @queryTable="getList" />
 */
const props = defineProps({
  showSearch: { type: Boolean, default: true }
})

const emit = defineEmits(['update:showSearch', 'queryTable'])

function toggleSearch() {
  emit('update:showSearch', !props.showSearch)
}

function refresh() {
  emit('queryTable')
}
</script>

<style scoped>
.right-toolbar {
  display: flex;
  gap: 8px;
}
</style>
