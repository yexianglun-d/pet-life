package com.petlife.server.common.exception;

import com.petlife.server.common.response.ResponseCode;

/**
 * 业务异常定义。
 *
 * <p>服务端在识别到预期内的业务失败时统一抛出该异常，
 * 由全局异常处理器转换为标准响应，避免控制器层散落重复判断。</p>
 */
public class BusinessException extends RuntimeException {

    private final ResponseCode responseCode;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }

    public BusinessException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }
}
