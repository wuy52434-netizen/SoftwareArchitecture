package com.library.book.service;

import com.library.book.config.BookEventConfig;
import com.library.book.event.BookChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * 把图书变更事件投递到 RabbitMQ，供 search-service 同步 ES 索引（KNWN-ES-01）。
 *
 * <p>刻意用 {@link TransactionalEventListener} 在**事务提交后**才发送：
 * 若直接在 {@code @Transactional} 方法体里发送，事务一旦回滚，索引里就会留下
 * 一条数据库根本不存在的“幽灵图书”。{@code fallbackExecution = true} 让
 * 没有事务上下文时（例如单元测试直接调 service）也能正常发送。
 *
 * <p>发送失败只记日志、不抛异常：图书写入是主流程，不应因为 MQ 抖动而回滚；
 * 代价是可能漏同步，需要靠定期全量对账兜底（见 search-service 的说明）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookChanged(BookChangedEvent event) {
        String routingKey = event.operation() == BookChangedEvent.Operation.DELETE
                ? BookEventConfig.ROUTING_KEY_DELETE
                : BookEventConfig.ROUTING_KEY_UPSERT;

        Map<String, Object> payload = new HashMap<>();
        payload.put("bookId", event.bookId());
        payload.put("operation", event.operation().name());
        payload.put("timestamp", System.currentTimeMillis());

        try {
            rabbitTemplate.convertAndSend(BookEventConfig.BOOK_EXCHANGE, routingKey, payload);
            log.info("图书变更事件已发送: bookId={}, op={}, routingKey={}",
                    event.bookId(), event.operation(), routingKey);
        } catch (Exception e) {
            log.warn("图书变更事件发送失败（不影响图书写入，索引可能滞后）: bookId={}, op={}, err={}",
                    event.bookId(), event.operation(), e.getMessage());
        }
    }
}
