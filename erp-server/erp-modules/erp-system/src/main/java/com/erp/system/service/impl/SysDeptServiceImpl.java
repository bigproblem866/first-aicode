package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.StringUtils;
import com.erp.common.core.utils.TreeUtils;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.dto.SysDeptQuery;
import com.erp.common.core.domain.TreeSelectVo;
import com.erp.system.mapper.SysDeptMapper;
import com.erp.system.service.ISysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门服务实现。
 * <p>
 * <b>ancestors 是本类最需要小心的字段</b>：它是一串逗号分隔的祖级 ID（如 {@code 0,100}），
 * "查某部门及所有下级"、数据权限过滤都靠它。因此：
 * <ul>
 *   <li>新增/移动部门时由服务端重算，不接受前端传入；</li>
 *   <li>移动部门时，其下所有子部门的 ancestors 也必须跟着重算，否则子孙的祖级链会指向旧位置。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements ISysDeptService {

    /** 根部门的父 ID，与 TreeUtils 的 rootParentId 保持一致 */
    private static final Long ROOT_PARENT_ID = 0L;
    /** 顶级部门的祖级列表 */
    private static final String ROOT_ANCESTORS = "0";

    private final SysDeptMapper sysDeptMapper;

    @Override
    public List<SysDept> selectDeptList(SysDeptQuery query) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.isNotBlank(query.getDeptName()), SysDept::getDeptName, query.getDeptName());
            wrapper.eq(StringUtils.isNotBlank(query.getStatus()), SysDept::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(SysDept::getParentId).orderByAsc(SysDept::getOrderNum);
        List<SysDept> depts = sysDeptMapper.selectList(wrapper);
        return TreeUtils.build(depts,
                SysDept::getId, SysDept::getParentId, SysDept::setChildren, ROOT_PARENT_ID);
    }

    @Override
    public SysDept selectDeptById(Long deptId) {
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            throw new ServiceException("部门不存在");
        }
        return dept;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertDept(SysDept dept) {
        if (dept.getParentId() == null) {
            dept.setParentId(ROOT_PARENT_ID);
        }
        dept.setAncestors(resolveAncestors(dept.getParentId()));
        sysDeptMapper.insert(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDept(SysDept dept) {
        if (dept.getId() == null) {
            throw new ServiceException("部门ID不能为空");
        }
        SysDept existing = sysDeptMapper.selectById(dept.getId());
        if (existing == null) {
            throw new ServiceException("部门不存在");
        }
        if (dept.getId().equals(dept.getParentId())) {
            throw new ServiceException("上级部门不能选择自己");
        }

        String newAncestors = resolveAncestors(dept.getParentId());
        // 环检测：新的祖级链里出现了「自己」，说明把部门挂到了自己的下级之下。
        // 正常数据不会出现；一旦出现，整棵树的查询会互相包含，前端渲染成死循环。
        if (containsId(newAncestors, dept.getId())) {
            throw new ServiceException("不能将部门移动到自己的下级部门下");
        }

        dept.setAncestors(newAncestors);
        sysDeptMapper.updateById(dept);
        // 自身位置变了，其所有子部门的 ancestors 也要跟着重算
        syncChildrenAncestors(dept.getId(), newAncestors);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDeptByIds(List<Long> deptIds) {
        if (StringUtils.isEmpty(deptIds)) {
            throw new ServiceException("请选择要删除的部门");
        }
        for (Long deptId : deptIds) {
            Long childCount = sysDeptMapper.selectCount(
                    new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, deptId));
            if (childCount != null && childCount > 0) {
                throw new ServiceException("存在下级部门，请先删除下级部门");
            }
            long userCount = sysDeptMapper.selectUserCountByDeptId(deptId);
            if (userCount > 0) {
                throw new ServiceException("部门下存在用户，不允许删除");
            }
        }
        sysDeptMapper.deleteByIds(deptIds);
    }

    @Override
    public List<TreeSelectVo> buildDeptTreeSelect() {
        List<SysDept> depts = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getStatus, CommonConstants.STATUS_NORMAL)
                .orderByAsc(SysDept::getParentId)
                .orderByAsc(SysDept::getOrderNum));
        List<SysDept> tree = TreeUtils.build(depts,
                SysDept::getId, SysDept::getParentId, SysDept::setChildren, ROOT_PARENT_ID);
        return toTreeSelectVos(tree);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /** 由父部门推导祖级列表：父的 ancestors + "," + 父 id；顶级则为 "0" */
    private String resolveAncestors(Long parentId) {
        if (parentId == null || ROOT_PARENT_ID.equals(parentId)) {
            return ROOT_ANCESTORS;
        }
        SysDept parent = sysDeptMapper.selectById(parentId);
        if (parent == null) {
            throw new ServiceException("上级部门不存在");
        }
        return parent.getAncestors() + "," + parent.getId();
    }

    /**
     * 重算某部门所有后代的 ancestors。
     * <p>
     * 用队列逐层下推而不是递归：层级被改坏成环时递归会栈溢出且难以定位；
     * 这里额外用 visited 集合兜底，遇到环也只是提前结束，不会死循环。
     * 每个节点的 ancestors 存在 {@code ancestorsByNode} 里，供推导它的下一层使用。
     */
    private void syncChildrenAncestors(Long deptId, String deptAncestors) {
        Map<Long, String> ancestorsByNode = new HashMap<>();
        ancestorsByNode.put(deptId, deptAncestors);

        Deque<Long> queue = new ArrayDeque<>();
        queue.add(deptId);
        Set<Long> visited = new HashSet<>();
        visited.add(deptId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            String currentAncestors = ancestorsByNode.get(currentId);
            List<SysDept> children = sysDeptMapper.selectList(
                    new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, currentId));
            for (SysDept child : children) {
                String childAncestors = currentAncestors + "," + currentId;
                SysDept update = new SysDept();
                update.setId(child.getId());
                update.setAncestors(childAncestors);
                sysDeptMapper.updateById(update);
                ancestorsByNode.put(child.getId(), childAncestors);
                if (visited.add(child.getId())) {
                    queue.add(child.getId());
                }
            }
        }
    }

    /** 祖级列表（逗号分隔）中是否包含某个 ID */
    private boolean containsId(String ancestors, Long id) {
        if (StringUtils.isBlank(ancestors) || id == null) {
            return false;
        }
        String target = String.valueOf(id);
        for (String segment : ancestors.split(",")) {
            if (target.equals(segment.trim())) {
                return true;
            }
        }
        return false;
    }

    private List<TreeSelectVo> toTreeSelectVos(List<SysDept> depts) {
        List<TreeSelectVo> result = new ArrayList<>();
        if (depts == null) {
            return result;
        }
        for (SysDept dept : depts) {
            List<TreeSelectVo> childVos = dept.getChildren() == null || dept.getChildren().isEmpty()
                    ? null : toTreeSelectVos(dept.getChildren());
            result.add(new TreeSelectVo(String.valueOf(dept.getId()), dept.getDeptName(), childVos));
        }
        return result;
    }
}
