-- =============================================================================
-- V2 主数据模块建表（mdm_*）
-- -----------------------------------------------------------------------------
-- 主数据（Master Data）是整个 ERP 的公共基础：物料、客户、供应商、仓库。
-- 这些数据被采购、库存、销售等所有业务表引用，所以：
--   1. 编码必须有唯一索引 —— 主数据重复是最难清理的脏数据
--   2. 预留 external_code / source_system 两个字段，将来对接金蝶等外部系统时
--      靠它们做映射，不用改表（本期不实现同步逻辑，只留位置）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 物料分类（树形）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mdm_material_category`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `parent_id`     BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID，顶级为0',
    `ancestors`     VARCHAR(500) NOT NULL DEFAULT '' COMMENT '祖级列表，逗号分隔，用于查某分类及所有下级',
    `category_code` VARCHAR(50) NOT NULL COMMENT '分类编码',
    `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
    `order_num`     INT         NOT NULL DEFAULT 0 COMMENT '显示顺序',
    `status`        CHAR(1)     NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `remark`        VARCHAR(500)         DEFAULT NULL COMMENT '备注',
    `create_by`     VARCHAR(64)          DEFAULT NULL COMMENT '创建人',
    `create_time`   DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)          DEFAULT NULL COMMENT '更新人',
    `update_time`   DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mdm_category_code` (`category_code`),
    KEY `idx_mdm_category_parent` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='物料分类表';

-- -----------------------------------------------------------------------------
-- 物料
-- 本表是「新增一个业务模块」的照抄模板，字段的取舍原则：
--   编码/名称/状态是必须的；规格型号品牌等按需；价格在这里只是「参考价」，
--   真正的采购价在采购单上（同一物料对不同供应商价格不同）。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mdm_material`
(
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '物料ID',
    `material_code`  VARCHAR(50)   NOT NULL COMMENT '物料编码',
    `material_name`  VARCHAR(200)  NOT NULL COMMENT '物料名称',
    `material_type`  VARCHAR(50)            DEFAULT NULL COMMENT '物料类型（字典 mdm_material_type）',
    `category_id`    BIGINT                 DEFAULT NULL COMMENT '物料分类ID',
    `unit`           VARCHAR(20)            DEFAULT NULL COMMENT '计量单位（字典 mdm_unit）',
    `spec`           VARCHAR(200)           DEFAULT NULL COMMENT '规格',
    `model`          VARCHAR(200)           DEFAULT NULL COMMENT '型号',
    `brand`          VARCHAR(100)           DEFAULT NULL COMMENT '品牌',
    `barcode`        VARCHAR(64)            DEFAULT NULL COMMENT '条形码',
    -- 安全库存：低于此值应在库存查询里高亮预警（本期只存不预警）
    `safety_stock`   DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '安全库存',
    `purchase_price` DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '参考采购价',
    `sale_price`     DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '参考销售价',
    `tax_rate`       DECIMAL(8, 4)  NOT NULL DEFAULT 0 COMMENT '税率，如 0.13 表示13%',
    `status`         CHAR(1)       NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `remark`         VARCHAR(500)           DEFAULT NULL COMMENT '备注',
    -- ↓ 为将来对接外部系统（金蝶账套等）预留，本期不用
    `external_code`  VARCHAR(64)            DEFAULT NULL COMMENT '外部系统编码',
    `source_system`  VARCHAR(50)            DEFAULT NULL COMMENT '来源系统标识',
    `create_by`      VARCHAR(64)            DEFAULT NULL COMMENT '创建人',
    `create_time`    DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(64)            DEFAULT NULL COMMENT '更新人',
    `update_time`    DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mdm_material_code` (`material_code`),
    KEY `idx_mdm_material_name` (`material_name`),
    KEY `idx_mdm_material_category` (`category_id`),
    KEY `idx_mdm_material_external` (`external_code`),
    KEY `idx_mdm_material_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='物料表';

-- -----------------------------------------------------------------------------
-- 客户
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mdm_customer`
(
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '客户ID',
    `customer_code` VARCHAR(50)    NOT NULL COMMENT '客户编码',
    `customer_name` VARCHAR(200)   NOT NULL COMMENT '客户名称',
    `customer_type` VARCHAR(50)             DEFAULT NULL COMMENT '客户类型（字典 mdm_customer_type）',
    `contact`       VARCHAR(50)             DEFAULT NULL COMMENT '联系人',
    `phone`         VARCHAR(20)             DEFAULT NULL COMMENT '联系电话',
    `email`         VARCHAR(64)             DEFAULT NULL COMMENT '邮箱',
    `address`       VARCHAR(255)            DEFAULT NULL COMMENT '地址',
    `tax_no`        VARCHAR(50)             DEFAULT NULL COMMENT '税号',
    `credit_limit`  DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '信用额度',
    `status`        CHAR(1)        NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `remark`        VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    `external_code` VARCHAR(64)             DEFAULT NULL COMMENT '外部系统编码',
    `source_system` VARCHAR(50)             DEFAULT NULL COMMENT '来源系统标识',
    `create_by`     VARCHAR(64)             DEFAULT NULL COMMENT '创建人',
    `create_time`   DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)             DEFAULT NULL COMMENT '更新人',
    `update_time`   DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mdm_customer_code` (`customer_code`),
    KEY `idx_mdm_customer_name` (`customer_name`),
    KEY `idx_mdm_customer_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='客户表';

-- -----------------------------------------------------------------------------
-- 供应商
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mdm_supplier`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
    `supplier_code` VARCHAR(50)  NOT NULL COMMENT '供应商编码',
    `supplier_name` VARCHAR(200) NOT NULL COMMENT '供应商名称',
    `contact`       VARCHAR(50)           DEFAULT NULL COMMENT '联系人',
    `phone`         VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
    `email`         VARCHAR(64)           DEFAULT NULL COMMENT '邮箱',
    `address`       VARCHAR(255)          DEFAULT NULL COMMENT '地址',
    `tax_no`        VARCHAR(50)           DEFAULT NULL COMMENT '税号',
    `status`        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `remark`        VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `external_code` VARCHAR(64)           DEFAULT NULL COMMENT '外部系统编码',
    `source_system` VARCHAR(50)           DEFAULT NULL COMMENT '来源系统标识',
    `create_by`     VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `create_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`     VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `update_time`   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mdm_supplier_code` (`supplier_code`),
    KEY `idx_mdm_supplier_name` (`supplier_name`),
    KEY `idx_mdm_supplier_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='供应商表';

-- -----------------------------------------------------------------------------
-- 仓库
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `mdm_warehouse`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
    `warehouse_code` VARCHAR(50)  NOT NULL COMMENT '仓库编码',
    `warehouse_name` VARCHAR(100) NOT NULL COMMENT '仓库名称',
    `address`        VARCHAR(255)          DEFAULT NULL COMMENT '仓库地址',
    `manager`        VARCHAR(50)           DEFAULT NULL COMMENT '仓管员',
    `phone`          VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
    `status`         CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0正常 1停用',
    `remark`         VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `create_by`      VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `create_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`      VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `update_time`    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mdm_warehouse_code` (`warehouse_code`),
    KEY `idx_mdm_warehouse_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='仓库表';
