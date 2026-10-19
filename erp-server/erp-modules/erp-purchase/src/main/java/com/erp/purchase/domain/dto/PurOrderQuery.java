package com.erp.purchase.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采购订单查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurOrderQuery extends PageQuery {

    /** 订单号，模糊匹配 */
    private String orderNo;

    /** 供应商ID */
    private Long supplierId;

    /** 状态 */
    private String status;
}
