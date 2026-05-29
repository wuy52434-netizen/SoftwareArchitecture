# 开发规范文档

本文档规定了图书管理系统的开发规范，所有开发人员必须遵守。

---

## 目录

- [环境配置](#环境配置)
- [代码规范](#代码规范)
- [命名规范](#命名规范)
- [架构规范](#架构规范)
- [接口规范](#接口规范)
- [数据库规范](#数据库规范)
- [Git 提交规范](#git-提交规范)
- [代码审查规范](#代码审查规范)
- [安全规范](#安全规范)

---

## 环境配置

### 开发环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java 开发环境 |
| Maven | 3.8+ | 项目构建工具 |
| Node.js | 18+ | 前端开发环境 |
| npm/yarn | - | 前端包管理 |
| Docker | 20.10+ | 容器化部署 |
| Docker Compose | 2.0+ | 容器编排 |

### IDE 配置

#### IntelliJ IDEA 推荐插件

| 插件名称 | 用途 |
|----------|------|
| Lombok | 简化 Java 代码 |
| Maven Helper | Maven 依赖分析 |
| Alibaba Java Coding Guidelines | 阿里巴巴代码规范检查 |
| SonarLint | 代码质量分析 |
| MyBatisX | MyBatis 代码生成 |
| GitToolBox | Git 增强工具 |

#### 代码格式化配置

在项目根目录添加 `.editorconfig` 文件：

```ini
root = true

[*]
charset = utf-8
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true
max_line_length = 120

[*.xml]
indent_size = 2

[*.yml, *.yaml]
indent_size = 2

[*.json]
indent_size = 2

[*.js, *.ts, *.vue]
indent_size = 2

[*.md]
max_line_length = off
trim_trailing_whitespace = false
```

### Maven 镜像配置

编辑 `~/.m2/settings.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
  <profiles>
    <profile>
      <id>jdk-17</id>
      <activation>
        <activeByDefault>true</activeByDefault>
        <jdk>17</jdk>
      </activation>
      <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <maven.compiler.compilerVersion>17</maven.compiler.compilerVersion>
      </properties>
    </profile>
  </profiles>
</settings>
```

### 本地开发环境搭建

#### 1. 安装依赖软件

按照 [部署文档](./DEPLOYMENT.md) 中的要求安装所需软件。

#### 2. 启动中间件

```bash
cd docker

# 启动必要的中间件
docker-compose up -d mysql redis rabbitmq elasticsearch nacos

# 等待初始化完成（建议等待 2-3 分钟）
```

#### 3. 编译项目

**方式 A：命令行编译**

```bash
cd backend

# 编译所有模块
mvn clean install -DskipTests
```

**方式 B：IDEA Maven 插件编译（推荐）**

1. 用 IDEA 打开 `backend` 目录作为 Maven 项目（File → Open）
2. 等待 IDEA 自动下载依赖完成（右下角进度条）
3. 打开右侧 **Maven** 工具窗口（View → Tool Windows → Maven）
4. 展开 **library-management (root)** → **Lifecycle**
5. 双击 **clean**，等待执行完成
6. 双击 **install**（或 **package**，效果同 `mvn clean install -DskipTests`）
7. 跳过测试：点击 Maven 窗口工具栏的 ⚡ 闪电图标（Toggle 'Skip Tests' Mode），开启后所有 Lifecycle 操作自动跳过测试

> 也可以直接使用 IDEA 菜单：**Build → Build Project** (Ctrl+F9) 仅编译不打包。

#### 4. 运行服务

**方式 A：IDEA 中运行（推荐）**

1. 在 IDEA 中找到各模块的 `Application.java` 主类
2. 右键主类 → Run，或点击 `main` 方法旁的 ▶ 绿色三角
3. 按以下顺序启动：
   - `UserServiceApplication` (端口 8081)
   - `BookServiceApplication` (端口 8082)
   - `BorrowServiceApplication` (端口 8083)
   - `SearchServiceApplication` (端口 8084)
   - `NotifyServiceApplication` (端口 8085)
   - `StatsServiceApplication` (端口 8086)
   - `ApiGatewayApplication` (端口 8080)

> **批量管理**：View → Tool Windows → Services，点击 + → Run Configuration Type → Spring Boot，可同时查看和管理所有服务的运行状态。

**方式 B：命令行运行**

按以下顺序启动各服务：

```bash
# 1. 用户服务
cd user-service
mvn spring-boot:run

# 2. 图书服务
cd book-service
mvn spring-boot:run

# 3. 借阅服务
cd borrow-service
mvn spring-boot:run

# 4. 其他服务...

# 5. API 网关（最后启动）
cd api-gateway
mvn spring-boot:run
```

#### 5. 启动前端

```bash
cd frontend

npm install
npm run dev
```

---

## 代码规范

### Java 代码规范

#### 1. 类和方法

```java
// 类名使用大驼峰命名
public class UserService {
    
    // 方法名使用小驼峰命名
    public UserDTO getUserById(Long id) {
        // 方法体
    }
}
```

#### 2. 常量和变量

```java
// 常量使用大写字母加下划线
public static final String DEFAULT_STATUS = "active";

// 成员变量使用小驼峰
private String userName;

// 局部变量使用小驼峰
int borrowCount = 0;
```

#### 3. 注释规范

```java
/**
 * 用户服务接口
 * 提供用户相关的业务逻辑处理
 * 
 * @author team-name
 * @version 1.0.0
 * @since 2024-01-01
 */
public interface UserService {
    
    /**
     * 根据用户ID获取用户信息
     * 
     * @param id 用户ID，不能为空
     * @return 用户信息DTO
     * @throws BusinessException 当用户不存在时抛出
     */
    UserDTO getUserById(Long id);
}
```

#### 4. 代码格式

- 使用 4 个空格缩进
- 大括号不换行（K&R 风格）
- 方法之间空一行
- 超过 120 字符换行

```java
public void exampleMethod(String param1, String param2, String param3, 
                          String param4, String param5) {
    
    if (condition1 && condition2 
            && condition3) {
        // 逻辑处理
    }
}
```

#### 5. 异常处理

```java
// 推荐：使用统一异常处理
public UserDTO getUserById(Long id) {
    User user = userMapper.selectById(id);
    if (user == null) {
        throw new BusinessException(ResultCode.USER_NOT_FOUND);
    }
    return convertToDTO(user);
}

// 不推荐：直接捕获 Exception
try {
    // 业务逻辑
} catch (Exception e) {
    log.error("错误", e);
}
```

#### 6. 使用 Lombok

```java
// 推荐：使用 Lombok 简化代码
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;
}

// 不推荐：手动编写 getter/setter
public class UserDTO {
    private Long id;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
}
```

### 前端代码规范

#### 1. Vue 组件规范

```vue
<template>
  <div class="user-card">
    <!-- 模板内容 -->
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  user: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['delete', 'edit'])

const router = useRouter()
const isLoading = ref(false)

const displayName = computed(() => {
  return props.user.name || props.user.username
})

const handleDelete = () => {
  emit('delete', props.user.id)
}
</script>

<style scoped>
.user-card {
  padding: 16px;
}
</style>
```

#### 2. 组件命名

```vue
<!-- 文件名：UserProfile.vue -->
<!-- 组件名：大驼峰 -->

<!-- 使用时：kebab-case 或 大驼峰 -->
<template>
  <UserProfile />
  <!-- 或 -->
  <user-profile />
</template>
```

#### 3. API 调用规范

```javascript
// api/books.js
import request from './index'

export function getBookList(params) {
  return request({
    url: '/api/books',
    method: 'get',
    params
  })
}

export function borrowBook(data) {
  return request({
    url: '/api/borrow',
    method: 'post',
    data
  })
}

// 使用示例
import { getBookList, borrowBook } from '@/api/books'

const loadBooks = async () => {
  try {
    const data = await getBookList({ page: 1, size: 10 })
    // 处理数据
  } catch (error) {
    // 错误处理
  }
}
```

#### 4. 样式规范

```css
/* 使用 BEM 命名规范 */
.user-card { }
.user-card__header { }
.user-card__body { }
.user-card--active { }

/* 不使用 ID 选择器 */
#header { }  /* 不推荐 */

/* 使用 scoped 避免样式污染 */
<style scoped>
.component-class { }
</style>
```

---

## 命名规范

### 包名规范

```
com.library
├── common              # 公共模块
│   ├── constant        # 常量
│   ├── exception       # 异常
│   ├── result          # 返回结果
│   └── util            # 工具类
├── user                # 用户服务
│   ├── controller      # 控制器
│   ├── service         # 业务逻辑
│   ├── service.impl    # 业务实现
│   ├── mapper          # 数据访问
│   ├── entity          # 实体类
│   ├── dto             # 数据传输对象
│   ├── vo              # 视图对象
│   ├── converter       # 转换器
│   └── config          # 配置类
└── gateway             # 网关服务
    ├── filter          # 过滤器
    └── config          # 配置类
```

### 类名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 控制器 | XxxController | UserController |
| 服务接口 | XxxService | UserService |
| 服务实现 | XxxServiceImpl | UserServiceImpl |
| Mapper | XxxMapper | UserMapper |
| 实体类 | Xxx | User, BookInfo |
| DTO | XxxDTO | UserDTO, BorrowDTO |
| VO | XxxVO | UserVO |
| 转换器 | XxxConverter | UserConverter |
| 配置类 | XxxConfig | SecurityConfig |
| 工具类 | XxxUtil | JwtUtil |
| 异常类 | XxxException | BusinessException |
| 过滤器 | XxxFilter | JwtAuthenticationFilter |
| 拦截器 | XxxInterceptor | LoginInterceptor |

### 方法名规范

| 操作 | 规范 | 示例 |
|------|------|------|
| 查询单个 | getXxxById, findXxx | getUserById |
| 查询列表 | listXxx, getXxxList | listUsers |
| 分页查询 | pageXxx | pageUsers |
| 新增 | createXxx, addXxx, saveXxx | createUser |
| 修改 | updateXxx | updateUser |
| 删除 | deleteXxx, removeXxx | deleteUser |
| 批量操作 | batchXxx | batchDeleteUsers |
| 统计 | countXxx | countActiveUsers |
| 检查 | checkXxx, validateXxx | checkUserStatus |

### 数据库命名规范

#### 表名

- 使用小写字母和下划线
- 不使用复数形式
- 不使用保留字

```sql
-- 推荐
CREATE TABLE user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50)
);

CREATE TABLE book_info (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200)
);

-- 不推荐
CREATE TABLE users ();
CREATE TABLE UserInfo ();
CREATE TABLE `order` ();  -- order 是保留字
```

#### 字段名

- 使用小写字母和下划线
- 主键使用 `id`
- 外键使用 `关联表名_id`
- 逻辑删除字段使用 `deleted`
- 时间字段使用 `created_at`, `updated_at`

```sql
-- 推荐
CREATE TABLE borrow_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '图书ID',
    borrow_date DATE COMMENT '借阅日期',
    due_date DATE COMMENT '到期日期',
    return_date DATE COMMENT '归还日期',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
);
```

#### 索引命名

```sql
-- 普通索引
CREATE INDEX idx_user_username ON user(username);

-- 唯一索引
CREATE UNIQUE INDEX uk_user_username ON user(username);

-- 联合索引
CREATE INDEX idx_borrow_user_book ON borrow_record(user_id, book_id);

-- 外键约束
ALTER TABLE borrow_record 
ADD CONSTRAINT fk_borrow_user 
FOREIGN KEY (user_id) REFERENCES user(id);
```

### 常量命名

```java
// 系统常量
public interface Constants {
    // 状态常量
    String STATUS_ACTIVE = "active";
    String STATUS_INACTIVE = "inactive";
    
    // Redis Key 前缀
    String REDIS_KEY_USER = "user:";
    String REDIS_KEY_BOOK = "book:";
    
    // 默认值
    int DEFAULT_PAGE_SIZE = 10;
    int MAX_PAGE_SIZE = 100;
    int DEFAULT_BORROW_DAYS = 30;
}
```

---

## 架构规范

### 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                      表现层 (Controller)                       │
│  处理 HTTP 请求、参数校验、响应封装                             │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                      业务层 (Service)                          │
│  业务逻辑处理、事务管理、数据转换                               │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                      数据层 (Mapper/DAO)                      │
│  数据库访问、SQL 执行                                          │
└───────────────────────────┬─────────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────────┐
│                         数据库 (MySQL)                         │
└─────────────────────────────────────────────────────────────┘
```

### 各层职责

#### Controller 层

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        // 1. 参数校验
        if (id == null || id <= 0) {
            return Result.error(ResultCode.PARAM_ERROR);
        }
        
        // 2. 调用 Service
        UserDTO userDTO = userService.getUserById(id);
        
        // 3. 转换为 VO 返回
        return Result.success(UserConverter.toVO(userDTO));
    }
}
```

#### Service 层

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserMapper userMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO createUser(UserDTO userDTO) {
        // 1. 业务校验
        if (userMapper.existsByUsername(userDTO.getUsername())) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        
        // 2. 数据转换
        User user = UserConverter.toEntity(userDTO);
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        
        // 3. 数据操作
        userMapper.insert(user);
        
        // 4. 返回结果
        return UserConverter.toDTO(user);
    }
}
```

#### Mapper 层

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);
    
    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(1) FROM user WHERE username = #{username} AND deleted = 0")
    boolean existsByUsername(@Param("username") String username);
    
    /**
     * 分页查询用户列表
     */
    Page<User> selectPageWithConditions(Page<User> page, 
                                        @Param("keyword") String keyword,
                                        @Param("status") String status);
}
```

### 数据对象规范

| 对象类型 | 用途 | 示例 |
|----------|------|------|
| Entity | 数据库实体，与表结构对应 | User, BookInfo |
| DTO | 数据传输对象，用于服务间调用 | UserDTO, BorrowDTO |
| VO | 视图对象，用于前端展示 | UserVO, BookListVO |
| Request | 请求参数对象 | LoginRequest, BorrowRequest |
| Response | 响应结果对象 | LoginResponse, BorrowResponse |

### 转换器规范

```java
@Component
public class UserConverter {
    
    public static UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        
        return UserDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole())
            .status(user.getStatus())
            .build();
    }
    
    public static UserVO toVO(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        
        UserVO vo = new UserVO();
        vo.setId(userDTO.getId());
        vo.setUsername(userDTO.getUsername());
        vo.setEmail(userDTO.getEmail());
        return vo;
    }
    
    public static User toEntity(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        
        User user = new User();
        user.setId(userDTO.getId());
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        return user;
    }
}
```

---

## 接口规范

### RESTful 风格

| 操作 | HTTP 方法 | URL 示例 | 说明 |
|------|-----------|----------|------|
| 查询列表 | GET | /api/users | 获取用户列表 |
| 查询单个 | GET | /api/users/{id} | 获取单个用户 |
| 新增 | POST | /api/users | 创建用户 |
| 全量更新 | PUT | /api/users/{id} | 更新用户 |
| 部分更新 | PATCH | /api/users/{id} | 部分更新用户 |
| 删除 | DELETE | /api/users/{id} | 删除用户 |

### URL 设计

```
# 资源路径
/api/users           # 用户资源
/api/books           # 图书资源
/api/borrows         # 借阅资源
/api/borrow-records  # 借阅记录资源

# 子资源
/api/users/{id}/borrows  # 用户的借阅记录
/api/books/{id}/copies   # 图书的副本列表

# 动作路径
/api/auth/login           # 登录
/api/auth/logout          # 登出
/api/borrow               # 借书
/api/return               # 还书
```

### 请求参数

#### 路径参数

```
GET /api/users/{id}
GET /api/books/{bookId}/copies/{copyId}
```

#### 查询参数

```
GET /api/users?page=1&size=10&keyword=admin&status=active
```

#### 请求体

```json
POST /api/users
Content-Type: application/json

{
  "username": "newuser",
  "password": "123456",
  "email": "user@example.com"
}
```

### 响应格式

#### 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin"
  }
}
```

#### 分页响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      { "id": 1, "username": "admin" },
      { "id": 2, "username": "user" }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

#### 错误响应

```json
{
  "code": 400,
  "message": "用户名已存在",
  "data": null
}
```

### 状态码规范

#### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

#### 业务错误码

```java
public enum ResultCode {
    
    SUCCESS(200, "操作成功"),
    FAILED(500, "操作失败"),
    
    // 参数错误
    PARAM_ERROR(400, "参数错误"),
    PARAM_IS_NULL(400, "参数为空"),
    
    // 用户相关
    USER_NOT_FOUND(404, "用户不存在"),
    USERNAME_EXISTS(400, "用户名已存在"),
    USERNAME_OR_PASSWORD_ERROR(401, "用户名或密码错误"),
    
    // 图书相关
    BOOK_NOT_FOUND(404, "图书不存在"),
    BOOK_NOT_AVAILABLE(400, "图书不可借"),
    
    // 借阅相关
    BORROW_NOT_FOUND(404, "借阅记录不存在"),
    BORROW_LIMIT_EXCEEDED(400, "借阅数量已达上限"),
    ALREADY_BORROWED(400, "已借阅该图书");
    
    // ...
}
```

---

## 数据库规范

### 建表规范

```sql
CREATE TABLE IF NOT EXISTS `table_name` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    
    -- 业务字段
    `column1` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '字段1',
    `column2` INT NOT NULL DEFAULT 0 COMMENT '字段2',
    `column3` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '字段3',
    
    -- 公共字段
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_column1` (`column1`),
    KEY `idx_column2` (`column2`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表注释';
```

### 字段类型规范

| 数据类型 | 推荐类型 | 示例 |
|----------|----------|------|
| 主键 | BIGINT UNSIGNED AUTO_INCREMENT | id |
| 金额 | DECIMAL(10, 2) | price, amount |
| 短文本 | VARCHAR(50-255) | username, title |
| 长文本 | TEXT | description, summary |
| 枚举值 | VARCHAR(20) | status, type |
| 日期 | DATE | borrow_date, due_date |
| 日期时间 | DATETIME | created_at, updated_at |
| 布尔值 | TINYINT | deleted, is_admin |

### 索引规范

```sql
-- 主键索引：自动创建
PRIMARY KEY (`id`)

-- 唯一索引：用于唯一约束
UNIQUE KEY `uk_username` (`username`)

-- 普通索引：用于查询条件
KEY `idx_user_id` (`user_id`)
KEY `idx_book_id` (`book_id`)

-- 联合索引：用于多条件查询
KEY `idx_user_book` (`user_id`, `book_id`)
KEY `idx_borrow_date` (`borrow_date`, `status`)
```

### 逻辑删除

```java
// 实体类配置
@Data
@TableName("user")
public class User {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String username;
    
    @TableLogic
    private Integer deleted;
}
```

```sql
-- 查询时自动添加条件
SELECT * FROM user WHERE deleted = 0

-- 删除时更新字段
UPDATE user SET deleted = 1 WHERE id = 1
```

---

## Git 提交规范

### 提交格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

| 类型 | 说明 |
|------|------|
| feat | 新增功能 |
| fix | 修复 bug |
| docs | 文档更新 |
| style | 代码格式（不影响功能） |
| refactor | 重构 |
| perf | 性能优化 |
| test | 测试相关 |
| chore | 构建/工具相关 |
| revert | 回滚 |

### Scope 范围

- `user` - 用户服务
- `book` - 图书服务
- `borrow` - 借阅服务
- `gateway` - API 网关
- `common` - 公共模块
- `frontend` - 前端
- `docker` - Docker 相关
- `*` - 其他

### Subject 主题

- 使用祈使语气，如 "add" 而非 "added"
- 首字母小写
- 结尾不加句号
- 不超过 50 字符

### 提交示例

```bash
# 新增功能
git commit -m "feat(user): 添加用户注册功能"

# 修复 bug
git commit -m "fix(borrow): 修复借书时库存扣减问题"

# 文档更新
git commit -m "docs: 更新 README 部署说明"

# 代码格式
git commit -m "style(frontend): 格式化代码"

# 重构
git commit -m "refactor(common): 重构 Result 统一返回类"

# 性能优化
git commit -m "perf(book): 添加图书列表缓存"

# 测试
git commit -m "test(user): 添加用户服务单元测试"

# 构建相关
git commit -m "chore(docker): 添加 Redis 配置文件"
```

### 分支管理

```
main
  ├── develop
  │     ├── feature/user-login
  │     ├── feature/book-search
  │     ├── fix/borrow-stock
  │     └── ...
  ├── release/v1.0.0
  └── hotfix/login-bug
```

| 分支 | 说明 |
|------|------|
| main | 主分支，生产环境代码 |
| develop | 开发分支 |
| feature/* | 功能分支 |
| fix/* | 修复分支 |
| release/* | 发布分支 |
| hotfix/* | 紧急修复分支 |

---

## 代码审查规范

### PR (Pull Request) 规范

1. **标题格式**：`<type>(<scope>): <description>`
2. **描述内容**：
   - 变更内容
   - 变更原因
   - 测试情况
   - 相关 Issue
3. **代码检查**：
   - 是否符合代码规范
   - 是否有足够的测试
   - 是否有安全问题
   - 是否有性能问题

### 审查清单

- [ ] 代码符合项目规范
- [ ] 命名清晰有意义
- [ ] 没有重复代码
- [ ] 没有硬编码的敏感信息
- [ ] 有适当的注释
- [ ] 有必要的错误处理
- [ ] 没有明显的性能问题
- [ ] 有相关的单元测试
- [ ] 测试通过

### 合并规范

- 至少 1 人审核通过
- 所有检查通过
- 无冲突
- 合并后删除源分支

---

## 安全规范

### 认证安全

```java
// 密码加密
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
String encodedPassword = encoder.encode(rawPassword);

// 密码验证
boolean matches = encoder.matches(rawPassword, encodedPassword);
```

```java
// JWT Token 生成
String token = Jwts.builder()
    .setSubject(username)
    .claim("role", role)
    .setIssuedAt(new Date())
    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
    .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
    .compact();
```

### 接口安全

```java
// 接口权限控制
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public Result<Void> deleteUser(@PathVariable Long id) {
    // 只有管理员可以删除用户
}

@PreAuthorize("hasAnyRole('ADMIN', 'READER')")
@GetMapping("/my-borrows")
public Result<List<BorrowVO>> getMyBorrows() {
    // 管理员和读者都可以访问
}
```

### SQL 注入防护

```java
// 推荐：使用 MyBatis-Plus 或参数绑定
@Select("SELECT * FROM user WHERE username = #{username}")
User selectByUsername(@Param("username") String username);

// 不推荐：字符串拼接
@Select("SELECT * FROM user WHERE username = '" + username + "'")
User selectByUsername(String username);
```

### XSS 防护

```javascript
// 前端使用 v-html 时要确保内容安全
<template>
  <div v-html="sanitizeHtml(content)"></div>
</template>

<script setup>
import DOMPurify from 'dompurify'

const sanitizeHtml = (html) => {
  return DOMPurify.sanitize(html)
}
</script>
```

### 敏感信息处理

```java
// 日志中不打印敏感信息
log.info("用户登录: username={}", username);
// 不打印: log.info("用户登录: password={}", password);

// 响应中不返回敏感字段
public class UserDTO {
    private Long id;
    private String username;
    // 不包含 password 字段
}
```

### 配置文件安全

```yaml
# 推荐：使用环境变量
spring:
  datasource:
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}

# 不推荐：硬编码密码
spring:
  datasource:
    username: root
    password: my_password_123
```

### 文件上传安全

```java
// 检查文件类型
if (!ALLOWED_TYPES.contains(file.getContentType())) {
    throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的文件类型");
}

// 检查文件大小
if (file.getSize() > MAX_FILE_SIZE) {
    throw new BusinessException(ResultCode.PARAM_ERROR, "文件大小超过限制");
}

// 重命名文件，防止路径遍历
String filename = UUID.randomUUID().toString() + getExtension(file.getOriginalFilename());
```

---

## 附录

### A. 推荐阅读

- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Vue 风格指南](https://v3.cn.vuejs.org/style-guide/)
- [RESTful API 设计指南](https://restfulapi.net/)

### B. 工具链接

- [Lombok](https://projectlombok.org/)
- [MapStruct](https://mapstruct.org/)
- [MyBatis-Plus](https://baomidou.com/)
- [SonarQube](https://www.sonarqube.org/)

---

本文档解释权归开发团队所有。
