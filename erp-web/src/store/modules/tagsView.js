import { defineStore } from 'pinia'

/**
 * 顶部标签页（多页签）。
 *
 * 只保存「可以放进标签栏的路由」的最小信息，不存组件。
 * 刷新浏览器后标签会清空，只保留当前页 —— 这是刻意的：
 * 把标签列表持久化会带来"重启后点标签跳到一个已无权限的页面"的问题。
 */
export const useTagsViewStore = defineStore('tagsView', {
  state: () => ({
    visitedViews: [],
    cachedViews: []
  }),

  actions: {
    addView(view) {
      this.addVisitedView(view)
      this.addCachedView(view)
    },

    addVisitedView(view) {
      if (!view || !view.path) return
      if (this.visitedViews.some((v) => v.path === view.path)) return

      const item = {
        name: view.name,
        path: view.path,
        fullPath: view.fullPath || view.path,
        title: (view.meta && view.meta.title) || '未命名',
        affix: !!(view.meta && view.meta.affix)
      }

      // 固定标签永远插在最前：即使它是后加的（例如直接刷新到某个业务页），
      // 首页也不会跑到业务标签后面
      if (item.affix) {
        const firstNonAffix = this.visitedViews.findIndex((v) => !v.affix)
        const index = firstNonAffix === -1 ? this.visitedViews.length : firstNonAffix
        this.visitedViews.splice(index, 0, item)
        return
      }

      this.visitedViews.push(item)
    },

    addCachedView(view) {
      if (!view || !view.name) return
      const noCache = view.meta && view.meta.noCache
      if (noCache) return
      if (this.cachedViews.includes(view.name)) return
      this.cachedViews.push(view.name)
    },

    delView(view) {
      const path = view.path
      this.visitedViews = this.visitedViews.filter((v) => v.path !== path)
      this.cachedViews = this.cachedViews.filter((name) => name !== view.name)
    },

    delOthersViews(view) {
      this.visitedViews = this.visitedViews.filter((v) => {
        return v.affix || v.path === view.path
      })
      this.cachedViews = this.cachedViews.filter((name) => name === view.name)
    },

    delAllViews() {
      this.visitedViews = this.visitedViews.filter((v) => v.affix)
      this.cachedViews = []
    },

    /**
     * 拖动排序。
     *
     * toIndex 是「插入前的目标位置」（0..length），与数组下标语义一致，
     * 调用方不用关心先删后插导致的下标偏移。
     *
     * 规则：固定标签（affix，首页）不能被拖动，其他标签也不能排到它前面。
     * 只调整 visitedViews；cachedViews 不参与 keep-alive（AppMain 用 path 做 key），
     * 顺序没有意义，不需要同步。
     */
    moveView(fromIndex, toIndex) {
      const views = this.visitedViews
      if (fromIndex < 0 || fromIndex >= views.length) return
      if (views[fromIndex].affix) return

      const affixCount = views.filter((v) => v.affix).length
      let target = Math.max(0, Math.min(toIndex, views.length))
      if (target < affixCount) target = affixCount

      // 落在原位的前后，都是没有变化
      if (target === fromIndex || target === fromIndex + 1) return

      const [moved] = views.splice(fromIndex, 1)
      const insertAt = target > fromIndex ? target - 1 : target
      views.splice(insertAt, 0, moved)
    }
  }
})

export default useTagsViewStore
