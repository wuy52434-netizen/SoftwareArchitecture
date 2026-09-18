# 测试技术栈全量工程（Test-Suite）

> 为 **图书自动借书机系统**（Spring Cloud Alibaba 微服务：Nacos / Gateway / Sentinel / RabbitMQ / Redis / ES / Prometheus）打造的**全维度**测试工程。
> 从测试金字塔基线扩展为 **七 大方向 + 六类质量需求**：分析/单测/契约/接口自动化/功能/安全/数据缓存/AI + 性能/兼容/冒烟/回归。
> 全部测试资产收敛在独立 `test/` 目录，与业务代码隔离，一条命令可复跑。

---

## 一、测试金字塔与技术栈全景

```
                    ┌──────────────────────────────┐
                    │  性能层  JMeter + 并发脚本       │ ← 吞吐、RT、限流(Sentinel)
                    ├──────────────────────────────┤
                    │  兼容层  多客户端/接入层          │ ← P05/方法语义/编码
                    ├──────────────────────────────┤
                    │  安全层  越权/注入/XSS           │ ← 权限边界
                    ├──────────────────────────────┤
                    │  数据一致性(Redis/ES/DB 对账)     │ ← Cache-Aside/并发守恒
                    ├──────────────────────────────┤
                    │  接口自动化( pytest + Allure )│ ← 61 接口场景
                    ├──────────────────────────────┤
                    │  契约层 (JSON Schema CDC)      │ ← 协议护栏
                    ├──────────────────────────────┤
                    │  单元层 (JUnit5+Mockito+JaCoCo)│ ← 业务规则
                    └──────────────────────────────┘
       切层： 功能(27) / 冒烟(10) / 回归(13) / AI 辅助贯穿
```

| 层级/方向 | 技术栈 | 位置 | 结果 |
| --- | --- | --- | --- |
| 单元测试 | JUnit5+Mockito+AssertJ+JaCoCo | `backend/<模块>/src/test` | 23/23 |
| 契约测试 | pytest+jsonschema | `test/contract/` | 6/6 |
| 接口自动化 | pytest+requests+Allure | `test/api/` | 58/61(2缺陷) |
| 功能测试 | 自研全接口场景 | `test/functional/` | 27/27 |
| 安全测试 | 自研越权/注入/XSS | `test/security/` | 13/14(XSS P1) |
| 数据一致性 | Redis/ES/DB 对账 | `test/data/` | 10/11 |
| 性能-JMeter | Apache JMeter 5.6.3 | `test/jmeter/` | 6000样本 0错误 |
| 性能补强 | Sentinel 并发+Prometheus | `test/perf/` | 7/7 |
| 冒烟测试 | 核心链路秒判 | `test/smoke/` | 10/10 |
| 回归测试 | 核心回归+缺陷复验 | `test/regression/` | 10/13(3缺陷) |
| 兼容性 | 多客户端/接入层 | `test/compat/` | 10/11(1P2) |
| AI 应用 | AI 辅助测试工程 | `test/ai/` | 方法论+案例 |

---

## 二、目录说明

```text
test/
├── analysis/    # 测试分析与用例设计（编号体系/风险矩阵）
├── ai/          # AI 在测试中的应用（案例 + Prompt 模板）
├── api/         # 接口自动化（pytest + requests + Allure）
├── compat/      # 兼容性测试（多客户端/接入层/参数边界）
├── contract/    # 契约测试（JSON Schema CDC）
├── data/        # 数据与缓存一致性（Redis 篡改/ES-DB 对账/并发守恒）
├── functional/  # 功能测试（全接口手工功能用例脚本化）
├── jmeter/      # 性能压测 JMeter 脚本 + Dashboard
├── perf/        # 性能补强：Sentinel 限流 + 阶梯加压 + Prometheus
├── regression/  # 回归测试（核心回归 + 历史缺陷复验）
├── reports/     # 汇总报告（全量总报告 HERE）
├── security/    # 安全与权限测试
├── smoke/       # 冒烟测试（核心链路）
├── unit/        # 单元测试驱动入口
└── README.md
```

---

## 三、主要交付物（重点看这几个）

- **`reports/全量测试技术栈总报告.md`** ← **本工程的总报告**：全景结果表 + 全量缺陷清单 + 技术栈 + 运行方式 + 简历亮点。
- `analysis/测试分析与用例设计.md` — 测试基线文档。
- `ai/AI工具在测试中的应用.md` — AI 工程化方法论与 prompt 模板。
- `compat/兼容性测试分析.md`、各层 `*_results.json` — 分层证据。

---

## 四、运行方法（全量，一条条可复跑）

```bash
# 前提：docker-compose 已启动全量服务（网关 8080）
cd test

# ① 单元测试（driven by Maven wrapper）  ② 契约
python ../tmp/run_unit_tests.py
cd contract && python -m pytest -v --html=report.html && cd ..

# ③ 接口自动化(Allure)  ④ 功能
cd api   && python run_with_allure.py && cd ..
cd functional && python run_functional_tests.py && cd ..

# ⑤ 安全  ⑥ 数据一致性  ⑦ 性能补强
cd security && python run_security_tests.py && cd ..
cd data     && python run_cache_data.py && cd ..
cd perf     && python run_perf_load.py && cd ..

# ⑧ 冒烟  ⑨ 回归  ⑩ 兼容
cd smoke    && python run_smoke.py && cd ..
cd regression && python run_regression.py && cd ..
cd compat   && python run_compatibility_test.py && cd ..

# ⑪ JMeter 压测
cd jmeter && E:/apache-jmeter-5.6.3/bin/jmeter.bat -n -t load_test.jmx -l result.jtl -e -o report
```

> 依赖：Python 3.11+（`pip install -r test/api/requirements.txt`）、服务需运行、`docker exec library-redis redis-cli -p 6379` 可操作 Redis。