package com.erp.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.BeanCopyUtils;
import com.erp.common.core.utils.TreeUtils;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.MdmMaterialCategory;
import com.erp.mdm.domain.dto.MdmMaterialCategoryDto;
import com.erp.mdm.domain.dto.MdmMaterialCategoryQuery;
import com.erp.common.core.domain.TreeSelectVo;
import com.erp.mdm.mapper.MdmMaterialCategoryMapper;
import com.erp.mdm.mapper.MdmMaterialMapper;
import com.erp.mdm.service.IMdmMaterialCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 物料分类服务实现。
 * <p>
 * ancestors 的维护规则（与 sys_dept 一致）：顶层为 "0"，子节点为「父的 ancestors + ',' + 父id」。
 * 这个字段是「按分类查其所有下级物料」的基础，改动父级层级时必须级联刷新所有后代，
 * 否则查到的是旧路径，症状是"物料换了分类后还能用旧分类搜到"。
 */
@Service
@RequiredArgsConstructor
public class MdmMaterialCategoryServiceImpl implements IMdmMaterialCategoryService {

    private static final Long ROOT_PARENT_ID = 0L;

    private final MdmMaterialCategoryMapper categoryMapper;
    private final MdmMaterialMapper materialMapper;

    @Override
    public List<MdmMaterialCategory> selectCategoryList(MdmMaterialCategoryQuery query) {
        LambdaQueryWrapper<MdmMaterialCategory> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(query.getCategoryCode() != null, MdmMaterialCategory::getCategoryCode, query.getCategoryCode());
            wrapper.like(query.getCategoryName() != null, MdmMaterialCategory::getCategoryName, query.getCategoryName());
            wrapper.eq(query.getStatus() != null, MdmMaterialCategory::getStatus, query.getStatus());
        }
        wrapper.orderByAsc(MdmMaterialCategory::getParentId).orderByAsc(MdmMaterialCategory::getOrderNum);
        List<MdmMaterialCategory> list = categoryMapper.selectList(wrapper);
        // 查询条件过滤掉中间层时，TreeUtils 会把挂不上的节点丢弃（孤儿节点），不会出错
        return TreeUtils.build(list, MdmMaterialCategory::getId, MdmMaterialCategory::getParentId,
                MdmMaterialCategory::setChildren, ROOT_PARENT_ID);
    }

    @Override
    public MdmMaterialCategory selectById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void insert(MdmMaterialCategoryDto dto) {
        checkCodeUnique(dto.getCategoryCode(), null);
        MdmMaterialCategory entity = BeanCopyUtils.copy(dto, MdmMaterialCategory.class);
        entity.setAncestors(buildAncestors(dto.getParentId()));
        if (entity.getStatus() == null) {
            entity.setStatus(CommonConstants.STATUS_NORMAL);
        }
        if (entity.getOrderNum() == null) {
            entity.setOrderNum(0);
        }
        categoryMapper.insert(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(MdmMaterialCategoryDto dto) {
        if (dto.getId() == null) {
            throw new ServiceException("分类ID不能为空");
        }
        MdmMaterialCategory exist = categoryMapper.selectById(dto.getId());
        if (exist == null) {
            throw new ServiceException("分类不存在或已删除");
        }
        if (dto.getId().equals(dto.getParentId())) {
            throw new ServiceException("上级分类不能选择自己");
        }
        checkCodeUnique(dto.getCategoryCode(), dto.getId());

        String oldAncestors = exist.getAncestors();
        String newAncestors = buildAncestors(dto.getParentId());

        MdmMaterialCategory entity = BeanCopyUtils.copy(dto, MdmMaterialCategory.class);
        entity.setAncestors(newAncestors);
        categoryMapper.updateById(entity);

        // 层级变了才需要级联刷新后代；同级移动（只改 order_num）不动 ancestors，跳过整棵树的写操作
        if (!newAncestors.equals(oldAncestors)) {
            refreshChildrenAncestors(dto.getId(), oldAncestors, newAncestors);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Long id) {
        MdmMaterialCategory exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("分类不存在或已删除");
        }
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<MdmMaterialCategory>().eq(MdmMaterialCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new ServiceException("存在下级分类，请先删除下级分类");
        }
        Long materialCount = materialMapper.selectCount(
                new LambdaQueryWrapper<MdmMaterial>().eq(MdmMaterial::getCategoryId, id));
        if (materialCount != null && materialCount > 0) {
            throw new ServiceException("该分类下存在物料，不能删除");
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public List<TreeSelectVo> buildTreeSelect() {
        LambdaQueryWrapper<MdmMaterialCategory> wrapper = new LambdaQueryWrapper<MdmMaterialCategory>()
                .orderByAsc(MdmMaterialCategory::getParentId)
                .orderByAsc(MdmMaterialCategory::getOrderNum);
        List<MdmMaterialCategory> list = categoryMapper.selectList(wrapper);
        List<MdmMaterialCategory> tree = TreeUtils.build(list, MdmMaterialCategory::getId,
                MdmMaterialCategory::getParentId, MdmMaterialCategory::setChildren, ROOT_PARENT_ID);
        return toTreeSelectVos(tree);
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    /** 父的 ancestors + "," + 父id；顶级分类固定为 "0" */
    private String buildAncestors(Long parentId) {
        if (parentId == null || ROOT_PARENT_ID.equals(parentId)) {
            return "0";
        }
        MdmMaterialCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new ServiceException("上级分类不存在");
        }
        return parent.getAncestors() + "," + parentId;
    }

    /**
     * 级联刷新后代 ancestors：把 oldPrefix 换成 newPrefix。
     * <p>
     * 只查「ancestors 以 oldPrefix 开头」的节点（即该节点的全部后代），
     * 逐条替换前缀后更新，避免把整表读出来。
     */
    private void refreshChildrenAncestors(Long id, String oldAncestors, String newAncestors) {
        String oldPrefix = oldAncestors + "," + id;
        List<MdmMaterialCategory> descendants = categoryMapper.selectList(
                new LambdaQueryWrapper<MdmMaterialCategory>()
                        .likeRight(MdmMaterialCategory::getAncestors, oldPrefix + ","));
        for (MdmMaterialCategory child : descendants) {
            String childAncestors = newAncestors + "," + id + child.getAncestors().substring(oldPrefix.length());
            MdmMaterialCategory update = new MdmMaterialCategory();
            update.setId(child.getId());
            update.setAncestors(childAncestors);
            categoryMapper.updateById(update);
        }
    }

    private void checkCodeUnique(String categoryCode, Long excludeId) {
        LambdaQueryWrapper<MdmMaterialCategory> wrapper = new LambdaQueryWrapper<MdmMaterialCategory>()
                .eq(MdmMaterialCategory::getCategoryCode, categoryCode);
        if (excludeId != null) {
            wrapper.ne(MdmMaterialCategory::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException("分类编码已存在：" + categoryCode);
        }
    }

    private List<TreeSelectVo> toTreeSelectVos(List<MdmMaterialCategory> categories) {
        List<TreeSelectVo> result = new ArrayList<>();
        if (categories == null) {
            return result;
        }
        for (MdmMaterialCategory category : categories) {
            List<TreeSelectVo> childVos = category.getChildren() == null || category.getChildren().isEmpty()
                    ? null : toTreeSelectVos(category.getChildren());
            result.add(new TreeSelectVo(String.valueOf(category.getId()), category.getCategoryName(), childVos));
        }
        return result;
    }
}
