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
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="form.parentId"
                :data="parentOptions"
                :props="{ label: 'label', children: 'children' }"
                node-key="id"
                check-strictly
                :render-after-expand="false"
                placeholder="不选则为顶级菜单"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单类型" prop="menuType">
              <el-radio-group v-model="form.menuType">
                <el-radio-button value="M">目录</el-radio-button>
                <el-radio-button value="C">菜单</el-radio-button>
                <el-radio-button value="F">按钮</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" :min="0" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'F'" :span="12">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="form.path" placeholder="如 system、user" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType === 'C'" :span="24">
            <el-form-item label="组件路径" prop="component">
              <el-input v-model="form.component" placeholder="相对 views/，如 system/user/index" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'M'" :span="12">
            <el-form-item label="权限标识" prop="perms">
              <el-input v-model="form.perms" placeholder="如 system:user:add" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'F'" :span="12">
            <el-form-item label="菜单图标">
              <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 Setting" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType !== 'F'" :span="12">
            <el-form-item label="显示状态">
              <el-radio-group v-model="form.visible">
                <el-radio value="0">显示</el-radio>
                <el-radio value="1">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单状态">
              <DictSelect v-model="form.status" type="sys_normal_disable" />
            </el-form-item>
          </el-col>
          <el-col v-if="form.menuType === 'C'" :span="12">
            <el-form-item label="是否缓存">
              <el-radio-group v-model="form.isCache">
                <el-radio :value="0">缓存</el-radio>
                <el-radio :value="1">不缓存</el-radio>
              </el-radio-group>
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
import { getMenu, addMenu, updateMenu, getMenuTreeSelect } from '@/api/system/menu'

/**
 * 菜单卡片页。
 *
 * 「新增下级」从列表工具条触发，带 ?parentId=&parentType= 过来：
 * 父级是目录 → 默认建菜单(C)；父级是菜单/按钮 → 默认建按钮(F)。
 */
const route = useRoute()
const router = useRouter()
const listPath = '/system/menu'
const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const formRef = ref()
const parentOptions = ref([])

function emptyForm() {
  return {
    id: undefined,
    parentId: 0,
    menuName: '',
    menuType: 'C',
    orderNum: 0,
    path: '',
    component: '',
    perms: '',
    icon: '',
    visible: '0',
    status: '0',
    isFrame: 0,
    isCache: 0,
    remark: ''
  }
}

const form = reactive(emptyForm())

const rules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  orderNum: [{ required: true, message: '请输入显示排序', trigger: 'blur' }]
}

const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['system:menu:add'],
    visible: !isNew.value,
    onClick: () => router.push(`${listPath}/new`)
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: [isNew.value ? 'system:menu:add' : 'system:menu:edit'],
    onClick: handleSave
  }
])

function loadParentOptions() {
  getMenuTreeSelect()
    .then((data) => {
      parentOptions.value = [{ id: '0', label: '主目录（顶级）', children: data || [] }]
    })
    .catch(() => {})
}

async function loadMenu() {
  loading.value = true
  try {
    const data = await getMenu(route.params.id)
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
      await addMenu({ ...form })
      ElMessage.success('新增成功')
      router.push(listPath)
    } else {
      await updateMenu({ ...form })
      ElMessage.success('保存成功')
      await loadMenu()
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
  loadParentOptions()
  if (isNew.value) {
    const parentId = route.query.parentId
    if (parentId) {
      form.parentId = parentId
      form.menuType = route.query.parentType === 'M' ? 'C' : 'F'
    }
  } else {
    loadMenu()
  }
})
</script>
