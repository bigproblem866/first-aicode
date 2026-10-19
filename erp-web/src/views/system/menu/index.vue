<template>
  <div class="app-container">
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="菜单名称">
          <el-input
            v-model="queryParams.menuName"
            placeholder="请输入菜单名称"
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
            v-hasPermi="['system:menu:add']"
            size="small"
            type="primary"
            :icon="Plus"
            @click="handleAdd"
          >
            新增
          </el-button>
          <el-button
            v-hasPermi="['system:menu:remove']"
            size="small"
            type="danger"
            :icon="Delete"
            :disabled="selectedIds.length === 0"
            @click="handleDelete()"
          >
            删除
          </el-button>
          <el-button
            v-hasPermi="['system:menu:add']"
            size="small"
            :disabled="selectedIds.length !== 1"
            @click="handleAddChild()"
          >
            新增下级
          </el-button>
        </div>
        <RightToolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        class="clickable-table"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        default-expand-all
        @row-click="handleRowClick"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" align="center" />
        <el-table-column label="菜单名称" prop="menuName" min-width="180" show-overflow-tooltip />
        <el-table-column label="图标" width="70" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="menuTypeTag(row.menuType)" size="small" disable-transitions>
              {{ menuTypeText(row.menuType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="orderNum" width="70" align="center" />
        <el-table-column label="权限标识" prop="perms" min-width="180" show-overflow-tooltip />
        <el-table-column label="组件路径" prop="component" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" type="sys_normal_disable" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import { listMenu, delMenu } from '@/api/system/menu'

/**
 * 菜单列表（树形）。
 *
 * 点行进入菜单卡片页；「新增下级」需要知道父节点，所以放在工具条、
 * 单行勾选时启用，并把 parentId / parentType 带到卡片页。
 */
const router = useRouter()

const loading = ref(false)
const showSearch = ref(true)
const list = ref([])
const selectedIds = ref([])
const selectedRows = ref([])

const queryParams = reactive({
  menuName: undefined,
  status: undefined
})

function menuTypeText(type) {
  return { M: '目录', C: '菜单', F: '按钮' }[type] || type
}

function menuTypeTag(type) {
  return { M: 'warning', C: 'primary', F: 'info' }[type] || 'info'
}

function getList() {
  loading.value = true
  listMenu(queryParams)
    .then((data) => {
      list.value = data || []
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  getList()
}

function resetQuery() {
  queryParams.menuName = undefined
  queryParams.status = undefined
  getList()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
  selectedIds.value = rows.map((row) => row.id)
}

function handleAdd() {
  router.push('/system/menu/new')
}

/** 新增下级：把父节点与类型带给卡片页，决定默认菜单类型 */
function handleAddChild() {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先勾选一个上级菜单')
    return
  }
  router.push(`/system/menu/new?parentId=${row.id}&parentType=${row.menuType}`)
}

/** 点行进入卡片；勾选框与树形展开箭头都不触发跳转 */
function handleRowClick(row, column, event) {
  if (column && column.type === 'selection') return
  if (event && event.target && event.target.closest('.el-table__expand-icon')) return
  router.push(`/system/menu/${row.id}`)
}

function handleDelete() {
  const ids = selectedIds.value.map((id) => String(id))
  if (!ids.length) {
    ElMessage.warning('请先选择要删除的数据')
    return
  }
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个菜单吗？`, '提示', { type: 'warning' })
    .then(() => Promise.all(ids.map((id) => delMenu(id))))
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
