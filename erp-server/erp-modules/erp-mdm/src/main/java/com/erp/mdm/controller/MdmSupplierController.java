package com.erp.mdm.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.mdm.domain.MdmSupplier;
import com.erp.mdm.domain.dto.MdmSupplierDto;
import com.erp.mdm.domain.dto.MdmSupplierQuery;
import com.erp.mdm.service.IMdmSupplierService;
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

import java.util.Arrays;
import java.util.List;

/**
 * 供应商管理接口。
 */
@Tag(name = "主数据-供应商", description = "供应商的增删改查")
@RestController
@RequestMapping("/api/mdm/supplier")
@RequiredArgsConstructor
public class MdmSupplierController {

    private final IMdmSupplierService supplierService;

    @Operation(summary = "供应商分页列表")
    @PreAuthorize("@ss.hasPermi('mdm:supplier:list')")
    @GetMapping("/list")
    public R<PageResult<MdmSupplier>> list(MdmSupplierQuery query) {
        return R.ok(supplierService.pageList(query));
    }

    @Operation(summary = "供应商下拉")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/optionselect")
    public R<List<MdmSupplier>> optionselect() {
        return R.ok(supplierService.listOptions());
    }

    @Operation(summary = "供应商详情")
    @PreAuthorize("@ss.hasPermi('mdm:supplier:query')")
    @GetMapping("/{id}")
    public R<MdmSupplier> getInfo(@PathVariable Long id) {
        return R.ok(supplierService.selectById(id));
    }

    @Operation(summary = "新增供应商")
    @PreAuthorize("@ss.hasPermi('mdm:supplier:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody MdmSupplierDto dto) {
        supplierService.insert(dto);
        return R.ok();
    }

    @Operation(summary = "修改供应商")
    @PreAuthorize("@ss.hasPermi('mdm:supplier:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody MdmSupplierDto dto) {
        supplierService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除供应商")
    @PreAuthorize("@ss.hasPermi('mdm:supplier:remove')")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        supplierService.deleteByIds(Arrays.asList(ids));
        return R.ok();
    }
}
