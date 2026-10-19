package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体，对应表 sys_user。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private Long deptId;

    @NotBlank(message = "登录账号不能为空")
    @Size(min = 2, max = 50, message = "登录账号长度需在 2-50 个字符之间")
    private String username;

    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 50, message = "用户昵称不能超过 50 个字符")
    private String nickName;

    @Email(message = "邮箱格式不正确")
    @Size(max = 64, message = "邮箱不能超过 64 个字符")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 性别：0男 1女 2未知 */
    private String sex;

    private String avatar;

    /**
     * BCrypt 密码哈希。
     * <p>
     * {@code @JsonProperty(access = WRITE_ONLY)}：允许前端传进来（新增/改密码），
     * 但序列化时绝不输出。加上它就不必担心某个接口不小心把密码哈希返回给前端。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 状态：0正常 1停用 */
    private String status;

    private String loginIp;

    private LocalDateTime loginDate;

    private String remark;

    // ---------------- 非数据库字段（联表/入参携带） ----------------

    /** 所属部门名称，列表页联表带出 */
    @TableField(exist = false)
    private String deptName;

    /** 角色 ID 列表，新增/修改用户时前端传的角色勾选结果 */
    @TableField(exist = false)
    private List<Long> roleIds;

    /** 角色名称列表，列表页展示用 */
    @TableField(exist = false)
    private List<String> roleNames;
}
