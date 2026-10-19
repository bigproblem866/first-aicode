<template>
  <div class="app-container">
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="单据号">
          <el-input
            v-model="queryParams.orderNo"
            placeholder="请输入单据号"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select
            v-model="queryParams.warehouseId"
            placeholder="请选择仓库"
            clearable
            filterable
            style="width: 150px"
          >
            <el-option
              v-for="wh in warehouseOptions"
              :key="wh.id"
              :label="wh.warehouseName"
              :value="wh.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="queryParams.status" type="inv_order_status" style="width: 120px" />
        </el-form-item>
        <el-form-item label="业务日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 230px"
          />
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
            v-hasPermi="['inv:order:add']"
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
                v-hasPermi="['inv:order:remove']"
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
        <el-table-column label="单据号" min-width="150">
          <template #default="{ row }">
            <span class="doc-link">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <DictTag :value="row.orderType" type="inv_order_type" />
          </template>
        </el-table-column>
        <el-table-column label="仓库" prop="warehouseName" min-width="140" show-overflow-tooltip />
        <el-table-column label="业务日期" prop="bizDate" width="120" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" type="inv_order_status" />
          </template>
        </el-table-column>
        <el-table-column label="来源单号" prop="sourceNo" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.sourceNo">{{ row.sourceNo }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="150" show-overflow-tooltip />
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
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { listOrder, delOrder } from '@/api/inventory/order'
import { getWarehouseOptionSelect } from '@/api/mdm/warehouse'

/**
 * 出入库单列表 —— 入库单与出库单共用同一个组件。
 *
 * 两者后端资源、字段、审核逻辑完全对称，菜单表里入库/出库都指向本文件
 * （component 都是 inventory/order/index），只有路由 path 不同（inbound / outbound）。
 * 所以这里不硬编码 IN/OUT，而是：
 *   1. 优先取路由 query 里的 orderType（若将来菜单支持带参数）；
 *   2. 否则按当前路由 path 判断（inbound → IN，outbound → OUT）。
 *
 * 交互：列表只负责浏览与进入。**点击一行进入单据卡片页面**
 * （/supply/inventory/inbound/:id 或 /supply/inventory/outbound/:id），
 * 编辑、审核、作废都在卡片页完成 —— 不再用弹窗。
 */
const route = useRoute()
const router = useRouter()

const orderType = computed(() => {
  const q = route.query.orderType
  if (q === 'IN' || q === 'OUT') return q
  return route.path.includes('outbound') ? 'OUT' : 'IN'
})

/** 列表与卡片页共用的基路径 */
const basePath = computed(() =>
  orderType.value === 'IN' ? '/supply/inventory/inbound' : '/supply/inventory/outbound'
)

const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const list = ref([])
const selectedIds = ref([])
const selectedRows = ref([])
const warehouseOptions = ref([])
const dateRange = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderType: 'IN',
  orderNo: undefined,
  warehouseId: undefined,
  status: undefined,
  bizDateStart: undefined,
  bizDateEnd: undefined
})

function getList() {
  loading.value = true
  listOrder(queryParams)
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
  queryParams.bizDateStart = dateRange.value && dateRange.value[0] ? dateRange.value[0] : undefined
  queryParams.bizDateEnd = dateRange.value && dateRange.value[1] ? dateRange.value[1] : undefined
  getList()
}

function resetQuery() {
  queryParams.orderNo = undefined
  queryParams.warehouseId = undefined
  queryParams.status = undefined
  dateRange.value = []
  handleQuery()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
  selectedIds.value = rows.map((row) => row.id)
}

/**
 * 删除按钮的置灰原因（空字符串表示可点）。
 *
 * 后端 deleteByIds 会逐条校验「必须是草稿」，混选一张非草稿整批失败；
 * 所以这里提前按状态拦住，避免用户点下去才看到报错。
 */
const deleteDisabledReason = computed(() => {
  if (!selectedRows.value.length) return '请先选择要删除的单据'
  if (selectedRows.value.some((row) => String(row.status) !== '0')) {
    return '只有草稿状态的单据可以删除'
  }
  return ''
})

function loadOptions() {
  getWarehouseOptionSelect()
    .then((data) => {
      warehouseOptions.value = data || []
    })
    .catch(() => {})
}

/** 新增：直接进入空白卡片页，保存后再回到列表 */
function handleAdd() {
  router.push(`${basePath.value}/new`)
}

/**
 * 点击行进入卡片页。
 * 勾选用的 selection 列要排除掉，否则"想勾选却跳走了"。
 */
function handleRowClick(row, column) {
  if (column && column.type === 'selection') return
  router.push(`${basePath.value}/${row.id}`)
}

function handleDelete() {
  const ids = selectedIds.value.map((id) => String(id))
  if (!ids.length) {
    ElMessage.warning('请先选择要删除的数据')
    return
  }
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 张单据吗？只有草稿状态可删。`, '提示', {
    type: 'warning'
  })
    .then(() => delOrder(ids.join(',')))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

// 路由 path 变了（入库单 ↔ 出库单），组件被 keep-alive 按 path 缓存，
// 回到本页时会重新挂载，这里用 watch 兜住"同一实例内切换"的情况
watch(
  orderType,
  (val) => {
    queryParams.orderType = val
    queryParams.pageNum = 1
    getList()
  },
  { immediate: true }
)

onMounted(loadOptions)

/**
 * 从卡片页返回时刷新列表。
 *
 * 列表被 keep-alive 按 path 缓存（见 AppMain.vue），返回时不会重新 mount；
 * 不刷新的后果是「在卡片页新增/审核/作废后回到列表，看到的还是旧数据」。
 * 首次激活不重复请求（挂载时 watch 已经拉过一次）。
 */
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
/* 整行可点：鼠标变成手型，单据号用主色提示可进入 */
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
