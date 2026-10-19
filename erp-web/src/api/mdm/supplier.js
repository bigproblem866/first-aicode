import request from '@/utils/request'

const BASE = '/mdm/supplier'

export function listSupplier(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

export function getSupplier(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

export function addSupplier(data) {
  return request({ url: BASE, method: 'post', data })
}

export function updateSupplier(data) {
  return request({ url: BASE, method: 'put', data })
}

export function delSupplier(ids) {
  return request({ url: `${BASE}/${ids}`, method: 'delete' })
}

/** 供应商下拉（需登录） */
export function getSupplierOptionSelect(query) {
  return request({ url: `${BASE}/optionselect`, method: 'get', params: query })
}
