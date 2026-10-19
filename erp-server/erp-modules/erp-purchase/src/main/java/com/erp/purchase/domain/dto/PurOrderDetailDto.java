package com.erp.purchase.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购订单明细入参。金额后端按「数量 × 单价」算，received_qty 由收货流程维护，均不接受前端传入。
 */
@Data
public class PurOrderDetailDto {

    private Long id;

    @NotNull(message = "物料不能为空")
    private Long materialId;

    @NotNull(message = "采购数量不能为空")
    @DecimalMin(value = "0.0001", message = "采购数量必须大于 0")
    private BigDecimal qty;

    private BigDecimal price;

    private String remark;
}
