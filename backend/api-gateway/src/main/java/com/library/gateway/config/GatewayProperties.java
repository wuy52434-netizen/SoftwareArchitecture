package com.library.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private List<RouteProperties> routes = new ArrayList<>();
    private List<String> whiteList = new ArrayList<>();

    @Data
    public static class RouteProperties {
        private String id;
        private String uri;
        private String path;
    }
}
