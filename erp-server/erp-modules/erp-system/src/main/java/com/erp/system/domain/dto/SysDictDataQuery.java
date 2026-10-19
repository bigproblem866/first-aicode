package com.erp.system.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictDataQuery extends PageQuery {

    /** 字典类型，精确匹配。字典数据页会带上下拉的当前类型 */
    private String dictType;

    /** 字典标签，模糊匹配 */
    private String dictLabel;

    /** 状态：0正常 1停用 */
    private String status;
}
