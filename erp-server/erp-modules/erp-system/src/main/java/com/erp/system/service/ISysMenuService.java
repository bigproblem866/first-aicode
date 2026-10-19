package com.erp.system.service;

import com.erp.framework.security.LoginUser;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.vo.MenuTreeSelectVo;
import com.erp.system.domain.vo.RouterVo;
import com.erp.common.core.domain.TreeSelectVo;

import java.util.List;

/**
 * 菜单服务。
 */
public interface ISysMenuService {

    /**
     * 菜单管理页的列表（含按钮）。
     * <p>
     * <b>返回扁平列表而不是树</b>：前端 el-table 的树形展示需要 children 数组，
     * 由前端用 handleTree 把扁平列表组装出来 —— 这样同一份数据既可以树形展示，
     * 也可以按 parent_id 排序平铺，前端更灵活。返回嵌套结构反而限制住了前端。
     * 已按 parent_id、order_num 排好序，前端不需要再排。
     */
    List<SysMenu> selectMenuList(SysMenu query);

    MenuTreeSelectVo selectMenuTreeSelectByRoleId(Long roleId);

    SysMenu selectMenuById(Long menuId);

    void insertMenu(SysMenu menu);

    void updateMenu(SysMenu menu);

    void deleteMenuById(Long menuId);

    /** 批量删除。逐个校验是否有子菜单，任何一个不合法就整体回滚 */
    void deleteMenuByIds(List<Long> menuIds);

    /** 构建前端动态路由，登录后由 /api/auth/routers 返回 */
    List<RouterVo> buildRouters(LoginUser loginUser);

    /** 角色分配菜单时用的树形下拉 */
    List<TreeSelectVo> buildMenuTreeSelect();

    /** 角色已授权的菜单 ID 列表，角色编辑页回显用 */
    List<Long> selectMenuIdsByRoleId(Long roleId);
}
