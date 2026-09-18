package com.library.search.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 图书检索文档 —— 字段集合与分词器配置必须与
 * {@code src/main/resources/es/books-mapping.json} 完全一致。
 *
 * <p><b>关于中文分词器（KNWN-ES-02）</b>：本类原先声明
 * {@code analyzer = "ik_max_word" / searchAnalyzer = "ik_smart"}，但运行时容器里的
 * Elasticsearch 从未安装 IK 插件（本环境插件包下载被网络策略阻断），
 * 索引实际用的是 standard 分析器 —— 声明与事实不符，属"文档骗人"。
 * 现在统一改为 standard，并在 mapping 文件中注明启用 IK 的完整步骤。
 * 需要更准的中文分词时，请同时改这里与 mapping 文件，两者必须保持一致。
 */
@Data
@Document(indexName = "books")
public class BookDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String isbn;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String author;

    @Field(type = FieldType.Keyword)
    private String publisher;

    @Field(type = FieldType.Integer)
    private Integer publishDate;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String summary;

    @Field(type = FieldType.Float)
    private BigDecimal price;

    @Field(type = FieldType.Keyword, index = false)
    private String coverUrl;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Integer)
    private Integer availableCopies;

    @Field(type = FieldType.Integer)
    private Integer borrowCount;

    @Field(type = FieldType.Date)
    private LocalDate createdAt;

    @Field(type = FieldType.Date)
    private LocalDate updatedAt;
}
