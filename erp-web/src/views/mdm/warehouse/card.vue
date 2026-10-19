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
            <el-form-item label="仓库编码" prop="warehouseCode">
              <el-input v-model="form.warehouseCode" placeholder="唯一编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓库名称" prop="warehouseName">
              <el-input v-model="form.warehouseName" placeholder="仓库名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="仓管员">
              <el-input v-model="form.manager" placeholder="仓管员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="仓库地址">
              <el-input v-model="form.address" placeholder="仓库地址" />
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
import { getWarehouse, addWarehouse, updateWarehouse } from '@/api/mdm/warehouse'

/** 仓库卡片页。仓库菜单挂在库存管理下，列表路径是 /supply/inventory/warehouse。 */
const route = useRoute()
const router = useRouter()
const listPath = '/supply/inventory/warehouse'
const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const formRef = ref()

function emptyForm() {
  return {
    id: undefined,
    warehouseCode: '',
    warehouseName: '',
    address: '',
    manager: '',
    phone: '',
    status: '0',
    remark: ''
  }
}

const form = reactive(emptyForm())

const rules = {
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }]
}

const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['mdm:warehouse:add'],
    visible: !isNew.value,
    onClick: () => router.push(`${listPath}/new`)
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: [isNew.value ? 'mdm:warehouse:add' : 'mdm:warehouse:edit'],
    onClick: handleSave
  }
])

async function loadWarehouse() {
  loading.value = true
  try {
    const data = await getWarehouse(route.params.id)
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
      await addWarehouse({ ...form })
      ElMessage.success('新增成功')
      router.push(listPath)
    } else {
      await updateWarehouse({ ...form })
      ElMessage.success('保存成功')
      await loadWarehouse()
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
  if (!isNew.value) loadWarehouse()
})
</script>
