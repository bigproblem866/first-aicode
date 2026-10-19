package com.erp.mdm.service;

import com.erp.common.core.domain.PageResult;
import com.erp.mdm.domain.MdmSupplier;
import com.erp.mdm.domain.dto.MdmSupplierDto;
import com.erp.mdm.domain.dto.MdmSupplierQuery;

import java.util.Collection;
import java.util.List;

/**
 * 供应商服务。
 */
public interface IMdmSupplierService {

    PageResult<MdmSupplier> pageList(MdmSupplierQuery query);

    MdmSupplier selectById(Long id);

    void insert(MdmSupplierDto dto);

    void update(MdmSupplierDto dto);

    void deleteByIds(Collection<Long> ids);

    /** 供应商下拉 */
    List<MdmSupplier> listOptions();
}
