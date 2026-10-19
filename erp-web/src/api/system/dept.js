import request from '@/utils/request'

const BASE = '/system/dept'

/** 部门列表（树形数据） */
export function listDept(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

/** 部门详情 */
export function getDept(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/** 新增部门 */
export function addDept(data) {
  return request({ url: BASE, method: 'post', data })
}

/** 修改部门 */
export function updateDept(data) {
  return request({ url: BASE, method: 'put', data })
}

/** 删除部门 */
export function delDept(id) {
  return request({ url: `${BASE}/${id}`, method: 'delete' })
}

/** 部门树下拉（免权限） */
export function getDeptTreeSelect() {
  return request({ url: `${BASE}/treeselect`, method: 'get' })
}
