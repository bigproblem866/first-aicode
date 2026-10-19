package com.erp.mdm.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料列表查询条件。继承 {@link PageQuery} 即自动获得 pageNum / pageSize / 排序参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmMaterialQuery extends PageQuery {

    /** 物料编码，模糊匹配 */
    private String materialCode;

    /** 物料名称，模糊匹配 */
    private String materialName;

    /** 物料类型（字典 mdm_material_type），精确匹配 */
    private String materialType;

    /** 物料分类ID */
    private Long categoryId;

    /** 状态：0正常 1停用 */
    private String status;
}
