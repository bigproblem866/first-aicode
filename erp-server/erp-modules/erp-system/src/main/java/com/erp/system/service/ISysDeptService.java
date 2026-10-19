package com.erp.system.service;

import com.erp.system.domain.SysDept;
import com.erp.system.domain.dto.SysDeptQuery;
import com.erp.common.core.domain.TreeSelectVo;

import java.util.List;

/**
 * 部门服务。
 * <p>
 * 部门是"一张表 + parent_id"的树形结构，数量有限，列表接口不分页，直接返回嵌套树。
 * 本类要守住的两件事：
 * <ul>
 *   <li>ancestors 由服务端自动维护，前端不传、也不允许传；</li>
 *   <li>有下级部门或有用户归属的部门不允许删除。</li>
 * </ul>
 */
public interface ISysDeptService {

    /** 部门列表（树形，不分页） */
    List<SysDept> selectDeptList(SysDeptQuery query);

    /** 部门详情 */
    SysDept selectDeptById(Long deptId);

    /** 新增部门：自动计算 ancestors */
    void insertDept(SysDept dept);

    /** 修改部门：检测环、同步下级部门的 ancestors */
    void updateDept(SysDept dept);

    /** 批量逻辑删除部门。有下级部门或有用户归属时禁止删除 */
    void deleteDeptByIds(List<Long> deptIds);

    /** 部门树下拉，供 el-tree-select 使用 */
    List<TreeSelectVo> buildDeptTreeSelect();
}
