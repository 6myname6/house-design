package com.housedesign.common;

import lombok.Getter;

/**
 * 业务异常：携带业务错误码，由全局异常处理器转换为统一 Result。
 * 对应接口文档 §1.3 错误码约定，如 404 = 资源不存在。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码（如 400/404） */
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 默认 400 业务异常 */
    public BusinessException(String message) {
        this(400, message);
    }
}
