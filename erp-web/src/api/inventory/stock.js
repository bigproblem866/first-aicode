import request from '@/utils/request'

const BASE = '/inv/stock'

/** 库存台账列表（联表带出物料/仓库名称） */
export function listStock(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}
