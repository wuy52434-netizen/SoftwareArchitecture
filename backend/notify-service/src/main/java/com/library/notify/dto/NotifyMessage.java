package com.library.notify.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyMessage implements Serializable {

    private String notifyType;
    private String receiver;
    private String templateCode;
    private String title;
    private String content;
    private Map<String, Object> params;
    private Long userId;
    private String username;

    public static NotifyMessage sms(String phone, String templateCode, Map<String, Object> params) {
        return NotifyMessage.builder()
                .notifyType("SMS")
                .receiver(phone)
                .templateCode(templateCode)
                .params(params)
                .build();
    }

    public static NotifyMessage email(String email, String title, String content) {
        return NotifyMessage.builder()
                .notifyType("EMAIL")
                .receiver(email)
                .title(title)
                .content(content)
                .build();
    }

    public static NotifyMessage inner(Long userId, String title, String content) {
        return NotifyMessage.builder()
                .notifyType("INNER")
                .userId(userId)
                .title(title)
                .content(content)
                .build();
    }
}
