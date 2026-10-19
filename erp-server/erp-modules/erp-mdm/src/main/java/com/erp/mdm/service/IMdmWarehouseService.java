package com.erp.mdm.service;

import com.erp.common.core.domain.PageResult;
import com.erp.mdm.domain.MdmWarehouse;
import com.erp.mdm.domain.dto.MdmWarehouseDto;
import com.erp.mdm.domain.dto.MdmWarehouseQuery;

import java.util.Collection;
import java.util.List;

/**
 * 仓库服务。
 */
public interface IMdmWarehouseService {

    PageResult<MdmWarehouse> pageList(MdmWarehouseQuery query);

    MdmWarehouse selectById(Long id);

    void insert(MdmWarehouseDto dto);

    void update(MdmWarehouseDto dto);

    void deleteByIds(Collection<Long> ids);

    /** 仓库下拉 */
    List<MdmWarehouse> listOptions();
}
