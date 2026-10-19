package com.erp.inventory.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成入库单的请求（跨模块协议）。
 * <p>
 * 采购收货确认后，采购模块构造本对象调用 {@code IInvStockService.createInboundFromReceipt}，
 * 由库存模块负责建单、加台账、写流水。采购模块不接触 inv_* 表。
 */
@Data
public class InboundRequest {

    /** 收货仓库ID */
    private Long warehouseId;

    /** 业务日期 */
    private LocalDate bizDate;

    /** 来源单据类型，如 PUR_RECEIPT */
    private String sourceType;

    /** 来源单据号，如收货单号 PR20261016001 */
    private String sourceNo;

    /** 备注 */
    private String remark;

    /** 入库明细 */
    private List<InboundItem> items = new ArrayList<>();
}
