# 1112图书自动借书机 API 接口文档

## 1. 基本信息

- 项目名称：图书自动借书机（Library Management System）
- 网关入口（Nginx）：`http://localhost`
- API 前缀：`/api`
- 返回格式：`application/json`
- 认证方式：Session（登录后 Cookie）

***

## 2. 统一响应约定

### 2.1 成功示例

```json
{
  "ok": true,
  "data": {}
}
```

### 2.2 失败示例

```json
{
  "ok": false,
  "error": "错误信息"
}
```

***

## 3. 认证模块

### 3.1 用户登录

- **URL**：`POST /api/login`
- **说明**：用户登录并建立会话
- **请求体**：

```json
{
  "username": "reader01",
  "password": "123456"
}
```

- **成功响应**：

```json
{
  "ok": true,
  "user": {
    "id": 1,
    "username": "reader01",
    "role": "reader"
  }
}
```

### 3.2 用户登出

- **URL**：`POST /api/logout`
- **说明**：清理会话

### 3.3 当前用户信息

- **URL**：`GET /api/current-user`
- **说明**：获取当前登录用户信息

***

## 4. 图书模块

### 4.1 图书列表（支持搜索/分页）

- **URL**：`GET /api/books`
- **说明**：获取图书列表（已接入 Redis 缓存）
- **查询参数**：
  - `search`：关键词（书名/作者/ISBN）
  - `category`：分类
  - `year`：年份或 `older`
  - `page`：页码（默认 1）
  - `per_page`：每页条数（默认 24）

### 4.2 图书详情

- **URL**：`GET /api/books/{book_id}`
- **说明**：获取单本图书（已接入 Redis 缓存）

### 4.3 新增图书（需登录）

- **URL**：`POST /api/books`
- **请求体示例**：

```json
{
  "title": "软件架构设计",
  "author": "张三",
  "category": "计算机",
  "isbn": "9787300000000"
}
```

### 4.4 修改图书（需登录）

- **URL**：`PUT /api/books/{book_id}`

### 4.5 删除图书（需登录）

- **URL**：`DELETE /api/books/{book_id}`

### 4.6 修改图书状态（需登录）

- **URL**：`PUT /api/books/{book_id}/status`
- **请求体示例**：

```json
{
  "status": "available"
}
```

***

## 5. 借还书模块（自动借书机核心）

### 5.1 借书（需登录）

- **URL**：`POST /api/borrow`
- **说明**：
  - 借阅人强制使用当前登录用户
  - 自动分配可借副本
  - 使用 Redis 分布式锁防并发超借
  - 借阅成功后自动清理图书缓存
- **请求体示例**：

```json
{
  "book_id": 10,
  "borrow_date": "2026-04-12",
  "due_date": "2026-05-12"
}
```

- **成功响应示例**：

```json
{
  "ok": true,
  "borrow": {
    "record_id": 101,
    "status": "借出"
  },
  "book": {
    "id": 10,
    "status": "borrowed"
  }
}
```

### 5.2 还书（需登录）

- **URL**：`POST /api/return`
- **请求方式**：`book_id` 与 `borrow_id` 二选一
- **请求体示例**：

```json
{
  "borrow_id": 101
}
```

- **成功响应包含**：
  - `overdue_days`：逾期天数
  - `fine_amount`：罚款金额

### 5.3 我的借阅记录（需登录）

- **URL**：`GET /api/my-borrows`
- **查询参数**：
  - `status=all|active|returned`

### 5.4 借阅记录列表（需登录）

- **URL**：`GET /api/borrow-records`
- **可选筛选参数**：
  - `user_id`
  - `book_id`
  - `status=active|returned|all`
  - `page`
  - `per_page`

> 兼容别名：`GET /api/borrows`

***

## 6. 用户模块（需登录）

### 6.1 用户列表

- **URL**：`GET /api/users`

### 6.2 新增用户

- **URL**：`POST /api/users`
- **请求体示例**：

```json
{
  "username": "reader02",
  "password": "123456",
  "fullname": "李四",
  "role": "reader"
}
```

### 6.3 删除用户

- **URL**：`DELETE /api/users/{user_id}`

***

## 7. 系统设置模块（需登录）

### 7.1 获取系统设置

- **URL**：`GET /api/settings`

### 7.2 更新系统设置

- **URL**：`PUT /api/settings`
- **请求体示例**：

```json
{
  "default_borrow_days": "30",
  "max_borrow_count": "5",
  "max_renewal_times": "2",
  "overdue_fine_per_day": "0.5",
  "max_fine_amount": "50",
  "reminder_days_before_due": "3"
}
```

***

## 8. 健康检查

### 8.1 API 状态

- **URL**：`GET /api/status`
- **说明**：用于服务可用性检查

***

## 9. 关键技术说明（答辩可用）

1. **Nginx**：统一入口、反向代理、静态资源分发。
2. **Redis 缓存**：图书列表与详情缓存，降低数据库查询压力。
3. **Redis 分布式锁**：借书流程并发保护，避免同一本副本被重复借出。
4. **Docker Compose**：一键启动 Nginx + Flask + Redis，便于演示与部署。

***

## 10. 本地联调建议

### 10.1 一键启动

```bash
docker compose up -d --build
```

### 10.2 访问地址

- 前端：`http://localhost`
- API：`http://localhost/api/...`

### 10.3 关闭服务

```bash
docker compose down
```

