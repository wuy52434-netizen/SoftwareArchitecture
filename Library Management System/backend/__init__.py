# -*- coding: utf-8 -*-
"""
图书管理系统 - 后端包初始化模块

本模块是backend包的初始化文件，用于导出主要组件。
通过在此处统一导出，其他模块可以方便地导入所需的核心组件。

导出的组件：
- create_app: Flask应用工厂函数
- db: SQLAlchemy数据库实例
- User: 用户数据模型
- Book: 图书数据模型
- BorrowRecord: 借阅记录数据模型

作者: 图书管理系统开发团队
版本: 2.0.0
日期: 2024-12-09
"""

# 导入Flask应用工厂函数
from .app import create_app

# 导入数据库实例和数据模型
from .models import db, User, Book, BorrowRecord

# 定义包的公共接口，方便外部导入
__all__ = [
    'create_app',    # Flask应用工厂函数
    'db',            # SQLAlchemy数据库实例
    'User',          # 用户数据模型
    'Book',          # 图书数据模型
    'BorrowRecord'   # 借阅记录数据模型
]
