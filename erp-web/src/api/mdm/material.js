import request from '@/utils/request'

const BASE = '/mdm/material'

/** 物料列表（分页） */
export function listMaterial(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

/** 物料详情 */
export function getMaterial(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/** 新增物料 */
export function addMaterial(data) {
  return request({ url: BASE, method: 'post', data })
}

/** 修改物料 */
export function updateMaterial(data) {
  return request({ url: BASE, method: 'put', data })
}

/** 删除物料，ids 支持逗号分隔多个 */
export function delMaterial(ids) {
  return request({ url: `${BASE}/${ids}`, method: 'delete' })
}

/** 物料下拉（单据选料用，返回精简字段，需登录） */
export function getMaterialOptionSelect(query) {
  return request({ url: `${BASE}/optionselect`, method: 'get', params: query })
}
