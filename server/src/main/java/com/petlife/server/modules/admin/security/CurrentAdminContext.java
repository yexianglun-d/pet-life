package com.petlife.server.modules.admin.security;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;
import java.util.Optional;

/**
 * 当前请求后台管理员上下文。
 *
 * <p>后台写治理必须记录真实操作者。这里用请求线程保存鉴权结果，
 * 让业务服务和审计服务不直接依赖 Servlet 或 Spring Security 细节。</p>
 */
public final class CurrentAdminContext {

    private static final ThreadLocal<AuthenticatedAdmin> CURRENT_ADMIN = new ThreadLocal<>();

    private CurrentAdminContext() {
    }

    public static void set(AuthenticatedAdmin authenticatedAdmin) {
        CURRENT_ADMIN.set(authenticatedAdmin);
    }

    public static Optional<AuthenticatedAdmin> current() {
        return Optional.ofNullable(CURRENT_ADMIN.get());
    }

    public static AuthenticatedAdmin require() {
        return current().orElseThrow(() -> new BusinessException(ResponseCode.UNAUTHORIZED, "请先登录后台"));
    }

    public static void clear() {
        CURRENT_ADMIN.remove();
    }
}
