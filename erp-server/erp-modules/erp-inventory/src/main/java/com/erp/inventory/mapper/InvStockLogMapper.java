package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.domain.InvStockLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存流水 Mapper。只用 BaseMapper 方法（查询条件都是本表字段，无需联表）。
 */
@Mapper
public interface InvStockLogMapper extends BaseMapper<InvStockLog> {
}
