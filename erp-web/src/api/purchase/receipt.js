import request from '@/utils/request'

const BASE = '/pur/receipt'

/** 收货单列表（分页） */
export function listReceipt(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

/** 收货单详情（含明细） */
export function getReceipt(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/** 可收货的采购订单（已审核且未收完，含明细），做收货时选单用 */
export function getPendingOrders(query) {
  return request({ url: `${BASE}/pendingOrders`, method: 'get', params: query })
}

/** 新增收货单（草稿） */
export function addReceipt(data) {
  return request({ url: BASE, method: 'post', data })
}

/** 删除收货单（只有草稿可删） */
export function delReceipt(ids) {
  return request({ url: `${BASE}/${ids}`, method: 'delete' })
}

/** 确认收货：生成入库单、增加库存、写流水 */
export function confirmReceipt(id) {
  return request({ url: `${BASE}/confirm/${id}`, method: 'post' })
}
