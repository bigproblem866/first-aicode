-- =============================================================================
-- V3 库存模块建表（inv_*）
-- -----------------------------------------------------------------------------
-- 库存的三张核心表及各自职责，理解这层分工是理解整个 ERP 的关键：
--
--   inv_stock       库存台账 —— 只存"现在有多少"，一个仓库+物料一行，会被反复更新
--   inv_stock_log   库存流水 —— 只增不改的流水账，记录每次变动前后的数量
--   inv_order       出入库单据 —— 业务动作的凭证，审核后才会去动台账和流水
--
-- 为什么必须有流水表：台账只反映结果，出了问题（数量对不上）时无法回溯是哪一单改的。
-- 流水表是库存模块唯一的"对账依据"，任何一次数量变动都必须写一条流水。
-- 台账的 before_qty/after_qty 与流水一一对应，可用于日终核对。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 库存台账
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inv_stock`
(
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    `warehouse_id` BIGINT         NOT NULL COMMENT '仓库ID',
    `material_id`  BIGINT         NOT NULL COMMENT '物料ID',
    `qty`          DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '现存数量',
    `remark`       VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    `create_by`    VARCHAR(64)             DEFAULT NULL COMMENT '创建人',
    `create_time`  DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)             DEFAULT NULL COMMENT '更新人',
    `update_time`  DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    -- 一个仓库 + 一个物料只能有一行台账。这条唯一索引是并发下库存不错乱的根本保证：
    -- 出库时用 UPDATE ... SET qty = qty - ? WHERE warehouse_id=? AND material_id=? AND qty >= ?
    -- 靠数据库行锁来保证不会超卖，而不是靠应用层先查再改（那样并发必错）。
    UNIQUE KEY `uk_inv_stock_wh_material` (`warehouse_id`, `material_id`),
    KEY `idx_inv_stock_material` (`material_id`),
    KEY `idx_inv_stock_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存台账表';

-- -----------------------------------------------------------------------------
-- 库存流水
-- 设计上只增不改不删：即便单据作废，也是再写一条反向流水，而不是删掉原记录。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inv_stock_log`
(
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    `warehouse_id` BIGINT         NOT NULL COMMENT '仓库ID',
    `material_id`  BIGINT         NOT NULL COMMENT '物料ID',
    `biz_type`     VARCHAR(20)    NOT NULL COMMENT '业务类型：IN入库 OUT出库 ADJUST盘点调整',
    `qty`          DECIMAL(18, 4) NOT NULL COMMENT '变动数量，入库为正、出库为负',
    `before_qty`   DECIMAL(18, 4) NOT NULL COMMENT '变动前数量',
    `after_qty`    DECIMAL(18, 4) NOT NULL COMMENT '变动后数量',
    -- 溯源三件套：从一条流水必须能点回到产生它的那张单据
    `source_type`  VARCHAR(30)             DEFAULT NULL COMMENT '来源单据类型：INV_ORDER出入库单 PUR_RECEIPT采购收货',
    `source_id`    BIGINT                  DEFAULT NULL COMMENT '来源单据ID',
    `source_no`    VARCHAR(50)             DEFAULT NULL COMMENT '来源单据号，直接展示给用户看',
    `biz_date`     DATE                    DEFAULT NULL COMMENT '业务日期',
    `remark`       VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    `create_by`    VARCHAR(64)             DEFAULT NULL COMMENT '创建人',
    `create_time`  DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)             DEFAULT NULL COMMENT '更新人',
    `update_time`  DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_inv_log_wh_material` (`warehouse_id`, `material_id`),
    KEY `idx_inv_log_material` (`material_id`),
    KEY `idx_inv_log_source` (`source_type`, `source_id`),
    KEY `idx_inv_log_biz_date` (`biz_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存流水表';

-- -----------------------------------------------------------------------------
-- 出入库单（单头）
-- 入库单与出库单共用一张表，用 order_type 区分：
-- 两者的字段、审核逻辑、对库存的影响方式完全对称，拆成两张表只会让
-- Service 层出现两份几乎一样的代码。
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inv_order`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '出入库单ID',
    `order_no`     VARCHAR(50)  NOT NULL COMMENT '单据号，如 IN20261016001',
    `order_type`   VARCHAR(10)  NOT NULL COMMENT '单据类型：IN入库 OUT出库',
    `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
    `biz_date`     DATE         NOT NULL COMMENT '业务日期',
    `status`       CHAR(1)      NOT NULL DEFAULT '0' COMMENT '状态：0草稿 1已审核 2已作废',
    `source_type`  VARCHAR(30)           DEFAULT NULL COMMENT '来源单据类型：MANUAL手工录入 PUR_RECEIPT采购收货',
    `source_no`    VARCHAR(50)           DEFAULT NULL COMMENT '来源单据号',
    `remark`       VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    `audit_by`     VARCHAR(64)           DEFAULT NULL COMMENT '审核人',
    `audit_time`   DATETIME              DEFAULT NULL COMMENT '审核时间',
    `create_by`    VARCHAR(64)           DEFAULT NULL COMMENT '创建人',
    `create_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)           DEFAULT NULL COMMENT '更新人',
    `update_time`  DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inv_order_no` (`order_no`),
    KEY `idx_inv_order_wh` (`warehouse_id`),
    KEY `idx_inv_order_status` (`status`),
    KEY `idx_inv_order_biz_date` (`biz_date`),
    KEY `idx_inv_order_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='出入库单主表';

-- -----------------------------------------------------------------------------
-- 出入库单明细
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inv_order_detail`
(
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`    BIGINT         NOT NULL COMMENT '出入库单ID',
    `material_id` BIGINT         NOT NULL COMMENT '物料ID',
    `qty`         DECIMAL(18, 4) NOT NULL COMMENT '数量（恒为正数，方向由单据的 order_type 决定）',
    `price`       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '单价',
    `amount`      DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '金额 = 数量 × 单价',
    `remark`      VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    `create_by`   VARCHAR(64)             DEFAULT NULL COMMENT '创建人',
    `create_time` DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`   VARCHAR(64)             DEFAULT NULL COMMENT '更新人',
    `update_time` DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_inv_detail_order` (`order_id`),
    KEY `idx_inv_detail_material` (`material_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='出入库单明细表';
