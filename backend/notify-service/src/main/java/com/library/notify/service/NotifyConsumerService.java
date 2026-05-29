package com.library.notify.service;

import com.library.notify.config.RabbitMQConfig;
import com.library.notify.dto.BorrowEvent;
import com.library.notify.dto.NotifyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyConsumerService {

    private final NotifySenderService notifySenderService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    @RabbitListener(queues = RabbitMQConfig.BORROW_SUCCESS_QUEUE)
    public void handleBorrowSuccess(Map<String, Object> message) {
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
