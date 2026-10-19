package com.erp.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.BeanCopyUtils;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.dto.MdmMaterialDto;
import com.erp.mdm.domain.dto.MdmMaterialQuery;
import com.erp.mdm.domain.vo.MdmMaterialVo;
import com.erp.mdm.mapper.MdmMaterialMapper;
import com.erp.mdm.service.IMdmMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 物料服务实现（本模块的照抄模板）。
 */
@Service
@RequiredArgsConstructor
public class MdmMaterialServiceImpl implements IMdmMaterialService {

    private final MdmMaterialMapper materialMapper;

    @Override
    public PageResult<MdmMaterialVo> pageList(MdmMaterialQuery query) {
        Page<MdmMaterialVo> page = query.toPage();
        materialMapper.selectMaterialPage(page, query);
        return PageResult.of(page);
    }

    @Override
    public MdmMaterial selectById(Long id) {
        return materialMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insert(MdmMaterialDto dto) {
        checkCodeUnique(dto.getMaterialCode(), null);
        MdmMaterial entity = BeanCopyUtils.copy(dto, MdmMaterial.class);
        // 拷贝会把 null 也带过来，显式兜住几个 NOT NULL 字段的默认值
        if (entity.getStatus() == null) {
            entity.setStatus(CommonConstants.STATUS_NORMAL);
        }
        materialMapper.insert(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(MdmMaterialDto dto) {
        if (dto.getId() == null) {
            throw new ServiceException("物料ID不能为空");
        }
        MdmMaterial exist = materialMapper.selectById(dto.getId());
        if (exist == null) {
            throw new ServiceException("物料不存在或已删除");
        }
        checkCodeUnique(dto.getMaterialCode(), dto.getId());
        MdmMaterial entity = BeanCopyUtils.copy(dto, MdmMaterial.class);
        materialMapper.updateById(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        // 用 Wrapper 而不是 deleteBatchIds：逻辑删除下同样走 UPDATE ... SET is_deleted = 1
        materialMapper.delete(new LambdaQueryWrapper<MdmMaterial>().in(MdmMaterial::getId, ids));
    }

    @Override
    public List<MdmMaterialVo> listOptions(String keyword) {
        return materialMapper.selectMaterialOptions(keyword);
    }

    /**
     * 编码唯一校验。excludeId 用于「修改时排除自己」，否则改别的字段也会被判重复。
     */
    private void checkCodeUnique(String materialCode, Long excludeId) {
        LambdaQueryWrapper<MdmMaterial> wrapper = new LambdaQueryWrapper<MdmMaterial>()
                .eq(MdmMaterial::getMaterialCode, materialCode);
        if (excludeId != null) {
            wrapper.ne(MdmMaterial::getId, excludeId);
        }
        Long count = materialMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException("物料编码已存在：" + materialCode);
        }
    }
}
