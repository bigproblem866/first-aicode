package com.erp.purchase.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购收货单明细入参。orderDetailId 指回采购订单明细行，确认收货时靠它定位累加位置。
 */
@Data
public class PurReceiptDetailDto {

    private Long id;

    @NotNull(message = "来源订单明细不能为空")
    private Long orderDetailId;

    @NotNull(message = "收货数量不能为空")
    @DecimalMin(value = "0.0001", message = "收货数量必须大于 0")
    private BigDecimal qty;

    private String remark;
}
