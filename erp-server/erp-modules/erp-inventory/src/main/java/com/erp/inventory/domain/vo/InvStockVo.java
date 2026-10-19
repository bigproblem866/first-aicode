package com.erp.inventory.domain.vo;

import com.erp.inventory.domain.InvStock;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存台账出参。
 * <p>
 * materialCode / materialName / unit 与 warehouseName <b>不是 join 出来的</b>，
 * 而是查出台账行后通过 {@code com.erp.mdm.api.MdmApi} 批量填充的（见 InvStockServiceImpl）。
 * 这是本项目演示「模块边界」的关键：inventory 里没有任何一句 SQL 触碰 mdm_* 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvStockVo extends InvStock {

    /** 物料编码（MdmApi 填充） */
    private String materialCode;

    /** 物料名称（MdmApi 填充） */
    private String materialName;

    /** 计量单位（MdmApi 填充） */
    private String unit;

    /** 仓库名称（MdmApi 填充） */
    private String warehouseName;
}
