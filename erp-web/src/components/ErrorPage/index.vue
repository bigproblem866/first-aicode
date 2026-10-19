<template>
  <div class="error-page">
    <div class="error-code" :class="`tone-${tone}`">{{ code }}</div>
    <div class="error-desc">{{ description }}</div>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

/**
 * 全屏错误页（401 / 404 共用）。
 *
 * 这两个页面原本是两个几乎一样的文件，只有数字和颜色不同。
 * 抽成一个组件后，改样式只改一处，也不会出现"改了 404 忘了 401"。
 *
 * tone: primary = 主色（404，普通"找不到"）；warning = 琥珀色（401，需要处理）
 */
defineProps({
  code: { type: String, required: true },
  description: { type: String, required: true },
  tone: { type: String, default: 'primary' }
})

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  background-color: var(--content-bg);
}

.error-code {
  font-size: 72px;
  font-weight: 700;
  line-height: 1;
}

.tone-primary {
  color: var(--primary-color);
}

.tone-warning {
  color: #e6a23c;
}

.error-desc {
  font-size: 14px;
  color: #8a94a6;
}
</style>
