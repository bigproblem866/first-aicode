package com.erp.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 出入库单明细实体，对应表 inv_order_detail。
 * <p>
 * qty 恒为正数，方向由单据的 orderType 决定 —— 拆成正负会让「修改单据类型」变成一件危险的事。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_order_detail")
public class InvOrderDetail extends BaseEntity {

    /** 出入库单ID */
    private Long orderId;

    /** 物料ID */
    private Long materialId;

    /** 数量（恒为正数） */
    private BigDecimal qty;

    /** 单价 */
    private BigDecimal price;

    /** 金额 = 数量 × 单价（后端算，不接受前端传） */
    private BigDecimal amount;

    /** 备注 */
    private String remark;
}
