/**
 * 菜单树工具。
 *
 * 后端下发的 RouterVo 里，子菜单的 path 是相对的（inventory / stock），
 * 必须拼到父级路径上才是真实路由。菜单浮层、功能导航页都依赖这套规则，
 * 集中在这里维护，避免两处各写一份、拼错一个前缀。
 */

/** 拼接父子路径；外链与已经是绝对路径的原样返回 */
export function joinPath(base, path) {
  if (!path) return base || '/'
  if (/^https?:\/\//.test(path)) return path
  if (path.startsWith('/')) return path
  if (!base) return `/${path}`
  return `${base}/${path}`.replace(/\/{2,}/g, '/')
}

/** 菜单标题统一从 meta.title 取 */
export function menuTitle(item) {
  return (item && item.meta && item.meta.title) || ''
}

/**
 * 收集某个目录下的全部叶子功能（递归）。
 *
 * 隐藏项与侧栏规则保持一致：hidden 的不进列表。
 * groupName 原样带出，调用方可以按它分区（如 库存查询 / 库存业务）。
 */
export function collectMenuFunctions(children, basePath) {
  const out = []
  for (const item of children || []) {
    if (item.hidden) continue
    const full = joinPath(basePath, item.path)
    const nested = (item.children || []).filter((child) => !child.hidden)
    if (nested.length) {
      out.push(...collectMenuFunctions(nested, full))
    } else if (menuTitle(item)) {
      out.push({
        path: full,
        title: menuTitle(item),
        icon: (item.meta && item.meta.icon) || '',
        groupName: item.groupName || ''
      })
    }
  }
  return out
}

/**
 * 在整棵已授权路由树里按完整路径查找。
 *
 * 必须同时返回 fullPath：二级目录的 path 是相对值（inventory / purchase），
 * 只拿节点自身的 path 当根路径会拼出 /purchase/order 这种错误地址。
 */
export function findMenuByPath(routes, base, target) {
  for (const item of routes || []) {
    if (item.hidden) continue
    const full = joinPath(base, item.path)
    if (full === target) return { route: item, fullPath: full }
    const found = findMenuByPath(item.children, full, target)
    if (found) return found
  }
  return null
}
