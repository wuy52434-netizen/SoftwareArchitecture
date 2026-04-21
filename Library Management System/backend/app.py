# -*- coding: utf-8 -*-
"""
图书管理系统 - Flask应用工厂模块

本模块实现了Flask应用工厂模式，用于创建和配置Flask应用实例。
采用工厂模式便于测试、支持多环境配置，提高代码的可维护性和复用性。

版本: 2.0.0
日期: 2024-12-09
"""

from flask import Flask  # Flask核心框架
from flask_cors import CORS  # 跨域资源共享支持
from models import db  # 数据库实例
from config import Config  # 配置类
from routes import register_routes  # 路由注册函数
import os  # 操作系统接口


def create_app():
    """
    Flask应用工厂函数
    
    创建并配置Flask应用实例，包括：
    1. 基础配置加载
    2. 安全密钥设置
    3. CORS跨域支持
    4. 数据库初始化
    5. 路由注册
    
    Returns:
        Flask: 配置完成的Flask应用实例
    """
    # 创建Flask应用实例，指定静态文件目录
    # static_folder指向frontend/static目录，template_folder指向frontend/templates目录
    app = Flask(__name__, 
                static_folder='../frontend/static',
                template_folder='../frontend/templates')
    
    # 从配置对象加载应用配置
    # 包括数据库URI、调试模式等设置
    app.config.from_object(Config)
    
    # 设置会话密钥，用于加密用户会话数据
    # 优先使用配置文件中的SECRET_KEY，如果没有则生成24位随机密钥
    app.secret_key = app.config.get('SECRET_KEY') or os.urandom(24)
    
    # 启用CORS（跨域资源共享）支持
    # supports_credentials=True允许发送认证信息（如cookies）
    # 这对于前后端分离架构中的session认证非常重要
    CORS(app, supports_credentials=True)
    
    # 初始化数据库连接
    # 将数据库实例与当前应用关联
    db.init_app(app)
    
    # 注册所有路由到应用实例
    # 包括认证、图书管理、借阅管理、用户管理等模块的路由
    register_routes(app)
    
    return app


if __name__ == '__main__':
    """
    应用启动入口
    
    当直接运行此文件时，创建应用实例并启动开发服务器。
    仅用于开发环境
    """
    # 创建应用实例
    app = create_app()
    
    # 启动Flask开发服务器
    # host='0.0.0.0': 容器/局域网可访问（便于Nginx反向代理）
    # port=5001: 监听5001端口
    # debug=False: 容器部署时关闭调试模式
    app.run(host='0.0.0.0', port=5001, debug=False)
