from app import create_app
from models import db, User, BookInfo, BookCopy, Publisher, BookCategory, Location, SystemSettings
from datetime import date, datetime
import os


def init_db():
    app = create_app()
    with app.app_context():
        db.drop_all()
        db.create_all()

        # create users
        admin = User(username='admin', user_type='admin', name='系统管理员')
        admin.set_password('admin123')

        user = User(username='user', user_type='reader', name='普通用户')
        user.set_password('user123')

        db.session.add(admin)
        db.session.add(user)

        # sample books (more comprehensive data)
        samples = [
            {'title': '三体', 'author': '刘慈欣', 'category': '科技', 'isbn': '9787229100605', 'publisher': '重庆出版社', 'pub_date': '2008-01-01'},
            {'title': '活着', 'author': '余华', 'category': '文学', 'isbn': '9787506365437', 'publisher': '作家出版社', 'pub_date': '2012-01-01'},
            {'title': '1984', 'author': '乔治·奥威尔', 'category': '文学', 'isbn': '9780141036144', 'publisher': '上海译文出版社', 'pub_date': '2010-01-01'},
            {'title': '银河系漫游指南', 'author': '道格拉斯·亚当斯', 'category': '科技', 'isbn': '9787532150074', 'publisher': '上海译文出版社', 'pub_date': '2018-01-01'},
            {'title': '人类简史', 'author': '尤瓦尔·赫拉利', 'category': '历史', 'isbn': '9787508649439', 'publisher': '中信出版社', 'pub_date': '2014-01-01'},
            {'title': '百年孤独', 'author': '加西亚·马尔克斯', 'category': '文学', 'isbn': '9787544748405', 'publisher': '南海出版公司', 'pub_date': '2011-01-01'},
            {'title': '人工智能', 'author': '李开复', 'category': '科技', 'isbn': '9787508674233', 'publisher': '中信出版社', 'pub_date': '2017-01-01'},
            {'title': '史记', 'author': '司马迁', 'category': '历史', 'isbn': '9787101003053', 'publisher': '中华书局', 'pub_date': '2013-01-01'},
            {'title': '艺术的故事', 'author': '贡布里希', 'category': '艺术', 'isbn': '9787549525589', 'publisher': '广西美术出版社', 'pub_date': '2014-01-01'},
            {'title': '教育心理学', 'author': '陈琦', 'category': '教育', 'isbn': '9787040196564', 'publisher': '高等教育出版社', 'pub_date': '2011-01-01'},
            {'title': '解忧杂货店', 'author': '东野圭吾', 'category': '文学', 'isbn': '9787539952567', 'publisher': '南海出版公司', 'pub_date': '2014-01-01'},
            {'title': '深度学习', 'author': 'Ian Goodfellow', 'category': '科技', 'isbn': '9781154614763', 'publisher': '人民邮电出版社', 'pub_date': '2017-01-01'},
            {'title': '梵高传', 'author': '欧文·斯通', 'category': '艺术', 'isbn': '9787532145223', 'publisher': '上海译文出版社', 'pub_date': '2015-01-01'},
            {'title': '明朝那些事儿', 'author': '当年明月', 'category': '历史', 'isbn': '9787501172914', 'publisher': '中国友谊出版公司', 'pub_date': '2006-01-01'},
            {'title': '给教师的建议', 'author': '苏霍姆林斯基', 'category': '教育', 'isbn': '9787533925066', 'publisher': '长江文艺出版社', 'pub_date': '2014-01-01'},
            {'title': '挪威的森林', 'author': '村上春树', 'category': '文学', 'isbn': '9787532746944', 'publisher': '上海译文出版社', 'pub_date': '2007-01-01'},
            {'title': '算法导论', 'author': 'Thomas H. Cormen', 'category': '科技', 'isbn': '9781114070107', 'publisher': '机械工业出版社', 'pub_date': '2013-01-01'},
            {'title': '中国通史', 'author': '吕思勉', 'category': '历史', 'isbn': '9787569901429', 'publisher': '北京时代华文书局', 'pub_date': '2014-01-01'},
            {'title': '西方美术史', 'author': '李春', 'category': '艺术', 'isbn': '9787300115624', 'publisher': '中国人民大学出版社', 'pub_date': '2010-01-01'},
            {'title': '教育学原理', 'author': '柳海民', 'category': '教育', 'isbn': '9787040237662', 'publisher': '高等教育出版社', 'pub_date': '2011-01-01'},
            {'title': 'JavaScript高级程序设计', 'author': 'Nicholas C. Zakas', 'category': '科技', 'isbn': '9781152757907', 'publisher': '人民邮电出版社', 'pub_date': '2012-01-01'},
            {'title': 'Python编程：从入门到实践', 'author': 'Eric Matthes', 'category': '科技', 'isbn': '9781154280288', 'publisher': '人民邮电出版社', 'pub_date': '2016-01-01'},
            {'title': '艺术哲学', 'author': '丹纳', 'category': '艺术', 'isbn': '9787532136597', 'publisher': '上海译文出版社', 'pub_date': '2013-01-01'},
            {'title': '论语', 'author': '孔子', 'category': '其他', 'isbn': '9787101003077', 'publisher': '中华书局', 'pub_date': '2012-01-01'},
        ]

        # Create default publisher and category first
        from models import Publisher, BookCategory
        
        # Create default publisher if not exists
        default_publisher = Publisher.query.filter_by(publisher_name='默认出版社').first()
        if not default_publisher:
            default_publisher = Publisher(publisher_name='默认出版社')
            db.session.add(default_publisher)
            db.session.flush()  # Get the ID without committing
        
        # Create default category if not exists
        default_category = BookCategory.query.filter_by(category_name='默认分类').first()
        if not default_category:
            default_category = BookCategory(category_name='默认分类')
            db.session.add(default_category)
            db.session.flush()  # Get the ID without committing
        
        # Create default location
        default_location = Location.query.filter_by(location_name='默认书库').first()
        if not default_location:
            default_location = Location(location_name='默认书库', description='系统默认书库')
            db.session.add(default_location)
            db.session.flush()

        created_books = []
        for s in samples:
            b = BookInfo(
                title=s['title'],
                author=s.get('author'),
                isbn=s.get('isbn'),
                publisher_id=default_publisher.publisher_id,
                category_id=default_category.category_id,
                price=30.00,  # Default price
                publication_date=datetime.fromisoformat(s['pub_date']).date() if s.get('pub_date') else None,
                language='中文',  # 设置语言
                description=f'{s["title"]}是一本优秀的{s["category"]}类图书，由{s["author"]}所著，内容丰富，值得一读。'  # 设置描述
            )
            db.session.add(b)
            db.session.flush()  # Get the book ID without committing
            created_books.append(b)

        # Create book copies for each book
        for book in created_books:
            copy_count = 0
            for copy_num in range(1, 6):  # Create 5 copies for each book
                copy = BookCopy(
                    isbn=book.isbn,
                    location_id=default_location.location_id,
                    barcode=f'{book.isbn}-{copy_num:03d}',  # Generate barcode
                    status='在馆'
                )
                db.session.add(copy)
                copy_count += 1
            
            # Update book copy counts
            book.total_copies = copy_count
            book.available_copies = copy_count

        db.session.commit()
        print('初始化完成，已创建用户 admin/user、若干图书和图书副本')


def init_system_settings():
    """初始化系统设置表"""
    app = create_app()
    with app.app_context():
        # 确保表存在
        db.create_all()
        
        # 检查是否已有设置，如果没有则创建默认设置
        existing_settings = SystemSettings.query.all()
        if not existing_settings:
            default_settings = [
                SystemSettings(setting_key='default_borrow_days', setting_value='30', description='默认借阅天数'),
                SystemSettings(setting_key='max_borrow_count', setting_value='5', description='单次最大借阅数量'),
                SystemSettings(setting_key='max_renewal_times', setting_value='2', description='允许续借次数'),
                SystemSettings(setting_key='overdue_fine_per_day', setting_value='0.5', description='逾期罚款（每天）'),
                SystemSettings(setting_key='max_fine_amount', setting_value='50', description='最高罚款上限'),
                SystemSettings(setting_key='reminder_days_before_due', setting_value='3', description='到期提醒提前天数')
            ]
            for setting in default_settings:
                db.session.add(setting)
            db.session.commit()
            print('系统设置表已初始化，已创建默认设置')
        else:
            print('系统设置表已存在，跳过初始化')


if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1 and sys.argv[1] == '--settings-only':
        init_system_settings()
    else:
        init_db()
        init_system_settings()
