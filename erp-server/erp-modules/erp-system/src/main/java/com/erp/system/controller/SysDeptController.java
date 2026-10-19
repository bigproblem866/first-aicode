package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.common.core.utils.StringUtils;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.dto.SysDeptQuery;
import com.erp.common.core.domain.TreeSelectVo;
import com.erp.system.service.ISysDeptService;
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
 * 部门管理接口。
 * <p>
 * 部门数量有限，列表接口不分页，直接返回嵌套树（前端表格组件按树形渲染）。
 * 权限串 {@code system:dept:xxx} 必须与菜单种子数据的 perms 列逐字一致。
 */
@Tag(name = "部门管理", description = "部门的增删改查与树形下拉")
@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final ISysDeptService sysDeptService;

    @Operation(summary = "部门列表", description = "树形结构，不分页")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public R<List<SysDept>> list(SysDeptQuery query) {
        return R.ok(sysDeptService.selectDeptList(query));
    }

    @Operation(summary = "部门详情")
    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping("/{deptId}")
    public R<SysDept> getInfo(@PathVariable("deptId") Long deptId) {
        return R.ok(sysDeptService.selectDeptById(deptId));
    }

    @Operation(summary = "新增部门", description = "ancestors 由服务端按上级部门自动计算")
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysDept dept) {
        sysDeptService.insertDept(dept);
        return R.ok();
    }

    @Operation(summary = "修改部门", description = "检测环并同步下级部门的 ancestors")
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysDept dept) {
        sysDeptService.updateDept(dept);
        return R.ok();
    }

    @Operation(summary = "删除部门", description = "ids 为逗号分隔字符串；有下级部门或存在归属用户时禁止删除")
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @DeleteMapping("/{deptIds}")
    public R<Void> remove(@PathVariable("deptIds") String deptIds) {
        sysDeptService.deleteDeptByIds(StringUtils.splitToLong(deptIds));
        return R.ok();
    }

    @Operation(summary = "部门树下拉", description = "返回正常状态部门的树形下拉，供 el-tree-select 使用")
    @GetMapping("/treeselect")
    public R<List<TreeSelectVo>> treeselect() {
        return R.ok(sysDeptService.buildDeptTreeSelect());
    }
}
