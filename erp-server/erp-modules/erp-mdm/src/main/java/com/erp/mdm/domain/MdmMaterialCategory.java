package com.erp.mdm.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 物料分类实体，对应表 mdm_material_category。树形结构，顶级 parent_id 为 0。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_material_category")
public class MdmMaterialCategory extends BaseEntity {

    /** 父分类ID，顶级为 0。用 @NotNull：0 是合法值，不能用字符串的 @NotBlank */
    @NotNull(message = "上级分类不能为空")
    private Long parentId;

    /**
     * 祖级列表，如 "0,100"。
     * <p>
     * 由 Service 在新增/修改时自动维护（父的 ancestors + "," + 父id），
     * 前端不传、也不允许传 —— 传错会导致「查某分类及所有下级」整棵树都查错。
     */
    private String ancestors;

    /** 分类编码，唯一 */
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    /** 显示顺序 */
    private Integer orderNum;

    /** 状态：0正常 1停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 子分类，构造分类树时使用 */
    @TableField(exist = false)
    private List<MdmMaterialCategory> children = new ArrayList<>();
}
