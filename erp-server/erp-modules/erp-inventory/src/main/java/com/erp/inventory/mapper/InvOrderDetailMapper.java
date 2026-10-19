package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.domain.InvOrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出入库单明细 Mapper。
 */
@Mapper
public interface InvOrderDetailMapper extends BaseMapper<InvOrderDetail> {
}
