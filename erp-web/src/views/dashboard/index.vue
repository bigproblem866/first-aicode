<template>
  <div class="app-container">
    <PageHeader title="工作台" description="业务概览。统计口径待业务接口确定，当前统一显示 —" />

    <el-row :gutter="12">
      <el-col v-for="card in cards" :key="card.title" :xs="24" :sm="12" :md="6">
        <el-card class="stat-card" shadow="never">
          <div class="stat-body">
            <div
              class="stat-icon"
              :style="{ color: card.color, backgroundColor: card.bg }"
            >
              <el-icon>
                <component :is="card.icon" />
              </el-icon>
            </div>
            <div class="stat-text">
              <div class="stat-title">{{ card.title }}</div>
              <div class="stat-value">{{ card.value }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <WelcomePopup />
  </div>
</template>

<script setup>
import WelcomePopup from '@/components/WelcomePopup/index.vue'

/**
 * 首页。这里只放占位统计卡片 —— 真正的统计口径要等业务接口出来再定，
 * 现在写死数字容易让人误以为它是准的，所以统一显示 "—"。
 */
const cards = [
  { title: '物料总数', value: '—', icon: 'Goods', color: '#d9001b', bg: '#fbe6e8' },
  { title: '客户数量', value: '—', icon: 'Avatar', color: '#67c23a', bg: '#f0f9eb' },
  {
    title: '供应商数量',
    value: '—',
    icon: 'OfficeBuilding',
    color: '#e6a23c',
    bg: '#fdf6ec'
  },
  { title: '待办单据', value: '—', icon: 'Tickets', color: '#f56c6c', bg: '#fef0f0' }
]
</script>

<style scoped>
.stat-card {
  margin-bottom: 12px;
}

.stat-body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  font-size: 22px;
  border-radius: 8px;
}

.stat-title {
  font-size: 12px;
  color: #8a94a6;
}

.stat-value {
  margin-top: 2px;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
  color: #1f2937;
}
</style>
