<template>
  <el-select
    v-model="selected"
    :placeholder="placeholder"
    :clearable="clearable"
    :multiple="multiple"
    :disabled="disabled"
    :style="{ width: width }"
    @change="handleChange"
  >
    <el-option
      v-for="dict in options"
      :key="dict.dictValue"
      :label="dict.dictLabel"
      :value="dict.dictValue"
      :disabled="dict.status === '1'"
    />
  </el-select>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { getDicts } from '@/api/system/dict'
import useDictStore from '@/store/modules/dict'

/**
 * 字典下拉。
 *
 *   <DictSelect v-model="query.status" type="sys_normal_disable" />
 *
 * 组件内部调 GET /api/system/dict/data/type/{type} 并走 Pinia 缓存，
 * 页面里永远不要手写 <el-option>。
 */
const props = defineProps({
  modelValue: { type: [String, Number, Array], default: undefined },
  /** 字典类型，如 sys_normal_disable / mdm_unit */
  type: { type: String, required: true },
  placeholder: { type: String, default: '请选择' },
  clearable: { type: Boolean, default: true },
  multiple: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  width: { type: String, default: '100%' }
})

const emit = defineEmits(['update:modelValue', 'change'])

const dictStore = useDictStore()
const options = ref([])

const selected = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

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

function handleChange(val) {
  emit('change', val)
}
</script>
