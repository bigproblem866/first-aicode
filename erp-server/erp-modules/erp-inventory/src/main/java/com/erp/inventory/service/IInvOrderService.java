package com.erp.inventory.service;

import com.erp.common.core.domain.PageResult;
import com.erp.inventory.domain.InvOrder;
import com.erp.inventory.domain.dto.InvOrderDto;
import com.erp.inventory.domain.dto.InvOrderQuery;
import com.erp.inventory.domain.vo.InvOrderVo;

import java.util.Collection;

/**
 * 出入库单服务。
 * <p>
 * 状态流转都在实现里校验：只有草稿能改/删，只有草稿能审核，只有已审核能作废。
 */
public interface IInvOrderService {

    PageResult<InvOrderVo> pageList(InvOrderQuery query);

    /** 单据详情，<b>含明细</b> */
    InvOrder selectById(Long id);

    /** 新增（单头 + 明细），状态=草稿，单据号后端生成 */
    void insert(InvOrderDto dto);

    /** 修改（仅草稿），先删旧明细再插新明细 */
    void update(InvOrderDto dto);

    /** 删除（仅草稿） */
    void deleteByIds(Collection<Long> ids);

    /** 审核：写库存台账 + 写流水 */
    void audit(Long id);

    /** 作废（仅已审核）：反向冲回库存并写反向流水，不删除原流水 */
    void cancel(Long id);
}
