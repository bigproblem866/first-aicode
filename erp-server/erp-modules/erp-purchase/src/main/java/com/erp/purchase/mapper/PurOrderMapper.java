package com.erp.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.purchase.domain.PurOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购订单主表 Mapper。供应商名称通过 MdmApi 填充，不 join mdm 表。
 */
@Mapper
public interface PurOrderMapper extends BaseMapper<PurOrder> {
}
