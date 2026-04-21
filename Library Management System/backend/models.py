from datetime import datetime, date
from werkzeug.security import generate_password_hash, check_password_hash
from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()


class User(db.Model):
    """用户表"""
    __tablename__ = 'user'
    
    user_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    username = db.Column(db.String(30), unique=True, nullable=False, comment='用户名')
    password = db.Column(db.String(100), nullable=False, comment='密码')
    name = db.Column(db.String(50), nullable=False, comment='姓名')
    user_type = db.Column(db.String(10), nullable=False, default='reader', comment='用户类型')
    gender = db.Column(db.String(2), comment='性别')
    birthday = db.Column(db.Date, comment='生日')
    phone = db.Column(db.String(20), comment='电话')
    category_id = db.Column(db.Integer, db.ForeignKey('reader_category.category_id'), comment='读者类别')
    last_login = db.Column(db.DateTime, comment='最后登录时间')
    status = db.Column(db.String(10), nullable=False, default='正常', comment='状态')
    
    # 关系
    reader_category = db.relationship('ReaderCategory', backref='users')
    borrow_records = db.relationship('BorrowRecord', backref='user', foreign_keys='BorrowRecord.user_id')
    operated_records = db.relationship('BorrowRecord', backref='operator', foreign_keys='BorrowRecord.operator_id')

    def set_password(self, password: str):
        """设置密码"""
        self.password = generate_password_hash(password)

    def check_password(self, password: str) -> bool:
        """验证密码"""
        return check_password_hash(self.password, password)

    def to_dict(self):
        """转换为字典"""
        return {
            'id': self.user_id,  # 前端期望的id字段
            'user_id': self.user_id,
            'username': self.username,
            'name': self.name,
            'real_name': self.name,  # 前端期望的字段
            'fullname': self.name,   # 前端期望的字段
            'role': self.user_type,  # 前端期望的role字段
            'user_type': self.user_type,
            'gender': self.gender,
            'birthday': self.birthday.isoformat() if self.birthday else None,
            'phone': self.phone,
            'email': self.phone,  # 临时使用phone作为email
            'category_id': self.category_id,
            'last_login': self.last_login.isoformat() if self.last_login else None,
            'created_at': self.last_login.isoformat() if self.last_login else None,  # 前端期望的字段
            'status': self.status,
            'reader_category': self.reader_category.to_dict() if self.reader_category else None
        }


class ReaderCategory(db.Model):
    """读者分类表"""
    __tablename__ = 'reader_category'
    
    category_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    category_name = db.Column(db.String(20), nullable=False, comment='读者类别名称')
    max_borrow = db.Column(db.Integer, nullable=False, comment='借书最大数量')
    loan_period = db.Column(db.Integer, nullable=False, comment='借书期限(天)')

    def to_dict(self):
        """转换为字典"""
        return {
            'category_id': self.category_id,
            'category_name': self.category_name,
            'max_borrow': self.max_borrow,
            'loan_period': self.loan_period
        }


class Publisher(db.Model):
    """出版社表"""
    __tablename__ = 'publisher'
    
    publisher_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    publisher_name = db.Column(db.String(100), nullable=False, comment='出版社名称')
    address = db.Column(db.String(200), comment='地址')
    phone = db.Column(db.String(20), comment='电话')

    def to_dict(self):
        """转换为字典"""
        return {
            'publisher_id': self.publisher_id,
            'publisher_name': self.publisher_name,
            'address': self.address,
            'phone': self.phone
        }


class BookInfo(db.Model):
    """图书信息表"""
    __tablename__ = 'book_info'
    
    # 添加自增主键ID，前端依赖ID字段
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    isbn = db.Column(db.String(20), unique=True, nullable=False, comment='ISBN号')
    title = db.Column(db.String(100), nullable=False, comment='书名')
    author = db.Column(db.String(50), nullable=False, comment='作者')
    publisher_id = db.Column(db.Integer, db.ForeignKey('publisher.publisher_id'), nullable=False, comment='出版社')
    price = db.Column(db.Numeric(8, 2), nullable=False, comment='价格')
    category_id = db.Column(db.Integer, db.ForeignKey('book_category.category_id'), nullable=False, comment='图书类别')
    summary = db.Column(db.Text, comment='内容简介')
    publication_date = db.Column(db.Date, comment='出版日期')
    cover_image = db.Column(db.String(200), comment='封面图片')
    total_copies = db.Column(db.Integer, nullable=False, default=0, comment='总副本数')
    available_copies = db.Column(db.Integer, nullable=False, default=0, comment='可借副本数')
    
    # 添加前端需要的字段
    status = db.Column(db.String(20), nullable=False, default='available', comment='图书状态')
    language = db.Column(db.String(20), default='中文', comment='语言')
    description = db.Column(db.Text, comment='详细描述')
    location = db.Column(db.String(100), comment='图书位置')
    
    # 关系
    publisher = db.relationship('Publisher', backref='books')
    category = db.relationship('BookCategory', backref='books')
    copies = db.relationship('BookCopy', backref='book_info')

    def to_dict(self):
        """转换为字典"""
        return {
            'id': self.id,  # 添加ID字段
            'isbn': self.isbn,
            'title': self.title,
            'author': self.author,
            'publisher_id': self.publisher_id,
            'publisher': self.publisher.publisher_name if self.publisher else '未知出版社',
            'price': float(self.price) if self.price else None,
            'category_id': self.category_id,
            'category': self.category.category_name if self.category else '未分类',
            'summary': self.summary,
            'description': self.description or self.summary or f'{self.title}是一本优秀的{self.category.category_name if self.category else "图书"}类图书，由{self.author}所著，内容丰富，值得一读。',
            'pub_date': self.publication_date.isoformat() if self.publication_date else None,
            'publishDate': self.publication_date.year if self.publication_date else None,  # 前端需要的字段
            'cover_url': self.cover_image or f'/static/images/{self.title}.jpg',  # 前端需要的字段
            'cover_image': self.cover_image,
            'total_copies': self.total_copies,
            'available_copies': self.available_copies,
            'stock': self.available_copies,  # 前端需要的字段
            'status': self.status,
            'language': self.language,
            'location': self.location,
            'publisher_obj': self.publisher.to_dict() if self.publisher else None,
            'category_obj': self.category.to_dict() if self.category else None
        }


class BookCategory(db.Model):
    """图书分类表"""
    __tablename__ = 'book_category'
    
    category_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    category_name = db.Column(db.String(50), nullable=False, unique=True, comment='分类名称')
    description = db.Column(db.String(200), comment='分类描述')

    def to_dict(self):
        """转换为字典"""
        return {
            'category_id': self.category_id,
            'category_name': self.category_name,
            'description': self.description
        }


class BookCopy(db.Model):
    """图书副本表"""
    __tablename__ = 'book_copy'
    
    copy_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    isbn = db.Column(db.String(20), db.ForeignKey('book_info.isbn'), nullable=False, comment='ISBN号')
    barcode = db.Column(db.String(30), unique=True, nullable=False, comment='条形码')
    storage_date = db.Column(db.Date, nullable=False, default=date.today, comment='入库日期')
    status = db.Column(db.String(10), nullable=False, default='在馆', comment='状态')
    location_id = db.Column(db.Integer, db.ForeignKey('location.location_id'), nullable=False, comment='位置编号')
    condition = db.Column(db.String(20), nullable=False, default='新书', comment='书籍状况')
    
    # 关系
    location = db.relationship('Location', backref='book_copies')
    borrow_records = db.relationship('BorrowRecord', backref='book_copy')

    def to_dict(self):
        """转换为字典"""
        return {
            'copy_id': self.copy_id,
            'isbn': self.isbn,
            'barcode': self.barcode,
            'storage_date': self.storage_date.isoformat() if self.storage_date else None,
            'status': self.status,
            'location_id': self.location_id,
            'condition': self.condition,
            'book_info': self.book_info.to_dict() if self.book_info else None,
            'location': self.location.to_dict() if self.location else None
        }


class Location(db.Model):
    """位置表"""
    __tablename__ = 'location'
    
    location_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    location_name = db.Column(db.String(50), nullable=False, unique=True, comment='位置名称')
    description = db.Column(db.String(200), comment='位置描述')

    def to_dict(self):
        """转换为字典"""
        return {
            'location_id': self.location_id,
            'location_name': self.location_name,
            'description': self.description
        }


class BorrowRecord(db.Model):
    """借阅记录表"""
    __tablename__ = 'borrow_record'
    
    record_id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    user_id = db.Column(db.Integer, db.ForeignKey('user.user_id'), nullable=False, comment='读者编号')
    copy_id = db.Column(db.Integer, db.ForeignKey('book_copy.copy_id'), nullable=False, comment='图书副本编号')
    borrow_date = db.Column(db.Date, nullable=False, default=date.today, comment='借出日期')
    due_date = db.Column(db.Date, nullable=False, comment='应还日期')
    return_date = db.Column(db.Date, comment='还入日期')
    status = db.Column(db.String(10), nullable=False, default='借出', comment='状态')
    operator_id = db.Column(db.Integer, db.ForeignKey('user.user_id'), nullable=False, comment='操作员编号')

    def to_dict(self):
        """转换为字典"""
        return {
            'record_id': self.record_id,
            'user_id': self.user_id,
            'copy_id': self.copy_id,
            'borrow_date': self.borrow_date.isoformat() if self.borrow_date else None,
            'due_date': self.due_date.isoformat() if self.due_date else None,
            'return_date': self.return_date.isoformat() if self.return_date else None,
            'status': self.status,
            'operator_id': self.operator_id,
            'user': self.user.to_dict() if self.user else None,
            'book_copy': self.book_copy.to_dict() if self.book_copy else None,
            'operator': self.operator.to_dict() if self.operator else None
        }


class SystemSettings(db.Model):
    """系统设置表"""
    __tablename__ = 'system_settings'
    
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)
    setting_key = db.Column(db.String(50), unique=True, nullable=False, comment='设置键')
    setting_value = db.Column(db.String(200), nullable=False, comment='设置值')
    description = db.Column(db.String(200), comment='设置描述')
    updated_at = db.Column(db.DateTime, default=datetime.now, onupdate=datetime.now, comment='更新时间')
    
    def to_dict(self):
        """转换为字典"""
        return {
            'id': self.id,
            'setting_key': self.setting_key,
            'setting_value': self.setting_value,
            'description': self.description,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None
        }


# 为了向后兼容，保留原有的类名映射
Book = BookInfo  # 将Book映射到BookInfo
