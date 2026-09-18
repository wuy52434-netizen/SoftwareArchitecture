package com.library.search.service;

import com.library.common.result.Result;
import com.library.common.result.ResultCode;
import com.library.search.client.BookClient;
import com.library.search.config.BookSyncConfig;
import com.library.search.document.BookDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 图书变更事件消费端：把 book-service 的写入同步到 Elasticsearch（KNWN-ES-01）。
 *
 * <p>修复前 {@code SearchService.indexBook()} 与 {@code deleteIndex()} 都**没有任何调用方**，
 * ES 索引只能靠手工跑 seed 脚本灌一次静态快照，于是：
 * 新上架的书在借书机搜不到、改过的书仍显示旧标题、删掉的书还能被搜到（点进去 404）。
 *
 * <p>异常处理原则：解析/回查失败直接抛出，让 Spring AMQP 的重试与死信机制接管
 * （队列已配 DLQ），而不是 try-catch 吞掉 —— 吞掉就等于"静默不同步"，
 * 而这正是本次要修的缺陷形态。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookSyncConsumer {

    private final SearchService searchService;
    private final BookClient bookClient;

    @RabbitListener(queues = BookSyncConfig.BOOK_SYNC_QUEUE)
    public void onBookChanged(Map<String, Object> message) {
        Object rawId = message.get("bookId");
        Object operation = message.get("operation");
        if (rawId == null) {
            log.error("图书变更事件缺少 bookId，无法同步: {}", message);
            return;
        }
        Long bookId = Long.valueOf(String.valueOf(rawId));

        if ("DELETE".equalsIgnoreCase(String.valueOf(operation))) {
            searchService.deleteIndex(bookId);
            log.info("索引同步完成: bookId={}, op=DELETE", bookId);
            return;
        }

        Result<BookClient.BookDetail> result = bookClient.getBookById(bookId);
        if (result == null) {
            throw new IllegalStateException("回查图书详情返回 null，本次索引同步中止: bookId=" + bookId);
        }
        if (!result.isSuccess()) {
            // 「图书已不存在」不是可重试错误，而是"这条书已经被删了"的正常结果。
            // 若把它当异常抛出→重试 5 次(退避后约 26s)→进死信，队列会被这类消息堵住，
            // 后面真正需要同步的事件要排队等它耗完（实测就是这么把索引同步拖挂的）。
            // 正确动作是幂等地把索引里的残留清掉，然后 ACK。
            if (ResultCode.BOOK_NOT_FOUND.getCode().equals(result.getCode())) {
                log.info("图书已不存在，改为清理索引残留: bookId={}", bookId);
                searchService.deleteIndex(bookId);
                return;
            }
            throw new IllegalStateException(String.format(
                    "回查图书详情失败，本次索引同步中止: bookId=%d, code=%s, message=%s",
                    bookId, result.getCode(), result.getMessage()));
        }
        if (result.getData() == null) {
            throw new IllegalStateException("回查图书详情成功但 data 为空: bookId=" + bookId);
        }

        searchService.indexBook(toDocument(result.getData()));
        log.info("索引同步完成: bookId={}, title={}", bookId, result.getData().getTitle());
    }

    private BookDocument toDocument(BookClient.BookDetail detail) {
        BookDocument doc = new BookDocument();
        doc.setId(detail.getId());
        doc.setIsbn(detail.getIsbn());
        doc.setTitle(detail.getTitle());
        doc.setAuthor(detail.getAuthor());
        doc.setPublisher(detail.getPublisher());
        doc.setCategory(detail.getCategory());
        doc.setSummary(detail.getSummary());
        doc.setPrice(detail.getPrice());
        doc.setPublishDate(detail.getPublishDate());
        doc.setCoverUrl(detail.getCoverUrl());
        doc.setStatus(detail.getStatus());
        doc.setAvailableCopies(detail.getAvailableCopies());
        doc.setBorrowCount(detail.getBorrowCount() != null ? detail.getBorrowCount() : 0);
        doc.setCreatedAt(parseDate(detail.getCreatedAt()));
        // 变更即写入，updatedAt 就是现在 —— 这个字段此前在索引里根本不存在（KNWN-ES-03）
        doc.setUpdatedAt(LocalDate.now());
        return doc;
    }

    /** 图书接口返回的 createdAt 是 "yyyy-MM-ddTHH:mm:ss" 字符串，索引里只存日期。 */
    private LocalDate parseDate(String raw) {
        if (raw == null || raw.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(raw.substring(0, 10));
        } catch (DateTimeParseException e) {
            log.warn("createdAt 无法解析为日期，索引中将留空: {}", raw);
            return null;
        }
    }
}
