package com.erp.purchase.service;

import com.erp.common.core.domain.PageResult;
import com.erp.purchase.domain.PurReceipt;
import com.erp.purchase.domain.dto.PurReceiptDto;
import com.erp.purchase.domain.dto.PurReceiptQuery;
import com.erp.purchase.domain.vo.PurReceiptVo;

import java.util.Collection;

/**
 * 采购收货单服务。
 * <p>
 * {@link #confirm(Long)} 是本工程「跨模块业务闭环」的核心：
 * 采购 + 库存两个模块在同一事务里完成收货、反写订单、生成入库单、加库存。
 */
public interface IPurReceiptService {

    PageResult<PurReceiptVo> pageList(PurReceiptQuery query);

    /** 收货单详情（含明细） */
    PurReceipt selectById(Long id);

    /** 新增收货单（草稿） */
    void insert(PurReceiptDto dto);

    /** 删除（仅草稿） */
    void deleteByIds(Collection<Long> ids);

    /** 确认收货：校验未收数量 → 反写已收数量 → 生成入库单 → 加库存 → 单据置为已确认 */
    void confirm(Long id);
}
