package com.erp.mdm.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 物料分类新增/修改入参。
 * <p>
 * ancestors 不在此列：它由 Service 依据 parentId 自动维护，前端传入无效。
 */
@Data
public class MdmMaterialCategoryDto {

    private Long id;

    @NotNull(message = "上级分类不能为空")
    private Long parentId;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 50, message = "分类编码长度不能超过 50")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称长度不能超过 100")
    private String categoryName;

    private Integer orderNum;

    /** 状态：0正常 1停用 */
    private String status;

    private String remark;
}
