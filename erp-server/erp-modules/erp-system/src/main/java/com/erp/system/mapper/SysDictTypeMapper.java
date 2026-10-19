package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysDictType;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字典类型 Mapper。全部为单表操作，无需 XML。
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {
}
