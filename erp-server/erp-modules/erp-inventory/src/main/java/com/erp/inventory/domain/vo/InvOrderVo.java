package com.erp.inventory.domain.vo;

import com.erp.inventory.domain.InvOrder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 出入库单列表出参。仓库名称由 MdmApi 填充（不 join mdm_warehouse）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvOrderVo extends InvOrder {

    /** 仓库名称（MdmApi 填充） */
    private String warehouseName;
}
