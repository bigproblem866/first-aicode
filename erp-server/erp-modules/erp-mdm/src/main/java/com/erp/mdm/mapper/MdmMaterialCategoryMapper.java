package com.erp.mdm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.mdm.domain.MdmMaterialCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物料分类 Mapper。树形结构查询用 BaseMapper 的 selectList 即可，无需 XML。
 */
@Mapper
public interface MdmMaterialCategoryMapper extends BaseMapper<MdmMaterialCategory> {
}
