package com.erp.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.BeanCopyUtils;
import com.erp.mdm.domain.MdmCustomer;
import com.erp.mdm.domain.dto.MdmCustomerDto;
import com.erp.mdm.domain.dto.MdmCustomerQuery;
import com.erp.mdm.mapper.MdmCustomerMapper;
import com.erp.mdm.service.IMdmCustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 客户服务实现。
 */
@Service
@RequiredArgsConstructor
public class MdmCustomerServiceImpl implements IMdmCustomerService {

    private final MdmCustomerMapper customerMapper;

    @Override
    public PageResult<MdmCustomer> pageList(MdmCustomerQuery query) {
        Page<MdmCustomer> page = query.toPage();
        LambdaQueryWrapper<MdmCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getCustomerCode() != null, MdmCustomer::getCustomerCode, query.getCustomerCode());
        wrapper.like(query.getCustomerName() != null, MdmCustomer::getCustomerName, query.getCustomerName());
        wrapper.eq(query.getCustomerType() != null, MdmCustomer::getCustomerType, query.getCustomerType());
        wrapper.eq(query.getStatus() != null, MdmCustomer::getStatus, query.getStatus());
        wrapper.orderByDesc(MdmCustomer::getId);
        return PageResult.of(customerMapper.selectPage(page, wrapper));
    }

    @Override
    public MdmCustomer selectById(Long id) {
        return customerMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insert(MdmCustomerDto dto) {
        checkCodeUnique(dto.getCustomerCode(), null);
        MdmCustomer entity = BeanCopyUtils.copy(dto, MdmCustomer.class);
        if (entity.getStatus() == null) {
            entity.setStatus(CommonConstants.STATUS_NORMAL);
        }
        customerMapper.insert(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(MdmCustomerDto dto) {
        if (dto.getId() == null) {
            throw new ServiceException("客户ID不能为空");
        }
        if (customerMapper.selectById(dto.getId()) == null) {
            throw new ServiceException("客户不存在或已删除");
        }
        checkCodeUnique(dto.getCustomerCode(), dto.getId());
        customerMapper.updateById(BeanCopyUtils.copy(dto, MdmCustomer.class));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        customerMapper.delete(new LambdaQueryWrapper<MdmCustomer>().in(MdmCustomer::getId, ids));
    }

    @Override
    public List<MdmCustomer> listOptions() {
        return customerMapper.selectList(new LambdaQueryWrapper<MdmCustomer>()
                .eq(MdmCustomer::getStatus, CommonConstants.STATUS_NORMAL)
                .orderByAsc(MdmCustomer::getCustomerCode));
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<MdmCustomer> wrapper = new LambdaQueryWrapper<MdmCustomer>()
                .eq(MdmCustomer::getCustomerCode, code);
        if (excludeId != null) {
            wrapper.ne(MdmCustomer::getId, excludeId);
        }
        Long count = customerMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException("客户编码已存在：" + code);
        }
    }
}
