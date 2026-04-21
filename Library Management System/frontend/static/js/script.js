// 图书数据存储
let books = [];
let nextId = 1;

// 用户数据存储
let users = [
    {
        id: 1,
        username: 'admin',
        role: 'admin',
        realName: '系统管理员',
        email: 'admin@library.com',
        registerTime: '2024-01-01',
        status: '正常'
    },
    {
        id: 2,
        username: 'user',
        role: 'user',
        realName: '张三',
        email: 'zhangsan@email.com',
        registerTime: '2024-03-15',
        status: '正常'
    },
    {
        id: 3,
        username: 'lisi',
        role: 'user',
        realName: '李四',
        email: 'lisi@email.com',
        registerTime: '2024-05-20',
        status: '正常'
    },
    {
        id: 4,
        username: 'wangwu',
        role: 'user',
        realName: '王五',
        email: 'wangwu@email.com',
        registerTime: '2024-06-10',
        status: '正常'
    },
    {
        id: 5,
        username: 'zhaoliu',
        role: 'user',
        realName: '赵六',
        email: 'zhaoliu@email.com',
        registerTime: '2024-07-08',
        status: '禁用'
    },
    {
        id: 6,
        username: 'sunqi',
        role: 'user',
        realName: '孙七',
        email: 'sunqi@email.com',
        registerTime: '2024-08-22',
        status: '正常'
    },
    {
        id: 7,
        username: 'zhouba',
        role: 'user',
        realName: '周八',
        email: 'zhouba@email.com',
        registerTime: '2024-09-05',
        status: '正常'
    },
    {
        id: 8,
        username: 'wujiu',
        role: 'user',
        realName: '吴九',
        email: 'wujiu@email.com',
        registerTime: '2024-10-12',
        status: '正常'
    },
    {
        id: 9,
        username: 'zhengshi',
        role: 'user',
        realName: '郑十',
        email: 'zhengshi@email.com',
        registerTime: '2024-11-01',
        status: '正常'
    },
    {
        id: 10,
        username: 'libmanager',
        role: 'admin',
        realName: '图书馆管理员',
        email: 'manager@library.com',
        registerTime: '2024-02-01',
        status: '正常'
    }
];

// 初始化示例数据
function initSampleData() {
    const sampleBooks = [
        {
            id: nextId++,
            title: '活着',
            author: '余华',
            category: '文学',
            isbn: '978-7-5302-1007-4',
            publisher: '作家出版社',
            publishDate: '2012-08-01',
            status: '可借阅',
            description: '一个关于生命的故事',
            coverImage: 'images/活着.jpg'
        },
        {
            id: nextId++,
            title: '三体',
            author: '刘慈欣',
            category: '科技',
            isbn: '978-7-5366-9293-0',
            publisher: '重庆出版社',
            publishDate: '2008-01-01',
            status: '可借阅',
            description: '科幻小说经典之作',
            coverImage: 'images/三体.jpg'
        },
        {
            id: nextId++,
            title: '人类简史',
            author: '尤瓦尔·赫拉利',
            category: '历史',
            isbn: '978-7-5086-4943-9',
            publisher: '中信出版社',
            publishDate: '2014-11-01',
            status: '已借出',
            description: '从动物到上帝',
            coverImage: 'images/人类简史.jpg'
        },
        {
            id: nextId++,
            title: '百年孤独',
            author: '加西亚·马尔克斯',
            category: '文学',
            isbn: '978-7-5447-4840-5',
            publisher: '南海出版公司',
            publishDate: '2011-06-01',
            status: '可借阅',
            description: '魔幻现实主义代表作',
            coverImage: 'images/百年孤独.jpg'
        },
        {
            id: nextId++,
            title: '人工智能',
            author: '李开复',
            category: '科技',
            isbn: '978-7-5086-7423-3',
            publisher: '中信出版社',
            publishDate: '2017-05-01',
            status: '可借阅',
            description: '关于AI的未来',
            coverImage: 'images/人工智能.jpg'
        }
    ];
    
    books = sampleBooks;
    saveToLocalStorage();
}

// 本地存储操作
function saveToLocalStorage() {
    localStorage.setItem('libraryBooks', JSON.stringify(books));
    localStorage.setItem('libraryNextId', nextId.toString());
}

function loadFromLocalStorage() {
    const savedBooks = localStorage.getItem('libraryBooks');
    const savedNextId = localStorage.getItem('libraryNextId');
    
    if (savedBooks) {
        books = JSON.parse(savedBooks);
        nextId = savedNextId ? parseInt(savedNextId) : 1;
    } else {
        initSampleData();
    }
}

// 标签页切换
function initTabs() {
    const navBtns = document.querySelectorAll('.nav-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    
    navBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');
            const targetTabEl = document.getElementById(`${targetTab}-tab`);
            
            if (!targetTabEl) return; // 如果目标标签页不存在则跳过
            
            // 移除所有活动状态
            navBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(tab => tab.classList.remove('active'));
            
            // 添加活动状态
            btn.classList.add('active');
            targetTabEl.classList.add('active');
            
            // 如果切换到统计页面，更新统计数据
            if (targetTab === 'stats') {
                updateStatistics();
            }
            
            // 如果切换到用户管理页面，渲染用户列表
            if (targetTab === 'users') {
                renderUsers();
            }
        });
    });
}

// 显示通知消息
function showNotification(message, type = 'success') {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.className = `notification ${type} show`;
    
    setTimeout(() => {
        notification.classList.remove('show');
    }, 3000);
}

// 渲染图书列表
function renderBooks(booksToRender = books) {
    const tbody = document.getElementById('books-tbody');
    
    if (booksToRender.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="9" style="text-align: center; padding: 40px;">
                    <div class="empty-state">
                        <h3>暂无图书数据</h3>
                        <p>请添加新图书或调整筛选条件</p>
                    </div>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = booksToRender.map(book => `
        <tr>
            <td>${book.id}</td>
            <td>
                <img src="${book.coverImage || 'images/default.jpg'}" 
                     alt="${book.title}" 
                     style="width: 50px; height: 70px; object-fit: cover; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);"
                     onerror="this.src='images/default.jpg'">
            </td>
            <td><strong>${book.title}</strong></td>
            <td>${book.author}</td>
            <td>${book.category}</td>
            <td>${book.isbn}</td>
            <td>${book.publishDate || '-'}</td>
            <td>
                <span class="status-badge ${book.status === '可借阅' ? 'status-available' : 'status-borrowed'}">
                    ${book.status}
                </span>
            </td>
            <td>
                <div class="action-btns">
                    <button class="btn-edit" onclick="openEditModal(${book.id})">编辑</button>
                    <button class="btn-toggle" onclick="toggleBookStatus(${book.id})">
                        ${book.status === '可借阅' ? '借出' : '归还'}
                    </button>
                    <button class="btn-delete" onclick="deleteBook(${book.id})">删除</button>
                </div>
            </td>
        </tr>
    `).join('');
}

// 搜索和筛选
function initSearch() {
    const searchInput = document.getElementById('search-input');
    const searchBtn = document.getElementById('search-btn');
    const categoryFilter = document.getElementById('category-filter');
    const statusFilter = document.getElementById('status-filter');
    
    function performSearch() {
        const searchTerm = searchInput.value.toLowerCase().trim();
        const category = categoryFilter.value;
        const status = statusFilter.value;
        
        let filteredBooks = books;
        
        // 搜索过滤
        if (searchTerm) {
            filteredBooks = filteredBooks.filter(book =>
                book.title.toLowerCase().includes(searchTerm) ||
                book.author.toLowerCase().includes(searchTerm) ||
                book.isbn.toLowerCase().includes(searchTerm)
            );
        }
        
        // 分类过滤
        if (category) {
            filteredBooks = filteredBooks.filter(book => book.category === category);
        }
        
        // 状态过滤
        if (status) {
            filteredBooks = filteredBooks.filter(book => book.status === status);
        }
        
        renderBooks(filteredBooks);
    }
    
    searchBtn.addEventListener('click', performSearch);
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            performSearch();
        }
    });
    categoryFilter.addEventListener('change', performSearch);
    statusFilter.addEventListener('change', performSearch);
}

// 添加图书
function initAddBookForm() {
    const form = document.getElementById('add-book-form');
    if (!form) return; // 如果表单不存在则跳过
    
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        
        const newBook = {
            id: nextId++,
            title: document.getElementById('book-title').value.trim(),
            author: document.getElementById('book-author').value.trim(),
            category: document.getElementById('book-category').value,
            isbn: document.getElementById('book-isbn').value.trim(),
            publisher: document.getElementById('book-publisher').value.trim(),
            publishDate: document.getElementById('book-date').value,
            status: '可借阅',
            description: document.getElementById('book-description').value.trim(),
            coverImage: document.getElementById('book-cover').value.trim() || 'images/default.jpg'
        };
        
        books.push(newBook);
        saveToLocalStorage();
        renderBooks();
        form.reset();
        
        showNotification('图书添加成功！', 'success');
        
        // 切换到图书列表页面
        const booksTab = document.querySelector('[data-tab="books"]');
        if (booksTab) booksTab.click();
    });
}

// 编辑图书
function openEditModal(bookId) {
    const book = books.find(b => b.id === bookId);
    if (!book) return;
    
    const modal = document.getElementById('edit-modal');
    if (!modal) return; // 如果模态框不存在则跳过
    
    const idEl = document.getElementById('edit-book-id');
    const titleEl = document.getElementById('edit-book-title');
    const authorEl = document.getElementById('edit-book-author');
    const categoryEl = document.getElementById('edit-book-category');
    const isbnEl = document.getElementById('edit-book-isbn');
    const publisherEl = document.getElementById('edit-book-publisher');
    const dateEl = document.getElementById('edit-book-date');
    const statusEl = document.getElementById('edit-book-status');
    const coverEl = document.getElementById('edit-book-cover');
    
    if (idEl) idEl.value = book.id;
    if (titleEl) titleEl.value = book.title;
    if (authorEl) authorEl.value = book.author;
    if (categoryEl) categoryEl.value = book.category;
    if (isbnEl) isbnEl.value = book.isbn;
    if (publisherEl) publisherEl.value = book.publisher || '';
    if (dateEl) dateEl.value = book.publishDate || '';
    if (statusEl) statusEl.value = book.status;
    if (coverEl) coverEl.value = book.coverImage || '';
    
    modal.classList.add('active');
}

function closeEditModal() {
    const modal = document.getElementById('edit-modal');
    if (modal) modal.classList.remove('active');
}

function initEditForm() {
    const form = document.getElementById('edit-book-form');
    if (!form) return; // 如果表单不存在则跳过
    
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        
        const bookId = parseInt(document.getElementById('edit-book-id').value);
        const bookIndex = books.findIndex(b => b.id === bookId);
        
        if (bookIndex !== -1) {
            books[bookIndex] = {
                ...books[bookIndex],
                title: document.getElementById('edit-book-title').value.trim(),
                author: document.getElementById('edit-book-author').value.trim(),
                category: document.getElementById('edit-book-category').value,
                isbn: document.getElementById('edit-book-isbn').value.trim(),
                publisher: document.getElementById('edit-book-publisher').value.trim(),
                publishDate: document.getElementById('edit-book-date').value,
                status: document.getElementById('edit-book-status').value,
                coverImage: document.getElementById('edit-book-cover').value.trim() || books[bookIndex].coverImage
            };
            
            saveToLocalStorage();
            renderBooks();
            closeEditModal();
            showNotification('图书信息更新成功！', 'success');
        }
    });
    
    // 点击模态框背景关闭
    const modal = document.getElementById('edit-modal');
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target.id === 'edit-modal') {
                closeEditModal();
            }
        });
    }
}

// 切换图书状态
function toggleBookStatus(bookId) {
    const book = books.find(b => b.id === bookId);
    if (!book) return;
    
    book.status = book.status === '可借阅' ? '已借出' : '可借阅';
    saveToLocalStorage();
    renderBooks();
    
    const action = book.status === '已借出' ? '借出' : '归还';
    showNotification(`《${book.title}》已${action}`, 'info');
}

// 删除图书
function deleteBook(bookId) {
    const book = books.find(b => b.id === bookId);
    if (!book) return;
    
    if (confirm(`确定要删除《${book.title}》吗？此操作无法撤销。`)) {
        books = books.filter(b => b.id !== bookId);
        saveToLocalStorage();
        renderBooks();
        showNotification('图书删除成功', 'success');
    }
}

// 更新统计数据
function updateStatistics() {
    const totalBooks = books.length;
    const availableBooks = books.filter(b => b.status === '可借阅').length;
    const borrowedBooks = books.filter(b => b.status === '已借出').length;
    
    // 检查元素是否存在
    const totalBooksEl = document.getElementById('total-books');
    const availableBooksEl = document.getElementById('available-books');
    const borrowedBooksEl = document.getElementById('borrowed-books');
    
    if (totalBooksEl) totalBooksEl.textContent = totalBooks;
    if (availableBooksEl) availableBooksEl.textContent = availableBooks;
    if (borrowedBooksEl) borrowedBooksEl.textContent = borrowedBooks;
    
    // 分类统计
    const categoryStats = {};
    books.forEach(book => {
        categoryStats[book.category] = (categoryStats[book.category] || 0) + 1;
    });
    
    const maxCount = Math.max(...Object.values(categoryStats), 1);
    const chartContainer = document.getElementById('category-chart');
    
    if (!chartContainer) return; // 如果图表容器不存在则跳过
    
    chartContainer.innerHTML = Object.entries(categoryStats)
        .sort((a, b) => b[1] - a[1])
        .map(([category, count]) => {
            const percentage = (count / maxCount) * 100;
            return `
                <div class="category-item">
                    <div class="category-label">${category}</div>
                    <div class="category-bar-container">
                        <div class="category-bar" style="width: ${percentage}%">
                            <span class="category-count">${count} 本</span>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
}

// 渲染用户列表
function renderUsers() {
    const tbody = document.getElementById('users-tbody');
    if (!tbody) return;
    
    if (users.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 40px; color: #94a3b8;">暂无用户数据</td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = users.map(user => `
        <tr>
            <td>${user.id}</td>
            <td><strong>${user.username}</strong></td>
            <td>
                <span class="badge ${user.role === 'admin' ? 'badge-admin' : 'badge-user'}">
                    ${user.role === 'admin' ? '管理员' : '普通用户'}
                </span>
            </td>
            <td>${user.realName}</td>
            <td>${user.email}</td>
            <td>${user.registerTime}</td>
            <td>
                <span class="status-badge ${user.status === '正常' ? 'status-active' : 'status-disabled'}">
                    ${user.status}
                </span>
            </td>
            <td class="action-btns">
                <button class="btn-edit" onclick="editUser(${user.id})" title="编辑">
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                </button>
                <button class="btn-delete" onclick="deleteUser(${user.id})" title="删除" ${user.role === 'admin' ? 'disabled' : ''}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                </button>
            </td>
        </tr>
    `).join('');
}

// 编辑用户（占位函数）
function editUser(userId) {
    const user = users.find(u => u.id === userId);
    if (!user) return;
    showNotification(`编辑用户功能开发中：${user.username}`, 'info');
}

// 删除用户
function deleteUser(userId) {
    const user = users.find(u => u.id === userId);
    if (!user) return;
    
    if (user.role === 'admin') {
        showNotification('不能删除管理员账户！', 'error');
        return;
    }
    
    if (confirm(`确定要删除用户 ${user.username} (${user.realName}) 吗？`)) {
        users = users.filter(u => u.id !== userId);
        renderUsers();
        showNotification('用户删除成功', 'success');
    }
}

// 添加用户模态框（占位函数）
function showAddUserModal() {
    showNotification('添加用户功能开发中...', 'info');
}

// 系统通知数据
const systemNotifications = [
    {
        id: 1,
        type: 'warning',
        title: '图书即将到期提醒',
        message: '有2本图书将在3天内到期，请提醒读者及时归还',
        time: '2小时前',
        unread: true
    },
    {
        id: 2,
        type: 'success',
        title: '新书入库通知',
        message: '《人工智能简史》等5本新书已成功入库',
        time: '5小时前',
        unread: true
    },
    {
        id: 3,
        type: 'info',
        title: '系统维护通知',
        message: '系统将于本周六22:00-24:00进行例行维护',
        time: '1天前',
        unread: false
    }
];

// 渲染系统通知列表
function renderNotifications() {
    const notificationsList = document.getElementById('notifications-list');
    const notificationCount = document.getElementById('notification-count');
    
    if (!notificationsList) return;
    
    // 更新未读通知数量
    const unreadCount = systemNotifications.filter(n => n.unread).length;
    if (notificationCount) {
        notificationCount.textContent = unreadCount;
    }
    
    // 渲染通知列表
    notificationsList.innerHTML = systemNotifications.map(notification => {
        const iconSvg = {
            'info': '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>',
            'warning': '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>',
            'success': '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>'
        };
        
        return `
            <div class="notification-item ${notification.unread ? 'unread' : ''} ${notification.type}" onclick="markNotificationAsRead(${notification.id})">
                <div class="notification-icon-wrapper ${notification.type}">
                    ${iconSvg[notification.type]}
                </div>
                <div class="notification-content">
                    <div class="notification-title">${notification.title}</div>
                    <div class="notification-message">${notification.message}</div>
                    <div class="notification-time">${notification.time}</div>
                </div>
            </div>
        `;
    }).join('');
}

// 标记通知为已读
function markNotificationAsRead(notificationId) {
    const notification = systemNotifications.find(n => n.id === notificationId);
    if (notification && notification.unread) {
        notification.unread = false;
        renderNotifications();
        showNotification('通知已标记为已读', 'success');
    }
}

// 查看全部通知
function viewAllNotifications() {
    showNotification('即将跳转到通知中心', 'info');
    // 这里可以跳转到专门的通知页面
}

// 登出处理函数
function handleLogout() {
    if (confirm('确定要退出登录吗？')) {
        showNotification('已退出登录', 'success');
        // 清除登录信息
        sessionStorage.removeItem('currentUser');
        setTimeout(() => {
            window.location.href = 'login.html';
        }, 1000);
    }
}

// 通知图标点击处理
function initNotificationIcon() {
    const notificationIcon = document.getElementById('notification-icon');
    if (notificationIcon) {
        notificationIcon.addEventListener('click', () => {
            showNotification('您有3条新通知', 'success');
            // 这里可以添加显示通知列表的逻辑
        });
    }
}

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    loadFromLocalStorage();
    initTabs();
    initSearch();
    initAddBookForm();
    initEditForm();
    initNotificationIcon();
    renderBooks();
    updateStatistics();
    renderNotifications();
    
    console.log('图书管理系统已加载，当前图书数量：', books.length);
});
