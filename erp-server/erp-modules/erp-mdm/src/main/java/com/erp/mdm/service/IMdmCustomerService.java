package com.erp.mdm.service;

import com.erp.common.core.domain.PageResult;
import com.erp.mdm.domain.MdmCustomer;
import com.erp.mdm.domain.dto.MdmCustomerDto;
import com.erp.mdm.domain.dto.MdmCustomerQuery;

import java.util.Collection;
import java.util.List;

/**
 * 客户服务。
 */
public interface IMdmCustomerService {

    PageResult<MdmCustomer> pageList(MdmCustomerQuery query);

    MdmCustomer selectById(Long id);

    void insert(MdmCustomerDto dto);

    void update(MdmCustomerDto dto);

    void deleteByIds(Collection<Long> ids);

    /** 客户下拉 */
    List<MdmCustomer> listOptions();
}
