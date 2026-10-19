package com.erp.inventory.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存流水查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InvStockLogQuery extends PageQuery {

    /** 仓库ID */
    private Long warehouseId;

    /** 物料ID */
    private Long materialId;

    /**
     * 物料编码（模糊匹配）。前端用文本框输入的是编码而不是 ID。
     * <p>
     * 它不参与 SQL：库存模块不能 join mdm_material（模块边界规则），
     * 所以由 Service 先通过 MdmApi 把编码解析成一批物料 ID，再按 ID 过滤。
     * <p>
     * ⚠ 字段名必须与前端传参一致。早期版本这里叫 bizDateBegin，前端传的是
     * bizDateStart，结果日期筛选"看起来有、实际没生效"且不报任何错 ——
     * 这类静默失效只能靠前后端字段名对账发现。
     */
    private String materialCode;

    /** 业务类型：IN / OUT / ADJUST */
    private String bizType;

    /** 业务日期起（含）。与前端 DatePicker 传参一致，命名统一用 Start/End */
    private String bizDateStart;

    /** 业务日期止（含） */
    private String bizDateEnd;
}
