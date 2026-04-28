package com.library.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.result.Result;
import com.library.common.result.ResultCode;
import com.library.gateway.config.GatewayProperties;
import com.library.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements Filter {

    private final JwtProperties jwtProperties;
    private final GatewayProperties gatewayProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        log.debug("请求路径: {} {}", method, path);

        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        if (isWhiteList(path)) {
            log.debug("白名单路径，跳过认证: {}", path);
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(httpRequest);
        if (!StringUtils.hasText(token)) {
            log.warn("未找到 Token，路径: {}", path);
            writeUnauthorizedResponse(httpResponse, "未登录或Token已过期");
            return;
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(
                    jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)
            );

            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            String tokenKey = "user:token:" + userId;
            Object storedToken = redisTemplate.opsForValue().get(tokenKey);
            if (storedToken == null || !token.equals(storedToken)) {
                log.warn("Token 已失效 (可能已登出): userId={}", userId);
                writeUnauthorizedResponse(httpResponse, "Token已失效，请重新登录");
                return;
            }

            httpRequest.setAttribute("userId", userId);
            httpRequest.setAttribute("username", username);
            httpRequest.setAttribute("role", role);

            log.debug("Token 验证通过: userId={}, username={}, role={}", userId, username, role);
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            writeUnauthorizedResponse(httpResponse, "Token无效或已过期");
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean isWhiteList(String path) {
        for (String pattern : gatewayProperties.getWhiteList()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<Void> result = Result.error(ResultCode.UNAUTHORIZED.getCode(), message);
        String json = objectMapper.writeValueAsString(result);
        response.getWriter().write(json);
    }
}
