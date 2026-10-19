<template>
  <div class="app-container">
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="物料">
          <el-input
            v-model="queryParams.materialCode"
            placeholder="物料编码"
            clearable
            style="width: 140px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="仓库">
          <el-select
            v-model="queryParams.warehouseId"
            placeholder="请选择仓库"
            clearable
            filterable
            style="width: 150px"
          >
            <el-option
              v-for="wh in warehouseOptions"
              :key="wh.id"
              :label="wh.warehouseName"
              :value="wh.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="queryParams.bizType" placeholder="全部" clearable style="width: 120px">
            <el-option label="入库" value="IN" />
            <el-option label="出库" value="OUT" />
            <el-option label="盘点调整" value="ADJUST" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 230px"
          />
        </el-form-item>
        <el-form-item>
          <el-button size="small" type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button size="small" :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <RightToolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-table v-loading="loading" :data="list" row-key="id">
        <el-table-column label="业务日期" prop="bizDate" width="120" align="center" />
        <el-table-column label="物料编码" prop="materialCode" min-width="130" show-overflow-tooltip />
        <el-table-column label="物料名称" prop="materialName" min-width="160" show-overflow-tooltip />
        <el-table-column label="仓库" prop="warehouseName" min-width="130" show-overflow-tooltip />
        <el-table-column label="业务类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="bizTypeTag(row.bizType)" size="small" disable-transitions>
              {{ bizTypeText(row.bizType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变动数量" width="120" align="right">
          <template #default="{ row }">
            <span :class="['cell-amount', Number(row.qty) < 0 ? 'qty-minus' : 'qty-plus']">
              {{ Number(row.qty) > 0 ? '+' : '' }}{{ formatQty(row.qty) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="变动前" width="110" align="right">
          <template #default="{ row }">{{ formatQty(row.beforeQty) }}</template>
        </el-table-column>
        <el-table-column label="变动后" width="110" align="right">
          <template #default="{ row }">{{ formatQty(row.afterQty) }}</template>
        </el-table-column>
        <el-table-column label="来源单号" prop="sourceNo" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.sourceNo">{{ row.sourceNo }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="140" show-overflow-tooltip />
        <el-table-column label="记账时间" prop="createTime" width="170" />
      </el-table>

      <Pagination
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { listStockLog } from '@/api/inventory/stockLog'
import { getWarehouseOptionSelect } from '@/api/mdm/warehouse'
import { formatQty } from '@/utils/format'

/**
 * 库存流水（只读）。库存对账全靠这张表，所以页面不提供任何修改入口。
 */
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const list = ref([])
const warehouseOptions = ref([])
const dateRange = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  materialCode: undefined,
  warehouseId: undefined,
  bizType: undefined,
  bizDateStart: undefined,
  bizDateEnd: undefined
})

function bizTypeText(type) {
  return { IN: '入库', OUT: '出库', ADJUST: '盘点调整' }[type] || type
}

function bizTypeTag(type) {
  return { IN: 'success', OUT: 'danger', ADJUST: 'warning' }[type] || 'info'
}

function getList() {
  loading.value = true
  listStockLog(queryParams)
    .then((res) => {
      list.value = (res && res.rows) || []
      total.value = (res && res.total) || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function handleQuery() {
  queryParams.pageNum = 1
  queryParams.bizDateStart = dateRange.value && dateRange.value[0] ? dateRange.value[0] : undefined
  queryParams.bizDateEnd = dateRange.value && dateRange.value[1] ? dateRange.value[1] : undefined
  getList()
}

function resetQuery() {
  queryParams.materialCode = undefined
  queryParams.warehouseId = undefined
  queryParams.bizType = undefined
  dateRange.value = []
  handleQuery()
}

onMounted(() => {
  getWarehouseOptionSelect()
    .then((data) => {
      warehouseOptions.value = data || []
    })
    .catch(() => {})
  getList()
})
</script>

<style scoped>
.qty-plus {
  color: #67c23a;
}

.qty-minus {
  color: #f56c6c;
}
</style>
