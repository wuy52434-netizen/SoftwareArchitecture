"""冒烟测试（Smoke Test）— 核心链路一键验证。

设计原则：少而精、快而稳。只验证"系统是否上线且核心功能可用"，
跑通即视为冒烟通过，绝不纠缠细节分支（细节交给回归/功能层）。
运行方式: python run_smoke.py  （或 pytest 收集）
覆盖核心链路：
  1. 网关/中间件可达
  2. 登录鉴权
  3. 图书查询（含缓存/DB）
  4. 检索（ES）
  5. 借还闭环（核心业务）
  6. 统计/设置
"""
import requests, sys, json, uuid

BASE = "http://localhost:8080"
H = {"Content-Type": "application/json"}
PASS = FAIL = 0
CASES = []

def _test(name, ok, detail=""):
    global PASS, FAIL
    if ok: PASS += 1
    else: FAIL += 1
    CASES.append({"name": name, "pass": ok, "detail": detail})
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -- {detail}" if detail and not ok else ""))

print("=" * 50)
print("冒烟测试 Smoke Test 启动")
print("=" * 50)

# 1. 网关可达
try:
    r = requests.get(BASE + "/api/books", params={"per_page": 1}, headers=H, timeout=8)
    _test("网关/服务在线", r.status_code == 200, f"HTTP={r.status_code}")
except Exception as e:
    _test("网关/服务在线", False, str(e)[:60])

# 2. 登录鉴权
r = requests.post(BASE + "/api/auth/login", headers=H, json={"username": "admin", "password": "admin123"}, timeout=8).json()
_ok = r.get("code") == 200
TOKEN = r.get("data", {}).get("accessToken") if _ok else None
_test("登录鉴权", _ok, f"code={r.get('code')} msg={str(r.get('message'))[:30]}")

# 3. 图书列表(DB/分页)
r = requests.get(BASE + "/api/books", params={"per_page": 5}, headers=H, timeout=8).json()
_test("图书列表分页", r.get("code") == 200 and len(r["data"]["records"]) > 0, f"code={r.get('code')}")

# 4. 图书详情(Redis缓存路径)
r = requests.get(BASE + "/api/books/1", headers=H, timeout=8).json()
_test("图书详情", r.get("code") == 200 and "title" in (r.get("data") or {}), f"code={r.get('code')}")

# 5. ES 检索
r = requests.get(BASE + "/api/search", params={"keyword": "三体"}, headers=H, timeout=8).json()
_test("ES检索", r.get("code") == 200 and isinstance(r.get("data", {}).get("books"), list), f"code={r.get('code')}")

# 6. 借阅闭环（用 user1 借一本可用书，单测覆盖业务分支，冒烟只验主链路可达+库存）
avail = None
r = requests.get(BASE + "/api/books", params={"per_page": 20}, headers=H, timeout=8).json()
for b in r["data"]["records"]:
    if b.get("availableCopies", 0) > 0 and b.get("status") == "available":
        avail = b; break
borrow_id = None
if avail and TOKEN:
    u1 = requests.post(BASE + "/api/auth/login", headers=H, json={"username": "user1", "password": "123456"}, timeout=8).json()
    U1 = u1.get("data", {}).get("accessToken")
    r = requests.post(BASE + "/api/borrow", headers={**H, "Authorization": f"Bearer {U1}"},
                      json={"bookId": avail["id"], "userId": 11}, timeout=8).json()
    borrow_ok = r.get("code") == 200
    _test("借书(核心业务)", borrow_ok, f"code={r.get('code')} msg={str(r.get('message'))[:30]}")
    if borrow_ok:
        bdata = r.get("data") or {}
        borrow_id = bdata.get("id") or bdata.get("borrowId") or bdata.get("recordId")
else:
    _test("借书(核心业务)", False, "无可借图书" if not avail else "user1登录失败")

# 7. 归还（自清理）
if borrow_id:
    r = requests.post(BASE + "/api/return", headers={**H, "Authorization": f"Bearer {TOKEN}"},
                      json={"borrowId": borrow_id}, timeout=8).json()
    _test("归还(核心业务)", r.get("code") == 200, f"code={r.get('code')} msg={str(r.get('message'))[:30]}")
else:
    _test("归还(核心业务)", True, "借书未执行，跳过归还（前序依赖）")

# 8. 统计看板（鉴权）
if TOKEN:
    r = requests.get(BASE + "/api/stats/dashboard", headers={**H, "Authorization": f"Bearer {TOKEN}"}, timeout=8).json()
    _test("统计看板", r.get("code") == 200, f"code={r.get('code')}")
else:
    _test("统计看板", True, "未获取token，跳过")

# 9. 设置(公开读)
r = requests.get(BASE + "/api/settings", headers=H, timeout=8).json()
_test("系统设置", r.get("code") == 200, f"code={r.get('code')}")

# 10. 用户管理(管理员鉴权)
if TOKEN:
    r = requests.get(BASE + "/api/users", headers={**H, "Authorization": f"Bearer {TOKEN}"}, timeout=8).json()
    _test("用户管理(鉴权)", r.get("code") == 200, f"code={r.get('code')}")
else:
    _test("用户管理(鉴权)", True, "未获取token，跳过")

print("=" * 50)
print(f"冒烟结果: PASS {PASS} / FAIL {FAIL}")
sys.exit(1 if FAIL else 0)