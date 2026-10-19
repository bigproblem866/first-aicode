<template>
  <section class="app-main">
    <!--
      面包屑已移除：当前页位置由「侧栏高亮 + 顶部标签栏」表达，
      内容区直接进入页面主体，纵向空间留给单据表格。
      Breadcrumb.vue 组件本身保留，需要时把这一行加回来即可。
    -->
    <router-view v-slot="{ Component, route }">
      <keep-alive :max="20">
        <component :is="Component" v-if="Component" :key="route.path" />
      </keep-alive>
    </router-view>
  </section>
</template>

<script setup>
/**
 * 内容区。
 *
 * keep-alive 用 max 限制缓存数量、以 route.path 作为 key：
 * 各视图文件没有声明 name，若用 include（按组件名匹配）实际一个也缓存不上，
 * 反而会给人"缓存已生效"的错觉。按 path 缓存是能真正保住页面状态的做法，
 * 代价是「关闭标签页」不会立刻销毁缓存实例（被 max 淘汰时才销毁）。
 *
 * 内边距统一在这里给：页面自己的 .app-container 已经改成 padding: 0，
 * 这样新老页面不会各有一套边距、切换时看出跳动。
 */
</script>

<style scoped>
.app-main {
  flex: 1;
  padding: var(--content-gutter);
  overflow: auto;
  background-color: var(--content-bg);
}

</style>
