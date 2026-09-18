package com.library.search.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 图书变更同步队列声明（KNWN-ES-01）。
 *
 * <p>谁消费谁声明队列：book-service 只声明交换机，队列与绑定在这里声明，
 * 这样交换机不会挂着一个"没人消费"的队列名。
 *
 * <p>同时给本队列配上死信参数并声明落地队列 —— 沿用 KNWN-MQ-01 的教训：
 * 声明了 DLX 就必须同时保证目标队列存在，否则消息会被默认交换机静默丢弃。
 */
@Configuration
public class BookSyncConfig {

    public static final String BOOK_EXCHANGE = "book.exchange";
    public static final String ROUTING_KEY_UPSERT = "book.upsert";
    public static final String ROUTING_KEY_DELETE = "book.delete";

    public static final String BOOK_SYNC_QUEUE = "queue.book.sync";
    public static final String BOOK_SYNC_DLQ = "queue.book.sync.dlq";

    @Bean
    public TopicExchange bookExchange() {
        return new TopicExchange(BOOK_EXCHANGE, true, false);
    }

    @Bean
    public Queue bookSyncQueue() {
        // 死信指向同名的 DLQ（默认交换机按"队列名 == routing key"路由）
        return QueueBuilder.durable(BOOK_SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", BOOK_SYNC_DLQ)
                .build();
    }

    /** 死信落地队列本体：没有它，上面那行 DLX 配置等于没配。 */
    @Bean
    public Queue bookSyncDeadLetterQueue() {
        return QueueBuilder.durable(BOOK_SYNC_DLQ).build();
    }

    @Bean
    public Binding bookSyncUpsertBinding() {
        return BindingBuilder.bind(bookSyncQueue()).to(bookExchange()).with(ROUTING_KEY_UPSERT);
    }

    @Bean
    public Binding bookSyncDeleteBinding() {
        return BindingBuilder.bind(bookSyncQueue()).to(bookExchange()).with(ROUTING_KEY_DELETE);
    }

    /**
     * 事件是 JSON 报文，必须有 JSON 转换器，否则 {@code @RabbitListener} 拿到的是 byte[]
     * 而不是 Map。不注册这个 Bean 时表现为消费端反序列化失败、消息被反复重投。
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
