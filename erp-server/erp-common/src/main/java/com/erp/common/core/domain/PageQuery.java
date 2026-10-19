package com.erp.common.core.domain;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页查询入参基类。所有列表查询 DTO 继承它。
 * <p>
 * 前端固定传 {@code pageNum} / {@code pageSize} / 可选 {@code orderByColumn} / {@code isAsc}：
 * <pre>
 *   GET /api/mdm/material/list?pageNum=1&amp;pageSize=10&amp;orderByColumn=materialCode&amp;isAsc=desc
 * </pre>
 */
@Data
public class PageQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final long MAX_PAGE_SIZE = 500L;

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;
    /** 每页条数，上限 500，防止前端传 999999 拖垮数据库 */
    private Integer pageSize = 10;
    /** 排序字段，前端传驼峰（如 materialCode），内部转下划线 */
    private String orderByColumn;
    /** 排序方向：asc / desc */
    private String isAsc = "asc";

    /**
     * 转成 MyBatis-Plus 的分页对象。
     * <p>
     * {@code orderByColumn} 做了白名单化（只允许字母数字下划线），防止 SQL 注入——
     * 排序字段是拼接进 SQL 的，不能直接信任前端输入。
     */
    public <T> Page<T> toPage() {
        long num = (pageNum == null || pageNum < 1) ? 1L : pageNum;
        long size = (pageSize == null || pageSize < 1) ? 10L : pageSize;
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        Page<T> page = new Page<>(num, size);

        String column = sanitizeOrderColumn(orderByColumn);
        if (StrUtil.isNotBlank(column)) {
            boolean asc = !"desc".equalsIgnoreCase(StrUtil.trimToEmpty(isAsc));
            page.addOrder(asc ? OrderItem.asc(column) : OrderItem.desc(column));
        }
        return page;
    }

    /** 只放行 [A-Za-z0-9_]，并把驼峰转下划线 */
    private String sanitizeOrderColumn(String column) {
        if (StrUtil.isBlank(column)) {
            return null;
        }
        String trimmed = column.trim();
        if (!trimmed.matches("[A-Za-z0-9_]+")) {
            return null;
        }
        return StrUtil.toUnderlineCase(trimmed);
    }
}
