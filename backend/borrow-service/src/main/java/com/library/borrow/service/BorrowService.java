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

    @SentinelResource(value = "borrowBook", blockHandler = "borrowBlockHandler", fallback = "borrowFallback")
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

            BookClient.BookCopy selectedCopy;
            if (copyId != null) {
                Result<BookClient.BookCopy> copyResult = bookClient.getCopyById(copyId);
                if (copyResult == null || !"200".equals(String.valueOf(copyResult.getCode()))) {
                    throw new BusinessException(ResultCode.BOOK_COPY_NOT_FOUND);
                }
                selectedCopy = copyResult.getData();
                if (!"available".equals(selectedCopy.getStatus())) {
                    throw new BusinessException(ResultCode.BOOK_COPY_NOT_AVAILABLE);
                }
            } else {
                selectedCopy = null;
                copyId = 1L;
            }

            Result<BookClient.BookCopy> decreaseResult = bookClient.decreaseStock(bookId, copyId);
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
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            record.setDeleted(0);

            borrowRecordMapper.insert(record);
            log.info("借阅记录创建成功: userId={}, bookId={}, copyId={}, recordId={}", 
                userId, bookId, copyId, record.getRecordId());

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

    @SentinelResource(value = "returnBook", blockHandler = "returnBlockHandler", fallback = "returnFallback")
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
            bookClient.increaseStock(record.getBookId(), record.getCopyId());
        } catch (Exception e) {
            log.warn("归还图书时增加库存失败: recordId={}, bookId={}, copyId={}, error={}", 
                record.getRecordId(), record.getBookId(), record.getCopyId(), e.getMessage());
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

    public int countActiveByUserId(Long userId) {
        return borrowRecordMapper.countActiveByUserId(userId);
    }

    public Page<BorrowRecord> pageRecords(int page, int size, Long userId, String status) {
        Page<BorrowRecord> pageParam = new Page<>(page, size);
        
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BorrowRecord::getDeleted, 0);
        
        if (userId != null) {
            wrapper.eq(BorrowRecord::getUserId, userId);
        }
        
        if (status != null && !status.isEmpty()) {
            if ("active".equals(status)) {
                wrapper.isNull(BorrowRecord::getReturnDate);
            } else if ("returned".equals(status)) {
                wrapper.isNotNull(BorrowRecord::getReturnDate);
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

    public void sendBorrowEvent(BorrowRecord record, String eventType) {
        try {
            String routingKey = "success".equals(eventType) ? ROUTING_KEY_BORROW_SUCCESS : ROUTING_KEY_BORROW_RETURN;
            rabbitTemplate.convertAndSend(EXCHANGE_BORROW, routingKey, record);
            log.info("借阅事件发送成功: eventType={}, recordId={}", eventType, record.getRecordId());
        } catch (Exception e) {
            log.warn("借阅事件发送失败: {}", e.getMessage());
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
