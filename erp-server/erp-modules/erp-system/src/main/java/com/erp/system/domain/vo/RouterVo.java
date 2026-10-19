package com.erp.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * 前端路由对象。后端把菜单树转成这个结构下发，前端 store/permission.js
 * 直接拿它调 {@code router.addRoute()}，不需要在前端硬编码任何菜单。
 * <p>
 * 字段含义与 vue-router 的路由对象一一对应：
 * <pre>
 * {
 *   "name": "User",                  // 路由名，必须唯一（前端 keep-alive 也用它）
 *   "path": "/system",               // 一级目录带 /，子菜单不带（vue-router 会自动拼）
 *   "component": "Layout",           // 一级目录固定 Layout；子菜单是相对 views/ 的路径
 *   "hidden": false,                 // 是否在侧边栏隐藏
 *   "redirect": "noRedirect",        // 目录的默认跳转
 *   "alwaysShow": true,              // 即使只有一个子菜单也显示父级
 *   "meta": { "title": "系统管理", "icon": "Setting", "noCache": false },
 *   "children": [ ... ]
 * }
 * </pre>
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouterVo {

    /** 路由名称，取 path 的首字母大写形式，保证唯一 */
    private String name;

    /** 路由地址 */
    private String path;

    /** 是否隐藏（菜单 visible=1 时为 true，侧边栏不显示但仍可访问） */
    private boolean hidden;

    /** 重定向地址。目录默认 noRedirect，点击时展开而不跳转 */
    private String redirect;

    /**
     * 组件地址。
     * <p>
     * 三个特殊值由前端识别：{@code Layout}（主框架）、{@code ParentView}（多级目录中间层）。
     * 其余是相对 views/ 的路径，前端用 import.meta.glob 匹配到真实组件。
     */
    private String component;

    /** 只有一个子路由时是否仍然显示父级菜单 */
    private Boolean alwaysShow;

    /**
     * 功能分组（如「库存查询」「报表分析」）。
     * 只供前端功能导航页分区显示，**不参与路由匹配**。
     */
    private String groupName;

    private MetaVo meta;

    private List<RouterVo> children;
}
