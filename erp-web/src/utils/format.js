/**
 * 数值展示格式化。
 *
 * 后端金额/数量都是 BigDecimal，序列化成字符串下发（如 "12.5000"），
 * 直接显示会出现一长串无意义的小数位，所以统一在这里处理。
 * 注意：这里只负责"显示"，回传给后端的仍然是原值，不做四舍五入后再提交。
 */

/** 金额：固定保留 2 位小数 */
export function formatAmount(value, digits = 2) {
  if (value === null || value === undefined || value === '') return '0.00'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  return num.toFixed(digits)
}

/** 数量：最多保留 4 位小数，并把末尾多余的 0 去掉（10.0000 → 10） */
export function formatQty(value, digits = 4) {
  if (value === null || value === undefined || value === '') return '0'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  return String(Number(num.toFixed(digits)))
}

/** 税率：0.13 → 13% */
export function formatTaxRate(value) {
  if (value === null || value === undefined || value === '') return '0%'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  return `${(num * 100).toFixed(2).replace(/\.?0+$/, '')}%`
}
