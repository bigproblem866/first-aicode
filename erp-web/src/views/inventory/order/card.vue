<template>
  <div v-loading="loading" class="app-container">
    <DocToolbar :actions="actions">
      <template #right>
        <el-button size="small" :icon="ArrowLeft" @click="goBack">返回列表</el-button>
      </template>
    </DocToolbar>

    <el-card class="bill-card" shadow="never">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="small"
        label-width="90px"
        :disabled="!editable"
      >
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="单据号">
              <el-input :model-value="isNew ? '保存后自动生成' : form.orderNo" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单据类型">
              <el-input :model-value="`${typeText}单`" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <div class="status-cell">
                <DictTag :value="form.status" type="inv_order_status" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="仓库" prop="warehouseId">
              <el-select
                v-model="form.warehouseId"
                placeholder="请选择仓库"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="wh in warehouseOptions"
                  :key="wh.id"
                  :label="wh.warehouseName"
                  :value="wh.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务日期" prop="bizDate">
              <el-date-picker
                v-model="form.bizDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择业务日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="来源单号">
              <el-input :model-value="form.sourceNo || '—'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="detail-head">
        <span class="detail-title">明细</span>
        <el-button
          v-if="editable"
          type="primary"
          link
          :icon="Plus"
          @click="addDetailRow"
        >
          添加明细行
        </el-button>
      </div>

      <el-table :data="form.details" border size="small" class="detail-table">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="物料" min-width="220">
          <template #default="{ row }">
            <el-select
              v-if="editable"
              v-model="row.materialId"
              size="small"
              placeholder="请选择物料"
              filterable
              style="width: 100%"
              @change="(val) => handleMaterialChange(row, val)"
            >
              <el-option
                v-for="m in materialOptions"
                :key="m.id"
                :label="`${m.materialCode} ${m.materialName}`"
                :value="m.id"
              />
            </el-select>
            <span v-else>{{ materialLabel(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="140">
          <template #default="{ row }">
            <el-input-number
              v-if="editable"
              v-model="row.qty"
              size="small"
              :min="0"
              :precision="4"
              :controls="false"
              style="width: 100%"
            />
            <span v-else class="cell-amount">{{ formatQty(row.qty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="140">
          <template #default="{ row }">
            <el-input-number
              v-if="editable"
              v-model="row.price"
              size="small"
              :min="0"
              :precision="4"
              :controls="false"
              style="width: 100%"
            />
            <span v-else class="cell-amount">{{ formatAmount(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="cell-amount">{{ formatAmount(rowAmount(row)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140">
          <template #default="{ row }">
            <el-input v-if="editable" v-model="row.remark" size="small" placeholder="备注" />
            <span v-else>{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="editable" label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" @click="removeDetailRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="detail-total">
        <span>明细行数：{{ form.details.length }}</span>
        <span>合计数量：{{ formatQty(totalQty) }}</span>
        <span>合计金额：{{ formatAmount(totalAmount) }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import {
  getOrder,
  addOrder,
  updateOrder,
  auditOrder,
  cancelOrder
} from '@/api/inventory/order'
import { getWarehouseOptionSelect } from '@/api/mdm/warehouse'
import { getMaterialOptionSelect } from '@/api/mdm/material'
import { formatAmount, formatQty } from '@/utils/format'

/**
 * 单据卡片页（单头 + 明细）。
 *
 * 这是「列表 → 卡片」范式里的卡片页：从列表点某一行进来，URL 带单据 id；
 * 新增时 id 为 'new'。草稿状态可编辑，已审核/已作废只读。
 *
 * 为什么用独立路由而不是弹窗：
 *   1. 单据能被 URL 直接打开、能进浏览器页签、刷新不丢；
 *   2. 明细表格有足够的横向与纵向空间，不用被弹窗宽度限制；
 *   3. 和列表是「浏览 / 处理」两种心智，不在一个页面上互相打断。
 *
 * 入库单与出库单共用本组件，靠路由 path 区分（inbound → IN，outbound → OUT）。
 */
const route = useRoute()
const router = useRouter()

const orderType = computed(() => (route.path.includes('outbound') ? 'OUT' : 'IN'))
const typeText = computed(() => (orderType.value === 'IN' ? '入库' : '出库'))
const listPath = computed(() =>
  orderType.value === 'IN' ? '/supply/inventory/inbound' : '/supply/inventory/outbound'
)

const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const warehouseOptions = ref([])
const materialOptions = ref([])
const formRef = ref()

function emptyDetailRow() {
  return { materialId: undefined, qty: 0, price: 0, remark: '' }
}

function emptyForm() {
  return {
    id: undefined,
    orderNo: '',
    orderType: orderType.value,
    warehouseId: undefined,
    bizDate: undefined,
    sourceNo: '',
    status: '0',
    remark: '',
    details: [emptyDetailRow()]
  }
}

const form = reactive(emptyForm())

const rules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  bizDate: [{ required: true, message: '请选择业务日期', trigger: 'change' }]
}

/** 草稿可编辑；已审核(1) / 已作废(2) 只读 */
const editable = computed(() => isNew.value || String(form.status) === '0')
const canAudit = computed(() => !isNew.value && String(form.status) === '0')
// 后端 InvOrderServiceImpl.cancel 明确只允许「已审核」作废；草稿直接删除即可。
// 按后端规则收紧，避免按钮点下去才报"只有已审核的单据才能作废"。
const canCancel = computed(() => !isNew.value && String(form.status) === '1')

const cancelReason = computed(() => {
  if (isNew.value) return '请先保存后再作废'
  const status = String(form.status)
  if (status === '0') return '草稿请直接删除，只有已审核单据可以作废'
  if (status === '2') return '单据已作废'
  return ''
})

/**
 * 操作条配置。
 *
 * 状态不可用时保留按钮并置灰 + 原因；库存单没有下游单据，所以不配置「下推」。
 */
const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['inv:order:add'],
    visible: !isNew.value,
    onClick: () => router.push(`${listPath.value}/new`)
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: ['inv:order:add', 'inv:order:edit'],
    disabled: !editable.value,
    reason: isNew.value ? '' : '已审核或已作废，不能修改',
    onClick: handleSave
  },
  {
    key: 'audit',
    label: '审核',
    type: 'success',
    perms: ['inv:order:audit'],
    disabled: !canAudit.value,
    reason: isNew.value ? '请先保存后再审核' : '仅草稿状态可以审核',
    onClick: handleAudit
  },
  {
    key: 'cancel',
    label: '作废',
    type: 'danger',
    perms: ['inv:order:audit'],
    disabled: !canCancel.value,
    reason: cancelReason.value,
    onClick: handleCancel
  }
])

function rowAmount(row) {
  if (editable.value) {
    return (Number(row.qty) || 0) * (Number(row.price) || 0)
  }
  // 只读态优先用后端算好的 amount，避免精度展示差异
  return row.amount != null
    ? row.amount
    : (Number(row.qty) || 0) * (Number(row.price) || 0)
}

const totalQty = computed(() =>
  form.details.reduce((sum, row) => sum + (Number(row.qty) || 0), 0)
)

const totalAmount = computed(() =>
  form.details.reduce((sum, row) => sum + Number(rowAmount(row) || 0), 0)
)

function materialLabel(row) {
  if (row.materialCode || row.materialName) {
    return `${row.materialCode || ''} ${row.materialName || ''}`.trim()
  }
  const matched = materialOptions.value.find(
    (m) => String(m.id) === String(row.materialId)
  )
  return matched ? `${matched.materialCode} ${matched.materialName}` : '—'
}

function loadOptions() {
  getWarehouseOptionSelect()
    .then((data) => {
      warehouseOptions.value = data || []
    })
    .catch(() => {})
  getMaterialOptionSelect()
    .then((data) => {
      materialOptions.value = data || []
    })
    .catch(() => {})
}

async function loadOrder() {
  loading.value = true
  try {
    const data = await getOrder(route.params.id)
    Object.assign(form, emptyForm(), {
      ...data,
      details:
        data.details && data.details.length ? data.details : [emptyDetailRow()]
    })
  } finally {
    loading.value = false
  }
}

function addDetailRow() {
  form.details.push(emptyDetailRow())
}

function removeDetailRow(index) {
  form.details.splice(index, 1)
  if (form.details.length === 0) form.details.push(emptyDetailRow())
}

/** 选了物料就把参考采购价带进单价，省得每行手敲 */
function handleMaterialChange(row, materialId) {
  const matched = materialOptions.value.find(
    (m) => String(m.id) === String(materialId)
  )
  if (matched && matched.purchasePrice !== undefined && !Number(row.price)) {
    row.price = Number(matched.purchasePrice)
  }
}

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  const details = form.details.filter((d) => d.materialId)
  if (details.length === 0) {
    ElMessage.warning('请至少添加一行明细')
    return
  }
  if (details.some((d) => !Number(d.qty))) {
    ElMessage.warning('明细数量必须大于 0')
    return
  }

  const payload = {
    ...form,
    orderType: orderType.value,
    details: details.map((d) => ({ ...d, amount: rowAmount(d) }))
  }

  saving.value = true
  try {
    if (isNew.value) {
      await addOrder(payload)
      ElMessage.success('新增成功')
      // 新增接口只返回 R<Void>，拿不到新单号，保存后回列表
      // （新单据在列表最上方，点进去即可继续操作）
      router.push(listPath.value)
    } else {
      await updateOrder(payload)
      ElMessage.success('保存成功')
      await loadOrder()
    }
  } catch (e) {
    // 业务失败提示由 axios 拦截器统一弹出
  } finally {
    saving.value = false
  }
}

function handleAudit() {
  ElMessageBox.confirm(
    `审核后将立即${orderType.value === 'IN' ? '增加' : '扣减'}库存并写入流水，确认审核「${form.orderNo}」吗？`,
    '提示',
    { type: 'warning' }
  )
    .then(() => auditOrder(String(form.id)))
    .then(() => {
      ElMessage.success('审核成功')
      return loadOrder()
    })
    .catch(() => {})
}

function handleCancel() {
  ElMessageBox.confirm(`确认作废「${form.orderNo}」吗？`, '提示', { type: 'warning' })
    .then(() => cancelOrder(String(form.id)))
    .then(() => {
      ElMessage.success('作废成功')
      return loadOrder()
    })
    .catch(() => {})
}

function goBack() {
  router.push(listPath.value)
}

onMounted(() => {
  loadOptions()
  if (!isNew.value) loadOrder()
})
</script>

<style scoped>
.status-cell {
  display: flex;
  align-items: center;
  min-height: 32px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 8px 0 10px;
}

.detail-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.detail-total {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  margin-top: 12px;
  font-size: 13px;
  color: #4b5563;
}
</style>
