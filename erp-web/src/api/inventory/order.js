import request from '@/utils/request'

const BASE = '/inv/order'

/**
 * 出入库单列表。
 * 入库单 / 出库单共用这套接口，用 query.orderType = 'IN' | 'OUT' 区分。
 */
export function listOrder(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

/** 单据详情（含明细） */
export function getOrder(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/** 新增单据（含明细），后端置为草稿 */
export function addOrder(data) {
  return request({ url: BASE, method: 'post', data })
}

/** 修改单据（只有草稿可改） */
export function updateOrder(data) {
  return request({ url: BASE, method: 'put', data })
}

/** 删除单据（只有草稿可删） */
export function delOrder(ids) {
  return request({ url: `${BASE}/${ids}`, method: 'delete' })
}

/** 审核：写库存台账 + 写流水 */
export function auditOrder(id) {
  return request({ url: `${BASE}/audit/${id}`, method: 'post' })
}

/** 作废（已审核的单据不允许作废） */
export function cancelOrder(id) {
  return request({ url: `${BASE}/cancel/${id}`, method: 'post' })
}
