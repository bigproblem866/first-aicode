package com.erp.mdm.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户新增/修改入参。
 */
@Data
public class MdmCustomerDto {

    private Long id;

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 50, message = "客户编码长度不能超过 50")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 200, message = "客户名称长度不能超过 200")
    private String customerName;

    private String customerType;

    private String contact;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String address;

    private String taxNo;

    private BigDecimal creditLimit;

    /** 状态：0正常 1停用 */
    private String status;

    private String remark;

    private String externalCode;

    private String sourceSystem;
}
