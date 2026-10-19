package com.erp.common.core.enums;

/**
 * 统一返回码。
 * <p>
 * 约定：成功固定为 200（前端 request.js 以此判断），业务失败用 4xx/5xx，
 * 与 HTTP 状态码语义保持一致，避免前端要记两套码。
 */
public enum ResultCode {

    /** 操作成功 */
    SUCCESS(200, "操作成功"),
    /** 参数校验失败 */
    BAD_REQUEST(400, "请求参数错误"),
    /** 未登录 / Token 失效 */
    UNAUTHORIZED(401, "未登录或登录已过期，请重新登录"),
    /** 已登录但无权限 */
    FORBIDDEN(403, "没有访问权限，请联系管理员"),
    /** 资源不存在 */
    NOT_FOUND(404, "资源不存在"),
    /**
     * 业务校验不通过（如编码重复、状态不允许、密码错误）。
     * <p>
     * 刻意用 <b>400</b> 而不是 500，与下面的 ERROR 区分开。
     * 早期版本两者都是 500，后果是排障时分不清"用户操作不合法"和"我们的代码出错了"——
     * 实测中就因此多绕了一圈（登录失败只看到一个 500，要去翻日志才知道是业务错误）。
     * 区分开之后：**body.code=500 就一定意味着程序有 bug**，值得立刻报警。
     */
    BUSINESS_ERROR(400, "操作失败"),
    /** 未捕获的系统异常。看到这个码说明代码有问题，不是用户的问题 */
    ERROR(500, "系统内部错误，请联系管理员");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
