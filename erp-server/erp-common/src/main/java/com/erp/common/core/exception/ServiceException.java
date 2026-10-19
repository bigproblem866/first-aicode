package com.erp.common.core.exception;

import com.erp.common.core.enums.ResultCode;

/**
 * 业务异常。凡是「能预期、需要给用户看一句人话」的失败都抛这个，例如：
 * <pre>
 *   if (materialMapper.exists(code)) {
 *       throw new ServiceException("物料编码已存在：" + code);
 *   }
 * </pre>
 * 由 {@code com.erp.framework.web.GlobalExceptionHandler} 统一转成
 * {@code {"code":500,"msg":"物料编码已存在：M001"}}，不会打印整页堆栈污染日志。
 * <p>
 * 注意：它是 RuntimeException，抛出后会触发 @Transactional 回滚。
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public ServiceException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public ServiceException(String message, int code) {
        super(message);
        this.code = code;
    }

    public ServiceException(ResultCode resultCode) {
        super(resultCode.getMsg());
        this.code = resultCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
