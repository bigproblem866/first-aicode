package com.erp.purchase.service;

import com.erp.common.core.domain.PageResult;
import com.erp.purchase.domain.PurOrder;
import com.erp.purchase.domain.dto.PurOrderDto;
import com.erp.purchase.domain.dto.PurOrderQuery;
import com.erp.purchase.domain.vo.PurOrderVo;

import java.util.Collection;
import java.util.List;

/**
 * 采购订单服务。只有草稿可改可删；已审核的订单不能改金额与明细。
 */
public interface IPurOrderService {

    PageResult<PurOrderVo> pageList(PurOrderQuery query);

    /** 订单详情（含明细） */
    PurOrder selectById(Long id);

    void insert(PurOrderDto dto);

    void update(PurOrderDto dto);

    void deleteByIds(Collection<Long> ids);

    /** 审核：草稿 → 已审核 */
    void audit(Long id);

    /** 作废：已审核未收货的订单作废 */
    void cancel(Long id);

    /** 已审核且未收完的订单（含明细、含已收/未收数量），收货选单用 */
    List<PurOrder> listPendingOrders();
}
