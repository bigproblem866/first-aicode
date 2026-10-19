<template>
  <div class="app-container">
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="角色名称">
          <el-input
            v-model="queryParams.roleName"
            placeholder="请输入角色名称"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="角色标识">
          <el-input
            v-model="queryParams.roleKey"
            placeholder="请输入角色标识"
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
            v-hasPermi="['system:role:add']"
            size="small"
            type="primary"
            :icon="Plus"
            @click="handleAdd"
          >
            新增
          </el-button>
          <el-button
            v-hasPermi="['system:role:remove']"
            size="small"
            type="danger"
            :icon="Delete"
            :disabled="selectedIds.length === 0"
            @click="handleDelete()"
          >
            删除
          </el-button>
          <el-button
            v-hasPermi="['system:role:edit']"
            size="small"
            :disabled="selectedIds.length !== 1"
            @click="handleMenu()"
          >
            分配权限
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
        <el-table-column label="角色名称" prop="roleName" min-width="140" show-overflow-tooltip />
        <el-table-column label="角色标识" prop="roleKey" min-width="140" show-overflow-tooltip />
        <el-table-column label="排序" prop="roleSort" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" type="sys_normal_disable" />
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createTime" width="170" />
      </el-table>

      <Pagination
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <!-- 分配菜单权限 -->
    <el-dialog v-model="menuDialog.visible" title="分配菜单权限" width="520px" append-to-body>
      <div class="tree-toolbar">
        <el-checkbox v-model="treeExpandAll" @change="handleExpandAll">展开/折叠</el-checkbox>
        <el-checkbox v-model="treeCheckedAll" @change="handleCheckedAll">全选/全不选</el-checkbox>
      </div>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        :props="{ label: 'label', children: 'children' }"
        node-key="id"
        show-checkbox
        :default-expand-all="true"
        class="perm-tree"
      />
      <template #footer>
        <el-button @click="menuDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMenu">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete } from '@element-plus/icons-vue'
import {
  listRole,
  updateRole,
  delRole,
  getRoleMenuTreeSelect
} from '@/api/system/role'

/**
 * 角色列表。
 *
 * 点行进入角色卡片页编辑；「分配权限」需要针对具体角色，放在工具条、单行勾选时启用。
 */
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const showSearch = ref(true)
const total = ref(0)
const list = ref([])
const selectedIds = ref([])
const selectedRows = ref([])

const menuTreeRef = ref()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  roleName: undefined,
  roleKey: undefined,
  status: undefined
})

const menuDialog = reactive({ visible: false, roleId: undefined })
const menuTree = ref([])
const treeExpandAll = ref(true)
const treeCheckedAll = ref(false)

function getList() {
  loading.value = true
  listRole(queryParams)
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
  queryParams.roleName = undefined
  queryParams.roleKey = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
  selectedIds.value = rows.map((row) => row.id)
}

function handleAdd() {
  router.push('/system/role/new')
}

/** 点行进入卡片；勾选框不触发跳转 */
function handleRowClick(row, column) {
  if (column && column.type === 'selection') return
  router.push(`/system/role/${row.id}`)
}

function handleDelete() {
  const ids = selectedIds.value.map((id) => String(id))
  if (!ids.length) {
    ElMessage.warning('请先选择要删除的数据')
    return
  }
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条数据吗？`, '提示', { type: 'warning' })
    .then(() => delRole(ids.join(',')))
    .then(() => {
      ElMessage.success('删除成功')
      getList()
    })
    .catch(() => {})
}

/**
 * 打开权限分配弹窗（使用工具条勾选的那个角色）。
 * 后端返回 { menus: 菜单树, checkedKeys: 该角色已勾选的菜单 id }，
 * 这里兼容「直接返回数组」与「返回对象」两种形态。
 */
function handleMenu() {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先勾选一个角色')
    return
  }
  menuDialog.roleId = row.id
  menuDialog.visible = true
  treeCheckedAll.value = false
  getRoleMenuTreeSelect(row.id)
    .then((data) => {
      const tree = Array.isArray(data) ? data : (data && data.menus) || []
      const checked = (data && data.checkedKeys) || []
      menuTree.value = tree
      // 等 el-tree 用新数据渲染完再设置勾选，否则 setCheckedKeys 会打在旧节点上
      setTimeout(() => {
        if (menuTreeRef.value) {
          menuTreeRef.value.setCheckedKeys(checked.map((id) => String(id)), false)
        }
      }, 0)
    })
    .catch(() => {
      menuTree.value = []
    })
}

function handleExpandAll() {
  const nodes = menuTreeRef.value && menuTreeRef.value.store
  if (!nodes) return
  Object.values(nodes.nodesMap).forEach((node) => {
    node.expanded = treeExpandAll.value
  })
}

function handleCheckedAll() {
  if (!menuTreeRef.value) return
  if (treeCheckedAll.value) {
    const allKeys = []
    const walk = (nodes) => {
      for (const node of nodes || []) {
        allKeys.push(node.id)
        walk(node.children)
      }
    }
    walk(menuTree.value)
    menuTreeRef.value.setCheckedKeys(allKeys, false)
  } else {
    menuTreeRef.value.setCheckedKeys([], false)
  }
}

async function submitMenu() {
  if (!menuTreeRef.value) return
  const checked = menuTreeRef.value.getCheckedKeys()
  const halfChecked = menuTreeRef.value.getHalfCheckedKeys()
  const menuIds = [...halfChecked, ...checked]
  submitting.value = true
  try {
    // 菜单授权走角色的更新接口：SysRole 上带 menuIds 字段，后端一并处理关联表
    await updateRole({ id: menuDialog.roleId, menuIds })
    ElMessage.success('权限分配成功')
    menuDialog.visible = false
  } catch (e) {
    // 业务失败提示由 axios 拦截器统一弹出
  } finally {
    submitting.value = false
  }
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

.tree-toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.perm-tree {
  max-height: 420px;
  overflow: auto;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  padding: 8px;
}
</style>
