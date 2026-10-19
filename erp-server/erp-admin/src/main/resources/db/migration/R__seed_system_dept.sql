-- =============================================================================
-- 种子数据：部门
-- -----------------------------------------------------------------------------
-- ancestors 字段存"从根到父节点"的路径，如 0,100。
-- 它的作用是让"查某部门及其所有下级"变成一条 LIKE 查询
-- （ancestors LIKE '0,100%'），而不必递归查询。
-- 代价是移动部门时需要级联更新所有下级的 ancestors —— 本期不做部门移动，
-- 后续实现时记得在 Service 里补上这段级联更新。
-- =============================================================================

INSERT INTO `sys_dept` (`id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `status`, `create_by`)
VALUES (100, 0, '0', 'ERP 演示公司', 1, '管理员', '0', 'system'),
       (101, 100, '0,100', '综合管理部', 1, '管理员', '0', 'system'),
       (102, 100, '0,100', '采购部', 2, '王采购', '0', 'system'),
       (103, 100, '0,100', '仓储部', 3, '李仓管', '0', 'system'),
       (104, 100, '0,100', '销售部', 4, '赵销售', '0', 'system')
ON DUPLICATE KEY UPDATE `parent_id` = VALUES(`parent_id`),
                        `ancestors` = VALUES(`ancestors`),
                        `dept_name` = VALUES(`dept_name`),
                        `order_num` = VALUES(`order_num`),
                        `status`    = VALUES(`status`);
