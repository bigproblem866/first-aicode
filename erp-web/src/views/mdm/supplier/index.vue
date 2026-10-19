<template>
  <div class="app-container">
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="供应商编码">
          <el-input
            v-model="queryParams.supplierCode"
            placeholder="请输入供应商编码"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="供应商名称">
          <el-input
            v-model="queryParams.supplierName"
            placeholder="请输入供应商名称"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
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
            v-hasPermi="['mdm:supplier:add']"
            size="small"
            type="primary"
            :icon="Plus"
            @click="handleAdd"
          >
            新增
          </el-button>
          <el-button
            v-hasPermi="['mdm:supplier:remove']"
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
        <el-table-column label="供应商编码" prop="supplierCode" min-width="140" show-overflow-tooltip />
        <el-table-column label="供应商名称" prop="supplierName" min-width="200" show-overflow-tooltip />
        <el-table-column label="联系人" prop="contact" width="110" />
        <el-table-column label="联系电话" prop="phone" width="140" />
        <el-table-column label="税号" prop="taxNo" min-width="140" show-overflow-tooltip />
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
import { listSupplier, delSupplier } from '@/api/mdm/supplier'

/** 供应商列表。列表点行进入供应商卡片页编辑。 */
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
  supplierCode: undefined,
  supplierName: undefined,
  status: undefined
})

function getList() {
  loading.value = true
  listSupplier(queryParams)
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
  queryParams.supplierCode = undefined
  queryParams.supplierName = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
  selectedIds.value = rows.map((row) => row.id)
}

function handleAdd() {
  router.push('/mdm/supplier/new')
}

/** 点行进入卡片；勾选框不触发跳转 */
function handleRowClick(row, column) {
  if (column && column.type === 'selection') return
  router.push(`/mdm/supplier/${row.id}`)
}

function handleDelete() {
  const ids = selectedIds.value.map((id) => String(id))
  if (!ids.length) {
    ElMessage.warning('请先选择要删除的数据')
    return
  }
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条数据吗？`, '提示', { type: 'warning' })
    .then(() => delSupplier(ids.join(',')))
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
