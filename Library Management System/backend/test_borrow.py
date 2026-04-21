#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试借阅功能的脚本

这个脚本模拟用户登录和借阅图书的完整流程，用于验证借阅功能是否正常工作。
"""

import requests
import json

# API基础URL
BASE_URL = "http://127.0.0.1:5001"

def test_borrow_function():
    """测试借阅功能的完整流程"""
    
    # 创建session来保持登录状态
    session = requests.Session()
    
    print("=== 开始测试借阅功能 ===")
    
    # 1. 登录
    print("\n1. 尝试登录...")
    login_data = {
        "username": "admin",
        "password": "admin123"
    }
    
    try:
        response = session.post(f"{BASE_URL}/api/login", json=login_data)
        print(f"登录响应状态码: {response.status_code}")
        
        if response.status_code == 200:
            login_result = response.json()
            if login_result.get('ok'):
                print("✓ 登录成功!")
                user_info = login_result.get('user', {})
                print(f"  用户名: {user_info.get('username')}")
                print(f"  用户类型: {user_info.get('user_type')}")
            else:
                print(f"✗ 登录失败: {login_result.get('error')}")
                return
        else:
            print(f"✗ 登录请求失败: {response.text}")
            return
    except Exception as e:
        print(f"✗ 登录请求异常: {e}")
        return
    
    # 2. 获取图书列表
    print("\n2. 获取图书列表...")
    try:
        response = session.get(f"{BASE_URL}/api/books")
        print(f"图书列表响应状态码: {response.status_code}")
        
        if response.status_code == 200:
            books_result = response.json()
            if books_result.get('ok'):
                books = books_result.get('books', [])
                print(f"✓ 获取到 {len(books)} 本图书")
                
                # 显示前3本图书的信息
                for i, book in enumerate(books[:3]):
                    print(f"  图书 {i+1}: {book.get('title')} - {book.get('author')} (可借: {book.get('available_copies')})")
                
                # 选择第一本图书进行借阅测试
                if books:
                    test_book = books[0]
                    book_id = test_book.get('id')
                    print(f"\n  选择图书进行借阅测试: {test_book.get('title')} (ID: {book_id})")
                    
                    # 3. 测试借阅
                    print("\n3. 测试借阅图书...")
                    borrow_data = {
                        "book_id": book_id,
                        "borrow_date": "2025-12-09",
                        "due_date": "2025-12-23",
                        "note": "测试借阅功能"
                    }
                    
                    response = session.post(f"{BASE_URL}/api/borrow", json=borrow_data)
                    print(f"借阅响应状态码: {response.status_code}")
                    
                    if response.status_code == 200:
                        borrow_result = response.json()
                        if borrow_result.get('ok'):
                            print("✓ 借阅成功!")
                            borrow_info = borrow_result.get('borrow', {})
                            book_info = borrow_result.get('book', {})
                            print(f"  借阅记录ID: {borrow_info.get('record_id')}")
                            print(f"  图书: {book_info.get('title')}")
                            print(f"  可借数量: {book_info.get('available_copies')}")
                            
                            # 4. 测试归还
                            print("\n4. 测试归还图书...")
                            return_data = {
                                "book_id": book_id
                            }
                            
                            response = session.post(f"{BASE_URL}/api/return", json=return_data)
                            print(f"归还响应状态码: {response.status_code}")
                            
                            if response.status_code == 200:
                                return_result = response.json()
                                if return_result.get('ok'):
                                    print("✓ 归还成功!")
                                    return_info = return_result.get('borrow', {})
                                    returned_book_info = return_result.get('book', {})
                                    print(f"  归还日期: {return_info.get('return_date')}")
                                    print(f"  图书可借数量: {returned_book_info.get('available_copies')}")
                                else:
                                    print(f"✗ 归还失败: {return_result.get('error')}")
                            else:
                                print(f"✗ 归还请求失败: {response.text}")
                        else:
                            print(f"✗ 借阅失败: {borrow_result.get('error')}")
                    else:
                        print(f"✗ 借阅请求失败: {response.text}")
                else:
                    print("✗ 没有可用的图书进行测试")
            else:
                print(f"✗ 获取图书列表失败: {books_result.get('error')}")
        else:
            print(f"✗ 获取图书列表请求失败: {response.text}")
    except Exception as e:
        print(f"✗ 获取图书列表异常: {e}")
        return
    
    print("\n=== 借阅功能测试完成 ===")

if __name__ == "__main__":
    test_borrow_function()
