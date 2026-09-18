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

    /**
     * 死信落地队列（KNWN-MQ-01 修复）。
     *
     * <p>{@link #borrowSuccessQueue()} 声明了
     * {@code x-dead-letter-exchange=""}（默认交换机）+ {@code x-dead-letter-routing-key="dead.letter.queue"}。
     * RabbitMQ 的默认交换机按「队列名 == routing key」自动路由，所以只要声明一个
     * **名字恰好等于该 routing key** 的队列，死信就能真正落地。
     *
     * <p>修复前全仓库从未声明过这个队列：被 nack(requeue=false) 或过期的消息转发到默认交换机后
     * 无队列承接，broker 直接丢弃 —— 配了死信交换机却等于没配。
     */
    public static final String DEAD_LETTER_QUEUE = "dead.letter.queue";

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
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_QUEUE)
                .build();
    }

    /**
     * 死信落地队列本体。
     *
     * <p>刻意<b>不</b>设置 TTL：死信的价值在于事后排查，自动过期就等于再次丢掉证据。
     * 代价是它只进不出，因此运维侧需要补一个监控/告警（例如对队列长度设告警阈值），
     * 或加一个把死信转人工工单的消费者。这一步属于后续运维约定，不在本次缺陷修复范围内。
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
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
