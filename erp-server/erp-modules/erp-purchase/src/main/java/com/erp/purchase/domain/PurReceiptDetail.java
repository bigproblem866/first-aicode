package com.erp.purchase.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 采购收货单明细实体，对应表 pur_receipt_detail。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pur_receipt_detail")
public class PurReceiptDetail extends BaseEntity {

    /** 收货单ID */
    private Long receiptId;

    /** 来源采购订单明细ID：收货数量要累加到"哪一行订单"上靠它定位 */
    private Long orderDetailId;

    /** 物料ID */
    private Long materialId;

    /** 本次收货数量 */
    private BigDecimal qty;

    /** 备注 */
    private String remark;

    /** 物料名称，非数据库字段，由 MdmApi 填充供展示 */
    @TableField(exist = false)
    private String materialName;
}
