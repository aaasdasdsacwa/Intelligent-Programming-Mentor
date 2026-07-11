package com.tutor.smart.common;

/**
 * 返回工具类
 */
public class ResultUtils {

    /**
     * 成功返回
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), data, "ok");
    }

    /**
     * 成功返回（带提示信息）
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), data, message);
    }

    /**
     * 错误返回（基于内置ErrorCode）
     */
    public static BaseResponse<?> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 错误返回（自定义错误码和错误信息）
     */
    public static BaseResponse<?> error(int code, String message) {
        return new BaseResponse<>(code, null, message);
    }

    /**
     * 错误返回（基于内置ErrorCode，并自定义详细提示）
     */
    public static BaseResponse<?> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), null, message);
    }
}