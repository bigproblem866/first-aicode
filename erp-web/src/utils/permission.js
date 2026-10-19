import useUserStore from '@/store/modules/user'

/** 超级管理员权限串，后端在 UserInfoVo.permissions 里下发 */
export const ALL_PERMISSION = '*:*:*'

function toArray(value) {
  if (value == null) return []
  return Array.isArray(value) ? value : [value]
}

/**
 * 是否拥有权限。
 *
 * - 支持传入单个字符串或字符串数组，满足其中任意一个即返回 true；
 * - 不传值视为「不需要权限」，返回 true（便于指令里无参调用）；
 * - 拥有 '*:*:*' 直接放行。
 */
export function checkPermi(value) {
  const required = toArray(value)
  if (required.length === 0) return true

  const owned = useUserStore().permissions || []
  if (owned.includes(ALL_PERMISSION)) return true

  return required.some((item) => owned.includes(item))
}

/**
 * 是否拥有角色。语义与 checkPermi 一致，比对的是角色标识（如 admin）。
 */
export function checkRole(value) {
  const required = toArray(value)
  if (required.length === 0) return true

  const owned = useUserStore().roles || []
  if (owned.includes('admin')) return true

  return required.some((item) => owned.includes(item))
}
