package com.library.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Data
@Component("customGatewayWhiteListProperties")
@ConfigurationProperties(prefix = "auth-filter")
public class GatewayProperties {

    private List<String> whiteList = new ArrayList<>();

    @PostConstruct
    public void init() {
        if (whiteList == null || whiteList.isEmpty()) {
            whiteList = Arrays.asList(
                "/api/auth/login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/api/books",
                "/api/books/**",
                "/api/search",
                "/api/search/**",
                "/api/settings",
                "/api/settings/**",
                "/api/borrows/**",
                "/api/borrow/**",
                "/api/return/**",
                "/api/borrow-records/**",
                "/api/categories/**",
                "/error"
            );
            log.warn("白名单从配置文件加载为空，使用默认白名单");
        }
        log.info("Gateway 白名单配置: {}", whiteList);
    }
}
