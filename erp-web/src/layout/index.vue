<template>
  <div class="app-wrapper">
    <!-- 全局顶栏：横跨整个窗口宽度；搜索框与菜单入口都在这一行 -->
    <GlobalHeader />

    <div class="app-body">
      <div class="main-wrap">
        <TagsView />
        <AppMain />
      </div>
    </div>
  </div>
</template>

<script setup>
import GlobalHeader from './components/GlobalHeader.vue'
import TagsView from './components/TagsView.vue'
import AppMain from './components/AppMain.vue'

/**
 * 主框架：全局顶栏 + 页签 + 内容区。
 *
 * 侧栏不再常驻左列：它收进了顶栏品牌区左侧的菜单图标（MenuLauncher.vue），
 * 悬停弹出浮层覆盖在内容上，所以内容区始终是整宽。
 * 搜索框也已并入顶栏右侧，不再单独占一行。
 */
</script>

<style scoped>
.app-wrapper {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.app-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}
</style>
