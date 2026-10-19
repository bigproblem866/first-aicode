import { checkRole } from '@/utils/permission'

/**
 * v-hasRole="['admin']"
 *
 * 语义与 v-hasPermi 一致，只比对角色标识。
 */
export default {
  mounted(el, binding) {
    const { value } = binding
    if (!checkRole(value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}
