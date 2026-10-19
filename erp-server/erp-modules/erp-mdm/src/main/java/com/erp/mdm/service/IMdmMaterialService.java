package com.erp.mdm.service;

import com.erp.common.core.domain.PageResult;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.dto.MdmMaterialDto;
import com.erp.mdm.domain.dto.MdmMaterialQuery;
import com.erp.mdm.domain.vo.MdmMaterialVo;

import java.util.Collection;
import java.util.List;

/**
 * 物料服务。写操作的状态/唯一性校验都在实现里做，Controller 只负责收参、包 R。
 */
public interface IMdmMaterialService {

    /** 分页查询（带分类名、单位名等展示字段） */
    PageResult<MdmMaterialVo> pageList(MdmMaterialQuery query);

    /** 按 ID 查详情 */
    MdmMaterial selectById(Long id);

    /** 新增 */
    void insert(MdmMaterialDto dto);

    /** 修改 */
    void update(MdmMaterialDto dto);

    /** 批量删除（逻辑删除） */
    void deleteByIds(Collection<Long> ids);

    /** 物料下拉（单据选料用） */
    List<MdmMaterialVo> listOptions(String keyword);
}
