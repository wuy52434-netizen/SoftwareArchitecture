package com.library.book.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图书变更事件的交换机声明（KNWN-ES-01）。
 *
 * <p>修复前 book-service 完全没有 ES 同步：图书新增/修改/删除后索引不变，
 * 借书机（走 ES）搜不到刚上架的书，已删除的书又能在检索结果里被搜到（点进去 404）。
 *
 * <p>这里只声明**交换机**；队列与绑定由消费方 search-service 声明
 * （谁消费谁声明队列，避免生产方绑定到不存在的消费队列）。
 */
@Configuration
public class BookEventConfig {

    /** 图书变更主题交换机。路由键：book.upsert / book.delete */
    public static final String BOOK_EXCHANGE = "book.exchange";

    public static final String ROUTING_KEY_UPSERT = "book.upsert";
    public static final String ROUTING_KEY_DELETE = "book.delete";

    @Bean
    public TopicExchange bookExchange() {
        return new TopicExchange(BOOK_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter bookJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate bookRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(bookJsonMessageConverter());
        return template;
    }
}
