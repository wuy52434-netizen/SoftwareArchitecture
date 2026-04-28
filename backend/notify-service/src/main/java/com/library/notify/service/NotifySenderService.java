package com.library.notify.service;

import com.library.notify.dto.NotifyMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotifySenderService {

    public void sendSms(NotifyMessage message) {
        log.info("[模拟] 发送短信到: {}, 模板: {}, 参数: {}",
                message.getReceiver(),
                message.getTemplateCode(),
                message.getParams());

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[模拟] 短信发送成功: {}", message.getReceiver());
    }

    public void sendEmail(NotifyMessage message) {
        log.info("[模拟] 发送邮件到: {}, 标题: {}",
                message.getReceiver(),
                message.getTitle());

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[模拟] 邮件发送成功: {}", message.getReceiver());
    }

    public void sendInnerMessage(NotifyMessage message) {
        log.info("[模拟] 发送站内消息给用户: {}, 标题: {}",
                message.getUserId(),
                message.getTitle());

        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[模拟] 站内消息发送成功: userId={}", message.getUserId());
    }
}
