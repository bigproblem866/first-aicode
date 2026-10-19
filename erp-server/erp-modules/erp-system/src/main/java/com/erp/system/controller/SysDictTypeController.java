package com.erp.system.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.common.core.utils.StringUtils;
import com.erp.system.domain.SysDictType;
import com.erp.system.domain.dto.SysDictTypeQuery;
import com.erp.system.service.ISysDictService;
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

/**
 * 字典类型接口。
 * <p>
 * 与 {@link SysDictDataController} 共用同一个 Service：删除类型要连带删除其下数据，
 * 这条跨表规则放在 Service 里，两个 Controller 谁都不用操心。
 */
@Tag(name = "字典类型", description = "字典类型的增删改查")
@RestController
@RequestMapping("/api/system/dict/type")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final ISysDictService sysDictService;

    @Operation(summary = "字典类型列表", description = "分页返回字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/list")
    public R<PageResult<SysDictType>> list(SysDictTypeQuery query) {
        return R.ok(sysDictService.selectDictTypePage(query));
    }

    @Operation(summary = "字典类型详情")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping("/{dictId}")
    public R<SysDictType> getInfo(@PathVariable("dictId") Long dictId) {
        return R.ok(sysDictService.selectDictTypeById(dictId));
    }

    @Operation(summary = "新增字典类型", description = "dictType 唯一")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysDictType dictType) {
        sysDictService.insertDictType(dictType);
        return R.ok();
    }

    @Operation(summary = "修改字典类型", description = "标识改名时会同步其下字典数据的 dict_type")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysDictType dictType) {
        sysDictService.updateDictType(dictType);
        return R.ok();
    }

    @Operation(summary = "删除字典类型", description = "ids 为逗号分隔字符串；同时逻辑删除该类型下的所有字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @DeleteMapping("/{dictIds}")
    public R<Void> remove(@PathVariable("dictIds") String dictIds) {
        sysDictService.deleteDictTypeByIds(StringUtils.splitToLong(dictIds));
        return R.ok();
    }
}
