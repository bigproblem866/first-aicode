package com.erp.mdm.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 客户实体，对应表 mdm_customer。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_customer")
public class MdmCustomer extends BaseEntity {

    /** 客户编码，唯一 */
    @NotBlank(message = "客户编码不能为空")
    private String customerCode;

    /** 客户名称 */
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    /** 客户类型（字典 mdm_customer_type） */
    private String customerType;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 地址 */
    private String address;

    /** 税号 */
    private String taxNo;

    /** 信用额度 */
    private BigDecimal creditLimit;

    /** 状态：0正常 1停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 外部系统编码（预留） */
    private String externalCode;

    /** 来源系统标识（预留） */
    private String sourceSystem;
}
