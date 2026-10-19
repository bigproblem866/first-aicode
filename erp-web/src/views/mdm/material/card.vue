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
            <el-form-item label="物料编码" prop="materialCode">
              <el-input v-model="form.materialCode" placeholder="唯一编码，如 M001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料名称" prop="materialName">
              <el-input v-model="form.materialName" placeholder="物料名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料类型">
              <DictSelect v-model="form.materialType" type="mdm_material_type" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料分类">
              <el-tree-select
                v-model="form.categoryId"
                :data="categoryOptions"
                :props="{ label: 'label', children: 'children' }"
                node-key="id"
                check-strictly
                :render-after-expand="false"
                clearable
                placeholder="请选择物料分类"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位">
              <DictSelect v-model="form.unit" type="mdm_unit" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="条形码">
              <el-input v-model="form.barcode" placeholder="条形码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格">
              <el-input v-model="form.spec" placeholder="规格" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="型号">
              <el-input v-model="form.model" placeholder="型号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌">
              <el-input v-model="form.brand" placeholder="品牌" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安全库存">
              <el-input-number
                v-model="form.safetyStock"
                :min="0"
                :precision="4"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参考采购价">
              <el-input-number
                v-model="form.purchasePrice"
                :min="0"
                :precision="4"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="参考销售价">
              <el-input-number
                v-model="form.salePrice"
                :min="0"
                :precision="4"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税率">
              <el-input-number
                v-model="form.taxRate"
                :min="0"
                :max="1"
                :step="0.01"
                :precision="4"
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
import { getMaterial, addMaterial, updateMaterial } from '@/api/mdm/material'
import { getMaterialCategoryTreeSelect } from '@/api/mdm/materialCategory'

/** 物料卡片页。列表点行进入，/new 表示新增。 */
const route = useRoute()
const router = useRouter()
const listPath = '/mdm/material'
const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const categoryOptions = ref([])

function emptyForm() {
  return {
    id: undefined,
    materialCode: '',
    materialName: '',
    materialType: undefined,
    categoryId: undefined,
    unit: undefined,
    spec: '',
    model: '',
    brand: '',
    barcode: '',
    safetyStock: 0,
    purchasePrice: 0,
    salePrice: 0,
    taxRate: 0,
    status: '0',
    remark: ''
  }
}

const form = reactive(emptyForm())

const rules = {
  materialCode: [
    { required: true, message: '请输入物料编码', trigger: 'blur' },
    { max: 50, message: '长度不能超过 50', trigger: 'blur' }
  ],
  materialName: [
    { required: true, message: '请输入物料名称', trigger: 'blur' },
    { max: 200, message: '长度不能超过 200', trigger: 'blur' }
  ]
}

const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['mdm:material:add'],
    visible: !isNew.value,
    onClick: () => router.push(`${listPath}/new`)
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: [isNew.value ? 'mdm:material:add' : 'mdm:material:edit'],
    onClick: handleSave
  }
])

function loadCategoryOptions() {
  getMaterialCategoryTreeSelect()
    .then((data) => {
      categoryOptions.value = data || []
    })
    .catch(() => {})
}

async function loadMaterial() {
  loading.value = true
  try {
    const data = await getMaterial(route.params.id)
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
      await addMaterial({ ...form })
      ElMessage.success('新增成功')
      router.push(listPath)
    } else {
      await updateMaterial({ ...form })
      ElMessage.success('保存成功')
      await loadMaterial()
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
  loadCategoryOptions()
  if (!isNew.value) loadMaterial()
})
</script>
