package com.erp.mdm.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 仓库列表查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmWarehouseQuery extends PageQuery {

    /** 仓库编码，模糊匹配 */
    private String warehouseCode;

    /** 仓库名称，模糊匹配 */
    private String warehouseName;

    /** 状态：0正常 1停用 */
    private String status;
}
