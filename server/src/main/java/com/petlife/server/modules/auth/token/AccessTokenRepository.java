package com.petlife.server.modules.auth.token;

import java.util.Optional;

/**
 * 访问令牌存储接口。
 *
 * <p>认证模块只依赖该接口完成 token 签发与校验，不直接感知底层是开发期内存还是数据库。
 * 这样可以按 Profile 平滑切换实现，避免业务服务被具体存储方案污染。</p>
 */
public interface AccessTokenRepository {

    /**
     * 为指定用户签发登录令牌。
     *
     * @param userId 用户 ID
     * @return 登录令牌对，仅原始令牌返回给客户端，数据库只保存摘要
     */
    IssuedLoginTokens issueLoginTokens(Long userId);

    /**
     * 根据原始访问令牌查找有效用户 ID。
     *
     * @param accessToken 原始访问令牌
     * @return 有效用户 ID
     */
    Optional<Long> findUserIdByAccessToken(String accessToken);

    /**
     * 吊销访问令牌。
     *
     * @param accessToken 原始访问令牌
     */
    void revokeAccessToken(String accessToken);
}
