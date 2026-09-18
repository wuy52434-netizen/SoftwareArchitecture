package com.library.notify.service;

import com.library.notify.config.RabbitMQConfig;
import com.library.notify.dto.BorrowEvent;
import com.library.notify.dto.NotifyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyConsumerService {

    private final NotifySenderService notifySenderService;
    private final StringRedisTemplate redisTemplate;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    /**
     * 去重键的保留时长。取出 1 天的理由：RabbitMQ 的重复投递集中在秒级到分钟级的重连窗口，
     * 1 天足够覆盖；同时避免 Redis 键无限增长。覆盖不到的超长延迟重复投递属于极端情况，
     * 需要业务侧对"通知"本身做可撤销设计，不在本缺陷范围内。
     */
    private static final Duration DEDUP_TTL = Duration.ofDays(1);

    private static final String DEDUP_KEY_PREFIX = "notify:dedup:";

    /**
     * 幂等闸门（KNWN-MQ-03 修复）。
     *
     * <p>RabbitMQ 是 at-least-once：消费者 ACK 丢失、连接抖动重连、生产端重试，
     * 都会让同一条业务事件被投递多次。broker 不会去重，必须消费端自己保证。
     *
     * <p>判据优先级：
     * <ol>
     *   <li>报文里的 {@code idempotentKey}（生产端由 "事件类型:业务主键" 确定性派生）；</li>
     *   <li>退化到 {@code eventType + ":" + recordId}，兼容尚未升级的旧报文；</li>
     *   <li>两者都取不到时**不去重**并告警 —— 宁可重复通知，也不静默丢弃用户通知。</li>
     * </ol>
     *
     * @return true 表示本次是首次处理，应继续；false 表示重复投递，应跳过
     */
    private boolean claimIdempotencySlot(Map<String, Object> message) {
        Object key = message.get("idempotentKey");
        if (key == null) {
            Object eventType = message.get("eventType");
            Object recordId = message.get("recordId");
            if (eventType != null && recordId != null) {
                key = eventType + ":" + recordId;
            }
        }
        if (key == null) {
            log.warn("事件既无 idempotentKey 也无 eventType+recordId，无法去重，按首次处理放行: {}", message.keySet());
            return true;
        }

        String redisKey = DEDUP_KEY_PREFIX + key;
        Boolean firstTime = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, String.valueOf(System.currentTimeMillis()), DEDUP_TTL);

        if (Boolean.TRUE.equals(firstTime)) {
            return true;
        }
        log.warn("检测到重复投递，跳过通知发送: idempotentKey={}", key);
        return false;
    }

    @RabbitListener(queues = RabbitMQConfig.BORROW_SUCCESS_QUEUE)
    public void handleBorrowSuccess(Map<String, Object> message) {
        if (!claimIdempotencySlot(message)) {
            return;
        }
        BorrowEvent event = toBorrowEvent(message);
        log.info("处理借书成功事件: userId={}, bookTitle={}", event.getUserId(), event.getBookTitle());

        try {
            if (event.getUserPhone() != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("username", event.getUsername());
                params.put("bookTitle", event.getBookTitle());
                params.put("dueDate", event.getDueDate() != null ? event.getDueDate().format(dateFormatter) : "");

                NotifyMessage smsMessage = NotifyMessage.sms(
                        event.getUserPhone(),
                        "BORROW_SUCCESS",
                        params
                );
                notifySenderService.sendSms(smsMessage);
            }

            if (event.getUserEmail() != null) {
                String title = "【图书馆】借书成功通知";
                String content = buildBorrowSuccessEmailContent(event);
                NotifyMessage emailMessage = NotifyMessage.email(
                        event.getUserEmail(),
                        title,
                        content
                );
                notifySenderService.sendEmail(emailMessage);
            }

            String innerTitle = "借书成功";
            String innerContent = String.format("您已成功借阅《%s》，请于%s前归还。",
                    event.getBookTitle(),
                    event.getDueDate() != null ? event.getDueDate().format(dateFormatter) : "");
            NotifyMessage innerMessage = NotifyMessage.inner(
                    event.getUserId(),
                    innerTitle,
                    innerContent
            );
            notifySenderService.sendInnerMessage(innerMessage);

            log.info("借书成功通知发送完成: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("处理借书成功事件失败: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.RETURN_SUCCESS_QUEUE)
    public void handleReturnSuccess(Map<String, Object> message) {
        if (!claimIdempotencySlot(message)) {
            return;
        }
        BorrowEvent event = toBorrowEvent(message);
        log.info("处理还书成功事件: userId={}, bookTitle={}", event.getUserId(), event.getBookTitle());

        try {
            if (event.getUserPhone() != null) {
                Map<String, Object> params = new HashMap<>();
                params.put("username", event.getUsername());
                params.put("bookTitle", event.getBookTitle());
                params.put("returnDate", event.getReturnDate() != null ? event.getReturnDate().format(dateFormatter) : "");

                NotifyMessage smsMessage = NotifyMessage.sms(
                        event.getUserPhone(),
                        "RETURN_SUCCESS",
                        params
                );
                notifySenderService.sendSms(smsMessage);
            }

            String innerTitle = "还书成功";
            String innerContent = String.format("您已成功归还《%s》，感谢使用图书馆服务。",
                    event.getBookTitle());
            NotifyMessage innerMessage = NotifyMessage.inner(
                    event.getUserId(),
                    innerTitle,
                    innerContent
            );
            notifySenderService.sendInnerMessage(innerMessage);

            log.info("还书成功通知发送完成: userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("处理还书成功事件失败: {}", e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.BORROW_FAIL_QUEUE)
    public void handleBorrowFail(Map<String, Object> message) {
        BorrowEvent event = toBorrowEvent(message);
        log.warn("处理借书失败事件: userId={}, bookTitle={}, reason={}",
                event.getUserId(), event.getBookTitle(), event.getFailReason());
    }

    /**
     * 消费 outbound 通知队列 queue.notify.sms / queue.notify.email。
     * 这些队列由 borrow.exchange 以 notify.sms / notify.email 路由键投递，
     * 负责把借阅事件真正"发送"(此处对接短信/邮件网关；无网关时落结构化日志并 ACK，
     * 保证消息不积压、链路闭环)。
     */
    @RabbitListener(queues = {
            RabbitMQConfig.NOTIFY_SMS_QUEUE,
            RabbitMQConfig.NOTIFY_EMAIL_QUEUE
    })
    public void handleOutboundNotify(Map<String, Object> message) {
        BorrowEvent event = toBorrowEvent(message);
        String channel = message.get("channel") != null
                ? String.valueOf(message.get("channel")) : "common";
        log.info("消费 outbound 通知队列: channel={}, userId={}, bookTitle={}, recordId={}",
                channel, event.getUserId(), event.getBookTitle(), event.getRecordId());

        try {
            // 站内通知正文（借书/还书）
            String body = buildNotifyBody(event);
            NotifyMessage inner = NotifyMessage.inner(event.getUserId(), "图书借阅通知", body);
            notifySenderService.sendInnerMessage(inner);

            log.info("outbound 通知处理完成(已消费并落站内/网关): userId={}, bookTitle={}",
                    event.getUserId(), event.getBookTitle());
        } catch (Exception e) {
            log.error("处理 outbound 通知失败: {}", e.getMessage(), e);
        }
    }

    private String buildNotifyBody(BorrowEvent event) {
        if (event.getReturnDate() != null) {
            return String.format("您已成功归还《%s》，感谢使用图书馆服务。",
                    event.getBookTitle());
        }
        return String.format("您已成功借阅《%s》，应还日期：%s。",
                event.getBookTitle(),
                event.getDueDate() != null ? event.getDueDate().format(dateFormatter) : "");
    }

    private BorrowEvent toBorrowEvent(Map<String, Object> message) {
        BorrowEvent event = new BorrowEvent();
        event.setUserId(toLong(message.get("userId")));
        event.setBookId(toLong(message.get("bookId")));
        event.setRecordId(toLong(message.get("recordId")));
        event.setBookTitle(String.valueOf(message.getOrDefault("bookTitle", "未知图书")));
        event.setBorrowDate(toDate(message.get("borrowDate")));
        event.setDueDate(toDate(message.get("dueDate")));
        event.setReturnDate(toDate(message.get("returnDate")));
        event.setUsername(String.valueOf(message.getOrDefault("username", "读者")));
        return event;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate toDate(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private String buildBorrowSuccessEmailContent(BorrowEvent event) {
        return String.format(
                "<html><body>" +
                        "<h3>尊敬的%s，您好！</h3>" +
                        "<p>您已成功借阅以下图书：</p>" +
                        "<table border='1' cellpadding='10'>" +
                        "<tr><th>书名</th><td>《%s》</td></tr>" +
                        "<tr><th>借阅日期</th><td>%s</td></tr>" +
                        "<tr><th>应还日期</th><td>%s</td></tr>" +
                        "</table>" +
                        "<p>请按时归还图书，逾期将产生罚款。</p>" +
                        "<p>此致</p>" +
                        "<p>图书馆管理系统</p>" +
                        "</body></html>",
                event.getUsername(),
                event.getBookTitle(),
                event.getBorrowDate() != null ? event.getBorrowDate().format(dateFormatter) : "",
                event.getDueDate() != null ? event.getDueDate().format(dateFormatter) : ""
        );
    }
}
