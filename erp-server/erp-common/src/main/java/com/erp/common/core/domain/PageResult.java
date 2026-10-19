package com.erp.common.core.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页返回。最终 JSON 形如：
 * <pre>
 *   { "code": 200, "msg": "操作成功", "data": { "total": 42, "rows": [ ... ] } }
 * </pre>
 * 前端分页组件固定取 {@code data.total} / {@code data.rows}。
 */
@Data
public class PageResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 总条数 */
    private long total;
    /** 当前页数据 */
    private List<T> rows;

    public PageResult() {
        this.total = 0L;
        this.rows = Collections.emptyList();
    }

    public PageResult(long total, List<T> rows) {
        this.total = total;
        this.rows = rows == null ? Collections.emptyList() : rows;
    }

    /** 直接由 MyBatis-Plus 分页对象构造（实体即出参时用） */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /** 实体分页 → VO 列表（保留 total，逐条转换） */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        List<T> rows = page.getRecords() == null
                ? Collections.emptyList()
                : page.getRecords().stream().map(converter).toList();
        return new PageResult<>(page.getTotal(), rows);
    }

    /** 由 IPage 的总数与已经转换好的列表构造（联表查询自写 SQL 时常用） */
    public static <T> PageResult<T> of(IPage<?> page, List<T> rows) {
        return new PageResult<>(page.getTotal(), rows);
    }

    /** 空结果 */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, Collections.emptyList());
    }
}
