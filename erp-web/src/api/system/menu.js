import request from '@/utils/request'

const BASE = '/system/menu'

/** 菜单列表（树形数据，前端用 el-table 的 tree-props 展开） */
export function listMenu(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

/** 菜单详情 */
export function getMenu(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/** 新增菜单 */
export function addMenu(data) {
  return request({ url: BASE, method: 'post', data })
}

/** 修改菜单 */
export function updateMenu(data) {
  return request({ url: BASE, method: 'put', data })
}

/** 删除菜单 */
export function delMenu(id) {
  return request({ url: `${BASE}/${id}`, method: 'delete' })
}

/** 菜单树下拉（角色分配权限、选择上级菜单用，免权限） */
export function getMenuTreeSelect() {
  return request({ url: `${BASE}/treeselect`, method: 'get' })
}
