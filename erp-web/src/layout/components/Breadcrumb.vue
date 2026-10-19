<template>
  <el-breadcrumb class="app-breadcrumb" separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(matched, index) in breadcrumbs" :key="matched.path">
        <span v-if="index === breadcrumbs.length - 1" class="no-redirect">
          {{ matched.title }}
        </span>
        <a v-else @click.prevent="handleLink(matched)">{{ matched.title }}</a>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

/**
 * 面包屑。
 * 数据直接取 route.matched（vue-router 已经把父子链算好了），
 * 只保留有 meta.title 的那几层，过滤掉没有标题的中间层。
 */
const route = useRoute()
const router = useRouter()

const breadcrumbs = computed(() => {
  const matched = route.matched.filter(
    (item) => item.meta && item.meta.title && item.meta.breadcrumb !== false
  )
  const list = matched.map((item) => ({
    path: item.path,
    title: item.meta.title
  }))

  // 首页永远作为第一级，方便从任何深层页面一键回到首页
  if (list.length === 0 || list[0].title !== '首页') {
    list.unshift({ path: '/dashboard', title: '首页' })
  }
  return list
})

function handleLink(item) {
  router.push(item.path).catch(() => {})
}
</script>

<style scoped>
.app-breadcrumb {
  display: inline-block;
  font-size: 12px;
  line-height: 1.2;
}

.app-breadcrumb .no-redirect {
  color: #9aa3b2;
  cursor: text;
}

.app-breadcrumb a {
  color: #6b7280;
  font-weight: normal;
  cursor: pointer;

  &:hover {
    color: var(--primary-color);
  }
}

.breadcrumb-enter-active,
.breadcrumb-leave-active {
  transition: all 0.3s;
}

.breadcrumb-enter-from,
.breadcrumb-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>
