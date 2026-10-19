package com.erp.purchase.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 采购订单明细实体，对应表 pur_order_detail。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pur_order_detail")
public class PurOrderDetail extends BaseEntity {

    /** 采购订单ID */
    private Long orderId;

    /** 物料ID */
    private Long materialId;

    /** 采购数量 */
    private BigDecimal qty;

    /** 采购单价 */
    private BigDecimal price;

    /** 金额 = 数量 × 单价 */
    private BigDecimal amount;

    /** 已收货数量，收货时累加 */
    private BigDecimal receivedQty;

    /** 备注 */
    private String remark;

    /** 物料名称，非数据库字段，由 MdmApi 填充供展示 */
    @TableField(exist = false)
    private String materialName;

    /** 未收数量，非数据库字段，= 采购数量 - 已收货数量 */
    @TableField(exist = false)
    private BigDecimal unreceivedQty;
}
