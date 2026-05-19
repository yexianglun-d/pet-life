package com.petlife.server.modules.auth.service.sms;

/**
 * 短信供应商抽象。
 */
public interface SmsProvider {

    String providerCode();

    SmsSendResult sendVerificationCode(String mobile, String scene, String code);
}
