package com.erp.common.core.domain;

import com.erp.common.core.enums.ResultCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一返回包装。
 * <p>
 * 所有 Controller 一律返回 {@code R<T>}，前端 request.js 只认 {@code code == 200}。
 *
 * <pre>
 *   { "code": 200, "msg": "操作成功", "data": {...} }
 * </pre>
 */
@Data
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 状态码，200 为成功 */
    private int code;
    /** 提示信息，失败时前端直接弹这个 */
    private String msg;
    /** 业务数据 */
    private T data;

    public R() {
    }

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // ---------------- 成功 ----------------

    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), msg, data);
    }

    // ---------------- 失败 ----------------

    public static <T> R<T> fail(String msg) {
        return new R<>(ResultCode.BUSINESS_ERROR.getCode(), msg, null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMsg(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode, String msg) {
        return new R<>(resultCode.getCode(), msg, null);
    }

    /**
     * 是否成功，供<b>后端业务代码</b>判断（如 if (result.isSuccess())）。
     * <p>
     * {@code @JsonIgnore} 是必须的：Lombok 生成的这个 getter 会被 Jackson 序列化成
     * 一个额外的 {@code "success":true} 字段，而接口契约里只有 code/msg/data。
     * 多出来的字段虽然无害，但会让"契约"和"实际返回"对不上，
     * 前端照着文档写解析逻辑时会以为漏了什么。
     */
    @JsonIgnore
    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}
