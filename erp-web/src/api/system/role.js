import request from '@/utils/request'

const BASE = '/system/role'

/** 角色列表（分页） */
export function listRole(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

/** 角色详情 */
export function getRole(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/** 新增角色 */
export function addRole(data) {
  return request({ url: BASE, method: 'post', data })
}

/** 修改角色 */
export function updateRole(data) {
  return request({ url: BASE, method: 'put', data })
}

/** 删除角色 */
export function delRole(ids) {
  return request({ url: `${BASE}/${ids}`, method: 'delete' })
}

/** 角色下拉（免权限） */
export function getRoleOptionSelect() {
  return request({ url: `${BASE}/optionselect`, method: 'get' })
}

/**
 * 角色对应的菜单树（树结构 + 该角色已勾选的菜单 id），给权限分配弹窗用。
 * 返回大致形如 { menus: [...树...], checkedKeys: [...] }
 */
export function getRoleMenuTreeSelect(roleId) {
  return request({ url: `/system/menu/roleMenuTreeselect/${roleId}`, method: 'get' })
}
