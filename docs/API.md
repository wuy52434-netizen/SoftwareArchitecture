# API 接口文档

本文档详细描述图书管理系统的所有 API 接口。

---

## 目录

- [基础信息](#基础信息)
- [认证模块](#认证模块)
- [用户模块](#用户模块)
- [图书模块](#图书模块)
- [借阅模块](#借阅模块)
- [搜索模块](#搜索模块)
- [通知模块](#通知模块)
- [统计模块](#统计模块)
- [错误码说明](#错误码说明)

---

## 基础信息

### 答辩主线说明

答辩演示以自助借书机为主，浏览器访问 `http://localhost` 时地址栏保持根路径，页面直接渲染借书机首页。读者门户 `/portal` 仍保留，但不作为主演示页面。

### 基础 URL

| 环境 | 地址 |
|------|------|
| 开发环境 | http://localhost:8080/api |
| 测试环境 | http://test-api.example.com/api |
| 生产环境 | https://api.example.com/api |

### 自助借书机核心接口

| 接口 | 方法 | 作用 | 关键参数 |
|------|------|------|----------|
| `/books/scan` | GET | 借书机扫码查询图书，支持 ISBN 或实体副本条码 | `code` |
| `/books/copy/{copyId}` | GET | 根据实体副本 ID 查询图书副本 | `copyId` |
| `/books/copy/barcode/{barcode}` | GET | 根据条码查询实体副本 | `barcode` |
| `/books/{id}/available-copy` | GET | 根据图书 ID 获取一册可借副本 | `id` |
| `/borrow` | POST | 借书，写入借阅记录并扣减副本库存 | `bookId`, `copyId`, `userId` |
| `/return` | POST | 还书，恢复副本状态并计算逾期金额 | `borrowId` 或 `bookId` |
| `/my-borrows` | GET | 查询读者借阅记录 | `userId`, `status` |

借书机必须传递实体副本信息。`book_info.isbn` 只表示书目，`book_copy.copy_id` 和 `book_copy.barcode` 才能表示实际借出的那一册书。

### 通用请求头

| Header | 类型 | 必需 | 说明 |
|--------|------|------|------|
| Content-Type | string | 是 | `application/json` |
| Authorization | string | 否 | Bearer Token（需要认证的接口必需） |

**认证示例**：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 通用响应格式

所有接口返回统一的 JSON 格式：

#### 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // 响应数据
  }
}
```

#### 错误响应

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null
}
```

### 统一结果码

| 码值 | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权/登录过期 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 系统错误 |
| 503 | 系统繁忙/限流 |

### 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      // 数据列表
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| list | array | 数据列表 |
| total | number | 总记录数 |
| size | number | 每页数量 |
| current | number | 当前页码 |
| pages | number | 总页数 |

### 日期时间格式

- 日期：`yyyy-MM-dd`（如：`2024-01-15`）
- 时间：`HH:mm:ss`（如：`14:30:00`）
- 日期时间：`yyyy-MM-dd'T'HH:mm:ss` 或 `yyyy-MM-dd HH:mm:ss`

---

## 认证模块

### 用户登录

**接口地址**：`POST /auth/login`

**认证要求**：不需要

**请求参数**：

```json
{
  "username": "admin",
  "password": "123456"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 密码 |

**成功响应**：

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTcwNTM5NjAwMCwicm9sZSI6ImFkbWluIn0...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTcwNTk5OTYwMH0...",
    "accessTokenExpire": 7200000,
    "refreshTokenExpire": 604800000,
    "user": {
      "id": 1,
      "username": "admin",
      "email": "admin@example.com",
      "phone": "13800138000",
      "role": "admin",
      "status": "active",
      "activeBorrowCount": 0,
      "maxBorrowCount": 5
    }
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| accessToken | string | 访问令牌（有效期 2 小时） |
| refreshToken | string | 刷新令牌（有效期 7 天） |
| accessTokenExpire | number | 访问令牌有效期（毫秒） |
| refreshTokenExpire | number | 刷新令牌有效期（毫秒） |
| user | object | 用户信息 |

**错误码**：

| 码值 | 说明 |
|------|------|
| 400 | 用户名或密码不能为空 |
| 401 | 用户名或密码错误 |
| 403 | 用户已被禁用 |

---

### 刷新 Token

**接口地址**：`POST /auth/refresh`

**认证要求**：不需要（使用 refreshToken）

**请求参数**：

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| refreshToken | string | 是 | 刷新令牌 |

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "accessTokenExpire": 7200000,
    "refreshTokenExpire": 604800000
  }
}
```

**错误码**：

| 码值 | 说明 |
|------|------|
| 400 | refreshToken 不能为空 |
| 401 | refreshToken 无效或已过期 |

---

### 用户注册

**接口地址**：`POST /auth/register`

**认证要求**：不需要

**请求参数**：

```json
{
  "username": "newuser",
  "password": "123456",
  "email": "user@example.com",
  "phone": "13800138000"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名（4-20 字符） |
| password | string | 是 | 密码（6-32 字符） |
| email | string | 否 | 邮箱地址 |
| phone | string | 否 | 手机号 |

**成功响应**：

```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 10,
    "username": "newuser",
    "email": "user@example.com",
    "phone": "13800138000",
    "role": "reader",
    "status": "active"
  }
}
```

**错误码**：

| 码值 | 说明 |
|------|------|
| 400 | 用户名已存在 |
| 400 | 参数格式错误 |

---

### 用户登出

**接口地址**：`POST /auth/logout`

**认证要求**：需要

**成功响应**：

```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

---

### 获取当前用户信息

**接口地址**：`GET /users/current`

**认证要求**：需要

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "phone": "13800138000",
    "role": "admin",
    "status": "active",
    "activeBorrowCount": 2,
    "maxBorrowCount": 5,
    "createdAt": "2024-01-01 10:00:00"
  }
}
```

---

## 用户模块

> **注意**：以下接口需要管理员权限（role: admin）

### 获取用户列表（分页）

**接口地址**：`GET /users`

**认证要求**：需要（管理员）

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | number | 否 | 1 | 页码 |
| size | number | 否 | 10 | 每页数量 |
| keyword | string | 否 | - | 搜索关键词（用户名/邮箱/手机号） |
| role | string | 否 | - | 角色筛选（admin/reader） |
| status | string | 否 | - | 状态筛选（active/inactive） |

**请求示例**：
```
GET /api/users?page=1&size=10&keyword=admin&role=admin
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "username": "admin",
        "email": "admin@example.com",
        "phone": "13800138000",
        "role": "admin",
        "status": "active",
        "activeBorrowCount": 0,
        "createdAt": "2024-01-01 10:00:00"
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  }
}
```

---

### 获取用户详情

**接口地址**：`GET /users/{id}`

**认证要求**：需要（管理员或用户本人）

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 用户 ID |

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "phone": "13800138000",
    "role": "admin",
    "status": "active",
    "activeBorrowCount": 2,
    "maxBorrowCount": 5,
    "createdAt": "2024-01-01 10:00:00",
    "updatedAt": "2024-01-15 14:30:00"
  }
}
```

---

### 创建用户

**接口地址**：`POST /users`

**认证要求**：需要（管理员）

**请求参数**：

```json
{
  "username": "librarian",
  "password": "123456",
  "email": "librarian@example.com",
  "phone": "13900139000",
  "role": "admin",
  "maxBorrowCount": 10
}
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| username | string | 是 | - | 用户名 |
| password | string | 是 | - | 密码 |
| email | string | 否 | - | 邮箱 |
| phone | string | 否 | - | 手机号 |
| role | string | 否 | reader | 角色（admin/reader） |
| maxBorrowCount | number | 否 | 5 | 最大借阅数量 |

---

### 更新用户信息

**接口地址**：`PUT /users/{id}`

**认证要求**：需要（管理员或用户本人）

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 用户 ID |

**请求参数**：

```json
{
  "email": "newemail@example.com",
  "phone": "13900139001"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 否 | 邮箱 |
| phone | string | 否 | 手机号 |
| status | string | 否 | 状态（仅管理员可修改） |
| role | string | 否 | 角色（仅管理员可修改） |
| maxBorrowCount | number | 否 | 最大借阅数量（仅管理员可修改） |

---

### 删除用户

**接口地址**：`DELETE /users/{id}`

**认证要求**：需要（管理员）

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 用户 ID |

**成功响应**：

```json
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```

---

### 重置用户密码

**接口地址**：`POST /users/{id}/reset-password`

**认证要求**：需要（管理员）

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 用户 ID |

**请求参数**：

```json
{
  "newPassword": "newpassword123"
}
```

---

## 图书模块

### 获取图书列表（分页）

**接口地址**：`GET /books`

**认证要求**：不需要（查询），需要（新增/修改/删除）

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | number | 否 | 1 | 页码 |
| size | number | 否 | 10 | 每页数量 |
| keyword | string | 否 | - | 搜索关键词（书名/作者/ISBN） |
| categoryId | number | 否 | - | 分类 ID |
| status | string | 否 | - | 状态筛选（available/unavailable） |
| sortBy | string | 否 | - | 排序字段（borrowCount/publishDate/title） |
| sortOrder | string | 否 | desc | 排序顺序（asc/desc） |

**请求示例**：
```
GET /api/books?page=1&size=20&keyword=Java&categoryId=1&sortBy=borrowCount&sortOrder=desc
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "Java编程思想",
        "author": "Bruce Eckel",
        "isbn": "9787111213826",
        "publisher": "机械工业出版社",
        "publishDate": "2007-01-01",
        "categoryId": 1,
        "categoryName": "计算机",
        "availableCopies": 5,
        "totalCopies": 10,
        "borrowCount": 150,
        "coverImage": "http://example.com/covers/java.jpg",
        "status": "available",
        "createdAt": "2024-01-01 10:00:00"
      }
    ],
    "total": 25,
    "size": 20,
    "current": 1,
    "pages": 2
  }
}
```

---

### 获取图书详情

**接口地址**：`GET /books/{id}`

**认证要求**：不需要

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 图书 ID |

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "Java编程思想",
    "author": "Bruce Eckel",
    "isbn": "9787111213826",
    "publisher": "机械工业出版社",
    "publishDate": "2007-01-01",
    "pages": 880,
    "summary": "本书赢得了全球程序员的广泛赞誉，即使是最晦涩的概念，在Bruce Eckel的文字亲和力和小而直接的编程示例面前也会化解于无形。",
    "categoryId": 1,
    "categoryName": "计算机",
    "language": "中文",
    "price": 108.00,
    "coverImage": "http://example.com/covers/java.jpg",
    "totalCopies": 10,
    "availableCopies": 5,
    "borrowCount": 150,
    "status": "available",
    "copies": [
      {
        "id": 1,
        "copyNumber": "CP001001",
        "location": "A区-01架-01层",
        "status": "available"
      },
      {
        "id": 2,
        "copyNumber": "CP001002",
        "location": "A区-01架-02层",
        "status": "borrowed"
      }
    ],
    "createdAt": "2024-01-01 10:00:00",
    "updatedAt": "2024-01-15 14:30:00"
  }
}
```

---

### 新增图书

**接口地址**：`POST /books`

**认证要求**：需要（管理员）

**请求参数**：

```json
{
  "title": "深入理解Java虚拟机",
  "author": "周志明",
  "isbn": "9787111421900",
  "publisher": "机械工业出版社",
  "publishDate": "2013-06-01",
  "pages": 480,
  "summary": "一位工作了近10年的架构师，一位写过百万级代码的程序员，一位在VM领域摸爬滚打了数年的研究者，用他的经验和智慧，为读者揭秘Java虚拟机的工作原理。",
  "categoryId": 1,
  "language": "中文",
  "price": 79.00,
  "coverImage": "http://example.com/covers/jvm.jpg",
  "totalCopies": 5,
  "location": "A区-02架"
}
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| title | string | 是 | - | 书名 |
| author | string | 是 | - | 作者 |
| isbn | string | 是 | - | ISBN |
| publisher | string | 否 | - | 出版社 |
| publishDate | string | 否 | - | 出版日期 |
| pages | number | 否 | - | 页数 |
| summary | string | 否 | - | 内容简介 |
| categoryId | number | 是 | - | 分类 ID |
| language | string | 否 | 中文 | 语言 |
| price | number | 否 | - | 定价 |
| coverImage | string | 否 | - | 封面图片 URL |
| totalCopies | number | 否 | 1 | 总副本数 |
| location | string | 否 | - | 存放位置 |

---

### 更新图书

**接口地址**：`PUT /books/{id}`

**认证要求**：需要（管理员）

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 图书 ID |

**请求参数**：同新增图书（可选字段）

---

### 删除图书

**接口地址**：`DELETE /books/{id}`

**认证要求**：需要（管理员）

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 图书 ID |

---

### 获取图书分类列表

**接口地址**：`GET /categories`

**认证要求**：不需要

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "计算机",
      "code": "computer",
      "description": "计算机相关书籍",
      "sort": 1,
      "bookCount": 150
    },
    {
      "id": 2,
      "name": "文学",
      "code": "literature",
      "description": "文学类书籍",
      "sort": 2,
      "bookCount": 320
    }
  ]
}
```

---

### 新增图书分类

**接口地址**：`POST /categories`

**认证要求**：需要（管理员）

**请求参数**：

```json
{
  "name": "历史",
  "code": "history",
  "description": "历史类书籍",
  "sort": 3
}
```

---

### 获取热门图书

**接口地址**：`GET /books/hot`

**认证要求**：不需要

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | number | 否 | 10 | 返回数量 |
| days | number | 否 | 30 | 统计天数 |

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "title": "Java编程思想",
      "author": "Bruce Eckel",
      "coverImage": "http://example.com/covers/java.jpg",
      "borrowCount": 150,
      "categoryName": "计算机"
    }
  ]
}
```

---

### 获取新书推荐

**接口地址**：`GET /books/new`

**认证要求**：不需要

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | number | 否 | 10 | 返回数量 |
| days | number | 否 | 30 | 入库天数 |

---

## 借阅模块

### 借书

**接口地址**：`POST /borrow`

**认证要求**：需要

**请求参数**：

```json
{
  "bookId": 1,
  "copyId": 1,
  "borrowDate": "2024-01-15",
  "dueDate": "2024-02-14"
}
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| bookId | number | 是 | - | 图书 ID |
| copyId | number | 否 | - | 副本 ID（不传则系统分配） |
| borrowDate | string | 否 | 当天 | 借阅日期 |
| dueDate | string | 否 | 30天后 | 到期日期 |

**成功响应**：

```json
{
  "code": 200,
  "message": "借阅成功",
  "data": {
    "recordId": 1001,
    "userId": 1,
    "bookId": 1,
    "copyId": 1,
    "bookTitle": "Java编程思想",
    "bookAuthor": "Bruce Eckel",
    "coverImage": "http://example.com/covers/java.jpg",
    "borrowDate": "2024-01-15",
    "dueDate": "2024-02-14",
    "returnDate": null,
    "status": "active",
    "fineAmount": 0.00,
    "finePaid": 0
  }
}
```

**错误码**：

| 码值 | 说明 |
|------|------|
| 400 | 图书 ID 不能为空 |
| 403 | 用户已被禁用 |
| 403 | 借阅数量已达上限 |
| 404 | 图书不存在 |
| 404 | 图书副本不存在 |
| 409 | 图书已被借出 |
| 429 | 系统繁忙，请稍后重试（分布式锁获取失败） |

---

### 还书

**接口地址**：`POST /return`

**认证要求**：需要

**请求参数**：

```json
{
  "borrowId": 1001
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| borrowId | number | 是 | 借阅记录 ID |

**成功响应**：

```json
{
  "code": 200,
  "message": "归还成功",
  "data": {
    "recordId": 1001,
    "bookId": 1,
    "bookTitle": "Java编程思想",
    "returnDate": "2024-01-25",
    "dueDate": "2024-02-14",
    "overdueDays": 0,
    "fineAmount": 0.00,
    "status": "已归还"
  }
}
```

**逾期示例响应**：

```json
{
  "code": 200,
  "message": "归还成功（逾期 15 天）",
  "data": {
    "recordId": 1002,
    "bookId": 2,
    "bookTitle": "深入理解Java虚拟机",
    "returnDate": "2024-03-01",
    "dueDate": "2024-02-14",
    "overdueDays": 15,
    "fineAmount": 7.50,
    "status": "已归还（需支付罚款）"
  }
}
```

---

### 获取我的借阅记录

**接口地址**：`GET /my-borrows`

**认证要求**：需要

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| status | string | 否 | - | 状态筛选（active/returned） |
| page | number | 否 | 1 | 页码 |
| size | number | 否 | 10 | 每页数量 |

**请求示例**：
```
GET /api/my-borrows?status=active&page=1&size=10
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1001,
        "bookId": 1,
        "bookTitle": "Java编程思想",
        "bookAuthor": "Bruce Eckel",
        "coverImage": "http://example.com/covers/java.jpg",
        "borrowDate": "2024-01-15",
        "dueDate": "2024-02-14",
        "returnDate": null,
        "status": "active",
        "fineAmount": 0.00,
        "remainingDays": 30
      }
    ],
    "total": 3,
    "size": 10,
    "current": 1,
    "pages": 1
  }
}
```

---

### 获取借阅记录详情

**接口地址**：`GET /borrow-records/{id}`

**认证要求**：需要（管理员或借阅者本人）

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 借阅记录 ID |

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1001,
    "userId": 1,
    "userName": "admin",
    "bookId": 1,
    "bookTitle": "Java编程思想",
    "bookAuthor": "Bruce Eckel",
    "bookIsbn": "9787111213826",
    "copyId": 1,
    "copyNumber": "CP001001",
    "borrowDate": "2024-01-15",
    "dueDate": "2024-02-14",
    "returnDate": null,
    "status": "active",
    "fineAmount": 0.00,
    "finePaid": 0,
    "operatorId": 1,
    "operatorName": "admin",
    "remark": null,
    "createdAt": "2024-01-15 14:30:00"
  }
}
```

---

### 管理员获取所有借阅记录

**接口地址**：`GET /borrow-records`

**认证要求**：需要（管理员）

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| userId | number | 否 | - | 用户 ID 筛选 |
| bookId | number | 否 | - | 图书 ID 筛选 |
| status | string | 否 | - | 状态筛选（active/returned/overdue） |
| keyword | string | 否 | - | 搜索关键词（用户名/书名） |
| borrowDateStart | string | 否 | - | 借阅日期开始 |
| borrowDateEnd | string | 否 | - | 借阅日期结束 |
| page | number | 否 | 1 | 页码 |
| size | number | 否 | 10 | 每页数量 |

---

### 续借

**接口地址**：`POST /borrow-records/{id}/renew`

**认证要求**：需要（管理员或借阅者本人）

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 借阅记录 ID |

**请求参数**：

```json
{
  "extendDays": 15
}
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| extendDays | number | 否 | 30 | 续借天数 |

---

### 检查逾期罚款

**接口地址**：`GET /borrow-records/{id}/overdue-check`

**认证要求**：需要（管理员或借阅者本人）

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordId": 1002,
    "dueDate": "2024-02-14",
    "today": "2024-03-01",
    "overdueDays": 15,
    "fineAmount": 7.50,
    "finePerDay": 0.50,
    "maxFine": 50.00,
    "isOverdue": true
  }
}
```

---

### 支付罚款

**接口地址**：`POST /borrow-records/{id}/pay-fine`

**认证要求**：需要（管理员或借阅者本人）

**请求参数**：

```json
{
  "amount": 7.50,
  "paymentMethod": "wechat"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| amount | number | 是 | 支付金额 |
| paymentMethod | string | 是 | 支付方式（wechat/alipay/cash） |

---

## 搜索模块

### 全文搜索

**接口地址**：`GET /search`

**认证要求**：不需要

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| keyword | string | 是 | - | 搜索关键词 |
| categoryId | number | 否 | - | 分类 ID 筛选 |
| status | string | 否 | - | 状态筛选（available/unavailable） |
| page | number | 否 | 1 | 页码 |
| size | number | 否 | 10 | 每页数量 |
| highlight | boolean | 否 | true | 是否高亮显示 |

**请求示例**：
```
GET /api/search?keyword=Java&categoryId=1&highlight=true&page=1&size=20
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "<em>Java</em>编程思想",
        "author": "Bruce Eckel",
        "isbn": "9787111213826",
        "summary": "本书赢得了全球<em>Java</em>程序员的广泛赞誉...",
        "publisher": "机械工业出版社",
        "categoryId": 1,
        "categoryName": "计算机",
        "availableCopies": 5,
        "totalCopies": 10,
        "borrowCount": 150,
        "coverImage": "http://example.com/covers/java.jpg",
        "highlightFields": ["title", "summary"]
      }
    ],
    "total": 25,
    "page": 1,
    "size": 10,
    "took": 45,
    "maxScore": 0.875
  }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| took | number | 查询耗时（毫秒） |
| maxScore | number | 最高相关性分数 |
| highlightFields | array | 包含高亮的字段列表 |

---

### 搜索建议（自动补全）

**接口地址**：`GET /search/suggest`

**认证要求**：不需要

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 是 | 搜索关键词前缀 |
| limit | number | 否 | 10 | 返回数量 |

**请求示例**：
```
GET /api/search/suggest?keyword=Jav
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "suggestions": [
      "Java编程思想",
      "Java核心技术",
      "Java并发编程实战",
      "深入理解Java虚拟机"
    ]
  }
}
```

---

### 搜索热词

**接口地址**：`GET /search/hot-words`

**认证要求**：不需要

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | number | 否 | 10 | 返回数量 |
| days | number | 否 | 7 | 统计天数 |

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "keyword": "Java",
      "count": 1520
    },
    {
      "keyword": "人工智能",
      "count": 980
    },
    {
      "keyword": "小说",
      "count": 756
    }
  ]
}
```

---

### 重建索引

**接口地址**：`POST /search/reindex`

**认证要求**：需要（管理员）

**说明**：重新构建 Elasticsearch 索引（全量）

**成功响应**：

```json
{
  "code": 200,
  "message": "索引重建任务已提交",
  "data": {
    "taskId": "reindex_20240115_143000",
    "totalBooks": 1500
  }
}
```

---

## 通知模块

### 获取通知列表

**接口地址**：`GET /notifications`

**认证要求**：需要

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| type | string | 否 | - | 类型筛选（borrow/return/overdue/system） |
| read | boolean | 否 | - | 已读/未读筛选 |
| page | number | 否 | 1 | 页码 |
| size | number | 否 | 10 | 每页数量 |

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "图书借阅成功",
        "content": "您已成功借阅《Java编程思想》，到期日期：2024-02-14",
        "type": "borrow",
        "read": false,
        "relatedType": "borrow",
        "relatedId": 1001,
        "createdAt": "2024-01-15 14:30:00"
      },
      {
        "id": 2,
        "title": "图书即将到期提醒",
        "content": "您借阅的《深入理解Java虚拟机》将于 2024-02-14 到期，请及时归还或续借",
        "type": "overdue",
        "read": false,
        "relatedType": "borrow",
        "relatedId": 1002,
        "createdAt": "2024-02-10 09:00:00"
      }
    ],
    "total": 5,
    "unreadCount": 3
  }
}
```

---

### 标记为已读

**接口地址**：`PUT /notifications/{id}/read`

**认证要求**：需要

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | number | 是 | 通知 ID |

---

### 全部标记为已读

**接口地址**：`PUT /notifications/read-all`

**认证要求**：需要

---

### 删除通知

**接口地址**：`DELETE /notifications/{id}`

**认证要求**：需要

---

### 获取未读数量

**接口地址**：`GET /notifications/unread-count`

**认证要求**：需要

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "unreadCount": 3,
    "unreadByType": {
      "borrow": 1,
      "overdue": 2,
      "system": 0
    }
  }
}
```

---

## 统计模块

> **注意**：以下接口需要管理员权限

### 获取仪表盘统计数据

**接口地址**：`GET /stats/dashboard`

**认证要求**：需要（管理员）

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalBooks": 1500,
    "totalCategories": 12,
    "totalCopies": 4500,
    "availableCopies": 3200,
    "totalUsers": 850,
    "activeUsers": 520,
    "activeBorrows": 120,
    "overdueCount": 5,
    "todayBorrows": 15,
    "todayReturns": 8,
    "thisMonthBorrows": 285,
    "thisMonthReturns": 210,
    "topBorrowedBooks": [
      {
        "id": 1,
        "title": "Java编程思想",
        "borrowCount": 150
      }
    ],
    "topActiveUsers": [
      {
        "id": 10,
        "username": "reader01",
        "borrowCount": 25
      }
    ]
  }
}
```

---

### 获取借阅趋势

**接口地址**：`GET /stats/borrow-trend`

**认证要求**：需要（管理员）

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| startDate | string | 否 | 30天前 | 开始日期 |
| endDate | string | 否 | 今天 | 结束日期 |
| interval | string | 否 | day | 时间间隔（day/week/month） |

**请求示例**：
```
GET /api/stats/borrow-trend?startDate=2024-01-01&endDate=2024-01-31&interval=day
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "dates": ["2024-01-01", "2024-01-02", "2024-01-03", "..."],
    "borrowCounts": [12, 18, 15, "..."],
    "returnCounts": [8, 10, 12, "..."],
    "totalBorrows": 385,
    "totalReturns": 290,
    "averageDailyBorrows": 12.4
  }
}
```

---

### 获取分类统计

**接口地址**：`GET /stats/category`

**认证要求**：需要（管理员）

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "计算机",
      "bookCount": 320,
      "borrowCount": 1520,
      "borrowRatio": 35.5,
      "availableCopies": 850
    },
    {
      "id": 2,
      "name": "文学",
      "bookCount": 450,
      "borrowCount": 1280,
      "borrowRatio": 29.9,
      "availableCopies": 1200
    }
  ]
}
```

---

### 获取热门图书排行

**接口地址**：`GET /stats/hot-books`

**认证要求**：需要（管理员）

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| limit | number | 否 | 10 | 返回数量 |
| days | number | 否 | 30 | 统计天数 |
| categoryId | number | 否 | - | 分类筛选 |

---

### 获取用户统计

**接口地址**：`GET /stats/users`

**认证要求**：需要（管理员）

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| startDate | string | 否 | 本月初 | 开始日期 |
| endDate | string | 否 | 今天 | 结束日期 |

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 850,
    "activeUsers": 520,
    "newUsers": 45,
    "usersByRole": {
      "admin": 5,
      "reader": 845
    },
    "usersByStatus": {
      "active": 780,
      "inactive": 70
    }
  }
}
```

---

### 获取逾期统计

**接口地址**：`GET /stats/overdue`

**认证要求**：需要（管理员）

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "overdueCount": 15,
    "overdueAmount": 45.50,
    "paidAmount": 120.00,
    "unpaidAmount": 45.50,
    "overdueByDays": {
      "1-7天": 5,
      "8-14天": 6,
      "15-30天": 3,
      "30天以上": 1
    }
  }
}
```

---

### 导出统计报表

**接口地址**：`POST /stats/export`

**认证要求**：需要（管理员）

**请求参数**：

```json
{
  "type": "borrow",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "format": "excel"
}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 报表类型（borrow/return/user/book/overdue） |
| startDate | string | 是 | 开始日期 |
| endDate | string | 是 | 结束日期 |
| format | string | 否 | 导出格式（excel/csv/pdf） |

**成功响应**：

```json
{
  "code": 200,
  "message": "导出任务已提交",
  "data": {
    "taskId": "export_20240115_143000",
    "estimatedTime": 5000
  }
}
```

---

## 错误码说明

### HTTP 状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未授权，需要登录或 Token 过期 |
| 403 | 权限不足，禁止访问 |
| 404 | 请求的资源不存在 |
| 409 | 资源冲突（如图书已被借出） |
| 429 | 请求过多，限流触发 |
| 500 | 服务器内部错误 |
| 503 | 服务不可用 |

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 参数错误 |
| 1002 | 用户不存在 |
| 1003 | 用户名或密码错误 |
| 1004 | 用户名已存在 |
| 1005 | 用户已被禁用 |
| 1006 | Token 无效 |
| 1007 | Token 已过期 |
| 1008 | 权限不足 |
| 2001 | 图书不存在 |
| 2002 | 图书已被借出 |
| 2003 | 图书副本不存在 |
| 2004 | 图书副本不可用 |
| 2005 | 图书分类不存在 |
| 3001 | 借阅记录不存在 |
| 3002 | 借阅数量已达上限 |
| 3003 | 图书已被借阅 |
| 3004 | 图书已归还 |
| 3005 | 逾期天数计算错误 |
| 3006 | 罚款金额不足 |
| 3007 | 锁获取失败（并发问题） |
| 4001 | 搜索关键词不能为空 |
| 4002 | 索引重建失败 |
| 5001 | 系统繁忙 |
| 5002 | 服务降级 |
| 5003 | 数据库操作失败 |
| 5004 | 缓存操作失败 |
| 5005 | 消息发送失败 |

### 错误响应示例

```json
{
  "code": 3002,
  "message": "借阅数量已达上限，最多可借阅 5 本",
  "data": null
}
```

---

## 附录

### A. 角色权限说明

| 角色 | 权限 |
|------|------|
| **admin**（管理员） | 所有权限 |
| **reader**（读者） | - 登录/登出<br>- 查看/搜索图书<br>- 借书/还书/续借<br>- 查看自己的借阅记录<br>- 修改个人信息 |

### B. 状态说明

#### 用户状态

| 状态码 | 说明 |
|--------|------|
| active | 正常 |
| inactive | 禁用 |
| locked | 锁定 |

#### 图书状态

| 状态码 | 说明 |
|--------|------|
| available | 可借 |
| unavailable | 不可借 |

#### 副本状态

| 状态码 | 说明 |
|--------|------|
| available | 可借 |
| borrowed | 已借 |
| repairing | 维修中 |
| lost | 丢失 |
| damaged | 损坏 |

#### 借阅状态

| 状态码 | 说明 |
|--------|------|
| active | 借阅中 |
| returned | 已归还 |
| renewed | 已续借 |
| overdue | 已逾期 |

### C. 时间戳说明

所有接口返回的时间戳均为以下格式之一：
- 日期时间：`yyyy-MM-dd HH:mm:ss`（如：`2024-01-15 14:30:00`）
- 日期：`yyyy-MM-dd`（如：`2024-01-15`）

### D. 金额说明

- 金额单位：元（人民币）
- 精度：两位小数
- 示例：`0.50` 表示 0.5 元

### E. 分页参数说明

| 参数 | 类型 | 说明 |
|------|------|------|
| page | number | 页码，从 1 开始 |
| size | number | 每页数量，默认 10，最大 100 |
| total | number | 总记录数 |
| current | number | 当前页码 |
| pages | number | 总页数 |

---

## 版本历史

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0.0 | 2024-01-01 | 初始版本 |

---

## 联系方式

如有问题，请联系技术团队。
