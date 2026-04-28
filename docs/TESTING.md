# 测试文档

本文档详细描述图书管理系统的测试规范和测试方法。

---

## 目录

- [测试规范](#测试规范)
- [单元测试](#单元测试)
- [集成测试](#集成测试)
- [接口测试](#接口测试)
- [性能测试](#性能测试)
- [安全测试](#安全测试)
- [测试覆盖率](#测试覆盖率)
- [测试报告](#测试报告)

---

## 测试规范

### 测试分层策略

```
┌─────────────────────────────────────────────────────────────┐
│                      E2E 测试 (10%)                           │
│                   端到端功能测试                                │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                      集成测试 (20%)                            │
│                   模块间协作测试                                │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                      单元测试 (70%)                            │
│                   单个组件测试                                  │
└─────────────────────────────────────────────────────────────┘
```

### 测试原则

1. **FIRST 原则**
   - **F**ast：快速执行
   - **I**ndependent：相互独立
   - **R**epeatable：可重复执行
   - **S**elf-validating：自动验证
   - **T**imely：及时编写

2. **AAA 模式**
   - **A**rrange：准备数据
   - **A**ct：执行操作
   - **A**ssert：验证结果

3. **测试命名规范**
   - `should_行为_when_条件`
   - `方法名_场景_期望结果`

### 测试覆盖率目标

| 类型 | 覆盖率目标 | 说明 |
|------|-----------|------|
| 行覆盖率 (Line) | >= 80% | 代码行覆盖率 |
| 分支覆盖率 (Branch) | >= 70% | 条件分支覆盖率 |
| 方法覆盖率 (Method) | >= 90% | 方法覆盖率 |
| 类覆盖率 (Class) | >= 90% | 类覆盖率 |

---

## 单元测试

### 测试环境

#### 依赖配置

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Mockito -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mysql</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>redis</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

#### 测试配置

创建 `src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: jdbc:tc:mysql:8.0:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  
  data:
    redis:
      host: localhost
      port: 6379
  
  rabbitmq:
    host: localhost
    port: 5672

jwt:
  secret: test-jwt-secret-key-for-testing-purposes-only
  access-token-expire: 3600000
  refresh-token-expire: 86400000

system:
  default-borrow-days: 30
  max-borrow-count: 5
  overdue-fine-per-day: 0.5
  max-fine-amount: 50.0

logging:
  level:
    com.library: debug
    org.springframework.test: debug
```

### Service 层测试

#### 示例：用户服务测试

```java
package com.library.user.service;

import com.library.common.exception.BusinessException;
import com.library.user.dto.UserDTO;
import com.library.user.entity.User;
import com.library.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTest {
    
    @Mock
    private UserMapper userMapper;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserServiceImpl userService;
    
    @Test
    @DisplayName("根据ID获取用户 - 成功")
    void should_return_user_when_get_by_id_given_valid_id() {
        // Arrange
        Long userId = 1L;
        User mockUser = User.builder()
            .id(userId)
            .username("testuser")
            .email("test@example.com")
            .status("active")
            .build();
        
        when(userMapper.selectById(userId)).thenReturn(mockUser);
        
        // Act
        UserDTO result = userService.getUserById(userId);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(userMapper, times(1)).selectById(userId);
    }
    
    @Test
    @DisplayName("根据ID获取用户 - 失败（用户不存在）")
    void should_throw_exception_when_get_by_id_given_invalid_id() {
        // Arrange
        Long userId = 999L;
        when(userMapper.selectById(userId)).thenReturn(null);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, 
            () -> userService.getUserById(userId));
        
        assertThat(exception.getCode()).isEqualTo(404);
        verify(userMapper, times(1)).selectById(userId);
    }
    
    @Test
    @DisplayName("创建用户 - 成功")
    void should_create_user_when_given_valid_data() {
        // Arrange
        UserDTO userDTO = UserDTO.builder()
            .username("newuser")
            .password("password123")
            .email("newuser@example.com")
            .build();
        
        when(userMapper.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encodedPassword");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        
        // Act
        UserDTO result = userService.createUser(userDTO);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("newuser");
        verify(userMapper, times(1)).existsByUsername("newuser");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userMapper, times(1)).insert(any(User.class));
    }
    
    @Test
    @DisplayName("创建用户 - 失败（用户名已存在）")
    void should_throw_exception_when_create_user_given_existing_username() {
        // Arrange
        UserDTO userDTO = UserDTO.builder()
            .username("existinguser")
            .password("password123")
            .build();
        
        when(userMapper.existsByUsername("existinguser")).thenReturn(true);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> userService.createUser(userDTO));
        
        assertThat(exception.getMessage()).contains("用户名已存在");
        verify(userMapper, times(1)).existsByUsername("existinguser");
        verify(userMapper, never()).insert(any(User.class));
    }
    
    @Test
    @DisplayName("更新用户 - 成功")
    void should_update_user_when_given_valid_data() {
        // Arrange
        Long userId = 1L;
        UserDTO updateDTO = UserDTO.builder()
            .id(userId)
            .email("updated@example.com")
            .phone("13800138000")
            .build();
        
        User existingUser = User.builder()
            .id(userId)
            .username("testuser")
            .email("old@example.com")
            .status("active")
            .build();
        
        when(userMapper.selectById(userId)).thenReturn(existingUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        
        // Act
        UserDTO result = userService.updateUser(userId, updateDTO);
        
        // Assert
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getPhone()).isEqualTo("13800138000");
        verify(userMapper, times(1)).updateById(any(User.class));
    }
    
    @Test
    @DisplayName("删除用户 - 成功")
    void should_delete_user_when_given_valid_id() {
        // Arrange
        Long userId = 1L;
        User existingUser = User.builder()
            .id(userId)
            .username("testuser")
            .deleted(0)
            .build();
        
        when(userMapper.selectById(userId)).thenReturn(existingUser);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        
        // Act
        userService.deleteUser(userId);
        
        // Assert
        verify(userMapper, times(1)).selectById(userId);
        verify(userMapper, times(1)).updateById(any(User.class));
    }
}
```

#### 示例：借阅服务测试

```java
package com.library.borrow.service;

import com.library.borrow.client.BookClient;
import com.library.borrow.client.UserClient;
import com.library.borrow.dto.BorrowDTO;
import com.library.borrow.entity.BorrowRecord;
import com.library.borrow.mapper.BorrowRecordMapper;
import com.library.common.exception.BusinessException;
import com.library.common.result.Result;
import com.library.common.util.RedisLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("借阅服务测试")
class BorrowServiceTest {
    
    @Mock
    private BorrowRecordMapper borrowRecordMapper;
    
    @Mock
    private RedisLock redisLock;
    
    @Mock
    private RabbitTemplate rabbitTemplate;
    
    @Mock
    private BookClient bookClient;
    
    @Mock
    private UserClient userClient;
    
    @InjectMocks
    private BorrowService borrowService;
    
    @Test
    @DisplayName("借书 - 成功")
    void should_borrow_book_when_given_valid_data() {
        // Arrange
        Long userId = 1L;
        Long bookId = 1L;
        Long copyId = 1L;
        
        BorrowDTO.BorrowRequest request = new BorrowDTO.BorrowRequest();
        request.setBookId(bookId);
        request.setCopyId(copyId);
        
        // Mock 锁获取成功
        RedisLock.LockResult lockResult = mock(RedisLock.LockResult.class);
        when(lockResult.isLocked()).thenReturn(true);
        when(redisLock.tryLock(anyString(), anyLong(), anyLong(), any())).thenReturn(lockResult);
        
        // Mock 用户信息
        UserClient.UserInfo userInfo = new UserClient.UserInfo();
        userInfo.setId(userId);
        userInfo.setStatus("active");
        userInfo.setMaxBorrowCount(5);
        when(userClient.getUserById(userId)).thenReturn(Result.success(userInfo));
        
        // Mock 活跃借阅数
        when(borrowRecordMapper.countActiveByUserId(userId)).thenReturn(0);
        
        // Mock 图书信息
        BookClient.BookInfo bookInfo = new BookClient.BookInfo();
        bookInfo.setId(bookId);
        bookInfo.setAvailableCopies(5);
        when(bookClient.getBookById(bookId)).thenReturn(Result.success(bookInfo));
        
        // Mock 副本信息
        BookClient.BookCopy bookCopy = new BookClient.BookCopy();
        bookCopy.setId(copyId);
        bookCopy.setStatus("available");
        when(bookClient.getCopyById(copyId)).thenReturn(Result.success(bookCopy));
        
        // Mock 库存扣减
        when(bookClient.decreaseStock(bookId, copyId)).thenReturn(Result.success(bookCopy));
        when(userClient.updateBorrowCount(eq(userId), eq(1))).thenReturn(Result.success());
        
        // Mock 插入记录
        when(borrowRecordMapper.insert(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord record = invocation.getArgument(0);
            record.setRecordId(1L);
            return 1;
        });
        
        // Act
        BorrowRecord result = borrowService.borrowBook(userId, request);
        
        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(bookId, result.getBookId());
        assertEquals("active", result.getStatus());
        verify(borrowRecordMapper, times(1)).insert(any(BorrowRecord.class));
        verify(redisLock, times(1)).unlock(any());
    }
    
    @Test
    @DisplayName("借书 - 失败（借阅数量已达上限）")
    void should_throw_exception_when_borrow_count_exceeds_limit() {
        // Arrange
        Long userId = 1L;
        Long bookId = 1L;
        
        BorrowDTO.BorrowRequest request = new BorrowDTO.BorrowRequest();
        request.setBookId(bookId);
        
        // Mock 锁获取成功
        RedisLock.LockResult lockResult = mock(RedisLock.LockResult.class);
        when(lockResult.isLocked()).thenReturn(true);
        when(redisLock.tryLock(anyString(), anyLong(), anyLong(), any())).thenReturn(lockResult);
        
        // Mock 用户信息
        UserClient.UserInfo userInfo = new UserClient.UserInfo();
        userInfo.setId(userId);
        userInfo.setStatus("active");
        userInfo.setMaxBorrowCount(5);
        when(userClient.getUserById(userId)).thenReturn(Result.success(userInfo));
        
        // Mock 活跃借阅数已满
        when(borrowRecordMapper.countActiveByUserId(userId)).thenReturn(5);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> borrowService.borrowBook(userId, request));
        
        assertTrue(exception.getMessage().contains("已借阅"));
        verify(borrowRecordMapper, never()).insert(any(BorrowRecord.class));
        verify(redisLock, times(1)).unlock(any());
    }
    
    @Test
    @DisplayName("借书 - 失败（图书不可借）")
    void should_throw_exception_when_book_not_available() {
        // Arrange
        Long userId = 1L;
        Long bookId = 1L;
        
        BorrowDTO.BorrowRequest request = new BorrowDTO.BorrowRequest();
        request.setBookId(bookId);
        
        // Mock 锁获取成功
        RedisLock.LockResult lockResult = mock(RedisLock.LockResult.class);
        when(lockResult.isLocked()).thenReturn(true);
        when(redisLock.tryLock(anyString(), anyLong(), anyLong(), any())).thenReturn(lockResult);
        
        // Mock 用户信息
        UserClient.UserInfo userInfo = new UserClient.UserInfo();
        userInfo.setStatus("active");
        userInfo.setMaxBorrowCount(5);
        when(userClient.getUserById(userId)).thenReturn(Result.success(userInfo));
        
        // Mock 活跃借阅数
        when(borrowRecordMapper.countActiveByUserId(userId)).thenReturn(0);
        
        // Mock 图书无库存
        BookClient.BookInfo bookInfo = new BookClient.BookInfo();
        bookInfo.setId(bookId);
        bookInfo.setAvailableCopies(0);
        when(bookClient.getBookById(bookId)).thenReturn(Result.success(bookInfo));
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> borrowService.borrowBook(userId, request));
        
        assertTrue(exception.getMessage().contains("不可借") || exception.getMessage().contains("库存"));
    }
    
    @Test
    @DisplayName("还书 - 成功（无逾期）")
    void should_return_book_successfully_when_no_overdue() {
        // Arrange
        Long userId = 1L;
        Long recordId = 1L;
        Long bookId = 1L;
        
        BorrowDTO.ReturnRequest request = new BorrowDTO.ReturnRequest();
        request.setBorrowId(recordId);
        
        // Mock 借阅记录
        BorrowRecord record = BorrowRecord.builder()
            .recordId(recordId)
            .userId(userId)
            .bookId(bookId)
            .copyId(1L)
            .dueDate(LocalDate.now().plusDays(10))
            .build();
        
        when(borrowRecordMapper.selectById(recordId)).thenReturn(record);
        when(bookClient.increaseStock(eq(bookId), anyLong())).thenReturn(Result.success());
        when(userClient.updateBorrowCount(eq(userId), eq(-1))).thenReturn(Result.success());
        when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);
        
        // Act
        BorrowDTO.ReturnResponse response = borrowService.returnBook(userId, request);
        
        // Assert
        assertNotNull(response);
        assertEquals(0, response.getOverdueDays());
        assertEquals(0, response.getFineAmount().compareTo(java.math.BigDecimal.ZERO));
        verify(borrowRecordMapper, times(1)).updateById(any(BorrowRecord.class));
    }
    
    @Test
    @DisplayName("还书 - 成功（有逾期）")
    void should_return_book_with_fine_when_overdue() {
        // Arrange
        Long userId = 1L;
        Long recordId = 1L;
        Long bookId = 1L;
        
        BorrowDTO.ReturnRequest request = new BorrowDTO.ReturnRequest();
        request.setBorrowId(recordId);
        
        // Mock 借阅记录（逾期 10 天）
        BorrowRecord record = BorrowRecord.builder()
            .recordId(recordId)
            .userId(userId)
            .bookId(bookId)
            .copyId(1L)
            .dueDate(LocalDate.now().minusDays(10))
            .build();
        
        when(borrowRecordMapper.selectById(recordId)).thenReturn(record);
        when(bookClient.increaseStock(eq(bookId), anyLong())).thenReturn(Result.success());
        when(userClient.updateBorrowCount(eq(userId), eq(-1))).thenReturn(Result.success());
        when(borrowRecordMapper.updateById(any(BorrowRecord.class))).thenReturn(1);
        
        // Act
        BorrowDTO.ReturnResponse response = borrowService.returnBook(userId, request);
        
        // Assert
        assertNotNull(response);
        assertTrue(response.getOverdueDays() >= 10);
        assertTrue(response.getFineAmount().compareTo(java.math.BigDecimal.valueOf(5.0)) >= 0);
    }
    
    @Test
    @DisplayName("还书 - 失败（记录不存在）")
    void should_throw_exception_when_return_record_not_found() {
        // Arrange
        Long userId = 1L;
        Long recordId = 999L;
        
        BorrowDTO.ReturnRequest request = new BorrowDTO.ReturnRequest();
        request.setBorrowId(recordId);
        
        when(borrowRecordMapper.selectById(recordId)).thenReturn(null);
        
        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
            () -> borrowService.returnBook(userId, request));
        
        assertTrue(exception.getMessage().contains("不存在"));
    }
}
```

### Controller 层测试

```java
package com.library.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.user.dto.UserDTO;
import com.library.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("用户控制器测试")
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("获取用户详情 - 成功")
    @WithMockUser(username = "testuser", roles = {"ADMIN"})
    void should_return_user_when_get_by_id() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDTO userDTO = UserDTO.builder()
            .id(userId)
            .username("testuser")
            .email("test@example.com")
            .role("reader")
            .status("active")
            .build();
        
        when(userService.getUserById(userId)).thenReturn(userDTO);
        
        // Act & Assert
        mockMvc.perform(get("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("testuser"))
            .andExpect(jsonPath("$.data.email").value("test@example.com"));
        
        verify(userService, times(1)).getUserById(userId);
    }
    
    @Test
    @DisplayName("创建用户 - 成功")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_create_user_when_post_valid_data() throws Exception {
        // Arrange
        UserDTO createDTO = UserDTO.builder()
            .username("newuser")
            .password("password123")
            .email("newuser@example.com")
            .build();
        
        UserDTO savedDTO = UserDTO.builder()
            .id(1L)
            .username("newuser")
            .email("newuser@example.com")
            .role("reader")
            .status("active")
            .build();
        
        when(userService.createUser(any(UserDTO.class))).thenReturn(savedDTO);
        
        // Act & Assert
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.username").value("newuser"));
        
        verify(userService, times(1)).createUser(any(UserDTO.class));
    }
    
    @Test
    @DisplayName("更新用户 - 成功")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_update_user_when_put_valid_data() throws Exception {
        // Arrange
        Long userId = 1L;
        UserDTO updateDTO = UserDTO.builder()
            .id(userId)
            .email("updated@example.com")
            .phone("13800138000")
            .build();
        
        UserDTO updatedDTO = UserDTO.builder()
            .id(userId)
            .username("testuser")
            .email("updated@example.com")
            .phone("13800138000")
            .build();
        
        when(userService.updateUser(eq(userId), any(UserDTO.class))).thenReturn(updatedDTO);
        
        // Act & Assert
        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.email").value("updated@example.com"))
            .andExpect(jsonPath("$.data.phone").value("13800138000"));
    }
    
    @Test
    @DisplayName("删除用户 - 成功")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void should_delete_user_when_delete_by_id() throws Exception {
        // Arrange
        Long userId = 1L;
        doNothing().when(userService).deleteUser(userId);
        
        // Act & Assert
        mockMvc.perform(delete("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
        
        verify(userService, times(1)).deleteUser(userId);
    }
}
```

### 工具类测试

```java
package com.library.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Redis 分布式锁测试")
class RedisLockTest {
    
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @InjectMocks
    private RedisLock redisLock;
    
    @Test
    @DisplayName("获取锁 - 成功")
    void should_acquire_lock_successfully() {
        // Arrange
        String lockKey = "test:lock:key";
        String requestId = "unique-request-id";
        long expireTime = 10;
        
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq(lockKey), anyString(), eq(expireTime), eq(TimeUnit.SECONDS)))
            .thenReturn(true);
        
        // Act
        RedisLock.LockResult result = redisLock.tryLock(lockKey, 3, expireTime, TimeUnit.SECONDS);
        
        // Assert
        assertTrue(result.isLocked());
        assertNotNull(result.getLockKey());
        assertNotNull(result.getRequestId());
    }
    
    @Test
    @DisplayName("获取锁 - 失败（已被占用）")
    void should_fail_to_acquire_lock_when_already_held() {
        // Arrange
        String lockKey = "test:lock:key";
        
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(false);
        
        // Act
        RedisLock.LockResult result = redisLock.tryLock(lockKey, 3, 10, TimeUnit.SECONDS);
        
        // Assert
        assertFalse(result.isLocked());
    }
    
    @Test
    @DisplayName("释放锁 - 成功")
    void should_release_lock_successfully() {
        // Arrange
        String lockKey = "test:lock:key";
        String requestId = "unique-request-id";
        
        RedisLock.LockResult lockResult = new RedisLock.LockResult(lockKey, requestId, true);
        
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenReturn(requestId);
        when(stringRedisTemplate.delete(lockKey)).thenReturn(true);
        
        // Act
        boolean result = redisLock.unlock(lockResult);
        
        // Assert
        assertTrue(result);
        verify(stringRedisTemplate, times(1)).delete(lockKey);
    }
    
    @Test
    @DisplayName("释放锁 - 失败（锁已过期）")
    void should_fail_to_release_lock_when_expired() {
        // Arrange
        String lockKey = "test:lock:key";
        String originalRequestId = "original-id";
        String currentValue = "another-id";
        
        RedisLock.LockResult lockResult = new RedisLock.LockResult(lockKey, originalRequestId, true);
        
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(lockKey)).thenReturn(currentValue);
        
        // Act
        boolean result = redisLock.unlock(lockResult);
        
        // Assert
        assertFalse(result);
        verify(stringRedisTemplate, never()).delete(anyString());
    }
}
```

---

## 集成测试

### 使用 Testcontainers

```java
package com.library.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RedisContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class BaseIntegrationTest {
    
    @Container
    static final MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
        .withDatabaseName("test_library")
        .withUsername("test")
        .withPassword("test123");
    
    @Container
    static final RedisContainer<?> redis = new RedisContainer<>(DockerImageName.parse("redis:7-alpine"));
    
    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        // MySQL
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        
        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
    
    @BeforeAll
    static void startContainers() {
        mysql.start();
        redis.start();
    }
    
    @AfterAll
    static void stopContainers() {
        mysql.stop();
        redis.stop();
    }
}
```

### 用户服务集成测试

```java
package com.library.user;

import com.library.integration.BaseIntegrationTest;
import com.library.user.entity.User;
import com.library.user.mapper.UserMapper;
import com.library.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("用户服务集成测试")
class UserServiceIntegrationTest extends BaseIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Test
    @Transactional
    @DisplayName("创建用户并查询 - 集成测试")
    void should_create_and_retrieve_user() {
        // Arrange
        User user = User.builder()
            .username("integration_test")
            .password(passwordEncoder.encode("password123"))
            .email("integration@test.com")
            .phone("13900139000")
            .role("reader")
            .status("active")
            .build();
        
        userMapper.insert(user);
        assertNotNull(user.getId());
        
        // Act
        var found = userService.getUserById(user.getId());
        
        // Assert
        assertNotNull(found);
        assertEquals("integration_test", found.getUsername());
        assertEquals("integration@test.com", found.getEmail());
    }
    
    @Test
    @Transactional
    @DisplayName("更新用户 - 集成测试")
    void should_update_user_info() {
        // Arrange
        User user = User.builder()
            .username("update_test")
            .password(passwordEncoder.encode("password123"))
            .email("old@email.com")
            .role("reader")
            .status("active")
            .build();
        
        userMapper.insert(user);
        
        // Act
        user.setEmail("new@email.com");
        user.setPhone("13800138000");
        userMapper.updateById(user);
        
        // Assert
        var updated = userMapper.selectById(user.getId());
        assertEquals("new@email.com", updated.getEmail());
        assertEquals("13800138000", updated.getPhone());
    }
    
    @Test
    @Transactional
    @DisplayName("逻辑删除用户 - 集成测试")
    void should_soft_delete_user() {
        // Arrange
        User user = User.builder()
            .username("delete_test")
            .password(passwordEncoder.encode("password123"))
            .email("delete@test.com")
            .role("reader")
            .status("active")
            .deleted(0)
            .build();
        
        userMapper.insert(user);
        
        // Act
        userService.deleteUser(user.getId());
        
        // Assert
        var deleted = userMapper.selectById(user.getId());
        assertEquals(1, deleted.getDeleted());
        
        // 验证通过逻辑删除条件查询不到
        var activeUsers = userMapper.selectList(
            com.baomidou.mybatisplus.core.toolkit.Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, "delete_test")
                .eq(User::getDeleted, 0)
        );
        assertTrue(activeUsers.isEmpty());
    }
}
```

---

## 接口测试

### REST Assured 测试

```java
package com.library.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("API 接口测试")
class ApiIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    private static String adminToken;
    private static String readerToken;
    
    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "/api";
    }
    
    @Test
    @Order(1)
    @DisplayName("管理员登录 - 获取 Token")
    void should_login_as_admin() {
        // Arrange
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "admin");
        loginRequest.put("password", "123456");
        
        // Act
        Response response = given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .when()
            .post("/auth/login");
        
        // Assert
        response.then()
            .statusCode(200)
            .body("code", equalTo(200))
            .body("data.accessToken", notNullValue())
            .body("data.user.role", equalTo("admin"));
        
        adminToken = response.jsonPath().getString("data.accessToken");
    }
    
    @Test
    @Order(2)
    @DisplayName("获取用户列表 - 需要管理员权限")
    void should_get_user_list_with_admin_token() {
        given()
            .header("Authorization", "Bearer " + adminToken)
            .contentType(ContentType.JSON)
            .param("page", 1)
            .param("size", 10)
            .when()
            .get("/users")
            .then()
            .statusCode(200)
            .body("code", equalTo(200))
            .body("data.list", notNullValue());
    }
    
    @Test
    @Order(3)
    @DisplayName("获取用户列表 - 无 Token 返回 401")
    void should_return_401_when_no_token() {
        given()
            .contentType(ContentType.JSON)
            .param("page", 1)
            .param("size", 10)
            .when()
            .get("/users")
            .then()
            .statusCode(401);
    }
    
    @Test
    @Order(4)
    @DisplayName("获取图书列表 - 无需登录")
    void should_get_book_list_without_auth() {
        given()
            .contentType(ContentType.JSON)
            .param("page", 1)
            .param("size", 10)
            .when()
            .get("/books")
            .then()
            .statusCode(200)
            .body("code", equalTo(200))
            .body("data.list", notNullValue());
    }
    
    @Test
    @Order(5)
    @DisplayName("搜索图书 - 无需登录")
    void should_search_books() {
        given()
            .contentType(ContentType.JSON)
            .param("keyword", "Java")
            .param("page", 1)
            .param("size", 10)
            .when()
            .get("/search")
            .then()
            .statusCode(200)
            .body("code", equalTo(200));
    }
    
    @Test
    @Order(6)
    @DisplayName("登录失败 - 密码错误")
    void should_fail_login_with_wrong_password() {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "admin");
        loginRequest.put("password", "wrongpassword");
        
        given()
            .contentType(ContentType.JSON)
            .body(loginRequest)
            .when()
            .post("/auth/login")
            .then()
            .statusCode(200)
            .body("code", equalTo(401))
            .body("message", containsString("密码"));
    }
}
```

### Postman 测试集合

创建 `tests/postman/Library Management API.postman_collection.json`：

```json
{
  "info": {
    "name": "Library Management API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Login",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\"username\":\"admin\",\"password\":\"123456\"}"
            },
            "url": {
              "raw": "{{baseUrl}}/api/auth/login",
              "host": ["{{baseUrl}}"],
              "path": ["api", "auth", "login"]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test(\"Login successful\", function() {",
                  "    pm.response.to.have.status(200);",
                  "    var jsonData = pm.response.json();",
                  "    pm.expect(jsonData.code).to.eql(200);",
                  "    pm.environment.set(\"accessToken\", jsonData.data.accessToken);",
                  "});"
                ]
              }
            }
          ]
        }
      ]
    },
    {
      "name": "Books",
      "item": [
        {
          "name": "Get Book List",
          "request": {
            "method": "GET",
            "header": [],
            "url": {
              "raw": "{{baseUrl}}/api/books?page=1&size=10",
              "host": ["{{baseUrl}}"],
              "path": ["api", "books"],
              "query": [
                { "key": "page", "value": "1" },
                { "key": "size", "value": "10" }
              ]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test(\"Get book list\", function() {",
                  "    pm.response.to.have.status(200);",
                  "    var jsonData = pm.response.json();",
                  "    pm.expect(jsonData.code).to.eql(200);",
                  "    pm.expect(jsonData.data).to.be.an('object');",
                  "});"
                ]
              }
            }
          ]
        }
      ]
    }
  ],
  "event": [
    {
      "listen": "prerequest",
      "script": {
        "type": "text/javascript",
        "exec": [""]
      }
    },
    {
      "listen": "test",
      "script": {
        "type": "text/javascript",
        "exec": [""]
      }
    }
  ],
  "variable": [
    { "key": "baseUrl", "value": "http://localhost:8080", "type": "string" }
  ]
}
```

---

## 性能测试

### JMeter 测试计划

创建 `tests/jmeter/library-api-test-plan.jmx`

### 测试场景

| 场景 | 并发用户 | 持续时间 | 目标 QPS |
|------|----------|----------|----------|
| 图书列表查询 | 100 | 5分钟 | >= 500 |
| 用户登录 | 50 | 5分钟 | >= 200 |
| 借书操作 | 30 | 5分钟 | >= 100 |
| 混合场景 | 200 | 10分钟 | >= 300 |

### 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 响应时间 (P95) | < 200ms | 95% 请求响应时间 |
| 响应时间 (P99) | < 500ms | 99% 请求响应时间 |
| 错误率 | < 1% | 请求失败率 |
| 吞吐量 | 根据场景 | 每秒处理请求数 |

---

## 安全测试

### 测试项清单

- [ ] SQL 注入测试
- [ ] XSS 攻击测试
- [ ] CSRF 攻击测试
- [ ] 越权访问测试
- [ ] 接口暴力破解测试
- [ ] 敏感信息泄露测试
- [ ] JWT Token 安全测试

### OWASP ZAP 测试

使用 OWASP ZAP 进行自动化安全扫描：

```bash
# 启动 ZAP 代理进行扫描
zap.sh -daemon -port 8090 -config api.addrs.addr.name=.* -config api.addrs.addr.regex=true

# 运行扫描
curl "http://localhost:8090/JSON/ascan/action/scan/?url=http://localhost:8080"
```

---

## 测试覆盖率

### 生成覆盖率报告

```bash
# 使用 JaCoCo 生成覆盖率报告
mvn clean test jacoco:report

# 报告位置
# backend/*/target/site/jacoco/index.html
```

### 覆盖率检查

```xml
<!-- pom.xml 配置 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## 测试报告

### 测试执行报告模板

```markdown
# 测试执行报告

## 基本信息

- 测试日期：2024-01-15
- 测试版本：v1.0.0
- 测试环境：测试环境

## 测试执行情况

| 测试类型 | 计划数 | 执行数 | 通过数 | 失败数 | 通过率 |
|----------|--------|--------|--------|--------|--------|
| 单元测试 | 120 | 120 | 118 | 2 | 98.3% |
| 集成测试 | 35 | 35 | 35 | 0 | 100% |
| 接口测试 | 45 | 45 | 43 | 2 | 95.6% |
| 性能测试 | 5 | 5 | 4 | 1 | 80% |
| **合计** | **205** | **205** | **200** | **5** | **97.6%** |

## 测试覆盖率

- 行覆盖率：85.2%
- 分支覆盖率：72.5%
- 方法覆盖率：91.3%

## 发现的问题

### 严重问题

| ID | 问题描述 | 影响 | 状态 |
|----|----------|------|------|
| BUG-001 | 借书接口并发场景下可能超借 | 数据一致性 | 已修复 |
| BUG-002 | JWT Token 过期后仍可使用 | 安全 | 已修复 |

### 一般问题

| ID | 问题描述 | 影响 | 状态 |
|----|----------|------|------|
| BUG-003 | 图书搜索结果排序不稳定性 | 用户体验 | 待修复 |
| BUG-004 | 部分接口缺少参数校验 | 鲁棒性 | 待修复 |

## 测试结论

### 通过标准

- 严重问题 0 个
- 一般问题 <= 5 个
- 通过率 >= 95%
- 覆盖率 >= 80%

### 结论

✅ **测试通过，可以发布**
```

---

## 附录

### A. 测试数据准备

```sql
-- 初始化测试用户
INSERT INTO `user` (`username`, `password`, `email`, `phone`, `role`, `status`) VALUES
('admin', '$2a$10$...', 'admin@example.com', '13800138000', 'admin', 'active'),
('test_reader', '$2a$10$...', 'reader@example.com', '13900139000', 'reader', 'active');

-- 初始化测试图书
INSERT INTO `book_info` (`title`, `author`, `isbn`, `publisher`, `category_id`, `total_copies`, `available_copies`, `status`) VALUES
('Java编程思想', 'Bruce Eckel', '9787111213826', '机械工业出版社', 1, 10, 10, 'available'),
('深入理解Java虚拟机', '周志明', '9787111421900', '机械工业出版社', 1, 5, 5, 'available');
```

### B. 常用测试命令

```bash
# 运行所有测试
mvn test

# 运行指定测试类
mvn test -Dtest=UserServiceTest

# 运行指定测试方法
mvn test -Dtest=UserServiceTest#should_return_user_when_get_by_id

# 跳过测试编译
mvn package -DskipTests

# 生成覆盖率报告
mvn clean test jacoco:report

# 运行集成测试
mvn test -Dtest=*IntegrationTest

# 运行性能测试（JMeter）
jmeter -n -t tests/jmeter/library-api-test-plan.jmx -l results.jtl
```

### C. 测试工具推荐

| 工具 | 用途 | 链接 |
|------|------|------|
| JUnit 5 | 单元测试框架 | https://junit.org/junit5/ |
| Mockito | Mock 框架 | https://site.mockito.org/ |
| AssertJ | 断言库 | https://assertj.github.io/doc/ |
| Testcontainers | 容器化测试 | https://testcontainers.com/ |
| REST Assured | API 测试 | https://rest-assured.io/ |
| JMeter | 性能测试 | https://jmeter.apache.org/ |
| OWASP ZAP | 安全测试 | https://www.zaproxy.org/ |
| Postman | API 测试工具 | https://www.postman.com/ |

---

本文档解释权归测试团队所有。
