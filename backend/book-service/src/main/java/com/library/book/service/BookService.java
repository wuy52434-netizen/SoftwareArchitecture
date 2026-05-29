package com.library.book.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.book.dto.BookDTO.*;
import com.library.book.entity.BookCategory;
import com.library.book.entity.BookCopy;
import com.library.book.entity.BookInfo;
import com.library.book.mapper.BookCopyMapper;
import com.library.book.mapper.BookInfoMapper;
import com.library.common.exception.BusinessException;
import com.library.common.result.ResultCode;
import com.library.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.library.common.constant.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookInfoMapper bookInfoMapper;
    private final BookCopyMapper bookCopyMapper;
    private final RedisUtil redisUtil;
    private final RedisTemplate<String, Object> redisTemplate;

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

    public Page<BookInfo> pageBooks(int page, int size, String search, Long categoryId, String language, String year, String status) {
        Page<BookInfo> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<BookInfo> wrapper = new LambdaQueryWrapper<>();
        
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

        if (language != null && !language.isEmpty()) {
            wrapper.eq(BookInfo::getLanguage, language);
        }

        if (year != null && !year.isEmpty()) {
            if ("older".equals(year)) {
                wrapper.lt(BookInfo::getPublishDate, java.time.LocalDate.of(2021, 1, 1));
            } else {
                try {
                    int publishYear = Integer.parseInt(year);
                    wrapper.ge(BookInfo::getPublishDate, java.time.LocalDate.of(publishYear, 1, 1))
                            .lt(BookInfo::getPublishDate, java.time.LocalDate.of(publishYear + 1, 1, 1));
                } catch (NumberFormatException e) {
                    log.debug("忽略非法出版年份筛选: {}", year);
                }
            }
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
        book.setPublishDate(request.getPublicationDate());
        book.setCoverUrl(request.getCoverImage());
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
        if (book.getId() == null) {
            BookInfo savedBook = getByIsbn(book.getIsbn());
            if (savedBook != null) {
                book.setId(savedBook.getId());
            }
        }
        createBookCopies(book, book.getTotalCopies());
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
            book.setPublishDate(request.getPublicationDate());
        }
        if (request.getCoverImage() != null) {
            book.setCoverUrl(request.getCoverImage());
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
        if (request.getTotalCopies() != null) {
            int oldTotal = book.getTotalCopies() != null ? book.getTotalCopies() : 0;
            int oldAvailable = book.getAvailableCopies() != null ? book.getAvailableCopies() : 0;
            int newTotal = Math.max(request.getTotalCopies(), 0);
            int delta = newTotal - oldTotal;

            book.setTotalCopies(newTotal);
            book.setAvailableCopies(Math.max(oldAvailable + delta, 0));
            if (delta > 0) {
                createBookCopies(book, delta);
            }
            if (book.getAvailableCopies() == 0) {
                book.setStatus(BOOK_STATUS_BORROWED);
            } else if (BOOK_STATUS_BORROWED.equals(book.getStatus())) {
                book.setStatus(BOOK_STATUS_AVAILABLE);
            }
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

        if (BOOK_STATUS_BORROWED.equals(book.getStatus()) || bookCopyMapper.countActiveBorrowsByBookId(id) > 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE, "该图书有未归还的借阅记录，无法删除");
        }

        bookInfoMapper.deleteById(id);

        log.info("图书删除成功: id={}", id);
        clearBookCache();
        redisUtil.delete(REDIS_BOOK_DETAIL_KEY + id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long bookId, int change) {
        BookInfo book = getById(bookId);
        int currentAvailable = book.getAvailableCopies() != null ? book.getAvailableCopies() : 0;
        int newAvailable = currentAvailable + change;
        
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

    private static final Map<Long, String> CATEGORY_NAME_MAP = Map.of(
        1L, "文学", 2L, "历史", 3L, "科技", 4L, "艺术", 5L, "教育", 6L, "其他"
    );

    public BookResponse toBookResponse(BookInfo book) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setIsbn(book.getIsbn());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setPrice(book.getPrice());
        response.setSummary(book.getSummary());
        response.setDescription(book.getDescription());
        response.setPublicationDate(book.getPublishDate());
        response.setPublishDate(book.getPublishDate() != null ? book.getPublishDate().getYear() : null);
        response.setCoverImage(book.getCoverUrl());
        response.setCoverUrl(book.getCoverUrl());
        response.setTotalCopies(book.getTotalCopies());
        response.setAvailableCopies(book.getAvailableCopies());
        response.setStock(book.getAvailableCopies());
        response.setStatus(book.getStatus());
        response.setLanguage(book.getLanguage());
        response.setLocation(book.getLocation());
        response.setCategory(CATEGORY_NAME_MAP.getOrDefault(book.getCategoryId(), "其他"));
        response.setCreatedAt(book.getCreatedAt() != null ? book.getCreatedAt().toString() : null);
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public void decreaseStock(Long bookId, Long copyId) {
        if (copyId != null) {
            decreaseCopyStock(bookId, copyId);
            return;
        }
        updateStock(bookId, -1);
        if (copyId != null) {
            try {
                updateCopyStatus(copyId, COPY_STATUS_BORROWED);
            } catch (Exception e) {
                log.debug("副本状态更新跳过: copyId={}", copyId);
            }
        }
    }

    private void decreaseCopyStock(Long bookId, Long copyId) {
        int copyUpdated = bookCopyMapper.updateStatusIfCurrent(copyId, bookId, COPY_STATUS_AVAILABLE, COPY_STATUS_BORROWED);
        if (copyUpdated <= 0) {
            throw new BusinessException(ResultCode.BOOK_COPY_NOT_AVAILABLE);
        }

        int stockUpdated = bookInfoMapper.updateAvailableCopies(bookId, -1, BOOK_STATUS_AVAILABLE, BOOK_STATUS_BORROWED);
        if (stockUpdated <= 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE);
        }

        clearBookCache();
        redisUtil.delete(REDIS_BOOK_DETAIL_KEY + bookId);
    }

    public BookCopy getAvailableCopyByBookId(Long bookId) {
        BookCopy copy = bookCopyMapper.selectFirstAvailableByBookId(bookId);
        if (copy == null) {
            throw new BusinessException(ResultCode.NO_COPY_AVAILABLE);
        }
        return copy;
    }

    public BookCopy getCopyByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "图书条码不能为空");
        }
        BookCopy copy = bookCopyMapper.selectByBarcode(barcode.trim());
        if (copy == null) {
            throw new BusinessException(ResultCode.BOOK_COPY_NOT_FOUND, "未找到该图书副本条码");
        }
        return copy;
    }

    public ScanBookResponse scanBook(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请输入图书条码或ISBN");
        }

        String keyword = code.trim();
        BookCopy copy = bookCopyMapper.selectByBarcode(keyword);
        BookInfo book;
        String inputType;

        if (copy != null) {
            book = getById(copy.getBookId());
            inputType = "barcode";
        } else {
            book = getByIsbn(keyword);
            if (book == null) {
                Page<BookInfo> pageResult = pageBooks(1, 1, keyword, null, null, null, null);
                book = pageResult.getRecords().isEmpty() ? null : pageResult.getRecords().get(0);
            }
            if (book == null) {
                throw new BusinessException(ResultCode.BOOK_NOT_FOUND, "未找到该图书");
            }
            inputType = "isbn";
            copy = bookCopyMapper.selectFirstAvailableByBookId(book.getId());
        }

        ScanBookResponse response = new ScanBookResponse();
        response.setInputType(inputType);
        response.setBook(toBookResponse(book));
        response.setCopy(copy != null ? toCopyResponse(copy, book) : null);
        response.setCopyAvailable(copy != null && COPY_STATUS_AVAILABLE.equals(copy.getStatus()));
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public void increaseStock(Long bookId, Long copyId) {
        if (copyId != null) {
            increaseCopyStock(bookId, copyId);
            return;
        }
        updateStock(bookId, 1);
        if (copyId != null) {
            try {
                updateCopyStatus(copyId, COPY_STATUS_AVAILABLE);
            } catch (Exception e) {
                log.debug("副本状态更新跳过: copyId={}", copyId);
            }
        }
    }

    private void increaseCopyStock(Long bookId, Long copyId) {
        int copyUpdated = bookCopyMapper.updateStatusIfCurrent(copyId, bookId, COPY_STATUS_BORROWED, COPY_STATUS_AVAILABLE);
        if (copyUpdated <= 0) {
            throw new BusinessException(ResultCode.BOOK_OPERATION_FAILED);
        }

        int stockUpdated = bookInfoMapper.updateAvailableCopies(bookId, 1, BOOK_STATUS_AVAILABLE, BOOK_STATUS_BORROWED);
        if (stockUpdated <= 0) {
            throw new BusinessException(ResultCode.BOOK_OPERATION_FAILED);
        }

        clearBookCache();
        redisUtil.delete(REDIS_BOOK_DETAIL_KEY + bookId);
    }

    public BookCopy getCopyById(Long copyId) {
        BookCopy copy = bookCopyMapper.selectById(copyId);
        if (copy == null) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND, "副本不存在");
        }
        return copy;
    }

    public CopyResponse toCopyResponse(BookCopy copy, BookInfo book) {
        CopyResponse response = new CopyResponse();
        response.setCopyId(copy.getCopyId());
        response.setBookId(copy.getBookId());
        response.setIsbn(book != null ? book.getIsbn() : null);
        response.setBarcode(copy.getBarcode());
        response.setStatus(copy.getStatus());
        response.setLocationId(copy.getLocationId());
        response.setCondition(copy.getBookCondition());
        response.setStorageDate(copy.getCreatedAt() != null ? copy.getCreatedAt().toLocalDate().toString() : null);
        if (book != null) {
            response.setBookInfo(toBookResponse(book));
        }
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateCopyStatus(Long copyId, String status) {
        BookCopy copy = getCopyById(copyId);
        copy.setStatus(status);
        bookCopyMapper.updateById(copy);
    }

    private void clearBookCache() {
        try {
            Set<String> keys = redisTemplate.keys(REDIS_BOOK_LIST_KEY + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清理图书列表缓存失败", e);
        }
    }

    private void createBookCopies(BookInfo book, int count) {
        if (count <= 0 || book.getId() == null) {
            return;
        }

        Long locationId = resolveLocationId(book);
        for (int i = 0; i < count; i++) {
            BookCopy copy = new BookCopy();
            copy.setBookId(book.getId());
            copy.setBarcode("BC" + book.getId() + "-" + UUID.randomUUID().toString().substring(0, 8));
            copy.setLocationId(locationId);
            copy.setStatus(COPY_STATUS_AVAILABLE);
            copy.setBookCondition("new");
            copy.setDeleted(0);
            copy.setCreatedAt(LocalDateTime.now());
            copy.setUpdatedAt(LocalDateTime.now());
            bookCopyMapper.insert(copy);
        }
    }

    private Long resolveLocationId(BookInfo book) {
        if (book.getLocation() != null && !book.getLocation().isBlank()) {
            Long locationId = bookCopyMapper.selectLocationIdByName(book.getLocation());
            if (locationId != null) {
                return locationId;
            }
        }
        Long fallbackLocationId = bookCopyMapper.selectFirstLocationId();
        if (fallbackLocationId == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请先初始化馆藏位置数据");
        }
        return fallbackLocationId;
    }
}
