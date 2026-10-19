package com.erp.system.domain.dto;

import com.erp.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysDictTypeQuery extends PageQuery {

    /** 字典名称，模糊匹配 */
    private String dictName;

    /** 字典类型标识，模糊匹配 */
    private String dictType;

    /** 状态：0正常 1停用 */
    private String status;
}
