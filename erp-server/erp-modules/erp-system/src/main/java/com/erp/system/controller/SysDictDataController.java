package com.erp.system.controller;

import com.erp.common.core.domain.PageResult;
import com.erp.common.core.domain.R;
import com.erp.common.core.utils.StringUtils;
import com.erp.system.domain.SysDictData;
import com.erp.system.domain.dto.SysDictDataQuery;
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

import java.util.List;

/**
 * 字典数据接口。
 * <p>
 * {@code GET /api/system/dict/data/type/{dictType}} 是前端最常用的接口，权限刻意只要求「已登录」：
 * 任何页面都可能用它渲染状态列/下拉，若要求 {@code system:dict:list} 权限，
 * 会出现"有单据权限的人看不到状态文字"。它返回的数据只读、不含敏感信息，放开是安全的。
 */
@Tag(name = "字典数据", description = "字典数据的增删改查与按类型取值")
@RestController
@RequestMapping("/api/system/dict/data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final ISysDictService sysDictService;

    @Operation(summary = "字典数据列表", description = "分页返回，按 dictType 过滤")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/list")
    public R<PageResult<SysDictData>> list(SysDictDataQuery query) {
        return R.ok(sysDictService.selectDictDataPage(query));
    }

    @Operation(summary = "按类型取字典数据", description = "返回该类型下 status='0' 的数据，按 dict_sort 排序；任何登录用户可用")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/type/{dictType}")
    public R<List<SysDictData>> dataByType(@PathVariable("dictType") String dictType) {
        return R.ok(sysDictService.selectDictDataByType(dictType));
    }

    @Operation(summary = "字典数据详情")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping("/{dictCode}")
    public R<SysDictData> getInfo(@PathVariable("dictCode") Long dictCode) {
        return R.ok(sysDictService.selectDictDataById(dictCode));
    }

    @Operation(summary = "新增字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysDictData dictData) {
        sysDictService.insertDictData(dictData);
        return R.ok();
    }

    @Operation(summary = "修改字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysDictData dictData) {
        sysDictService.updateDictData(dictData);
        return R.ok();
    }

    @Operation(summary = "删除字典数据", description = "ids 为逗号分隔字符串")
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @DeleteMapping("/{dictCodes}")
    public R<Void> remove(@PathVariable("dictCodes") String dictCodes) {
        sysDictService.deleteDictDataByIds(StringUtils.splitToLong(dictCodes));
        return R.ok();
    }
}
