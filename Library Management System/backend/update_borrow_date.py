# -*- coding: utf-8 -*-
"""
更新借阅记录日期脚本
用于测试逾期罚款功能
"""
from app import create_app
from models import db, BorrowRecord
from datetime import date, timedelta

def update_borrow_dates():
    """将最新的借阅记录的日期更新为昨天"""
    app = create_app()
    with app.app_context():
        # 获取最新的借阅记录（未归还的）
        borrow_record = BorrowRecord.query.filter(
            BorrowRecord.return_date.is_(None)
        ).order_by(BorrowRecord.record_id.desc()).first()
        
        if not borrow_record:
            print("没有找到未归还的借阅记录")
            return
        
        # 计算昨天的日期（2025-12-10）
        yesterday = date(2025, 12, 10)
        # 应还日期也设为昨天（2025-12-10），这样就会显示为逾期
        due_date = date(2025, 12, 10)
        
        print(f"找到借阅记录 ID: {borrow_record.record_id}")
        print(f"原借阅日期: {borrow_record.borrow_date}")
        print(f"原应还日期: {borrow_record.due_date}")
        
        # 更新日期
        borrow_record.borrow_date = yesterday
        borrow_record.due_date = due_date
        
        # 保存到数据库
        db.session.add(borrow_record)
        db.session.commit()
        
        print("更新成功！")
        print(f"新借阅日期: {borrow_record.borrow_date}")
        print(f"新应还日期: {borrow_record.due_date}")
        print(f"今天日期: {date.today()}")
        is_overdue = date.today() > borrow_record.due_date
        print(f"是否逾期: {'是' if is_overdue else '否'}")

if __name__ == '__main__':
    update_borrow_dates()

