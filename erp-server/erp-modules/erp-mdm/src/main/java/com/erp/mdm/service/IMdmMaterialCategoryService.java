package com.erp.mdm.service;

import com.erp.mdm.domain.MdmMaterialCategory;
import com.erp.mdm.domain.dto.MdmMaterialCategoryDto;
import com.erp.mdm.domain.dto.MdmMaterialCategoryQuery;
import com.erp.common.core.domain.TreeSelectVo;

import java.util.List;

/**
 * 物料分类服务。树形结构，列表接口返回嵌套树、不分页。
 */
public interface IMdmMaterialCategoryService {

    /** 分类列表（返回树形） */
    List<MdmMaterialCategory> selectCategoryList(MdmMaterialCategoryQuery query);

    /** 按 ID 查详情 */
    MdmMaterialCategory selectById(Long id);

    /** 新增（自动维护 ancestors） */
    void insert(MdmMaterialCategoryDto dto);

    /** 修改（自动维护 ancestors，并级联刷新下级的 ancestors） */
    void update(MdmMaterialCategoryDto dto);

    /** 删除（有下级或已被物料引用时抛业务异常） */
    void deleteById(Long id);

    /** 分类树下拉 */
    List<TreeSelectVo> buildTreeSelect();
}
