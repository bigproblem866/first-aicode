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
            <el-form-item label="订单号">
              <el-input :model-value="isNew ? '保存后自动生成' : form.orderNo" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <div class="status-cell">
                <DictTag :value="form.status" type="pur_order_status" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总金额">
              <el-input :model-value="formatAmount(totalAmount)" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商" prop="supplierId">
              <el-select
                v-model="form.supplierId"
                placeholder="请选择供应商"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="sp in supplierOptions"
                  :key="sp.id"
                  :label="sp.supplierName"
                  :value="sp.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="下单日期" prop="orderDate">
              <el-date-picker
                v-model="form.orderDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择下单日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="期望到货">
              <el-date-picker
                v-model="form.expectDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择期望到货日期"
                style="width: 100%"
              />
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
        <el-table-column label="采购数量" width="140">
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
        <el-table-column label="采购单价" width="140">
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
      <div class="detail-hint">总金额由后端按明细汇总，前端只读</div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Promotion } from '@element-plus/icons-vue'
import {
  getPurOrder,
  addPurOrder,
  updatePurOrder,
  auditPurOrder,
  cancelPurOrder
} from '@/api/purchase/order'
import { getSupplierOptionSelect } from '@/api/mdm/supplier'
import { getMaterialOptionSelect } from '@/api/mdm/material'
import { formatAmount, formatQty } from '@/utils/format'

/**
 * 采购订单卡片页（单头 + 明细）。
 *
 * 与出入库单卡片同一套范式：从列表点行进入，URL 带 id，新增时 id 为 'new'。
 * 草稿可编辑，其余状态只读。
 */
const route = useRoute()
const router = useRouter()

const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const supplierOptions = ref([])
const materialOptions = ref([])
const formRef = ref()

function emptyDetailRow() {
  return { materialId: undefined, qty: 0, price: 0, remark: '' }
}

function emptyForm() {
  return {
    id: undefined,
    orderNo: '',
    supplierId: undefined,
    orderDate: undefined,
    expectDate: undefined,
    status: '0',
    remark: '',
    details: [emptyDetailRow()]
  }
}

const form = reactive(emptyForm())

const rules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择下单日期', trigger: 'change' }]
}

/** 草稿可编辑；已作废(4) 及其他状态只读 */
const editable = computed(() => isNew.value || String(form.status) === '0')
const canAudit = computed(() => !isNew.value && String(form.status) === '0')
// 后端只允许「草稿 / 已审核」作废（部分收货、已完成都不行），按后端规则收紧
const canCancel = computed(
  () => !isNew.value && ['0', '1'].includes(String(form.status))
)

/** 已审核/部分收货可以下推生成收货单；草稿、已完成、已作废不行 */
const canPushReceipt = computed(
  () => !isNew.value && ['1', '2'].includes(String(form.status))
)

const pushReason = computed(() => {
  if (isNew.value) return '请先保存并审核订单'
  const status = String(form.status)
  if (status === '0') return '订单审核后才能下推收货'
  if (status === '3') return '订单已完成，不能再下推'
  if (status === '4') return '订单已作废，不能下推'
  return ''
})

const voidReason = computed(() => {
  if (isNew.value) return '请先保存后再作废'
  const status = String(form.status)
  if (status === '2' || status === '3') return '已部分或全部收货，不能作废'
  if (status === '4') return '订单已作废'
  return ''
})

/**
 * 操作条配置。
 *
 * 状态不可用时**保留按钮并置灰**，同时给出原因：用户能看到「这张单据能做哪些事、
 * 为什么现在不能做」。业务上根本不存在的动作（例如库存单的下推）才用 visible:false。
 */
const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['pur:order:add'],
    visible: !isNew.value,
    onClick: () => router.push('/supply/purchase/order/new')
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: ['pur:order:add', 'pur:order:edit'],
    disabled: !editable.value,
    reason: isNew.value ? '' : '已审核或已作废，不能修改',
    onClick: handleSave
  },
  {
    key: 'audit',
    label: '审核',
    type: 'success',
    perms: ['pur:order:audit'],
    disabled: !canAudit.value,
    reason: isNew.value ? '请先保存后再审核' : '仅草稿状态可以审核',
    onClick: handleAudit
  },
  {
    key: 'cancel',
    label: '作废',
    type: 'danger',
    perms: ['pur:order:audit'],
    disabled: !canCancel.value,
    reason: voidReason.value,
    onClick: handleCancel
  },
  {
    key: 'push',
    label: '下推',
    icon: Promotion,
    perms: ['pur:receipt:add'],
    disabled: !canPushReceipt.value,
    reason: pushReason.value,
    onClick: handlePushReceipt
  }
])

function handlePushReceipt() {
  router.push(`/supply/purchase/receipt/new?orderId=${form.id}`)
}

function rowAmount(row) {
  if (editable.value) {
    return (Number(row.qty) || 0) * (Number(row.price) || 0)
  }
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
  getSupplierOptionSelect()
    .then((data) => {
      supplierOptions.value = data || []
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
    const data = await getPurOrder(route.params.id)
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

/** 选了物料就把参考采购价带进单价 */
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
    ElMessage.warning('明细采购数量必须大于 0')
    return
  }

  // 注意：totalAmount 不提交，后端按明细汇总（规范里明确禁止前端传总金额）
  const payload = {
    id: form.id,
    supplierId: form.supplierId,
    orderDate: form.orderDate,
    expectDate: form.expectDate,
    remark: form.remark,
    details: details.map((d) => ({
      materialId: d.materialId,
      qty: d.qty,
      price: d.price,
      amount: rowAmount(d),
      remark: d.remark
    }))
  }

  saving.value = true
  try {
    if (isNew.value) {
      await addPurOrder(payload)
      ElMessage.success('新增成功')
      router.push('/supply/purchase/order')
    } else {
      await updatePurOrder(payload)
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
  ElMessageBox.confirm(`确认审核订单「${form.orderNo}」吗？`, '提示', { type: 'warning' })
    .then(() => auditPurOrder(String(form.id)))
    .then(() => {
      ElMessage.success('审核成功')
      return loadOrder()
    })
    .catch(() => {})
}

function handleCancel() {
  ElMessageBox.confirm(`确认作废订单「${form.orderNo}」吗？`, '提示', { type: 'warning' })
    .then(() => cancelPurOrder(String(form.id)))
    .then(() => {
      ElMessage.success('作废成功')
      return loadOrder()
    })
    .catch(() => {})
}

function goBack() {
  router.push('/supply/purchase/order')
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

.detail-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #8a94a6;
  text-align: right;
}
</style>
