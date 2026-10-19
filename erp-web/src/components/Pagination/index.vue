<template>
  <div class="pagination-wrapper">
    <el-pagination
      :current-page="page"
      :page-size="limit"
      :total="total"
      :page-sizes="pageSizes"
      :layout="layout"
      :background="background"
      :disabled="disabled"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
/**
 * 分页封装。
 *
 * 用法：
 *   <Pagination
 *     v-model:page="queryParams.pageNum"
 *     v-model:limit="queryParams.pageSize"
 *     :total="total"
 *     @pagination="getList"
 *   />
 *
 * 刻意不用 el-pagination 的 v-model:current-page + @current-change 同时绑，
 * 那样会 emit 两次 update:page；这里只用一个入口，逻辑更好追。
 */
const props = defineProps({
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  limit: { type: Number, default: 10 },
  pageSizes: {
    type: Array,
    default: () => [10, 20, 30, 50, 100]
  },
  layout: {
    type: String,
    default: 'total, sizes, prev, pager, next, jumper'
  },
  background: { type: Boolean, default: true },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['update:page', 'update:limit', 'pagination'])

function handleSizeChange(size) {
  // 改了每页条数后当前页大概率越界，回到第 1 页是通行做法
  emit('update:limit', size)
  emit('update:page', 1)
  emit('pagination', { page: 1, limit: size })
}

function handleCurrentChange(currentPage) {
  emit('update:page', currentPage)
  emit('pagination', { page: currentPage, limit: props.limit })
}
</script>
