<template>
  <div class="app-container dict-page">
    <!-- 左：字典类型 -->
    <el-card class="dict-left" shadow="never">
      <template #header>
        <div class="card-header">
          <span>字典类型</span>
          <el-button v-hasPermi="['system:dict:add']" type="primary" link :icon="Plus" @click="handleAddType">
            新增
          </el-button>
        </div>
      </template>

      <el-form :model="typeQuery" size="small" inline @submit.prevent>
        <el-form-item label="">
          <el-input
            v-model="typeQuery.dictName"
            placeholder="字典名称"
            clearable
            style="width: 130px"
            @keyup.enter="getTypeList"
          />
        </el-form-item>
        <el-form-item>
          <el-button size="small" type="primary" :icon="Search" @click="getTypeList">搜索</el-button>
        </el-form-item>
      </el-form>

      <el-table
        v-loading="typeLoading"
        :data="typeList"
        highlight-current-row
        :current-row-key="currentType.dictType"
        row-key="dictType"
        height="520"
        @current-change="handleTypeChange"
      >
        <el-table-column label="字典名称" prop="dictName" min-width="110" show-overflow-tooltip />
        <el-table-column label="字典类型" prop="dictType" min-width="130" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" type="sys_normal_disable" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button v-hasPermi="['system:dict:edit']" link type="primary" @click.stop="handleUpdateType(row)">
              修改
            </el-button>
            <el-button v-hasPermi="['system:dict:remove']" link type="danger" @click.stop="handleDeleteType(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 右：字典数据 -->
    <el-card class="dict-right" shadow="never">
      <template #header>
        <div class="card-header">
          <span>
            字典数据
            <el-tag v-if="currentType.dictType" size="small" type="info" class="ml-8">
              {{ currentType.dictType }}
            </el-tag>
          </span>
          <el-button
            v-hasPermi="['system:dict:add']"
            type="primary"
            link
            :icon="Plus"
            :disabled="!currentType.dictType"
            @click="handleAddData"
          >
            新增
          </el-button>
        </div>
      </template>

      <el-table v-loading="dataLoading" :data="dataList" height="560">
        <el-table-column label="字典标签" prop="dictLabel" min-width="110" show-overflow-tooltip />
        <el-table-column label="字典键值" prop="dictValue" min-width="100" show-overflow-tooltip />
        <el-table-column label="排序" prop="dictSort" width="70" align="center" />
        <el-table-column label="回显样式" prop="listClass" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.listClass">{{ row.listClass }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <DictTag :value="row.status" type="sys_normal_disable" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button v-hasPermi="['system:dict:edit']" link type="primary" @click="handleUpdateData(row)">
              修改
            </el-button>
            <el-button v-hasPermi="['system:dict:remove']" link type="danger" @click="handleDeleteData(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 字典类型表单 -->
    <el-dialog v-model="typeDialog.visible" :title="typeDialog.title" width="520px" append-to-body>
      <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="90px">
        <el-form-item label="字典名称" prop="dictName">
          <el-input v-model="typeForm.dictName" placeholder="如：计量单位" />
        </el-form-item>
        <el-form-item label="字典类型" prop="dictType">
          <el-input v-model="typeForm.dictType" placeholder="如：mdm_unit" />
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="typeForm.status" type="sys_normal_disable" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="typeForm.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitType">确 定</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据表单 -->
    <el-dialog v-model="dataDialog.visible" :title="dataDialog.title" width="560px" append-to-body>
      <el-form ref="dataFormRef" :model="dataForm" :rules="dataRules" label-width="90px">
        <el-form-item label="字典类型">
          <el-input v-model="dataForm.dictType" disabled />
        </el-form-item>
        <el-form-item label="字典标签" prop="dictLabel">
          <el-input v-model="dataForm.dictLabel" placeholder="界面显示的文字" />
        </el-form-item>
        <el-form-item label="字典键值" prop="dictValue">
          <el-input v-model="dataForm.dictValue" placeholder="存进业务表的值" />
        </el-form-item>
        <el-form-item label="显示排序" prop="dictSort">
          <el-input-number v-model="dataForm.dictSort" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="回显样式">
          <el-select v-model="dataForm.listClass" clearable placeholder="el-tag 类型" style="width: 100%">
            <el-option label="primary" value="primary" />
            <el-option label="success" value="success" />
            <el-option label="info" value="info" />
            <el-option label="warning" value="warning" />
            <el-option label="danger" value="danger" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <DictSelect v-model="dataForm.status" type="sys_normal_disable" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dataForm.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataDialog.visible = false">取 消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitData">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import {
  listDictType,
  addDictType,
  updateDictType,
  delDictType,
  listDictData,
  addDictData,
  updateDictData,
  delDictData
} from '@/api/system/dict'
import useDictStore from '@/store/modules/dict'

/**
 * 字典管理：左类型、右数据。
 * 改动字典后要清掉 Pinia 缓存，否则其它页面还在用旧字典渲染。
 */
const dictStore = useDictStore()

const typeLoading = ref(false)
const dataLoading = ref(false)
const submitting = ref(false)
const typeList = ref([])
const dataList = ref([])
const currentType = reactive({ dictType: '' })

const typeFormRef = ref()
const dataFormRef = ref()

const typeQuery = reactive({ dictName: undefined })
const typeDialog = reactive({ visible: false, title: '' })
const dataDialog = reactive({ visible: false, title: '' })
const isEditType = ref(false)
const isEditData = ref(false)

function emptyType() {
  return { id: undefined, dictName: '', dictType: '', status: '0', remark: '' }
}
function emptyData() {
  return {
    id: undefined,
    dictType: '',
    dictLabel: '',
    dictValue: '',
    dictSort: 0,
    listClass: '',
    status: '0',
    remark: ''
  }
}

const typeForm = reactive(emptyType())
const dataForm = reactive(emptyData())

const typeRules = {
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
  dictType: [{ required: true, message: '请输入字典类型', trigger: 'blur' }]
}
const dataRules = {
  dictLabel: [{ required: true, message: '请输入字典标签', trigger: 'blur' }],
  dictValue: [{ required: true, message: '请输入字典键值', trigger: 'blur' }],
  dictSort: [{ required: true, message: '请输入显示排序', trigger: 'blur' }]
}

function getTypeList() {
  typeLoading.value = true
  listDictType({ pageNum: 1, pageSize: 200, ...typeQuery })
    .then((res) => {
      // 类型列表既可能是分页结构，也可能直接返回数组，两种都兼容
      typeList.value = (res && res.rows) || (Array.isArray(res) ? res : [])
      if (!currentType.dictType && typeList.value.length) {
        handleTypeChange(typeList.value[0])
      }
    })
    .finally(() => {
      typeLoading.value = false
    })
}

function getDataList() {
  if (!currentType.dictType) {
    dataList.value = []
    return
  }
  dataLoading.value = true
  listDictData({ pageNum: 1, pageSize: 200, dictType: currentType.dictType })
    .then((res) => {
      dataList.value = (res && res.rows) || (Array.isArray(res) ? res : [])
    })
    .finally(() => {
      dataLoading.value = false
    })
}

function handleTypeChange(row) {
  if (!row) return
  currentType.dictType = row.dictType
  getDataList()
}

// ---- 字典类型 CRUD ----
function handleAddType() {
  Object.assign(typeForm, emptyType())
  isEditType.value = false
  typeDialog.title = '新增字典类型'
  typeDialog.visible = true
}

function handleUpdateType(row) {
  Object.assign(typeForm, emptyType(), row)
  isEditType.value = true
  typeDialog.title = '修改字典类型'
  typeDialog.visible = true
}

async function submitType() {
  if (!typeFormRef.value) return
  try {
    await typeFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    if (isEditType.value) {
      await updateDictType({ ...typeForm })
      ElMessage.success('修改成功')
    } else {
      await addDictType({ ...typeForm })
      ElMessage.success('新增成功')
    }
    typeDialog.visible = false
    dictStore.clearDict()
    getTypeList()
  } catch (e) {
    // 忽略
  } finally {
    submitting.value = false
  }
}

function handleDeleteType(row) {
  ElMessageBox.confirm(`确认删除字典类型「${row.dictName}」吗？`, '提示', { type: 'warning' })
    .then(() => delDictType(row.id))
    .then(() => {
      ElMessage.success('删除成功')
      dictStore.clearDict()
      getTypeList()
    })
    .catch(() => {})
}

// ---- 字典数据 CRUD ----
function handleAddData() {
  Object.assign(dataForm, emptyData(), { dictType: currentType.dictType })
  isEditData.value = false
  dataDialog.title = '新增字典数据'
  dataDialog.visible = true
}

function handleUpdateData(row) {
  Object.assign(dataForm, emptyData(), row)
  isEditData.value = true
  dataDialog.title = '修改字典数据'
  dataDialog.visible = true
}

async function submitData() {
  if (!dataFormRef.value) return
  try {
    await dataFormRef.value.validate()
  } catch (e) {
    return
  }
  submitting.value = true
  try {
    if (isEditData.value) {
      await updateDictData({ ...dataForm })
      ElMessage.success('修改成功')
    } else {
      await addDictData({ ...dataForm })
      ElMessage.success('新增成功')
    }
    dataDialog.visible = false
    // 数据变了，缓存必须作废
    dictStore.removeDict(dataForm.dictType)
    getDataList()
  } catch (e) {
    // 忽略
  } finally {
    submitting.value = false
  }
}

function handleDeleteData(row) {
  ElMessageBox.confirm(`确认删除字典数据「${row.dictLabel}」吗？`, '提示', { type: 'warning' })
    .then(() => delDictData(row.id))
    .then(() => {
      ElMessage.success('删除成功')
      dictStore.removeDict(row.dictType)
      getDataList()
    })
    .catch(() => {})
}

onMounted(getTypeList)
</script>

<style scoped>
.dict-page {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
}

.dict-left {
  width: 480px;
  flex-shrink: 0;
}

.dict-right {
  flex: 1;
  min-width: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ml-8 {
  margin-left: 8px;
}
</style>
