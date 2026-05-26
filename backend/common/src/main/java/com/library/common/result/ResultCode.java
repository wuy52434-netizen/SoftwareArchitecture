package com.library.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败"),
    SYSTEM_ERROR(500, "系统错误"),
    SYSTEM_BUSY(503, "系统繁忙，请稍后重试"),
    
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USER_DISABLED(1003, "用户已被禁用"),
    USER_STATUS_INVALID(1003, "用户状态无效"),
    USERNAME_EXIST(1004, "用户名已存在"),
    
    BOOK_NOT_FOUND(2001, "图书不存在"),
    BOOK_NOT_AVAILABLE(2002, "图书已被借出"),
    BOOK_FROZEN(2003, "图书已被冻结"),
    ISBN_EXIST(2004, "ISBN已存在"),
    NO_COPY_AVAILABLE(2005, "没有可用的图书副本"),
    BOOK_COPY_NOT_FOUND(2006, "图书副本不存在"),
    BOOK_COPY_NOT_AVAILABLE(2007, "图书副本不可用"),
    BOOK_OPERATION_FAILED(2008, "图书操作失败"),
    
    BORROW_NOT_FOUND(3001, "借阅记录不存在"),
    BORROW_ALREADY_RETURNED(3002, "图书已归还"),
    BORROW_LIMIT_EXCEEDED(3003, "借阅数量已达上限"),
    OVERDUE_EXIST(3004, "存在逾期未还图书"),
    
    SYSTEM_SETTING_NOT_FOUND(4001, "系统设置不存在"),
    
    LOCK_ACQUIRE_FAILED(5001, "当前请求较多，请稍后重试"),
    
    SERVICE_UNAVAILABLE(6001, "服务暂时不可用");

    private final Integer code;
    private final String message;
}
