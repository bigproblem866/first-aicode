package com.erp.inventory.api.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 入库明细项，{@link InboundRequest} 的一部分。
 * <p>
 * 放在 api 包内：它是跨模块调用协议的一部分，采购模块会构造它。
 */
@Data
public class InboundItem {

    /** 物料ID */
    private Long materialId;

    /** 入库数量（正数） */
    private BigDecimal qty;

    /** 单价，用于入库明细金额展示 */
    private BigDecimal price;

    /** 备注 */
    private String remark;
}
