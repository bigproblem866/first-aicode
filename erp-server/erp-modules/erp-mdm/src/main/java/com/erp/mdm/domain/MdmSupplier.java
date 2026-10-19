package com.erp.mdm.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 供应商实体，对应表 mdm_supplier。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_supplier")
public class MdmSupplier extends BaseEntity {

    /** 供应商编码，唯一 */
    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    /** 供应商名称 */
    @NotBlank(message = "供应商名称不能为空")
    private String supplierName;

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

    /** 状态：0正常 1停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 外部系统编码（预留） */
    private String externalCode;

    /** 来源系统标识（预留） */
    private String sourceSystem;
}
