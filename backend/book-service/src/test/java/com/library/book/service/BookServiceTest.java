package com.library.book.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.book.dto.BookDTO.BookCreateRequest;
import com.library.book.dto.BookDTO.BookResponse;
import com.library.book.dto.BookDTO.BookUpdateRequest;
import com.library.book.dto.BookDTO.ScanBookResponse;
import com.library.book.entity.BookCopy;
import com.library.book.entity.BookInfo;
import com.library.book.event.BookChangedEvent;
import com.library.book.mapper.BookCopyMapper;
import com.library.book.mapper.BookInfoMapper;
import com.library.common.constant.Constants;
import com.library.common.exception.BusinessException;
import com.library.common.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BookService 单元测试
 * 技术栈: JUnit5 + Mockito + AssertJ
 * 覆盖核心业务规则: Redis 缓存旁路(Cache-Aside)、异常分支、库存边界、条码/ISBN 扫描。
 */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookInfoMapper bookInfoMapper;
    @Mock private BookCopyMapper bookCopyMapper;
    @Mock private RedisUtil redisUtil;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private SetOperations<String, Object> setOperations;
    /** 图书变更事件发布器：写操作必须发出同步事件（KNWN-ES-01 的单元级保障） */
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private BookService bookService;

    private BookInfo buildBook(Long id) {
        BookInfo book = new BookInfo();
        book.setId(id);
        book.setIsbn("9787123456789");
        book.setTitle("三体");
        book.setAuthor("刘慈欣");
        book.setPublisherId(1L);
        book.setPrice(new BigDecimal("68.00"));
        book.setCategoryId(1L);
        book.setSummary("科幻小说");
        book.setPublishDate(LocalDate.of(2008, 1, 1));
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setStatus(Constants.BOOK_STATUS_AVAILABLE);
        book.setLanguage("中文");
        return book;
    }

    /**
     * 让 clearBookCache() 不报错。默认返回空 key 集合（不会真正 delete）。
     * 使用 lenient 避免 strict stubbing 对"未触发缓存清理"的用例报 UnnecessaryStubbing。
     */
    private void stubClearCacheBase() {
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.keys(Constants.REDIS_BOOK_LIST_KEY + "*")).thenReturn(Collections.emptySet());
    }

    /**
     * 取出并校验写操作发出的图书变更事件（KNWN-ES-01）。
     *
     * <p>把"写操作必须发事件"下沉到单元测试：这条链路一旦被误删，
     * 检索索引会静默停止更新，而接口层看不出任何异常 —— 必须有人盯着。
     */
    private BookChangedEvent captureBookChangedEvent() {
        ArgumentCaptor<BookChangedEvent> captor = ArgumentCaptor.forClass(BookChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("getById - Redis 缓存旁路")
    class GetByIdTest {

        @Test
        @DisplayName("缓存命中时直接返回缓存值，不查数据库")
        void hitCache_shouldReturnCachedAndNotQueryDb() {
            BookInfo cached = buildBook(1L);
            when(redisUtil.get(Constants.REDIS_BOOK_DETAIL_KEY + 1L)).thenReturn(cached);

            BookInfo result = bookService.getById(1L);

            assertThat(result).isSameAs(cached);
            verify(bookInfoMapper, never()).selectById(any());
        }

        @Test
        @DisplayName("缓存未命中时查库并将结果回填缓存")
        void missCache_shouldQueryDbAndBackfill() {
            BookInfo db = buildBook(1L);
            when(redisUtil.get(Constants.REDIS_BOOK_DETAIL_KEY + 1L)).thenReturn(null);
            when(bookInfoMapper.selectById(1L)).thenReturn(db);

            BookInfo result = bookService.getById(1L);

            assertThat(result).isEqualTo(db);
            verify(redisUtil).setEx(
                    eq(Constants.REDIS_BOOK_DETAIL_KEY + 1L),
                    eq(db),
                    eq((long) Constants.CACHE_BOOK_DETAIL_TTL));
        }

        @Test
        @DisplayName("缓存与数据库均未命中时抛出 BusinessException")
        void notFound_shouldThrowBusinessException() {
            when(redisUtil.get(Constants.REDIS_BOOK_DETAIL_KEY + 999L)).thenReturn(null);
            when(bookInfoMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> bookService.getById(999L))
                    .isInstanceOf(BusinessException.class);
            verify(redisUtil, never()).setEx(anyString(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("getByIsbn")
    class IsbnQueryTest {

        @Test
        @DisplayName("按 ISBN 精确查询")
        void byIsbn_shouldDelegateToMapper() {
            String isbn = "9787123456789";
            when(bookInfoMapper.selectByIsbn(isbn)).thenReturn(buildBook(1L));

            BookInfo result = bookService.getByIsbn(isbn);

            assertThat(result.getIsbn()).isEqualTo(isbn);
        }
    }

    @Nested
    @DisplayName("pageBooks - 分页筛选")
    class PageBooksTest {

        private void stubEmptyPage() {
            Page<BookInfo> empty = new Page<>(1, 24);
            empty.setRecords(Collections.emptyList());
            when(bookInfoMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(empty);
        }

        @Test
        @DisplayName("search 关键字被正确传递")
        void withSearch_shouldDelegate() {
            stubEmptyPage();
            Page<BookInfo> result = bookService.pageBooks(1, 24, "三体", null, null, null, null);
            assertThat(result.getRecords()).isEmpty();
        }

        @Test
        @DisplayName("year 为具体年份时按区间筛选")
        void withYear_ok() {
            stubEmptyPage();
            Page<BookInfo> result = bookService.pageBooks(1, 24, null, null, null, "2010", null);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("year 非法输入不抛异常")
        void invalidYear_ignored() {
            stubEmptyPage();
            Page<BookInfo> result = bookService.pageBooks(1, 24, null, null, null, "abc", null);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("createBook")
    class CreateBookTest {

        private BookCreateRequest buildCreateRequest() {
            BookCreateRequest req = new BookCreateRequest();
            req.setIsbn("9780000000001");
            req.setTitle("新书");
            req.setAuthor("作者甲");
            req.setCategoryId(3L);
            req.setPrice(new BigDecimal("39.00"));
            req.setTotalCopies(2);
            req.setPublicationDate(LocalDate.of(2024, 5, 1));
            req.setLanguage("中文");
            return req;
        }

        @Test
        @DisplayName("ISBN 重复时抛出 BusinessException，不插入")
        void duplicateIsbn_shouldThrow() {
            when(bookInfoMapper.selectByIsbn("9780000000001")).thenReturn(buildBook(1L));

            assertThatThrownBy(() -> bookService.createBook(buildCreateRequest()))
                    .isInstanceOf(BusinessException.class);
            verify(bookInfoMapper, never()).insert(any(BookInfo.class));
        }

        @Test
        @DisplayName("正常创建：写入书 + 创建副本 + 清理列表缓存")
        void createOk() {
            when(bookInfoMapper.selectByIsbn("9780000000001")).thenReturn(null);
            when(bookInfoMapper.insert(any(BookInfo.class))).thenAnswer(inv -> {
                BookInfo b = inv.getArgument(0);
                b.setId(99L);
                return 1;
            });
            when(bookCopyMapper.selectFirstLocationId()).thenReturn(1L);
            // 返回非空 key 集合，且匹配 delete 调用
            lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(redisTemplate.keys(Constants.REDIS_BOOK_LIST_KEY + "*"))
                    .thenReturn(new HashSet<>(Collections.singleton(Constants.REDIS_BOOK_DETAIL_KEY + "1")));

            BookInfo created = bookService.createBook(buildCreateRequest());

            assertThat(created.getId()).isEqualTo(99L);
            assertThat(created.getAvailableCopies()).isEqualTo(2);
            assertThat(created.getStatus()).isEqualTo(Constants.BOOK_STATUS_AVAILABLE);
            verify(bookCopyMapper, times(2)).insert(any(BookCopy.class));
            verify(redisTemplate).delete(anySet());

            // 新建图书必须发出 UPSERT 事件，否则索引不会更新（KNWN-ES-01）
            BookChangedEvent event = captureBookChangedEvent();
            assertThat(event.operation()).isEqualTo(BookChangedEvent.Operation.UPSERT);
            assertThat(event.bookId()).isEqualTo(99L);
        }
    }

    @Nested
    @DisplayName("updateBook")
    class UpdateBookTest {

        @Test
        @DisplayName("更新 title 并清理对应详情缓存")
        void updateTitle_shouldClearDetailCache() {
            BookInfo db = buildBook(1L);
            when(redisUtil.get(Constants.REDIS_BOOK_DETAIL_KEY + 1L)).thenReturn(db);
            stubClearCacheBase();

            BookUpdateRequest req = new BookUpdateRequest();
            req.setTitle("三体·新改");

            BookInfo updated = bookService.updateBook(1L, req);

            assertThat(updated.getTitle()).isEqualTo("三体·新改");
            verify(redisUtil).delete(Constants.REDIS_BOOK_DETAIL_KEY + 1L);

            // 修改图书必须发出 UPSERT 事件，否则检索结果长期是旧标题（KNWN-ES-01）
            BookChangedEvent event = captureBookChangedEvent();
            assertThat(event.operation()).isEqualTo(BookChangedEvent.Operation.UPSERT);
            assertThat(event.bookId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("deleteBook")
    class DeleteBookTest {

        @Test
        @DisplayName("有未归还借阅记录时禁止删除")
        void hasActiveBorrow_shouldThrow() {
            BookInfo borrowed = buildBook(1L);
            borrowed.setStatus(Constants.BOOK_STATUS_BORROWED);
            when(redisUtil.get(Constants.REDIS_BOOK_DETAIL_KEY + 1L)).thenReturn(null);
            when(bookInfoMapper.selectById(1L)).thenReturn(borrowed);

            assertThatThrownBy(() -> bookService.deleteBook(1L))
                    .isInstanceOf(BusinessException.class);
            verify(bookInfoMapper, never()).deleteById(any());
        }

        @Test
        @DisplayName("无借阅时可删除并清理缓存")
        void deleteOk() {
            when(redisUtil.get(Constants.REDIS_BOOK_DETAIL_KEY + 1L)).thenReturn(null);
            when(bookInfoMapper.selectById(1L)).thenReturn(buildBook(1L));
            when(bookCopyMapper.countActiveBorrowsByBookId(1L)).thenReturn(0);
            stubClearCacheBase();

            bookService.deleteBook(1L);

            verify(bookInfoMapper).deleteById(1L);
            verify(redisUtil).delete(Constants.REDIS_BOOK_DETAIL_KEY + 1L);

            // 删除必须发出 DELETE 事件，否则索引里会留下"点进去 404"的幽灵记录（KNWN-ES-01）
            BookChangedEvent event = captureBookChangedEvent();
            assertThat(event.operation()).isEqualTo(BookChangedEvent.Operation.DELETE);
            assertThat(event.bookId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("副本条码与扫码")
    class BarcodeTest {

        @Test
        @DisplayName("条码为空时抛 BusinessException")
        void blankBarcode_shouldThrow() {
            assertThatThrownBy(() -> bookService.getCopyByBarcode("   "))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("扫码命中条码副本时 inputType=barcode")
        void scanByBarcode_ok() {
            BookCopy copy = new BookCopy();
            copy.setCopyId(50L);
            copy.setBookId(1L);
            copy.setBarcode("BC111-BARCODE");
            copy.setStatus(Constants.COPY_STATUS_AVAILABLE);
            when(bookCopyMapper.selectByBarcode("BC111-BARCODE")).thenReturn(copy);
            when(redisUtil.get(Constants.REDIS_BOOK_DETAIL_KEY + 1L)).thenReturn(null);
            when(bookInfoMapper.selectById(1L)).thenReturn(buildBook(1L));

            ScanBookResponse resp = bookService.scanBook("BC111-BARCODE");

            assertThat(resp.getInputType()).isEqualTo("barcode");
            assertThat(resp.getCopyAvailable()).isTrue();
            assertThat(resp.getBook().getTitle()).isEqualTo("三体");
        }
    }

    @Nested
    @DisplayName("库存变更")
    class StockTest {

        @Test
        @DisplayName("库存减到 0 时状态变为 borrowed")
        void stockToZero_shouldMarkBorrowed() {
            BookInfo book = buildBook(1L);
            book.setAvailableCopies(1);
            when(redisUtil.get(Constants.REDIS_BOOK_DETAIL_KEY + 1L)).thenReturn(null);
            when(bookInfoMapper.selectById(1L)).thenReturn(book);
            stubClearCacheBase();

            bookService.updateStock(1L, -1);

            ArgumentCaptor<BookInfo> captor = ArgumentCaptor.forClass(BookInfo.class);
            verify(bookInfoMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(Constants.BOOK_STATUS_BORROWED);
            assertThat(captor.getValue().getAvailableCopies()).isZero();
        }
    }

    @Nested
    @DisplayName("toBookResponse 映射")
    class MappingTest {

        @Test
        @DisplayName("BookInfo 正确映射为 BookResponse")
        void mapping_ok() {
            BookInfo book = buildBook(1L);
            BookResponse resp = bookService.toBookResponse(book);

            assertThat(resp).isNotNull();
            assertThat(resp.getTitle()).isEqualTo("三体");
            assertThat(resp.getTotalCopies()).isEqualTo(3);
            assertThat(resp.getCategory()).isEqualTo("文学");
            assertThat(resp.getPublishDate()).isEqualTo(2008);
        }
    }
}