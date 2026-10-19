package com.erp.inventory.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存台账查询条件。
 * <p>
 * 没有按物料编码/名称的关键字条件：那需要 join mdm_material，
 * 而本模块不允许 join mdm 表。需要按物料筛选时，前端先用物料下拉拿到 materialId 再传进来。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvStockQuery extends PageQuery {

    /** 仓库ID */
    private Long warehouseId;

    /** 物料ID */
    private Long materialId;
}
