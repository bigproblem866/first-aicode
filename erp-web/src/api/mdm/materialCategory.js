import request from '@/utils/request'

const BASE = '/mdm/materialCategory'

/** 物料分类列表（树形数据） */
export function listMaterialCategory(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

export function getMaterialCategory(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

export function addMaterialCategory(data) {
  return request({ url: BASE, method: 'post', data })
}

export function updateMaterialCategory(data) {
  return request({ url: BASE, method: 'put', data })
}

export function delMaterialCategory(id) {
  return request({ url: `${BASE}/${id}`, method: 'delete' })
}

/** 分类树下拉（需登录） */
export function getMaterialCategoryTreeSelect() {
  return request({ url: `${BASE}/treeselect`, method: 'get' })
}
