import { checkPermi } from '@/utils/permission'

/**
 * v-hasPermi="['mdm:material:add']"
 *
 * 没有权限时把元素从 DOM 中移除（而不是 display:none）：
 * 隐藏的元素依然能被脚本触发，移除才是干净的做法。
 */
export default {
  mounted(el, binding) {
    const { value } = binding
    if (!checkPermi(value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}
