package com.erp.inventory.service;

import com.erp.common.core.domain.PageResult;
import com.erp.inventory.api.dto.InboundRequest;
import com.erp.inventory.domain.InvOrder;
import com.erp.inventory.domain.dto.InvStockQuery;
import com.erp.inventory.domain.vo.InvStockVo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存台账 + 台账变动的服务。
 * <p>
 * {@link #increaseStock} / {@link #decreaseStock} 是库存数量变动的<b>唯一入口</b>：
 * 出库单审核、单据作废冲回、采购收货入库都走这里，保证「改台账必写流水」这条不变量。
 * <p>
 * {@link #createInboundFromReceipt} 是对 erp-purchase 暴露的跨模块接口。
 */
public interface IInvStockService {

    /** 库存台账分页（物料/仓库名称通过 MdmApi 填充） */
    PageResult<InvStockVo> pageList(InvStockQuery query);

    /**
     * 入库：UPSERT 台账 + 写一条 qty 为正的流水。
     *
     * @param sourceType 来源单据类型，如 INV_ORDER
     * @param sourceId   来源单据ID
     * @param sourceNo   来源单据号
     */
    void increaseStock(Long warehouseId, Long materialId, BigDecimal qty, String bizType,
                       String sourceType, Long sourceId, String sourceNo, LocalDate bizDate, String remark);

    /**
     * 出库：带条件的原子扣减 + 写一条 qty 为负的流水；库存不足抛 ServiceException。
     * <p>
     * 调用方必须在事务里调用它，失败时整单回滚。
     */
    void decreaseStock(Long warehouseId, Long materialId, BigDecimal qty, String bizType,
                       String sourceType, Long sourceId, String sourceNo, LocalDate bizDate, String remark);

    /**
     * 采购收货确认后生成一张「已审核」的入库单，并完成台账与流水的写入。
     * <p>
     * 由 erp-purchase 调用。返回生成的入库单（含 id 与 orderNo），供调用方回写到收货单。
     */
    InvOrder createInboundFromReceipt(InboundRequest request);
}
