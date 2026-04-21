// 管理员界面JavaScript
let books = [];
let users = [];
let currentPage = 1;
let pageSize = 10;
let totalPages = 1;

// API基础URL - 使用相对路径
const API_BASE = '/api';

// 显示通知
function showNotification(message, type = 'success') {
    const notification = document.getElementById('notification');
    notification.textContent = message;
    notification.className = `notification ${type} show`;
    
    setTimeout(() => {
        notification.classList.remove('show');
    }, 3000);
}

// 标签页切换
function initTabs() {
    const navBtns = document.querySelectorAll('.nav-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    
    navBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');
            
            // 移除所有活动状态
            navBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(tab => tab.classList.remove('active'));
            
            // 添加活动状态
            btn.classList.add('active');
            document.getElementById(`${targetTab}-tab`).classList.add('active');
            
            // 根据标签页加载数据
            if (targetTab === 'books') {
                loadBooks();
            } else if (targetTab === 'users') {
                loadUsers();
            }
        });
    });
}

// 从API加载图书数据
async function loadBooks(page = 1) {
    try {
        const searchInput = document.getElementById('search-input');
        const categoryFilter = document.getElementById('category-filter');
        const statusFilter = document.getElementById('status-filter');
        
        // 构建查询参数
        let url = `${API_BASE}/books?page=${page}&per_page=${pageSize}`;
        
        if (searchInput && searchInput.value.trim()) {
            url += `&search=${encodeURIComponent(searchInput.value.trim())}`;
        }
        if (categoryFilter && categoryFilter.value) {
            url += `&category=${encodeURIComponent(categoryFilter.value)}`;
        }
        if (statusFilter && statusFilter.value) {
            const statusMap = {
                '可借阅': 'available',
                '已借出': 'borrowed',
                '已冻结': 'frozen'
            };
            url += `&status=${statusMap[statusFilter.value] || statusFilter.value}`;
        }
        
        const response = await fetch(url, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取图书数据失败');
        }
        
        const data = await response.json();
        books = data.books || [];
        totalPages = data.pagination ? data.pagination.pages : 1;
        currentPage = page;
        
        renderBooks();
        updatePagination();
        
    } catch (error) {
        console.error('加载图书数据失败:', error);
        showNotification('加载图书数据失败，请刷新页面重试', 'error');
        // 显示错误状态
        const tbody = document.getElementById('books-tbody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" style="text-align: center; padding: 40px; color: #ef4444;">
                        <div class="empty-state">
                            <h3>加载失败</h3>
                            <p>无法连接到服务器，请检查网络连接</p>
                            <button class="btn-primary" onclick="loadBooks()">重试</button>
                        </div>
                    </td>
                </tr>
            `;
        }
    }
}

// 渲染图书列表
function renderBooks() {
    const tbody = document.getElementById('books-tbody');
    if (!tbody) return;
    
    if (books.length === 0) {
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
    
    tbody.innerHTML = books.map(book => {
        // 处理封面图片
        const coverImage = book.cover_url || `/static/images/${book.title}.jpg`;
        let statusText, statusClass;
        
        if (book.status === 'available') {
            statusText = '可借阅';
            statusClass = 'status-available';
        } else if (book.status === 'borrowed') {
            statusText = '已借出';
            statusClass = 'status-borrowed';
        } else if (book.status === 'frozen') {
            statusText = '已冻结';
            statusClass = 'status-frozen';
        } else {
            statusText = '未知状态';
            statusClass = 'status-unknown';
        }
        
        // 计算库存信息
        const totalCopies = book.total_copies || 0;
        const availableCopies = book.available_copies || 0;
        const borrowedCopies = totalCopies - availableCopies;
        
        return `
            <tr>
                <td>${book.id}</td>
                <td>
                    <img src="${coverImage}" 
                         alt="${book.title}" 
                         style="width: 50px; height: 70px; object-fit: cover; border-radius: 4px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);"
                         onerror="this.src='data:image/svg+xml,%3Csvg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"280\" viewBox=\"0 0 200 280\"%3E%3Crect width=\"200\" height=\"280\" fill=\"%23667eea\"/%3E%3Cg transform=\"translate(50, 90)\"%3E%3Cpath d=\"M20 60V10c0-2.2 1.8-4 4-4h72c2.2 0 4 1.8 4 4v50\" fill=\"none\" stroke=\"white\" stroke-width=\"3\"/%3E%3Cpath d=\"M24 6h68v54h-68z\" fill=\"none\" stroke=\"white\" stroke-width=\"2\"/%3E%3C/g%3E%3Ctext x=\"100\" y=\"240\" font-family=\"Arial\" font-size=\"16\" fill=\"white\" text-anchor=\"middle\"%3E暂无封面%3C/text%3E%3C/svg%3E'">
                </td>
                <td><strong>${book.title}</strong></td>
                <td>${book.author}</td>
                <td>${book.category}</td>
                <td>${book.isbn}</td>
                <td>${book.pub_date ? new Date(book.pub_date).getFullYear() : '-'}</td>
                <td>
                    <span class="status-badge ${statusClass}">
                        ${statusText}
                    </span>
                </td>
                <td>
                    <div class="stock-info">
                        <div class="stock-item">
                            <span class="stock-label">总量:</span>
                            <span class="stock-value">${totalCopies}</span>
                        </div>
                        <div class="stock-item">
                            <span class="stock-label">可借:</span>
                            <span class="stock-value available">${availableCopies}</span>
                        </div>
                        <div class="stock-item">
                            <span class="stock-label">已借:</span>
                            <span class="stock-value borrowed">${borrowedCopies}</span>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="action-btns">
                        <button class="btn-edit" onclick="openEditModal(${book.id})">编辑</button>
                        <button class="btn-toggle" onclick="toggleBookStatus(${book.id})">
                            ${book.status === 'available' ? '冻结' : '解冻'}
                        </button>
                        <button class="btn-delete" onclick="deleteBook(${book.id})">删除</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// 更新分页
function updatePagination() {
    // 这里可以添加分页逻辑，如果需要的话
    // 目前管理员界面使用表格显示，可能不需要分页
}

// 搜索和筛选
function initSearch() {
    const searchInput = document.getElementById('search-input');
    const searchBtn = document.getElementById('search-btn');
    const categoryFilter = document.getElementById('category-filter');
    const statusFilter = document.getElementById('status-filter');
    
    function performSearch() {
        currentPage = 1;
        loadBooks();
    }
    
    if (searchBtn) {
        searchBtn.addEventListener('click', performSearch);
    }
    
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                performSearch();
            }
        });
    }
    
    if (categoryFilter) {
        categoryFilter.addEventListener('change', performSearch);
    }
    
    if (statusFilter) {
        statusFilter.addEventListener('change', performSearch);
    }
}

// 从API加载用户数据
async function loadUsers() {
    try {
        const response = await fetch(`${API_BASE}/users`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取用户数据失败');
        }
        
        const data = await response.json();
        users = data.users || [];
        renderUsers();
        
    } catch (error) {
        console.error('加载用户数据失败:', error);
        showNotification('加载用户数据失败，请刷新页面重试', 'error');
        // 显示错误状态
        const tbody = document.getElementById('users-tbody');
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="8" style="text-align: center; padding: 40px; color: #ef4444;">
                        <div class="empty-state">
                            <h3>加载失败</h3>
                            <p>无法连接到服务器，请检查网络连接</p>
                            <button class="btn-primary" onclick="loadUsers()">重试</button>
                        </div>
                    </td>
                </tr>
            `;
        }
    }
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
            <td>${user.real_name || user.username}</td>
            <td>${user.email || '-'}</td>
            <td>${user.created_at ? new Date(user.created_at).toLocaleDateString() : '-'}</td>
            <td>
                <span class="status-badge status-active">
                    正常
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

// 编辑图书
async function openEditModal(bookId) {
    try {
        const response = await fetch(`${API_BASE}/books/${bookId}`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取图书详情失败');
        }
        
        const data = await response.json();
        if (!data.ok) {
            throw new Error(data.error || '获取图书详情失败');
        }
        
        const book = data.book;
        
        // 填充表单
        document.getElementById('edit-book-id').value = book.id;
        document.getElementById('edit-book-title').value = book.title;
        document.getElementById('edit-book-author').value = book.author;
        document.getElementById('edit-book-category').value = book.category;
        document.getElementById('edit-book-isbn').value = book.isbn;
        document.getElementById('edit-book-publisher').value = book.publisher || '';
        document.getElementById('edit-book-date').value = book.pub_date ? book.pub_date.split('T')[0] : '';
        // 正确处理状态映射，包括frozen状态
        const statusMap = {
            'available': '可借阅',
            'borrowed': '已借出',
            'frozen': '已冻结'
        };
        document.getElementById('edit-book-status').value = statusMap[book.status] || '可借阅';
        document.getElementById('edit-book-cover').value = book.cover_url || '';
        
        document.getElementById('edit-modal').classList.add('active');
        
    } catch (error) {
        console.error('获取图书详情失败:', error);
        showNotification('获取图书详情失败，请重试', 'error');
    }
}

function closeEditModal() {
    document.getElementById('edit-modal').classList.remove('active');
}

// 初始化编辑表单
function initEditForm() {
    const form = document.getElementById('edit-book-form');
    if (!form) return;
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const bookId = parseInt(document.getElementById('edit-book-id').value);
        // 正确处理状态映射，包括frozen状态
        const statusValue = document.getElementById('edit-book-status').value;
        const statusMap = {
            '可借阅': 'available',
            '已借出': 'borrowed',
            '已冻结': 'frozen'
        };
        
        const bookData = {
            title: document.getElementById('edit-book-title').value.trim(),
            author: document.getElementById('edit-book-author').value.trim(),
            category: document.getElementById('edit-book-category').value,
            isbn: document.getElementById('edit-book-isbn').value.trim(),
            publisher: document.getElementById('edit-book-publisher').value.trim(),
            pub_date: document.getElementById('edit-book-date').value,
            status: statusMap[statusValue] || 'available',
            cover_url: document.getElementById('edit-book-cover').value.trim()
        };
        
        try {
            const response = await fetch(`${API_BASE}/books/${bookId}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify(bookData)
            });
            
            if (!response.ok) {
                throw new Error('更新图书失败');
            }
            
            const data = await response.json();
            if (!data.ok) {
                throw new Error(data.error || '更新图书失败');
            }
            
            showNotification('图书信息更新成功！', 'success');
            closeEditModal();
            loadBooks(); // 重新加载图书列表
            
        } catch (error) {
            console.error('更新图书失败:', error);
            showNotification(error.message || '更新图书失败，请重试', 'error');
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

// 切换图书状态（冻结/解冻）
async function toggleBookStatus(bookId) {
    const book = books.find(b => b.id === bookId);
    if (!book) return;
    
    const action = book.status === 'available' ? '冻结' : '解冻';
    
    if (!confirm(`确定要${action}《${book.title}》吗？`)) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/books/${bookId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({ 
                status: book.status === 'available' ? 'frozen' : 'available'
            })
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || `${action}图书失败`);
        }
        
        const data = await response.json();
        if (!data.ok) {
            throw new Error(data.error || `${action}图书失败`);
        }
        
        showNotification(`《${book.title}》已${action}`, 'success');
        loadBooks(); // 重新加载图书列表
        
    } catch (error) {
        console.error(`${action}图书失败:`, error);
        showNotification(error.message || `${action}图书失败，请重试`, 'error');
    }
}

// 删除图书
async function deleteBook(bookId) {
    const book = books.find(b => b.id === bookId);
    if (!book) return;
    
    if (!confirm(`确定要删除《${book.title}》吗？此操作无法撤销。`)) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/books/${bookId}`, {
            method: 'DELETE',
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('删除图书失败');
        }
        
        const data = await response.json();
        if (!data.ok) {
            throw new Error(data.error || '删除图书失败');
        }
        
        showNotification('图书删除成功', 'success');
        loadBooks(); // 重新加载图书列表
        
    } catch (error) {
        console.error('删除图书失败:', error);
        showNotification(error.message || '删除图书失败，请重试', 'error');
    }
}

// 编辑用户（占位函数）
function editUser(userId) {
    const user = users.find(u => u.id === userId);
    if (!user) return;
    showNotification(`编辑用户功能开发中：${user.username}`, 'info');
}

// 删除用户
async function deleteUser(userId) {
    const user = users.find(u => u.id === userId);
    if (!user) return;
    
    if (user.role === 'admin') {
        showNotification('不能删除管理员账户！', 'error');
        return;
    }
    
    if (!confirm(`确定要删除用户 ${user.username} 吗？`)) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/users/${userId}`, {
            method: 'DELETE',
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('删除用户失败');
        }
        
        const data = await response.json();
        if (!data.ok) {
            throw new Error(data.error || '删除用户失败');
        }
        
        showNotification('用户删除成功', 'success');
        loadUsers(); // 重新加载用户列表
        
    } catch (error) {
        console.error('删除用户失败:', error);
        showNotification(error.message || '删除用户失败，请重试', 'error');
    }
}

// 添加用户模态框（占位函数）
function showAddUserModal() {
    showNotification('添加用户功能开发中...', 'info');
}

// 登出处理函数
function handleLogout() {
    if (confirm('确定要退出登录吗？')) {
        showNotification('已退出登录', 'success');
        // 清除登录信息
        sessionStorage.removeItem('currentUser');
        setTimeout(() => {
            window.location.href = '/templates/login.html';
        }, 1000);
    }
}

// 借阅记录管理功能
let allBorrows = [];
let allBooksForFilter = [];

// 加载借阅记录
async function loadBorrows(bookId = null) {
    try {
        let url = `${API_BASE}/borrows`;
        if (bookId) {
            url += `?book_id=${bookId}`;
        }
        
        const response = await fetch(url, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取借阅记录失败');
        }
        
        const data = await response.json();
        // 后端返回的是records字段，不是borrows
        allBorrows = data.records || [];
        
        renderBorrows();
        updateBorrowStats();
        
    } catch (error) {
        console.error('加载借阅记录失败:', error);
        showNotification('加载借阅记录失败，请刷新页面重试', 'error');
    }
}

// 加载图书列表用于筛选
async function loadBooksForFilter() {
    try {
        const response = await fetch(`${API_BASE}/books`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取图书列表失败');
        }
        
        const data = await response.json();
        allBooksForFilter = data.books || [];
        
        // 更新筛选下拉框
        const bookFilter = document.getElementById('book-filter');
        if (bookFilter) {
            bookFilter.innerHTML = '<option value="">全部图书</option>';
            allBooksForFilter.forEach(book => {
                const option = document.createElement('option');
                option.value = book.id;
                option.textContent = `${book.title} - ${book.author}`;
                bookFilter.appendChild(option);
            });
        }
        
    } catch (error) {
        console.error('加载图书列表失败:', error);
    }
}

// 渲染借阅记录列表
function renderBorrows() {
    const borrowsList = document.getElementById('admin-borrows-list');
    const emptyState = document.getElementById('admin-empty-borrows');
    
    if (!borrowsList) return;
    
    if (allBorrows.length === 0) {
        borrowsList.style.display = 'none';
        if (emptyState) emptyState.style.display = 'block';
        return;
    }
    
    borrowsList.style.display = 'flex';
    if (emptyState) emptyState.style.display = 'none';
    
    borrowsList.innerHTML = allBorrows.map(borrow => {
        const borrowDate = new Date(borrow.borrow_date).toLocaleDateString();
        const dueDate = new Date(borrow.due_date).toLocaleDateString();
        const returnDate = borrow.return_date ? new Date(borrow.return_date).toLocaleDateString() : null;
        
        // 从嵌套对象中获取用户名和书名
        const username = borrow.user?.username || '未知用户';
        const bookTitle = borrow.book_copy?.book_info?.title || '未知图书';
        
        // 计算状态
        let statusClass, statusText;
        const today = new Date();
        const due = new Date(borrow.due_date);
        
        if (borrow.return_date) {
            statusClass = 'admin-status-returned';
            statusText = '已归还';
        } else if (today > due) {
            statusClass = 'admin-status-overdue';
            statusText = '已逾期';
        } else {
            statusClass = 'admin-status-active';
            statusText = '借阅中';
        }
        
        // 获取用户名首字母作为头像
        const userInitial = username ? username.charAt(0).toUpperCase() : 'U';
        
        return `
            <div class="admin-borrow-record">
                <div class="admin-borrow-book-info">
                    <div class="admin-borrow-book-title">${bookTitle}</div>
                    <div class="admin-borrow-user-info">
                        <div class="admin-borrow-user-avatar">${userInitial}</div>
                        <span>借阅人：${username}</span>
                    </div>
                    <div class="admin-borrow-details">
                        <div class="admin-borrow-detail-item">
                            <div class="admin-borrow-detail-label">借阅日期</div>
                            <div class="admin-borrow-detail-value">${borrowDate}</div>
                        </div>
                        <div class="admin-borrow-detail-item">
                            <div class="admin-borrow-detail-label">应还日期</div>
                            <div class="admin-borrow-detail-value ${today > due && !borrow.return_date ? 'overdue' : ''}">${dueDate}</div>
                        </div>
                        <div class="admin-borrow-detail-item">
                            <div class="admin-borrow-detail-label">归还日期</div>
                            <div class="admin-borrow-detail-value">${returnDate || '未归还'}</div>
                        </div>
                    </div>
                </div>
                <div class="admin-borrow-actions">
                    <div class="admin-borrow-status ${statusClass}">${statusText}</div>
                </div>
            </div>
        `;
    }).join('');
}

// 更新借阅统计信息
function updateBorrowStats() {
    const totalBorrows = document.getElementById('total-borrows');
    const activeBorrows = document.getElementById('active-borrows');
    const overdueBorrows = document.getElementById('overdue-borrows');
    const returnedBorrows = document.getElementById('returned-borrows');
    
    const today = new Date();
    let active = 0;
    let overdue = 0;
    let returned = 0;
    
    allBorrows.forEach(borrow => {
        if (borrow.return_date) {
            returned++;
        } else {
            active++;
            const due = new Date(borrow.due_date);
            if (today > due) {
                overdue++;
            }
        }
    });
    
    if (totalBorrows) totalBorrows.textContent = allBorrows.length;
    if (activeBorrows) activeBorrows.textContent = active;
    if (overdueBorrows) overdueBorrows.textContent = overdue;
    if (returnedBorrows) returnedBorrows.textContent = returned;
}

// 清除图书筛选
function clearBookFilter() {
    const bookFilter = document.getElementById('book-filter');
    if (bookFilter) {
        bookFilter.value = '';
        loadBorrows();
    }
}

// 初始化借阅记录功能
function initBorrowsTab() {
    const bookFilter = document.getElementById('book-filter');
    if (bookFilter) {
        bookFilter.addEventListener('change', (e) => {
            const bookId = e.target.value ? parseInt(e.target.value) : null;
            loadBorrows(bookId);
        });
    }
}

// 更新标签页切换逻辑
function initTabs() {
    const navBtns = document.querySelectorAll('.nav-btn');
    const tabContents = document.querySelectorAll('.tab-content');
    
    navBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');
            
            // 移除所有活动状态
            navBtns.forEach(b => b.classList.remove('active'));
            tabContents.forEach(tab => tab.classList.remove('active'));
            
            // 添加活动状态
            btn.classList.add('active');
            document.getElementById(`${targetTab}-tab`).classList.add('active');
            
            // 根据标签页加载数据
            if (targetTab === 'books') {
                loadBooks();
            } else if (targetTab === 'users') {
                loadUsers();
            } else if (targetTab === 'borrows') {
                loadBorrows();
                loadBooksForFilter();
            } else if (targetTab === 'settings') {
                loadSettings();
            }
        });
    });
}

// 加载系统设置
async function loadSettings() {
    try {
        const response = await fetch(`${API_BASE}/settings`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取系统设置失败');
        }
        
        const data = await response.json();
        if (!data.ok) {
            throw new Error(data.error || '获取系统设置失败');
        }
        
        const settings = data.settings || {};
        
        // 填充表单
        document.getElementById('default_borrow_days').value = settings.default_borrow_days || '30';
        document.getElementById('max_borrow_count').value = settings.max_borrow_count || '5';
        document.getElementById('max_renewal_times').value = settings.max_renewal_times || '2';
        document.getElementById('overdue_fine_per_day').value = settings.overdue_fine_per_day || '0.5';
        document.getElementById('max_fine_amount').value = settings.max_fine_amount || '50';
        document.getElementById('reminder_days_before_due').value = settings.reminder_days_before_due || '3';
        
    } catch (error) {
        console.error('加载系统设置失败:', error);
        showNotification('加载系统设置失败，请刷新页面重试', 'error');
    }
}

// 保存系统设置
async function saveSettings(settingsData) {
    try {
        const response = await fetch(`${API_BASE}/settings`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify(settingsData)
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.error || '保存系统设置失败');
        }
        
        const data = await response.json();
        if (!data.ok) {
            throw new Error(data.error || '保存系统设置失败');
        }
        
        showNotification('系统设置已保存', 'success');
        return true;
        
    } catch (error) {
        console.error('保存系统设置失败:', error);
        showNotification(error.message || '保存系统设置失败，请重试', 'error');
        return false;
    }
}

// 重置系统设置
function resetSettings() {
    if (!confirm('确定要重置为默认值吗？')) {
        return;
    }
    
    // 设置默认值
    document.getElementById('default_borrow_days').value = '30';
    document.getElementById('max_borrow_count').value = '5';
    document.getElementById('max_renewal_times').value = '2';
    document.getElementById('overdue_fine_per_day').value = '0.5';
    document.getElementById('max_fine_amount').value = '50';
    document.getElementById('reminder_days_before_due').value = '3';
    
    showNotification('已重置为默认值，请点击保存按钮保存设置', 'info');
}

// 初始化系统设置表单
function initSettingsForm() {
    const form = document.getElementById('settings-form');
    if (!form) return;
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const submitBtn = form.querySelector('button[type="submit"]');
        const originalText = submitBtn.innerHTML;
        
        // 禁用提交按钮
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg> 保存中...';
        
        const settingsData = {
            default_borrow_days: parseInt(document.getElementById('default_borrow_days').value),
            max_borrow_count: parseInt(document.getElementById('max_borrow_count').value),
            max_renewal_times: parseInt(document.getElementById('max_renewal_times').value),
            overdue_fine_per_day: parseFloat(document.getElementById('overdue_fine_per_day').value),
            max_fine_amount: parseFloat(document.getElementById('max_fine_amount').value),
            reminder_days_before_due: parseInt(document.getElementById('reminder_days_before_due').value)
        };
        
        const success = await saveSettings(settingsData);
        
        // 恢复提交按钮
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalText;
        
        if (success) {
            // 保存成功后可以执行其他操作
        }
    });
}

// 页面加载时初始化
document.addEventListener('DOMContentLoaded', () => {
    // 初始化功能
    initTabs();
    initSearch();
    initEditForm();
    initBorrowsTab();
    initSettingsForm();
    
    // 加载初始数据
    loadBooks();
    
    console.log('管理员界面已加载');
});
