<template>
  <div class="app-container">
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="收货单号">
          <el-input
            v-model="queryParams.receiptNo"
            placeholder="请输入收货单号"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="来源订单号">
          <el-input
            v-model="queryParams.orderNo"
            placeholder="请输入采购订单号"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="queryParams.status" type="pur_receipt_status" style="width: 120px" />
        </el-form-item>
        <el-form-item>
          <el-button size="small" type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button size="small" :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button
            v-hasPermi="['pur:receipt:add']"
            size="small"
            type="primary"
            :icon="Plus"
            @click="handleAdd"
          >
            新增
          </el-button>
          <el-tooltip
            :content="deleteDisabledReason"
            :disabled="!deleteDisabledReason"
            placement="top"
          >
            <span class="toolbar-item">
              <el-button
                v-hasPermi="['pur:receipt:remove']"
                size="small"
                type="danger"
                :icon="Delete"
                :disabled="!!deleteDisabledReason"
                @click="handleDelete()"
              >
                删除
              </el-button>
            </span>
          </el-tooltip>
        </div>
        <RightToolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        class="clickable-table"
        @row-click="handleRowClick"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="收货单号" min-width="150">
          <template #default="{ row }">
            <span class="doc-link">{{ row.receiptNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源订单号" prop="orderNo" min-width="150" show-overflow-tooltip />
        <el-table-column label="供应商" prop="supplierName" min-width="170" show-overflow-tooltip />
        <el-table-column label="收货仓库" prop="warehouseName" min-width="130" show-overflow-tooltip />
        <el-table-column label="收货日期" prop="receiptDate" width="120" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" type="pur_receipt_status" />
          </template>
        </el-table-column>
        <el-table-column label="生成的入库单" prop="invOrderNo" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.invOrderNo">{{ row.invOrderNo }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <Pagination
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { listReceipt, delReceipt } from '@/api/purchase/receipt'

/**
 * 采购收货列表。
 *
 * 与另外两张单据一致：列表只负责浏览与进入，**点击一行进入收货单卡片页**
 * （/supply/purchase/receipt/:id）。新建也走卡片页（/supply/purchase/receipt/new），
 * 在卡片页里选采购订单、带出明细。
 */
const router = useRouter()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const list = ref([])
const selectedIds = ref([])
const selectedRows = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  receiptNo: undefined,
  orderNo: undefined,
  status: undefined
})

function getList() {
  loading.value = true
  listReceipt(queryParams)
    .then((res) => {
      list.value = (res && res.rows) || []
      total.value = (res && res.total) || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.receiptNo = undefined
  queryParams.orderNo = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
  selectedIds.value = rows.map((row) => row.id)
}

/** 删除按钮的置灰原因（空字符串表示可点）：后端只允许草稿整批删除 */
const deleteDisabledReason = computed(() => {
  if (!selectedRows.value.length) return '请先选择要删除的收货单'
  if (selectedRows.value.some((row) => String(row.status) !== '0')) {
    return '只有草稿状态的收货单可以删除'
  }
  return ''
})

function handleAdd() {
  router.push('/supply/purchase/receipt/new')
}

/** 点击行进入收货单卡片；勾选用的 selection 列排除掉 */
function handleRowClick(row, column) {
  if (column && column.type === 'selection') return
  router.push(`/supply/purchase/receipt/${row.id}`)
}

function handleDelete() {
  const ids = selectedIds.value.map((id) => String(id))
  if (!ids.length) {
    ElMessage.warning('请先选择要删除的数据')
    return
  }
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 张收货单吗？只有草稿状态可删。`, '提示', {
    type: 'warning'
  })
    .then(() => delReceipt(ids.join(',')))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

onMounted(getList)

/** 从卡片页返回时刷新（列表被 keep-alive 缓存，不会重新挂载） */
let activatedOnce = false
onActivated(() => {
  if (!activatedOnce) {
    activatedOnce = true
    return
  }
  getList()
})
</script>

<style scoped>
.clickable-table :deep(.el-table__row) {
  cursor: pointer;
}

.doc-link {
  color: var(--primary-color);
  font-weight: 500;
}

/* 包住按钮的 span，让 el-tooltip 在按钮禁用时仍能显示原因 */
.toolbar-item {
  display: inline-flex;
}
</style>
