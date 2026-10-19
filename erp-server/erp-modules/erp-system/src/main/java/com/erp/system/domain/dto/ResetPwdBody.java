package com.erp.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置他人密码的入参。
 */
@Data
public class ResetPwdBody {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 30, message = "密码长度需在 6-30 个字符之间")
    private String password;
}
