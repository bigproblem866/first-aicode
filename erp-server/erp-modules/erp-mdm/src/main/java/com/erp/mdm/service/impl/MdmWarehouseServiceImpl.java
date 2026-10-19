package com.erp.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.BeanCopyUtils;
import com.erp.mdm.domain.MdmWarehouse;
import com.erp.mdm.domain.dto.MdmWarehouseDto;
import com.erp.mdm.domain.dto.MdmWarehouseQuery;
import com.erp.mdm.mapper.MdmWarehouseMapper;
import com.erp.mdm.service.IMdmWarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 仓库服务实现。
 */
@Service
@RequiredArgsConstructor
public class MdmWarehouseServiceImpl implements IMdmWarehouseService {

    private final MdmWarehouseMapper warehouseMapper;

    @Override
    public PageResult<MdmWarehouse> pageList(MdmWarehouseQuery query) {
        Page<MdmWarehouse> page = query.toPage();
        LambdaQueryWrapper<MdmWarehouse> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getWarehouseCode() != null, MdmWarehouse::getWarehouseCode, query.getWarehouseCode());
        wrapper.like(query.getWarehouseName() != null, MdmWarehouse::getWarehouseName, query.getWarehouseName());
        wrapper.eq(query.getStatus() != null, MdmWarehouse::getStatus, query.getStatus());
        wrapper.orderByDesc(MdmWarehouse::getId);
        return PageResult.of(warehouseMapper.selectPage(page, wrapper));
    }

    @Override
    public MdmWarehouse selectById(Long id) {
        return warehouseMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insert(MdmWarehouseDto dto) {
        checkCodeUnique(dto.getWarehouseCode(), null);
        MdmWarehouse entity = BeanCopyUtils.copy(dto, MdmWarehouse.class);
        if (entity.getStatus() == null) {
            entity.setStatus(CommonConstants.STATUS_NORMAL);
        }
        warehouseMapper.insert(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(MdmWarehouseDto dto) {
        if (dto.getId() == null) {
            throw new ServiceException("仓库ID不能为空");
        }
        if (warehouseMapper.selectById(dto.getId()) == null) {
            throw new ServiceException("仓库不存在或已删除");
        }
        checkCodeUnique(dto.getWarehouseCode(), dto.getId());
        warehouseMapper.updateById(BeanCopyUtils.copy(dto, MdmWarehouse.class));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        warehouseMapper.delete(new LambdaQueryWrapper<MdmWarehouse>().in(MdmWarehouse::getId, ids));
    }

    @Override
    public List<MdmWarehouse> listOptions() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getStatus, CommonConstants.STATUS_NORMAL)
                .orderByAsc(MdmWarehouse::getWarehouseCode));
    }

    private void checkCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<MdmWarehouse> wrapper = new LambdaQueryWrapper<MdmWarehouse>()
                .eq(MdmWarehouse::getWarehouseCode, code);
        if (excludeId != null) {
            wrapper.ne(MdmWarehouse::getId, excludeId);
        }
        Long count = warehouseMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException("仓库编码已存在：" + code);
        }
    }
}
