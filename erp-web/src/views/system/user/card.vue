<template>
  <div v-loading="loading" class="app-container">
    <DocToolbar :actions="actions">
      <template #right>
        <el-button size="small" :icon="ArrowLeft" @click="goBack">返回列表</el-button>
      </template>
    </DocToolbar>

    <el-card class="bill-card" shadow="never">
      <el-form ref="formRef" :model="form" :rules="rules" size="small" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="登录账号" :disabled="!isNew" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickName">
              <el-input v-model="form.nickName" placeholder="用户昵称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-tree-select
                v-model="form.deptId"
                :data="deptOptions"
                :props="{ label: 'label', children: 'children' }"
                node-key="id"
                check-strictly
                :render-after-expand="false"
                clearable
                placeholder="请选择部门"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <DictSelect v-model="form.sex" type="sys_user_sex" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <DictSelect v-model="form.status" type="sys_normal_disable" />
            </el-form-item>
          </el-col>
          <el-col v-if="isNew" :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="初始密码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色分配">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option
                  v-for="role in roleOptions"
                  :key="role.id"
                  :label="role.roleName"
                  :value="role.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import {
  getUser,
  addUser,
  updateUser,
  getRoleOptionSelect,
  getDeptTreeSelect
} from '@/api/system/user'

/**
 * 用户卡片页。
 *
 * 与采购订单卡片同一套范式：列表点行进入，/new 表示新增。
 * 新增时填写初始密码；修改时不显示密码（走列表上的「重置密码」）。
 */
const route = useRoute()
const router = useRouter()
const listPath = '/system/user'
const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const roleOptions = ref([])
const deptOptions = ref([])

function emptyForm() {
  return {
    id: undefined,
    username: '',
    nickName: '',
    deptId: undefined,
    phone: '',
    email: '',
    sex: '2',
    status: '0',
    password: '',
    roleIds: [],
    remark: ''
  }
}

const form = reactive(emptyForm())

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度需在 2 - 50 之间', trigger: 'blur' }
  ],
  nickName: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['system:user:add'],
    visible: !isNew.value,
    onClick: () => router.push(`${listPath}/new`)
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: [isNew.value ? 'system:user:add' : 'system:user:edit'],
    onClick: handleSave
  }
])

function loadOptions() {
  getRoleOptionSelect()
    .then((data) => {
      roleOptions.value = data || []
    })
    .catch(() => {})
  getDeptTreeSelect()
    .then((data) => {
      deptOptions.value = data || []
    })
    .catch(() => {})
}

async function loadUser() {
  loading.value = true
  try {
    const data = await getUser(route.params.id)
    Object.assign(form, emptyForm(), data, { roleIds: data.roleIds || [] })
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  saving.value = true
  try {
    if (isNew.value) {
      await addUser({ ...form })
      ElMessage.success('新增成功')
      router.push(listPath)
    } else {
      const payload = { ...form }
      delete payload.password
      await updateUser(payload)
      ElMessage.success('保存成功')
      await loadUser()
    }
  } catch (e) {
    // 业务失败提示由 axios 拦截器统一弹出
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push(listPath)
}

onMounted(() => {
  loadOptions()
  if (!isNew.value) loadUser()
})
</script>
