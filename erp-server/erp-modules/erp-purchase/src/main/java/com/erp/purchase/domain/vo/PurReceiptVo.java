package com.erp.purchase.domain.vo;

import com.erp.purchase.domain.PurReceipt;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采购收货单列表出参。供应商/仓库名称由 MdmApi 填充。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PurReceiptVo extends PurReceipt {

    /** 供应商名称（MdmApi 填充） */
    private String supplierName;

    /** 仓库名称（MdmApi 填充） */
    private String warehouseName;
}
