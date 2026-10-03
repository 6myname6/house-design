package com.housedesign.common;

import java.util.stream.Collectors;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器：把未捕获的异常统一转换为 Result 响应，
 * 避免直接暴露 500 错误页 / 堆栈给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：按携带的 code 返回（如 400/404） */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** 参数非法（如校验失败、非法 code）：统一 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数非法：{}", e.getMessage());
        return Result.error(400, e.getMessage());
    }

    /** @RequestBody @Valid 校验失败：返回具体字段的错误原因 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = formatFieldErrors(e.getBindingResult());
        log.warn("请求体参数校验失败：{}", message);
        return Result.error(400, message);
    }

    /** 表单方式参数绑定/校验失败（非 @RequestBody）：字段错误拼法同上 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = formatFieldErrors(e.getBindingResult());
        log.warn("表单参数校验失败：{}", message);
        return Result.error(400, message);
    }

    /** 缺少必填的 @RequestParam 参数 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必要参数：{}", e.getParameterName());
        return Result.error(400, "缺少必要参数：" + e.getParameterName());
    }

    /** 请求体 JSON 格式错误或为空，无法反序列化 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败：{}", e.getMessage());
        return Result.error(400, "请求数据格式错误，请检查输入内容");
    }

    /** 上传文件超过大小限制（application.yml 配置单文件 20MB） */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制：{}", e.getMessage());
        return Result.error(400, "图片大小不能超过 20MB");
    }

    /** @Validated 方法级参数校验失败（散装 @RequestParam/@PathVariable 上的约束注解） */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("方法参数校验失败：{}", message);
        return Result.error(400, message.isBlank() ? "参数校验失败" : message);
    }

    /** 兜底：未知异常记日志，返回 500，不把内部细节暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "服务器内部错误，请稍后重试");
    }

    /**
     * 把校验结果中的字段错误拼成 "字段：原因；字段：原因"。
     * 无明细时返回兜底文案，保证前端一定能收到可读提示。
     */
    private String formatFieldErrors(BindingResult bindingResult) {
        if (bindingResult == null || !bindingResult.hasErrors()) {
            return "参数校验失败";
        }
        return bindingResult.getFieldErrors().stream()
                .map(this::formatOneFieldError)
                .collect(Collectors.joining("；"));
    }

    /** 单条字段错误：无 message 时只给字段名，避免出现 "字段：null" */
    private String formatOneFieldError(FieldError fieldError) {
        String reason = fieldError.getDefaultMessage();
        return fieldError.getField() + "：" + (reason == null ? "输入不合法" : reason);
    }
}
