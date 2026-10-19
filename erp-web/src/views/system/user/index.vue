<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model="queryParams.username"
            placeholder="请输入用户名"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input
            v-model="queryParams.phone"
            placeholder="请输入手机号"
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
            v-hasPermi="['system:user:add']"
            size="small"
            type="primary"
            :icon="Plus"
            @click="handleAdd"
          >
            新增
          </el-button>
          <el-button
            v-hasPermi="['system:user:remove']"
            size="small"
            type="danger"
            :icon="Delete"
            :disabled="selectedIds.length === 0"
            @click="handleDelete()"
          >
            删除
          </el-button>
          <el-button
            v-hasPermi="['system:user:resetPwd']"
            size="small"
            :disabled="selectedIds.length !== 1"
            @click="handleResetPwd()"
          >
            重置密码
          </el-button>
          <el-button
            v-hasPermi="['system:user:edit']"
            size="small"
            :disabled="selectedIds.length !== 1"
            @click="handleAssignRole()"
          >
            分配角色
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
        <el-table-column label="用户名" prop="username" min-width="120" show-overflow-tooltip />
        <el-table-column label="昵称" prop="nickName" min-width="120" show-overflow-tooltip />
        <el-table-column label="部门" prop="deptName" min-width="140" show-overflow-tooltip />
        <el-table-column label="手机号" prop="phone" min-width="130" />
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <span v-if="row.roleNames && row.roleNames.length">{{ row.roleNames.join('、') }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" type="sys_normal_disable" />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="170" />
      </el-table>

      <Pagination
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdDialog.visible" title="重置密码" width="420px" append-to-body>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="用户">
          <span>{{ pwdDialog.nickName }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitResetPwd">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="roleDialog.visible" title="分配角色" width="460px" append-to-body>
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ roleDialog.nickName }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="roleDialog.roleIds"
            multiple
            placeholder="请选择角色"
            style="width: 100%"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.roleName"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssignRole">确 定</el-button>
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
  listUser,
  getUser,
  updateUser,
  delUser,
  resetUserPwd,
  getRoleOptionSelect
} from '@/api/system/user'

/**
 * 用户列表。
 *
 * 与采购订单一致：列表只负责浏览与勾选，点行进入卡片页编辑。
 * 「重置密码」需要知道具体用户，所以放在工具条、按单行勾选启用。
 */
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const showSearch = ref(true)
const total = ref(0)
const list = ref([])
const selectedIds = ref([])
const selectedRows = ref([])

const pwdFormRef = ref()
const roleOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  username: undefined,
  phone: undefined,
  status: undefined
})

const pwdDialog = reactive({ visible: false, nickName: '' })
const pwdForm = reactive({ userId: undefined, password: '' })

/** 分配角色弹窗：保存用户的完整数据 + 新的 roleIds */
const roleDialog = reactive({
  visible: false,
  nickName: '',
  roleIds: [],
  user: null
})

const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

function getList() {
  loading.value = true
  listUser(queryParams)
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
  queryParams.username = undefined
  queryParams.phone = undefined
  queryParams.status = undefined
  handleQuery()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
  selectedIds.value = rows.map((row) => row.id)
}

function handleAdd() {
  router.push('/system/user/new')
}

/** 点行进入卡片；勾选框不触发跳转 */
function handleRowClick(row, column) {
  if (column && column.type === 'selection') return
  router.push(`/system/user/${row.id}`)
}

function handleResetPwd() {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先勾选一个用户')
    return
  }
  pwdForm.userId = row.id
  pwdForm.password = ''
  pwdDialog.nickName = row.nickName || row.username
  pwdDialog.visible = true
  if (pwdFormRef.value) pwdFormRef.value.clearValidate()
}

async function submitResetPwd() {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    await resetUserPwd({ userId: pwdForm.userId, password: pwdForm.password })
    ElMessage.success('密码重置成功')
    pwdDialog.visible = false
  } catch (e) {
    // 业务失败提示由 axios 拦截器统一弹出
  } finally {
    submitting.value = false
  }
}

function loadRoleOptions() {
  if (roleOptions.value.length) return
  getRoleOptionSelect()
    .then((data) => {
      roleOptions.value = data || []
    })
    .catch(() => {})
}

/**
 * 分配角色。
 *
 * 必须先把用户完整数据拉回来再改 roleIds：
 * SysUser.roleIds 不是数据库字段，只传 { id, roleIds } 的话
 * MyBatis-Plus 的 updateById 没有任何 SET 列，会直接报错。
 */
function handleAssignRole() {
  const row = selectedRows.value[0]
  if (!row) {
    ElMessage.warning('请先勾选一个用户')
    return
  }
  roleDialog.nickName = row.nickName || row.username
  roleDialog.roleIds = []
  roleDialog.user = null
  roleDialog.visible = true

  loadRoleOptions()
  getUser(row.id)
    .then((data) => {
      roleDialog.user = data
      roleDialog.roleIds = (data.roleIds || []).slice()
    })
    .catch(() => {
      roleDialog.visible = false
    })
}

async function submitAssignRole() {
  if (!roleDialog.user) return
  submitting.value = true
  try {
    await updateUser({ ...roleDialog.user, roleIds: roleDialog.roleIds })
    ElMessage.success('角色分配成功，该用户重新登录后生效')
    roleDialog.visible = false
    getList()
  } catch (e) {
    // 业务失败提示由 axios 拦截器统一弹出
  } finally {
    submitting.value = false
  }
}

function handleDelete() {
  const ids = selectedIds.value.map((id) => String(id))
  if (!ids.length) {
    ElMessage.warning('请先选择要删除的数据')
    return
  }
  ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条数据吗？`, '提示', {
    type: 'warning'
  })
    .then(() => delUser(ids.join(',')))
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
