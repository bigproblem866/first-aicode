package com.erp.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功返回体。
 * <p>
 * 用对象而不是 {@code Map<String, String>}：接口文档里能看到明确的字段，
 * 前端也不用猜 key 的名字。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVo {

    /** JWT，前端存起来，后续请求放在 Authorization: Bearer xxx 里 */
    private String token;
}
