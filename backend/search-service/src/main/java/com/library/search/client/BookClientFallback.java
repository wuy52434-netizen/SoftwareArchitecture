package com.library.search.client;

import com.library.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 图书详情降级实现。
 *
 * <p>返回 {@code success=false} 而不是编造一份空文档：消费端据此决定"跳过本次同步并让消息
 * 进入重试/死信"，而不是往索引里写一条字段全空的记录。后者会让检索结果出现空标题条目，
 * 比暂时不同步更难排查。
 */
@Slf4j
@Component
public class BookClientFallback implements BookClient {

    @Override
    public Result<BookDetail> getBookById(Long id) {
        log.warn("图书服务降级 - getBookById: id={}，本次索引同步跳过", id);
        return Result.error(500, "图书服务暂时不可用");
    }
}
