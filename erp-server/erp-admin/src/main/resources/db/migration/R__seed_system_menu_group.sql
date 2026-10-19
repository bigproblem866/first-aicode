-- =============================================================================
-- 种子数据：菜单的功能分组
-- -----------------------------------------------------------------------------
-- 只影响前端「功能导航页」的分区标题，不改路由、不改权限。
-- 按菜单 ID 更新，幂等；后续新增菜单时在这里补一行即可。
--
-- 分组思路参考金蝶：把同一目录下的功能按业务环节分开，避免几十个链接平铺。
-- =============================================================================

-- 系统管理
UPDATE `sys_menu` SET `group_name` = '组织与权限' WHERE `id` IN (100, 101, 102, 103) AND `is_deleted` = 0;
UPDATE `sys_menu` SET `group_name` = '系统配置'   WHERE `id` IN (104) AND `is_deleted` = 0;

-- 基础资料
UPDATE `sys_menu` SET `group_name` = '物料'       WHERE `id` IN (200, 201) AND `is_deleted` = 0;
UPDATE `sys_menu` SET `group_name` = '往来单位'   WHERE `id` IN (202, 203) AND `is_deleted` = 0;

-- 库存管理
UPDATE `sys_menu` SET `group_name` = '基础资料'   WHERE `id` IN (204) AND `is_deleted` = 0;
UPDATE `sys_menu` SET `group_name` = '库存查询'   WHERE `id` IN (300, 303) AND `is_deleted` = 0;
UPDATE `sys_menu` SET `group_name` = '库存业务'   WHERE `id` IN (301, 302) AND `is_deleted` = 0;

-- 采购管理
UPDATE `sys_menu` SET `group_name` = '采购业务'   WHERE `id` IN (400, 401) AND `is_deleted` = 0;
