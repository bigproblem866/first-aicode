import request from '@/utils/request'

// ---------------------------------------------------------------------------
// 字典类型 /system/dict/type
// ---------------------------------------------------------------------------
export function listDictType(query) {
  return request({ url: '/system/dict/type/list', method: 'get', params: query })
}

export function getDictType(id) {
  return request({ url: `/system/dict/type/${id}`, method: 'get' })
}

export function addDictType(data) {
  return request({ url: '/system/dict/type', method: 'post', data })
}

export function updateDictType(data) {
  return request({ url: '/system/dict/type', method: 'put', data })
}

export function delDictType(ids) {
  return request({ url: `/system/dict/type/${ids}`, method: 'delete' })
}

// ---------------------------------------------------------------------------
// 字典数据 /system/dict/data
// ---------------------------------------------------------------------------
export function listDictData(query) {
  return request({ url: '/system/dict/data/list', method: 'get', params: query })
}

export function getDictData(id) {
  return request({ url: `/system/dict/data/${id}`, method: 'get' })
}

export function addDictData(data) {
  return request({ url: '/system/dict/data', method: 'post', data })
}

export function updateDictData(data) {
  return request({ url: '/system/dict/data', method: 'put', data })
}

export function delDictData(ids) {
  return request({ url: `/system/dict/data/${ids}`, method: 'delete' })
}

/**
 * 按字典类型取字典数据。
 * 免权限（后端只要求已登录），DictSelect / DictTag / useDict 都走它。
 */
export function getDicts(dictType) {
  return request({ url: `/system/dict/data/type/${dictType}`, method: 'get' })
}
