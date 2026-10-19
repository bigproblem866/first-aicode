import request from '@/utils/request'

const BASE = '/mdm/customer'

export function listCustomer(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

export function getCustomer(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

export function addCustomer(data) {
  return request({ url: BASE, method: 'post', data })
}

export function updateCustomer(data) {
  return request({ url: BASE, method: 'put', data })
}

export function delCustomer(ids) {
  return request({ url: `${BASE}/${ids}`, method: 'delete' })
}

/** 客户下拉（需登录） */
export function getCustomerOptionSelect(query) {
  return request({ url: `${BASE}/optionselect`, method: 'get', params: query })
}
