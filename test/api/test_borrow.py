"""借阅/归还流程测试：借阅记录、我的借阅、归还。

覆盖核心业务流程的读取与状态查询。
"""
import uuid

import pytest


class TestBorrowRecords:

    def test_borrow_records_list(self, auth_client):
        """借阅记录分页列表。"""
        body = auth_client.get("/api/borrow-records", params={"page": 1, "size": 5}).json()
        assert body["code"] == 200
        assert "records" in body["data"]

    def test_my_borrows_requires_login(self, fresh_client):
        """/api/my-borrows 未登录应 401。"""
        body = fresh_client.get("/api/my-borrows").json()
        assert body["code"] in (401,)

    def test_my_borrows_with_admin(self, auth_client):
        """登录后查询我的借阅记录。"""
        # 用 userId=9 (admin) 查询
        body = auth_client.get("/api/my-borrows", params={"userId": 9}).json()
        assert body["code"] == 200
        # 返回借阅列表（可能为空，但结构正确）
        assert isinstance(body["data"], list)


class TestBorrowFlow:

    @staticmethod
    def _first_borrowable_book(client) -> int | None:
        """从真实图书列表里挑一本 status=available 且库存 >0 的书。

        之前把 bookId 写死为 1，一旦这本书不可借就 ``pytest.skip`` 掉整个闭环 ——
        等于"借书/归还主链路"经常没被真正执行过。改为动态挑书，闭环始终真跑。
        """
        records = client.get("/api/books", params={"page": 1, "per_page": 100}).json()["data"]["records"]
        for book in records:
            if book.get("status") == "available" and (book.get("stock") or 0) > 0:
                return book["id"]
        return None

    @staticmethod
    def _create_dedicated_borrower(client) -> dict:
        """建一个专用测试用户，返回 {userId, username, password}。

        不能再用演示账号 userId=9：它已被历史测试借满 5 本（账号额度上限），
        再跑必然返回 3003「已借阅5本，最多可借阅5本」——那是**测试隔离缺陷**，
        不是业务缺陷。专用用户与演示数据、历史残留彻底解耦，闭环才会稳定真跑。
        """
        username = f"qa_borrower_{uuid.uuid4().hex[:8]}"
        password = "Passw0rd!"
        created = client.post("/api/users", json={
            "username": username,
            "password": password,
            "realName": "借阅闭环测试用户",
            "userType": "reader",
        }).json()
        assert created["code"] == 200, f"创建专用借阅用户失败：{created}"
        return {"userId": created["data"]["userId"], "username": username, "password": password}

    def test_borrow_and_return_roundtrip(self, client, admin_token, fresh_client):
        """借书→查我的借阅→归还，自清理真实闭环。

        关键事实（本轮实测确认，原用例理解错了）：
        1. **借阅身份取自 token，请求体里的 ``userId`` 会被后端忽略** ——
           用管理员 token 借书时，无论 body 写哪个 userId，都记在管理员名下，
           所以必须用专用用户**自己的 token**；
        2. 借阅成功返回的是 ``recordId``（没有 ``borrowId``），
           原用例读 ``data["borrowId"]`` 恒为 None，导致"我的借阅包含该记录"这条断言从未真正执行；
        3. 归还接口认 ``bookId`` 或 ``borrowId``（不认 ``recordId``），因此按 ``bookId`` 归还。
        """
        borrower = self._create_dedicated_borrower(client)
        user_id = borrower["userId"]
        try:
            assert fresh_client.login(borrower["username"], borrower["password"]), "专用用户登录失败"
            user_client = fresh_client          # 已携带该用户自己的 token

            book_id = self._first_borrowable_book(client)
            assert book_id is not None, (
                "环境里没有任何 status=available 且 stock>0 的图书，借阅闭环无法验证。"
                "这不是业务分支，而是测试数据不足，需先补一本可借图书。"
            )

            borrow = user_client.post("/api/borrow", json={"bookId": book_id}).json()
            assert borrow["code"] == 200, f"对明确可借的图书 id={book_id} 发起借阅仍失败：{borrow}"

            record_id = borrow["data"].get("recordId")
            assert record_id, f"借阅成功响应里没有 recordId，无法追踪该记录：{borrow['data']}"

            try:
                # 我的借阅必须包含这条记录（这次断言真的会执行）
                mine = user_client.get("/api/my-borrows").json()
                assert mine["code"] == 200
                assert isinstance(mine["data"], list)
                assert any(r.get("recordId") == record_id for r in mine["data"]), (
                    f"刚借出的 recordId={record_id} 未出现在我的借阅列表里：{mine['data']}"
                )
            finally:
                # 按 bookId 归还，避免污染库存
                ret = user_client.post("/api/return", json={"bookId": book_id}).json()
                assert ret["code"] == 200, f"归还失败：{ret}"

                after = user_client.get("/api/my-borrows").json()["data"]
                target = next(r for r in after if r.get("recordId") == record_id)
                assert target.get("returnDate"), f"归还后 returnDate 仍为空：{target}"
        finally:
            # 停用专用账号，避免用户表随每次跑测试持续膨胀（已积累 16 个 tester_* 残留）
            client.put(f"/api/users/{user_id}/status", params={"status": "disabled"})

    def test_borrow_ignores_user_id_in_body(self, client, admin_token, fresh_client):
        """安全回归：借阅只能借给自己，body 里的 ``userId`` 不得生效（防代人借书）。

        实测：用专用用户 token 调 ``POST /api/borrow {"bookId":X,"userId":9}``，
        落库的 ``userId`` 是该用户自己的 id 而非 9。此行为正确，用例把它钉住，
        防止后续"为了方便"改成信任 body 里的 userId 而引入越权。
        """
        borrower = self._create_dedicated_borrower(client)
        try:
            assert fresh_client.login(borrower["username"], borrower["password"]), "专用用户登录失败"

            book_id = self._first_borrowable_book(client)
            assert book_id is not None, "没有可借图书，无法验证借阅归属"

            # 故意把 userId 指向演示管理员账号（9）
            borrow = fresh_client.post("/api/borrow", json={"bookId": book_id, "userId": 9}).json()
            assert borrow["code"] == 200, f"借阅失败：{borrow}"
            assert borrow["data"]["userId"] == borrower["userId"], (
                f"body 里的 userId=9 被采纳了（落库 userId={borrow['data']['userId']}），"
                f"存在代人借书风险"
            )

            fresh_client.post("/api/return", json={"bookId": book_id})
        finally:
            client.put(f"/api/users/{borrower['userId']}/status", params={"status": "disabled"})

    def test_return_without_borrow_id(self, auth_client):
        """归还缺 borrowId 时结构完整。"""
        resp = auth_client.post("/api/return", json={"bookId": 999999})
        body = resp.json()
        assert "code" in body


class TestBorrowRecordsDetail:

    def test_borrow_records_status_filter(self, auth_client):
        body = auth_client.get("/api/borrow-records",
                               params={"page": 1, "size": 10, "status": "active"}).json()
        assert body["code"] == 200
        assert "records" in body["data"]

    def test_borrow_record_by_id(self, auth_client):
        body = auth_client.get("/api/borrow-records/46").json()
        assert "code" in body
        if body["code"] == 200:
            assert "bookTitle" in body["data"] or "bookId" in body["data"]

    def test_renew_record_no_500(self, auth_client):
        resp = auth_client.put("/api/borrow-records/46/renew")
        assert resp.json()["code"] != 500

    def test_scan_borrow_kiosk(self, auth_client):
        body = auth_client.get("/api/books/scan", params={"code": "BC0016001"}).json()
        assert body["code"] in (200, 1001, 4004, 5001)