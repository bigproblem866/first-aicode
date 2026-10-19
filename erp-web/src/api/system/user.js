import request from '@/utils/request'

const BASE = '/system/user'

/** 用户列表（分页） */
export function listUser(query) {
  return request({ url: `${BASE}/list`, method: 'get', params: query })
}

/** 用户详情 */
export function getUser(id) {
  return request({ url: `${BASE}/${id}`, method: 'get' })
}

/** 新增用户 */
export function addUser(data) {
  return request({ url: BASE, method: 'post', data })
}

/** 修改用户 */
export function updateUser(data) {
  return request({ url: BASE, method: 'put', data })
}

/** 删除用户，ids 支持逗号分隔的多个 id */
export function delUser(ids) {
  return request({ url: `${BASE}/${ids}`, method: 'delete' })
}

/** 重置密码（重置他人） */
export function resetUserPwd(data) {
  return request({ url: `${BASE}/resetPwd`, method: 'put', data })
}

/** 角色下拉（给用户分配角色用，免权限） */
export function getRoleOptionSelect() {
  return request({ url: '/system/role/optionselect', method: 'get' })
}

/** 部门树下拉（免权限） */
export function getDeptTreeSelect() {
  return request({ url: '/system/dept/treeselect', method: 'get' })
}
