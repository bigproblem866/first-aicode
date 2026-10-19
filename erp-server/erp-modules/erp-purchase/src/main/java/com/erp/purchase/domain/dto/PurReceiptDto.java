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
 * 采购收货单（含明细）新增入参。收货单只新增与删除草稿，不做修改。
 */
@Data
public class PurReceiptDto {

    private Long id;

    @NotNull(message = "来源采购订单不能为空")
    private Long orderId;

    @NotNull(message = "收货仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "收货日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate receiptDate;

    private String remark;

    @Valid
    @NotEmpty(message = "收货明细不能为空")
    private List<PurReceiptDetailDto> details = new ArrayList<>();
}
