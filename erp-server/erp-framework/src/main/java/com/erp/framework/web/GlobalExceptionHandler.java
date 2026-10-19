package com.erp.framework.web;

import com.erp.common.core.domain.R;
import com.erp.common.core.enums.ResultCode;
import com.erp.common.core.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * 全局异常处理。所有异常最终都变成统一的 {@code {"code":..,"msg":".."}}。
 * <p>
 * <b>HTTP 状态码约定</b>（前端据此写拦截器）：
 * <ul>
 *   <li>业务异常、参数校验失败 → <b>HTTP 200</b> + body.code=400/500。
 *       理由：HTTP 层面这次请求是成功送达并处理了的，失败发生在业务语义层，
 *       前端统一按 {@code res.code !== 200} 弹 msg 即可（这也是原设计文档的约定）。</li>
 *   <li>401 / 403 → <b>真实 HTTP 状态码</b>。这两者可恢复性不同：
 *       401 要清 Token 跳登录页，403 只提示不能踢人，必须能让前端区分，
 *       所以在过滤器层（AuthenticationEntryPoint / AccessDeniedHandler）就写掉了。</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：能预期、消息可以直接给用户看 */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("业务异常 [{} {}]：{}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 权限不足。
     * <p>
     * <b>必须单独声明</b>：@PreAuthorize 抛出的 AuthorizationDeniedException 继承自
     * AccessDeniedException，而它是在 Controller 调用过程中抛出的，会被 @RestControllerAdvice
     * 先接住。如果不写这个 handler，它会被下面的 Exception 兜底处理成 500，
     * 前端把"没权限"当成"系统错误"，问题就难查了。
     * <p>
     * 返回类型用 {@code ResponseEntity} 而不是 {@code R}：必须把 HTTP 状态码也设成 403，
     * 与 SecurityConfig 里 AccessDeniedHandlerImpl 的行为保持一致。
     * <p>
     * 为什么这一点重要：接口契约规定
     * <ul>
     *   <li>401 → 前端清 Token 跳登录页</li>
     *   <li><b>403 → 前端只弹提示，绝不能把人踢下线</b></li>
     * </ul>
     * 如果这里返回 HTTP 200，前端看到的就只是"一次普通的业务失败"，
     * 无法与真正的业务错误区分。实测中发现过这个不一致：@PreAuthorize 拦截
     * 返回的是 HTTP 200 + body.code=403，而契约写的是 HTTP 403。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("权限不足 [{} {}]：{}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail(ResultCode.FORBIDDEN));
    }

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return R.fail(ResultCode.BAD_REQUEST, msg.isBlank() ? ResultCode.BAD_REQUEST.getMsg() : msg);
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return R.fail(ResultCode.BAD_REQUEST, msg.isBlank() ? ResultCode.BAD_REQUEST.getMsg() : msg);
    }

    /** 方法参数上的 @Validated 校验失败（如 @PathVariable @Min(1)） */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        return R.fail(ResultCode.BAD_REQUEST, msg.isBlank() ? ResultCode.BAD_REQUEST.getMsg() : msg);
    }

    /** 必填请求参数缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(ResultCode.BAD_REQUEST, "缺少必填参数：" + e.getParameterName());
    }

    /** 参数类型不对，比如把 "abc" 传到 Long id 上 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail(ResultCode.BAD_REQUEST, "参数类型错误：" + e.getName());
    }

    /** 请求体 JSON 格式错误 / 字段类型对不上 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return R.fail(ResultCode.BAD_REQUEST, "请求体格式错误，请检查 JSON 与字段类型");
    }

    /** 请求方法不支持，比如用 GET 调了新增接口 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return R.fail(ResultCode.BAD_REQUEST, "不支持的请求方法：" + e.getMethod());
    }

    /**
     * 唯一索引冲突。
     * <p>
     * 把数据库的 "Duplicate entry 'M001' for key 'uk_mdm_material_code'" 翻译成人话。
     * 有这一层，业务代码里就不必每处都先查一次"编码是否存在"（虽然并发下仍需唯一索引兜底）。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一约束冲突：{}", e.getMessage());
        return R.fail("数据已存在，请检查编码/名称是否重复");
    }

    /** 兜底。走到这里说明是没预料到的 bug，必须打完整堆栈 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 [{} {}]", request.getMethod(), request.getRequestURI(), e);
        return R.fail(ResultCode.ERROR);
    }
}
