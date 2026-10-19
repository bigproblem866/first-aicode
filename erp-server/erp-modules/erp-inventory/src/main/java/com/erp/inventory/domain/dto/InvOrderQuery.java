package com.erp.inventory.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 出入库单查询条件。orderType 传 IN/OUT 区分入库单/出库单菜单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvOrderQuery extends PageQuery {

    /** 单据号，模糊匹配 */
    private String orderNo;

    /** 单据类型：IN入库 OUT出库 */
    private String orderType;

    /** 状态：0草稿 1已审核 2已作废 */
    private String status;

    /** 仓库ID */
    private Long warehouseId;
}
