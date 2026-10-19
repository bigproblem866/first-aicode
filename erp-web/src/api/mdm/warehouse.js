import request from '@/utils/request'

const BASE = '/mdm/warehouse'

export function listWarehouse(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

export function getWarehouse(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

export function addWarehouse(data) {
  return request({ url: BASE, method: 'post', data })
}

export function updateWarehouse(data) {
  return request({ url: BASE, method: 'put', data })
}

export function delWarehouse(ids) {
  return request({ url: `${BASE}/${ids}`, method: 'delete' })
}

/** 仓库下拉（需登录） */
export function getWarehouseOptionSelect(query) {
  return request({ url: `${BASE}/optionselect`, method: 'get', params: query })
}
