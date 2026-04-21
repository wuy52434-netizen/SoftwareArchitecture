import requests
from pprint import pprint

BASE = 'http://127.0.0.1:5000'

def run():
    s = requests.Session()
    print('登录 admin...')
    r = s.post(f'{BASE}/api/login', json={'username': 'admin', 'password': 'admin123'})
    pprint(r.json())

    print('\n获取图书列表...')
    r = s.get(f'{BASE}/api/books')
    data = r.json()
    pprint(data)

    books = data.get('books') or []
    if not books:
        print('没有图书可借，测试结束')
        return

    book_id = books[0]['id']
    print(f'尝试借书 id={book_id}...')
    r = s.post(f'{BASE}/api/borrow', json={'book_id': book_id, 'reader_id': 2, 'borrow_date': '2025-12-01', 'return_date': '2025-12-30', 'note': '测试借阅'})
    borrow = r.json()
    pprint(borrow)

    if not borrow.get('ok'):
        print('借书失败，结束')
        return

    borrow_id = borrow.get('borrow', {}).get('id')
    print(f'尝试还书 borrow_id={borrow_id}...')
    r = s.post(f'{BASE}/api/return', json={'borrow_id': borrow_id})
    pprint(r.json())

if __name__ == '__main__':
    run()
