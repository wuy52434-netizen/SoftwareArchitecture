# RabbitMQ 消息中间件专项测试

对图书自动借书机系统的消息链路做专项验证，覆盖**拓扑一致性、投递路由、消息契约、死信队列、幂等与重复消费**五个维度。

## 被测拓扑

来源：`backend/notify-service/src/main/java/com/library/notify/config/RabbitMQConfig.java`、
`backend/common/src/main/java/com/library/common/constant/Constants.java`。

| 类型 | 名称 | 说明 |
|---|---|---|
| Exchange | `borrow.exchange` | topic，借还书事件主交换机 |
| Exchange | `notify.exchange` | direct，通知分发 |
| Exchange | `stats.exchange` | fanout，统计广播 |
| Queue | `queue.borrow.success` | 借书成功（配了 DLX） |
| Queue | `queue.borrow.fail` | 借书失败 |
| Queue | `queue.return.success` | 还书成功 |
| Queue | `queue.notify.sms` / `queue.notify.email` / `queue.notify.inner` | 短信 / 邮件 / 站内信 |
| Queue | `queue.stats.daily` | 每日统计 |
| RoutingKey | `borrow.success` / `borrow.return` / `borrow.fail` | 生产端实际发送的键 |

生产端：`BorrowService.sendBorrowEvent()` → `rabbitTemplate.convertAndSend(borrow.exchange, borrow.success|borrow.return, event)`。

## 前置条件

```bash
# 1. 起 broker（含 Management API 15672，拓扑断言依赖它）
docker compose -f docker/docker-compose.yml up -d rabbitmq

# 2. 起消费端，让 Spring AMQP 声明拓扑
#    notify-service 声明通知相关队列与绑定；stats-service 声明统计队列
```

浏览器打开 <http://127.0.0.1:15672>（guest/guest）确认拓扑已声明。

broker 或 Management API 不可达时，用例会**自动跳过**而不是报假失败。

## 运行

```bash
pip install -r requirements.txt
pytest -v
pytest -v --html=report.html --self-contained-html
pytest -v -k dead_letter          # 只跑死信相关用例

# 连接非本机 broker
RABBITMQ_HOST=10.0.0.5 RABBITMQ_MGMT_URL=http://10.0.0.5:15672 pytest -v
```

## 目录结构

```
test/mq/
├── mq_helper.py            # 连接、只读内省、投递、临时队列
├── conftest.py             # broker / mgmt / clean_queues 夹具
├── test_topology.py        # 拓扑与绑定一致性（7 条）
├── test_routing.py         # 投递与路由，含负向用例（5 条）
├── test_message_contract.py# 事件报文契约（7 条）
├── test_dead_letter.py     # 死信队列（3 条）
└── test_idempotency.py     # 幂等与重复消费（3 条）
```

## 设计要点（为什么这么写）

1. **拓扑断言只读获取**。测试不调用 `queue_declare` 去"帮忙"声明业务拓扑。
   如果测试自己声明，就会把被测系统缺的队列补上，缺陷被永久掩盖。
2. **路由验证用临时队列，不消费业务队列**。业务队列上挂着
   notify-service / stats-service 的消费者，直接 `basic_get` 会与它们抢消息，
   断言随机失败。临时队列（exclusive + auto_delete）只订阅、不抢，退出即删。
3. **每条正向用例配一条负向用例**。`test_unmatched_routing_key_is_not_delivered`
   保证测试真的在区分路由键，而不是"发什么都断言通过"。
4. **已知缺陷用 `xfail(strict=True)` 标记**，套件保持绿色，同时缺陷被固化成
   可执行的文档 —— 一旦有人修好，strict 模式会让它变成 XPASS 并失败，提醒更新用例。

## 已定位缺陷

| 编号 | 严重度 | 问题 | 证据 |
|---|---|---|---|
| **KNWN-MQ-01** | P1 | `queue.borrow.success` 声明了 `x-dead-letter-exchange=""` + `x-dead-letter-routing-key="dead.letter.queue"`，但 `dead.letter.queue` **从未被声明**。消息被死信转发到默认交换机后无队列承接，broker 以 `basic.return` 退回；生产端未处理 return，消息**静默丢失**。等于"配了死信机制但死信无处可去"。 | `RabbitMQConfig.java:62-67`；`test_dead_letter.py` 两条用例 |
| **KNWN-MQ-02** | P1 | `NotifyConsumerService.handleBorrowSuccess()` 用 `try/catch(Exception) + log.error` 包住整个处理逻辑。任何异常都被吞掉、消息照常 ACK，**既不重试也不进死信队列**，通知永久丢失且只在日志里留痕。 | `NotifyConsumerService.java:29-72` |
| **KNWN-MQ-03** | P2 | 借阅事件报文不含任何幂等键，消费端也不按 `recordId` 去重。RabbitMQ 是 at-least-once 语义，网络重连或消费者重投会**重复发送短信 / 邮件 / 站内信**。 | `BorrowService.sendBorrowEvent()` 报文字段；`test_idempotency.py` |

> KNWN-MQ-02 属于消费端代码逻辑问题，本套件通过 KNWN-MQ-01 的死信可达性用例
> 间接暴露其后果（失败消息无处可去）。修 KNWN-MQ-02 需要给 `@RabbitListener`
> 配 `errorHandler` 或 `defaultRequeueRejected=false` + 显式 DLX 声明。

## 修复建议

```java
// 1. 补上死信队列与其绑定（解决 KNWN-MQ-01）
@Bean
public Queue deadLetterQueue() {
    return QueueBuilder.durable("dead.letter.queue").build();
}

// 2. 显式声明死信交换机，而不是复用默认交换机（默认交换机不能 bind，只能靠队列同名路由）
//    推荐新建 dead.letter.exchange 并绑定 dead.letter.queue
@Bean
public DirectExchange deadLetterExchange() {
    return new DirectExchange("dead.letter.exchange", true, false);
}

// 3. 消费端不要吞异常（解决 KNWN-MQ-02）：抛出异常让 Spring 走重试 + 死信，
//    或配置 default-requeue-rejected: false
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(...) {
    factory.setDefaultRequeueRejected(false);
    return factory;
}

// 4. 事件补幂等键（解决 KNWN-MQ-03），消费端按 messageId 落库去重
message.put("messageId", record.getRecordId() + ":" + eventType);
```
