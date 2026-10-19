-- =============================================================================
-- 种子数据：字典
-- -----------------------------------------------------------------------------
-- R 开头 = Flyway 可重复迁移：文件内容变化时会自动重跑，内容不变则跳过。
-- 所以这类脚本必须幂等 —— 统一用「固定主键 + ON DUPLICATE KEY UPDATE」，
-- 这样既能新增、也能修正已有行，重复执行不报错。
--
-- 注意：不要用 REPLACE INTO，它会先删后插，导致行上的 create_time 每次都被重置。
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 字典类型
-- ---------------------------------------------------------------------------
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_type`, `status`, `remark`, `create_by`)
VALUES (1, '用户性别', 'sys_user_sex', '0', '用户性别列表', 'system'),
       (2, '系统状态', 'sys_normal_disable', '0', '通用启用/停用状态', 'system'),
       (3, '系统是否', 'sys_yes_no', '0', '通用是/否', 'system'),
       (4, '计量单位', 'mdm_unit', '0', '物料计量单位', 'system'),
       (5, '物料类型', 'mdm_material_type', '0', '物料分类属性', 'system'),
       (6, '客户类型', 'mdm_customer_type', '0', '客户分类', 'system'),
       (7, '出入库类型', 'inv_order_type', '0', '入库/出库', 'system'),
       (8, '出入库单状态', 'inv_order_status', '0', '出入库单状态', 'system'),
       (9, '采购订单状态', 'pur_order_status', '0', '采购订单状态', 'system'),
       (10, '收货单状态', 'pur_receipt_status', '0', '采购收货单状态', 'system')
ON DUPLICATE KEY UPDATE `dict_name` = VALUES(`dict_name`),
                        `status`    = VALUES(`status`),
                        `remark`    = VALUES(`remark`);

-- ---------------------------------------------------------------------------
-- 字典数据
-- list_class 对应 Element Plus 的 tag 类型（success/warning/danger/info），
-- 表格里用 <DictTag> 渲染时会直接取这个值上色。
-- ---------------------------------------------------------------------------
INSERT INTO `sys_dict_data` (`id`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `list_class`,
                             `is_default`, `status`, `create_by`)
VALUES
    -- 用户性别
    (1, 1, '男', '0', 'sys_user_sex', NULL, 'Y', '0', 'system'),
    (2, 2, '女', '1', 'sys_user_sex', NULL, 'N', '0', 'system'),
    (3, 3, '未知', '2', 'sys_user_sex', NULL, 'N', '0', 'system'),
    -- 系统状态
    (10, 1, '正常', '0', 'sys_normal_disable', 'success', 'Y', '0', 'system'),
    (11, 2, '停用', '1', 'sys_normal_disable', 'danger', 'N', '0', 'system'),
    -- 是 / 否
    (20, 1, '是', 'Y', 'sys_yes_no', 'success', 'Y', '0', 'system'),
    (21, 2, '否', 'N', 'sys_yes_no', 'info', 'N', '0', 'system'),
    -- 计量单位
    (30, 1, '个', '1', 'mdm_unit', NULL, 'Y', '0', 'system'),
    (31, 2, '台', '2', 'mdm_unit', NULL, 'N', '0', 'system'),
    (32, 3, '套', '3', 'mdm_unit', NULL, 'N', '0', 'system'),
    (33, 4, '千克', '4', 'mdm_unit', NULL, 'N', '0', 'system'),
    (34, 5, '米', '5', 'mdm_unit', NULL, 'N', '0', 'system'),
    (35, 6, '升', '6', 'mdm_unit', NULL, 'N', '0', 'system'),
    (36, 7, '箱', '7', 'mdm_unit', NULL, 'N', '0', 'system'),
    (37, 8, '包', '8', 'mdm_unit', NULL, 'N', '0', 'system'),
    -- 物料类型
    (40, 1, '原材料', '1', 'mdm_material_type', 'primary', 'Y', '0', 'system'),
    (41, 2, '半成品', '2', 'mdm_material_type', 'warning', 'N', '0', 'system'),
    (42, 3, '成品', '3', 'mdm_material_type', 'success', 'N', '0', 'system'),
    (43, 4, '包装物', '4', 'mdm_material_type', 'info', 'N', '0', 'system'),
    (44, 5, '备品备件', '5', 'mdm_material_type', 'info', 'N', '0', 'system'),
    -- 客户类型
    (50, 1, '直接客户', '1', 'mdm_customer_type', 'primary', 'Y', '0', 'system'),
    (51, 2, '经销商', '2', 'mdm_customer_type', 'success', 'N', '0', 'system'),
    (52, 3, '代理商', '3', 'mdm_customer_type', 'warning', 'N', '0', 'system'),
    -- 出入库类型
    (60, 1, '入库', 'IN', 'inv_order_type', 'success', 'Y', '0', 'system'),
    (61, 2, '出库', 'OUT', 'inv_order_type', 'danger', 'N', '0', 'system'),
    -- 出入库单状态
    (70, 1, '草稿', '0', 'inv_order_status', 'info', 'Y', '0', 'system'),
    (71, 2, '已审核', '1', 'inv_order_status', 'success', 'N', '0', 'system'),
    (72, 3, '已作废', '2', 'inv_order_status', 'danger', 'N', '0', 'system'),
    -- 采购订单状态
    (80, 1, '草稿', '0', 'pur_order_status', 'info', 'Y', '0', 'system'),
    (81, 2, '已审核', '1', 'pur_order_status', 'primary', 'N', '0', 'system'),
    (82, 3, '部分收货', '2', 'pur_order_status', 'warning', 'N', '0', 'system'),
    (83, 4, '已完成', '3', 'pur_order_status', 'success', 'N', '0', 'system'),
    (84, 5, '已作废', '4', 'pur_order_status', 'danger', 'N', '0', 'system'),
    -- 收货单状态
    (90, 1, '草稿', '0', 'pur_receipt_status', 'info', 'Y', '0', 'system'),
    (91, 2, '已确认', '1', 'pur_receipt_status', 'success', 'N', '0', 'system'),
    (92, 3, '已作废', '2', 'pur_receipt_status', 'danger', 'N', '0', 'system')
ON DUPLICATE KEY UPDATE `dict_label` = VALUES(`dict_label`),
                        `dict_value` = VALUES(`dict_value`),
                        `dict_type`  = VALUES(`dict_type`),
                        `dict_sort`  = VALUES(`dict_sort`),
                        `list_class` = VALUES(`list_class`),
                        `status`     = VALUES(`status`);
