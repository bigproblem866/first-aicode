import { ref } from 'vue'
import useDictStore from '@/store/modules/dict'
import { getDicts } from '@/api/system/dict'

/**
 * 字典组合式函数。
 *
 * 用法：
 *   const { sys_normal_disable, mdm_unit } = useDict('sys_normal_disable', 'mdm_unit')
 *   // 模板里： sys_normal_disable.value 就是 [{ dictLabel, dictValue, listClass }, ...]
 *
 * 返回的是 ref 数组 —— 字典是异步取回的，用 ref 才能让模板在数据到达后自动刷新。
 * 取过的字典会写进 Pinia 的 dict store，同一类型只请求一次。
 */
export function useDict(...types) {
  const dictStore = useDictStore()
  const result = {}

  types.forEach((type) => {
    const cached = dictStore.getDict(type)
    const list = ref(cached || [])
    result[type] = list

    if (!cached || cached.length === 0) {
      getDicts(type)
        .then((data) => {
          const rows = data || []
          list.value = rows
          dictStore.setDict(type, rows)
        })
        .catch(() => {
          // 字典接口失败时保持空数组即可，错误提示已由 axios 拦截器统一弹出
        })
    }
  })

  return result
}

/**
 * 命令式取字典（给非 setup 环境或需要在回调里用字典时使用）。
 */
export function getDictOptions(type) {
  return getDicts(type).then((data) => data || [])
}
