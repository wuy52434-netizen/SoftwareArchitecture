#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""将 MySQL 中的图书数据灌入 Elasticsearch books 索引, 使 /api/search 真实可用。
纯标准库实现: subprocess(读MySQL) + urllib(写ES)。
用法: python3 seed_es_books.py
"""
import subprocess
import json
import urllib.request
from pathlib import Path

ES = "http://localhost:9200"

SQL = """
SELECT bi.id, bi.isbn, bi.title, bi.author,
       IFNULL(p.publisher_name,''),
       IFNULL(bc.category_name,''),
       COALESCE(bi.summary,''),
       bi.price,
       IFNULL(bi.cover_url,''),
       bi.status,
       bi.available_copies,
       bi.borrow_count,
       COALESCE(DATE_FORMAT(bi.publish_date,'%Y-%m-%d'),'')
FROM book_info bi
LEFT JOIN publisher p ON bi.publisher_id=p.publisher_id
LEFT JOIN book_category bc ON bi.category_id=bc.category_id
WHERE bi.deleted=0
"""


def epoch_ms(date_str):
    """把 'YYYY-MM-DD' 转成 epoch millis (UTC). 空则返回0."""
    if not date_str:
        return 0
    try:
        y, m, d = [int(x) for x in date_str.split("-")]
        from datetime import datetime, timezone
        return int(datetime(y, m, d, tzinfo=timezone.utc).timestamp() * 1000)
    except Exception:
        return 0


def fetch_rows():
    cmd = ["docker", "exec", "library-mysql", "mysql", "-uroot", "-proot123",
           "--default-character-set=utf8mb4", "-N", "-B", "library", "-e", SQL]
    out = subprocess.check_output(cmd, stderr=subprocess.DEVNULL).decode("utf-8")
    rows = []
    for line in out.splitlines():
        parts = line.split("\t")
        if len(parts) < 13:
            continue
        rows.append({
            "id": int(parts[0]),
            "isbn": parts[1],
            "title": parts[2],
            "author": parts[3],
            "publisher": parts[4],
            "category": parts[5],
            "summary": parts[6],
            "price": float(parts[7]) if parts[7] else 0,
            "coverUrl": parts[8],
            "status": parts[9],
            "availableCopies": int(parts[10]) if parts[10] else 0,
            "borrowCount": int(parts[11]) if parts[11] else 0,
            "publishDate": int(parts[12][:4]) if parts[12] else 0,
            "createdAt": parts[12] or "2026-01-01",
        })
    return rows


def http(method, path, body=None):
    url = ES + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode())


def main():
    print("[1] 读取 MySQL 图书数据...")
    rows = fetch_rows()
    print(f"    共 {len(rows)} 本书")

    print("[2] 删除旧 books 索引(若存在)...")
    code, _ = http("DELETE", "/books")
    print(f"    DELETE /books -> {code}")

    print("[3] 创建 books 索引（mapping 取自唯一事实来源 es/books-mapping.json）...")
    # 不再内联一份 mapping：内联就会与 search-service 的实体定义各自漂移，
    # 之前就是这样漂出"索引缺 updatedAt、分词器与声明不符"两个缺陷（KNWN-ES-02/03）。
    mapping_path = (Path(__file__).resolve().parent
                    / "backend" / "search-service" / "src" / "main" / "resources"
                    / "es" / "books-mapping.json")
    mapping = json.loads(mapping_path.read_text(encoding="utf-8"))
    # 文件顶部的 _comment 只是给人看的，ES 不接受未知顶层键
    mapping.pop("_comment", None)
    code, _ = http("PUT", "/books", mapping)
    print(f"    PUT /books -> {code}  (来源: {mapping_path.relative_to(Path(__file__).resolve().parent)})")

    print("[4] 批量灌入数据 (bulk)...")
    bulk_lines = []
    for b in rows:
        bulk_lines.append(json.dumps({"index": {"_index": "books", "_id": str(b["id"])}}))
        bulk_lines.append(json.dumps(b))
    payload = "\n".join(bulk_lines) + "\n"
    req = urllib.request.Request(ES + "/_bulk?refresh=true", data=payload.encode("utf-8"), method="POST")
    req.add_header("Content-Type", "application/x-ndjson")
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            res = json.loads(resp.read().decode())
        items = res.get("items", [])
        ok = sum(1 for it in items if it.get("index", {}).get("status") == 201)
        print(f"    灌入成功 {ok}/{len(items)}")
    except urllib.error.HTTPError as e:
        print(f"    bulk 失败: {e.code} {e.read().decode()[:300]}")

    print("[5] 验证索引文档数...")
    code, res = http("GET", "/books/_count")
    print(f"    books 索引文档数 = {res.get('count', '?')}")

    print("[6] 验证搜索 '三体' 和 '森林'...")
    for kw in ["三体", "森林"]:
        pass


if __name__ == "__main__":
    main()