<template>
  <div class="app-container">
    <el-card v-show="showSearch" class="search-card is-compact" shadow="never">
      <el-form :model="queryParams" size="small" inline @submit.prevent>
        <el-form-item label="物料">
          <el-input
            v-model="queryParams.materialCode"
            placeholder="物料编码"
            clearable
            style="width: 150px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="物料名称">
          <el-input
            v-model="queryParams.materialName"
            placeholder="物料名称"
            clearable
            style="width: 150px"
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
        <el-table-column label="物料编码" prop="materialCode" min-width="130" show-overflow-tooltip />
        <el-table-column label="物料名称" prop="materialName" min-width="170" show-overflow-tooltip />
        <el-table-column label="规格" prop="spec" min-width="120" show-overflow-tooltip />
        <el-table-column label="单位" width="80" align="center">
          <template #default="{ row }">
            <DictTag :value="row.unit" type="mdm_unit" />
          </template>
        </el-table-column>
        <el-table-column label="仓库" prop="warehouseName" min-width="140" show-overflow-tooltip />
        <el-table-column label="现存数量" width="130" align="right">
          <template #default="{ row }">
            <span class="cell-amount">{{ formatQty(row.qty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" width="170" />
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
import { listStock } from '@/api/inventory/stock'
import { getWarehouseOptionSelect } from '@/api/mdm/warehouse'
import { formatQty } from '@/utils/format'

/**
 * 库存查询（只读）。
 * 数量不允许在这里改 —— 库存只能通过单据审核变动，这是库存模块的基本原则。
 */
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const list = ref([])
const warehouseOptions = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  materialCode: undefined,
  materialName: undefined,
  warehouseId: undefined
})

function getList() {
  loading.value = true
  listStock(queryParams)
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
  getList()
}

function resetQuery() {
  queryParams.materialCode = undefined
  queryParams.materialName = undefined
  queryParams.warehouseId = undefined
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
