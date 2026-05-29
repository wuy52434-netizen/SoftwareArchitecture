package com.library.notify.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String BORROW_EXCHANGE = "borrow.exchange";
    public static final String NOTIFY_EXCHANGE = "notify.exchange";
    public static final String STATS_EXCHANGE = "stats.exchange";

    public static final String BORROW_SUCCESS_QUEUE = "queue.borrow.success";
    public static final String BORROW_FAIL_QUEUE = "queue.borrow.fail";
    public static final String RETURN_SUCCESS_QUEUE = "queue.return.success";
    public static final String NOTIFY_SMS_QUEUE = "queue.notify.sms";
    public static final String NOTIFY_EMAIL_QUEUE = "queue.notify.email";
    public static final String NOTIFY_INNER_QUEUE = "queue.notify.inner";
    public static final String STATS_DAILY_QUEUE = "queue.stats.daily";

    public static final String ROUTING_KEY_BORROW_SUCCESS = "borrow.success";
    public static final String ROUTING_KEY_BORROW_FAIL = "borrow.fail";
    public static final String ROUTING_KEY_RETURN_SUCCESS = "borrow.return";
    public static final String ROUTING_KEY_NOTIFY_SMS = "notify.sms";
    public static final String ROUTING_KEY_NOTIFY_EMAIL = "notify.email";
    public static final String ROUTING_KEY_NOTIFY_INNER = "notify.inner";
    public static final String ROUTING_KEY_STATS_DAILY = "stats.daily";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public TopicExchange borrowExchange() {
        return new TopicExchange(BORROW_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange notifyExchange() {
        return new DirectExchange(NOTIFY_EXCHANGE, true, false);
    }

    @Bean
    public FanoutExchange statsExchange() {
        return new FanoutExchange(STATS_EXCHANGE, true, false);
    }

    @Bean
    public Queue borrowSuccessQueue() {
        return QueueBuilder.durable(BORROW_SUCCESS_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "dead.letter.queue")
                .build();
    }

    @Bean
    public Queue borrowFailQueue() {
        return QueueBuilder.durable(BORROW_FAIL_QUEUE).build();
    }

    @Bean
    public Queue returnSuccessQueue() {
        return QueueBuilder.durable(RETURN_SUCCESS_QUEUE).build();
    }

    @Bean
    public Queue notifySmsQueue() {
        return QueueBuilder.durable(NOTIFY_SMS_QUEUE).build();
    }

    @Bean
    public Queue notifyEmailQueue() {
        return QueueBuilder.durable(NOTIFY_EMAIL_QUEUE).build();
    }

    @Bean
    public Queue notifyInnerQueue() {
        return QueueBuilder.durable(NOTIFY_INNER_QUEUE).build();
    }

    @Bean
    public Queue statsDailyQueue() {
        return QueueBuilder.durable(STATS_DAILY_QUEUE).build();
    }

    @Bean
    public Binding borrowSuccessBinding() {
        return BindingBuilder
                .bind(borrowSuccessQueue())
                .to(borrowExchange())
                .with(ROUTING_KEY_BORROW_SUCCESS);
    }

    @Bean
    public Binding borrowFailBinding() {
        return BindingBuilder
                .bind(borrowFailQueue())
                .to(borrowExchange())
                .with(ROUTING_KEY_BORROW_FAIL);
    }

    @Bean
    public Binding returnSuccessBinding() {
        return BindingBuilder
                .bind(returnSuccessQueue())
                .to(borrowExchange())
                .with(ROUTING_KEY_RETURN_SUCCESS);
    }

    @Bean
    public Binding notifySmsBinding() {
        return BindingBuilder
                .bind(notifySmsQueue())
                .to(notifyExchange())
                .with(ROUTING_KEY_NOTIFY_SMS);
    }

    @Bean
    public Binding notifyEmailBinding() {
        return BindingBuilder
                .bind(notifyEmailQueue())
                .to(notifyExchange())
                .with(ROUTING_KEY_NOTIFY_EMAIL);
    }

    @Bean
    public Binding notifyInnerBinding() {
        return BindingBuilder
                .bind(notifyInnerQueue())
                .to(notifyExchange())
                .with(ROUTING_KEY_NOTIFY_INNER);
    }

    @Bean
    public Binding statsDailyBinding() {
        return BindingBuilder
                .bind(statsDailyQueue())
                .to(statsExchange());
    }

    @Bean
    public Binding borrowSuccessToNotifySms() {
        return BindingBuilder
                .bind(notifySmsQueue())
                .to(borrowExchange())
                .with(ROUTING_KEY_BORROW_SUCCESS);
    }

    @Bean
    public Binding borrowSuccessToNotifyEmail() {
        return BindingBuilder
                .bind(notifyEmailQueue())
                .to(borrowExchange())
                .with(ROUTING_KEY_BORROW_SUCCESS);
    }

    @Bean
    public Binding borrowSuccessToStats() {
        return BindingBuilder
                .bind(statsDailyQueue())
                .to(borrowExchange())
                .with(ROUTING_KEY_BORROW_SUCCESS);
    }

    @Bean
    public Binding returnSuccessToNotifySms() {
        return BindingBuilder
                .bind(notifySmsQueue())
                .to(borrowExchange())
                .with(ROUTING_KEY_RETURN_SUCCESS);
    }

    @Bean
    public Binding returnSuccessToStats() {
        return BindingBuilder
                .bind(statsDailyQueue())
                .to(borrowExchange())
                .with(ROUTING_KEY_RETURN_SUCCESS);
    }
}
