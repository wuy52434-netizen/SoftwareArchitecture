package com.library.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.book.dto.BookDTO.*;
import com.library.book.entity.BookCategory;
import com.library.book.entity.BookCopy;
import com.library.book.entity.BookInfo;
import com.library.book.mapper.BookInfoMapper;
import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import com.library.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.library.common.constant.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookInfoMapper bookInfoMapper;
    private final RedisUtil redisUtil;

    public BookInfo getById(Long id) {
        String cacheKey = REDIS_BOOK_DETAIL_KEY + id;
        BookInfo cached = redisUtil.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BookInfo book = bookInfoMapper.selectById(id);
        if (book == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }

        redisUtil.setEx(cacheKey, book, CACHE_BOOK_DETAIL_TTL);
        return book;
    }

    public BookInfo getByIsbn(String isbn) {
        return bookInfoMapper.selectByIsbn(isbn);
    }

    public Page<BookInfo> pageBooks(int page, int size, String search, Long categoryId, String status) {
        Page<BookInfo> pageParam = new Page<>(page, size);
        
        LambdaQueryWrapper<BookInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookInfo::getDeleted, 0);
        
        if (search != null && !search.isEmpty()) {
            wrapper.and(w -> w
                    .like(BookInfo::getTitle, search)
                    .or()
                    .like(BookInfo::getAuthor, search)
                    .or()
                    .like(BookInfo::getIsbn, search)
            );
        }
        
        if (categoryId != null) {
            wrapper.eq(BookInfo::getCategoryId, categoryId);
        }
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(BookInfo::getStatus, status);
        }
        
        wrapper.orderByDesc(BookInfo::getCreatedAt);
        
        return bookInfoMapper.selectPage(pageParam, wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public BookInfo createBook(BookCreateRequest request) {
        BookInfo existBook = getByIsbn(request.getIsbn());
        if (existBook != null) {
            throw new BusinessException(ResultCode.ISBN_EXIST);
        }

        BookInfo book = new BookInfo();
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisherId(request.getPublisherId() != null ? request.getPublisherId() : 1L);
        book.setCategoryId(request.getCategoryId());
        book.setPrice(request.getPrice());
        book.setSummary(request.getSummary());
        book.setPublicationDate(request.getPublicationDate());
        book.setCoverImage(request.getCoverImage());
        book.setTotalCopies(request.getTotalCopies() != null ? request.getTotalCopies() : 0);
        book.setAvailableCopies(request.getTotalCopies() != null ? request.getTotalCopies() : 0);
        book.setStatus(BOOK_STATUS_AVAILABLE);
        book.setLanguage(request.getLanguage() != null ? request.getLanguage() : "中文");
        book.setDescription(request.getDescription());
        book.setLocation(request.getLocation());
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        book.setDeleted(0);

        bookInfoMapper.insert(book);
        log.info("图书创建成功: id={}, title={}", book.getId(), book.getTitle());
        
        clearBookCache();
        return book;
    }

    @Transactional(rollbackFor = Exception.class)
    public BookInfo updateBook(Long id, BookUpdateRequest request) {
        BookInfo book = getById(id);

        if (request.getTitle() != null) {
            book.setTitle(request.getTitle());
        }
        if (request.getAuthor() != null) {
            book.setAuthor(request.getAuthor());
        }
        if (request.getCategoryId() != null) {
            book.setCategoryId(request.getCategoryId());
        }
        if (request.getPrice() != null) {
            book.setPrice(request.getPrice());
        }
        if (request.getSummary() != null) {
            book.setSummary(request.getSummary());
        }
        if (request.getPublicationDate() != null) {
            book.setPublicationDate(request.getPublicationDate());
        }
        if (request.getCoverImage() != null) {
            book.setCoverImage(request.getCoverImage());
        }
        if (request.getStatus() != null) {
            book.setStatus(request.getStatus());
        }
        if (request.getLanguage() != null) {
            book.setLanguage(request.getLanguage());
        }
        if (request.getDescription() != null) {
            book.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            book.setLocation(request.getLocation());
        }

        book.setUpdatedAt(LocalDateTime.now());
        bookInfoMapper.updateById(book);
        
        log.info("图书更新成功: id={}", id);
        clearBookCache();
        redisUtil.delete(REDIS_BOOK_DETAIL_KEY + id);
        
        return book;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBook(Long id) {
        BookInfo book = getById(id);
        
        if (BOOK_STATUS_BORROWED.equals(book.getStatus())) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE, "该图书有未归还的借阅记录，无法删除");
        }

        book.setDeleted(1);
        book.setUpdatedAt(LocalDateTime.now());
        bookInfoMapper.updateById(book);
        
        log.info("图书删除成功: id={}", id);
        clearBookCache();
        redisUtil.delete(REDIS_BOOK_DETAIL_KEY + id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long bookId, int change) {
        BookInfo book = getById(bookId);
        int newAvailable = book.getAvailableCopies() + change;
        
        if (newAvailable < 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE);
        }
        
        book.setAvailableCopies(newAvailable);
        
        if (newAvailable == 0) {
            book.setStatus(BOOK_STATUS_BORROWED);
        } else if (newAvailable > 0 && BOOK_STATUS_BORROWED.equals(book.getStatus())) {
            book.setStatus(BOOK_STATUS_AVAILABLE);
        }
        
        book.setUpdatedAt(LocalDateTime.now());
        bookInfoMapper.updateById(book);
        
        log.info("图书库存更新: id={}, change={}, available={}", bookId, change, newAvailable);
        clearBookCache();
        redisUtil.delete(REDIS_BOOK_DETAIL_KEY + bookId);
    }

    public BookResponse toBookResponse(BookInfo book) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setIsbn(book.getIsbn());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setPrice(book.getPrice());
        response.setSummary(book.getSummary());
        response.setDescription(book.getDescription());
        response.setPublicationDate(book.getPublicationDate());
        response.setPublishDate(book.getPublicationDate() != null ? book.getPublicationDate().getYear() : null);
        response.setCoverImage(book.getCoverImage());
        response.setCoverUrl(book.getCoverImage() != null ? book.getCoverImage() : "/static/images/" + book.getTitle() + ".jpg");
        response.setTotalCopies(book.getTotalCopies());
        response.setAvailableCopies(book.getAvailableCopies());
        response.setStock(book.getAvailableCopies());
        response.setStatus(book.getStatus());
        response.setLanguage(book.getLanguage());
        response.setLocation(book.getLocation());
        response.setCreatedAt(book.getCreatedAt() != null ? book.getCreatedAt().toString() : null);
        return response;
    }

    private void clearBookCache() {
    }
}
