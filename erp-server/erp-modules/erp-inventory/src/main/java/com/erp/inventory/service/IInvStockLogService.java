package com.erp.inventory.service;

import com.erp.common.core.domain.PageResult;
import com.erp.inventory.domain.dto.InvStockLogQuery;
import com.erp.inventory.domain.vo.InvStockLogVo;

/**
 * 库存流水服务。
 */
public interface IInvStockLogService {

    /** 流水分页（物料/仓库名称通过 MdmApi 填充） */
    PageResult<InvStockLogVo> pageList(InvStockLogQuery query);
}
