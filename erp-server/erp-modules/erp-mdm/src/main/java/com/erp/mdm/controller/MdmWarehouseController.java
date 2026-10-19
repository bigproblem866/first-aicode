package com.erp.mdm.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.mdm.domain.MdmWarehouse;
import com.erp.mdm.domain.dto.MdmWarehouseDto;
import com.erp.mdm.domain.dto.MdmWarehouseQuery;
import com.erp.mdm.service.IMdmWarehouseService;
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
 * 仓库管理接口。
 */
@Tag(name = "主数据-仓库", description = "仓库的增删改查")
@RestController
@RequestMapping("/api/mdm/warehouse")
@RequiredArgsConstructor
public class MdmWarehouseController {

    private final IMdmWarehouseService warehouseService;

    @Operation(summary = "仓库分页列表")
    @PreAuthorize("@ss.hasPermi('mdm:warehouse:list')")
    @GetMapping("/list")
    public R<PageResult<MdmWarehouse>> list(MdmWarehouseQuery query) {
        return R.ok(warehouseService.pageList(query));
    }

    @Operation(summary = "仓库下拉")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/optionselect")
    public R<List<MdmWarehouse>> optionselect() {
        return R.ok(warehouseService.listOptions());
    }

    @Operation(summary = "仓库详情")
    @PreAuthorize("@ss.hasPermi('mdm:warehouse:query')")
    @GetMapping("/{id}")
    public R<MdmWarehouse> getInfo(@PathVariable Long id) {
        return R.ok(warehouseService.selectById(id));
    }

    @Operation(summary = "新增仓库")
    @PreAuthorize("@ss.hasPermi('mdm:warehouse:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody MdmWarehouseDto dto) {
        warehouseService.insert(dto);
        return R.ok();
    }

    @Operation(summary = "修改仓库")
    @PreAuthorize("@ss.hasPermi('mdm:warehouse:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody MdmWarehouseDto dto) {
        warehouseService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除仓库")
    @PreAuthorize("@ss.hasPermi('mdm:warehouse:remove')")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        warehouseService.deleteByIds(Arrays.asList(ids));
        return R.ok();
    }
}
