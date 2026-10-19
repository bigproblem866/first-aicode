-- =============================================================================
-- 种子数据：菜单与权限点
-- -----------------------------------------------------------------------------
-- 这张表驱动前端左侧导航 + 动态路由 + 按钮权限，是整个权限体系的数据源头。
-- 三列的组合规则（前端 store/permission.js 就是按这个规则解析的）：
--
--   menu_type = M 目录：path='system'（一级地址），component 为空 → 前端挂到 Layout 下
--   menu_type = C 菜单：path='user'（相对父级的子地址），component='system/user/index'
--                       → 前端映射到 src/views/system/user/index.vue
--   menu_type = F 按钮：path/component 为空，只提供 perms 字符串
--                       → 前端 v-hasPermi 指令 + 后端 @PreAuthorize 都用它
--
-- 权限串命名规范：模块:资源:动作。必须与后端 @PreAuthorize("@ss.hasPermi('...')")
-- 里的字符串**逐字一致**，写错一个字母的结果是按钮可见但接口 403（很难查）。
--
-- ID 分配规则（便于人工维护与排查）：
--   1-99     一级目录
--   100-199  二级菜单
--   1000+    按钮（前两位跟随所属菜单）
-- =============================================================================

INSERT INTO `sys_menu` (`id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`,
                        `visible`, `status`, `perms`, `icon`, `is_frame`, `is_cache`, `create_by`)
VALUES
    -- ===================== 一级目录 =====================
    (1, '系统管理', 0, 1, 'system', NULL, 'M', '0', '0', NULL, 'Setting', 0, 0, 'system'),
    (2, '基础资料', 0, 2, 'mdm', NULL, 'M', '0', '0', NULL, 'Files', 0, 0, 'system'),
    -- 供应链是二级目录的容器：库存、采购、销售（后续）都属于它。
    -- 后端把非一级的目录统一映射成 ParentView（纯 <router-view/> 容器），
    -- 所以三级路由不需要前端额外写代码。
    (5, '供应链', 0, 3, 'supply', NULL, 'M', '0', '0', NULL, 'Connection', 0, 0, 'system'),
    (3, '库存管理', 5, 1, 'inventory', NULL, 'M', '0', '0', NULL, 'Box', 0, 0, 'system'),
    (4, '采购管理', 5, 2, 'purchase', NULL, 'M', '0', '0', NULL, 'ShoppingCart', 0, 0, 'system'),

    -- ===================== 系统管理 =====================
    (100, '用户管理', 1, 1, 'user', 'system/user/index', 'C', '0', '0', 'system:user:list', 'User', 0, 0, 'system'),
    (101, '角色管理', 1, 2, 'role', 'system/role/index', 'C', '0', '0', 'system:role:list', 'UserFilled', 0, 0, 'system'),
    (102, '菜单管理', 1, 3, 'menu', 'system/menu/index', 'C', '0', '0', 'system:menu:list', 'Menu', 0, 0, 'system'),
    (103, '部门管理', 1, 4, 'dept', 'system/dept/index', 'C', '0', '0', 'system:dept:list', 'OfficeBuilding', 0, 0, 'system'),
    (104, '字典管理', 1, 5, 'dict', 'system/dict/index', 'C', '0', '0', 'system:dict:list', 'Notebook', 0, 0, 'system'),

    -- 用户管理按钮
    (1001, '用户查询', 100, 1, NULL, NULL, 'F', '0', '0', 'system:user:query', NULL, 0, 0, 'system'),
    (1002, '用户新增', 100, 2, NULL, NULL, 'F', '0', '0', 'system:user:add', NULL, 0, 0, 'system'),
    (1003, '用户修改', 100, 3, NULL, NULL, 'F', '0', '0', 'system:user:edit', NULL, 0, 0, 'system'),
    (1004, '用户删除', 100, 4, NULL, NULL, 'F', '0', '0', 'system:user:remove', NULL, 0, 0, 'system'),
    (1005, '重置密码', 100, 5, NULL, NULL, 'F', '0', '0', 'system:user:resetPwd', NULL, 0, 0, 'system'),
    -- 角色管理按钮
    (1011, '角色查询', 101, 1, NULL, NULL, 'F', '0', '0', 'system:role:query', NULL, 0, 0, 'system'),
    (1012, '角色新增', 101, 2, NULL, NULL, 'F', '0', '0', 'system:role:add', NULL, 0, 0, 'system'),
    (1013, '角色修改', 101, 3, NULL, NULL, 'F', '0', '0', 'system:role:edit', NULL, 0, 0, 'system'),
    (1014, '角色删除', 101, 4, NULL, NULL, 'F', '0', '0', 'system:role:remove', NULL, 0, 0, 'system'),
    -- 菜单管理按钮
    (1021, '菜单查询', 102, 1, NULL, NULL, 'F', '0', '0', 'system:menu:query', NULL, 0, 0, 'system'),
    (1022, '菜单新增', 102, 2, NULL, NULL, 'F', '0', '0', 'system:menu:add', NULL, 0, 0, 'system'),
    (1023, '菜单修改', 102, 3, NULL, NULL, 'F', '0', '0', 'system:menu:edit', NULL, 0, 0, 'system'),
    (1024, '菜单删除', 102, 4, NULL, NULL, 'F', '0', '0', 'system:menu:remove', NULL, 0, 0, 'system'),
    -- 部门管理按钮
    (1031, '部门查询', 103, 1, NULL, NULL, 'F', '0', '0', 'system:dept:query', NULL, 0, 0, 'system'),
    (1032, '部门新增', 103, 2, NULL, NULL, 'F', '0', '0', 'system:dept:add', NULL, 0, 0, 'system'),
    (1033, '部门修改', 103, 3, NULL, NULL, 'F', '0', '0', 'system:dept:edit', NULL, 0, 0, 'system'),
    (1034, '部门删除', 103, 4, NULL, NULL, 'F', '0', '0', 'system:dept:remove', NULL, 0, 0, 'system'),
    -- 字典管理按钮
    (1041, '字典查询', 104, 1, NULL, NULL, 'F', '0', '0', 'system:dict:query', NULL, 0, 0, 'system'),
    (1042, '字典新增', 104, 2, NULL, NULL, 'F', '0', '0', 'system:dict:add', NULL, 0, 0, 'system'),
    (1043, '字典修改', 104, 3, NULL, NULL, 'F', '0', '0', 'system:dict:edit', NULL, 0, 0, 'system'),
    (1044, '字典删除', 104, 4, NULL, NULL, 'F', '0', '0', 'system:dict:remove', NULL, 0, 0, 'system'),

    -- ===================== 基础资料 =====================
    (200, '物料管理', 2, 1, 'material', 'mdm/material/index', 'C', '0', '0', 'mdm:material:list', 'Goods', 0, 0, 'system'),
    (201, '物料分类', 2, 2, 'materialCategory', 'mdm/materialCategory/index', 'C', '0', '0', 'mdm:materialCategory:list', 'Grid', 0, 0, 'system'),
    (202, '客户管理', 2, 3, 'customer', 'mdm/customer/index', 'C', '0', '0', 'mdm:customer:list', 'Avatar', 0, 0, 'system'),
    (203, '供应商管理', 2, 4, 'supplier', 'mdm/supplier/index', 'C', '0', '0', 'mdm:supplier:list', 'OfficeBuilding', 0, 0, 'system'),

    -- 物料管理按钮
    (2001, '物料查询', 200, 1, NULL, NULL, 'F', '0', '0', 'mdm:material:query', NULL, 0, 0, 'system'),
    (2002, '物料新增', 200, 2, NULL, NULL, 'F', '0', '0', 'mdm:material:add', NULL, 0, 0, 'system'),
    (2003, '物料修改', 200, 3, NULL, NULL, 'F', '0', '0', 'mdm:material:edit', NULL, 0, 0, 'system'),
    (2004, '物料删除', 200, 4, NULL, NULL, 'F', '0', '0', 'mdm:material:remove', NULL, 0, 0, 'system'),
    -- 物料分类按钮
    (2011, '分类查询', 201, 1, NULL, NULL, 'F', '0', '0', 'mdm:materialCategory:query', NULL, 0, 0, 'system'),
    (2012, '分类新增', 201, 2, NULL, NULL, 'F', '0', '0', 'mdm:materialCategory:add', NULL, 0, 0, 'system'),
    (2013, '分类修改', 201, 3, NULL, NULL, 'F', '0', '0', 'mdm:materialCategory:edit', NULL, 0, 0, 'system'),
    (2014, '分类删除', 201, 4, NULL, NULL, 'F', '0', '0', 'mdm:materialCategory:remove', NULL, 0, 0, 'system'),
    -- 客户管理按钮
    (2021, '客户查询', 202, 1, NULL, NULL, 'F', '0', '0', 'mdm:customer:query', NULL, 0, 0, 'system'),
    (2022, '客户新增', 202, 2, NULL, NULL, 'F', '0', '0', 'mdm:customer:add', NULL, 0, 0, 'system'),
    (2023, '客户修改', 202, 3, NULL, NULL, 'F', '0', '0', 'mdm:customer:edit', NULL, 0, 0, 'system'),
    (2024, '客户删除', 202, 4, NULL, NULL, 'F', '0', '0', 'mdm:customer:remove', NULL, 0, 0, 'system'),
    -- 供应商管理按钮
    (2031, '供应商查询', 203, 1, NULL, NULL, 'F', '0', '0', 'mdm:supplier:query', NULL, 0, 0, 'system'),
    (2032, '供应商新增', 203, 2, NULL, NULL, 'F', '0', '0', 'mdm:supplier:add', NULL, 0, 0, 'system'),
    (2033, '供应商修改', 203, 3, NULL, NULL, 'F', '0', '0', 'mdm:supplier:edit', NULL, 0, 0, 'system'),
    (2034, '供应商删除', 203, 4, NULL, NULL, 'F', '0', '0', 'mdm:supplier:remove', NULL, 0, 0, 'system'),
    -- 仓库管理按钮
    (2041, '仓库查询', 204, 1, NULL, NULL, 'F', '0', '0', 'mdm:warehouse:query', NULL, 0, 0, 'system'),
    (2042, '仓库新增', 204, 2, NULL, NULL, 'F', '0', '0', 'mdm:warehouse:add', NULL, 0, 0, 'system'),
    (2043, '仓库修改', 204, 3, NULL, NULL, 'F', '0', '0', 'mdm:warehouse:edit', NULL, 0, 0, 'system'),
    (2044, '仓库删除', 204, 4, NULL, NULL, 'F', '0', '0', 'mdm:warehouse:remove', NULL, 0, 0, 'system'),

    -- ===================== 库存管理 =====================
    -- 仓库属于库存域（库存按仓库归集、出入库单按仓库选择），所以放在这里，
    -- 而不是和物料/客户一起放在基础资料下。菜单 ID 204 沿用，只换了父级，
    -- 因此 2041-2044 四个按钮权限不用动。
    -- 入库单与出库单共用后端资源，所以两者的菜单权限串都是 inv:order:list，靠前端传 orderType 区分
    (204, '仓库管理', 3, 1, 'warehouse', 'mdm/warehouse/index', 'C', '0', '0', 'mdm:warehouse:list', 'House', 0, 0, 'system'),
    (300, '库存查询', 3, 2, 'stock', 'inventory/stock/index', 'C', '0', '0', 'inv:stock:list', 'Search', 0, 0, 'system'),
    (301, '入库单', 3, 3, 'inbound', 'inventory/order/index', 'C', '0', '0', 'inv:order:list', 'Download', 0, 0, 'system'),
    (302, '出库单', 3, 4, 'outbound', 'inventory/order/index', 'C', '0', '0', 'inv:order:list', 'Upload', 0, 0, 'system'),
    (303, '库存流水', 3, 5, 'stockLog', 'inventory/stockLog/index', 'C', '0', '0', 'inv:log:list', 'Tickets', 0, 0, 'system'),

    (3011, '单据查询', 301, 1, NULL, NULL, 'F', '0', '0', 'inv:order:query', NULL, 0, 0, 'system'),
    (3012, '单据新增', 301, 2, NULL, NULL, 'F', '0', '0', 'inv:order:add', NULL, 0, 0, 'system'),
    (3013, '单据修改', 301, 3, NULL, NULL, 'F', '0', '0', 'inv:order:edit', NULL, 0, 0, 'system'),
    (3014, '单据删除', 301, 4, NULL, NULL, 'F', '0', '0', 'inv:order:remove', NULL, 0, 0, 'system'),
    (3015, '单据审核', 301, 5, NULL, NULL, 'F', '0', '0', 'inv:order:audit', NULL, 0, 0, 'system'),

    -- ===================== 采购管理 =====================
    (400, '采购订单', 4, 1, 'order', 'purchase/order/index', 'C', '0', '0', 'pur:order:list', 'Document', 0, 0, 'system'),
    (401, '采购收货', 4, 2, 'receipt', 'purchase/receipt/index', 'C', '0', '0', 'pur:receipt:list', 'Van', 0, 0, 'system'),

    (4001, '订单查询', 400, 1, NULL, NULL, 'F', '0', '0', 'pur:order:query', NULL, 0, 0, 'system'),
    (4002, '订单新增', 400, 2, NULL, NULL, 'F', '0', '0', 'pur:order:add', NULL, 0, 0, 'system'),
    (4003, '订单修改', 400, 3, NULL, NULL, 'F', '0', '0', 'pur:order:edit', NULL, 0, 0, 'system'),
    (4004, '订单删除', 400, 4, NULL, NULL, 'F', '0', '0', 'pur:order:remove', NULL, 0, 0, 'system'),
    (4005, '订单审核', 400, 5, NULL, NULL, 'F', '0', '0', 'pur:order:audit', NULL, 0, 0, 'system'),

    (4011, '收货查询', 401, 1, NULL, NULL, 'F', '0', '0', 'pur:receipt:query', NULL, 0, 0, 'system'),
    (4012, '收货新增', 401, 2, NULL, NULL, 'F', '0', '0', 'pur:receipt:add', NULL, 0, 0, 'system'),
    (4013, '收货删除', 401, 3, NULL, NULL, 'F', '0', '0', 'pur:receipt:remove', NULL, 0, 0, 'system'),
    (4014, '确认收货', 401, 4, NULL, NULL, 'F', '0', '0', 'pur:receipt:confirm', NULL, 0, 0, 'system')

ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`),
                        `parent_id` = VALUES(`parent_id`),
                        `order_num` = VALUES(`order_num`),
                        `path`      = VALUES(`path`),
                        `component` = VALUES(`component`),
                        `menu_type` = VALUES(`menu_type`),
                        `visible`   = VALUES(`visible`),
                        `status`    = VALUES(`status`),
                        `perms`     = VALUES(`perms`),
                        `icon`      = VALUES(`icon`);
