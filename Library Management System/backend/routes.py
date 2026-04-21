# -*- coding: utf-8 -*-
"""
图书管理系统 - 路由模块

本模块定义了所有的API路由和页面路由，按功能模块组织：
1. 认证相关路由：登录、登出、当前用户信息
2. 图书管理路由：图书的增删改查
3. 借阅管理路由：借阅和归还操作
4. 用户管理路由：用户信息管理
5. 静态文件服务：CSS、JS、图片等静态资源

作者: 图书管理系统开发团队
版本: 2.0.0
日期: 2024-12-09
"""

from flask import request, jsonify, session, send_from_directory, redirect, url_for
from datetime import date, datetime, timedelta
from models import db, User, BookInfo, BookCategory, BookCopy, BorrowRecord, SystemSettings
from sqlalchemy.orm import joinedload
from redis_client import redis_get_json, redis_set_json, redis_delete_pattern, redis_lock
import os


def get_system_setting(key, default_value):
    """
    获取系统设置值
    
    Args:
        key: 设置键名
        default_value: 默认值（如果设置不存在）
    
    Returns:
        设置值（字符串形式）
    """
    setting = SystemSettings.query.filter_by(setting_key=key).first()
    return setting.setting_value if setting else str(default_value)


def get_system_settings_dict():
    """
    获取所有系统设置的字典
    
    Returns:
        dict: 系统设置字典
    """
    settings = SystemSettings.query.all()
    settings_dict = {s.setting_key: s.setting_value for s in settings}
    
    # 默认值
    default_settings = {
        'default_borrow_days': '30',
        'max_borrow_count': '5',
        'max_renewal_times': '2',
        'overdue_fine_per_day': '0.5',
        'max_fine_amount': '50',
        'reminder_days_before_due': '3'
    }
    
    # 合并默认值和数据库中的值
    return {**default_settings, **settings_dict}


def require_login(func):
    """
    登录验证装饰器
    
    用于保护需要登录才能访问的API接口。
    检查session中是否存在user_id，如果不存在则返回401未授权错误。
    
    Args:
        func: 被装饰的函数
        
    Returns:
        function: 装饰后的函数
        
    Usage:
        @require_login
        def protected_api():
            return jsonify({'ok': True})
    """
    def wrapper(*args, **kwargs):
        # 检查session中是否有用户ID
        if 'user_id' not in session:
            return jsonify({'ok': False, 'error': '未登录'}), 401
        return func(*args, **kwargs)
    
    # 保持原函数的名称，避免Flask路由冲突
    wrapper.__name__ = func.__name__
    return wrapper


def register_routes(app):
    """
    注册所有路由到Flask应用
    
    将所有路由定义注册到Flask应用实例中，包括：
    - 页面路由和静态文件服务
    - API状态检查
    - 认证相关API
    - 图书管理API
    - 借阅管理API
    - 用户管理API
    
    Args:
        app: Flask应用实例
    """
    
    # ==================== 页面路由和静态文件服务 ====================
    
    @app.route('/')
    def index():
        """
        根路由处理函数
        
        将根路径重定向到登录页面，确保用户首先进行身份验证。
        
        Returns:
            Response: 重定向到登录页面的响应
        """
        return redirect('/templates/login.html')

    @app.route('/static/<path:filename>')
    def serve_static(filename):
        """
        静态文件服务路由
        
        提供CSS、JavaScript、图片等静态资源的访问服务。
        支持路径参数，可以访问frontend/static目录下的任何文件。
        
        Args:
            filename (str): 静态文件的相对路径
            
        Returns:
            Response: 静态文件内容
        """
        # 获取项目根目录（backend目录的上级目录）
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        # 构建静态文件目录路径
        static_dir = os.path.join(project_root, 'frontend', 'static')
        # 返回静态文件
        return send_from_directory(static_dir, filename)
    
    @app.route('/templates/<path:filename>')
    def serve_template(filename):
        """
        模板文件服务路由
        
        提供HTML模板文件的访问服务。
        支持路径参数，可以访问frontend/templates目录下的任何HTML文件。
        
        Args:
            filename (str): 模板文件的相对路径
            
        Returns:
            Response: HTML模板文件内容
        """
        # 获取项目根目录
        project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        # 构建模板文件目录路径
        template_dir = os.path.join(project_root, 'frontend', 'templates')
        # 返回模板文件
        return send_from_directory(template_dir, filename)

    @app.route('/api/status')
    def api_status():
        """
        API状态检查接口
        
        提供API服务状态检查，用于监控和健康检查。
        返回API运行状态、版本信息和当前时间戳。
        
        Returns:
            JSON: 包含状态信息的JSON响应
        """
        return jsonify({
            'ok': True,
            'message': '图书管理系统API运行正常',
            'version': '1.0.0',
            'timestamp': datetime.now().isoformat()
        })

    # ==================== 认证相关路由 ====================
    
    @app.route('/api/login', methods=['POST'])
    def login():
        """
        用户登录接口
        
        处理用户登录请求，验证用户名和密码，创建用户会话。
        支持JSON格式的请求体，返回用户信息和登录状态。
        
        Request Body:
            {
                "username": "用户名",
                "password": "密码"
            }
        
        Returns:
            JSON: 登录结果和用户信息
            - 成功: {"ok": true, "user": {...}}
            - 失败: {"ok": false, "error": "错误信息"}
        """
        # 获取请求数据
        data = request.get_json() or {}
        username = data.get('username')
        password = data.get('password')
        
        # 验证必填字段
        if not username or not password:
            return jsonify({'ok': False, 'error': '用户名或密码不能为空'}), 400

        # 查询用户
        user = User.query.filter_by(username=username).first()
        
        # 验证用户存在性和密码正确性
        if not user or not user.check_password(password):
            return jsonify({'ok': False, 'error': '用户名或密码错误'}), 401

        # 创建用户会话
        session['user_id'] = user.user_id
        session['username'] = user.username
        session['role'] = user.user_type

        # 返回成功响应和用户信息
        return jsonify({'ok': True, 'user': user.to_dict()})

    @app.route('/api/logout', methods=['POST'])
    def logout():
        """
        用户登出接口
        
        清除用户会话信息，实现用户登出功能。
        无论用户是否登录都返回成功状态。
        
        Returns:
            JSON: 登出结果
            - 成功: {"ok": true}
        """
        # 清除会话中的用户信息
        session.pop('user_id', None)
        session.pop('username', None)
        session.pop('role', None)
        
        return jsonify({'ok': True})

    @app.route('/api/current-user', methods=['GET'])
    def get_current_user():
        """
        获取当前用户信息接口
        
        获取当前登录用户的详细信息，用于前端验证登录状态和显示用户信息。
        
        Returns:
            JSON: 当前用户信息
            - 成功: {"ok": true, "user": {...}}
            - 未登录: {"ok": false, "error": "未登录"}
            - 用户不存在: {"ok": false, "error": "用户不存在"}
        """
        # 检查用户是否登录
        if 'user_id' not in session:
            return jsonify({'ok': False, 'error': '未登录'}), 401
        
        # 获取用户信息
        user = User.query.get(session['user_id'])
        if not user:
            return jsonify({'ok': False, 'error': '用户不存在'}), 404
        
        return jsonify({'ok': True, 'user': user.to_dict()})

    # ==================== 图书管理路由 ====================
    
    @app.route('/api/books', methods=['GET'])
    def list_books():
        """
        获取图书列表接口
        
        支持多条件搜索、筛选和分页功能。
        可以按书名、作者、ISBN进行搜索，按分类和出版年份筛选。
        
        Query Parameters:
            search (str): 搜索关键词，支持书名、作者、ISBN
            category (str): 分类筛选
            language (str): 语言筛选（暂未实现）
            year (str): 出版年份筛选，支持具体年份或'older'（2021年之前）
            page (int): 页码，默认1
            per_page (int): 每页数量，默认24
        
        Returns:
            JSON: 图书列表和分页信息
            {
                "ok": true,
                "books": [...],
                "pagination": {
                    "page": 1,
                    "per_page": 24,
                    "total": 100,
                    "pages": 5,
                    "has_prev": false,
                    "has_next": true
                }
            }
        """
        # 获取查询参数
        search = request.args.get('search', '').strip()
        category = request.args.get('category', '')
        language = request.args.get('language', '')
        year = request.args.get('year', '')
        page = int(request.args.get('page', 1))
        per_page = int(request.args.get('per_page', 24))

        # Redis 缓存 Key（查询型接口缓存 60 秒）
        cache_key = f"books:list:{search}:{category}:{language}:{year}:{page}:{per_page}"
        cached = redis_get_json(cache_key)
        if cached:
            return jsonify(cached)
        
        # 构建基础查询
        query = BookInfo.query
        
        # 搜索过滤：支持书名、作者、ISBN的模糊搜索
        if search:
            query = query.filter(
                db.or_(
                    BookInfo.title.contains(search),
                    BookInfo.author.contains(search),
                    BookInfo.isbn.contains(search)
                )
            )
        
        # 分类过滤
        if category:
            query = query.join(BookCategory).filter(BookCategory.category_name == category)
        
        # 语言过滤
        if language:
            query = query.filter(BookInfo.language == language)
        
        # 年份过滤
        if year:
            if year == 'older':
                # 筛选2021年之前出版的图书
                query = query.filter(db.extract('year', BookInfo.publication_date) < 2021)
            else:
                # 筛选指定年份出版的图书
                query = query.filter(db.extract('year', BookInfo.publication_date) == int(year))
        
        # 执行分页查询
        pagination = query.paginate(page=page, per_page=per_page, error_out=False)
        books = pagination.items

        result = {
            'ok': True,
            'books': [b.to_dict() for b in books],
            'pagination': {
                'page': page,
                'per_page': per_page,
                'total': pagination.total,
                'pages': pagination.pages,
                'has_prev': pagination.has_prev,
                'has_next': pagination.has_next
            }
        }

        redis_set_json(cache_key, result, ex=60)
        return jsonify(result)

    @app.route('/api/books/<int:book_id>', methods=['GET'])
    def get_book(book_id):
        """
        获取单本图书详情接口（带 Redis 缓存）
        """
        cache_key = f"books:detail:{book_id}"
        cached = redis_get_json(cache_key)
        if cached:
            return jsonify(cached)

        book = BookInfo.query.get(book_id)
        if not book:
            return jsonify({'ok': False, 'error': '图书不存在'}), 404

        result = {'ok': True, 'book': book.to_dict()}
        redis_set_json(cache_key, result, ex=120)
        return jsonify(result)

    @app.route('/api/books', methods=['POST'])
    @require_login
    def add_book():
        """
        添加新图书接口
        
        需要登录权限才能添加图书。验证必填字段和ISBN唯一性。
        
        Request Body:
            {
                "title": "书名",
                "author": "作者",
                "category": "分类",
                "isbn": "ISBN号",
                "publisher": "出版社（可选）",
                "pub_date": "出版日期（可选，ISO格式）",
                "cover_url": "封面图片URL（可选）"
            }
        
        Returns:
            JSON: 添加结果和图书信息
            - 成功: {"ok": true, "book": {...}}
            - 失败: {"ok": false, "error": "错误信息"}
        """
        data = request.get_json() or {}
        
        # 验证必填字段
        required_fields = ['title', 'author', 'category', 'isbn']
        for field in required_fields:
            if not data.get(field):
                return jsonify({'ok': False, 'error': f'{field} 不能为空'}), 400
        
        # 检查ISBN是否已存在，确保唯一性
        existing_book = BookInfo.query.filter_by(isbn=data['isbn']).first()
        if existing_book:
            return jsonify({'ok': False, 'error': 'ISBN已存在'}), 400
        
        # 创建新书对象
        book = BookInfo(
            title=data['title'],
            author=data['author'],
            isbn=data['isbn'],
            publisher_id=1,  # 默认出版社ID
            category_id=1,   # 默认分类ID
            price=30.00,     # 默认价格
            publication_date=datetime.fromisoformat(data['pub_date']).date() if data.get('pub_date') else None,
            cover_image=data.get('cover_url'),
            status='available'  # 新书默认状态为可借阅
        )
        
        # 保存到数据库
        db.session.add(book)
        db.session.commit()
        
        return jsonify({'ok': True, 'book': book.to_dict()})

    @app.route('/api/books/<int:book_id>', methods=['PUT'])
    @require_login
    def update_book(book_id):
        """
        更新图书信息接口
        
        需要登录权限才能更新图书信息。支持部分字段更新。
        
        Args:
            book_id (int): 图书ID
            
        Request Body:
            {
                "title": "书名（可选）",
                "author": "作者（可选）",
                "category": "分类（可选）",
                "isbn": "ISBN号（可选）",
                "publisher": "出版社（可选）",
                "pub_date": "出版日期（可选，ISO格式）",
                "cover_url": "封面图片URL（可选）",
                "status": "状态（可选）"
            }
        
        Returns:
            JSON: 更新结果和图书信息
            - 成功: {"ok": true, "book": {...}}
            - 失败: {"ok": false, "error": "错误信息"}
        """
        book = BookInfo.query.get(book_id)
        if not book:
            return jsonify({'ok': False, 'error': '图书不存在'}), 404
        
        data = request.get_json() or {}
        
        # 更新可编辑字段
        updatable_fields = ['title', 'author', 'category', 'isbn', 'publisher', 'cover_url', 'status']
        for field in updatable_fields:
            if field in data:
                setattr(book, field, data[field])
        
        # 特殊处理日期字段
        if data.get('pub_date'):
            book.pub_date = datetime.fromisoformat(data['pub_date']).date()
        
        # 保存更改
        db.session.commit()
        
        return jsonify({'ok': True, 'book': book.to_dict()})

    @app.route('/api/books/<int:book_id>', methods=['DELETE'])
    @require_login
    def delete_book(book_id):
        """
        删除图书接口
        
        需要登录权限才能删除图书。检查是否有未归还的借阅记录。
        
        Args:
            book_id (int): 图书ID
            
        Returns:
            JSON: 删除结果
            - 成功: {"ok": true}
            - 失败: {"ok": false, "error": "错误信息"}
        """
        book = BookInfo.query.get(book_id)
        if not book:
            return jsonify({'ok': False, 'error': '图书不存在'}), 404
        
        # 检查是否有未归还的借阅记录，防止删除正在被借阅的图书
        # 注意：BorrowRecord中使用的是copy_id，不是book_id
        active_borrows = db.session.query(BorrowRecord).join(BookCopy).filter(BookCopy.isbn == book.isbn, BorrowRecord.return_date.is_(None)).first()
        if active_borrows:
            return jsonify({'ok': False, 'error': '该图书有未归还的借阅记录，无法删除'}), 400
        
        # 删除图书
        db.session.delete(book)
        db.session.commit()
        
        return jsonify({'ok': True})

    @app.route('/api/books/<int:book_id>/status', methods=['PUT'])
    @require_login
    def update_book_status(book_id):
        """
        更新图书状态接口
        
        需要登录权限才能更新图书状态。支持在可借阅和已借出状态之间切换。
        
        Args:
            book_id (int): 图书ID
            
        Request Body:
            {
                "status": "available" 或 "borrowed"
            }
        
        Returns:
            JSON: 更新结果和图书信息
            - 成功: {"ok": true, "book": {...}}
            - 失败: {"ok": false, "error": "错误信息"}
        """
        book = BookInfo.query.get(book_id)
        if not book:
            return jsonify({'ok': False, 'error': '图书不存在'}), 404
        
        data = request.get_json() or {}
        new_status = data.get('status')
        
        # 验证状态值
        if new_status not in ['available', 'borrowed', 'frozen']:
            return jsonify({'ok': False, 'error': '状态值无效，必须是 available、borrowed 或 frozen'}), 400
        
        # 如果要将图书状态设为已借出，检查是否已有未归还的借阅记录
        if new_status == 'borrowed':
            active_borrows = db.session.query(BorrowRecord).join(BookCopy).filter(BookCopy.isbn == book.isbn, BorrowRecord.return_date.is_(None)).first()
            if not active_borrows:
                return jsonify({'ok': False, 'error': '该图书没有借阅记录，无法设为已借出状态'}), 400
        
        # 如果要将图书状态设为可借阅，检查是否还有未归还的借阅记录
        if new_status == 'available':
            active_borrows = db.session.query(BorrowRecord).join(BookCopy).filter(BookCopy.isbn == book.isbn, BorrowRecord.return_date.is_(None)).first()
            if active_borrows:
                return jsonify({'ok': False, 'error': '该图书有未归还的借阅记录，无法设为可借阅状态'}), 400
        
        # 如果要将图书状态设为冻结，检查是否存在未归还的借阅记录
        if new_status == 'frozen':
            active_borrows = db.session.query(BorrowRecord).join(BookCopy).filter(BookCopy.isbn == book.isbn, BorrowRecord.return_date.is_(None)).first()
            if not active_borrows:
                return jsonify({'ok': False, 'error': '该图书没有未归还的借阅记录，无法冻结'}), 400
        
        # 更新图书状态
        book.status = new_status
        db.session.commit()
        
        return jsonify({'ok': True, 'book': book.to_dict()})

    # ==================== 借阅管理路由 ====================
    
    @app.route('/api/borrows', methods=['GET'])
    @require_login
    def list_borrows():
        """
        获取借阅记录列表接口（别名）
        
        与/api/borrow-records功能相同，提供前端兼容性。
        """
        return list_borrow_records()

    @app.route('/api/borrow-records', methods=['GET'])
    @require_login
    def list_borrow_records():
        """
        获取借阅记录列表接口
        
        需要登录权限。支持按用户、图书、状态筛选和分页功能。
        
        Query Parameters:
            user_id (int): 用户ID筛选
            book_id (int): 图书ID筛选
            status (str): 状态筛选 ('active'未归还, 'returned'已归还, 'all'全部)
            page (int): 页码，默认1
            per_page (int): 每页数量，默认20
        
        Returns:
            JSON: 借阅记录列表和分页信息
        """
        # 获取查询参数
        user_id = request.args.get('user_id')
        book_id = request.args.get('book_id')
        status = request.args.get('status')  # 'active', 'returned', 'all'
        page = int(request.args.get('page', 1))
        per_page = int(request.args.get('per_page', 20))
        
        # 构建基础查询
        query = BorrowRecord.query
        
        # 用户过滤
        if user_id:
            query = query.filter(BorrowRecord.user_id == user_id)
        
        # 图书过滤
        if book_id:
            query = query.join(BookCopy).join(BookInfo).filter(BookInfo.id == book_id)
        
        # 状态过滤
        if status == 'active':
            # 未归还的记录
            query = query.filter(BorrowRecord.return_date.is_(None))
        elif status == 'returned':
            # 已归还的记录
            query = query.filter(BorrowRecord.return_date.isnot(None))
        
        # 按借阅日期倒序排列（最新的在前）
        query = query.order_by(BorrowRecord.borrow_date.desc())
        
        # 执行分页查询
        pagination = query.paginate(page=page, per_page=per_page, error_out=False)
        records = pagination.items
        
        return jsonify({
            'ok': True,
            'records': [r.to_dict() for r in records],
            'pagination': {
                'page': page,
                'per_page': per_page,
                'total': pagination.total,
                'pages': pagination.pages,
                'has_prev': pagination.has_prev,
                'has_next': pagination.has_next
            }
        })

    @app.route('/api/borrow', methods=['POST'])
    @require_login
    def borrow_book():
        """
        借阅图书接口（自动借书机场景）

        增强点：
        1. Redis 分布式锁，避免并发超借
        2. 借阅成功后清理图书缓存
        """
        data = request.get_json() or {}
        book_id = data.get('book_id')

        current_user_id = session.get('user_id')
        if not current_user_id:
            return jsonify({'ok': False, 'error': '未登录'}), 401

        try:
            reader_id = int(current_user_id)
        except (ValueError, TypeError):
            return jsonify({'ok': False, 'error': '无效的用户ID'}), 400

        operator_id = reader_id
        borrow_date = data.get('borrow_date')
        due_date = data.get('due_date')

        if not book_id:
            return jsonify({'ok': False, 'error': 'book_id required'}), 400

        lock_key = f"lock:borrow:book:{book_id}"
        with redis_lock(lock_key, timeout=5, blocking_timeout=2) as locked:
            if not locked:
                return jsonify({'ok': False, 'error': '当前借阅请求较多，请稍后重试'}), 429

            book = BookInfo.query.get(book_id)
            if not book:
                return jsonify({'ok': False, 'error': '图书不存在'}), 404

            if book.status == 'borrowed':
                return jsonify({'ok': False, 'error': '图书已被借出'}), 400

            if book.status == 'frozen':
                return jsonify({'ok': False, 'error': '该图书已被冻结，无法借阅'}), 400

            available_copy = BookCopy.query.filter_by(isbn=book.isbn, status='在馆').first()
            if not available_copy:
                return jsonify({'ok': False, 'error': '没有可用的图书副本'}), 400

            try:
                borrow_date_obj = datetime.fromisoformat(borrow_date).date() if borrow_date else date.today()
                if not due_date:
                    default_borrow_days = int(get_system_setting('default_borrow_days', 30))
                    due_date_obj = borrow_date_obj + timedelta(days=default_borrow_days)
                else:
                    due_date_obj = datetime.fromisoformat(due_date).date()
            except Exception:
                return jsonify({'ok': False, 'error': '日期格式应为 ISO 格式 (YYYY-MM-DD)'}), 400

            br = BorrowRecord(
                user_id=reader_id,
                copy_id=available_copy.copy_id,
                borrow_date=borrow_date_obj,
                due_date=due_date_obj,
                operator_id=operator_id,
                status='借出'
            )

            available_copy.status = '借出'

            if book.available_copies > 0:
                book.available_copies -= 1
                if book.available_copies == 0:
                    book.status = 'borrowed'

            db.session.add(br)
            db.session.add(available_copy)
            db.session.add(book)
            db.session.commit()

            # 清理图书缓存（列表、详情）
            redis_delete_pattern('books:list:*')
            redis_delete_pattern(f'books:detail:{book_id}')

            return jsonify({'ok': True, 'borrow': br.to_dict(), 'book': book.to_dict()})

    @app.route('/api/return', methods=['POST'])
    @require_login
    def return_book():
        """
        归还图书接口
        
        需要登录权限。更新借阅记录和图书状态。
        支持通过图书ID或借阅记录ID进行归还操作。
        
        Request Body:
            {
                "book_id": "图书ID（可选，与borrow_id二选一）",
                "borrow_id": "借阅记录ID（可选，与book_id二选一）"
            }
        
        Returns:
            JSON: 归还结果和更新后的信息
        """
        data = request.get_json() or {}
        book_id = data.get('book_id')
        borrow_id = data.get('borrow_id')

        # 验证参数
        if not book_id and not borrow_id:
            return jsonify({'ok': False, 'error': 'book_id 或 borrow_id 必须提供'}), 400

        # 查找借阅记录
        br = None
        if borrow_id:
            # 通过借阅记录ID查找
            br = BorrowRecord.query.get(borrow_id)
        else:
            # 通过图书ID查找未归还的记录
            br = db.session.query(BorrowRecord).join(BookCopy).join(BookInfo).filter(
                BookInfo.id == book_id, 
                BorrowRecord.return_date.is_(None)
            ).first()

        if not br:
            return jsonify({'ok': False, 'error': '未找到对应的借阅记录或已归还'}), 404

        # 计算逾期罚款
        return_date_obj = date.today()
        overdue_days = 0
        fine_amount = 0.0
        
        if br.due_date and return_date_obj > br.due_date:
            # 计算逾期天数
            overdue_days = (return_date_obj - br.due_date).days
            
            # 从系统设置获取逾期罚款金额
            fine_per_day = float(get_system_setting('overdue_fine_per_day', 0.5))
            max_fine = float(get_system_setting('max_fine_amount', 50))
            
            # 计算罚款金额
            fine_amount = min(overdue_days * fine_per_day, max_fine)
        
        # 设置归还日期为今天
        br.return_date = return_date_obj
        br.status = '已归还'
        
        # 更新图书副本状态为在馆
        if br.book_copy:
            br.book_copy.status = '在馆'
        
        # 更新图书信息中的可借数量
        if br.book_copy and br.book_copy.book_info:
            book_info = br.book_copy.book_info
            if book_info.available_copies < book_info.total_copies:
                book_info.available_copies += 1
            if book_info.available_copies > 0:
                book_info.status = 'available'

        # 保存更改
        db.session.add(br)
        if br.book_copy:
            db.session.add(br.book_copy)
        if br.book_copy and br.book_copy.book_info:
            db.session.add(br.book_copy.book_info)
        db.session.commit()

        # 清理图书缓存（列表、详情）
        redis_delete_pattern('books:list:*')
        if br.book_copy and br.book_copy.book_info:
            redis_delete_pattern(f"books:detail:{br.book_copy.book_info.id}")

        return jsonify({
            'ok': True, 
            'borrow': br.to_dict(), 
            'book': br.book_copy.book_info.to_dict() if br.book_copy and br.book_copy.book_info else None,
            'overdue_days': overdue_days,
            'fine_amount': fine_amount
        })

    @app.route('/api/my-borrows', methods=['GET'])
    @require_login
    def my_borrows():
        """
        获取当前用户的借阅记录接口
        
        需要登录权限。返回当前登录用户的所有借阅记录，格式化为前端需要的格式。
        
        Query Parameters:
            status (str): 状态筛选 ('active'未归还, 'returned'已归还, 'all'全部)，默认'all'
        
        Returns:
            JSON: 借阅记录列表
            {
                "ok": true,
                "borrows": [
                    {
                        "id": "借阅记录ID",
                        "book_title": "书名",
                        "book_author": "作者",
                        "book_cover_url": "封面URL",
                        "borrow_date": "借阅日期",
                        "due_date": "应还日期",
                        "return_date": "归还日期",
                        "status": "状态"
                    }
                ]
            }
        """
        user_id = session.get('user_id')
        if not user_id:
            return jsonify({'ok': False, 'error': '未登录'}), 401
        
        # 确保user_id是整数类型
        try:
            user_id = int(user_id)
        except (ValueError, TypeError):
            return jsonify({'ok': False, 'error': '用户ID格式错误'}), 400
        
        # 获取状态筛选参数
        status = request.args.get('status', 'all')
        
        # 构建查询 - 确保类型匹配，并预加载关联数据
        query = BorrowRecord.query.options(
            joinedload(BorrowRecord.book_copy).joinedload(BookCopy.book_info)
        ).filter(BorrowRecord.user_id == user_id)
        
        # 状态过滤
        if status == 'active':
            query = query.filter(BorrowRecord.return_date.is_(None))
        elif status == 'returned':
            query = query.filter(BorrowRecord.return_date.isnot(None))
        
        # 按借阅日期倒序排列
        query = query.order_by(BorrowRecord.borrow_date.desc())
        
        # 获取所有记录
        records = query.all()
        
        # 调试信息：记录查询结果
        print(f"[DEBUG] 查询用户 {user_id} (类型: {type(user_id)}) 的借阅记录，找到 {len(records)} 条记录")
        
        # 检查数据库中所有借阅记录（用于调试）
        all_records = BorrowRecord.query.all()
        print(f"[DEBUG] 数据库中总共有 {len(all_records)} 条借阅记录")
        for r in all_records[:5]:  # 只显示前5条
            print(f"[DEBUG]   记录ID: {r.record_id}, user_id: {r.user_id} (类型: {type(r.user_id)}), copy_id: {r.copy_id}")
        
        if len(records) > 0:
            print(f"[DEBUG] 第一条记录的user_id: {records[0].user_id} (类型: {type(records[0].user_id)})")
        
        # 格式化为前端需要的格式
        borrows = []
        skipped_count = 0
        for record in records:
            # 确保正确加载关联数据
            if not record.book_copy:
                print(f"[DEBUG] 借阅记录 {record.record_id} 没有关联的book_copy (copy_id: {record.copy_id})")
                skipped_count += 1
                continue  # 跳过没有book_copy的记录
            
            book_info = None
            if record.book_copy:
                # 使用join加载book_info，避免N+1查询问题
                book_info = record.book_copy.book_info
            
            if not book_info:
                print(f"[DEBUG] 借阅记录 {record.record_id} 的book_copy没有关联的book_info (copy_id: {record.copy_id}, isbn: {record.book_copy.isbn if record.book_copy else 'N/A'})")
                skipped_count += 1
                continue  # 跳过没有book_info的记录
            
            borrow = {
                'id': record.record_id,
                'book_title': book_info.title if book_info else '未知图书',
                'book_author': book_info.author if book_info else '未知作者',
                'book_cover_url': book_info.cover_image if book_info and book_info.cover_image else None,
                'borrow_date': record.borrow_date.isoformat() if record.borrow_date else None,
                'due_date': record.due_date.isoformat() if record.due_date else None,
                'return_date': record.return_date.isoformat() if record.return_date else None,
                'status': record.status
            }
            borrows.append(borrow)
        
        print(f"[DEBUG] 成功格式化 {len(borrows)} 条借阅记录，跳过了 {skipped_count} 条记录")
        
        return jsonify({
            'ok': True,
            'borrows': borrows
        })

    # ==================== 用户管理路由 ====================
    
    @app.route('/api/users', methods=['GET'])
    @require_login
    def list_users():
        """
        获取用户列表接口
        
        需要登录权限。获取系统中所有用户的基本信息。
        
        Returns:
            JSON: 用户列表信息
            {
                "ok": true,
                "users": [...]
            }
        """
        users = User.query.all()
        return jsonify({'ok': True, 'users': [u.to_dict() for u in users]})

    @app.route('/api/users', methods=['POST'])
    @require_login
    def add_user():
        """
        添加新用户接口
        
        需要登录权限。创建新用户账户，验证用户名唯一性。
        
        Request Body:
            {
                "username": "用户名（必填）",
                "password": "密码（必填）",
                "fullname": "用户全名（可选）",
                "role": "用户角色（可选，默认为'reader'）"
            }
        
        Returns:
            JSON: 创建结果和用户信息
            - 成功: {"ok": true, "user": {...}}
            - 失败: {"ok": false, "error": "错误信息"}
        """
        data = request.get_json() or {}
        
        # 验证必填字段
        if not data.get('username') or not data.get('password'):
            return jsonify({'ok': False, 'error': '用户名和密码不能为空'}), 400
        
        # 检查用户名是否已存在，确保唯一性
        existing_user = User.query.filter_by(username=data['username']).first()
        if existing_user:
            return jsonify({'ok': False, 'error': '用户名已存在'}), 400
        
        # 创建新用户
        user = User(
            username=data['username'],
            name=data.get('fullname', data.get('name', '')),
            user_type=data.get('role', data.get('user_type', 'reader'))  # 默认角色为读者
        )
        # 设置密码（自动加密）
        user.set_password(data['password'])
        
        # 保存到数据库
        db.session.add(user)
        db.session.commit()
        
        return jsonify({'ok': True, 'user': user.to_dict()})

    @app.route('/api/users/<int:user_id>', methods=['DELETE'])
    @require_login
    def delete_user(user_id):
        """
        删除用户接口
        
        需要登录权限。删除指定用户，检查是否有未归还的借阅记录。
        
        Args:
            user_id (int): 用户ID
            
        Returns:
            JSON: 删除结果
            - 成功: {"ok": true}
            - 失败: {"ok": false, "error": "错误信息"}
        """
        user = User.query.get(user_id)
        if not user:
            return jsonify({'ok': False, 'error': '用户不存在'}), 404
        
        # 检查是否为管理员用户，防止删除管理员
        if user.user_type == 'admin':
            return jsonify({'ok': False, 'error': '不能删除管理员账户'}), 400
        
        # 检查是否有未归还的借阅记录
        active_borrows = BorrowRecord.query.filter_by(user_id=user_id, return_date=None).first()
        if active_borrows:
            return jsonify({'ok': False, 'error': '该用户有未归还的借阅记录，无法删除'}), 400
        
        # 删除用户
        db.session.delete(user)
        db.session.commit()
        
        return jsonify({'ok': True})
    
    # ==================== 系统设置路由 ====================
    
    @app.route('/api/settings', methods=['GET'])
    @require_login
    def get_settings():
        """
        获取系统设置接口
        
        需要登录权限。返回所有系统设置。
        
        Returns:
            JSON: 系统设置列表
        """
        try:
            # 确保数据库表存在
            try:
                db.create_all()
            except Exception as e:
                print(f"[ERROR] 创建数据库表失败: {e}")
            
            settings = SystemSettings.query.all()
            settings_dict = {s.setting_key: s.setting_value for s in settings}
            
            # 如果没有设置，返回默认值
            default_settings = {
                'default_borrow_days': '30',
                'max_borrow_count': '5',
                'max_renewal_times': '2',
                'overdue_fine_per_day': '0.5',
                'max_fine_amount': '50',
                'reminder_days_before_due': '3'
            }
            
            # 合并默认值和数据库中的值
            result = {**default_settings, **settings_dict}
            
            return jsonify({'ok': True, 'settings': result})
        except Exception as e:
            print(f"[ERROR] 获取系统设置失败: {e}")
            import traceback
            traceback.print_exc()
            # 即使出错也返回默认值
            default_settings = {
                'default_borrow_days': '30',
                'max_borrow_count': '5',
                'max_renewal_times': '2',
                'overdue_fine_per_day': '0.5',
                'max_fine_amount': '50',
                'reminder_days_before_due': '3'
            }
            return jsonify({'ok': True, 'settings': default_settings})
    
    @app.route('/api/settings', methods=['PUT'])
    @require_login
    def update_settings():
        """
        更新系统设置接口
        
        需要登录权限。更新系统设置。
        
        Request Body:
            {
                "default_borrow_days": "30",
                "max_borrow_count": "5",
                "max_renewal_times": "2",
                "overdue_fine_per_day": "0.5",
                "max_fine_amount": "50",
                "reminder_days_before_due": "3"
            }
        
        Returns:
            JSON: 更新结果
        """
        try:
            data = request.get_json() or {}
            
            # 定义允许的设置键和描述
            allowed_settings = {
                'default_borrow_days': '默认借阅天数',
                'max_borrow_count': '单次最大借阅数量',
                'max_renewal_times': '允许续借次数',
                'overdue_fine_per_day': '逾期罚款（每天）',
                'max_fine_amount': '最高罚款上限',
                'reminder_days_before_due': '到期提醒提前天数'
            }
            
            # 确保数据库表存在
            try:
                db.create_all()
            except Exception as e:
                print(f"[ERROR] 创建数据库表失败: {e}")
            
            for key, value in data.items():
                if key in allowed_settings:
                    # 查找或创建设置
                    setting = SystemSettings.query.filter_by(setting_key=key).first()
                    if setting:
                        setting.setting_value = str(value)
                        setting.updated_at = datetime.now()
                    else:
                        setting = SystemSettings(
                            setting_key=key,
                            setting_value=str(value),
                            description=allowed_settings[key]
                        )
                        db.session.add(setting)
            
            db.session.commit()
            
            return jsonify({'ok': True, 'message': '设置已保存'})
        except Exception as e:
            db.session.rollback()
            print(f"[ERROR] 保存系统设置失败: {e}")
            import traceback
            traceback.print_exc()
            return jsonify({'ok': False, 'error': f'保存系统设置失败: {str(e)}'}), 500
