package com.erp.system.service;

import com.erp.common.core.domain.PageResult;
import com.erp.system.domain.SysDictData;
import com.erp.system.domain.SysDictType;
import com.erp.system.domain.dto.SysDictDataQuery;
import com.erp.system.domain.dto.SysDictTypeQuery;

import java.util.List;

/**
 * 字典服务。
 * <p>
 * 字典拆成「类型」+「数据」两张表（只用一张表无法区分"性别"和"客户类型"里都叫 value='1' 的数据）。
 * 二者是一对多、且强关联，所以放在同一个 Service 里管理，不拆成两个 ——
 * 删除类型时必须连带删除其下数据，这条规则只有放在一个类里才不会漏。
 * <p>
 * {@link #selectDictDataByType(String)} 带进程内缓存，前端几乎所有页面都会调它渲染下拉与状态列。
 */
public interface ISysDictService {

    // ---------------- 字典类型 ----------------

    /** 字典类型分页列表 */
    PageResult<SysDictType> selectDictTypePage(SysDictTypeQuery query);

    /** 字典类型详情 */
    SysDictType selectDictTypeById(Long dictId);

    /** 新增字典类型：dictType 唯一 */
    void insertDictType(SysDictType dictType);

    /** 修改字典类型：dictType 唯一；标识被改名时同步其下数据的 dict_type */
    void updateDictType(SysDictType dictType);

    /** 批量逻辑删除字典类型，同时逻辑删除其下的所有字典数据，避免产生孤儿数据 */
    void deleteDictTypeByIds(List<Long> dictIds);

    // ---------------- 字典数据 ----------------

    /** 字典数据分页列表（按 dictType 过滤） */
    PageResult<SysDictData> selectDictDataPage(SysDictDataQuery query);

    /**
     * 按类型取字典数据（只含 status='0'，按 dict_sort 排序）。
     * <p>
     * 前端 DictSelect / DictTag 组件就靠它渲染，是全站最热的接口之一，结果进 Caffeine 缓存。
     */
    List<SysDictData> selectDictDataByType(String dictType);

    /** 字典数据详情 */
    SysDictData selectDictDataById(Long dictCode);

    /** 新增字典数据（清理字典缓存） */
    void insertDictData(SysDictData dictData);

    /** 修改字典数据（清理字典缓存） */
    void updateDictData(SysDictData dictData);

    /** 批量逻辑删除字典数据（清理字典缓存） */
    void deleteDictDataByIds(List<Long> dictCodes);
}
