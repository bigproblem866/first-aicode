package com.erp.mdm.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料分类查询条件。分类列表返回树形、不分页，但仍继承 PageQuery 以便与规范统一。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmMaterialCategoryQuery extends PageQuery {

    /** 分类编码，模糊匹配 */
    private String categoryCode;

    /** 分类名称，模糊匹配 */
    private String categoryName;

    /** 状态：0正常 1停用 */
    private String status;
}
