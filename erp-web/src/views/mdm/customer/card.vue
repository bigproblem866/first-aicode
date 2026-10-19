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
            <el-form-item label="客户编码" prop="customerCode">
              <el-input v-model="form.customerCode" placeholder="唯一编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="form.customerName" placeholder="客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户类型">
              <DictSelect v-model="form.customerType" type="mdm_customer_type" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contact" placeholder="联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="form.phone" placeholder="联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税号">
              <el-input v-model="form.taxNo" placeholder="税号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="信用额度">
              <el-input-number
                v-model="form.creditLimit"
                :min="0"
                :precision="4"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="地址">
              <el-input v-model="form.address" placeholder="地址" />
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
import { getCustomer, addCustomer, updateCustomer } from '@/api/mdm/customer'

/** 客户卡片页。列表点行进入，/new 表示新增。 */
const route = useRoute()
const router = useRouter()
const listPath = '/mdm/customer'
const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const formRef = ref()

function emptyForm() {
  return {
    id: undefined,
    customerCode: '',
    customerName: '',
    customerType: undefined,
    contact: '',
    phone: '',
    email: '',
    address: '',
    taxNo: '',
    creditLimit: 0,
    status: '0',
    remark: ''
  }
}

const form = reactive(emptyForm())

const rules = {
  customerCode: [{ required: true, message: '请输入客户编码', trigger: 'blur' }],
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['mdm:customer:add'],
    visible: !isNew.value,
    onClick: () => router.push(`${listPath}/new`)
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: [isNew.value ? 'mdm:customer:add' : 'mdm:customer:edit'],
    onClick: handleSave
  }
])

async function loadCustomer() {
  loading.value = true
  try {
    const data = await getCustomer(route.params.id)
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
      await addCustomer({ ...form })
      ElMessage.success('新增成功')
      router.push(listPath)
    } else {
      await updateCustomer({ ...form })
      ElMessage.success('保存成功')
      await loadCustomer()
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
  if (!isNew.value) loadCustomer()
})
</script>
