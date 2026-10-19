package com.erp.purchase.domain.vo;

import com.erp.purchase.domain.PurOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采购订单列表出参。供应商名称由 MdmApi 填充，不 join mdm 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurOrderVo extends PurOrder {

    /** 供应商名称（MdmApi 填充） */
    private String supplierName;
}
