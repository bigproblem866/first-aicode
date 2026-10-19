package com.erp.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.BeanCopyUtils;
import com.erp.mdm.domain.MdmSupplier;
import com.erp.mdm.domain.dto.MdmSupplierDto;
import com.erp.mdm.domain.dto.MdmSupplierQuery;
import com.erp.mdm.mapper.MdmSupplierMapper;
import com.erp.mdm.service.IMdmSupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 供应商服务实现。
 */
@Service
@RequiredArgsConstructor
public class MdmSupplierServiceImpl implements IMdmSupplierService {

    private final MdmSupplierMapper supplierMapper;

    @Override
    public PageResult<MdmSupplier> pageList(MdmSupplierQuery query) {
        Page<MdmSupplier> page = query.toPage();
        LambdaQueryWrapper<MdmSupplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getSupplierCode() != null, MdmSupplier::getSupplierCode, query.getSupplierCode());
        wrapper.like(query.getSupplierName() != null, MdmSupplier::getSupplierName, query.getSupplierName());
        wrapper.eq(query.getStatus() != null, MdmSupplier::getStatus, query.getStatus());
        wrapper.orderByDesc(MdmSupplier::getId);
        return PageResult.of(supplierMapper.selectPage(page, wrapper));
    }

    @Override
    public MdmSupplier selectById(Long id) {
        return supplierMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insert(MdmSupplierDto dto) {
        checkCodeUnique(dto.getSupplierCode(), null);
        MdmSupplier entity = BeanCopyUtils.copy(dto, MdmSupplier.class);
        if (entity.getStatus() == null) {
            entity.setStatus(CommonConstants.STATUS_NORMAL);
        }
        supplierMapper.insert(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(MdmSupplierDto dto) {
        if (dto.getId() == null) {
            throw new ServiceException("供应商ID不能为空");
        }
        if (supplierMapper.selectById(dto.getId()) == null) {
            throw new ServiceException("供应商不存在或已删除");
        }
        checkCodeUnique(dto.getSupplierCode(), dto.getId());
        supplierMapper.updateById(BeanCopyUtils.copy(dto, MdmSupplier.class));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        supplierMapper.delete(new LambdaQueryWrapper<MdmSupplier>().in(MdmSupplier::getId, ids));
    }

    @Override
    public List<MdmSupplier> listOptions() {
        return supplierMapper.selectList(new LambdaQueryWrapper<MdmSupplier>()
                .eq(MdmSupplier::getStatus, CommonConstants.STATUS_NORMAL)
                .orderByAsc(MdmSupplier::getSupplierCode));
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<MdmSupplier> wrapper = new LambdaQueryWrapper<MdmSupplier>()
                .eq(MdmSupplier::getSupplierCode, code);
        if (excludeId != null) {
            wrapper.ne(MdmSupplier::getId, excludeId);
        }
        Long count = supplierMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException("供应商编码已存在：" + code);
        }
    }
}
