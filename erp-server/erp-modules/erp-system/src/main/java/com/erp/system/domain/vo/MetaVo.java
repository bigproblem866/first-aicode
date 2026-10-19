package com.erp.system.domain.vo;

import lombok.Data;

/**
 * 路由元信息，对应 vue-router 路由对象里的 meta 字段。
 */
@Data
public class MetaVo {

    /** 菜单标题，显示在侧边栏、面包屑、浏览器标签页上 */
    private String title;

    /** Element Plus 图标名（不含 el-icon- 前缀），如 Setting、Goods */
    private String icon;

    /** 是否禁用 keep-alive 缓存（菜单 is_cache=1 时为 true） */
    private Boolean noCache;

    /** 外链地址，菜单配置为外链时使用 */
    private String link;

    public MetaVo() {
    }

    public MetaVo(String title, String icon, Boolean noCache, String link) {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
        this.link = link;
    }
}
