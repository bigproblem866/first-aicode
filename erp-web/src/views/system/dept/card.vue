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
          <el-col :span="24">
            <el-form-item label="上级部门">
              <el-tree-select
                v-model="form.parentId"
                :data="treeOptions"
                :props="{ label: 'label', children: 'children' }"
                node-key="id"
                check-strictly
                :render-after-expand="false"
                placeholder="不选则为顶级部门"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门名称" prop="deptName">
              <el-input v-model="form.deptName" placeholder="部门名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-input v-model="form.leader" placeholder="负责人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="邮箱" />
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
import { getDept, addDept, updateDept, getDeptTreeSelect } from '@/api/system/dept'

/** 部门卡片页。「新增下级」带 ?parentId= 过来预选上级部门。 */
const route = useRoute()
const router = useRouter()
const listPath = '/system/dept'
const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const treeOptions = ref([])

function emptyForm() {
  return {
    id: undefined,
    parentId: 0,
    deptName: '',
    orderNum: 0,
    leader: '',
    phone: '',
    email: '',
    status: '0',
    remark: ''
  }
}

const form = reactive(emptyForm())

const rules = {
  deptName: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入显示排序', trigger: 'blur' }]
}

const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['system:dept:add'],
    visible: !isNew.value,
    onClick: () => router.push(`${listPath}/new`)
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: [isNew.value ? 'system:dept:add' : 'system:dept:edit'],
    onClick: handleSave
  }
])

function loadTreeOptions() {
  getDeptTreeSelect()
    .then((data) => {
      treeOptions.value = [{ id: '0', label: '顶级部门', children: data || [] }]
    })
    .catch(() => {})
}

async function loadDept() {
  loading.value = true
  try {
    const data = await getDept(route.params.id)
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
      await addDept({ ...form })
      ElMessage.success('新增成功')
      router.push(listPath)
    } else {
      await updateDept({ ...form })
      ElMessage.success('保存成功')
      await loadDept()
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
  loadTreeOptions()
  if (isNew.value) {
    const parentId = route.query.parentId
    if (parentId) form.parentId = parentId
  } else {
    loadDept()
  }
})
</script>
