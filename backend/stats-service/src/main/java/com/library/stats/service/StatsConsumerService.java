package com.library.stats.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsConsumerService {

    private final StatsService statsService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "queue.stats.daily")
    public void handleStatsMessage(Map<String, Object> message) {
        log.info("处理统计消息: {}", message);

        try {
            String eventType = (String) message.get("eventType");
            if (eventType == null) {
                return;
            }

            switch (eventType) {
                case "BORROW_SUCCESS":
                    Long bookId = message.get("bookId") != null ?
                            Long.parseLong(String.valueOf(message.get("bookId"))) : null;
                    String bookTitle = (String) message.get("bookTitle");
                    if (bookId != null && bookTitle != null) {
                        statsService.incrementBorrow(bookId, bookTitle);
                    }
                    break;
                case "RETURN_SUCCESS":
                    statsService.incrementReturn();
                    break;
                default:
                    log.debug("未知的统计事件类型: {}", eventType);
            }
        } catch (Exception e) {
            log.error("处理统计消息失败: {}", e.getMessage(), e);
        }
    }
}
