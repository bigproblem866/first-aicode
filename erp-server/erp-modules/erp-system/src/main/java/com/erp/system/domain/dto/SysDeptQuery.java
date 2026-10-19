package com.erp.system.domain.dto;

import lombok.Data;

/**
 * 部门查询条件。部门是树形结构、数量有限，列表接口不做分页，所以不继承 PageQuery。
 */
@Data
public class SysDeptQuery {

    /** 部门名称，模糊匹配 */
    private String deptName;

    /** 状态：0正常 1停用 */
    private String status;
}
