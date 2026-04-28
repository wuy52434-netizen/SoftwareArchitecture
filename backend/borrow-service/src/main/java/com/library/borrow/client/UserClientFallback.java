package com.library.borrow.client;

import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserClientFallback implements UserClient {

    @Override
    public Result<UserInfo> getUserById(Long id) {
        log.warn("用户服务降级 - getUserById: id={}", id);
        return Result.error(500, "用户服务暂时不可用");
    }

    @Override
    public Result<UserInfo> getCurrentUser(String token) {
        log.warn("用户服务降级 - getCurrentUser");
        return Result.error(500, "用户服务暂时不可用");
    }

    @Override
    public Result<Void> updateBorrowCount(Long id, Integer delta) {
        log.warn("用户服务降级 - updateBorrowCount: id={}, delta={}", id, delta);
        return Result.error(500, "用户服务暂时不可用");
    }

    @Override
    public Result<Void> updateUserStatus(Long id, String status) {
        log.warn("用户服务降级 - updateUserStatus: id={}, status={}", id, status);
        return Result.error(500, "用户服务暂时不可用");
    }
}
