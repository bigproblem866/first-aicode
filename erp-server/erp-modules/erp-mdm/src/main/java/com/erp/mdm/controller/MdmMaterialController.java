package com.erp.mdm.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.mdm.domain.MdmMaterial;
import com.erp.mdm.domain.dto.MdmMaterialDto;
import com.erp.mdm.domain.dto.MdmMaterialQuery;
import com.erp.mdm.domain.vo.MdmMaterialVo;
import com.erp.mdm.service.IMdmMaterialService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 物料管理接口（本模块的照抄模板）。
 * <p>
 * 路径直接写全 /api/mdm/material（含 /api），前端 Vite 代理不做 rewrite。
 * 权限串必须与 R__seed_system_menu.sql 的 perms 列逐字一致。
 */
@Tag(name = "主数据-物料", description = "物料的增删改查与下拉")
@RestController
@RequestMapping("/api/mdm/material")
@RequiredArgsConstructor
public class MdmMaterialController {

    private final IMdmMaterialService materialService;

    @Operation(summary = "物料分页列表")
    @PreAuthorize("@ss.hasPermi('mdm:material:list')")
    @GetMapping("/list")
    public R<PageResult<MdmMaterialVo>> list(MdmMaterialQuery query) {
        return R.ok(materialService.pageList(query));
    }

    @Operation(summary = "物料下拉", description = "单据选料用，返回精简字段，免细粒度权限")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/optionselect")
    public R<List<MdmMaterialVo>> optionselect(@RequestParam(required = false) String keyword) {
        return R.ok(materialService.listOptions(keyword));
    }

    @Operation(summary = "物料详情")
    @PreAuthorize("@ss.hasPermi('mdm:material:query')")
    @GetMapping("/{id}")
    public R<MdmMaterial> getInfo(@PathVariable Long id) {
        return R.ok(materialService.selectById(id));
    }

    @Operation(summary = "新增物料")
    @PreAuthorize("@ss.hasPermi('mdm:material:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody MdmMaterialDto dto) {
        materialService.insert(dto);
        return R.ok();
    }

    @Operation(summary = "修改物料")
    @PreAuthorize("@ss.hasPermi('mdm:material:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody MdmMaterialDto dto) {
        materialService.update(dto);
        return R.ok();
    }

    @Operation(summary = "删除物料", description = "逻辑删除，支持批量：/api/mdm/material/1,2,3")
    @PreAuthorize("@ss.hasPermi('mdm:material:remove')")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        materialService.deleteByIds(Arrays.asList(ids));
        return R.ok();
    }
}
