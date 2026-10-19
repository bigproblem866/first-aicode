package com.erp.mdm.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商列表查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmSupplierQuery extends PageQuery {

    /** 供应商编码，模糊匹配 */
    private String supplierCode;

    /** 供应商名称，模糊匹配 */
    private String supplierName;

    /** 状态：0正常 1停用 */
    private String status;
}
