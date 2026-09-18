package com.library.stats.client;

import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图书服务降级实现。
 *
 * <p>返回 {@code success=false} 而不是编造一个数字：调用方据此区分
 * "拿到了权威值" 与 "图书服务不可用"，前者直接用，后者才退回到本地计数器并打告警。
 * 若这里直接编一个 0 或默认值，看板会静默展示错数字，比明显报错更危险。
 */
@Slf4j
@Component
public class BookClientFallback implements BookClient {

    @Override
    public Result<BookPage> listBooks(int page, int perPage, String status) {
        log.warn("图书服务降级 - listBooks: status={}，看板将退回本地计数器口径", status);
        return Result.error(500, "图书服务暂时不可用");
    }
}
