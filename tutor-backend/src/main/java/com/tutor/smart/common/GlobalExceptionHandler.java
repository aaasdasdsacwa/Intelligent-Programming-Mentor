package com.tutor.smart.common.exception;

import com.tutor.smart.common.BaseResponse;
import com.tutor.smart.common.BusinessException;
import com.tutor.smart.common.ErrorCode;
import com.tutor.smart.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 (捕获 Controller 层抛出的异常)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获自定义的业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException发生: [code: {}] [message: {}]", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 捕获系统未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("系统运行异常 (RuntimeException): ", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统运行异常，请稍后再试");
    }
}