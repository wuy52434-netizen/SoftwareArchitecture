-- 图书自动借书机系统 - 数据库初始化脚本
-- 数据库: library
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci

-- ============================================
-- 创建数据库
-- ============================================
CREATE DATABASE IF NOT EXISTS library DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE library;

-- ============================================
-- 读者分类表
-- ============================================
CREATE TABLE IF NOT EXISTS reader_category (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    category_name VARCHAR(20) NOT NULL COMMENT '分类名称',
    max_borrow INT NOT NULL DEFAULT 5 COMMENT '最大借阅数量',
    loan_period INT NOT NULL DEFAULT 30 COMMENT '借阅期限(天)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='读者分类表';

-- ============================================
-- 用户表
-- ============================================
CREATE TABLE IF NOT EXISTS `user` (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    user_type VARCHAR(20) NOT NULL DEFAULT 'reader' COMMENT '用户类型: admin/reader',
    gender VARCHAR(10) COMMENT '性别: 男/女',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    category_id BIGINT COMMENT '读者分类ID',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/inactive',
    last_login_time DATETIME COMMENT '最后登录时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0/1',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_phone (phone),
    INDEX idx_user_type (user_type),
    FOREIGN KEY (category_id) REFERENCES reader_category(category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 出版社表
-- ============================================
CREATE TABLE IF NOT EXISTS publisher (
    publisher_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '出版社ID',
    publisher_name VARCHAR(100) NOT NULL COMMENT '出版社名称',
    address VARCHAR(200) COMMENT '地址',
    phone VARCHAR(20) COMMENT '电话',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_publisher_name (publisher_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出版社表';

-- ============================================
-- 图书分类表
-- ============================================
CREATE TABLE IF NOT EXISTS book_category (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    category_name VARCHAR(50) NOT NULL UNIQUE COMMENT '分类名称',
    description VARCHAR(200) COMMENT '分类描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

-- ============================================
-- 图书信息表
-- ============================================
CREATE TABLE IF NOT EXISTS book_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '图书ID',
    isbn VARCHAR(20) NOT NULL UNIQUE COMMENT 'ISBN号',
    title VARCHAR(200) NOT NULL COMMENT '书名',
    author VARCHAR(100) NOT NULL COMMENT '作者',
    publisher_id BIGINT NOT NULL COMMENT '出版社ID',
    publish_date DATE COMMENT '出版日期',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    summary TEXT COMMENT '内容简介',
    cover_url VARCHAR(500) COMMENT '封面图片URL',
    total_copies INT NOT NULL DEFAULT 0 COMMENT '总副本数',
    available_copies INT NOT NULL DEFAULT 0 COMMENT '可借副本数',
    status VARCHAR(20) NOT NULL DEFAULT 'available' COMMENT '状态: available/borrowed/frozen',
    language VARCHAR(20) DEFAULT '中文' COMMENT '语言',
    location VARCHAR(100) COMMENT '馆藏位置',
    description TEXT COMMENT '图书描述',
    borrow_count INT NOT NULL DEFAULT 0 COMMENT '借阅次数',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0/1',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_isbn (isbn),
    INDEX idx_title (title),
    INDEX idx_category (category_id),
    INDEX idx_author (author),
    INDEX idx_status (status),
    FOREIGN KEY (publisher_id) REFERENCES publisher(publisher_id),
    FOREIGN KEY (category_id) REFERENCES book_category(category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书信息表';

-- ============================================
-- 馆藏位置表
-- ============================================
CREATE TABLE IF NOT EXISTS location (
    location_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '位置ID',
    location_name VARCHAR(50) NOT NULL UNIQUE COMMENT '位置名称',
    description VARCHAR(200) COMMENT '位置描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='馆藏位置表';

-- ============================================
-- 图书副本表
-- ============================================
CREATE TABLE IF NOT EXISTS book_copy (
    copy_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '副本ID',
    book_id BIGINT NOT NULL COMMENT '图书ID',
    barcode VARCHAR(50) NOT NULL UNIQUE COMMENT '条形码',
    location_id BIGINT NOT NULL COMMENT '馆藏位置ID',
    status VARCHAR(20) NOT NULL DEFAULT 'available' COMMENT '状态: available/borrowed/damaged/lost',
    book_condition VARCHAR(50) DEFAULT 'new' COMMENT '书籍状况',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0/1',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_book_id (book_id),
    INDEX idx_barcode (barcode),
    INDEX idx_status (status),
    FOREIGN KEY (book_id) REFERENCES book_info(id),
    FOREIGN KEY (location_id) REFERENCES location(location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书副本表';

-- ============================================
-- 借阅记录表
-- ============================================
CREATE TABLE IF NOT EXISTS borrow_record (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '借阅记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT COMMENT '图书ID',
    copy_id BIGINT NOT NULL COMMENT '副本ID',
    borrow_date DATE NOT NULL COMMENT '借阅日期',
    due_date DATE NOT NULL COMMENT '应还日期',
    return_date DATE COMMENT '实际归还日期',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active/returned/overdue',
    fine_amount DECIMAL(10,2) DEFAULT 0 COMMENT '罚款金额',
    fine_paid TINYINT DEFAULT 0 COMMENT '是否已缴费: 0/1',
    operator_id BIGINT COMMENT '操作人ID',
    remark VARCHAR(500) COMMENT '备注',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0/1',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_copy_id (copy_id),
    INDEX idx_status (status),
    INDEX idx_due_date (due_date),
    INDEX idx_borrow_date (borrow_date),
    FOREIGN KEY (user_id) REFERENCES `user`(user_id),
    FOREIGN KEY (copy_id) REFERENCES book_copy(copy_id),
    FOREIGN KEY (operator_id) REFERENCES `user`(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

-- ============================================
-- 系统设置表
-- ============================================
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL UNIQUE COMMENT '设置键',
    setting_value VARCHAR(500) NOT NULL COMMENT '设置值',
    description VARCHAR(500) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置表';

-- ============================================
-- 站内消息表
-- ============================================
CREATE TABLE IF NOT EXISTS inner_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '接收用户ID',
    title VARCHAR(200) NOT NULL COMMENT '消息标题',
    content TEXT COMMENT '消息内容',
    message_type VARCHAR(20) NOT NULL DEFAULT 'system' COMMENT '消息类型: system/borrow/return/fine',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读: 0/1',
    read_time DATETIME COMMENT '阅读时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    FOREIGN KEY (user_id) REFERENCES `user`(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';

-- ============================================
-- 初始化数据
-- ============================================

-- 读者分类
INSERT INTO reader_category (category_name, max_borrow, loan_period) VALUES
('普通读者', 5, 30),
('教师', 10, 60),
('学生', 5, 30),
('VIP读者', 15, 90);

-- 图书分类
INSERT INTO book_category (category_name, description) VALUES
('文学', '文学类图书'),
('历史', '历史类图书'),
('哲学', '哲学类图书'),
('艺术', '艺术类图书'),
('教育', '教育类图书'),
('计算机', '计算机技术类图书'),
('科学', '自然科学类图书'),
('经济', '经济管理类图书');

-- 出版社
INSERT INTO publisher (publisher_name, address, phone) VALUES
('人民文学出版社', '北京市朝阳区建国门内大街166号', '010-65252968'),
('机械工业出版社', '北京市西城区百万庄大街22号', '010-88379203'),
('中信出版社', '北京市朝阳区惠新东街甲4号', '010-84849283'),
('电子工业出版社', '北京市海淀区万寿路173信箱', '010-88254888'),
('三联书店', '北京市东城区美术馆东街22号', '010-64002728'),
('人民教育出版社', '北京市海淀区中关村南大街17号院1号楼', '010-58758866');

-- 馆藏位置
INSERT INTO location (location_name, description) VALUES
('A区-文学书架', 'A区第1-5排，文学类图书'),
('B区-历史书架', 'B区第1-4排，历史类图书'),
('C区-科技书架', 'C区第1-6排，科技类图书'),
('D区-教育书架', 'D区第1-3排，教育类图书'),
('E区-新书展示区', 'E区，最新到馆图书');

-- 系统设置
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('default_borrow_days', '30', '默认借阅天数'),
('max_borrow_count', '5', '最大借阅数量'),
('max_renewal_times', '2', '最大续借次数'),
('overdue_fine_per_day', '0.5', '逾期每天罚款(元)'),
('max_fine_amount', '50', '最高罚款上限(元)'),
('reminder_days_before_due', '3', '到期提醒提前天数'),
('library_name', '智慧图书馆', '图书馆名称'),
('system_version', '1.0.0', '系统版本');
