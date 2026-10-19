package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型实体，对应表 sys_dict_type。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    /**
     * 字典类型标识，如 mdm_unit。
     * <p>
     * 只允许字母数字下划线：这个值会被当作缓存 key 和查询条件，带特殊字符容易出问题；
     * 前端也是按这个字符串去请求字典数据的。
     */
    @NotBlank(message = "字典类型不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "字典类型只能由小写字母、数字、下划线组成，且以字母开头")
    private String dictType;

    /** 状态：0正常 1停用 */
    private String status;

    private String remark;
}
