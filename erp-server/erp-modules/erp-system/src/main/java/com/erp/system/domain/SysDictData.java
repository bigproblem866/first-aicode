package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据实体，对应表 sys_dict_data。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    private Integer dictSort;

    /** 界面显示的文字 */
    @NotBlank(message = "字典标签不能为空")
    private String dictLabel;

    /** 存进业务表的值 */
    @NotBlank(message = "字典键值不能为空")
    private String dictValue;

    /** 所属字典类型，对应 sys_dict_type.dict_type */
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    private String cssClass;

    /** Element Plus 的 tag 类型：success / info / warning / danger，表格回显上色用 */
    private String listClass;

    /** 是否默认选中：Y是 N否 */
    private String isDefault;

    /** 状态：0正常 1停用 */
    private String status;

    private String remark;
}
