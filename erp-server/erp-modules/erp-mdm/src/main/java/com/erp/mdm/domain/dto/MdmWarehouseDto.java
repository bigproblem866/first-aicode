package com.erp.mdm.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 仓库新增/修改入参。
 */
@Data
public class MdmWarehouseDto {

    private Long id;

    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 50, message = "仓库编码长度不能超过 50")
    private String warehouseCode;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 100, message = "仓库名称长度不能超过 100")
    private String warehouseName;

    private String address;

    private String manager;

    private String phone;

    /** 状态：0正常 1停用 */
    private String status;

    private String remark;
}
