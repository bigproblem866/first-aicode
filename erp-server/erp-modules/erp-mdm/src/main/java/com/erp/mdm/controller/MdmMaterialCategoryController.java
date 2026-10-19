package com.erp.mdm.controller;

import com.erp.common.core.domain.R;
import com.erp.mdm.domain.MdmMaterialCategory;
import com.erp.mdm.domain.dto.MdmMaterialCategoryDto;
import com.erp.mdm.domain.dto.MdmMaterialCategoryQuery;
import com.erp.common.core.domain.TreeSelectVo;
import com.erp.mdm.service.IMdmMaterialCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 物料分类接口。列表返回树形、不分页。
 */
@Tag(name = "主数据-物料分类", description = "树形分类的增删改查与树下拉")
@RestController
@RequestMapping("/api/mdm/materialCategory")
@RequiredArgsConstructor
public class MdmMaterialCategoryController {

    private final IMdmMaterialCategoryService categoryService;

    @Operation(summary = "分类列表（树形）")
    @PreAuthorize("@ss.hasPermi('mdm:materialCategory:list')")
    @GetMapping("/list")
    public R<List<MdmMaterialCategory>> list(MdmMaterialCategoryQuery query) {
        return R.ok(categoryService.selectCategoryList(query));
    }

    @Operation(summary = "分类树下拉")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/treeselect")
    public R<List<TreeSelectVo>> treeselect() {
        return R.ok(categoryService.buildTreeSelect());
    }

    @Operation(summary = "分类详情")
    @PreAuthorize("@ss.hasPermi('mdm:materialCategory:query')")
    @GetMapping("/{id}")
    public R<MdmMaterialCategory> getInfo(@PathVariable Long id) {
        return R.ok(categoryService.selectById(id));
    }

    @Operation(summary = "新增分类")
    @PreAuthorize("@ss.hasPermi('mdm:materialCategory:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody MdmMaterialCategoryDto dto) {
        categoryService.insert(dto);
        return R.ok();
    }

    @Operation(summary = "修改分类")
    @PreAuthorize("@ss.hasPermi('mdm:materialCategory:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody MdmMaterialCategoryDto dto) {
        categoryService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除分类", description = "有下级或已被物料引用时不允许删除")
    @PreAuthorize("@ss.hasPermi('mdm:materialCategory:remove')")
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        categoryService.deleteById(id);
        return R.ok();
    }
}
