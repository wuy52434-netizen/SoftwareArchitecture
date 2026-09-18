package com.library.book.event;

/**
 * 图书变更领域事件（KNWN-ES-01）。
 *
 * <p>只携带 **图书 id + 操作类型**，不带整份图书快照：
 * 字段到 ES 文档的映射只应存在一处（search-service 的 BookDocument 附近），
 * 如果在生产端也复制一份映射，两边迟早漂移。消费端拿到 id 后回查图书详情即可。
 *
 * @param bookId 图书主键
 * @param operation UPSERT（新增/修改/上下架/库存变化）或 DELETE
 */
public record BookChangedEvent(Long bookId, Operation operation) {

    public enum Operation {
        UPSERT,
        DELETE
    }

    public static BookChangedEvent upsert(Long bookId) {
        return new BookChangedEvent(bookId, Operation.UPSERT);
    }

    public static BookChangedEvent delete(Long bookId) {
        return new BookChangedEvent(bookId, Operation.DELETE);
    }
}
