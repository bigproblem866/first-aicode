package com.erp.inventory.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 出入库单（含明细）新增/修改入参。
 * <p>
 * 单据号由后端生成、状态固定为草稿、金额由明细汇总 —— 这几个字段都不在入参里，
 * 避免被前端伪造。
 */
@Data
public class InvOrderDto {

    /** 修改时必传 */
    private Long id;

    @NotBlank(message = "单据类型不能为空")
    private String orderType;

    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @NotNull(message = "业务日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bizDate;

    private String remark;

    /** 明细行，至少一行 */
    @Valid
    @NotEmpty(message = "单据明细不能为空")
    private List<InvOrderDetailDto> details = new ArrayList<>();
}
