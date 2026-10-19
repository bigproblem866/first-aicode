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
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="如：仓库管理员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色标识" prop="roleKey">
              <el-input v-model="form.roleKey" placeholder="如：warehouse_keeper" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示顺序" prop="roleSort">
              <el-input-number
                v-model="form.roleSort"
                :min="0"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <DictSelect v-model="form.status" type="sys_normal_disable" />
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
import { getRole, addRole, updateRole } from '@/api/system/role'

/** 角色卡片页。菜单权限分配仍从列表工具条触发（需要选中一行）。 */
const route = useRoute()
const router = useRouter()
const listPath = '/system/role'
const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const formRef = ref()

function emptyForm() {
  return {
    id: undefined,
    roleName: '',
    roleKey: '',
    roleSort: 0,
    status: '0',
    remark: ''
  }
}

const form = reactive(emptyForm())

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }]
}

const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['system:role:add'],
    visible: !isNew.value,
    onClick: () => router.push(`${listPath}/new`)
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: [isNew.value ? 'system:role:add' : 'system:role:edit'],
    onClick: handleSave
  }
])

async function loadRole() {
  loading.value = true
  try {
    const data = await getRole(route.params.id)
    Object.assign(form, emptyForm(), data)
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
      await addRole({ ...form })
      ElMessage.success('新增成功')
      router.push(listPath)
    } else {
      await updateRole({ ...form })
      ElMessage.success('保存成功')
      await loadRole()
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
  if (!isNew.value) loadRole()
})
</script>
