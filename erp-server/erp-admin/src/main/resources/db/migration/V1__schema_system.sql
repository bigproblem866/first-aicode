-- =============================================================================
-- V1 系统管理模块建表（sys_*）
-- -----------------------------------------------------------------------------
-- 执行方式：应用启动时由 Flyway 自动执行，不需要手工跑。
-- 命名：V{序号}__{说明}.sql，两个下划线。序号全局唯一，切勿重复。
--
-- 建表规范（所有业务表一致）：
--   1. 必带六个基础字段：id / create_by / create_time / update_by / update_time / is_deleted
--      其中 create_by/update_by 由 com.erp.framework.mybatis.ErpMetaObjectHandler 自动填充，
--      数据库的 DEFAULT 只兜住时间字段，兜不住"谁操作的"。
--   2. 业务唯一键必须建 UNIQUE（如用户名、字典类型），不能只靠代码里查重
--      —— 并发下"先查再插"必然重复。
--   3. 逻辑删除后所有查询都会带 is_deleted = 0，所以 is_deleted 需要索引。
--   4. 金额、数量统一 DECIMAL(18,4)；不要用 FLOAT/DOUBLE（二进制浮点算钱会出误差）。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 部门
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dept`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '部门ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父部门ID，顶级为0',
    `ancestors`   VARCHAR(500) NOT NULL DEFAULT '' COMMENT '祖级列表，逗号分隔，如 0,100,101。用于"查某部门及所有下级"',
    `dept_name`   VARCHAR(50)  NOT NULL COMMENT '部门名称',
    `order_num`   INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `leader`      VARCHAR(30)           DEFAULT NULL COMMENT '负责人',
    `phone`       VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
    `email`       VARCHAR(64)           DEFAULT NULL COMMENT '邮箱',
    `status`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `remark`      VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_dept_parent` (`parent_id`),
    KEY `idx_dept_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='部门表';

-- -----------------------------------------------------------------------------
-- 用户
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `dept_id`     BIGINT                DEFAULT NULL COMMENT '部门ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `nick_name`   VARCHAR(50)  NOT NULL COMMENT '用户昵称',
    `email`       VARCHAR(64)           DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `sex`         CHAR(1)      NOT NULL DEFAULT '2' COMMENT '性别：0男 1女 2未知',
    `avatar`      VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
    -- BCrypt 哈希固定 60 字符，留 100 有余量（将来换算法也不用改表）
    `password`    VARCHAR(100) NOT NULL COMMENT '密码哈希（BCrypt，绝不存明文）',
    `status`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `login_ip`    VARCHAR(64)           DEFAULT NULL COMMENT '最后登录IP',
    `login_date`  DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    `remark`      VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    -- 用户名唯一。注意 MySQL 的 UNIQUE 对逻辑删除不友好：账号被"删除"后同名校验仍会冲突，
    -- 这是刻意的取舍 —— 用户名是审计线索，不允许复用。
    UNIQUE KEY `uk_sys_user_username` (`username`),
    KEY `idx_sys_user_dept` (`dept_id`),
    KEY `idx_sys_user_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- -----------------------------------------------------------------------------
-- 角色
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name`   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    `role_key`    VARCHAR(100) NOT NULL COMMENT '角色标识，如 admin、warehouse_keeper',
    `role_sort`   INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `data_scope`  CHAR(1)      NOT NULL DEFAULT '1' COMMENT '数据范围：1全部 2本部门及以下 3本部门 4仅本人（本期只实现1，其余预留）',
    `status`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `remark`      VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_key` (`role_key`),
    KEY `idx_sys_role_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

-- -----------------------------------------------------------------------------
-- 菜单 / 权限点
-- 这张表同时承担两件事：左侧导航渲染 + 按钮级权限点，是整个前端动态路由的数据来源。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_menu`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `menu_name`   VARCHAR(50) NOT NULL COMMENT '菜单名称',
    `parent_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '父菜单ID，顶级为0',
    `order_num`   INT         NOT NULL DEFAULT 0 COMMENT '显示顺序',
    -- path/component 的组合决定前端路由：
    --   目录(M)：path='system'，component=NULL，前端挂到 Layout 下
    --   菜单(C)：path='user'，component='system/user/index'，前端映射到 views/system/user/index.vue
    --   按钮(F)：path 为空，只提供 perms
    `path`        VARCHAR(200)         DEFAULT NULL COMMENT '路由地址',
    `component`   VARCHAR(255)         DEFAULT NULL COMMENT '前端组件路径（相对 views/，不带 .vue）',
    `menu_type`   CHAR(1)     NOT NULL COMMENT '菜单类型：M目录 C菜单 F按钮',
    `visible`     CHAR(1)     NOT NULL DEFAULT '0' COMMENT '是否显示：0显示 1隐藏',
    `status`      CHAR(1)     NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    -- 权限串，格式 模块:资源:动作，与后端 @PreAuthorize("@ss.hasPermi('system:user:add')") 里的字符串逐字对应
    `perms`       VARCHAR(100)         DEFAULT NULL COMMENT '权限标识',
    `icon`        VARCHAR(100)         DEFAULT NULL COMMENT '菜单图标（Element Plus 图标名，不含 el-icon- 前缀）',
    `is_frame`    TINYINT     NOT NULL DEFAULT 0 COMMENT '是否外链：0否 1是',
    `is_cache`    TINYINT     NOT NULL DEFAULT 0 COMMENT '是否缓存该页面（keep-alive）：0缓存 1不缓存',
    `remark`      VARCHAR(500)         DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64)          DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)          DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_menu_parent` (`parent_id`),
    KEY `idx_sys_menu_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='菜单权限表';

-- -----------------------------------------------------------------------------
-- 用户-角色 关联
-- 纯关联表，不加逻辑删除字段：取消授权就是物理删除一行，留历史没有意义
-- （真要审计"谁什么时候取消了授权"，应该在操作日志表里记，而不是靠这张表）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_user_role_role` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';

-- -----------------------------------------------------------------------------
-- 角色-菜单 关联
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_menu`
(
    `role_id` BIGINT NOT NULL COMMENT '角色ID',
    `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (`role_id`, `menu_id`),
    KEY `idx_role_menu_menu` (`menu_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='角色菜单关联表';

-- -----------------------------------------------------------------------------
-- 字典类型
-- 字典必须拆成「类型」+「数据」两张表。只用一张表的话无法区分
-- "性别"和"客户类型"里都叫 value='1' 的两条数据，下拉框会串数据。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_type`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
    `dict_name`   VARCHAR(100) NOT NULL COMMENT '字典名称，如：用户性别',
    `dict_type`   VARCHAR(100) NOT NULL COMMENT '字典类型标识，如：sys_user_sex',
    `status`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `remark`      VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_dict_type` (`dict_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='字典类型表';

-- -----------------------------------------------------------------------------
-- 字典数据
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_data`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '字典数据ID',
    `dict_sort`   INT          NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `dict_label`  VARCHAR(100) NOT NULL COMMENT '字典标签（界面显示的文字）',
    `dict_value`  VARCHAR(100) NOT NULL COMMENT '字典键值（存进业务表的值）',
    `dict_type`   VARCHAR(100) NOT NULL COMMENT '所属字典类型（对应 sys_dict_type.dict_type）',
    `css_class`   VARCHAR(100)          DEFAULT NULL COMMENT '样式类名',
    -- Element Plus 的 tag 类型：success/info/warning/danger，用于表格里把状态渲染成彩色标签
    `list_class`  VARCHAR(100)          DEFAULT NULL COMMENT '表格回显样式',
    `is_default`  CHAR(1)      NOT NULL DEFAULT 'N' COMMENT '是否默认选中：Y是 N否',
    `status`      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `remark`      VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_dict_data_type` (`dict_type`),
    KEY `idx_dict_data_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='字典数据表';
