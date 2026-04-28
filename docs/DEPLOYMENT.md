# 部署文档

本文档详细说明图书管理系统的各种部署方式。

---

## 目录

- [环境要求](#环境要求)
- [快速部署（Docker Compose）](#快速部署docker-compose)
- [手动部署](#手动部署)
- [Kubernetes 部署](#kubernetes-部署)
- [高可用部署](#高可用部署)
- [配置说明](#配置说明)
- [常见问题](#常见问题)

---

## 环境要求

### 硬件要求

| 环境 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 开发环境 | 4 核 | 8 GB | 50 GB |
| 测试环境 | 8 核 | 16 GB | 100 GB |
| 生产环境 | 16 核+ | 32 GB+ | 200 GB+ SSD |

### 软件要求

| 软件 | 版本 | 说明 |
|------|------|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | 2.0+ | 容器编排 |
| JDK | 17+ | Java 开发环境 |
| Maven | 3.8+ | 项目构建工具 |
| Node.js | 18+ | 前端构建环境 |
| npm/yarn | - | 前端包管理 |

### 操作系统支持

- Linux (推荐生产环境)
  - Ubuntu 20.04+
  - CentOS 8+
  - Debian 11+
- Windows 10+ (开发环境)
- macOS 11+ (开发环境)

---

## 快速部署（Docker Compose）

这是最简单的部署方式，适合开发和测试环境。

### 1. 准备环境

```bash
# 检查 Docker 版本
docker --version

# 检查 Docker Compose 版本
docker compose version

# 确认 Docker 运行正常
docker run hello-world
```

### 2. 获取代码

```bash
# 克隆项目
git clone <repository-url>
cd softwarearchitecture

# 或使用已有的代码目录
cd /path/to/softwarearchitecture
```

### 3. 配置环境变量（可选）

创建 `docker/.env` 文件：

```bash
# MySQL 配置
MYSQL_ROOT_PASSWORD=your_secure_password
MYSQL_DATABASE=library
MYSQL_USER=library
MYSQL_PASSWORD=library_password

# Redis 配置（如需密码）
REDIS_PASSWORD=your_redis_password

# JWT 密钥（生产环境必须修改）
JWT_SECRET=your_super_secret_jwt_key_at_least_256_bits_long

# Nacos 配置
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos_password
```

### 4. 启动中间件

```bash
cd docker

# 启动中间件容器
docker-compose up -d mysql redis rabbitmq elasticsearch nacos sentinel-dashboard

# 查看容器状态
docker-compose ps

# 查看日志（可选）
docker-compose logs -f mysql
```

**等待中间件初始化完成**：
- MySQL: 约 30-60 秒
- Elasticsearch: 约 1-2 分钟
- Nacos: 约 30-60 秒

### 5. 验证中间件

```bash
# 验证 MySQL
docker exec -it library-mysql mysql -uroot -proot123 -e "SELECT 1"

# 验证 Redis
docker exec -it library-redis redis-cli ping

# 验证 Elasticsearch
curl http://localhost:9200

# 验证 Nacos（浏览器访问）
# http://localhost:8848/nacos
# 用户名: nacos, 密码: nacos
```

### 6. 编译后端项目

```bash
cd ../backend

# 编译所有模块
mvn clean package -DskipTests

# 或编译指定模块
mvn clean package -pl common,user-service -am -DskipTests
```

编译成功后，各服务的 `target` 目录下会生成 jar 包。

### 7. 启动所有服务

```bash
cd ../docker

# 启动所有服务（包括后端微服务）
docker-compose up -d

# 查看所有容器状态
docker-compose ps

# 查看特定服务日志
docker-compose logs -f api-gateway
docker-compose logs -f user-service
```

### 8. 验证服务

```bash
# 检查 API 网关健康状态
curl http://localhost:8080/actuator/health

# 检查用户服务
curl http://localhost:8081/actuator/health

# 登录测试
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

### 9. 访问各控制台

| 服务 | 地址 | 用户名 | 密码 |
|------|------|--------|------|
| Nacos 控制台 | http://localhost:8848/nacos | nacos | nacos |
| Sentinel 控制台 | http://localhost:8858 | sentinel | sentinel123 |
| RabbitMQ 管理界面 | http://localhost:15672 | admin | admin123 |
| Prometheus | http://localhost:9090 | - | - |
| Grafana | http://localhost:3001 | admin | admin123 |

### 10. 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（慎用！会删除所有数据）
docker-compose down -v

# 停止特定服务
docker-compose stop api-gateway
```

---

## 手动部署

适用于需要精细化控制的生产环境。

### 1. 安装依赖软件

#### MySQL 8.0

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install mysql-server-8.0

# CentOS/RHEL
sudo dnf install mysql-server

# 启动并设置开机自启
sudo systemctl start mysql
sudo systemctl enable mysql

# 安全配置
sudo mysql_secure_installation
```

创建数据库和用户：

```sql
-- 登录 MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER 'library'@'%' IDENTIFIED BY 'your_password';

-- 授权
GRANT ALL PRIVILEGES ON library_db.* TO 'library'@'%';
FLUSH PRIVILEGES;

-- 初始化表结构
source /path/to/database/init.sql;
```

#### Redis 7.x

```bash
# Ubuntu/Debian
sudo apt install redis-server

# CentOS/RHEL
sudo dnf install redis

# 启动并设置开机自启
sudo systemctl start redis
sudo systemctl enable redis

# 验证安装
redis-cli ping
```

配置 Redis（编辑 `/etc/redis/redis.conf`）：

```conf
bind 0.0.0.0
protected-mode no
maxmemory 1gb
maxmemory-policy allkeys-lru
```

#### RabbitMQ 3.x

```bash
# Ubuntu/Debian
sudo apt install rabbitmq-server

# CentOS/RHEL
sudo dnf install rabbitmq-server

# 启动并设置开机自启
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server

# 启用管理界面
sudo rabbitmq-plugins enable rabbitmq_management

# 创建管理员用户
sudo rabbitmqctl add_user admin your_password
sudo rabbitmqctl set_user_tags admin administrator
sudo rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
```

访问管理界面：http://localhost:15672

#### Elasticsearch 8.x

```bash
# 导入 GPG 密钥
wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | sudo gpg --dearmor -o /usr/share/keyrings/elasticsearch-keyring.gpg

# 添加源
echo "deb [signed-by=/usr/share/keyrings/elasticsearch-keyring.gpg] https://artifacts.elastic.co/packages/8.x/apt stable main" | sudo tee /etc/apt/sources.list.d/elastic.list

# 安装
sudo apt update
sudo apt install elasticsearch

# 配置（编辑 /etc/elasticsearch/elasticsearch.yml）
cluster.name: library-cluster
node.name: node-1
network.host: 0.0.0.0
discovery.type: single-node
xpack.security.enabled: false

# 启动
sudo systemctl start elasticsearch
sudo systemctl enable elasticsearch

# 验证
curl http://localhost:9200
```

#### Nacos 2.x

```bash
# 下载
wget https://github.com/alibaba/nacos/releases/download/2.2.3/nacos-server-2.2.3.tar.gz

# 解压
tar -zxf nacos-server-2.2.3.tar.gz
cd nacos

# 单机模式启动
bin/startup.sh -m standalone

# 访问
# http://localhost:8848/nacos
# 默认账号: nacos / nacos
```

#### JDK 17

```bash
# Ubuntu/Debian
sudo apt install openjdk-17-jdk

# CentOS/RHEL
sudo dnf install java-17-openjdk-devel

# 验证
java -version
```

配置环境变量：

```bash
# 编辑 ~/.bashrc 或 /etc/profile
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# 生效
source ~/.bashrc
```

#### Maven 3.8+

```bash
# 下载
wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz

# 解压
sudo tar -zxf apache-maven-3.9.6-bin.tar.gz -C /opt

# 配置环境变量
export MAVEN_HOME=/opt/apache-maven-3.9.6
export PATH=$MAVEN_HOME/bin:$PATH

# 验证
mvn -version
```

配置 Maven 镜像（编辑 `~/.m2/settings.xml`）：

```xml
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
  </mirrors>
</settings>
```

### 2. 编译项目

```bash
# 进入后端目录
cd backend

# 编译
mvn clean package -DskipTests

# 检查生成的 jar 包
ls -la */target/*.jar
```

### 3. 部署各服务

建议创建统一的部署目录：

```bash
# 创建部署目录
sudo mkdir -p /opt/library-services/{logs,config}

# 复制 jar 包
sudo cp backend/*/target/*.jar /opt/library-services/
```

创建启动脚本 `/opt/library-services/start.sh`：

```bash
#!/bin/bash

SERVICE_DIR=/opt/library-services
LOG_DIR=$SERVICE_DIR/logs

# 确保日志目录存在
mkdir -p $LOG_DIR

# JVM 参数
JVM_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 启动函数
start_service() {
    local jar=$1
    local name=$2
    local port=$3
    
    echo "Starting $name on port $port..."
    
    nohup java $JVM_OPTS \
        -Dserver.port=$port \
        -jar $SERVICE_DIR/$jar \
        --spring.profiles.active=prod \
        > $LOG_DIR/$name.log 2>&1 &
    
    echo $! > $LOG_DIR/$name.pid
    echo "$name started with PID: $(cat $LOG_DIR/$name.pid)"
}

# 按顺序启动
start_service "user-service-1.0.0.jar" "user-service" 8081
sleep 5

start_service "book-service-1.0.0.jar" "book-service" 8082
sleep 5

start_service "borrow-service-1.0.0.jar" "borrow-service" 8083
sleep 5

start_service "search-service-1.0.0.jar" "search-service" 8084
sleep 5

start_service "notify-service-1.0.0.jar" "notify-service" 8085
sleep 5

start_service "stats-service-1.0.0.jar" "stats-service" 8086
sleep 5

start_service "api-gateway-1.0.0.jar" "api-gateway" 8080

echo "All services started!"
```

创建停止脚本 `/opt/library-services/stop.sh`：

```bash
#!/bin/bash

LOG_DIR=/opt/library-services/logs

stop_service() {
    local name=$1
    local pid_file=$LOG_DIR/$name.pid
    
    if [ -f $pid_file ]; then
        local pid=$(cat $pid_file)
        echo "Stopping $name (PID: $pid)..."
        kill $pid 2>/dev/null || true
        rm -f $pid_file
        echo "$name stopped"
    else
        echo "$name not running"
    fi
}

# 按相反顺序停止
stop_service "api-gateway"
stop_service "stats-service"
stop_service "notify-service"
stop_service "search-service"
stop_service "borrow-service"
stop_service "book-service"
stop_service "user-service"

echo "All services stopped!"
```

创建状态检查脚本 `/opt/library-services/status.sh`：

```bash
#!/bin/bash

LOG_DIR=/opt/library-services/logs

check_service() {
    local name=$1
    local port=$2
    local pid_file=$LOG_DIR/$name.pid
    
    if [ -f $pid_file ]; then
        local pid=$(cat $pid_file)
        if ps -p $pid > /dev/null 2>&1; then
            # 检查端口
            if netstat -tlnp 2>/dev/null | grep -q ":$port "; then
                echo "[OK] $name (PID: $pid, Port: $port)"
            else
                echo "[WARN] $name running but port $port not listening"
            fi
        else
            echo "[DEAD] $name (PID file exists but process not running)"
            rm -f $pid_file
        fi
    else
        echo "[STOPPED] $name"
    fi
}

echo "Service Status:"
echo "==============="
check_service "api-gateway" 8080
check_service "user-service" 8081
check_service "book-service" 8082
check_service "borrow-service" 8083
check_service "search-service" 8084
check_service "notify-service" 8085
check_service "stats-service" 8086
```

赋予执行权限：

```bash
sudo chmod +x /opt/library-services/*.sh
```

### 4. 配置 Systemd 服务（推荐）

为每个服务创建 Systemd 单元文件，实现开机自启和自动重启。

**用户服务** `/etc/systemd/system/library-user.service`：

```ini
[Unit]
Description=Library User Service
After=network.target mysql.service redis.service

[Service]
Type=simple
User=library
Group=library
WorkingDirectory=/opt/library-services
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar user-service-1.0.0.jar
Restart=always
RestartSec=10

# 日志
StandardOutput=journal
StandardError=journal

# 环境变量
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="MYSQL_HOST=localhost"
Environment="REDIS_HOST=localhost"
Environment="NACOS_SERVER_ADDR=localhost:8848"

[Install]
WantedBy=multi-user.target
```

**API 网关** `/etc/systemd/system/library-gateway.service`：

```ini
[Unit]
Description=Library API Gateway
After=network.target library-user.service library-book.service library-borrow.service

[Service]
Type=simple
User=library
Group=library
WorkingDirectory=/opt/library-services
ExecStart=/usr/bin/java -Xms512m -Xmx1g -jar api-gateway-1.0.0.jar
Restart=always
RestartSec=10

StandardOutput=journal
StandardError=journal

Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="REDIS_HOST=localhost"
Environment="NACOS_SERVER_ADDR=localhost:8848"

[Install]
WantedBy=multi-user.target
```

创建专用用户：

```bash
# 创建运行用户
sudo useradd -r -s /sbin/nologin library

# 设置权限
sudo chown -R library:library /opt/library-services
```

启用并启动服务：

```bash
# 重载配置
sudo systemctl daemon-reload

# 启用开机自启
sudo systemctl enable library-user
sudo systemctl enable library-book
sudo systemctl enable library-borrow
sudo systemctl enable library-gateway

# 启动服务
sudo systemctl start library-user
sudo systemctl start library-book
sudo systemctl start library-borrow
sudo systemctl start library-gateway

# 查看状态
sudo systemctl status library-user

# 查看日志
sudo journalctl -u library-user -f
```

### 5. 部署前端

```bash
cd frontend

# 安装依赖
npm install

# 构建生产版本
npm run build

# 构建产物在 dist 目录
ls -la dist/
```

使用 Nginx 部署前端：

```nginx
# /etc/nginx/conf.d/library.conf

upstream api_gateway {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    root /var/www/library-frontend;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript 
               application/x-javascript application/xml+rss 
               application/javascript application/json;

    # 前端路由（SPA 支持）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://api_gateway;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
        
        # 超时设置
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

复制前端文件并重启 Nginx：

```bash
# 创建目录
sudo mkdir -p /var/www/library-frontend

# 复制构建产物
sudo cp -r frontend/dist/* /var/www/library-frontend/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

---

## Kubernetes 部署

### 1. 准备 Kubernetes 集群

确保已安装：
- Kubernetes 1.24+
- Helm 3.8+
- kubectl 配置正确

### 2. 安装依赖组件

#### MySQL

```bash
# 使用 Helm 安装 MySQL
helm repo add bitnami https://charts.bitnami.com/bitnami

helm install mysql bitnami/mysql \
  --namespace library \
  --create-namespace \
  --set auth.rootPassword=root123 \
  --set auth.database=library_db \
  --set auth.username=library \
  --set auth.password=library123 \
  --set primary.persistence.size=20Gi
```

#### Redis

```bash
helm install redis bitnami/redis \
  --namespace library \
  --set auth.password=redis123 \
  --set master.persistence.size=10Gi
```

#### RabbitMQ

```bash
helm install rabbitmq bitnami/rabbitmq \
  --namespace library \
  --set auth.username=admin \
  --set auth.password=admin123 \
  --set plugins=rabbitmq_management \
  --set persistence.size=10Gi
```

#### Elasticsearch

```bash
helm install elasticsearch bitnami/elasticsearch \
  --namespace library \
  --set master.replicas=1 \
  --set data.replicas=1 \
  --set security.enabled=false \
  --set master.persistence.size=30Gi \
  --set data.persistence.size=30Gi
```

#### Nacos

```bash
# 或使用官方 Nacos Helm Chart
```

### 3. 创建 Kubernetes 部署文件

**命名空间** `k8s/namespace.yaml`：

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: library
  labels:
    name: library
```

**配置映射** `k8s/configmap.yaml`：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: library-config
  namespace: library
data:
  SPRING_PROFILES_ACTIVE: "k8s"
  MYSQL_HOST: "mysql.library.svc.cluster.local"
  MYSQL_PORT: "3306"
  MYSQL_DATABASE: "library_db"
  REDIS_HOST: "redis-master.library.svc.cluster.local"
  REDIS_PORT: "6379"
  RABBITMQ_HOST: "rabbitmq.library.svc.cluster.local"
  RABBITMQ_PORT: "5672"
  ELASTICSEARCH_HOSTS: "elasticsearch-coordinating-only.library.svc.cluster.local:9200"
  NACOS_SERVER_ADDR: "nacos.library.svc.cluster.local:8848"
```

**用户服务部署** `k8s/user-service.yaml`：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: library
  labels:
    app: user-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: your-registry/library/user-service:1.0.0
        ports:
        - containerPort: 8081
        envFrom:
        - configMapRef:
            name: library-config
        env:
        - name: MYSQL_USERNAME
          valueFrom:
            secretKeyRef:
              name: mysql
              key: username
        - name: MYSQL_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql
              key: password
        resources:
          requests:
            cpu: "250m"
            memory: "256Mi"
          limits:
            cpu: "500m"
            memory: "512Mi"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: library
  labels:
    app: user-service
spec:
  ports:
  - port: 8081
    targetPort: 8081
  selector:
    app: user-service
```

**API 网关部署** `k8s/api-gateway.yaml`：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
  namespace: library
  labels:
    app: api-gateway
spec:
  replicas: 2
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: your-registry/library/api-gateway:1.0.0
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: library-config
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "1000m"
            memory: "1Gi"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
  namespace: library
  labels:
    app: api-gateway
spec:
  type: ClusterIP
  ports:
  - port: 8080
    targetPort: 8080
  selector:
    app: api-gateway
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: library-ingress
  namespace: library
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: library.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend
            port:
              number: 80
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: api-gateway
            port:
              number: 8080
```

### 4. 构建并推送镜像

```bash
# 登录镜像仓库
docker login your-registry

# 构建镜像（使用 Dockerfile）
cd backend

# 构建用户服务
docker build -f Dockerfile.user -t your-registry/library/user-service:1.0.0 .

# 构建其他服务...

# 推送镜像
docker push your-registry/library/user-service:1.0.0
```

### 5. 部署到 Kubernetes

```bash
# 创建命名空间
kubectl apply -f k8s/namespace.yaml

# 创建配置
kubectl apply -f k8s/configmap.yaml

# 部署各服务
kubectl apply -f k8s/user-service.yaml
kubectl apply -f k8s/book-service.yaml
kubectl apply -f k8s/api-gateway.yaml

# 查看部署状态
kubectl get pods -n library
kubectl get services -n library
kubectl get ingress -n library

# 查看日志
kubectl logs -f deployment/user-service -n library
```

---

## 高可用部署

### 1. 数据库高可用

#### MySQL 主从复制

**主库配置** `/etc/mysql/mysql.conf.d/master.cnf`：

```ini
[mysqld]
server-id=1
log-bin=mysql-bin
binlog-format=ROW
binlog-do-db=library_db
```

**从库配置** `/etc/mysql/mysql.conf.d/slave.cnf`：

```ini
[mysqld]
server-id=2
relay-log=relay-bin
log-slave-updates=1
read-only=1
```

配置主从同步：

```sql
-- 主库创建复制用户
CREATE USER 'repl'@'%' IDENTIFIED BY 'repl_password';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;

-- 查看主库状态
SHOW MASTER STATUS;

-- 从库配置
CHANGE MASTER TO
  MASTER_HOST='master-ip',
  MASTER_USER='repl',
  MASTER_PASSWORD='repl_password',
  MASTER_LOG_FILE='mysql-bin.000001',
  MASTER_LOG_POS=1234;

-- 启动从库
START SLAVE;

-- 查看从库状态
SHOW SLAVE STATUS\G
```

#### 或使用云数据库

生产环境推荐使用云服务商提供的数据库服务：
- 阿里云 RDS
- 腾讯云 CDB
- AWS RDS
- Google Cloud SQL

### 2. Redis 高可用

#### Redis Sentinel 模式

架构：1 主 + 2 从 + 3 Sentinel

**Sentinel 配置** `sentinel.conf`：

```conf
port 26379
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel auth-pass mymaster redis123
sentinel down-after-milliseconds mymaster 30000
sentinel parallel-syncs mymaster 1
sentinel failover-timeout mymaster 180000
```

启动 Sentinel：

```bash
redis-sentinel sentinel.conf
```

#### Redis Cluster 模式

适用于大规模场景，多主多从架构。

### 3. 服务高可用

#### 多实例部署

通过 Docker Compose 或 Kubernetes 部署多个实例：

```yaml
# Kubernetes Deployment
spec:
  replicas: 3  # 3 个实例
```

#### 负载均衡

- **Kubernetes**：使用 Service + Ingress
- **传统部署**：使用 Nginx 或 HAProxy

**Nginx 负载均衡配置**：

```nginx
upstream user_service {
    least_conn;
    server 192.168.1.101:8081 max_fails=3 fail_timeout=30s;
    server 192.168.1.102:8081 max_fails=3 fail_timeout=30s;
    server 192.168.1.103:8081 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    
    location /api/ {
        proxy_pass http://user_service;
        proxy_next_upstream error timeout invalid_header http_500 http_502 http_503 http_504;
    }
}
```

### 4. 完整高可用架构

```
                    ┌─────────────────┐
                    │   负载均衡器     │
                    │  (Nginx/ALB)    │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
   ┌──────────┐         ┌──────────┐        ┌──────────┐
   │API Gateway│         │API Gateway│        │API Gateway│
   │  (8080)  │         │  (8080)  │        │  (8080)  │
   └────┬─────┘         └────┬─────┘        └────┬─────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
   ┌──────────┐         ┌──────────┐        ┌──────────┐
   │User Svc  │         │User Svc  │        │User Svc  │
   │ (8081)   │         │ (8081)   │        │ (8081)   │
   └──────────┘         └──────────┘        └──────────┘
   
   (其他服务同样多实例部署)
   
   
┌─────────────────────────────────────────────────────────┐
│                      中间件层                            │
├─────────────────────────────────────────────────────────┤
│  MySQL 主从复制          │  Redis Sentinel 集群         │
│  (主库 + 从库 + 从库)    │  (1主 + 2从 + 3 Sentinel)   │
├─────────────────────────────────────────────────────────┤
│  RabbitMQ 集群           │  Elasticsearch 集群          │
│  (3节点镜像模式)          │  (3主 + 3从)                 │
├─────────────────────────────────────────────────────────┤
│  Nacos 集群              │  Prometheus + Grafana        │
│  (3节点)                 │  监控告警                      │
└─────────────────────────────────────────────────────────┘
```

---

## 配置说明

### 数据库配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| 主机 | MYSQL_HOST | localhost | MySQL 服务器地址 |
| 端口 | MYSQL_PORT | 3306 | MySQL 端口 |
| 数据库 | MYSQL_DATABASE | library_db | 数据库名称 |
| 用户名 | MYSQL_USERNAME | root | 数据库用户 |
| 密码 | MYSQL_PASSWORD | - | 数据库密码 |

### Redis 配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| 主机 | REDIS_HOST | localhost | Redis 服务器地址 |
| 端口 | REDIS_PORT | 6379 | Redis 端口 |
| 密码 | REDIS_PASSWORD | - | Redis 密码 |
| 数据库 | REDIS_DATABASE | 0 | 数据库索引 |

### JWT 配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| 密钥 | JWT_SECRET | 见代码 | JWT 签名密钥 |
| 访问令牌有效期 | - | 2小时 | Access Token |
| 刷新令牌有效期 | - | 7天 | Refresh Token |

**生产环境必须修改 JWT 密钥**：

```bash
# 生成安全的密钥
openssl rand -base64 64
```

### 系统设置

| 设置项 | 配置键 | 默认值 | 说明 |
|--------|--------|--------|------|
| 默认借阅天数 | default_borrow_days | 30 | 天 |
| 最大借阅数量 | max_borrow_count | 5 | 本 |
| 逾期罚款 | overdue_fine_per_day | 0.5 | 元/天 |
| 最高罚款 | max_fine_amount | 50 | 元 |

---

## 常见问题

### Q1: 服务启动失败，数据库连接超时

**症状**：
```
Caused by: java.net.SocketTimeoutException: connect timed out
```

**可能原因**：
1. 数据库未启动或未完全初始化
2. 防火墙阻止连接
3. 连接地址或端口错误

**解决方案**：
```bash
# 1. 检查数据库是否运行
docker ps | grep mysql
# 或
systemctl status mysql

# 2. 测试连接
telnet localhost 3306
# 或
nc -zv localhost 3306

# 3. 检查防火墙
sudo ufw status  # Ubuntu/Debian
sudo firewall-cmd --list-ports  # CentOS/RHEL

# 4. 开放端口（如需要）
sudo ufw allow 3306
sudo firewall-cmd --add-port=3306/tcp --permanent
```

### Q2: Redis 分布式锁不生效

**症状**：
- 高并发场景下出现超借
- 错误日志显示获取锁失败

**可能原因**：
1. Redis 未配置持久化，重启后数据丢失
2. 锁超时时间设置不合理
3. Redis 主从切换导致锁丢失

**解决方案**：
```bash
# 1. 检查 Redis 持久化配置
grep -E "save|appendonly" /etc/redis/redis.conf

# 2. 推荐配置
# RDB 持久化
save 900 1
save 300 10
save 60 10000

# AOF 持久化（可选，更安全但性能稍低）
appendonly yes
appendfsync everysec

# 3. 生产环境使用 RedLock 或 Redis Cluster
```

### Q3: Nacos 服务注册失败

**症状**：
```
failed to req API: /nacos/v1/ns/instance
```

**可能原因**：
1. Nacos 未启动
2. 网络不通
3. Nacos 地址配置错误

**解决方案**：
```bash
# 1. 检查 Nacos 状态
docker ps | grep nacos
# 或查看进程
ps aux | grep nacos

# 2. 测试 Nacos 连接
curl http://localhost:8848/nacos/

# 3. 检查服务配置
# 确保 spring.cloud.nacos.discovery.server-addr 配置正确
```

### Q4: Elasticsearch 内存不足

**症状**：
- ES 容器不断重启
- 日志显示 `java.lang.OutOfMemoryError`

**解决方案**：
```bash
# 1. 增加 Docker 内存（Windows/Mac）
# Docker Desktop -> Settings -> Resources -> Memory

# 2. 调整 ES JVM 堆内存
# 在 docker-compose.yml 中设置：
environment:
  - ES_JAVA_OPTS=-Xms512m -Xmx512m

# 3. 生产环境建议至少 2GB 堆内存
```

### Q5: 前端页面刷新 404

**症状**：
- 直接访问路由页面（如 `/books`）返回 404
- 首页点击路由正常

**原因**：
- SPA 单页应用的路由需要服务器端支持

**解决方案**：

**Nginx 配置**：
```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

**Kubernetes Ingress 配置**：
```yaml
annotations:
  nginx.ingress.kubernetes.io/rewrite-target: /
  nginx.ingress.kubernetes.io/try-files: $uri $uri/ /index.html
```

### Q6: Docker Compose 启动顺序问题

**症状**：
- 服务启动时报错数据库不存在
- 中间件未完全初始化

**解决方案**：

1. **分步启动**：
```bash
# 先启动中间件
docker-compose up -d mysql redis rabbitmq elasticsearch nacos

# 等待初始化（建议 2-3 分钟）
sleep 180

# 再启动应用服务
docker-compose up -d
```

2. **使用健康检查**：

在 `docker-compose.yml` 中添加健康检查依赖：

```yaml
services:
  user-service:
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
```

### Q7: 端口冲突

**症状**：
```
Bind for 0.0.0.0:8080 failed: port is already allocated
```

**解决方案**：

1. **查找占用端口的进程**：
```bash
# Windows
netstat -ano | findstr :8080

# Linux/macOS
lsof -i :8080
```

2. **修改端口配置**：

编辑 `docker/docker-compose.yml`：
```yaml
services:
  api-gateway:
    ports:
      - "9080:8080"  # 修改外部端口
```

或修改应用配置 `application.yml`：
```yaml
server:
  port: 9080
```

---

## 部署检查清单

### 部署前

- [ ] 所有依赖软件已安装正确版本
- [ ] 网络端口已开放（防火墙/安全组）
- [ ] 数据库已创建并授权
- [ ] 环境变量配置正确
- [ ] JWT 密钥已修改为安全值
- [ ] 敏感配置已使用环境变量或配置中心

### 部署后

- [ ] 所有容器/服务状态正常
- [ ] 各服务健康检查通过 (`/actuator/health`)
- [ ] Nacos 服务列表显示所有服务
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] 前端页面可访问
- [ ] API 接口可正常调用
- [ ] 登录功能正常
- [ ] 借书/还书功能正常
- [ ] 监控系统正常运行

### 生产环境额外检查

- [ ] 数据库已配置主从复制
- [ ] Redis 已配置 Sentinel 或 Cluster
- [ ] 所有服务至少 2 个实例
- [ ] 负载均衡配置正确
- [ ] SSL 证书已配置（HTTPS）
- [ ] 日志已配置集中收集
- [ ] 监控告警已配置
- [ ] 备份策略已实施

---

## 参考资料

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Cloud Alibaba 官方文档](https://github.com/alibaba/spring-cloud-alibaba)
- [Docker 官方文档](https://docs.docker.com/)
- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Nacos 官方文档](https://nacos.io/zh-cn/docs/)
