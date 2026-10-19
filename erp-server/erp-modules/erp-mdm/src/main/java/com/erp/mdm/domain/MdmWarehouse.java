package com.erp.mdm.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 仓库实体，对应表 mdm_warehouse。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_warehouse")
public class MdmWarehouse extends BaseEntity {

    /** 仓库编码，唯一 */
    @NotBlank(message = "仓库编码不能为空")
    private String warehouseCode;

    /** 仓库名称 */
    @NotBlank(message = "仓库名称不能为空")
    private String warehouseName;

    /** 仓库地址 */
    private String address;

    /** 仓管员 */
    private String manager;

    /** 联系电话 */
    private String phone;

    /** 状态：0正常 1停用 */
    private String status;

    /** 备注 */
    private String remark;
}
