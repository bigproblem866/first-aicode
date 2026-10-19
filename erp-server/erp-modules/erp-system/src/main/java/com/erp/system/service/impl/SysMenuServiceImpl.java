package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.constant.CommonConstants;
import com.erp.common.core.exception.ServiceException;
import com.erp.common.core.utils.TreeUtils;
import com.erp.framework.security.LoginUser;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.vo.MenuTreeSelectVo;
import com.erp.system.domain.vo.MetaVo;
import com.erp.system.domain.vo.RouterVo;
import com.erp.common.core.domain.TreeSelectVo;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.system.mapper.SysRoleMenuMapper;
import com.erp.system.service.ISysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单服务实现。
 * <p>
 * 本类里最值得看的是 {@link #buildRouters(LoginUser)}：它把数据库里的菜单表
 * 翻译成 vue-router 能直接 addRoute 的路由数组。这是整个前后端权限联动的枢纽。
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements ISysMenuService {

    private static final Long ROOT_PARENT_ID = 0L;

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysMenu> selectMenuList(SysMenu query) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            wrapper.like(query.getMenuName() != null, SysMenu::getMenuName, query.getMenuName());
            wrapper.eq(query.getStatus() != null, SysMenu::getStatus, query.getStatus());
            wrapper.eq(query.getVisible() != null, SysMenu::getVisible, query.getVisible());
        }
        wrapper.orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getOrderNum);
        return sysMenuMapper.selectList(wrapper);
    }

    @Override
    public SysMenu selectMenuById(Long menuId) {
        return sysMenuMapper.selectById(menuId);
    }

    @Override
    public void insertMenu(SysMenu menu) {
        checkMenuNameUnique(menu, null);
        sysMenuMapper.insert(menu);
    }

    @Override
    public void updateMenu(SysMenu menu) {
        if (menu.getId() == null) {
            throw new ServiceException("菜单ID不能为空");
        }
        if (menu.getId().equals(menu.getParentId())) {
            throw new ServiceException("上级菜单不能选择自己");
        }
        checkMenuNameUnique(menu, menu.getId());
        sysMenuMapper.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenuById(Long menuId) {
        // 有子菜单就不允许删：直接删父级会让子菜单变成孤儿节点，
        // 前端渲染出一堆挂在不存在父级下的菜单，比报错更难修
        Long childCount = sysMenuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (childCount != null && childCount > 0) {
            throw new ServiceException("存在子菜单，请先删除子菜单");
        }
        // 先清掉角色授权再删菜单。不清的话 sys_role_menu 会留下指向已删菜单的行：
        // 现在看不出问题，但一旦菜单 ID 被重用到别的功能上，
        // 那些角色就会凭空多出一个不该有的权限。
        sysRoleMenuMapper.deleteByMenuId(menuId);
        sysMenuMapper.deleteById(menuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenuByIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            throw new ServiceException("请选择要删除的菜单");
        }
        // 逐个走 deleteMenuById，保留"有子菜单则拒绝"的校验；
        // 事务保证任何一条不合法时整体回滚，不会删一半留一半
        for (Long menuId : menuIds) {
            deleteMenuById(menuId);
        }
    }

    /**
     * 构建前端路由。
     * <p>
     * 超级管理员取全部菜单；普通用户取自己角色授权的菜单。
     * 转换规则（与 SysMenu 字段注释、R__seed_system_menu.sql 对应）：
     * <ul>
     *   <li>一级目录：path 加前导 /，component 固定为 Layout</li>
     *   <li>二级目录：component 为 ParentView（多级目录的中间层）</li>
     *   <li>菜单：component 用数据库里的 component 字段</li>
     * </ul>
     */
    @Override
    public List<RouterVo> buildRouters(LoginUser loginUser) {
        boolean superAdmin = loginUser.getRoles() != null
                && loginUser.getRoles().contains(CommonConstants.SUPER_ADMIN_ROLE_KEY);

        List<SysMenu> menus = superAdmin
                ? sysMenuMapper.selectMenuTreeAll()
                : sysMenuMapper.selectMenuTreeByUserId(loginUser.getUserId());

        List<SysMenu> tree = TreeUtils.build(menus,
                SysMenu::getId, SysMenu::getParentId, SysMenu::setChildren, ROOT_PARENT_ID);

        return toRouterVos(tree, true);
    }

    @Override
    public List<TreeSelectVo> buildMenuTreeSelect() {
        List<SysMenu> menus = sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .orderByAsc(SysMenu::getParentId)
                        .orderByAsc(SysMenu::getOrderNum));
        List<SysMenu> tree = TreeUtils.build(menus,
                SysMenu::getId, SysMenu::getParentId, SysMenu::setChildren, ROOT_PARENT_ID);
        return toTreeSelectVos(tree);
    }

    @Override
    public List<Long> selectMenuIdsByRoleId(Long roleId) {
        return sysMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    public MenuTreeSelectVo selectMenuTreeSelectByRoleId(Long roleId) {
        MenuTreeSelectVo vo = new MenuTreeSelectVo();
        vo.setMenus(buildMenuTreeSelect());
        vo.setCheckedKeys(selectMenuIdsByRoleId(roleId));
        return vo;
    }

    // ------------------------------------------------------------------
    // 内部方法
    // ------------------------------------------------------------------

    private void checkMenuNameUnique(SysMenu menu, Long excludeId) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getMenuName, menu.getMenuName())
                .eq(SysMenu::getParentId, menu.getParentId());
        if (excludeId != null) {
            wrapper.ne(SysMenu::getId, excludeId);
        }
        Long count = sysMenuMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new ServiceException("同级下已存在同名菜单：" + menu.getMenuName());
        }
    }

    private List<RouterVo> toRouterVos(List<SysMenu> menus, boolean topLevel) {
        List<RouterVo> routers = new ArrayList<>();
        if (menus == null) {
            return routers;
        }
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden(CommonConstants.STATUS_DISABLE.equals(menu.getVisible()));
            router.setName(buildRouteName(menu));
            router.setPath(buildRoutePath(menu, topLevel));
            router.setComponent(buildComponent(menu, topLevel));
            // 仅用于前端功能导航页分区，不参与路由
            router.setGroupName(menu.getGroupName());
            // 目录始终显示：只有一个子菜单时也保留父级，否则侧边栏会突然"少一层"，用户会以为菜单丢了
            router.setAlwaysShow(true);
            router.setMeta(new MetaVo(
                    menu.getMenuName(),
                    menu.getIcon(),
                    Integer.valueOf(1).equals(menu.getIsCache()),
                    null));

            List<SysMenu> children = menu.getChildren();
            if (children != null && !children.isEmpty()) {
                router.setChildren(toRouterVos(children, false));
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 路由名。
     * <p>
     * 用 path 首字母大写，因为前端 keep-alive 与 addRoute 都要求路由名唯一。
     * path 在同一个父级下本来就要求唯一（否则 vue-router 会覆盖），
     * 所以用它派生名字最省事。注意出库单/入库单共用组件但 path 不同，名字也就不同。
     */
    private String buildRouteName(SysMenu menu) {
        String path = menu.getPath();
        if (path == null || path.isBlank()) {
            return "Menu" + menu.getId();
        }
        String cleaned = path.replace("/", "");
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private String buildRoutePath(SysMenu menu, boolean topLevel) {
        String path = menu.getPath() == null ? "" : menu.getPath();
        // 一级路由必须以 / 开头，否则 vue-router 会当成相对路径挂到当前路由下
        if (topLevel && !path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

    private String buildComponent(SysMenu menu, boolean topLevel) {
        if (CommonConstants.MENU_TYPE_DIR.equals(menu.getMenuType())) {
            return topLevel ? CommonConstants.LAYOUT : CommonConstants.PARENT_VIEW;
        }
        return menu.getComponent();
    }

    private List<TreeSelectVo> toTreeSelectVos(List<SysMenu> menus) {
        List<TreeSelectVo> result = new ArrayList<>();
        if (menus == null) {
            return result;
        }
        for (SysMenu menu : menus) {
            List<TreeSelectVo> childVos = menu.getChildren() == null || menu.getChildren().isEmpty()
                    ? null : toTreeSelectVos(menu.getChildren());
            result.add(new TreeSelectVo(String.valueOf(menu.getId()), menu.getMenuName(), childVos));
        }
        return result;
    }
}
