<template>
  <ErrorPage
    code="401"
    description="你没有访问该页面的权限，请联系管理员分配"
    tone="warning"
  />
</template>

<script setup>
import ErrorPage from '@/components/ErrorPage/index.vue'
</script>
