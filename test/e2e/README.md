# 前端 UI 自动化测试（Playwright + 页面对象模式）

对 Vue3 前端做端到端回归，覆盖**登录链路**与**两条检索链路**。

## 为什么是 Playwright

| 维度 | 说明 |
|---|---|
| 语言 | Python，与项目已有 pytest / requests / Allure 技术栈统一，不需要再引一套 Node 测试体系 |
| 自动等待 | 内置 actionability 检查（可见 / 可编辑 / 稳定 / 可接收事件），不需要写 `sleep`，天然抗抖动 |
| 定位器 | 原生支持 `data-testid`，与样式解耦 |
| 追踪 | `--tracing on` 可产出带 DOM 快照的 trace，失败复盘不用靠猜 |

## 两条检索链路必须分开覆盖

这是本套件最关键的设计判断。项目里"搜书"有两个**完全不同**的实现：

| 页面 | 前端文件 | 后端接口 | 数据源 |
|---|---|---|---|
| 读者门户 `/portal/books` | `portal/Books.vue` | `GET /api/books` | **MySQL**（LIKE 检索） |
| 借书机 `/kiosk/search` | `kiosk/Search.vue` | `GET /api/search` | **Elasticsearch** |

只测其中一条会漏掉一半问题——尤其 ES 那条链路，它是「索引与数据库脱节」唯一暴露给用户的入口。

## 定位策略

**只允许 `data-testid`。** 前端原本一个都没有，本次已在以下文件补齐：

| 文件 | 新增 testid |
|---|---|
| `frontend/src/views/Login.vue` | `login-form` `login-username` `login-password` `login-remember` `login-submit` `quick-login-admin` `quick-login-user` |
| `frontend/src/views/portal/Books.vue` | `books-search-input` `books-total-count` `books-grid` `book-card` `book-card-title` `book-card-author` `books-empty-state` `books-pagination` |
| `frontend/src/views/kiosk/Search.vue` | `kiosk-search-input` `kiosk-search-submit` `kiosk-search-results` `kiosk-search-total` |

为什么不直接用 class：`.book-card` `.el-input` 这类选择器会随样式重构失效，
而 `data-testid` 是显式契约，改样式不会误伤测试。

> 注意：Element Plus 的 `el-input` 会把 `data-testid` 透传到**外层 div**，
> 真正的 `<input>` 在内部。所以页面对象里统一写成
> `testid("login-username").locator("input")`。

## 前置条件

```bash
# 1. 起后端（网关 8080 + MySQL + Redis + Nacos + ES）
docker compose -f docker/docker-compose.yml up -d

# 2. 起前端 dev server（3000，已配 /api → localhost:8080 代理）
cd frontend && npm install && npm run dev
```

前端不可达时整个套件自动跳过，不会产生假失败。

## 运行

```bash
pip install -r requirements.txt
playwright install chromium        # 首次需执行一次

pytest -v
pytest -v --html=report.html --self-contained-html
pytest -v -k login                 # 只跑登录链路
pytest -v --headed                 # 打开浏览器观察执行过程
pytest -v --tracing on             # 产出 trace，失败时用 playwright show-trace 复盘
WEB_BASE_URL=http://10.0.0.5:3000 pytest -v
```

## 目录结构

```
test/e2e/
├── pages/
│   ├── base_page.py          # 基类：testid 定位、导航、通用断言
│   ├── login_page.py         # 登录页
│   ├── books_page.py         # 门户检索页（MySQL 链路）
│   └── kiosk_search_page.py  # 借书机检索页（ES 链路）
├── conftest.py               # 环境哨兵 + browser_context_args + 已登录夹具
├── test_login.py             # 登录链路（6 条，含参数化）
├── test_book_search.py       # 门户 MySQL 检索（5 条）
└── test_kiosk_search.py      # 借书机 ES 检索（3 条）
```

## 设计要点

1. **环境哨兵前置**：`_web_env_guard` 在 session 级探测 3000 端口，不通直接跳过全部用例。
2. **`browser_context_args` 注入 `base_url`**：页面对象里写 `goto("/login")` 即可，
   不把 host 硬编码进代码，一套用例可切任意环境。
3. **不写 `sleep`**：`BooksPage.wait_settled()` 等的是 `el-loading` 遮罩消失，
   这是"数据真的回来了"的确定性信号，比固定等待可靠。
4. **断言跟着用户可见信号走**：比如"登录成功"断言的是 URL 离开 `/login`，
   而不是去读 Pinia 内部状态——后者能过但用户可能看不到页面，属于假通过。
5. **阳性用例配阴性用例**：有「错误密码被拦住」，就必须有「正确密码放行」，
   否则用例在"登录功能整体坏掉"时也会通过。

## 已知问题

| 编号 | 严重度 | 问题 |
|---|---|---|
| **KNWN-UI-01** | P2 | 前端原本零 `data-testid`，本次已补。后续新增页面/组件请沿用同一约定，否则 UI 自动化会重新变得脆弱。 |
| **KNWN-ES-01/02** | P1 | 借书机检索走 ES，而 ES 索引是手工 seed 的静态快照且未用 IK 分词器。表现为：门店（门户）能查到、借书机搜不到同一本书。见 `test_kiosk_search.py`。 |
| **KNWN-UI-02** | P3 | 项目无 ESLint / 无前端单测框架。当前 UI 回归只有这 14 条端到端用例，组件级问题只能等到 E2E 才暴露。 |
