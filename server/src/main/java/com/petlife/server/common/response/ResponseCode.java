package com.petlife.server.common.response;

/**
 * 统一响应码定义。
 *
 * <p>当前仅保留基础码值，后续进入业务开发时按模块继续扩展，
 * 避免早期把错误码设计成无法维护的平铺常量集合。</p>
 */
public enum ResponseCode {

    OK("OK", "success"),
    BAD_REQUEST("BAD_REQUEST", "bad request"),
    UNAUTHORIZED("UNAUTHORIZED", "unauthorized"),
    FORBIDDEN("FORBIDDEN", "forbidden"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "resource not found"),
    AUTH_SMS_CODE_INVALID("AUTH_SMS_CODE_INVALID", "sms verification code is invalid"),
    USER_CURRENT_PET_NOT_FOUND("USER_CURRENT_PET_NOT_FOUND", "current pet does not exist"),
    PET_NOT_FOUND("PET_NOT_FOUND", "pet does not exist"),
    REMINDER_NOT_FOUND("REMINDER_NOT_FOUND", "reminder does not exist"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "internal server error");

    private final String code;
    private final String message;

    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
