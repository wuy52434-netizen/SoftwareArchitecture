package com.library.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.result.Result;
import com.library.common.result.ResultCode;
import com.library.gateway.config.GatewayProperties;
import com.library.gateway.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtProperties jwtProperties;
    private final GatewayProperties gatewayProperties;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        log.debug("请求路径: {} {}", method, path);

        if ("OPTIONS".equalsIgnoreCase(method)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            if (isWhiteList(path)) {
                log.debug("白名单路径且未携带 Token，跳过认证: {}", path);
                return chain.filter(exchange);
            }
            log.warn("未找到 Token，路径: {}", path);
            return writeUnauthorizedResponse(exchange.getResponse(), "未登录或Token已过期");
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

            return reactiveRedisTemplate.opsForValue().get(tokenKey)
                    .defaultIfEmpty("")
                    .flatMap(storedToken -> {
                        ServerHttpRequest mutatedRequest = request.mutate()
                                .header("X-User-Id", String.valueOf(userId))
                                .header("X-User-Name", username)
                                .header("X-User-Role", role)
                                .build();

                        log.debug("Token 验证通过: userId={}, username={}, role={}", userId, username, role);
                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    });

        } catch (Exception e) {
            log.error("Token 验证失败: {}", e.getMessage());
            if (isWhiteList(path)) {
                log.debug("白名单路径携带无效 Token，按匿名请求放行: {}", path);
                return chain.filter(exchange);
            }
            return writeUnauthorizedResponse(exchange.getResponse(), "Token无效或已过期");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean isWhiteList(String path) {
        List<String> whiteList = gatewayProperties.getWhiteList();
        if (whiteList == null || whiteList.isEmpty()) {
            log.warn("白名单为空，对路径 {} 使用内置白名单判断", path);
            return path.startsWith("/api/auth/") || path.startsWith("/api/books")
                || path.startsWith("/api/search") || path.startsWith("/api/settings")
                || path.startsWith("/api/borrow") || path.startsWith("/api/return")
                || path.startsWith("/api/categories") || path.equals("/error");
        }
        for (String pattern : whiteList) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> writeUnauthorizedResponse(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<Void> result = Result.error(ResultCode.UNAUTHORIZED.getCode(), message);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":401,\"message\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
