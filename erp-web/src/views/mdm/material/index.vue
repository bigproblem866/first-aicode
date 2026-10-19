<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="物料编码">
          <el-input
            v-model="queryParams.materialCode"
            placeholder="请输入物料编码"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input
            v-model="queryParams.materialName"
            placeholder="请输入物料名称"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="物料类型">
          <DictSelect
            v-model="queryParams.materialType"
            type="mdm_material_type"
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="queryParams.status" type="sys_normal_disable" style="width: 120px" />
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
            v-hasPermi="['mdm:material:add']"
            size="small"
            type="primary"
            :icon="Plus"
            @click="handleAdd"
          >
            新增
          </el-button>
          <el-button
            v-hasPermi="['mdm:material:remove']"
            size="small"
            type="danger"
            :icon="Delete"
            :disabled="selectedIds.length === 0"
            @click="handleDelete()"
          >
            删除
          </el-button>
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
        <el-table-column label="物料编码" prop="materialCode" min-width="130" show-overflow-tooltip />
        <el-table-column label="物料名称" prop="materialName" min-width="160" show-overflow-tooltip />
        <el-table-column label="物料类型" width="110" align="center">
          <template #default="{ row }">
            <DictTag :value="row.materialType" type="mdm_material_type" />
          </template>
        </el-table-column>
        <el-table-column label="分类" prop="categoryName" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.categoryName">{{ row.categoryName }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="规格" prop="spec" min-width="120" show-overflow-tooltip />
        <el-table-column label="型号" prop="model" min-width="110" show-overflow-tooltip />
        <el-table-column label="单位" width="80" align="center">
          <template #default="{ row }">
            <DictTag :value="row.unit" type="mdm_unit" />
          </template>
        </el-table-column>
        <el-table-column label="安全库存" width="110" align="right">
          <template #default="{ row }">{{ formatQty(row.safetyStock) }}</template>
        </el-table-column>
        <el-table-column label="参考采购价" width="120" align="right">
          <template #default="{ row }">
            <span class="cell-amount">{{ formatAmount(row.purchasePrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="参考销售价" width="120" align="right">
          <template #default="{ row }">
            <span class="cell-amount">{{ formatAmount(row.salePrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="税率" width="90" align="right">
          <template #default="{ row }">{{ formatTaxRate(row.taxRate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" type="sys_normal_disable" />
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
import { onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { listMaterial, delMaterial } from '@/api/mdm/material'
import { formatAmount, formatQty, formatTaxRate } from '@/utils/format'

/**
 * 物料列表。
 *
 * 列表点行进入物料卡片页编辑；新增/删除在工具条。
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
  materialCode: undefined,
  materialName: undefined,
  materialType: undefined,
  status: undefined
})

function getList() {
  loading.value = true
  listMaterial(queryParams)
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
  queryParams.materialCode = undefined
  queryParams.materialName = undefined
  queryParams.materialType = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
  selectedIds.value = rows.map((row) => row.id)
}

function handleAdd() {
  router.push('/mdm/material/new')
}

/** 点行进入卡片；勾选框不触发跳转 */
function handleRowClick(row, column) {
  if (column && column.type === 'selection') return
  router.push(`/mdm/material/${row.id}`)
}

function handleDelete() {
  const ids = selectedIds.value.map((id) => String(id))
  if (!ids.length) {
    ElMessage.warning('请先选择要删除的数据')
    return
  }
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条数据吗？`, '提示', { type: 'warning' })
    .then(() => delMaterial(ids.join(',')))
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
</style>
