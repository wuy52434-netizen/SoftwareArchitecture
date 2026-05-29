package com.library.borrow.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.borrow.client.BookClient;
import com.library.borrow.client.UserClient;
import com.library.borrow.dto.BorrowDTO.*;
import com.library.borrow.entity.BorrowRecord;
import com.library.borrow.mapper.BorrowRecordMapper;
import com.library.common.exception.BusinessException;
import com.library.common.result.Result;
import com.library.common.result.ResultCode;
import com.library.common.util.RedisLock;
import com.library.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.library.common.constant.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BorrowService {

    private final BorrowRecordMapper borrowRecordMapper;
    private final RedisLock redisLock;
    private final RedisUtil redisUtil;
    private final RabbitTemplate rabbitTemplate;
    private final BookClient bookClient;
    private final UserClient userClient;

    @Value("${system.default-borrow-days:30}")
    private int defaultBorrowDays;

    @Value("${system.max-borrow-count:5}")
    private int maxBorrowCount;

    @Value("${system.overdue-fine-per-day:0.5}")
    private double overdueFinePerDay;

    @Value("${system.max-fine-amount:50.0}")
    private double maxFineAmount;

    @SentinelResource(value = "borrowBook", blockHandler = "borrowBlockHandler", fallback = "borrowFallback",
            exceptionsToIgnore = BusinessException.class)
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord borrowBook(Long userId, BorrowRequest request) {
        Long bookId = request.getBookId();
        Long copyId = request.getCopyId();
        
        String lockKey = REDIS_BORROW_LOCK_KEY + bookId + ":" + (copyId != null ? copyId : "any");

        RedisLock.LockResult lockResult = redisLock.tryLock(lockKey, 3, 10, java.util.concurrent.TimeUnit.SECONDS);
        
        if (!lockResult.isLocked()) {
            throw new BusinessException(ResultCode.LOCK_ACQUIRE_FAILED);
        }

        try {
            Result<UserClient.UserInfo> userResult = userClient.getUserById(userId);
            if (userResult == null || !"200".equals(String.valueOf(userResult.getCode()))) {
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }
            UserClient.UserInfo userInfo = userResult.getData();
            
            if (!"active".equals(userInfo.getStatus())) {
                throw new BusinessException(ResultCode.USER_STATUS_INVALID);
            }

            int activeCount = borrowRecordMapper.countActiveByUserId(userId);
            int userMaxCount = userInfo.getMaxBorrowCount() != null ? userInfo.getMaxBorrowCount() : maxBorrowCount;
            
            if (activeCount >= userMaxCount) {
                throw new BusinessException(ResultCode.BORROW_LIMIT_EXCEEDED, 
                    String.format("已借阅%d本，最多可借阅%d本", activeCount, userMaxCount));
            }

            Result<BookClient.BookInfo> bookResult = bookClient.getBookById(bookId);
            if (bookResult == null || !"200".equals(String.valueOf(bookResult.getCode()))) {
                throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
            }
            BookClient.BookInfo bookInfo = bookResult.getData();

            if (bookInfo.getAvailableCopies() == null || bookInfo.getAvailableCopies() <= 0) {
                throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE);
            }

            if (copyId != null) {
                Result<BookClient.BookCopy> copyResult = bookClient.getCopyById(copyId);
                if (copyResult == null || !"200".equals(String.valueOf(copyResult.getCode()))) {
                    throw new BusinessException(ResultCode.BOOK_COPY_NOT_FOUND);
                }
                BookClient.BookCopy selectedCopy = copyResult.getData();
                if (selectedCopy == null || selectedCopy.getBookId() == null || !bookId.equals(selectedCopy.getBookId())) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "图书副本与图书不匹配");
                }
                if (!"available".equals(selectedCopy.getStatus())) {
                    throw new BusinessException(ResultCode.BOOK_COPY_NOT_AVAILABLE);
                }
            } else {
                Result<BookClient.BookCopy> copyResult = bookClient.getAvailableCopy(bookId);
                if (copyResult == null || !"200".equals(String.valueOf(copyResult.getCode())) || copyResult.getData() == null) {
                    throw new BusinessException(ResultCode.NO_COPY_AVAILABLE);
                }
                copyId = copyResult.getData().getId();
            }

            Result<Void> decreaseResult = bookClient.decreaseStock(bookId, copyId);
            if (decreaseResult == null || !"200".equals(String.valueOf(decreaseResult.getCode()))) {
                throw new BusinessException(ResultCode.BOOK_OPERATION_FAILED, "扣减库存失败");
            }

            userClient.updateBorrowCount(userId, 1);

            LocalDate borrowDate = request.getBorrowDate() != null ? request.getBorrowDate() : LocalDate.now();
            LocalDate dueDate = request.getDueDate() != null ? request.getDueDate() : borrowDate.plusDays(defaultBorrowDays);

            BorrowRecord record = new BorrowRecord();
            record.setUserId(userId);
            record.setBookId(bookId);
            record.setCopyId(copyId);
            record.setBorrowDate(borrowDate);
            record.setDueDate(dueDate);
            record.setReturnDate(null);
            record.setStatus(BORROW_STATUS_ACTIVE);
            record.setFineAmount(BigDecimal.ZERO);
            record.setFinePaid(0);
            record.setOperatorId(userId);
            record.setRemark(request.getNote());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.setDeleted(0);

            borrowRecordMapper.insert(record);
            log.info("借阅记录创建成功: userId={}, bookId={}, copyId={}, recordId={}",
                userId, bookId, copyId, record.getRecordId());

            record.setBookTitle(bookInfo.getTitle());
            sendBorrowEvent(record, "success");
            clearBookCache(bookId);

            return record;

        } catch (BusinessException e) {
            log.error("借书失败: userId={}, bookId={}, error={}", userId, bookId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("借书异常: userId={}, bookId={}, error={}", userId, bookId, e.getMessage(), e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "借书操作失败");
        } finally {
            redisLock.unlock(lockResult);
        }
    }

    @SentinelResource(value = "returnBook", blockHandler = "returnBlockHandler", fallback = "returnFallback",
            exceptionsToIgnore = BusinessException.class)
    @Transactional(rollbackFor = Exception.class)
    public ReturnResponse returnBook(Long userId, ReturnRequest request) {
        BorrowRecord record;
        
        if (request.getBorrowId() != null) {
            record = borrowRecordMapper.selectById(request.getBorrowId());
        } else if (request.getBookId() != null) {
            record = borrowRecordMapper.selectActiveByBookId(request.getBookId());
        } else {
            throw new BusinessException(ResultCode.PARAM_ERROR, "bookId 或 borrowId 必须提供");
        }

        if (record == null) {
            throw new BusinessException(ResultCode.BORROW_NOT_FOUND);
        }

        if (record.getReturnDate() != null) {
            throw new BusinessException(ResultCode.BORROW_ALREADY_RETURNED);
        }

        LocalDate returnDate = LocalDate.now();
        LocalDate dueDate = record.getDueDate();
        
        int overdueDays = 0;
        BigDecimal fineAmount = BigDecimal.ZERO;
        
        if (dueDate != null && returnDate.isAfter(dueDate)) {
            overdueDays = (int) ChronoUnit.DAYS.between(dueDate, returnDate);
            double fine = Math.min(overdueDays * overdueFinePerDay, maxFineAmount);
            fineAmount = BigDecimal.valueOf(fine);
        }

        try {
            Result<Void> increaseResult = bookClient.increaseStock(record.getBookId(), record.getCopyId());
            if (increaseResult == null || !"200".equals(String.valueOf(increaseResult.getCode()))) {
                throw new BusinessException(ResultCode.BOOK_OPERATION_FAILED, "增加库存失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("归还图书时增加库存失败: recordId={}, bookId={}, copyId={}, error={}",
                    record.getRecordId(), record.getBookId(), record.getCopyId(), e.getMessage(), e);
            throw new BusinessException(ResultCode.BOOK_OPERATION_FAILED, "归还失败，库存未更新");
        }

        try {
            userClient.updateBorrowCount(userId, -1);
        } catch (Exception e) {
            log.warn("归还图书时更新用户借阅数失败: userId={}, error={}", userId, e.getMessage());
        }

        record.setReturnDate(returnDate);
        record.setStatus(BORROW_STATUS_RETURNED);
        record.setFineAmount(fineAmount);
        record.setUpdatedAt(LocalDateTime.now());
        
        borrowRecordMapper.updateById(record);

        log.info("图书归还成功: recordId={}, overdueDays={}, fineAmount={}", 
                record.getRecordId(), overdueDays, fineAmount);

        populateBookTitle(record);
        sendBorrowEvent(record, "return");
        clearBookCache(record.getBookId());

        ReturnResponse response = new ReturnResponse();
        response.setRecordId(record.getRecordId());
        response.setBookId(record.getBookId());
        response.setReturnDate(returnDate);
        response.setOverdueDays(overdueDays);
        response.setFineAmount(fineAmount);
        response.setStatus("已归还");
        
        return response;
    }

    public BorrowRecord getById(Long recordId) {
        BorrowRecord record = borrowRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(ResultCode.BORROW_NOT_FOUND);
        }
        return record;
    }

    public List<BorrowRecord> getActiveByUserId(Long userId) {
        return borrowRecordMapper.selectActiveByUserId(userId);
    }

    public BorrowRecord getActiveByBookId(Long bookId) {
        BorrowRecord record = borrowRecordMapper.selectActiveByBookId(bookId);
        if (record == null) {
            throw new BusinessException(ResultCode.BORROW_NOT_FOUND);
        }
        return record;
    }

    public int countActiveByUserId(Long userId) {
        return borrowRecordMapper.countActiveByUserId(userId);
    }

    public Page<BorrowRecord> pageRecords(int page, int size, Long userId, Long bookId, String status) {
        Page<BorrowRecord> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (userId != null) {
            wrapper.eq(BorrowRecord::getUserId, userId);
        }

        if (bookId != null) {
            wrapper.eq(BorrowRecord::getBookId, bookId);
        }
        
        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            if ("active".equals(status)) {
                wrapper.isNull(BorrowRecord::getReturnDate);
            } else if ("returned".equals(status)) {
                wrapper.isNotNull(BorrowRecord::getReturnDate);
            } else if ("overdue".equals(status)) {
                wrapper.isNull(BorrowRecord::getReturnDate)
                        .lt(BorrowRecord::getDueDate, LocalDate.now());
            } else {
                wrapper.eq(BorrowRecord::getStatus, status);
            }
        }
        
        wrapper.orderByDesc(BorrowRecord::getCreatedAt);
        
        return borrowRecordMapper.selectPage(pageParam, wrapper);
    }

    public BigDecimal checkOverdue(Long recordId) {
        BorrowRecord record = getById(recordId);
        if (record.getReturnDate() != null) {
            return record.getFineAmount() != null ? record.getFineAmount() : BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = record.getDueDate();
        
        if (dueDate != null && today.isAfter(dueDate)) {
            int overdueDays = (int) ChronoUnit.DAYS.between(dueDate, today);
            double fine = Math.min(overdueDays * overdueFinePerDay, maxFineAmount);
            return BigDecimal.valueOf(fine);
        }
        
        return BigDecimal.ZERO;
    }

    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord renewBook(Long userId, Long recordId, Integer days) {
        BorrowRecord record = getById(recordId);
        if (record.getReturnDate() != null || BORROW_STATUS_RETURNED.equals(record.getStatus())) {
            throw new BusinessException(ResultCode.BORROW_ALREADY_RETURNED);
        }
        if (userId != null && !userId.equals(record.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        int renewDays = days != null && days > 0 ? days : defaultBorrowDays;
        LocalDate baseDate = record.getDueDate() != null && record.getDueDate().isAfter(LocalDate.now())
                ? record.getDueDate()
                : LocalDate.now();
        record.setDueDate(baseDate.plusDays(renewDays));
        record.setUpdatedAt(LocalDateTime.now());
        borrowRecordMapper.updateById(record);

        log.info("借阅记录续借成功: recordId={}, newDueDate={}", recordId, record.getDueDate());
        return record;
    }

    public void sendBorrowEvent(BorrowRecord record, String eventType) {
        try {
            String routingKey = "success".equals(eventType) ? ROUTING_KEY_BORROW_SUCCESS : ROUTING_KEY_BORROW_RETURN;

            java.util.Map<String, Object> message = new java.util.HashMap<>();
            message.put("eventType", "success".equals(eventType) ? "BORROW_SUCCESS" : "RETURN_SUCCESS");
            message.put("recordId", record.getRecordId());
            message.put("userId", record.getUserId());
            message.put("bookId", record.getBookId());
            message.put("copyId", record.getCopyId());
            message.put("bookTitle", record.getBookTitle());
            message.put("borrowDate", record.getBorrowDate() != null ? record.getBorrowDate().toString() : null);
            message.put("dueDate", record.getDueDate() != null ? record.getDueDate().toString() : null);
            message.put("returnDate", record.getReturnDate() != null ? record.getReturnDate().toString() : null);
            message.put("timestamp", System.currentTimeMillis());

            rabbitTemplate.convertAndSend(EXCHANGE_BORROW, routingKey, message);
            log.info("借阅事件发送成功: eventType={}, recordId={}", eventType, record.getRecordId());
        } catch (Exception e) {
            log.warn("借阅事件发送失败(不影响核心借还流程): {}", e.getMessage());
        }
    }

    private void clearBookCache(Long bookId) {
        try {
            redisUtil.del("book:info:" + bookId);
            redisUtil.del("book:list:*");
            log.debug("图书缓存已清除: bookId={}", bookId);
        } catch (Exception e) {
            log.warn("清除图书缓存失败: {}", e.getMessage());
        }
    }

    private void populateBookTitle(BorrowRecord record) {
        if (record.getBookTitle() != null) {
            return;
        }
        try {
            Result<BookClient.BookInfo> bookResult = bookClient.getBookById(record.getBookId());
            if (bookResult != null && "200".equals(String.valueOf(bookResult.getCode())) && bookResult.getData() != null) {
                record.setBookTitle(bookResult.getData().getTitle());
            }
        } catch (Exception e) {
            log.debug("获取图书标题失败: bookId={}", record.getBookId());
        }
    }

    public BorrowResponse toBorrowResponse(BorrowRecord record) {
        BorrowResponse response = new BorrowResponse();
        response.setRecordId(record.getRecordId());
        response.setUserId(record.getUserId());
        response.setBookId(record.getBookId());
        response.setCopyId(record.getCopyId());
        response.setBorrowDate(record.getBorrowDate());
        response.setDueDate(record.getDueDate());
        response.setReturnDate(record.getReturnDate());
        response.setStatus(record.getStatus());
        response.setFineAmount(record.getFineAmount());
        response.setFinePaid(record.getFinePaid());
        response.setRemark(record.getRemark());

        try {
            Result<BookClient.BookInfo> bookResult = bookClient.getBookById(record.getBookId());
            if (bookResult != null && "200".equals(String.valueOf(bookResult.getCode()))) {
                BookClient.BookInfo book = bookResult.getData();
                response.setBookTitle(book.getTitle());
                response.setBookAuthor(book.getAuthor());
                response.setBookCoverUrl(book.getCoverImage());
            }
        } catch (Exception e) {
            log.debug("获取图书信息失败: bookId={}", record.getBookId());
        }

        try {
            if (record.getCopyId() != null) {
                Result<BookClient.BookCopy> copyResult = bookClient.getCopyById(record.getCopyId());
                if (copyResult != null && "200".equals(String.valueOf(copyResult.getCode())) && copyResult.getData() != null) {
                    response.setCopyBarcode(copyResult.getData().getBarcode());
                }
            }
        } catch (Exception e) {
            log.debug("获取图书副本信息失败: copyId={}", record.getCopyId());
        }

        try {
            Result<UserClient.UserInfo> userResult = userClient.getUserById(record.getUserId());
            if (userResult != null && "200".equals(String.valueOf(userResult.getCode()))) {
                UserClient.UserInfo user = userResult.getData();
                response.setReaderName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                response.setReaderId(user.getUsername());
            }
        } catch (Exception e) {
            log.debug("获取用户信息失败: userId={}", record.getUserId());
        }

        return response;
    }

    public BorrowRecord borrowBlockHandler(Long userId, BorrowRequest request, Throwable e) {
        log.warn("借书请求被限流: userId={}, bookId={}", userId, request.getBookId());
        throw new BusinessException(ResultCode.SYSTEM_BUSY);
    }

    public BorrowRecord borrowFallback(Long userId, BorrowRequest request, Throwable e) {
        log.warn("借书请求降级: userId={}, bookId={}, error={}", userId, request.getBookId(), e.getMessage());
        throw new BusinessException(ResultCode.SYSTEM_ERROR, "借书服务暂时不可用");
    }

    public ReturnResponse returnBlockHandler(Long userId, ReturnRequest request, Throwable e) {
        log.warn("还书请求被限流: userId={}", userId);
        throw new BusinessException(ResultCode.SYSTEM_BUSY);
    }

    public ReturnResponse returnFallback(Long userId, ReturnRequest request, Throwable e) {
        log.warn("还书请求降级: userId={}, error={}", userId, e.getMessage());
        throw new BusinessException(ResultCode.SYSTEM_ERROR, "还书服务暂时不可用");
    }
}
