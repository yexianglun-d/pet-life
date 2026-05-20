package com.petlife.server.modules.notification.service.provider;

/**
 * Push 供应商抽象。
 */
public interface PushProvider {

    String providerCode();

    boolean dispatchEnabled();
}
