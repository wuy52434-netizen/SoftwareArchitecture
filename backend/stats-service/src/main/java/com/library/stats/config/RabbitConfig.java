package com.library.stats.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String BORROW_EXCHANGE = "borrow.exchange";
    public static final String STATS_DAILY_QUEUE = "queue.stats.daily";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange borrowExchange() {
        return new TopicExchange(BORROW_EXCHANGE, true, false);
    }

    @Bean
    public Queue statsDailyQueue() {
        return QueueBuilder.durable(STATS_DAILY_QUEUE).build();
    }

    @Bean
    public Binding statsBorrowSuccessBinding() {
        return BindingBuilder.bind(statsDailyQueue()).to(borrowExchange()).with("borrow.success");
    }

    @Bean
    public Binding statsReturnSuccessBinding() {
        return BindingBuilder.bind(statsDailyQueue()).to(borrowExchange()).with("borrow.return");
    }
}
