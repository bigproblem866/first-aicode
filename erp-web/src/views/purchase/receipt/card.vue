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
        label-width="100px"
        :disabled="!isNew"
      >
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="收货单号">
              <el-input :model-value="isNew ? '保存后自动生成' : form.receiptNo" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <div class="status-cell">
                <DictTag :value="form.status" type="pur_receipt_status" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="生成的入库单">
              <el-input :model-value="form.invOrderNo || '—'" disabled />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="采购订单" prop="orderId">
              <el-select
                v-if="isNew"
                v-model="form.orderId"
                placeholder="请选择已审核且未收完的订单"
                filterable
                style="width: 100%"
                @change="handleOrderChange"
              >
                <el-option
                  v-for="po in pendingOrders"
                  :key="po.id"
                  :label="`${po.orderNo}（${po.supplierName || ''}）`"
                  :value="po.id"
                />
              </el-select>
              <el-input v-else :model-value="form.orderNo || '—'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="供应商">
              <el-input :model-value="supplierLabel" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收货仓库" prop="warehouseId">
              <el-select
                v-model="form.warehouseId"
                placeholder="请选择收货仓库"
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
            <el-form-item label="收货日期" prop="receiptDate">
              <el-date-picker
                v-model="form.receiptDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择收货日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="detail-head">
        <span class="detail-title">收货明细</span>
        <span v-if="isNew" class="text-muted">
          默认按「订单数量 − 已收货」带出，可改小（支持分批到货）
        </span>
      </div>

      <el-table :data="form.details" border size="small" class="detail-table">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column label="物料" min-width="200">
          <template #default="{ row }">
            <span>{{ materialLabel(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isNew" label="订单数量" width="110" align="right">
          <template #default="{ row }">{{ formatQty(row.orderQty) }}</template>
        </el-table-column>
        <el-table-column v-if="isNew" label="已收货" width="110" align="right">
          <template #default="{ row }">{{ formatQty(row.receivedQty) }}</template>
        </el-table-column>
        <el-table-column label="本次收货" width="150">
          <template #default="{ row }">
            <el-input-number
              v-if="isNew"
              v-model="row.qty"
              size="small"
              :min="0"
              :max="maxQty(row)"
              :precision="4"
              :controls="false"
              style="width: 100%"
            />
            <span v-else class="cell-amount">{{ formatQty(row.qty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="150">
          <template #default="{ row }">
            <el-input v-if="isNew" v-model="row.remark" size="small" placeholder="备注" />
            <span v-else>{{ row.remark || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="isNew && !pendingOrders.length && !loading" class="empty-hint">
        当前没有可收货的采购订单（需要已审核且未收完）
      </div>

      <div v-if="!isNew" class="detail-total">
        <span>明细行数：{{ form.details.length }}</span>
        <span>合计收货数量：{{ formatQty(totalQty) }}</span>
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
  getReceipt,
  getPendingOrders,
  addReceipt,
  confirmReceipt
} from '@/api/purchase/receipt'
import { getWarehouseOptionSelect } from '@/api/mdm/warehouse'
import { getMaterialOptionSelect } from '@/api/mdm/material'
import { getSupplierOptionSelect } from '@/api/mdm/supplier'
import { formatQty } from '@/utils/format'

/**
 * 采购收货卡片页。
 *
 * 与另外两张单据略有不同：
 *   1. 明细不是自由录入，而是**选一张采购订单带出来**（只收订单上有的物料）；
 *   2. 后端没有「修改收货单」接口，所以**只有新建态可编辑**，
 *      已存在的草稿只能确认收货或删除（删除在列表里批量做）。
 */
const route = useRoute()
const router = useRouter()

const isNew = computed(() => String(route.params.id) === 'new')

const loading = ref(false)
const saving = ref(false)
const warehouseOptions = ref([])
const materialOptions = ref([])
const supplierOptions = ref([])
const pendingOrders = ref([])
const formRef = ref()

function emptyForm() {
  return {
    id: undefined,
    receiptNo: '',
    orderId: undefined,
    orderNo: '',
    supplierId: undefined,
    supplierName: '',
    warehouseId: undefined,
    receiptDate: undefined,
    status: '0',
    invOrderNo: '',
    remark: '',
    details: []
  }
}

const form = reactive(emptyForm())

const rules = {
  orderId: [{ required: true, message: '请选择采购订单', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择收货仓库', trigger: 'change' }],
  receiptDate: [{ required: true, message: '请选择收货日期', trigger: 'change' }]
}

/** 只有已存在的草稿可以确认收货 */
const canConfirm = computed(
  () => !isNew.value && String(form.status) === '0'
)

/**
 * 操作条配置。
 *
 * 与另外两张单据的差别：后端没有「修改收货单」接口，所以只有新建态能保存；
 * 已保存的草稿只能确认收货（或回列表删除）。
 */
const actions = computed(() => [
  {
    key: 'add',
    label: '新增',
    icon: Plus,
    perms: ['pur:receipt:add'],
    visible: !isNew.value,
    onClick: () => router.push('/supply/purchase/receipt/new')
  },
  {
    key: 'save',
    label: '保存',
    type: 'primary',
    loading: saving.value,
    perms: ['pur:receipt:add'],
    disabled: !isNew.value,
    reason: isNew.value ? '' : '后端未提供修改收货单接口，已保存的收货单只读',
    onClick: handleSave
  },
  {
    key: 'confirm',
    label: '确认收货',
    type: 'success',
    perms: ['pur:receipt:confirm'],
    disabled: !canConfirm.value,
    reason: isNew.value
      ? '请先保存后再确认收货'
      : String(form.status) === '0'
        ? ''
        : '仅草稿状态可以确认收货',
    onClick: handleConfirm
  }
])

const totalQty = computed(() =>
  form.details.reduce((sum, row) => sum + (Number(row.qty) || 0), 0)
)

/**
 * PurReceipt 实体没有 supplierName（列表接口是联表查出来的），
 * 卡片页只拿到 supplierId，所以从供应商选项里回填名称。
 */
const supplierLabel = computed(() => {
  if (form.supplierName) return form.supplierName
  const matched = supplierOptions.value.find(
    (s) => String(s.id) === String(form.supplierId)
  )
  return matched ? matched.supplierName : '—'
})

function maxQty(row) {
  return Math.max(Number(row.orderQty || 0) - Number(row.receivedQty || 0), 0)
}

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
  getSupplierOptionSelect()
    .then((data) => {
      supplierOptions.value = data || []
    })
    .catch(() => {})
}

function loadPendingOrders() {
  getPendingOrders()
    .then((data) => {
      pendingOrders.value = data || []

      // 采购订单卡片页的「下推收货」会带 ?orderId= 过来，自动选中并带出明细
      const preId = route.query.orderId
      if (!preId) return
      const matched = pendingOrders.value.find(
        (o) => String(o.id) === String(preId)
      )
      if (matched) {
        form.orderId = matched.id
        handleOrderChange(matched.id)
      }
    })
    .catch(() => {
      pendingOrders.value = []
    })
}

async function loadReceipt() {
  loading.value = true
  try {
    const data = await getReceipt(route.params.id)
    Object.assign(form, emptyForm(), data || {})
  } finally {
    loading.value = false
  }
}

/** 选中订单后把明细带出来，默认按「订单数量 - 已收货」预填 */
function handleOrderChange(orderId) {
  const order = pendingOrders.value.find((o) => String(o.id) === String(orderId))
  if (!order) {
    form.details = []
    return
  }
  form.orderNo = order.orderNo
  form.supplierId = order.supplierId
  form.supplierName = order.supplierName
  form.details = (order.details || []).map((d) => {
    const orderQty = Number(d.qty) || 0
    const receivedQty = Number(d.receivedQty) || 0
    return {
      orderDetailId: d.id,
      materialId: d.materialId,
      materialCode: d.materialCode,
      materialName: d.materialName,
      orderQty,
      receivedQty,
      qty: Math.max(orderQty - receivedQty, 0),
      remark: ''
    }
  })
}

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  const details = form.details.filter((d) => Number(d.qty) > 0)
  if (!details.length) {
    ElMessage.warning('本次收货数量必须大于 0')
    return
  }

  const payload = {
    orderId: form.orderId,
    orderNo: form.orderNo,
    supplierId: form.supplierId,
    warehouseId: form.warehouseId,
    receiptDate: form.receiptDate,
    remark: form.remark,
    details: details.map((d) => ({
      orderDetailId: d.orderDetailId,
      materialId: d.materialId,
      qty: d.qty,
      remark: d.remark
    }))
  }

  saving.value = true
  try {
    await addReceipt(payload)
    ElMessage.success('收货单已保存为草稿，确认收货后才会入库')
    router.push('/supply/purchase/receipt')
  } catch (e) {
    // 业务失败提示由 axios 拦截器统一弹出
  } finally {
    saving.value = false
  }
}

function handleConfirm() {
  ElMessageBox.confirm(
    `确认收货后将生成入库单、增加库存并写入库存流水，确认处理「${form.receiptNo}」吗？`,
    '提示',
    { type: 'warning' }
  )
    .then(() => confirmReceipt(String(form.id)))
    .then(() => {
      ElMessage.success('确认收货成功')
      return loadReceipt()
    })
    .catch(() => {})
}

function goBack() {
  router.push('/supply/purchase/receipt')
}

onMounted(() => {
  loadOptions()
  if (isNew.value) {
    loadPendingOrders()
  } else {
    loadReceipt()
  }
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
  gap: 16px;
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

.empty-hint {
  margin-top: 12px;
  font-size: 13px;
  color: #8a94a6;
}
</style>
