import request from '@/utils/request'

const BASE = '/inv/log'

/** 库存流水列表（只读） */
export function listStockLog(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}
