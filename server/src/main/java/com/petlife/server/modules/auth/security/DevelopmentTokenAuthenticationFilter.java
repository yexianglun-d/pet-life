package com.petlife.server.modules.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petlife.server.common.response.ApiResponse;
import com.petlife.server.common.response.ResponseCode;
import com.petlife.server.modules.admin.security.AuthenticatedAdmin;
import com.petlife.server.modules.admin.security.CurrentAdminContext;
import com.petlife.server.modules.admin.token.AdminAccessTokenRepository;
import com.petlife.server.modules.auth.token.AccessTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 开发期 Bearer Token 鉴权过滤器。
 *
 * <p>当前阶段还没有接入正式 JWT 与用户权限模型，但移动端已经具备登录态与本地 token 存储。
 * 该过滤器用于先把「登录后访问受保护接口」的后端边界收紧，避免主链路长期处于所有接口裸奔状态。
 * 后续接入正式认证时，可以保留请求放行策略并替换 token 校验实现。</p>
 */
@Component
public class DevelopmentTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenRepository accessTokenRepository;
    private final AdminAccessTokenRepository adminAccessTokenRepository;
    private final ObjectMapper objectMapper;

    public DevelopmentTokenAuthenticationFilter(
        AccessTokenRepository accessTokenRepository,
        AdminAccessTokenRepository adminAccessTokenRepository,
        ObjectMapper objectMapper
    ) {
        this.accessTokenRepository = accessTokenRepository;
        this.adminAccessTokenRepository = adminAccessTokenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
            || requestPath.startsWith("/api/v1/auth/")
            || requestPath.startsWith("/api/v1/admin/auth/")
            || requestPath.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = resolveAccessToken(request);
        if (request.getRequestURI().startsWith("/api/v1/admin/")) {
            Optional<AuthenticatedAdmin> authenticatedAdmin =
                adminAccessTokenRepository.findAdminByAccessToken(accessToken);
            if (authenticatedAdmin.isEmpty()) {
                writeUnauthorizedResponse(response);
                return;
            }

            try {
                CurrentAdminContext.set(authenticatedAdmin.get());
                filterChain.doFilter(request, response);
            } finally {
                CurrentAdminContext.clear();
            }
            return;
        }

        Optional<Long> userId = accessTokenRepository.findUserIdByAccessToken(accessToken);
        if (userId.isEmpty()) {
            writeUnauthorizedResponse(response);
            return;
        }

        try {
            CurrentUserContext.setUserId(userId.get());
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
        }
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
            response.getWriter(),
            ApiResponse.failure(ResponseCode.UNAUTHORIZED, "请先登录后再继续操作")
        );
    }
}
