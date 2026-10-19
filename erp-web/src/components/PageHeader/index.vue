<template>
  <div class="page-header">
    <div class="page-header-main">
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="description" class="page-desc">{{ description }}</p>
    </div>

    <!-- 页面级主操作（新增/审核/导入…）。表内的行操作仍留在表格里 -->
    <div v-if="$slots.actions" class="page-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup>
/**
 * 页面头部：标题 + 说明 + 右侧操作区。
 *
 * 用它是为了让每个页面有明确的「我在哪一页、这一页能做什么」，而不是
 * 一进来就是一张表格。BIP 一类的 ERP 里，页面标题和主操作都在这一层。
 *
 * 用法：
 *   <PageHeader title="物料管理" description="维护物料主数据">
 *     <template #actions>
 *       <el-button type="primary">新增</el-button>
 *     </template>
 *   </PageHeader>
 */
defineProps({
  title: { type: String, required: true },
  description: { type: String, default: '' }
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
  color: #1f2937;
}

.page-desc {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #8a94a6;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
</style>
