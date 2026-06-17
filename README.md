# 图书自动借书机系统

本项目是课程大作业的图书自动借书机系统。README 只写部署和运行方法；课程技术说明、架构说明、JMeter 可用性演示步骤见 [技术汇总报告.md](技术汇总报告.md)。

## 1. 项目结构

```text
SoftwareArchitecture
├── backend/                 # Spring Boot 多模块后端
│   ├── common/
│   ├── api-gateway/
│   ├── user-service/
│   ├── book-service/
│   ├── borrow-service/
│   ├── search-service/
│   ├── notify-service/
│   └── stats-service/
├── frontend/                # Vue 3 前端
├── database/init.sql        # MySQL 初始化脚本
├── docker/docker-compose.yml
├── docker/nginx.conf
└── demo/jmeter/             # JMeter 可用性演示计划
```

## 2. 环境要求

| 软件 | 建议版本 | 用途 |
|---|---:|---|
| JDK | 17+ | 后端编译运行 |
| IntelliJ IDEA | 2023+ | 使用内置 Maven 打包后端 |
| Node.js | 18+ | 前端构建 |
| Docker Desktop | 20.10+ | 运行 MySQL、Redis、Nacos、服务容器等 |
| Docker Compose | 2.x | 一键启动全部容器 |
| JMeter | 5.6.3 | 可选，用于答辩演示两个可用性场景 |

说明：Maven 只是构建工具，不计入课程要求的技术选型数量。如果本机没有单独安装 Maven，可以直接使用 IDEA 内置 Maven。

## 3. 拉取代码

```powershell
git clone <你的远程仓库地址>
cd SoftwareArchitecture
```

如果项目路径包含空格，PowerShell 里要加引号：

```powershell
cd "E:\Ai Code\SoftwareArchitecture"
```

## 4. 后端打包

后端 Dockerfile 会复制 `backend/*/target/*.jar` 到容器内，所以首次部署必须先生成 jar 包。

### 方式一：使用 IDEA 内置 Maven

1. 用 IDEA 打开项目根目录 `SoftwareArchitecture`。
2. 等待 IDEA 识别 `backend/pom.xml` 并完成 Maven 依赖导入。
3. 打开右侧 `Maven` 面板。
4. 找到根项目 `library-management`。
5. 展开 `Lifecycle`。
6. 先双击 `clean`。
7. 再双击 `package`。

```text
backend/api-gateway/target/api-gateway-1.0.0.jar
backend/user-service/target/user-service-1.0.0.jar
backend/book-service/target/book-service-1.0.0.jar
backend/borrow-service/target/borrow-service-1.0.0.jar
backend/search-service/target/search-service-1.0.0.jar
backend/notify-service/target/notify-service-1.0.0.jar
backend/stats-service/target/stats-service-1.0.0.jar
```

### 方式二：使用命令行 Maven

如果本机已安装 Maven 并配置了 PATH：

```powershell
cd backend
mvn clean package -DskipTests
cd ..
```

## 5. 前端构建

```powershell
cd frontend
npm install
npm run build
cd ..
```

构建产物会生成在：

```text
frontend/dist
```

Nginx 容器会把这个目录挂载为前端静态页面目录。

## 6. 启动系统

确认 Docker Desktop 已启动，然后在项目根目录执行：

```powershell
docker compose -f docker/docker-compose.yml up -d --build
```

查看容器状态：

```powershell
docker compose -f docker/docker-compose.yml ps
```

正常情况下应看到这些容器处于 `Up` 状态：

```text
library-nginx
library-api-gateway
library-user-service
library-book-service
library-borrow-service
library-search-service
library-notify-service
library-stats-service
library-mysql
library-redis
library-rabbitmq
library-elasticsearch
library-nacos
library-sentinel
library-prometheus
library-grafana
```

## 7. 访问地址

| 页面/服务 | 地址 | 说明 |
|---|---|---|
| 自助借书机 | `http://localhost` | 默认入口，地址栏保持根路径 |
| 管理后台 | `http://localhost/admin` | 图书、用户、借阅、统计管理 |
| 读者门户 | `http://localhost/portal` | 保留扩展功能，不作为主演示入口 |
| API 网关 | `http://localhost:8080` | 后端统一入口 |
| Nacos | `http://localhost:8848/nacos` | 服务注册发现 |
| Sentinel | `http://localhost:8858` | 限流熔断控制台 |
| RabbitMQ | `http://localhost:15672` | 消息队列管理 |
| Prometheus | `http://localhost:9090` | 指标采集 |
| Grafana | `http://localhost:3001` | 监控展示 |
| Elasticsearch | `http://localhost:9200` | 搜索引擎 |

## 8. 默认账号

| 系统 | 用户名 | 密码 | 说明 |
|---|---|---|---|
| 管理后台 | `admin` | `admin123` | 管理员账号 |
| 借书机读者 | `user1` | `123456` | 可在借书机身份验证中输入 `user1` |
| Nacos | `nacos` | `nacos` | 服务治理控制台 |
| Sentinel | `sentinel` | `sentinel123` | 限流熔断控制台 |
| RabbitMQ | `admin` | `admin123` | 消息队列控制台 |
| Grafana | `admin` | `admin123` | 监控面板 |

## 9. 健康检查

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost/ | Select-Object StatusCode
Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health
Invoke-WebRequest -UseBasicParsing http://localhost:8082/actuator/health
Invoke-WebRequest -UseBasicParsing http://localhost:8083/actuator/health
```

验证借书机扫码接口：

```powershell
Invoke-WebRequest -UseBasicParsing "http://localhost/api/books/scan?code=BC0024001"
```

## 10. JMeter 可用性演示

JMeter 不计入课程 12 项技术，只作为现场验证工具。测试计划在：

```text
demo/jmeter/borrow-concurrency.jmx
demo/jmeter/duplicate-click.jmx
demo/jmeter/reset-demo-copy.sql
```

如果 JMeter 安装在 `E:\apache-jmeter-5.6.3`，可以执行：

```powershell
Get-Content demo\jmeter\reset-demo-copy.sql | docker exec -i library-mysql mysql --default-character-set=utf8mb4 -uroot -proot123 library
& "E:\apache-jmeter-5.6.3\bin\jmeter.bat" -n -t demo\jmeter\borrow-concurrency.jmx -l demo\jmeter\borrow-concurrency.jtl -e -o demo\jmeter\report-concurrency

Get-Content demo\jmeter\reset-demo-copy.sql | docker exec -i library-mysql mysql --default-character-set=utf8mb4 -uroot -proot123 library
& "E:\apache-jmeter-5.6.3\bin\jmeter.bat" -n -t demo\jmeter\duplicate-click.jmx -l demo\jmeter\duplicate-click.jtl -e -o demo\jmeter\report-duplicate
```

更详细的演示说明见 [demo/jmeter/README.md](demo/jmeter/README.md) 和 [技术汇总报告.md](技术汇总报告.md)。

## 11. 常用运维命令

查看日志：

```powershell
docker logs -f library-nginx
docker logs -f library-api-gateway
docker logs -f library-borrow-service
docker logs -f library-book-service
```

重新加载 Nginx：

```powershell
docker exec library-nginx nginx -s reload
```

进入 MySQL：

```powershell
docker exec -it library-mysql mysql --default-character-set=utf8mb4 -uroot -proot123 library
```

检查借阅副本数据：

```sql
SELECT bc.copy_id, bc.book_id, bc.barcode, bc.status, bi.title, bi.available_copies
FROM book_copy bc
JOIN book_info bi ON bi.id = bc.book_id
WHERE bc.barcode = 'BC0024001';
```

停止服务：

```powershell
docker compose -f docker/docker-compose.yml down
```

停止并清空数据卷：

```powershell
docker compose -f docker/docker-compose.yml down -v
```

## 12. 更新代码后的重新部署

只修改前端：

```powershell
cd frontend
npm run build
cd ..
docker exec library-nginx nginx -s reload
```

修改后端：

```powershell
cd backend
mvn clean package -DskipTests
cd ..
docker compose -f docker/docker-compose.yml up -d --build
```

修改数据库初始化脚本 `database/init.sql` 后，如果要重新初始化数据，需要清空数据卷：

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build
```

## 13. 常见问题

### 13.1 Docker 构建时报找不到 jar

原因：没有先打包后端。

处理：用 IDEA 内置 Maven 执行 `clean` 和 `package`，或命令行执行：

```powershell
cd backend
mvn clean package -DskipTests
```

### 13.2 `mvn` 命令不存在

可以不单独安装 Maven，直接使用 IDEA 内置 Maven：

1. IDEA 打开项目。
2. 打开右侧 `Maven` 面板。
3. 选择根项目 `library-management`。
4. 依次执行 `clean`、`package`。

### 13.3 前端页面没有更新

先重新构建前端，再 reload Nginx：

```powershell
cd frontend
npm run build
cd ..
docker exec library-nginx nginx -s reload
```

### 13.4 端口被占用

本项目默认占用：

```text
80, 8080-8086, 8848, 8858, 9090, 9200, 9300, 3001, 3307, 5672, 6380, 15672
```

如果端口冲突，先停止占用端口的程序，或修改 `docker/docker-compose.yml` 中对应端口映射。

### 13.5 MySQL 数据没有重新初始化

MySQL 使用 Docker volume 持久化数据。只改 `database/init.sql` 不会自动重建旧数据。需要执行：

```powershell
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d --build
```
