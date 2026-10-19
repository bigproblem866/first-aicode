import { defineStore } from 'pinia'

/**
 * 字典缓存。
 *
 * 结构：[{ type: 'sys_normal_disable', value: [ {dictLabel, dictValue, listClass}, ... ] }]
 * 用数组而非对象存，是为了避免字典类型名恰好是 'constructor' 之类时踩原型链的坑。
 */
export const useDictStore = defineStore('dict', {
  state: () => ({
    dicts: []
  }),

  actions: {
    /** 取缓存，未缓存返回 null */
    getDict(type) {
      const hit = this.dicts.find((item) => item.type === type)
      return hit ? hit.value : null
    },

    setDict(type, value) {
      const hit = this.dicts.find((item) => item.type === type)
      if (hit) {
        hit.value = value
      } else {
        this.dicts.push({ type, value })
      }
    },

    removeDict(type) {
      this.dicts = this.dicts.filter((item) => item.type !== type)
    },

    clearDict() {
      this.dicts = []
    }
  }
})

export default useDictStore
