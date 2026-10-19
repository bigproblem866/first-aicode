<template>
  <router-view />
</template>

<script setup>
/**
 * 多级目录的中间层容器。
 *
 * 后端 RouterVo.component 返回 'ParentView' 时映射到本组件：
 * 它本身没有任何 UI，只负责把下一级路由渲染出来。
 * 例如「系统管理 > 权限设置 > 用户管理」这种三层菜单，中间那层就是它。
 * 当前种子数据只有两层，暂时用不上，但菜单表随时可能加层级，先留着。
 */
</script>
