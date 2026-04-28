# 运维文档

本文档详细描述图书管理系统的运维操作和故障处理流程。

---

## 目录

- [日常运维](#日常运维)
- [监控告警](#监控告警)
- [日志管理](#日志管理)
- [备份恢复](#备份恢复)
- [故障处理](#故障处理)
- [性能优化](#性能优化)
- [版本发布](#版本发布)
- [应急预案](#应急预案)

---

## 日常运维

### 服务状态检查

#### Docker Compose 环境

```bash
# 进入 docker 目录
cd /path/to/softwarearchitecture/docker

# 查看所有容器状态
docker-compose ps

# 查看所有容器状态（含停止的）
docker-compose ps -a

# 查看指定服务状态
docker-compose ps api-gateway
docker-compose ps user-service
```

#### Systemd 服务（传统部署）

```bash
# 查看所有服务状态
systemctl status library-*.service

# 查看指定服务状态
systemctl status library-gateway
systemctl status library-user

# 查看服务日志
journalctl -u library-gateway -f
journalctl -u library-user -f
```

#### Kubernetes 环境

```bash
# 查看 Pod 状态
kubectl get pods -n library

# 查看 Service 状态
kubectl get services -n library

# 查看 Deployment 状态
kubectl get deployments -n library

# 查看 Pod 详细信息
kubectl describe pod <pod-name> -n library

# 查看 Pod 日志
kubectl logs <pod-name> -n library
kubectl logs -f <pod-name> -n library
```

### 服务启停操作

#### Docker Compose

```bash
# 启动所有服务
docker-compose up -d

# 启动指定服务
docker-compose up -d api-gateway user-service

# 停止所有服务
docker-compose down

# 停止指定服务
docker-compose stop api-gateway

# 重启所有服务
docker-compose restart

# 重启指定服务
docker-compose restart api-gateway
```

#### Systemd

```bash
# 启动服务
systemctl start library-gateway
systemctl start library-user

# 停止服务
systemctl stop library-gateway
systemctl stop library-user

# 重启服务
systemctl restart library-gateway
systemctl restart library-user

# 重新加载配置
systemctl daemon-reload

# 开机自启
systemctl enable library-gateway
systemctl disable library-gateway
```

#### Kubernetes

```bash
# 重启 Deployment
kubectl rollout restart deployment/api-gateway -n library

# 扩容/缩容
kubectl scale deployment/api-gateway --replicas=3 -n library

# 更新镜像
kubectl set image deployment/api-gateway api-gateway=new-image:tag -n library

# 回滚
kubectl rollout undo deployment/api-gateway -n library
```

### 中间件状态检查

#### MySQL

```bash
# Docker 环境
docker exec -it library-mysql mysql -uroot -proot123

# 命令行连接
mysql -h localhost -P 3306 -u root -p

# 查看状态
SHOW STATUS;
SHOW PROCESSLIST;

# 查看主从状态
SHOW MASTER STATUS;
SHOW SLAVE STATUS\G

# 查看慢查询
SHOW VARIABLES LIKE 'slow_query%';
```

#### Redis

```bash
# Docker 环境
docker exec -it library-redis redis-cli

# 命令行连接
redis-cli -h localhost -p 6379

# 查看状态
INFO
INFO replication
INFO memory
INFO stats

# 查看连接数
CLIENT LIST

# 查看慢查询
SLOWLOG GET

# 持久化检查
BGSAVE
LASTSAVE
```

#### RabbitMQ

```bash
# 管理界面
# http://localhost:15672

# 命令行
docker exec -it library-rabbitmq rabbitmqctl status

# 查看队列
rabbitmqctl list_queues

# 查看连接
rabbitmqctl list_connections

# 查看消费者
rabbitmqctl list_consumers

# 查看交换机
rabbitmqctl list_exchanges
```

#### Elasticsearch

```bash
# 健康检查
curl -X GET "localhost:9200/_cluster/health?pretty"

# 节点信息
curl -X GET "localhost:9200/_cat/nodes?v"

# 索引列表
curl -X GET "localhost:9200/_cat/indices?v"

# 索引状态
curl -X GET "localhost:9200/_cat/indices/books?v"

# 查看分片
curl -X GET "localhost:9200/_cat/shards?v"
```

#### Nacos

```bash
# 管理界面
# http://localhost:8848/nacos

# 集群状态
curl -X GET "http://localhost:8848/nacos/v1/ns/operator/cluster/state"

# 服务列表
curl -X GET "http://localhost:8848/nacos/v1/ns/operator/service/list"

# 实例列表
curl -X GET "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=user-service"
```

---

## 监控告警

### Prometheus 监控

#### 常用查询

```promql
# JVM 内存使用
jvm_memory_used_bytes{application="user-service"}

# HTTP 请求数
http_server_requests_seconds_count{application="user-service"}

# HTTP 请求耗时 P95
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))

# 活跃线程数
jvm_threads_live_threads{application="user-service"}

# GC 次数
rate(jvm_gc_pause_seconds_count[5m])

# MySQL 连接数
hikaricp_connections_active{application="user-service"}

# Redis 命令执行数
rate(redis_commands_total[5m])
```

#### 监控面板

访问 Grafana：http://localhost:3001

**推荐导入的仪表盘**：

| ID | 名称 | 说明 |
|------|------|------|
| 4701 | JVM (Micrometer) | JVM 监控 |
| 10280 | Spring Boot Statistics | Spring Boot 监控 |
| 7362 | MySQL | MySQL 监控 |
| 763 | Redis | Redis 监控 |
| 10991 | RabbitMQ | RabbitMQ 监控 |
| 1860 | Node Exporter Full | 服务器监控 |

### 告警规则

创建 `docker/prometheus-alert-rules.yml`：

```yaml
groups:
  - name: service-alerts
    rules:
      # 服务实例下线告警
      - alert: ServiceInstanceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "服务实例下线"
          description: "实例 {{ $labels.instance }} 已下线超过 1 分钟"

      # JVM 内存告警
      - alert: JVMMemoryHigh
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "JVM 内存使用率过高"
          description: "服务 {{ $labels.application }} 内存使用率已达 {{ $value | humanizePercentage }}"

      # GC 频繁告警
      - alert: GCFrequent
        expr: rate(jvm_gc_pause_seconds_count[5m]) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "GC 频繁"
          description: "服务 {{ $labels.application }} GC 过于频繁"

      # HTTP 错误率告警
      - alert: HTTPErrorRateHigh
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application) 
              / sum(rate(http_server_requests_seconds_count[5m])) by (application) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "HTTP 错误率过高"
          description: "服务 {{ $labels.application }} 错误率已达 {{ $value | humanizePercentage }}"

  - name: database-alerts
    rules:
      # MySQL 连接数告警
      - alert: MySQLConnectionsHigh
        expr: mysql_global_status_threads_connected > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MySQL 连接数过高"
          description: "MySQL 连接数已达 {{ $value }}"

      # MySQL 慢查询告警
      - alert: MySQLSlowQueries
        expr: rate(mysql_global_status_slow_queries[5m]) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "MySQL 慢查询"
          description: "MySQL 存在慢查询"

  - name: middleware-alerts
    rules:
      # Redis 内存告警
      - alert: RedisMemoryHigh
        expr: redis_memory_used_bytes / redis_memory_max_bytes > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis 内存使用率过高"
          description: "Redis 内存使用率已达 {{ $value | humanizePercentage }}"

      # Redis 连接数告警
      - alert: RedisConnectionsHigh
        expr: redis_connected_clients > 500
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Redis 连接数过高"
          description: "Redis 连接数已达 {{ $value }}"
```

### 告警通知

#### AlertManager 配置

创建 `docker/alertmanager.yml`：

```yaml
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'default-receiver'
  routes:
    - match:
        severity: critical
      receiver: 'critical-alerts'
    - match:
        severity: warning
      receiver: 'warning-alerts'

receivers:
  - name: 'default-receiver'
    email_configs:
      - to: 'admin@example.com'
        send_resolved: true

  - name: 'critical-alerts'
    email_configs:
      - to: 'ops-team@example.com'
        send_resolved: true
    webhook_configs:
      - url: 'http://your-alert-service/api/alerts'
        send_resolved: true
    # 钉钉/企业微信配置
    # webhook_configs:
    #   - url: 'https://oapi.dingtalk.com/robot/send?access_token=xxx'

  - name: 'warning-alerts'
    email_configs:
      - to: 'dev-team@example.com'
        send_resolved: true

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'cluster', 'service']
```

---

## 日志管理

### 日志位置

#### Docker 环境

```bash
# 查看容器日志
docker-compose logs api-gateway
docker-compose logs -f api-gateway
docker-compose logs --tail=100 api-gateway

# 查看所有日志
docker-compose logs

# 导出日志
docker-compose logs api-gateway > logs/api-gateway.log
```

#### 传统部署

```bash
# Systemd 日志
journalctl -u library-gateway
journalctl -u library-gateway -f
journalctl -u library-gateway --since="2024-01-15"

# 应用日志文件
/opt/library-services/logs/
```

### 日志级别

#### 动态调整日志级别

```bash
# 查看当前日志级别
curl -X GET "http://localhost:8081/actuator/loggers"

# 调整指定包的日志级别
curl -X POST "http://localhost:8081/actuator/loggers/com.library" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

# 调整根日志级别
curl -X POST "http://localhost:8081/actuator/loggers/ROOT" \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "INFO"}'
```

### 日志分析

#### 常用日志命令

```bash
# 查看错误日志
grep -i "ERROR" logs/app.log
grep -i "Exception" logs/app.log

# 查看特定时间的日志
grep "2024-01-15 14:" logs/app.log

# 统计错误数量
grep -c "ERROR" logs/app.log

# 查看慢查询
grep "SlowQuery" logs/app.log

# 实时查看日志
tail -f logs/app.log

# 实时过滤错误
tail -f logs/app.log | grep -i "ERROR\|Exception"
```

#### ELK 日志收集（推荐）

使用 ELK 栈进行集中化日志管理：

- **Filebeat**：日志收集
- **Logstash**：日志处理
- **Elasticsearch**：日志存储
- **Kibana**：日志可视化

---

## 备份恢复

### MySQL 备份

#### 手动备份

```bash
# 全量备份
docker exec library-mysql mysqldump -u root -proot123 --all-databases > backup/all-databases-$(date +%Y%m%d).sql

# 指定库备份
docker exec library-mysql mysqldump -u root -proot123 library_db > backup/library-db-$(date +%Y%m%d).sql

# 仅备份结构
docker exec library-mysql mysqldump -u root -proot123 -d library_db > backup/library-db-schema.sql

# 仅备份数据
docker exec library-mysql mysqldump -u root -proot123 -t library_db > backup/library-db-data.sql
```

#### 自动备份脚本

创建 `scripts/mysql-backup.sh`：

```bash
#!/bin/bash

# 配置
BACKUP_DIR="/data/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
MYSQL_ROOT_PASSWORD="root123"
CONTAINER_NAME="library-mysql"
RETENTION_DAYS=7

# 创建备份目录
mkdir -p $BACKUP_DIR

# 执行备份
echo "开始备份 MySQL ..."
docker exec $CONTAINER_NAME mysqldump -u root -p$MYSQL_ROOT_PASSWORD --all-databases \
  > $BACKUP_DIR/mysql-all-databases-$DATE.sql

# 压缩备份文件
gzip $BACKUP_DIR/mysql-all-databases-$DATE.sql

# 删除过期备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete

# 备份完成
echo "备份完成: $BACKUP_DIR/mysql-all-databases-$DATE.sql.gz"
echo "备份文件大小: $(du -h $BACKUP_DIR/mysql-all-databases-$DATE.sql.gz | awk '{print $1}')"
```

添加定时任务：

```bash
# 编辑 crontab
crontab -e

# 添加每日凌晨 2 点执行备份
0 2 * * * /path/to/scripts/mysql-backup.sh >> /var/log/mysql-backup.log 2>&1
```

#### MySQL 恢复

```bash
# 恢复全量备份
gunzip < backup/mysql-all-databases-20240115_020000.sql.gz | docker exec -i library-mysql mysql -u root -proot123

# 恢复指定库
docker exec -i library-mysql mysql -u root -proot123 library_db < backup/library-db-20240115.sql

# 从容器内恢复
docker exec -it library-mysql mysql -u root -p

# 执行 SQL
source /path/to/backup.sql;
```

### Redis 备份

#### 手动备份

```bash
# 创建 RDB 快照
docker exec library-redis redis-cli BGSAVE

# 等待完成后查看
docker exec library-redis redis-cli LASTSAVE

# 复制备份文件
docker cp library-redis:/data/dump.rdb backup/redis-dump-$(date +%Y%m%d).rdb

# AOF 备份（如果启用）
docker cp library-redis:/data/appendonly.aof backup/redis-appendonly-$(date +%Y%m%d).aof
```

#### Redis 恢复

```bash
# 停止 Redis
docker-compose stop redis

# 备份当前数据（可选）
docker cp library-redis:/data/dump.rdb backup/dump.rdb.bak

# 恢复 RDB 文件
docker cp backup/redis-dump-20240115.rdb library-redis:/data/dump.rdb

# 重启 Redis
docker-compose start redis

# 验证数据
docker exec library-redis redis-cli KEYS "*"
```

### Elasticsearch 备份

#### 快照备份

```bash
# 创建快照仓库
curl -X PUT "localhost:9200/_snapshot/library_backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/usr/share/elasticsearch/backups",
    "compress": true
  }
}
'

# 创建快照
curl -X PUT "localhost:9200/_snapshot/library_backup/snapshot_$(date +%Y%m%d)?wait_for_completion=true" -H 'Content-Type: application/json' -d'
{
  "indices": "books",
  "include_global_state": false
}
'

# 查看快照
curl -X GET "localhost:9200/_snapshot/library_backup/_all"
```

#### 恢复快照

```bash
# 关闭索引
curl -X POST "localhost:9200/books/_close"

# 恢复快照
curl -X POST "localhost:9200/_snapshot/library_backup/snapshot_20240115/_restore" -H 'Content-Type: application/json' -d'
{
  "indices": "books"
}
'

# 打开索引
curl -X POST "localhost:9200/books/_open"

# 验证
curl -X GET "localhost:9200/books/_count"
```

### 配置备份

```bash
# 备份 Docker Compose 配置
cp -r docker/ backup/docker-$(date +%Y%m%d)/

# 备份 Nginx 配置
cp /etc/nginx/conf.d/library.conf backup/nginx-$(date +%Y%m%d).conf

# 备份应用配置
cp -r /opt/library-services/config/ backup/app-config-$(date +%Y%m%d)/
```

---

## 故障处理

### 常见故障排查

#### 服务无法启动

**现象**：容器启动后立即退出

**排查步骤**：
1. 查看容器状态：`docker-compose ps`
2. 查看容器日志：`docker-compose logs <service>`
3. 检查配置文件：`cat docker/docker-compose.yml`
4. 检查端口占用：`netstat -tlnp | grep <port>`

**常见原因**：
- 端口被占用
- 配置文件错误
- 依赖服务未启动
- 资源不足

**解决方案**：
```bash
# 查看端口占用
netstat -tlnp | grep 8080
lsof -i :8080

# 检查配置文件语法
docker-compose config

# 手动运行查看详细错误
docker run --rm -e SPRING_PROFILES_ACTIVE=test your-image
```

#### 数据库连接失败

**现象**：服务日志显示连接数据库超时

**排查步骤**：
1. 检查 MySQL 容器状态：`docker-compose ps mysql`
2. 检查 MySQL 日志：`docker-compose logs mysql`
3. 测试连接：`docker exec -it library-mysql mysql -u root -p`
4. 检查网络：`docker network ls`

**常见原因**：
- MySQL 未完全初始化
- 用户名/密码错误
- 网络隔离问题

**解决方案**：
```bash
# 等待 MySQL 初始化完成（通常需要 30-60 秒）
sleep 60

# 重启 MySQL 服务
docker-compose restart mysql

# 检查用户权限
docker exec -it library-mysql mysql -u root -p
> SELECT user, host FROM mysql.user;
> SHOW GRANTS FOR 'library'@'%';
```

#### Redis 连接失败

**现象**：服务无法连接 Redis

**排查步骤**：
1. 检查 Redis 状态：`docker-compose ps redis`
2. 测试连接：`docker exec -it library-redis redis-cli ping`
3. 检查密码配置：`docker exec library-redis redis-cli CONFIG GET requirepass`

**常见原因**：
- Redis 未启动
- 密码不匹配
- 网络问题

**解决方案**：
```bash
# 重启 Redis
docker-compose restart redis

# 检查 Redis 配置
docker exec -it library-redis redis-cli info server
```

#### Elasticsearch 连接失败

**现象**：搜索服务无法连接 ES

**排查步骤**：
1. 检查 ES 健康状态：`curl http://localhost:9200/_cluster/health`
2. 检查 ES 日志：`docker-compose logs elasticsearch`
3. 检查端口：`curl http://localhost:9200`

**常见原因**：
- 内存不足（ES 需要至少 2GB 堆内存）
- 权限问题
- 索引不存在

**解决方案**：
```bash
# 增加 Docker 内存（Windows/Mac）
# Docker Desktop -> Settings -> Resources -> Memory

# 检查 ES 内存设置
docker exec library-elasticsearch cat config/jvm.options | grep -E "^-X"

# 重建索引
curl -X POST "http://localhost:8084/api/search/reindex"
```

### 性能问题排查

#### 服务响应慢

**排查步骤**：
1. 查看服务 CPU/内存：`docker stats`
2. 查看 JVM 状态：访问 `/actuator/metrics/jvm.memory.used`
3. 查看数据库连接池：`/actuator/metrics/hikaricp.connections.active`
4. 检查慢查询日志

**解决方案**：
```bash
# 查看容器资源使用
docker stats

# 查看 JVM 堆内存
curl http://localhost:8081/actuator/metrics/jvm.memory.used?tag=area:heap

# 查看 GC 情况
curl http://localhost:8081/actuator/metrics/jvm.gc.pause

# 查看线程数
curl http://localhost:8081/actuator/metrics/jvm.threads.live
```

#### 数据库慢查询

**排查步骤**：
1. 开启慢查询日志
2. 分析慢查询日志
3. 查看执行计划
4. 添加索引优化

**解决方案**：
```sql
-- 查看慢查询配置
SHOW VARIABLES LIKE '%slow_query%';

-- 临时开启慢查询（生产环境需谨慎）
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;  -- 超过 1 秒的查询

-- 查看慢查询
SELECT * FROM mysql.slow_log;

-- 分析执行计划
EXPLAIN SELECT * FROM borrow_record WHERE user_id = 1;

-- 添加索引
CREATE INDEX idx_borrow_user ON borrow_record(user_id);
CREATE INDEX idx_borrow_book ON borrow_record(book_id);
CREATE INDEX idx_borrow_date ON borrow_record(borrow_date);
```

#### Redis 性能问题

**排查步骤**：
1. 查看 Redis 性能指标
2. 检查内存使用
3. 分析慢查询
4. 检查 key 分布

**解决方案**：
```bash
# 查看 Redis 信息
redis-cli info
redis-cli info memory
redis-cli info stats

# 查看慢查询
redis-cli slowlog get 100

# 查看大 key
redis-cli --bigkeys

# 查看内存使用分布
redis-cli info keyspace

# 清理过期 key
redis-cli info stats | grep expired_keys
```

### 高可用故障转移

#### MySQL 主从切换

**故障场景**：主库宕机

**操作步骤**：
1. 确认主库不可用
2. 选择一个从库提升为主库
3. 修改从库连接配置
4. 更新应用配置

```sql
-- 1. 在从库上执行，停止复制
STOP SLAVE;

-- 2. 查看从库状态
SHOW SLAVE STATUS\G

-- 3. 重置主从关系
RESET MASTER;

-- 4. 其他从库切换到新主库
CHANGE MASTER TO
  MASTER_HOST='new-master-ip',
  MASTER_USER='repl',
  MASTER_PASSWORD='password';

START SLAVE;
```

#### Redis Sentinel 故障转移

Redis Sentinel 会自动处理故障转移，无需手动操作。

**查看集群状态**：
```bash
redis-cli -p 26379 sentinel master mymaster
redis-cli -p 26379 sentinel slaves mymaster
```

---

## 性能优化

### 应用优化

#### JVM 参数调优

```bash
# 开发环境
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 生产环境
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 带 GC 日志
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -Xlog:gc*:file=logs/gc.log:time,uptime:filecount=5,filesize=100m"
```

#### 连接池优化

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
```

#### Redis 连接池优化

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 100
          max-idle: 50
          min-idle: 10
          max-wait: 3000ms
```

### 数据库优化

#### MySQL 配置优化

```ini
# /etc/mysql/my.cnf

[mysqld]
# 基础配置
port = 3306
datadir = /var/lib/mysql
socket = /var/run/mysqld/mysqld.sock

# 字符集
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci

# 连接配置
max_connections = 500
max_connect_errors = 1000
wait_timeout = 600
interactive_timeout = 600

# 缓冲区配置
key_buffer_size = 256M
max_allowed_packet = 64M
table_open_cache = 2048
sort_buffer_size = 2M
read_buffer_size = 2M
read_rnd_buffer_size = 8M
join_buffer_size = 8M

# InnoDB 配置
innodb_buffer_pool_size = 2G
innodb_log_file_size = 256M
innodb_log_buffer_size = 64M
innodb_flush_log_at_trx_commit = 2
innodb_lock_wait_timeout = 120
innodb_file_per_table = 1

# 慢查询
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 1

# 查询缓存（MySQL 8.0 已移除）
# query_cache_type = 1
# query_cache_size = 64M
```

#### 索引优化

```sql
-- 用户表
CREATE INDEX idx_user_username ON user(username);
CREATE INDEX idx_user_email ON user(email);
CREATE INDEX idx_user_status ON user(status);

-- 图书表
CREATE INDEX idx_book_title ON book_info(title);
CREATE INDEX idx_book_author ON book_info(author);
CREATE INDEX idx_book_isbn ON book_info(isbn);
CREATE INDEX idx_book_category ON book_info(category_id);
CREATE INDEX idx_book_status ON book_info(status);

-- 借阅记录表（重点优化）
CREATE INDEX idx_borrow_user ON borrow_record(user_id);
CREATE INDEX idx_borrow_book ON borrow_record(book_id);
CREATE INDEX idx_borrow_copy ON borrow_record(copy_id);
CREATE INDEX idx_borrow_date ON borrow_record(borrow_date);
CREATE INDEX idx_borrow_due ON borrow_record(due_date);
CREATE INDEX idx_borrow_return ON borrow_record(return_date);
CREATE INDEX idx_borrow_status ON borrow_record(status);

-- 复合索引
CREATE INDEX idx_borrow_user_status ON borrow_record(user_id, status);
CREATE INDEX idx_borrow_due_status ON borrow_record(due_date, status);
```

### 缓存优化

#### Redis 缓存策略

| 数据类型 | 缓存 Key | 过期时间 | 更新策略 |
|----------|----------|----------|----------|
| 图书列表 | book:list:{page}:{size} | 60s | 写时更新 |
| 图书详情 | book:info:{id} | 120s | 写时更新 |
| 用户信息 | user:info:{id} | 300s | 写时更新 |
| 搜索热词 | search:hot:words | 600s | 定时更新 |

#### 缓存穿透保护

```java
// 空值缓存
public BookInfo getBookById(Long id) {
    String key = "book:info:" + id;
    
    // 1. 从缓存获取
    String value = redisUtil.get(key);
    if (value != null) {
        // 空值缓存
        if (NULL_VALUE.equals(value)) {
            return null;
        }
        return JSON.parseObject(value, BookInfo.class);
    }
    
    // 2. 从数据库获取
    BookInfo book = bookMapper.selectById(id);
    
    // 3. 写入缓存（包括空值）
    if (book == null) {
        redisUtil.setEx(key, NULL_VALUE, 60);  // 空值缓存 60 秒
    } else {
        redisUtil.setEx(key, JSON.toJSONString(book), 120);
    }
    
    return book;
}
```

#### 缓存击穿保护

```java
// 互斥锁
public BookInfo getBookByIdWithLock(Long id) {
    String key = "book:info:" + id;
    String lockKey = "lock:book:" + id;
    
    // 1. 从缓存获取
    String value = redisUtil.get(key);
    if (value != null) {
        return JSON.parseObject(value, BookInfo.class);
    }
    
    // 2. 获取互斥锁
    RedisLock.LockResult lockResult = redisLock.tryLock(lockKey, 3, 10, TimeUnit.SECONDS);
    
    try {
        if (!lockResult.isLocked()) {
            // 获取锁失败，休眠重试
            Thread.sleep(100);
            return getBookByIdWithLock(id);
        }
        
        // 3. 再次查询缓存（双重检查）
        value = redisUtil.get(key);
        if (value != null) {
            return JSON.parseObject(value, BookInfo.class);
        }
        
        // 4. 从数据库获取
        BookInfo book = bookMapper.selectById(id);
        
        // 5. 写入缓存
        if (book != null) {
            redisUtil.setEx(key, JSON.toJSONString(book), 120);
        }
        
        return book;
        
    } finally {
        redisLock.unlock(lockResult);
    }
}
```

---

## 版本发布

### 发布流程

```
开发环境 → 测试环境 → 预发布环境 → 生产环境
```

### 版本号规范

```
主版本号.次版本号.修订号
示例：1.0.0, 1.1.0, 1.1.1

- 主版本号：不兼容的 API 修改
- 次版本号：向下兼容的功能新增
- 修订号：向下兼容的问题修复
```

### 发布步骤

#### 1. 代码准备

```bash
# 拉取最新代码
git checkout main
git pull origin main

# 查看提交记录
git log --oneline -10

# 或切换到指定版本
git checkout v1.0.0
```

#### 2. 编译打包

```bash
# 后端编译
cd backend
mvn clean package -DskipTests

# 前端构建
cd frontend
npm run build
```

#### 3. 构建镜像

```bash
# 构建后端镜像
cd backend

docker build -f Dockerfile.gateway -t library/api-gateway:1.0.0 .
docker build -f Dockerfile.user -t library/user-service:1.0.0 .
docker build -f Dockerfile.book -t library/book-service:1.0.0 .
docker build -f Dockerfile.borrow -t library/borrow-service:1.0.0 .

# 推送镜像（如果使用镜像仓库）
docker push library/api-gateway:1.0.0
```

#### 4. 预发布验证

```bash
# 切换到预发布配置
cd docker
cp docker-compose.yml docker-compose.staging.yml

# 修改镜像版本
# image: library/api-gateway:1.0.0

# 启动预发布环境
docker-compose -f docker-compose.staging.yml up -d

# 验证功能
# 1. 检查服务状态
# 2. 执行接口测试
# 3. 验证核心功能
```

#### 5. 生产发布

**滚动发布（推荐）**：

```bash
# Kubernetes 滚动更新
kubectl set image deployment/api-gateway api-gateway=library/api-gateway:1.0.0 -n library

# 查看更新状态
kubectl rollout status deployment/api-gateway -n library

# 如需回滚
kubectl rollout undo deployment/api-gateway -n library
```

**Docker Compose 蓝绿发布**：

```bash
# 蓝环境（当前运行）
docker-compose -p blue up -d

# 绿环境（新版本）
docker-compose -p green up -d

# 切换流量
# 修改 Nginx 配置或负载均衡配置

# 验证后停止蓝环境
docker-compose -p blue down
```

#### 6. 发布验证

```bash
# 检查服务状态
docker-compose ps
kubectl get pods -n library

# 健康检查
curl http://localhost:8080/actuator/health

# 功能测试
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

### 回滚方案

#### 快速回滚

```bash
# Docker Compose 回滚
docker-compose down
docker-compose pull  # 如果使用镜像
docker-compose up -d

# Kubernetes 回滚
kubectl rollout undo deployment/api-gateway -n library

# 或指定版本回滚
kubectl rollout undo deployment/api-gateway --to-revision=1 -n library
```

#### 数据库回滚

```bash
# 恢复数据库备份
gunzip < backup/mysql-all-databases-20240114.sql.gz | docker exec -i library-mysql mysql -u root -proot123
```

### 发布检查清单

- [ ] 代码已合并到主分支
- [ ] 所有测试通过
- [ ] 代码审查完成
- [ ] 数据库变更脚本准备就绪
- [ ] 回滚方案准备就绪
- [ ] 配置文件已更新
- [ ] 镜像已构建并推送
- [ ] 预发布验证通过
- [ ] 发布文档已更新
- [ ] 相关人员已通知

---

## 应急预案

### 应急预案概览

| 场景 | 影响 | 优先级 | 响应时间 |
|------|------|--------|----------|
| 数据库宕机 | 高 | P0 | 5 分钟 |
| 核心服务宕机 | 高 | P0 | 5 分钟 |
| 缓存服务宕机 | 中 | P1 | 15 分钟 |
| 性能严重下降 | 中 | P1 | 15 分钟 |
| 部分功能异常 | 低 | P2 | 30 分钟 |

### P0 级故障处理（数据库宕机）

**故障现象**：所有服务无法连接数据库

**处理流程**：

1. **确认故障**（1 分钟）
   ```bash
   # 检查数据库状态
   docker-compose ps mysql
   docker-compose logs mysql
   
   # 尝试连接
   docker exec -it library-mysql mysql -u root -p
   ```

2. **启动应急方案**（3 分钟）
   - 如果有从库，切换到从库
   - 如果有备份，准备恢复

3. **尝试重启**（2 分钟）
   ```bash
   docker-compose restart mysql
   
   # 等待启动
   sleep 30
   
   # 验证
   docker exec library-mysql mysql -u root -proot123 -e "SELECT 1"
   ```

4. **如果无法启动，执行恢复**（根据备份大小）
   ```bash
   # 找到最近的备份
   ls -lt backup/ | head -5
   
   # 恢复备份
   gunzip < backup/mysql-all-databases-20240114_020000.sql.gz | \
     docker exec -i library-mysql mysql -u root -proot123
   ```

5. **验证恢复**（2 分钟）
   - 检查表是否存在
   - 验证数据完整性
   - 重启应用服务

6. **通知相关人员**
   - 技术团队
   - 业务团队

### P0 级故障处理（核心服务宕机）

**故障现象**：API 网关或用户服务无法访问

**处理流程**：

1. **确认故障**（1 分钟）
   ```bash
   # 检查服务状态
   docker-compose ps
   
   # 检查日志
   docker-compose logs api-gateway
   docker-compose logs user-service
   
   # 健康检查
   curl http://localhost:8080/actuator/health
   ```

2. **尝试重启**（2 分钟）
   ```bash
   # 重启单个服务
   docker-compose restart api-gateway
   
   # 或重启所有服务
   docker-compose restart
   
   # 等待启动
   sleep 10
   ```

3. **检查资源**（2 分钟）
   ```bash
   # 检查系统资源
   docker stats
   
   # 检查端口占用
   netstat -tlnp | grep 8080
   
   # 检查磁盘空间
   df -h
   ```

4. **快速扩容（如果是性能问题）**（3 分钟）
   ```bash
   # Kubernetes 环境
   kubectl scale deployment/api-gateway --replicas=5 -n library
   
   # 或增加资源限制
   # 修改 docker-compose.yml，增加内存限制
   ```

5. **回滚版本（如果是新版本问题）**（5 分钟）
   ```bash
   # 查看历史版本
   kubectl rollout history deployment/api-gateway -n library
   
   # 回滚到上一个版本
   kubectl rollout undo deployment/api-gateway -n library
   ```

6. **验证恢复**（2 分钟）
   - 服务状态正常
   - 接口可访问
   - 核心功能可用

### P1 级故障处理（缓存服务宕机）

**故障现象**：Redis 服务不可用，服务降级

**处理流程**：

1. **确认故障**（1 分钟）
   ```bash
   # 检查 Redis 状态
   docker-compose ps redis
   
   # 测试连接
   docker exec -it library-redis redis-cli ping
   
   # 查看日志
   docker-compose logs redis
   ```

2. **评估影响**（1 分钟）
   - 系统是否启用了缓存降级
   - 数据库压力是否可控
   - 是否需要限流

3. **尝试重启**（2 分钟）
   ```bash
   docker-compose restart redis
   
   # 等待启动
   sleep 5
   
   # 验证
   docker exec library-redis redis-cli ping
   ```

4. **如果无法启动，执行恢复**（5 分钟）
   ```bash
   # 检查备份
   ls backup/redis-*.rdb
   
   # 恢复 RDB 备份
   docker stop library-redis
   docker cp backup/redis-dump-20240114.rdb library-redis:/data/dump.rdb
   docker start library-redis
   ```

5. **监控数据库压力**（持续）
   - 开启慢查询日志
   - 监控连接数
   - 必要时限流

### 通用故障排查流程图

```
发现故障
    │
    ▼
确认故障现象
    │
    ├── 服务是否响应？ ──否──► 检查端口/进程/资源
    │         │
    │         是
    │         │
    ▼         ▼
查看日志错误
    │
    ▼
定位问题类型
    │
    ├── 配置错误 ──► 修正配置/重启服务
    │
    ├── 资源不足 ──► 扩容/释放资源
    │
    ├── 网络问题 ──► 检查网络/防火墙/DNS
    │
    ├── 数据库问题 ──► 检查连接/执行计划/索引
    │
    └── 依赖服务故障 ──► 检查依赖服务状态
                        │
                        ├── 可恢复 ──► 恢复依赖服务
                        │
                        └── 不可恢复 ──► 启用降级/切换备用
                                      │
                                      ▼
                                验证系统恢复
                                      │
                                      ▼
                                记录故障分析报告
```

---

## 附录

### A. 常用命令速查

#### Docker Compose

```bash
# 查看状态
docker-compose ps
docker-compose ps -a

# 查看日志
docker-compose logs
docker-compose logs -f
docker-compose logs --tail=100 <service>

# 启动/停止
docker-compose up -d
docker-compose down
docker-compose start
docker-compose stop
docker-compose restart

# 进入容器
docker-compose exec <service> bash
docker-compose exec <service> sh
```

#### MySQL

```bash
# 连接
mysql -h localhost -P 3306 -u root -p

# 常用命令
SHOW DATABASES;
SHOW TABLES;
DESCRIBE <table>;
SHOW PROCESSLIST;
SHOW STATUS;
SHOW VARIABLES LIKE '%<keyword>%';

# 备份
mysqldump -u root -p database > backup.sql

# 恢复
mysql -u root -p database < backup.sql
```

#### Redis

```bash
# 连接
redis-cli -h localhost -p 6379

# 常用命令
INFO
INFO server
INFO memory
INFO stats

# KEY 操作
KEYS *
SCAN 0
TYPE <key>
TTL <key>

# 字符串操作
GET <key>
SET <key> <value>
SET EX <key> <seconds> <value>
DEL <key>

# 集合操作
SADD <key> <value>
SMEMBERS <key>

# 哈希操作
HGETALL <key>
HSET <key> <field> <value>

# 持久化
BGSAVE
SAVE
LASTSAVE
```

### B. 联系人列表

| 角色 | 姓名 | 联系方式 |
|------|------|----------|
| 技术负责人 | - | - |
| 运维负责人 | - | - |
| DBA | - | - |
| 后端开发 | - | - |
| 前端开发 | - | - |

### C. 外部依赖

| 服务 | 提供商 | 联系方式 |
|------|--------|----------|
| 云服务器 | - | - |
| 域名 DNS | - | - |
| CDN | - | - |
| 短信服务 | - | - |
| 邮件服务 | - | - |

---

本文档解释权归运维团队所有。
