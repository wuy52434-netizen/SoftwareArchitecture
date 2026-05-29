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
('科技', '科学技术类图书'),
('艺术', '艺术类图书'),
('教育', '教育类图书'),
('其他', '其他类别图书');

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

-- 默认用户（密码：admin/admin123，user1/123456）
INSERT INTO `user` (username, password, real_name, user_type, gender, phone, email, category_id, status) VALUES
('admin', '$2b$10$X7O/rhQWeBnwSxGQImZdSuIIM3cet7abxIMh7buCqO6x1vkizp9i.', '系统管理员', 'admin', '男', '13800000000', 'admin@example.com', 2, 'active'),
('user1', '$2b$10$GLGWOlSFJbzOMCpxWtmR8uiwfD2A5ARE81YKS6i1qIVJX0rPJj0Qq', '普通读者', 'reader', '女', '13900000000', 'reader@example.com', 1, 'active');

-- 演示图书数据
INSERT INTO book_info (isbn, title, author, publisher_id, publish_date, category_id, price, summary, cover_url, total_copies, available_copies, status, location) VALUES
('9787229100605', '三体', '刘慈欣', 2, '2008-01-01', 3, 68.00, '中国科幻巅峰之作，讲述地球文明与三体文明的宇宙对决', '/static/images/三体.jpg', 5, 5, 'available', 'C区-科技书架'),
('9787506365437', '活着', '余华', 1, '2012-08-01', 1, 29.00, '讲述一个人一生的故事，通过苦难展现生命的韧性', '/static/images/活着.jpg', 4, 4, 'available', 'A区-文学书架'),
('9780141036144', '1984', '乔治·奥威尔', 3, '2009-07-01', 1, 38.00, '反乌托邦文学经典，对极权主义的深刻批判', '/static/images/1984.jpg', 3, 3, 'available', 'A区-文学书架'),
('9787532150074', '银河系漫游指南', '道格拉斯·亚当斯', 5, '2005-06-01', 3, 32.00, '科幻喜剧经典，关于宇宙生命和一切的终极答案', '/static/images/银河系漫游指南.jpg', 3, 3, 'available', 'C区-科技书架'),
('9787508649439', '人类简史', '尤瓦尔·赫拉利', 3, '2014-11-01', 2, 68.00, '从动物到上帝的人类进化史', '/static/images/人类简史.jpg', 4, 4, 'available', 'B区-历史书架'),
('9787544748405', '百年孤独', '加西亚·马尔克斯', 1, '2011-06-01', 1, 55.00, '魔幻现实主义文学经典，布恩迪亚家族百年兴衰', '/static/images/百年孤独.jpg', 3, 3, 'available', 'A区-文学书架'),
('9787508674233', '人工智能', '李开复', 3, '2017-05-01', 3, 58.00, '人工智能时代的思考与展望', '/static/images/人工智能.jpg', 4, 4, 'available', 'C区-科技书架'),
('9787101003053', '史记', '司马迁', 1, '2006-01-01', 2, 126.00, '中国第一部纪传体通史，二十四史之首', '/static/images/史记.jpg', 2, 2, 'available', 'B区-历史书架'),
('9787549525589', '艺术的故事', '贡布里希', 5, '2008-04-01', 4, 172.00, '西方艺术史入门经典', '/static/images/艺术的故事.jpg', 3, 3, 'available', 'E区-艺术书架'),
('9787040196564', '教育心理学', '陈琦', 6, '2007-06-01', 5, 49.00, '教育心理学权威教材', '/static/images/教育心理学.jpg', 4, 4, 'available', 'D区-教育书架'),
('9787539952567', '解忧杂货店', '东野圭吾', 1, '2014-05-01', 1, 39.50, '温暖人心的奇幻故事', '/static/images/解忧杂货店.jpg', 5, 5, 'available', 'A区-文学书架'),
('9781154614763', '深度学习', 'Ian Goodfellow', 4, '2017-08-01', 3, 168.00, '深度学习领域经典教材', '/static/images/深度学习.jpg', 3, 3, 'available', 'C区-科技书架'),
('9787532145223', '梵高传', '欧文·斯通', 5, '2015-01-01', 4, 45.00, '梵高的艺术人生传记', '/static/images/梵高传.jpg', 2, 2, 'available', 'E区-艺术书架'),
('9787501172914', '明朝那些事儿', '当年明月', 3, '2009-04-01', 2, 208.00, '以通俗方式讲述明朝历史', '/static/images/明朝那些事儿.jpg', 4, 4, 'available', 'B区-历史书架'),
('9787533925066', '给教师的建议', '苏霍姆林斯基', 6, '2005-01-01', 5, 32.00, '教育学经典著作', '/static/images/给教师的建议.jpg', 3, 3, 'available', 'D区-教育书架'),
('9787532746944', '挪威的森林', '村上春树', 5, '2010-09-01', 1, 36.00, '村上春树最具影响力的小说', '/static/images/挪威的森林.jpg', 4, 4, 'available', 'A区-文学书架'),
('9781114070107', '算法导论', 'Thomas H. Cormen', 2, '2013-01-01', 3, 128.00, '计算机算法权威教材', '/static/images/算法导论.jpg', 2, 2, 'available', 'C区-科技书架'),
('9787569901429', '中国通史', '吕思勉', 3, '2016-05-01', 2, 56.00, '中国通史经典读本', '/static/images/中国通史.jpg', 3, 3, 'available', 'B区-历史书架'),
('9787300115624', '西方美术史', '李春', 6, '2010-06-01', 4, 48.00, '西方美术发展史教材', '/static/images/西方美术史.jpg', 2, 2, 'available', 'E区-艺术书架'),
('9787040237662', '教育学原理', '柳海民', 6, '2011-01-01', 5, 39.00, '教育学核心教材', '/static/images/教育学原理.jpg', 3, 3, 'available', 'D区-教育书架'),
('9781152757907', 'JavaScript高级程序设计', 'Nicholas C. Zakas', 4, '2020-10-01', 3, 129.00, '前端开发经典教程', '/static/images/JavaScript高级程序设计.jpg', 3, 3, 'available', 'C区-科技书架'),
('9781154280288', 'Python编程：从入门到实践', 'Eric Matthes', 4, '2020-01-01', 3, 89.00, 'Python入门经典教程', '/static/images/Python编程：从入门到实践.jpg', 4, 4, 'available', 'C区-科技书架'),
('9787532136597', '艺术哲学', '丹纳', 5, '2016-08-01', 4, 42.00, '艺术哲学经典著作', '/static/images/艺术哲学.jpg', 2, 2, 'available', 'E区-艺术书架'),
('9787101003077', '论语', '孔子', 1, '2006-01-01', 6, 26.00, '儒家经典，影响深远的思想著作', '/static/images/论语.jpg', 5, 5, 'available', 'A区-文学书架');

-- 为每本图书生成可借副本，确保自助借书不需要手动传 copyId
INSERT INTO book_copy (book_id, barcode, location_id, status, book_condition)
SELECT
    bi.id,
    CONCAT('BC', LPAD(bi.id, 4, '0'), LPAD(n.n, 3, '0')),
    CASE
        WHEN bi.category_id = 2 THEN 2
        WHEN bi.category_id = 3 THEN 3
        WHEN bi.category_id = 4 THEN 5
        WHEN bi.category_id = 5 THEN 4
        ELSE 1
    END AS location_id,
    'available',
    'new'
FROM book_info bi
JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
    UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
    UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
) n ON n.n <= bi.total_copies;
