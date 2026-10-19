package com.erp.system.domain.vo;

import com.erp.common.core.domain.TreeSelectVo;
import lombok.Data;

import java.util.List;

/**
 * 「角色分配菜单」页需要的组合数据：完整的菜单树 + 该角色已勾选的菜单 ID。
 * <p>
 * 合成一个接口返回，而不是让前端调两次：
 * 前端拿到树之后要立刻按 checkedKeys 回显勾选状态，分两次请求会出现
 * 「树先渲染出来、勾选状态晚一拍才显示」的闪烁，体验很差。
 */
@Data
public class MenuTreeSelectVo {

    /** 完整菜单树（所有菜单，含按钮） */
    private List<TreeSelectVo> menus;

    /** 该角色已授权的菜单 ID */
    private List<Long> checkedKeys;
}
