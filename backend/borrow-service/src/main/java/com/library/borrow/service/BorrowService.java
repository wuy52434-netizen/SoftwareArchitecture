package com.library.borrow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.borrow.dto.BorrowDTO.*;
import com.library.borrow.entity.BorrowRecord;
import com.library.borrow.mapper.BorrowRecordMapper;
import com.library.common.exception.BusinessException;
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

    @Value("${system.default-borrow-days:30}")
    private int defaultBorrowDays;

    @Value("${system.max-borrow-count:5}")
    private int maxBorrowCount;

    @Value("${system.overdue-fine-per-day:0.5}")
    private double overdueFinePerDay;

    @Value("${system.max-fine-amount:50.0}")
    private double maxFineAmount;

    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord borrowBook(Long userId, BorrowRequest request) {
        Long bookId = request.getBookId();
        String lockKey = REDIS_BORROW_LOCK_KEY + bookId;

        RedisLock.LockResult lockResult = redisLock.tryLock(lockKey, 3, 5, java.util.concurrent.TimeUnit.SECONDS);
        
        if (!lockResult.isLocked()) {
            throw new BusinessException(ResultCode.LOCK_ACQUIRE_FAILED);
        }

        try {
            int activeCount = borrowRecordMapper.countActiveByUserId(userId);
            if (activeCount >= maxBorrowCount) {
                throw new BusinessException(ResultCode.BORROW_LIMIT_EXCEEDED);
            }

            LocalDate borrowDate = request.getBorrowDate() != null ? request.getBorrowDate() : LocalDate.now();
            LocalDate dueDate = request.getDueDate() != null ? request.getDueDate() : borrowDate.plusDays(defaultBorrowDays);

            BorrowRecord record = new BorrowRecord();
            record.setUserId(userId);
            record.setCopyId(1L);
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
            log.info("借阅记录创建成功: userId={}, bookId={}, recordId={}", userId, bookId, record.getRecordId());

            sendBorrowEvent(record, "success");
            clearBookCache();

            return record;

        } finally {
            redisLock.unlock(lockResult);
        }
    }

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

        record.setReturnDate(returnDate);
        record.setStatus(BORROW_STATUS_RETURNED);
        record.setFineAmount(fineAmount);
        record.setUpdatedAt(LocalDateTime.now());
        
        borrowRecordMapper.updateById(record);

        log.info("图书归还成功: recordId={}, overdueDays={}, fineAmount={}", 
                record.getRecordId(), overdueDays, fineAmount);

        sendBorrowEvent(record, "return");
        clearBookCache();

        ReturnResponse response = new ReturnResponse();
        response.setRecordId(record.getRecordId());
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

    private void sendBorrowEvent(BorrowRecord record, String eventType) {
        try {
            String routingKey = "success".equals(eventType) ? ROUTING_KEY_BORROW_SUCCESS : ROUTING_KEY_BORROW_RETURN;
            rabbitTemplate.convertAndSend(EXCHANGE_BORROW, routingKey, record);
            log.info("借阅事件发送成功: eventType={}, recordId={}", eventType, record.getRecordId());
        } catch (Exception e) {
            log.warn("借阅事件发送失败: {}", e.getMessage());
        }
    }

    private void clearBookCache() {
    }

    public BorrowResponse toBorrowResponse(BorrowRecord record) {
        BorrowResponse response = new BorrowResponse();
        response.setRecordId(record.getRecordId());
        response.setUserId(record.getUserId());
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
}
