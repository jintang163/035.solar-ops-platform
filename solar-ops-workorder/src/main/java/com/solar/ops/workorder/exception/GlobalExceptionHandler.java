package com.solar.ops.workorder.exception;

import com.solar.ops.common.exception.BusinessException;
import com.solar.ops.common.result.Result;
import com.solar.ops.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("参数校验异常：{}", e.getMessage());
        BindingResult bindingResult = e.getBindingResult();
        String message = bindingResult.getFieldError() != null
                ? bindingResult.getFieldError().getDefaultMessage()
                : ResultCode.PARAM_ERROR.getMessage();
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        log.warn("参数绑定异常：{}", e.getMessage());
        BindingResult bindingResult = e.getBindingResult();
        String message = bindingResult.getFieldError() != null
                ? bindingResult.getFieldError().getDefaultMessage()
                : ResultCode.PARAM_ERROR.getMessage();
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return Result.fail(ResultCode.FAIL.getCode(), "系统异常，请联系管理员");
    }
}
