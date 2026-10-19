package com.erp.inventory.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 出入库单明细入参。金额由后端按「数量 × 单价」计算，前端传入无效。
 */
@Data
public class InvOrderDetailDto {

    /** 修改时回显用；新增可空 */
    private Long id;

    @NotNull(message = "物料不能为空")
    private Long materialId;

    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.0001", message = "数量必须大于 0")
    private BigDecimal qty;

    /** 单价，不传按 0 处理 */
    private BigDecimal price;

    private String remark;
}
