import request from '@/utils/request'

const BASE = '/pur/order'

/** 采购订单列表（分页） */
export function listPurOrder(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

/** 采购订单详情（含明细） */
export function getPurOrder(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/** 新增订单（含明细）；总金额由后端按明细汇总，前端不要传 totalAmount */
export function addPurOrder(data) {
  return request({ url: BASE, method: 'post', data })
}

/** 修改订单（只有草稿可改） */
export function updatePurOrder(data) {
  return request({ url: BASE, method: 'put', data })
}

/** 删除订单（只有草稿可删） */
export function delPurOrder(ids) {
  return request({ url: `${BASE}/${ids}`, method: 'delete' })
}

/** 审核订单 */
export function auditPurOrder(id) {
  return request({ url: `${BASE}/audit/${id}`, method: 'post' })
}

/** 作废订单 */
export function cancelPurOrder(id) {
  return request({ url: `${BASE}/cancel/${id}`, method: 'post' })
}
