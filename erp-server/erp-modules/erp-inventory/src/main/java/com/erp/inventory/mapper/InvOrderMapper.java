package com.erp.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.inventory.domain.InvOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出入库单主表 Mapper。
 * <p>
 * 仓库名不在 SQL 里 join —— 按模块边界规则，跨模块取 mdm 数据必须走 MdmApi，
 * 由 Service 查完后填充（见 InvOrderServiceImpl）。
 */
@Mapper
public interface InvOrderMapper extends BaseMapper<InvOrder> {
}
