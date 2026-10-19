package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.domain.PageResult;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.StringUtils;
import com.erp.framework.config.CaffeineCacheConfig;
import com.erp.system.domain.SysDictData;
import com.erp.system.domain.SysDictType;
import com.erp.system.domain.dto.SysDictDataQuery;
import com.erp.system.domain.dto.SysDictTypeQuery;
import com.erp.system.mapper.SysDictDataMapper;
import com.erp.system.mapper.SysDictTypeMapper;
import com.erp.system.service.ISysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字典服务实现（类型 + 数据一起管）。
 * <p>
 * <b>缓存注意事项</b>：{@link #selectDictDataByType(String)} 上的 {@code @Cacheable} 依赖 Spring 代理，
 * 只有"外部调用"（Controller → Service）才会命中。同类内部的自调用会绕过代理、直接走原方法，
 * 注解形同虚设 —— 所以缓存注解只打在本类的公开方法上，且这些方法由 Controller 直接调用。
 * <p>
 * 一旦字典数据发生增删改，对应用户可能立刻刷新页面，因此这些方法都做 {@code allEntries = true} 的
 * 全量清理：字典总量小，全清的成本远低于"按类型精细清理"漏清理导致的"改了没生效"。
 */
@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements ISysDictService {

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictDataMapper sysDictDataMapper;

    // ==================================================================
    // 字典类型
    // ==================================================================

    @Override
    public PageResult<SysDictType> selectDictTypePage(SysDictTypeQuery query) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.isNotBlank(query.getDictName()), SysDictType::getDictName, query.getDictName());
            wrapper.like(StringUtils.isNotBlank(query.getDictType()), SysDictType::getDictType, query.getDictType());
            wrapper.eq(StringUtils.isNotBlank(query.getStatus()), SysDictType::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysDictType::getId);
        return PageResult.of(sysDictTypeMapper.selectPage(query.toPage(), wrapper));
    }

    @Override
    public SysDictType selectDictTypeById(Long dictId) {
        SysDictType dictType = sysDictTypeMapper.selectById(dictId);
        if (dictType == null) {
            throw new ServiceException("字典类型不存在");
        }
        return dictType;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertDictType(SysDictType dictType) {
        checkDictTypeUnique(dictType.getDictType(), null);
        if (StringUtils.isBlank(dictType.getStatus())) {
            dictType.setStatus(CommonConstants.STATUS_NORMAL);
        }
        sysDictTypeMapper.insert(dictType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_DICT, allEntries = true)
    public void updateDictType(SysDictType dictType) {
        if (dictType.getId() == null) {
            throw new ServiceException("字典类型ID不能为空");
        }
        SysDictType existing = sysDictTypeMapper.selectById(dictType.getId());
        if (existing == null) {
            throw new ServiceException("字典类型不存在");
        }
        checkDictTypeUnique(dictType.getDictType(), dictType.getId());
        sysDictTypeMapper.updateById(dictType);

        // 类型标识被改名时，连同其下字典数据的 dict_type 一起改，
        // 否则这些数据会挂在一个不再存在的类型上，变成永远查不出来的孤儿数据
        if (StringUtils.isNotBlank(dictType.getDictType())
                && !dictType.getDictType().equals(existing.getDictType())) {
            SysDictData update = new SysDictData();
            update.setDictType(dictType.getDictType());
            sysDictDataMapper.update(update, new LambdaUpdateWrapper<SysDictData>()
                    .eq(SysDictData::getDictType, existing.getDictType()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_DICT, allEntries = true)
    public void deleteDictTypeByIds(List<Long> dictIds) {
        if (StringUtils.isEmpty(dictIds)) {
            throw new ServiceException("请选择要删除的字典类型");
        }
        // 先查出类型标识，删掉类型后就查不到了，也就无从删除其下数据
        List<SysDictType> types = sysDictTypeMapper.selectList(
                new LambdaQueryWrapper<SysDictType>().in(SysDictType::getId, dictIds));

        sysDictTypeMapper.deleteByIds(dictIds);

        // 连带逻辑删除该类型下的所有字典数据：只删类型会留下一堆孤儿数据，
        // 它们既没有归属类型可维护，又可能被按 dictType 查询的接口意外命中
        for (SysDictType type : types) {
            sysDictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
                    .eq(SysDictData::getDictType, type.getDictType()));
        }
    }

    /** 字典类型标识查重。excludeId 用于修改时排除自己 */
    private void checkDictTypeUnique(String dictType, Long excludeId) {
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getDictType, dictType);
        if (excludeId != null) {
            wrapper.ne(SysDictType::getId, excludeId);
        }
        if (sysDictTypeMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("字典类型已存在：" + dictType);
        }
    }

    // ==================================================================
    // 字典数据
    // ==================================================================

    @Override
    public PageResult<SysDictData> selectDictDataPage(SysDictDataQuery query) {
        LambdaQueryWrapper<SysDictData> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.eq(StringUtils.isNotBlank(query.getDictType()), SysDictData::getDictType, query.getDictType());
            wrapper.like(StringUtils.isNotBlank(query.getDictLabel()), SysDictData::getDictLabel, query.getDictLabel());
            wrapper.eq(StringUtils.isNotBlank(query.getStatus()), SysDictData::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysDictData::getDictSort);
        return PageResult.of(sysDictDataMapper.selectPage(query.toPage(), wrapper));
    }

    @Override
    @Cacheable(cacheNames = CaffeineCacheConfig.CACHE_DICT, key = "#dictType")
    public List<SysDictData> selectDictDataByType(String dictType) {
        return sysDictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, CommonConstants.STATUS_NORMAL)
                .orderByAsc(SysDictData::getDictSort));
    }

    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        SysDictData dictData = sysDictDataMapper.selectById(dictCode);
        if (dictData == null) {
            throw new ServiceException("字典数据不存在");
        }
        return dictData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_DICT, allEntries = true)
    public void insertDictData(SysDictData dictData) {
        if (StringUtils.isBlank(dictData.getStatus())) {
            dictData.setStatus(CommonConstants.STATUS_NORMAL);
        }
        if (StringUtils.isBlank(dictData.getIsDefault())) {
            dictData.setIsDefault(CommonConstants.NO);
        }
        sysDictDataMapper.insert(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_DICT, allEntries = true)
    public void updateDictData(SysDictData dictData) {
        if (dictData.getId() == null) {
            throw new ServiceException("字典数据ID不能为空");
        }
        sysDictDataMapper.updateById(dictData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(cacheNames = CaffeineCacheConfig.CACHE_DICT, allEntries = true)
    public void deleteDictDataByIds(List<Long> dictCodes) {
        if (StringUtils.isEmpty(dictCodes)) {
            throw new ServiceException("请选择要删除的字典数据");
        }
        sysDictDataMapper.deleteByIds(dictCodes);
    }
}
