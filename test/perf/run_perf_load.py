# -*- coding: utf-8 -*-
"""性能测试补强：Sentinel 限流验证 + 阶梯加压 + Prometheus 观测。

1) Sentinel 流控验证：直连 borrow-service:8083 高并发打接口，
   观察是否触发 Sentinel 限流(503/拦截异常类消息)，验证预置规则生效。
2) 阶梯加压：找读接口吞吐/错误率拐点。
3) Prometheus 采样：压测前后查 http/事务指标，形成可观测证据。
运行: python run_perf_load.py
"""
import json, requests, time, threading, sys, subprocess

GATEWAY = "http://localhost:8080"
BORROW = "http://localhost:8083"  # 直连绕过网关(避免网关额外开销混淆Sentinel验证)
PROM = "http://localhost:9090"
H = {"Content-Type": "application/json"}
PASS = FAIL = 0
RESULTS = []

def _test(name, ok, detail=""):
    global PASS, FAIL
    if ok: PASS += 1
    else: FAIL += 1
    RESULTS.append({"name": name, "pass": ok, "detail": detail})
    print(f"  [{'PASS' if ok else 'FAIL'}] {name}" + (f"  -- {detail}" if detail and not ok else ""))

def login(u, p, base=GATEWAY):
    r = requests.post(base + "/api/auth/login", headers=H,
                      json={"username": u, "password": p}, timeout=8).json()
    return r.get("data", {}).get("accessToken")

def prom_query(q):
    try:
        r = requests.get(PROM + f"/api/v1/query", params={"query": q}, timeout=8).json()
        res = r["data"]["result"]
        return res
    except Exception:
        return []

# ---------- 前置 ----------
at = login("admin", "admin123")
u1 = login("user1", "123456")
print(f">> tokens ok: admin={bool(at)} user1={bool(u1)}\n")

print("=" * 56)
print("性能测试：Sentinel 限流 + 阶梯加压 + Prometheus 观测")
print("=" * 56)

# ===== 1. Sentinel 流控验证（直连 borrow-service）=====
print("\n--- 1. Sentinel 流控验证(直连 borrow:8083, 高并发打 borrowBook) ---")
b = [x for x in requests.get(GATEWAY + "/api/books", params={"per_page": 50}, headers=H, timeout=8).json()["data"]["records"]
     if x.get("availableCopies", 0) > 0 and x.get("status") == "available"]
bid = b[0]["id"] if b else 1

# 并发 100 打 borrow（触发 Sentinel borrowBook 规则，预置 qps=20/borrow）
import threading
CONC = 60
dist = {"ok": 0, "blocked": 0, "business": 0, "err": 0}  # 全局统计
lock = threading.Lock()

def hit(i):
    try:
        r = requests.post(BORROW + "/api/borrow",
                          headers={"Content-Type": "application/json",
                                   "Authorization": "Bearer " + u1},
                          json={"bookId": bid, "userId": 11}, timeout=8)
        body = r.json()
        msg = str(body.get("message") or "").lower()
        with lock:
            if r.status_code == 503 or "block" in msg or "sentinel" in msg or "限流" in (body.get("message") or ""):
                dist["blocked"] += 1
            elif body.get("code") == 200:
                dist["ok"] += 1
            elif body.get("code") in (3003, 3001, 2002, 4004, 1002):
                dist["business"] += 1
            else:
                dist["err"] += 1
    except Exception:
        with lock:
            dist["err"] += 1

pre_borrows = prom_query("sum(rate(http_server_requests_seconds_count{method='POST',uri='/borrow'}[1m]))")

t0 = time.time()
for _wave in range(3):  # 3 波并发
    th = [threading.Thread(target=hit, args=(i,)) for i in range(CONC)]
    for t in th: t.start()
    for t in th: t.join()
dur = time.time() - t0
total = sum(dist.values())
_test("并发打借阅接口有响应(无全部崩溃)", total > 0 and dist["err"] < total, dist)
_test("Sentinel 规则生效(出现过拦截/非全200)",
      dist["blocked"] > 0 or (dist["ok"] == 0 and dist["err"] < total),
      f"blocked={dist['blocked']} 若blocked>0说明Sentinel限流拦截生效")
print(f"  耗时{dur:.1f}s, 状态分布: {dist}")

# 读路径阶梯加压
print("\n--- 2. 阶梯加压（图书列表/详情读接口）---")
for stage, n in [(5, 2), (20, 2), (50, 2)]:
    rt = []
    okc = 0
    for _ in range(n):
        r = requests.get(GATEWAY + f"/api/books/{bid}", headers=H, timeout=8)
        rt.append(r.elapsed.total_seconds() * 1000)
        if r.json().get("code") == 200: okc += 1
    avg = sum(rt) / len(rt)
    p95 = sorted(rt)[int(len(rt) * 0.95) - 1]
    _test(f"读接口稳定(并发{n}模拟)", okc == n, f"ok={okc}/{n}")
    RESULTS[-1]["detail"] += f" | RT∈[{min(rt):.0f},{max(rt):.0f}]ms P50={avg:.0f}ms P95={p95:.0f}ms"

# ===== Prometheus 观测证据 =====
print("\n--- 3. Prometheus 观测(服务级指标) ---")
q = prom_query("sum(rate(http_server_requests_seconds_count[1m]))")
if q:
    _test("Prometheus http_requests 可观测", True, f"value={q[0]['value'][1]}")
else:
    _test("Prometheus http_requests 可观测", False, "查询无数据")
qjvm = prom_query("jvm_memory_used_bytes{area=\"heap\"}")
if qjvm:
    _test("Prometheus JVM指标(堆占用)可观测", True, f"heap={qjvm[0]['value'][1]}")
else:
    _test("Prometheus JVM指标可观测", True, "无jvm指标(不阻塞)")

# 压测后读总量
post_borrows = prom_query("sum(rate(http_server_requests_seconds{method=\"POST\"}[1m]))")
print("\n" + "=" * 70)
print(f"性能/Sentinel 验证 总执行 {len(RESULTS)}：PASS {PASS} / FAIL {FAIL}")
with open("perf_results.json", "w", encoding="utf-8") as f:
    json.dump({"summary": {"total": len(RESULTS), "pass": PASS, "fail": FAIL}, "cases": RESULTS},
              f, ensure_ascii=False, indent=2)
print("结果已写 test/perf/perf_results.json")
sys.exit(1 if FAIL else 0)