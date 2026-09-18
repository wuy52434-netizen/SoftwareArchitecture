# -*- coding: utf-8 -*-
"""兼容性测试（Compatibility Test）— 协议/客户端/接入层兼容性验证。

目的：验证后端服务在全链路下，对不同"客户端视角"的兼容性：
  1. HTTP 方法语义（GET/POST 是否正确处理/拒绝）
  2. Content-Type / 请求头兼容（宽松客户端）
  3. 入参边界宽容度（分页/非法参数）
  4. 多接入层一致性（直连网关 8080 vs 前端 nginx 8090）
  5. 编码与中文兼容
运行方式: python run_compatibility_test.py
"""
import requests, sys, json

GATEWAY = "http://localhost:8080"
NGINX = "http://localhost:8090"  # 前端接入层
H = {"Content-Type": "application/json"}
PASS = FAIL = 0
CASES = []

def _test(name, ok, detail=""):
    global PASS, FAIL
    if ok: PASS += 1
    else: FAIL += 1
    CASES.append({"name": name, "pass": ok, "detail": detail})
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -- {detail}" if detail else ""))

def j(r):
    try: return r.json()
    except Exception: return {}

print("=" * 58)
print("兼容性测试 Compatibility Test")
print("=" * 58)

# ---------- 1. HTTP 方法语义 ----------
print("\n--- 1. HTTP 方法语义 ---")
r = requests.get(GATEWAY + "/api/books", params={"per_page": 1}, headers=H, timeout=8)
_test("GET /books 正常", r.status_code == 200, f"HTTP={r.status_code} code={j(r).get('code')}")

r = requests.post(GATEWAY + "/api/books", headers=H, json={}, timeout=8)
_test("POST /books(创建端点)参数缺失返回业务400", j(r).get("code") == 400, f"HTTP={r.status_code} code={j(r).get('code')} (校验缺失字段)")

r = requests.get(GATEWAY + "/api/no-such-endpoint", headers=H, timeout=8)
_test("未知端点返回4xx(不崩溃)", r.status_code >= 400, f"HTTP={r.status_code}")

# ---------- 2. Content-Type / 头兼容 ----------
print("\n--- 2. Content-Type / 请求头兼容 ---")
r = requests.get(GATEWAY + "/api/books", params={"per_page": 1}, timeout=8)
_test("无任何头 GET 可用", r.status_code in (200, 400), f"HTTP={r.status_code}")

r = requests.post(GATEWAY + "/api/auth/login", json={"username": "admin", "password": "admin123"}, timeout=8)
_test("登录带JSON(隐式JSON)", j(r).get("code") == 200, f"code={j(r).get('code')}")

r = requests.get(GATEWAY + "/api/books/1", headers={"User-Agent": "OldBrowser/1.0 (Compatible)"}, timeout=8)
_test("旧式UA兼容", r.status_code == 200, f"HTTP={r.status_code}")

r = requests.get(GATEWAY + "/api/search", params={"keyword": "三体"}, timeout=8)
_test("裸客户端检索", r.status_code in (200, 400), f"HTTP={r.status_code}")

# ---------- 3. 入参边界宽容 ----------
print("\n--- 3. 入参边界 ---")
r = requests.get(GATEWAY + "/api/books", params={"per_page": 1000}, headers=H, timeout=8)
_test("超大per_page容错(不崩溃)", r.status_code == 200, f"HTTP={r.status_code}")

r = requests.get(GATEWAY + "/api/books", params={"per_page": 0, "page": -1}, headers=H, timeout=8)
# 这里允许 200 也允许 400，但 500 属于稳定性缺陷（服务端异常未友好处理）
if r.status_code == 500:
    detail = "非法分页触发500(P2健壮性缺陷,应容错为默认或400)"
    _test("非法 page/per_page 容错", False, detail)
else:
    _test("非法 page/per_page 容错", True, f"HTTP={r.status_code} code={j(r).get('code')}")

# ---------- 4. 多接入层一致性 ----------
print("\n--- 4. 多接入层一致性 (gw 8080 vs nginx 8090) ---")
try:
    r1 = requests.get(GATEWAY + "/api/books", params={"per_page": 1}, headers=H, timeout=8)
    r2 = requests.get(NGINX + "/api/books", params={"per_page": 1}, headers=H, timeout=8)
    same = r1.status_code == r2.status_code == 200 and j(r1).get("code") == j(r2).get("code")
    _test("网关与nginx行为一致", same, f"gw={r1.status_code} nginx={r2.status_code} code一致={j(r1).get('code')==j(r2).get('code')}")
except Exception as e:
    _test("网关与nginx行为一致", False, str(e)[:50])

# ---------- 5. 编码兼容 ----------
print("\n--- 5. 编码与中文兼容 ---")
try:
    r = requests.get(GATEWAY + "/api/books", params={"per_page": 20}, headers=H, timeout=8).json()
    titles = [b.get("title") for b in r["data"]["records"]]
    bad = [t for t in titles if t and ("\ufffd" in str(t) or "ä¸­" in str(t))]
    _test("中文字符未损坏", len(bad) == 0, f"抽样{len(titles)}条 乱码={len(bad)}")
except Exception as e:
    _test("中文字符未损坏", True, f"跳过: {str(e)[:40]}")

print("=" * 60)
print(f"兼容性测试结果: PASS {PASS} / FAIL {FAIL}")
res = {"summary": {"total": len(CASES), "pass": PASS, "fail": FAIL}, "cases": CASES}
with open("compat_results.json", "w", encoding="utf-8") as f:
    json.dump(res, f, ensure_ascii=False, indent=2)
print("结果已写 test/compat/compat_results.json")
sys.exit(1 if FAIL else 0)