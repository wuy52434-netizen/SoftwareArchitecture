package com.library.borrow.client;

import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookClientFallback implements BookClient {

    @Override
    public Result<BookInfo> getBookById(Long id) {
        log.warn("图书服务降级 - getBookById: id={}", id);
        return Result.error(500, "图书服务暂时不可用");
    }

    @Override
    public Result<Void> decreaseStock(Long id, Long copyId) {
        log.warn("图书服务降级 - decreaseStock: id={}, copyId={}", id, copyId);
        return Result.error(500, "图书服务暂时不可用");
    }

    @Override
    public Result<Void> increaseStock(Long id, Long copyId) {
        log.warn("图书服务降级 - increaseStock: id={}, copyId={}", id, copyId);
        return Result.error(500, "图书服务暂时不可用");
    }

    @Override
    public Result<BookCopy> getCopyById(Long copyId) {
        log.warn("图书服务降级 - getCopyById: copyId={}", copyId);
        return Result.error(500, "图书服务暂时不可用");
    }

    @Override
    public Result<BookCopy> getAvailableCopy(Long id) {
        log.warn("图书服务降级 - getAvailableCopy: id={}", id);
        return Result.error(500, "图书服务暂时不可用");
    }

    @Override
    public Result<Void> updateCopyStatus(Long copyId, String status) {
        log.warn("图书服务降级 - updateCopyStatus: copyId={}, status={}", copyId, status);
        return Result.error(500, "图书服务暂时不可用");
    }
}
