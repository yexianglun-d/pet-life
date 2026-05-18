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
    AUTH_REFRESH_TOKEN_INVALID("AUTH_REFRESH_TOKEN_INVALID", "refresh token is invalid"),
    USER_CURRENT_PET_NOT_FOUND("USER_CURRENT_PET_NOT_FOUND", "current pet does not exist"),
    FAMILY_NOT_FOUND("FAMILY_NOT_FOUND", "family does not exist"),
    FAMILY_MEMBER_NOT_FOUND("FAMILY_MEMBER_NOT_FOUND", "family member does not exist"),
    FAMILY_INVITATION_NOT_FOUND("FAMILY_INVITATION_NOT_FOUND", "family invitation does not exist"),
    FAMILY_ROLE_FORBIDDEN("FAMILY_ROLE_FORBIDDEN", "family role is forbidden"),
    PET_NOT_FOUND("PET_NOT_FOUND", "pet does not exist"),
    PET_PERMISSION_DENIED("PET_PERMISSION_DENIED", "no permission for current pet"),
    HEALTH_RECORD_NOT_FOUND("HEALTH_RECORD_NOT_FOUND", "health record does not exist"),
    REMINDER_NOT_FOUND("REMINDER_NOT_FOUND", "reminder does not exist"),
    DAILY_LOG_NOT_FOUND("DAILY_LOG_NOT_FOUND", "daily log does not exist"),
    COMMUNITY_POST_NOT_FOUND("COMMUNITY_POST_NOT_FOUND", "community post does not exist"),
    MODERATION_REPORT_NOT_FOUND("MODERATION_REPORT_NOT_FOUND", "moderation report does not exist"),
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "notification does not exist"),
    MEDIA_ASSET_NOT_FOUND("MEDIA_ASSET_NOT_FOUND", "media asset does not exist"),
    SERVICE_PROVIDER_NOT_FOUND("SERVICE_PROVIDER_NOT_FOUND", "service provider does not exist"),
    SERVICE_APPOINTMENT_NOT_FOUND("SERVICE_APPOINTMENT_NOT_FOUND", "service appointment does not exist"),
    SERVICE_REVIEW_NOT_FOUND("SERVICE_REVIEW_NOT_FOUND", "service review does not exist"),
    APPOINTMENT_SLOT_INVALID("APPOINTMENT_SLOT_INVALID", "appointment slot is invalid"),
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
