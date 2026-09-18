package com.library.search.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 覆盖默认 ES Client,为 JacksonJsonpMapper 显式注册 java.time (JSR-310) 模块,
 * 解决 `Failed to decode response` / LocalDate 字段反序列化失败问题。
 */
@Configuration
public class EsClientConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String esUris;

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient() {
        String base = esUris.replace("http://", "").replace("https://", "");
        String host = base.contains(":") ? base.split(":")[0] : base;
        int port = base.contains(":") ? Integer.parseInt(base.split(":")[1]) : 9200;

        RestClient restClient = RestClient.builder(new org.apache.http.HttpHost(host, port, "http")).build();

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        om.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        om.findAndRegisterModules();
        om.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // LocalDate 默认会被写成 [2026,9,18] 数组，ES 的 date 字段解析不了，报
        // "failed to parse field [createdAt] of type [date] ... Preview of field's value: '9'"。
        // 关闭时间戳式序列化，改成 ISO 字符串（2026-09-18），与 mapping 的 format 一致。
        om.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        ElasticsearchTransport transport = new RestClientTransport(restClient,
                new JacksonJsonpMapper(om));

        return new ElasticsearchClient(transport);
    }
}