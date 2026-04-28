# 图书管理系统 - 微服务架构版

一个基于 Spring Boot 微服务架构的现代化图书馆管理系统，支持自助借还书、图书管理、用户管理等功能。

## 目录

- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [服务说明](#服务说明)
- [API 概览](#api-概览)
- [部署指南](#部署指南)
- [监控与治理](#监控与治理)
- [开发指南](#开发指南)

---

## 功能特性

- 📚 **图书管理**：图书的增删改查、分类管理、搜索筛选
- 📖 **借阅管理**：自助借书、还书、借阅记录查询、逾期罚款计算
- 👥 **用户管理**：多角色用户（管理员/读者）、JWT 认证
- 🔍 **全文检索**：基于 Elasticsearch 的图书全文检索
- 📨 **消息通知**：RabbitMQ 异步消息通知（借阅/归还/逾期提醒）
- 📊 **数据统计**：借阅统计、ECharts 图表数据接口
- ⚡ **性能优化**：Redis 缓存 + 分布式锁
- 🛡️ **安全可靠**：Sentinel 限流熔断、服务降级
- 🐳 **容器化部署**：Docker Compose 一键启动

---

## 技术架构

### 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Security | 6.x | 安全框架 |
| Spring Cloud Alibaba | 2023.0.1.2 | 微服务生态 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| Nacos | 2.x | 服务注册发现 |
| Sentinel | 1.8.6 | 限流熔断 |
| Redis | 7.x | 缓存 + 分布式锁 |
| RabbitMQ | 3.12 | 消息队列 |
| Elasticsearch | 8.11 | 全文检索 |
| MySQL | 8.0 | 主数据库 |
| Prometheus | 2.47 | 监控指标收集 |
| Grafana | 10.2 | 监控可视化 |
| Vue | 3.x | 前端框架 |
| Element Plus | - | UI 组件库 |
| Docker | - | 容器化部署 |

### 架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              前端 (Vue 3)                                  │
│                    端口: 3000 (开发) / 80 (Nginx 生产)                    │
└───────────────────────────────────┬───────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        API Gateway (8080)                                 │
│              ┌─────────────────────────────────────────┐                   │
│              │ 路由转发 │ JWT 鉴权 │ Sentinel 限流 │ CORS 处理 │         │
│              └─────────────────────────────────────────┘                   │
└───────────────────────────────────┬───────────────────────────────────────┘
                                    │
        ┌───────────────┬───────────┴───────────┬───────────────┐
        ▼               ▼                       ▼               ▼
┌───────────────┐ ┌───────────┐ ┌───────────────┐ ┌───────────┐
│ User Service  │ │Book Service│ │Borrow Service │ │Search Svc │
│    (8081)     │ │  (8082)   │ │    (8083)     │ │  (8084)  │
│               │ │           │ │               │ │           │
│  用户管理     │ │ 图书管理  │ │ 借阅管理     │ │ ES 检索  │
│  登录认证     │ │ 库存管理  │ │ 逾期计算     │ │ 分词搜索  │
│  权限控制     │ │ 分类管理  │ │ Feign 调用   │ │ 高亮显示  │
└───────┬───────┘ └─────┬─────┘ └───────┬───────┘ └─────┬─────┘
        │               │               │               │
        └───────────────┴───────┬───────┴───────────────┘
                                │
        ┌───────────────┬───────┴───────┬───────────────┐
        ▼               ▼               ▼               ▼
┌───────────────┐ ┌───────────┐ ┌───────────────┐ ┌───────────┐
│ Notify Service│ │Stats Svc  │ │   MySQL       │ │  Redis    │
│    (8085)     │ │  (8086)   │ │   (3306)      │ │  (6379)   │
│               │ │           │ │               │ │           │
│ MQ 消息消费   │ │ 数据统计  │ │ 业务数据     │ │ 缓存      │
│ 短信/邮件通知 │ │ ECharts   │ │               │ │ 分布式锁  │
│ 站内通知      │ │ 报表分析  │ │               │ │           │
└───────────────┘ └───────────┘ └───────────────┘ └───────────┘

┌───────────────┐ ┌───────────┐ ┌───────────────┐ ┌───────────┐
│  RabbitMQ     │ │    ES     │ │  Prometheus   │ │  Grafana  │
│  (5672/15672) │ │ (9200)   │ │   (9090)      │ │  (3001)   │
│               │ │           │ │               │ │           │
│ 异步消息队列  │ │ 全文检索  │ │ 指标收集     │ │ 可视化   │
│ 延迟消息      │ │ 分词索引  │ │ 告警规则     │ │ 仪表盘   │
└───────────────┘ └───────────┘ └───────────────┘ └───────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                           Nacos (8848)                                    │
│                       服务注册发现 + 配置管理                               │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 项目结构

```
softwarearchitecture/
├── backend/                          # 后端微服务
│   ├── pom.xml                       # 父工程 POM
│   ├── Dockerfile.gateway            # API 网关 Dockerfile
│   ├── Dockerfile.user               # 用户服务 Dockerfile
│   ├── Dockerfile.book               # 图书服务 Dockerfile
│   ├── Dockerfile.borrow             # 借阅服务 Dockerfile
│   ├── Dockerfile.search             # 搜索服务 Dockerfile
│   ├── Dockerfile.notify             # 通知服务 Dockerfile
│   ├── Dockerfile.stats              # 统计服务 Dockerfile
│   │
│   ├── common/                        # 公共模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/library/common/
│   │       ├── constant/              # 常量定义
│   │       │   └── Constants.java
│   │       ├── exception/             # 异常处理
│   │       │   ├── BusinessException.java
│   │       │   └── GlobalExceptionHandler.java
│   │       ├── result/                # 统一返回结果
│   │       │   ├── Result.java
│   │       │   ├── ResultCode.java
│   │       │   └── PageResult.java
│   │       └── util/                  # 工具类
│   │           ├── JwtUtil.java       # JWT 工具
│   │           ├── RedisUtil.java     # Redis 工具
│   │           └── RedisLock.java     # 分布式锁
│   │
│   ├── api-gateway/                   # API 网关 (端口 8080)
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/library/gateway/
│   │       │   ├── config/            # 配置类
│   │       │   │   ├── GatewayProperties.java
│   │       │   │   ├── JwtProperties.java
│   │       │   │   └── WebConfig.java
│   │       │   ├── filter/            # 过滤器
│   │       │   │   └── JwtAuthenticationFilter.java
│   │       │   └── ApiGatewayApplication.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── user-service/                  # 用户服务 (端口 8081)
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/library/user/
│   │       │   ├── controller/        # 控制器
│   │       │   │   ├── AuthController.java
│   │       │   │   └── UserController.java
│   │       │   ├── service/           # 业务逻辑
│   │       │   │   ├── AuthService.java
│   │       │   │   └── UserService.java
│   │       │   ├── security/          # 安全配置
│   │       │   │   ├── SecurityConfig.java
│   │       │   │   ├── JwtAuthenticationFilter.java
│   │       │   │   └── CustomUserDetailsService.java
│   │       │   ├── entity/            # 实体类
│   │       │   │   └── User.java
│   │       │   ├── dto/               # 数据传输对象
│   │       │   │   └── UserDTO.java
│   │       │   ├── mapper/            # MyBatis Mapper
│   │       │   │   └── UserMapper.java
│   │       │   └── UserServiceApplication.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── book-service/                  # 图书服务 (端口 8082)
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/library/book/
│   │       │   ├── service/
│   │       │   │   └── BookService.java
│   │       │   ├── entity/
│   │       │   │   ├── BookInfo.java
│   │       │   │   ├── BookCategory.java
│   │       │   │   └── BookCopy.java
│   │       │   ├── dto/
│   │       │   │   └── BookDTO.java
│   │       │   ├── mapper/
│   │       │   │   └── BookInfoMapper.java
│   │       │   └── BookServiceApplication.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── borrow-service/                # 借阅服务 (端口 8083)
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/library/borrow/
│   │       │   ├── controller/
│   │       │   │   └── BorrowController.java
│   │       │   ├── service/
│   │       │   │   └── BorrowService.java
│   │       │   ├── client/            # Feign 客户端
│   │       │   │   ├── BookClient.java
│   │       │   │   ├── BookClientFallback.java
│   │       │   │   ├── UserClient.java
│   │       │   │   └── UserClientFallback.java
│   │       │   ├── entity/
│   │       │   │   └── BorrowRecord.java
│   │       │   ├── dto/
│   │       │   │   └── BorrowDTO.java
│   │       │   ├── mapper/
│   │       │   │   └── BorrowRecordMapper.java
│   │       │   └── BorrowServiceApplication.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── search-service/                # 搜索服务 (端口 8084)
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/library/search/
│   │       │   ├── controller/
│   │       │   │   └── SearchController.java
│   │       │   ├── service/
│   │       │   │   └── SearchService.java
│   │       │   ├── document/          # ES 文档实体
│   │       │   │   └── BookDocument.java
│   │       │   ├── dto/
│   │       │   │   └── SearchDTO.java
│   │       │   └── SearchServiceApplication.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   ├── notify-service/                # 通知服务 (端口 8085)
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/library/notify/
│   │       │   ├── config/
│   │       │   │   └── RabbitMQConfig.java
│   │       │   ├── service/
│   │       │   │   ├── NotifyConsumerService.java
│   │       │   │   └── NotifySenderService.java
│   │       │   ├── dto/
│   │       │   │   ├── BorrowEvent.java
│   │       │   │   └── NotifyMessage.java
│   │       │   └── NotifyServiceApplication.java
│   │       └── resources/
│   │           └── application.yml
│   │
│   └── stats-service/                 # 统计服务 (端口 8086)
│       ├── pom.xml
│       └── src/main/
│           ├── java/com/library/stats/
│           │   ├── controller/
│           │   │   └── StatsController.java
│           │   ├── service/
│           │   │   ├── StatsService.java
│           │   │   └── StatsConsumerService.java
│           │   ├── dto/
│           │   │   ├── ChartData.java
│           │   │   └── DashboardStats.java
│           │   └── StatsServiceApplication.java
│           └── resources/
│               └── application.yml
│
├── frontend/                           # 前端 (Vue 3)
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── main.js
│       ├── App.vue
│       ├── router/
│       │   └── index.js
│       ├── stores/
│       │   └── auth.js
│       ├── api/
│       │   ├── index.js
│       │   ├── auth.js
│       │   ├── books.js
│       │   ├── borrows.js
│       │   ├── users.js
│       │   └── settings.js
│       ├── layouts/
│       │   ├── AdminLayout.vue
│       │   ├── KioskLayout.vue
│       │   └── PortalLayout.vue
│       ├── views/
│       │   ├── Login.vue
│       │   ├── admin/
│       │   │   ├── Books.vue
│       │   │   ├── Borrows.vue
│       │   │   ├── Users.vue
│       │   │   └── Settings.vue
│       │   ├── kiosk/
│       │   │   ├── Home.vue
│       │   │   ├── Search.vue
│       │   │   ├── Borrow.vue
│       │   │   └── Return.vue
│       │   └── portal/
│       │       ├── Home.vue
│       │       ├── Books.vue
│       │       └── MyBorrows.vue
│       └── styles/
│           └── index.css
│
├── docker/                             # Docker 配置
│   ├── docker-compose.yml              # 完整编排配置
│   ├── redis.conf                      # Redis 配置
│   ├── prometheus.yml                  # Prometheus 配置
│   └── nginx.conf                      # Nginx 配置
│
├── database/                           # 数据库脚本
│   └── init.sql                        # 初始化脚本
│
├── docs/                               # 文档目录
├── 架构设计方案.md                      # 架构设计文档
├── 团队开发方案.md                      # 团队开发方案
└── README.md                           # 本文档
```

---

## 快速开始

### 方式一：Docker Compose（推荐）

**前置要求**：
- Docker 20.10+
- Docker Compose 2.0+

1. **进入项目目录**
```bash
cd softwarearchitecture
```

2. **启动中间件**（先启动中间件，等待初始化完成）
```bash
cd docker
docker-compose up -d mysql redis rabbitmq elasticsearch nacos sentinel-dashboard
```

3. **等待中间件启动**
- MySQL: 约 30 秒
- Elasticsearch: 约 1-2 分钟
- Nacos: 约 30 秒

4. **编译后端项目**
```bash
cd ../backend
mvn clean package -DskipTests
```

5. **启动所有服务**
```bash
cd ../docker
docker-compose up -d
```

6. **查看服务状态**
```bash
docker-compose ps
```

7. **访问应用**
| 服务 | 地址 | 说明 |
|------|------|------|
| API 网关 | http://localhost:8080 | 后端 API 入口 |
| Nacos 控制台 | http://localhost:8848/nacos | 服务注册发现 |
| Sentinel 控制台 | http://localhost:8858 | 限流熔断控制台 |
| Prometheus | http://localhost:9090 | 监控指标 |
| Grafana | http://localhost:3001 | 监控可视化 |
| RabbitMQ 管理界面 | http://localhost:15672 | 消息队列管理 |

**默认账户**：
| 服务 | 用户名 | 密码 |
|------|--------|------|
| Nacos | nacos | nacos |
| Sentinel | sentinel | sentinel123 |
| RabbitMQ | admin | admin123 |
| Grafana | admin | admin123 |
| MySQL | root | root123 |

8. **停止服务**
```bash
docker-compose down
```

**停止并删除数据**
```bash
docker-compose down -v
```

### 方式二：本地开发运行

**前置要求**：
- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0
- Redis 7.x
- RabbitMQ 3.x
- Elasticsearch 8.x
- Nacos 2.x

1. **启动本地中间件**
确保以下服务已启动并运行在默认端口：
- MySQL: 3306
- Redis: 6379
- RabbitMQ: 5672
- Elasticsearch: 9200
- Nacos: 8848

2. **初始化数据库**
```bash
mysql -u root -p < database/init.sql
```

3. **编译并启动后端服务**
```bash
cd backend

# 编译
mvn clean install -DskipTests

# 按顺序启动各服务（新开终端窗口）
# 1. 用户服务
cd user-service
mvn spring-boot:run

# 2. 图书服务
cd book-service
mvn spring-boot:run

# 3. 借阅服务
cd borrow-service
mvn spring-boot:run

# 4. 搜索服务
cd search-service
mvn spring-boot:run

# 5. 通知服务
cd notify-service
mvn spring-boot:run

# 6. 统计服务
cd stats-service
mvn spring-boot:run

# 7. API 网关
cd api-gateway
mvn spring-boot:run
```

4. **启动前端**
```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

5. **访问应用**
- 前端页面: http://localhost:3000
- API 网关: http://localhost:8080

---

## 服务说明

### API 网关 (api-gateway, 端口 8080)

**功能**：
- 统一入口：所有请求通过网关路由
- JWT 鉴权：白名单之外的接口需要 Token 验证
- Sentinel 限流：接口级别 QPS 控制
- CORS 处理：跨域资源共享
- 路由转发：基于服务名的负载均衡路由

**白名单接口**（无需 Token）：
- `POST /api/auth/login` - 登录
- `POST /api/auth/register` - 注册
- `POST /api/auth/refresh` - 刷新 Token
- `GET /api/books/**` - 图书查询
- `GET /api/search/**` - 搜索接口

### 用户服务 (user-service, 端口 8081)

**功能**：
- 用户认证：登录/注册/Token 刷新
- 用户管理：CRUD 操作
- 权限控制：RBAC 角色权限模型
- Spring Security + JWT：无状态认证

**角色说明**：
| 角色 | 权限 |
|------|------|
| admin | 所有权限（用户管理、图书管理、借阅管理、系统设置） |
| reader | 借书、还书、查看自己的借阅记录、搜索图书 |

### 图书服务 (book-service, 端口 8082)

**功能**：
- 图书 CRUD：图书信息增删改查
- 分类管理：图书分类维护
- 库存管理：可借副本数量管理
- Redis 缓存：图书列表/详情缓存

**缓存策略**：
| 缓存 Key | 类型 | TTL |
|----------|------|-----|
| book:list:* | String | 60s |
| book:info:{id} | String | 120s |

### 借阅服务 (borrow-service, 端口 8083)

**功能**：
- 借书：核心借阅流程（用户校验、库存扣减、记录创建）
- 还书：归还流程（库存恢复、逾期计算）
- 逾期罚款：自动计算逾期天数和罚款金额
- Redis 分布式锁：防止并发超借
- RabbitMQ 消息：借阅事件异步通知

**借书流程**：
```
1. 获取 Redis 分布式锁 (lock:borrow:book:{bookId}:{copyId})
2. 校验用户状态和借阅数量限制
3. 调用图书服务扣减库存
4. 更新用户借阅数
5. 创建借阅记录
6. 发送 RabbitMQ 事件
7. 释放分布式锁
```

**系统设置**：
| 设置项 | 默认值 | 说明 |
|--------|--------|------|
| default_borrow_days | 30 | 默认借阅天数 |
| max_borrow_count | 5 | 最大借阅数量 |
| overdue_fine_per_day | 0.5 | 逾期罚款（元/天） |
| max_fine_amount | 50 | 最高罚款金额 |

### 搜索服务 (search-service, 端口 8084)

**功能**：
- Elasticsearch 全文检索：书名/作者/摘要分词搜索
- 高亮显示：搜索结果关键词高亮
- 热门图书：按借阅量排序
- 多条件筛选：分类、状态、关键词组合查询

**ES 索引结构**：
```json
{
  "books": {
    "mappings": {
      "properties": {
        "id": { "type": "long" },
        "title": { "type": "text", "analyzer": "ik_max_word" },
        "author": { "type": "text", "analyzer": "ik_max_word" },
        "isbn": { "type": "keyword" },
        "summary": { "type": "text", "analyzer": "ik_max_word" },
        "categoryId": { "type": "long" },
        "categoryName": { "type": "keyword" },
        "availableCopies": { "type": "integer" },
        "borrowCount": { "type": "integer" },
        "status": { "type": "keyword" }
      }
    }
  }
}
```

### 通知服务 (notify-service, 端口 8085)

**功能**：
- RabbitMQ 消息消费：监听借阅事件
- 消息通知：站内通知、短信、邮件
- 延迟消息：到期提醒、逾期提醒

**消息队列配置**：
| 交换机 | 路由键 | 说明 |
|--------|--------|------|
| borrow.exchange | borrow.success | 借书成功 |
| borrow.exchange | borrow.return | 还书成功 |
| borrow.exchange | borrow.overdue | 逾期提醒 |
| notify.exchange | notify.sms | 短信通知 |
| notify.exchange | notify.email | 邮件通知 |

### 统计服务 (stats-service, 端口 8086)

**功能**：
- 借阅统计：日/周/月/年借阅趋势
- 热门图书：借阅量排行榜
- 用户统计：活跃用户、新用户统计
- ECharts 数据接口：图表数据标准化输出

**统计接口**：
| 接口 | 说明 |
|------|------|
| GET /api/stats/dashboard | 仪表盘统计数据 |
| GET /api/stats/borrow-trend | 借阅趋势（按日期范围） |
| GET /api/stats/hot-books | 热门图书排行 |
| GET /api/stats/category-stats | 分类统计 |
| GET /api/stats/user-stats | 用户统计 |

---

## API 概览

### 认证模块

#### 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}

Response:
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "username": "admin",
      "role": "admin",
      "activeBorrowCount": 0
    }
  }
}
```

#### 刷新 Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 用户注册
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "password": "123456",
  "email": "user@example.com",
  "phone": "13800138000"
}
```

### 图书模块

#### 获取图书列表
```http
GET /api/books?page=1&size=10&keyword=Java&categoryId=1&status=available

Response:
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "title": "Java编程思想",
        "author": "Bruce Eckel",
        "isbn": "9787111213826",
        "categoryName": "计算机",
        "availableCopies": 5,
        "totalCopies": 10,
        "coverImage": "http://..."
      }
    ],
    "total": 100,
    "size": 10,
    "current": 1
  }
}
```

#### 获取图书详情
```http
GET /api/books/{id}

Response:
{
  "code": 200,
  "data": {
    "id": 1,
    "title": "Java编程思想",
    "author": "Bruce Eckel",
    "isbn": "9787111213826",
    "publisher": "机械工业出版社",
    "publishDate": "2007-01-01",
    "pages": 880,
    "summary": "本书赢得了全球程序员的广泛赞誉...",
    "categoryName": "计算机",
    "availableCopies": 5,
    "totalCopies": 10,
    "borrowCount": 150,
    "coverImage": "http://..."
  }
}
```

### 借阅模块

#### 借书
```http
POST /api/borrow
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "bookId": 1,
  "copyId": 1
}

Response:
{
  "code": 200,
  "message": "借阅成功",
  "data": {
    "recordId": 1001,
    "userId": 1,
    "bookId": 1,
    "borrowDate": "2024-01-15",
    "dueDate": "2024-02-14",
    "status": "active"
  }
}
```

#### 还书
```http
POST /api/return
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "borrowId": 1001
}

Response:
{
  "code": 200,
  "message": "归还成功",
  "data": {
    "recordId": 1001,
    "returnDate": "2024-01-25",
    "overdueDays": 0,
    "fineAmount": 0.00,
    "status": "已归还"
  }
}
```

#### 获取我的借阅记录
```http
GET /api/my-borrows?status=active
Authorization: Bearer {accessToken}

Response:
{
  "code": 200,
  "data": [
    {
      "id": 1001,
      "bookTitle": "Java编程思想",
      "borrowDate": "2024-01-15",
      "dueDate": "2024-02-14",
      "status": "active"
    }
  ]
}
```

### 搜索模块

#### 全文搜索
```http
GET /api/search?keyword=Java&page=1&size=10&highlight=true

Response:
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "title": "<em>Java</em>编程思想",
        "author": "Bruce Eckel",
        "summary": "本书赢得了全球<em>Java</em>程序员的广泛赞誉...",
        "highlightFields": ["title", "summary"]
      }
    ],
    "total": 25,
    "page": 1,
    "size": 10
  }
}
```

### 统计模块

#### 获取仪表盘数据
```http
GET /api/stats/dashboard
Authorization: Bearer {accessToken}

Response:
{
  "code": 200,
  "data": {
    "totalBooks": 1500,
    "totalUsers": 850,
    "activeBorrows": 120,
    "todayBorrows": 15,
    "todayReturns": 8,
    "overdueCount": 5
  }
}
```

---

## 部署指南

### 环境要求

| 组件 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 4 核 | 8 核+ |
| 内存 | 8 GB | 16 GB+ |
| 磁盘 | 50 GB | 100 GB+ SSD |
| 操作系统 | Linux / Windows / macOS | Linux (生产环境) |

### 生产环境部署

#### 1. 准备服务器

确保服务器已安装：
- Docker 20.10+
- Docker Compose 2.0+

#### 2. 克隆项目
```bash
git clone <repository-url>
cd softwarearchitecture
```

#### 3. 修改生产环境配置

编辑 `docker/docker-compose.yml`，根据需要修改：
- 数据库密码
- JWT 密钥
- 服务副本数
- 资源限制

#### 4. 创建数据目录
```bash
mkdir -p /data/mysql /data/redis /data/elasticsearch /data/nacos
```

#### 5. 启动服务
```bash
cd docker

# 启动中间件
docker-compose up -d mysql redis rabbitmq elasticsearch nacos

# 等待中间件初始化（约 2-3 分钟）
sleep 180

# 编译后端
cd ../backend
mvn clean package -DskipTests

# 启动所有服务
cd ../docker
docker-compose up -d
```

#### 6. 配置反向代理（可选）

使用 Nginx 作为前端反向代理：
```nginx
upstream api-gateway {
    server 127.0.0.1:8080;
}

server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /usr/share/nginx/html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://api-gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### 高可用部署

#### 1. 数据库高可用
- MySQL 主从复制
- 或使用云数据库 RDS

#### 2. Redis 高可用
- Redis Sentinel 模式
- 或 Redis Cluster

#### 3. 服务高可用
- 多实例部署 + Nacos 负载均衡
- Kubernetes 部署（推荐）

---

## 监控与治理

### Sentinel 限流熔断

**访问控制台**：http://localhost:8858

**默认限流规则**：
| 资源名 | 限流类型 | QPS |
|--------|----------|-----|
| /api/borrow | QPS | 100 |
| /api/return | QPS | 100 |
| /api/books | QPS | 200 |
| /api/search | QPS | 500 |

**熔断规则**：
| 资源名 | 异常比例 | 熔断时长 |
|--------|----------|----------|
| 所有接口 | 50% | 60秒 |

### Prometheus + Grafana 监控

**访问 Grafana**：http://localhost:3001

**默认用户名密码**：admin / admin123

**监控指标**：
| 指标类型 | 说明 |
|----------|------|
| JVM 指标 | 堆内存、非堆内存、GC、线程 |
| HTTP 指标 | 请求数、响应时间、错误率 |
| 数据库指标 | 连接池、活跃连接 |
| 缓存指标 | Redis 命中率、命令执行 |
| 业务指标 | 借阅次数、还书次数 |

**推荐仪表盘**：
1. JVM (Micrometer) Dashboard - ID: 4701
2. Spring Boot Statistics Dashboard - ID: 10280
3. MySQL Dashboard - ID: 7362
4. Redis Dashboard - ID: 763
5. RabbitMQ Dashboard - ID: 10991

### 日志管理

**日志级别配置**：
```yaml
logging:
  level:
    com.library: DEBUG
    org.springframework: INFO
    com.alibaba.csp.sentinel: INFO
```

**日志文件位置**：
- Docker 环境：容器内 `/app/logs`
- 本地环境：项目目录下 `logs`

**建议使用 ELK 栈**：
- Filebeat：日志收集
- Logstash：日志处理
- Elasticsearch：日志存储
- Kibana：日志可视化

---

## 开发指南

### 环境搭建

#### 1. IDE 配置

推荐使用 IntelliJ IDEA 或 VS Code：
- 安装 Lombok 插件
- 配置 JDK 17
- 配置 Maven

#### 2. 代码规范

- Java 代码遵循阿里巴巴 Java 开发规范
- 使用统一的代码格式化配置
- 提交前运行 `mvn checkstyle:check`

### 新增服务模块

1. **在 backend 目录下创建新模块目录**
```
backend/new-service/
├── pom.xml
└── src/
    └── main/
        ├── java/com/library/newservice/
        │   ├── controller/
        │   ├── service/
        │   ├── entity/
        │   ├── dto/
        │   ├── mapper/
        │   └── NewServiceApplication.java
        └── resources/
            └── application.yml
```

2. **在父 pom.xml 中添加模块**
```xml
<modules>
    ...
    <module>new-service</module>
</modules>
```

3. **创建 pom.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.library</groupId>
        <artifactId>library-management</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>new-service</artifactId>
    <description>新服务</description>

    <dependencies>
        <dependency>
            <groupId>com.library</groupId>
            <artifactId>common</artifactId>
        </dependency>
        <!-- 其他依赖 -->
    </dependencies>
</project>
```

### 接口规范

#### 统一返回格式
```java
// 成功
Result.success(data);
Result.success("操作成功", data);

// 失败
Result.error(ResultCode.PARAM_ERROR);
Result.error(400, "参数错误");

// 分页
PageResult.of(list, total, size, current);
```

#### 异常处理
```java
// 业务异常
throw new BusinessException(ResultCode.PARAM_ERROR, "参数错误");

// 全局异常处理
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.error(ResultCode.SYSTEM_ERROR);
    }
}
```

### 测试规范

#### 单元测试
```java
@SpringBootTest
class BookServiceTest {
    
    @Autowired
    private BookService bookService;
    
    @MockBean
    private BookInfoMapper bookInfoMapper;
    
    @Test
    void testGetBookById() {
        // Given
        BookInfo book = new BookInfo();
        book.setId(1L);
        book.setTitle("Test Book");
        
        when(bookInfoMapper.selectById(1L)).thenReturn(book);
        
        // When
        BookInfo result = bookService.getById(1L);
        
        // Then
        assertEquals("Test Book", result.getTitle());
    }
}
```

#### 集成测试
使用 TestContainers 进行集成测试：
```java
@Testcontainers
@SpringBootTest
class BorrowServiceIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @Container
    static RedisContainer<?> redis = new RedisContainer<>("redis:7");
}
```

### Git 提交规范

遵循 Conventional Commits 规范：

| 类型 | 说明 |
|------|------|
| feat | 新功能 |
| fix | 修复 bug |
| docs | 文档更新 |
| style | 代码格式 |
| refactor | 重构 |
| test | 测试 |
| chore | 构建/工具 |

**示例**：
```bash
git commit -m "feat(borrow): 添加逾期自动提醒功能"
git commit -m "fix(user): 修复登录 Token 过期时间问题"
git commit -m "docs: 更新 README.md"
```

---

## 常见问题

### Q1: 服务启动失败，连接不上 MySQL

**原因**：MySQL 未完全启动或连接配置错误

**解决**：
1. 检查 MySQL 是否运行正常
2. 确认数据库名称和用户权限
3. 检查防火墙和网络配置

### Q2: Redis 分布式锁不生效

**原因**：Redis 未配置或连接超时

**解决**：
1. 检查 Redis 服务状态
2. 确认 `spring.data.redis` 配置
3. 检查锁 Key 是否正确

### Q3: JWT Token 解析失败

**原因**：JWT 密钥不一致或 Token 过期

**解决**：
1. 确认所有服务使用相同的 `jwt.secret`
2. 检查 Token 有效期配置
3. 使用刷新 Token 获取新的访问 Token

### Q4: Elasticsearch 连接失败

**原因**：ES 版本不兼容或安全配置问题

**解决**：
1. 确认 ES 版本（项目使用 8.11）
2. 检查 `xpack.security.enabled` 配置
3. 确认 ES 服务正常运行

### Q5: RabbitMQ 消息不消费

**原因**：队列配置错误或消费者未启动

**解决**：
1. 检查 RabbitMQ 管理界面确认队列存在
2. 确认消费者服务已启动
3. 检查交换器和路由键绑定

---

## 许可证

本项目仅供学习交流使用。

---

## 联系方式

如有问题或建议，请提交 Issue 或 PR。
