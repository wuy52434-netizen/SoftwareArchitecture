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
            // 兜底白名单与 application.yml 保持同一策略：读接口匿名、写接口鉴权。
            // 历史默认值把 "/api/settings" 整条放行（不分方法），是 KNWN-SEC-02 的第二个来源，
            // 一旦配置文件缺失就会悄悄退化回不安全状态，因此这里也必须按方法限定。
            whiteList = Arrays.asList(
                "POST /api/auth/login",
                "POST /api/auth/register",
                "POST /api/auth/refresh",
                "GET /api/books",
                "GET /api/books/**",
                "GET /api/categories/**",
                "GET /api/search",
                "GET /api/search/**",
                "GET /api/settings",
                "GET /api/settings/**",
                "GET /api/borrow-records/**",
                "GET /api/users/card/**",
                "GET /error",
                "POST /api/borrow",
                "POST /api/borrow/**",
                "POST /api/return",
                "POST /api/return/**"
            );
            log.warn("白名单从配置文件加载为空，使用默认白名单（读匿名/写鉴权）");
        }
        log.info("Gateway 白名单配置: {}", whiteList);
    }
}
