<template>
  <span class="dict-tag">
    <el-tag
      v-for="(tag, index) in tags"
      :key="index"
      :type="tag.type"
      :disable-transitions="true"
      size="small"
    >
      {{ tag.label }}
    </el-tag>
  </span>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getDicts } from '@/api/system/dict'
import useDictStore from '@/store/modules/dict'

/**
 * 字典回显。
 *
 *   <DictTag :value="row.status" type="sys_normal_disable" />
 *
 * 颜色直接取字典行的 list_class（success / info / warning / danger / primary），
 * 字典改了颜色，前端不用改代码。
 */
const props = defineProps({
  value: { type: [String, Number], default: '' },
  type: { type: String, required: true },
  /** 多个值时的分隔符（如逗号分隔的角色），默认单个值 */
  separator: { type: String, default: ',' },
  /** 字典里查不到时显示的兜底文字，不传则原样显示值 */
  defaultValue: { type: String, default: '' }
})

const dictStore = useDictStore()
const options = ref([])

function loadDict(type) {
  if (!type) {
    options.value = []
    return
  }
  const cached = dictStore.getDict(type)
  if (cached && cached.length) {
    options.value = cached
    return
  }
  options.value = []
  getDicts(type)
    .then((data) => {
      const rows = data || []
      options.value = rows
      dictStore.setDict(type, rows)
    })
    .catch(() => {
      options.value = []
    })
}

watch(() => props.type, (t) => loadDict(t), { immediate: true })

/** el-tag 允许的 type 白名单，字典里配了别的值就降级成 info，避免控制台告警 */
const VALID_TAG_TYPES = ['primary', 'success', 'info', 'warning', 'danger']

const tags = computed(() => {
  const raw = props.value
  if (raw === null || raw === undefined || raw === '') {
    return []
  }
  const values = String(raw).split(props.separator)
  return values.map((val) => {
    const matched = options.value.find((item) => item.dictValue === val)
    if (matched) {
      const type = VALID_TAG_TYPES.includes(matched.listClass) ? matched.listClass : 'info'
      return { label: matched.dictLabel, type }
    }
    return { label: props.defaultValue || val, type: 'info' }
  })
})
</script>

<style scoped>
.dict-tag .el-tag + .el-tag {
  margin-left: 4px;
}
</style>
