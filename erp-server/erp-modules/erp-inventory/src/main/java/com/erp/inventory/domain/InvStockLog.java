package com.erp.inventory.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存流水实体，对应表 inv_stock_log。
 * <p>
 * 只增不改不删：单据作废时再写一条反向流水，而不是删掉原记录。
 * 这是库存模块唯一的对账依据 —— 台账只反映结果，出了问题要靠流水回溯是哪一单改的。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inv_stock_log")
public class InvStockLog extends BaseEntity {

    /** 仓库ID */
    private Long warehouseId;

    /** 物料ID */
    private Long materialId;

    /** 业务类型：IN入库 OUT出库 ADJUST盘点调整 */
    private String bizType;

    /** 变动数量：入库为正、出库为负 */
    private BigDecimal qty;

    /** 变动前数量 */
    private BigDecimal beforeQty;

    /** 变动后数量 */
    private BigDecimal afterQty;

    /** 来源单据类型：INV_ORDER出入库单 PUR_RECEIPT采购收货 */
    private String sourceType;

    /** 来源单据ID */
    private Long sourceId;

    /** 来源单据号 */
    private String sourceNo;

    /** 业务日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate bizDate;

    /** 备注 */
    private String remark;
}
