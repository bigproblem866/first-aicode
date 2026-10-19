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
            <el-form-item label="上级分类">
              <el-tree-select
                v-model="form.parentId"
                :data="treeOptions"
                :props="{ label: 'label', children: 'children' }"
                node-key="id"
                check-strictly
                :render-after-expand="false"
                placeholder="不选则为顶级分类"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类编码" prop="categoryCode">
              <el-input v-model="form.categoryCode" placeholder="唯一编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类名称" prop="categoryName">
              <el-input v-model="form.categoryName" placeholder="分类名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" controls-position="right" />
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
import {
  getMaterialCategory,
  addMaterialCategory,
  updateMaterialCategory,
  getMaterialCategoryTreeSelect
} from '@/api/mdm/materialCategory'

/** 物料分类卡片页。「新增下级」带 ?parentId= 过来预选上级分类。 */
const route = useRoute()
const router = useRouter()
const listPath = '/mdm/materialCategory'
const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const treeOptions = ref([])

function emptyForm() {
  return {
    id: undefined,
    parentId: 0,
    categoryCode: '',
    categoryName: '',
    orderNum: 0,
    status: '0',
    remark: ''
  }
}

const form = reactive(emptyForm())

const rules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['mdm:materialCategory:add'],
    visible: !isNew.value,
    onClick: () => router.push(`${listPath}/new`)
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: [
      isNew.value ? 'mdm:materialCategory:add' : 'mdm:materialCategory:edit'
    ],
    onClick: handleSave
  }
])

function loadTreeOptions() {
  getMaterialCategoryTreeSelect()
    .then((data) => {
      treeOptions.value = [{ id: '0', label: '顶级分类', children: data || [] }]
    })
    .catch(() => {})
}

async function loadCategory() {
  loading.value = true
  try {
    const data = await getMaterialCategory(route.params.id)
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
      await addMaterialCategory({ ...form })
      ElMessage.success('新增成功')
      router.push(listPath)
    } else {
      await updateMaterialCategory({ ...form })
      ElMessage.success('保存成功')
      await loadCategory()
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
    loadCategory()
  }
})
</script>
