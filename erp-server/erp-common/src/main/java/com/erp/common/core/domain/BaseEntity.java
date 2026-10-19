package com.erp.common.core.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有业务实体的基类，对应建表规范里「每张表必须带的六个基础字段」。
 * <p>
 * create_by / create_time / update_by / update_time / is_deleted 的赋值不是手写的，
 * 由 {@code com.erp.framework.mybatis.ErpMetaObjectHandler} 自动填充
 * （见该类注释：为什么 create_by 不能指望数据库默认值兜底）。
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键。AUTO = 用数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 创建人（存用户名） */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新人（存用户名） */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记：0 正常 / 1 已删除。
     * <p>
     * 全局配置见 application.yml 的 {@code mybatis-plus.global-config.db-config.logic-delete-field}，
     * 两者必须一致。调用 {@code removeById()} 会走 UPDATE ... SET is_deleted = 1，
     * 所有查询自动追加 {@code is_deleted = 0}。
     * <p>
     * 标记 {@link JsonIgnore}：这是内部字段，不需要回传给前端，也避免前端误传值覆盖。
     */
    @JsonIgnore
    @TableLogic
    @TableField(value = "is_deleted", fill = FieldFill.INSERT)
    private Integer isDeleted;
}
