-- =============================================================================
-- V4 采购模块建表（pur_*）
-- -----------------------------------------------------------------------------
-- 本期实现的主链路（也是用来演示"跨模块调用"的样板）：
--
--   采购订单(已审核) → 采购收货(选择订单) → 确认收货
--        → 生成采购入库单(inv_order, source_type=PUR_RECEIPT)
--        → 增加库存台账(inv_stock) + 写库存流水(inv_stock_log)
--
-- 注意 pur_receipt_detail.received_qty 反写回 pur_order_detail，
-- 用于判断订单是"部分收货"还是"已完成"。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 采购订单（单头）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pur_order`
(
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '采购订单ID',
    `order_no`     VARCHAR(50)    NOT NULL COMMENT '订单号，如 PO20261016001',
    `supplier_id`  BIGINT         NOT NULL COMMENT '供应商ID',
    `order_date`   DATE           NOT NULL COMMENT '下单日期',
    `expect_date`  DATE                    DEFAULT NULL COMMENT '期望到货日期',
    `status`       CHAR(1)        NOT NULL DEFAULT '0' COMMENT '状态：0草稿 1已审核 2部分收货 3已完成 4已作废',
    `total_amount` DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '订单总金额（由明细汇总，不允许手工填）',
    `remark`       VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    `audit_by`     VARCHAR(64)             DEFAULT NULL COMMENT '审核人',
    `audit_time`   DATETIME                DEFAULT NULL COMMENT '审核时间',
    `create_by`    VARCHAR(64)             DEFAULT NULL COMMENT '创建人',
    `create_time`  DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)             DEFAULT NULL COMMENT '更新人',
    `update_time`  DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pur_order_no` (`order_no`),
    KEY `idx_pur_order_supplier` (`supplier_id`),
    KEY `idx_pur_order_status` (`status`),
    KEY `idx_pur_order_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='采购订单主表';

-- -----------------------------------------------------------------------------
-- 采购订单明细
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pur_order_detail`
(
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`     BIGINT         NOT NULL COMMENT '采购订单ID',
    `material_id`  BIGINT         NOT NULL COMMENT '物料ID',
    `qty`          DECIMAL(18, 4) NOT NULL COMMENT '采购数量',
    `price`        DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '采购单价',
    `amount`       DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '金额 = 数量 × 单价',
    -- 已收货数量。收货时累加，用于判断订单是否收货完毕；
    -- 允许部分收货是采购买的常态（供应商分批到货），所以不能只用一个"是否收货"的布尔值
    `received_qty` DECIMAL(18, 4) NOT NULL DEFAULT 0 COMMENT '已收货数量',
    `remark`       VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    `create_by`    VARCHAR(64)             DEFAULT NULL COMMENT '创建人',
    `create_time`  DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)             DEFAULT NULL COMMENT '更新人',
    `update_time`  DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_pur_detail_order` (`order_id`),
    KEY `idx_pur_detail_material` (`material_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='采购订单明细表';

-- -----------------------------------------------------------------------------
-- 采购收货单（单头）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pur_receipt`
(
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '收货单ID',
    `receipt_no`   VARCHAR(50) NOT NULL COMMENT '收货单号，如 PR20261016001',
    `order_id`     BIGINT      NOT NULL COMMENT '来源采购订单ID',
    `order_no`     VARCHAR(50) NOT NULL COMMENT '来源采购订单号（冗余存一份，列表页不用再联表）',
    `supplier_id`  BIGINT      NOT NULL COMMENT '供应商ID',
    `warehouse_id` BIGINT      NOT NULL COMMENT '收货仓库ID',
    `receipt_date` DATE        NOT NULL COMMENT '收货日期',
    `status`       CHAR(1)     NOT NULL DEFAULT '0' COMMENT '状态：0草稿 1已确认（已入库） 2已作废',
    -- 生成的入库单ID/单号，用于从收货单直接跳转到入库单查看
    `inv_order_id` BIGINT               DEFAULT NULL COMMENT '生成的入库单ID',
    `inv_order_no` VARCHAR(50)          DEFAULT NULL COMMENT '生成的入库单号',
    `remark`       VARCHAR(500)         DEFAULT NULL COMMENT '备注',
    `create_by`    VARCHAR(64)          DEFAULT NULL COMMENT '创建人',
    `create_time`  DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`    VARCHAR(64)          DEFAULT NULL COMMENT '更新人',
    `update_time`  DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pur_receipt_no` (`receipt_no`),
    KEY `idx_pur_receipt_order` (`order_id`),
    KEY `idx_pur_receipt_is_deleted` (`is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='采购收货单主表';

-- -----------------------------------------------------------------------------
-- 采购收货单明细
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `pur_receipt_detail`
(
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `receipt_id`      BIGINT         NOT NULL COMMENT '收货单ID',
    -- 指回采购订单明细行：收货数量要累加到"哪一行订单"上，靠这个字段定位
    `order_detail_id` BIGINT         NOT NULL COMMENT '来源采购订单明细ID',
    `material_id`     BIGINT         NOT NULL COMMENT '物料ID',
    `qty`             DECIMAL(18, 4) NOT NULL COMMENT '本次收货数量',
    `remark`          VARCHAR(500)            DEFAULT NULL COMMENT '备注',
    `create_by`       VARCHAR(64)             DEFAULT NULL COMMENT '创建人',
    `create_time`     DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by`       VARCHAR(64)             DEFAULT NULL COMMENT '更新人',
    `update_time`     DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1删除',
    PRIMARY KEY (`id`),
    KEY `idx_pur_receipt_detail_receipt` (`receipt_id`),
    KEY `idx_pur_receipt_detail_order_detail` (`order_detail_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='采购收货单明细表';
