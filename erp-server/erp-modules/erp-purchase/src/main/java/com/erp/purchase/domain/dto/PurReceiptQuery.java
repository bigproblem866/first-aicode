package com.erp.purchase.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采购收货单查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurReceiptQuery extends PageQuery {

    /** 收货单号，模糊匹配 */
    private String receiptNo;

    /** 来源采购订单ID */
    private Long orderId;

    /** 供应商ID */
    private Long supplierId;

    /** 状态 */
    private String status;
}
