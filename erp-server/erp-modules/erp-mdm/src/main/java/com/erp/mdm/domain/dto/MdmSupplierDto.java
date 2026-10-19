package com.erp.mdm.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 供应商新增/修改入参。
 */
@Data
public class MdmSupplierDto {

    private Long id;

    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 50, message = "供应商编码长度不能超过 50")
    private String supplierCode;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 200, message = "供应商名称长度不能超过 200")
    private String supplierName;

    private String contact;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String address;

    private String taxNo;

    /** 状态：0正常 1停用 */
    private String status;

    private String remark;

    private String externalCode;

    private String sourceSystem;
}
