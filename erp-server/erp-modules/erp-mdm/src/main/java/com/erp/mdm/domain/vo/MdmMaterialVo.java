package com.erp.mdm.domain.vo;

import com.erp.mdm.domain.MdmMaterial;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 物料列表出参。
 * <p>
 * 在实体全部字段的基础上，额外带出联表/字典翻译出来的展示字段
 * （categoryName / unitName / materialTypeName / statusName），
 * 前端表格直接渲染，不用再逐行调字典接口。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MdmMaterialVo extends MdmMaterial {

    /** 分类名称（联 mdm_material_category） */
    private String categoryName;

    /** 计量单位名称（字典 mdm_unit 的 label） */
    private String unitName;

    /** 物料类型名称（字典 mdm_material_type 的 label） */
    private String materialTypeName;

    /** 状态名称（字典 sys_normal_disable 的 label） */
    private String statusName;
}
