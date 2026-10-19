<template>
  <div class="app-container">
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="订单号">
          <el-input
            v-model="queryParams.orderNo"
            placeholder="请输入订单号"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select
            v-model="queryParams.supplierId"
            placeholder="请选择供应商"
            clearable
            filterable
            style="width: 160px"
          >
            <el-option
              v-for="sp in supplierOptions"
              :key="sp.id"
              :label="sp.supplierName"
              :value="sp.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="queryParams.status" type="pur_order_status" style="width: 120px" />
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
            v-hasPermi="['pur:order:add']"
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
                v-hasPermi="['pur:order:remove']"
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
        <el-table-column label="订单号" min-width="150">
          <template #default="{ row }">
            <span class="doc-link">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="供应商" prop="supplierName" min-width="180" show-overflow-tooltip />
        <el-table-column label="下单日期" prop="orderDate" width="120" align="center" />
        <el-table-column label="期望到货" prop="expectDate" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.expectDate">{{ row.expectDate }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="总金额" width="130" align="right">
          <template #default="{ row }">
            <span class="cell-amount">{{ formatAmount(row.totalAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" type="pur_order_status" />
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
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
import { listPurOrder, delPurOrder } from '@/api/purchase/order'
import { getSupplierOptionSelect } from '@/api/mdm/supplier'
import { formatAmount } from '@/utils/format'

/**
 * 采购订单列表。
 *
 * 与出入库单一致：列表只负责浏览与进入，**点击一行进入订单卡片页**
 * （/supply/purchase/order/:id），编辑、审核、作废都在卡片页完成。
 */
const router = useRouter()

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const list = ref([])
const selectedIds = ref([])
const selectedRows = ref([])
const supplierOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: undefined,
  supplierId: undefined,
  status: undefined
})

function getList() {
  loading.value = true
  listPurOrder(queryParams)
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
  queryParams.orderNo = undefined
  queryParams.supplierId = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
  selectedIds.value = rows.map((row) => row.id)
}

/** 删除按钮的置灰原因（空字符串表示可点）：后端只允许草稿整批删除 */
const deleteDisabledReason = computed(() => {
  if (!selectedRows.value.length) return '请先选择要删除的单据'
  if (selectedRows.value.some((row) => String(row.status) !== '0')) {
    return '只有草稿状态的订单可以删除'
  }
  return ''
})

function loadOptions() {
  getSupplierOptionSelect()
    .then((data) => {
      supplierOptions.value = data || []
    })
    .catch(() => {})
}

function handleAdd() {
  router.push('/supply/purchase/order/new')
}

/** 点击行进入订单卡片；勾选用的 selection 列排除掉 */
function handleRowClick(row, column) {
  if (column && column.type === 'selection') return
  router.push(`/supply/purchase/order/${row.id}`)
}

function handleDelete() {
  const ids = selectedIds.value.map((id) => String(id))
  if (!ids.length) {
    ElMessage.warning('请先选择要删除的数据')
    return
  }
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 张订单吗？只有草稿状态可删。`, '提示', {
    type: 'warning'
  })
    .then(() => delPurOrder(ids.join(',')))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

onMounted(() => {
  loadOptions()
  getList()
})

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
