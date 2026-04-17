package com.petlife.server.modules.auth.security;

import com.petlife.server.common.exception.BusinessException;
import com.petlife.server.common.response.ResponseCode;

/**
 * 当前请求用户上下文。
 *
 * <p>当前服务端采用无状态 Bearer Token 鉴权，过滤器完成 token 校验后把用户 ID 放入
 * ThreadLocal。业务服务只读取用户 ID，不直接解析 token，避免鉴权细节扩散到各业务模块。</p>
 */
public final class CurrentUserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long requireUserId() {
        Long userId = CURRENT_USER_ID.get();
        if (userId == null) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED, "请先登录后再继续操作");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
