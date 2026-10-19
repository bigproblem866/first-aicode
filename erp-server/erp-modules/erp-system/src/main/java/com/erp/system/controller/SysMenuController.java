package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.common.core.utils.StringUtils;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.vo.MenuTreeSelectVo;
import com.erp.common.core.domain.TreeSelectVo;
import com.erp.system.service.ISysMenuService;
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
 * 菜单管理。
 * <p>
 * 注意这个模块的特殊性：它管的是「权限本身」。
 * 一旦误删或误改菜单，可能把系统锁死（比如删掉系统管理目录），
 * 所以 Service 里对"有子菜单不允许删"这类校验比别的模块严格。
 */
@Tag(name = "系统管理-菜单", description = "菜单与权限点的维护")
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final ISysMenuService sysMenuService;

    @Operation(summary = "菜单列表", description = "返回扁平列表（不含 children），已按 parent_id、order_num 排序，前端自行组装树形表格")
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public R<List<SysMenu>> list(SysMenu query) {
        return R.ok(sysMenuService.selectMenuList(query));
    }

    @Operation(summary = "菜单详情")
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping("/{id}")
    public R<SysMenu> getById(@PathVariable("id") Long id) {
        return R.ok(sysMenuService.selectMenuById(id));
    }

    @Operation(summary = "新增菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @PostMapping
    public R<Void> add(@Valid @RequestBody SysMenu menu) {
        sysMenuService.insertMenu(menu);
        return R.ok();
    }

    @Operation(summary = "修改菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @PutMapping
    public R<Void> edit(@Valid @RequestBody SysMenu menu) {
        sysMenuService.updateMenu(menu);
        return R.ok();
    }

    @Operation(summary = "删除菜单", description = "ids 为逗号分隔的菜单ID；存在子菜单时拒绝删除")
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable("ids") String ids) {
        sysMenuService.deleteMenuByIds(StringUtils.splitToLong(ids));
        return R.ok();
    }

    @Operation(summary = "菜单树下拉", description = "给角色分配权限时用，含按钮节点")
    @GetMapping("/treeselect")
    public R<List<TreeSelectVo>> treeSelect() {
        return R.ok(sysMenuService.buildMenuTreeSelect());
    }

    @Operation(summary = "角色菜单树 + 已选中项", description = "角色编辑页一次性拿到菜单树与该角色已勾选的菜单ID")
    @GetMapping("/roleMenuTreeselect/{roleId}")
    public R<MenuTreeSelectVo> roleMenuTreeSelect(@PathVariable("roleId") Long roleId) {
        return R.ok(sysMenuService.selectMenuTreeSelectByRoleId(roleId));
    }
}
