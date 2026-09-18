# -*- coding: utf-8 -*-
"""功能测试执行引擎 — 覆盖图书/鉴权/借阅/搜索/统计/设置全模块
运行: python run_functional_tests.py  （连接网关 8080 真实运行态）
产出: functional_results.json + 控制台 PASS/FAIL 统计
"""
import json, requests, sys, traceback, uuid, time

BASE = "http://localhost:8080"
TOKEN = None
ADMIN = {"username": "admin", "password": "admin123"}
PASS, FAIL = 0, 0
RESULTS = []


def request(method, path, token=None, json_body=None, params=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    return requests.request(method, BASE + path, headers=headers, json=json_body,
                            params=params, timeout=10)


def test(case_id, name, passed, detail=""):
    global PASS, FAIL
    if passed:
        PASS += 1
    else:
        FAIL += 1
    RESULTS.append({"id": case_id, "name": name, "pass": passed, "detail": detail})
    mark = "PASS" if passed else "FAIL"
    print(f"[{mark}] {case_id} {name}" + (f"  -- {detail}" if detail and not passed else ""))


# ---------- 前置：登录 ----------
login = request("POST", "/api/auth/login", json_body=ADMIN).json()
assert login.get("code") == 200, "前置：admin 登录失败"
TOKEN = login["data"]["accessToken"]
print(f">> 前置登录 OK, admin userId={login['data']['user']['userId']}\n")

# ============ A 鉴权功能 ============
r = request("POST", "/api/auth/login", json_body={"username": "admin", "password": "wrong"}).json()
test("AUTH-FN-01", "错误密码登录", r.get("code") != 200, f"code={r.get('code')}")

r = request("POST", "/api/auth/login", json_body={"username": "no_such_user_xyz", "password": "123"}).json()
test("AUTH-FN-02", "不存在用户登录", r.get("code") != 200, f"code={r.get('code')}")

r = request("GET", "/api/auth/me", token=TOKEN).json()
test("AUTH-FN-03", "带token查询当前用户", r.get("code") == 200, f"code={r.get('code')}")

r = request("GET", "/api/auth/me").json()
test("AUTH-FN-04", "无token查询当前用户被拒", r.get("code") == 401, f"code={r.get('code')} (期望401)")

# refresh token
rt = login["data"]["refreshToken"]
r = request("POST", "/api/auth/refresh", json_body={"refreshToken": rt}).json()
test("AUTH-FN-05", "刷新令牌", r.get("code") == 200, f"code={r.get('code')} msg={r.get('message')}")

# ============ B 图书功能 ============
r = request("GET", "/api/books", params={"page": 1, "per_page": 24}).json()
ok = r.get("code") == 200 and isinstance(r["data"].get("records"), list)
test("BOOK-FN-01", "图书列表分页", ok, f"total={r.get('data',{}).get('total')}")

r = request("GET", "/api/books", params={"page": 999, "per_page": 24}).json()
test("BOOK-FN-02", "分页越界(999)不崩溃", r.get("code") == 200, f"code={r.get('code')}")

r = request("GET", "/api/books", params={"category": 1}).json()
test("BOOK-FN-03", "分类过滤(文学)", r.get("code") == 200, f"code={r.get('code')}")

r = request("GET", "/api/books/1").json()
test("BOOK-FN-04", "图书详情 id=1", r.get("code") == 200, f"code={r.get('code')}")

r = request("GET", "/api/books/99999").json()
test("BOOK-FN-05", "详情不存在(99999)", r.get("code") != 200, f"code={r.get('code')} (期望业务失败)")

r = request("GET", "/api/books/categories").json()
test("BOOK-FN-06", "分类列表", r.get("code") == 200 and len(r["data"]) >= 6, f"分类数={len(r.get('data',[]))}")

r = request("GET", "/api/books/popular").json()
test("BOOK-FN-07", "热门图书", r.get("code") == 200, f"code={r.get('code')}")

r = request("GET", "/api/books/newest").json()
test("BOOK-FN-08", "新书上架", r.get("code") == 200, f"code={r.get('code')}")

# 扫码查书（借书机核心）
r = request("GET", "/api/books/scan", params={"code": "1"}).json()
test("BOOK-FN-09", "扫码查书(code=1)", r.get("code") == 200, f"code={r.get('code')} msg={r.get('message')}")

# 副本条码
book_list = request("GET", "/api/books", params={"per_page": 3}).json()["data"]["records"]
if book_list:
    b = book_list[0]
    r = request("GET", f"/api/books/{b['id']}").json()
    test("BOOK-FN-10", f"详情回查记录图书#{b['id']}结构", r.get("code") == 200 and "data" in r, f"title={r.get('data',{}).get('title')}")

# ============ C 图书写操作(经鉴权) ============
import uuid
uniq = str(uuid.uuid4())[:6]
create_body = {"isbn": "999000"+uniq, "title": f"功能测试书-{uniq}", "author": "测试员",
               "price": 25.0, "totalCopies": 2, "categoryId": 3, "summary": "功能测试用例创建的临时书"}
r = request("POST", "/api/books", token=TOKEN, json_body=create_body).json()
test("BOOK-FN-11", "创建图书(经TOKEN)", r.get("code") == 200, f"code={r.get('code')} msg={r.get('message')}")
created_id = r.get("data", {}).get("id")

if created_id:
    r = request("PUT", f"/api/books/{created_id}", token=TOKEN,
                json_body={"title": f"测试书-改-{uniq}", "author": "测试员"}).json()
    test("BOOK-FN-12", "更新图书", r.get("code") == 200, f"code={r.get('code')} msg={r.get('message')}")

    # 扣减到0观察状态机
    r = request("PUT", f"/api/books/{created_id}/decrease-stock", token=TOKEN).json()
    test("BOOK-FN-13", "扣减库存", r.get("code") == 200, f"code={r.get('code')}")

# ============ D 借阅闭环 ============
# 用 user1(userId=11) 借一本可用书，验证核心闭环；admin(9)已满额作为业务规则用例
USER1 = request("POST", "/api/auth/login", json_body={"username": "user1", "password": "123456"}).json()
U1TOKEN = USER1.get("data", {}).get("accessToken")
u1id = USER1.get("data", {}).get("user", {}).get("userId", 11)

# 隔离测试数据：把 user1 的全部在借记录归还，清空额度，保证借阅闭环不因历史数据干扰
if U1TOKEN:
    try:
        _recs = request("GET", "/api/borrow-records", params={"userId": u1id}, token=U1TOKEN).json()
        for _x in ((_recs.get("data") or {}).get("records") or []):
            if _x.get("status") == "active" and _x.get("recordId"):
                request("POST", "/api/return", token=U1TOKEN, json_body={"borrowId": _x["recordId"]})
    except Exception:
        pass

avail_book = None
for bk in request("GET", "/api/books", params={"per_page": 10}).json()["data"]["records"]:
    if bk.get("availableCopies", 0) > 0 and bk.get("status") == "available":
        avail_book = bk
        break

if avail_book and U1TOKEN:
    r = request("POST", "/api/borrow", token=U1TOKEN, json_body={"bookId": avail_book["id"], "userId": u1id}).json()
    test("BORROW-FN-01", f"借书 user1->book#{avail_book['id']}", r.get("code") == 200,
         f"code={r.get('code')} msg={r.get('message')}")
    bdata = r.get("data") or {}
    borrow_id = bdata.get("id") or bdata.get("recordId")
    # 自清理：借完即归还，避免持续占用 user1 借阅额度
    if borrow_id:
        request("POST", "/api/return", token=U1TOKEN, json_body={"borrowId": borrow_id})
else:
    test("BORROW-FN-01", "借书", bool(avail_book), "无可借图书" if not avail_book else "user1登录失败")

# admin 满额业务规则（3003 超借限制）
r = request("POST", "/api/borrow", token=TOKEN, json_body={"bookId": 1, "userId": 9}).json()
test("BORROW-FN-01b", "超借限制 admin满5本被拒", r.get("code") != 200 and r.get("code") != 500,
     f"code={r.get('code')} msg={str(r.get('message'))[:40]}")

# 我的借阅
r = request("GET", "/api/my-borrows", params={"userId": u1id}, token=U1TOKEN).json()
test("BORROW-FN-02", "我的借阅(借书机输入读者证)", r.get("code") == 200, f"code={r.get('code')}")

r = request("GET", "/api/borrow-records", params={"page": 1, "size": 5}).json()
test("BORROW-FN-03", "借阅记录列表", r.get("code") == 200, f"code={r.get('code')} total={r.get('data',{}).get('total')}")

# ============ E 搜索 ============
r = request("GET", "/api/search", params={"keyword": "三体"}).json()
test("SEARCH-FN-01", "ES检索'三体'", r.get("code") == 200, f"code={r.get('code')} msg={r.get('message')}")

r = request("GET", "/api/search/hot").json()
test("SEARCH-FN-02", "热搜词", r.get("code") == 200, f"code={r.get('code')}")

# ============ F 统计 ============
r = request("GET", "/api/stats/dashboard", token=TOKEN).json()
test("STAT-FN-01", "统计看板(带TOKEN)", r.get("code") == 200, f"code={r.get('code')} msg={r.get('message')}")
r0 = request("GET", "/api/stats/dashboard").json()
test("STAT-FN-02", "统计看板无TOKEN应拒", r0.get("code") == 401, f"code={r0.get('code')}")

# ============ G 设置 ============
r = request("GET", "/api/settings").json()
test("SETTINGS-FN-01", "获取系统设置", r.get("code") == 200 and "maxBorrowCount" in r.get("data", {}), f"code={r.get('code')}")

# 汇总
print("\n" + "=" * 46)
print(f"总执行 {len(RESULTS)} 用例：PASS {PASS} / FILED {FAIL}")
with open("functional_results.json", "w", encoding="utf-8") as f:
    json.dump({"summary": {"total": len(RESULTS), "pass": PASS, "fail": FAIL}, "cases": RESULTS}, f,
              ensure_ascii=False, indent=2)
print("结果已写 test/functional/functional_results.json")
sys.exit(1 if FAIL else 0)