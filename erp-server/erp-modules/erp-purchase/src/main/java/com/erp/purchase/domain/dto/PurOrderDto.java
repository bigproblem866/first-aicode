package com.erp.purchase.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购订单（含明细）新增/修改入参。
 * <p>
 * total_amount 不在这里：订单总金额一律由明细汇总，前端传入无效。
 */
@Data
public class PurOrderDto {

    private Long id;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull(message = "下单日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectDate;

    private String remark;

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    private List<PurOrderDetailDto> details = new ArrayList<>();
}
