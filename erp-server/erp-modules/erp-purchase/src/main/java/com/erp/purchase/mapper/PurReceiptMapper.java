package com.erp.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.purchase.domain.PurReceipt;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购收货单主表 Mapper。
 */
@Mapper
public interface PurReceiptMapper extends BaseMapper<PurReceipt> {
}
