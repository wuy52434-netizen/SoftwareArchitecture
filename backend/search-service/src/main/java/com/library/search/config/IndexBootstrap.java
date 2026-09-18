package com.library.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 启动时用统一 mapping 建索引（KNWN-ES-02 / KNWN-ES-03）。
 *
 * <p>为什么必须由服务来建：此前索引只由手工脚本 {@code seed_es_books.py} 创建，
 * 于是"代码里声明的字段与分词器"和"线上索引实际有什么"就成了两份会各自漂移的定义 ——
 * 实测漂移结果就是索引缺 {@code updatedAt}、分词器与实体声明不一致。
 *
 * <p>现在 mapping 收敛到唯一事实来源
 * {@code src/main/resources/es/books-mapping.json}，服务与脚本都读它。
 * 已存在则不动（不覆盖线上数据），只在缺失时创建。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexBootstrap implements ApplicationRunner {

    private static final String INDEX_NAME = "books";
    private static final String MAPPING_RESOURCE = "es/books-mapping.json";

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean exists = elasticsearchClient.indices()
                    .exists(e -> e.index(INDEX_NAME))
                    .value();
            if (exists) {
                log.info("索引 {} 已存在，跳过创建（不覆盖线上数据）", INDEX_NAME);
                return;
            }

            try (InputStream mapping = new ClassPathResource(MAPPING_RESOURCE).getInputStream()) {
                elasticsearchClient.indices().create(c -> c.index(INDEX_NAME).withJson(mapping));
                log.info("索引 {} 已按 {} 创建完成（字段与分词器以该文件为唯一事实来源）",
                        INDEX_NAME, MAPPING_RESOURCE);
            }
        } catch (Exception e) {
            // 不让建索引失败阻断服务启动：检索会退化为查不到，但接口仍需可用以便暴露问题
            log.error("索引 {} 初始化失败（检索可能不可用）: {}", INDEX_NAME, e.getMessage());
        }
    }
}
