package com.erp.mdm.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 物料新增/修改入参。
 * <p>
 * 与实体分开而不直接复用实体，原因有两个：
 * <ol>
 *   <li>修改时前端会带上 id，但 create_by / create_time 等公共字段绝不允许由前端指定，
 *       拆开 DTO 能从入参层面就杜绝被覆盖；</li>
 *   <li>校验注解只挂在 DTO 上，实体保持"纯数据"便于被其他模块（如 inventory）复用。</li>
 * </ol>
 */
@Data
public class MdmMaterialDto {

    /** 修改时必传；新增时为空 */
    private Long id;

    @NotBlank(message = "物料编码不能为空")
    @Size(max = 50, message = "物料编码长度不能超过 50")
    private String materialCode;

    @NotBlank(message = "物料名称不能为空")
    @Size(max = 200, message = "物料名称长度不能超过 200")
    private String materialName;

    private String materialType;

    private Long categoryId;

    private String unit;

    private String spec;

    private String model;

    private String brand;

    private String barcode;

    private BigDecimal safetyStock;

    private BigDecimal purchasePrice;

    private BigDecimal salePrice;

    private BigDecimal taxRate;

    /** 状态：0正常 1停用，不传默认正常 */
    private String status;

    private String remark;

    private String externalCode;

    private String sourceSystem;
}
