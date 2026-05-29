# JMeter 可用性演示

JMeter 只作为演示和验证工具，不计入课程 12 项技术。

## 文件

| 文件 | 作用 |
|---|---|
| `borrow-concurrency.jmx` | 场景一：高并发同时借同一实体副本 |
| `duplicate-click.jmx` | 场景二：网络抖动/连续点击导致重复借书 |
| `reset-demo-copy.sql` | 演示前恢复 `BC0024001` 为可借状态 |

## 演示前准备

```powershell
cd "E:\Ai Code\SoftwareArchitecture"
Get-Content demo\jmeter\reset-demo-copy.sql | docker exec -i library-mysql mysql --default-character-set=utf8mb4 -uroot -proot123 library
```

确认副本状态：

```powershell
docker exec library-mysql mysql --default-character-set=utf8mb4 -uroot -proot123 library -e "SELECT bc.copy_id, bc.book_id, bc.barcode, bc.status, bi.title, bi.available_copies FROM book_copy bc JOIN book_info bi ON bi.id=bc.book_id WHERE bc.barcode='BC0024001';"
```

当前演示库里 `user1` 的 `user_id` 是 `11`，两个 JMeter 文件默认使用 `userId=11`。如果重建过数据库导致 ID 变化，先查询：

```powershell
docker exec library-mysql mysql --default-character-set=utf8mb4 -uroot -proot123 library -e "SELECT user_id, username, status FROM user WHERE username='user1';"
```

然后在 JMeter 的 `Demo variables` 中把 `userId` 改成查询结果。

## GUI 演示

1. 打开 JMeter。
2. File -> Open，选择 `demo/jmeter/borrow-concurrency.jmx`。
3. 点击启动按钮，查看 `View Results Tree`、`Aggregate Report`。
4. 再执行一次 reset SQL。
5. 打开 `demo/jmeter/duplicate-click.jmx`，重复执行。

## 命令行演示

```powershell
& "E:\apache-jmeter-5.6.3\bin\jmeter.bat" -n -t demo\jmeter\borrow-concurrency.jmx -l demo\jmeter\borrow-concurrency.jtl -e -o demo\jmeter\report-concurrency
Get-Content demo\jmeter\reset-demo-copy.sql | docker exec -i library-mysql mysql --default-character-set=utf8mb4 -uroot -proot123 library
& "E:\apache-jmeter-5.6.3\bin\jmeter.bat" -n -t demo\jmeter\duplicate-click.jmx -l demo\jmeter\duplicate-click.jtl -e -o demo\jmeter\report-duplicate
```

## 预期结果

| 场景 | 预期结果 |
|---|---|
| 高并发借同一副本 | 只有 1 个请求业务成功，其他请求返回副本不可用、锁冲突或系统繁忙 |
| 连续点击重复借书 | 第一次成功，后续请求返回 `2007 图书副本不可用` |

JMeter 测试计划中加入了 `$.code == 200` 的业务断言，因此 Aggregate Report 里会把后续被系统拦截的请求计为失败。这不是系统故障，而是用来证明“重复请求没有借书成功”。

注意：这些请求走 `http://localhost/api/borrow`，会经过 Nginx 和 API Gateway，不是直接调用 borrow-service。
