package com.erp.mdm.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 物料实体，对应表 mdm_material。
 * <p>
 * 本实体是「新增一个业务模块」的照抄模板：字段与 V2__schema_mdm.sql 一一对应，
 * 公共字段（id / create_by / create_time / update_by / update_time / is_deleted）
 * 继承自 {@link BaseEntity}，由 MyBatis-Plus 自动填充，不要在本类里重复声明。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mdm_material")
public class MdmMaterial extends BaseEntity {

    /** 物料编码，全局唯一（uk_mdm_material_code） */
    @NotBlank(message = "物料编码不能为空")
    private String materialCode;

    /** 物料名称 */
    @NotBlank(message = "物料名称不能为空")
    private String materialName;

    /** 物料类型（字典 mdm_material_type） */
    private String materialType;

    /** 物料分类ID，指向 mdm_material_category.id */
    private Long categoryId;

    /** 计量单位（字典 mdm_unit） */
    private String unit;

    /** 规格 */
    private String spec;

    /** 型号 */
    private String model;

    /** 品牌 */
    private String brand;

    /** 条形码 */
    private String barcode;

    /** 安全库存 */
    private BigDecimal safetyStock;

    /** 参考采购价 */
    private BigDecimal purchasePrice;

    /** 参考销售价 */
    private BigDecimal salePrice;

    /** 税率，如 0.13 表示 13% */
    private BigDecimal taxRate;

    /** 状态：0正常 1停用 */
    private String status;

    /** 备注 */
    private String remark;

    /** 外部系统编码（预留，本期不用） */
    private String externalCode;

    /** 来源系统标识（预留，本期不用） */
    private String sourceSystem;
}
