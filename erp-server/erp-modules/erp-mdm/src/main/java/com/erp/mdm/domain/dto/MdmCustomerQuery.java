package com.erp.mdm.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户列表查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmCustomerQuery extends PageQuery {

    /** 客户编码，模糊匹配 */
    private String customerCode;

    /** 客户名称，模糊匹配 */
    private String customerName;

    /** 客户类型（字典 mdm_customer_type） */
    private String customerType;

    /** 状态：0正常 1停用 */
    private String status;
}
