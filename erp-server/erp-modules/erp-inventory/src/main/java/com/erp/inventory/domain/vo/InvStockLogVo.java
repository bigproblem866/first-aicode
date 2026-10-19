package com.erp.inventory.domain.vo;

import com.erp.inventory.domain.InvStockLog;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存流水出参。物料/仓库名称同样由 MdmApi 填充，不 join mdm 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvStockLogVo extends InvStockLog {

    /** 物料编码（MdmApi 填充） */
    private String materialCode;

    /** 物料名称（MdmApi 填充） */
    private String materialName;

    /** 计量单位（MdmApi 填充） */
    private String unit;

    /** 仓库名称（MdmApi 填充） */
    private String warehouseName;
}
