// 图书数据
let allBooks = [];
let currentPage = 1;
let pageSize = 24;
let filteredBooks = [];
let selectedBookForBorrow = null;
let totalPages = 1;

// 默认封面图片
const defaultCoverImage = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="200" height="280" viewBox="0 0 200 280"%3E%3Crect width="200" height="280" fill="%23667eea"/%3E%3Cg transform="translate(50, 90)"%3E%3Cpath d="M20 60V10c0-2.2 1.8-4 4-4h72c2.2 0 4 1.8 4 4v50" fill="none" stroke="white" stroke-width="3"/%3E%3Cpath d="M24 6h68v54h-68z" fill="none" stroke="white" stroke-width="2"/%3E%3C/g%3E%3Ctext x="100" y="240" font-family="Arial" font-size="16" fill="white" text-anchor="middle"%3E暂无封面%3C/text%3E%3C/svg%3E';

// API基础URL（通过 Nginx 统一入口转发到后端）
const API_BASE = `${window.location.origin}/api`;

// 检查图片是否存在
function getBookCoverImage(path) {
    return new Promise((resolve) => {
        const img = new Image();
        img.onload = () => resolve(path);
        img.onerror = () => resolve(defaultCoverImage);
        img.src = path;
    });
}

// 初始化示例数据
function initBooksData() {
    const sampleBooks = [
        {
            id: 1,
            title: '银河系漫游指南',
            author: '道格拉斯·亚当斯',
            category: '科技',
            isbn: '978-7-5321-5007-4',
            publisher: '上海译文出版社',
            publishDate: '2018',
            language: '中文',
            stock: 5,
            status: '在库',
            description: '一部充满想象力的科幻小说，讲述了地球被毁灭前，主人公亚瑟·邓特在银河系中的冒险经历。',
            coverImage: 'images/银河系漫游指南.jpg',
            location: '二楼西区11排A面7列4层'
        },
        {
            id: 2,
            title: '三体',
            author: '刘慈欣',
            category: '科技',
            isbn: '978-7-5366-9293-0',
            publisher: '重庆出版社',
            publishDate: '2008',
            language: '中文',
            stock: 3,
            status: '在库',
            description: '中国科幻文学的里程碑之作，讲述了地球文明与三体文明的碰撞。',
            coverImage: 'images/三体.jpg',
            location: '二楼西区11排A面8列3层'
        },
        {
            id: 3,
            title: '活着',
            author: '余华',
            category: '文学',
            isbn: '978-7-5302-1007-4',
            publisher: '作家出版社',
            publishDate: '2012',
            language: '中文',
            stock: 7,
            status: '在库',
            description: '一个关于生命的故事，展现了中国农民在历史变革中的命运。',
            coverImage: 'images/活着.jpg',
            location: '三楼东区5排B面2列6层'
        },
        {
            id: 4,
            title: 'JavaScript高级程序设计',
            author: 'Nicholas C. Zakas',
            category: '科技',
            isbn: '978-7-115-27579-0',
            publisher: '人民邮电出版社',
            publishDate: '2012',
            language: '中文',
            stock: 0,
            status: '已借完',
            description: 'JavaScript经典教程，全面介绍JavaScript的核心概念和高级特性。',
            coverImage: 'images/JavaScript高级程序设计.jpg',
            location: '二楼西区12排A面5列2层'
        },
        {
            id: 5,
            title: '人类简史',
            author: '尤瓦尔·赫拉利',
            category: '历史',
            isbn: '978-7-5086-4943-9',
            publisher: '中信出版社',
            publishDate: '2014',
            language: '中文',
            stock: 4,
            status: '在库',
            description: '从动物到上帝，人类简史带你了解人类发展的全貌。',
            coverImage: 'images/人类简史.jpg',
            location: '三楼北区7排C面3列4层'
        },
        {
            id: 6,
            title: '百年孤独',
            author: '加西亚·马尔克斯',
            category: '文学',
            isbn: '978-7-5447-4840-5',
            publisher: '南海出版公司',
            publishDate: '2011',
            language: '中文',
            stock: 6,
            status: '在库',
            description: '魔幻现实主义的代表作，讲述了布恩迪亚家族七代人的传奇故事。',
            coverImage: 'images/百年孤独.jpg',
            location: '三楼东区6排B面4列5层'
        },
        {
            id: 7,
            title: '人工智能',
            author: '李开复',
            category: '科技',
            isbn: '978-7-5086-7423-3',
            publisher: '中信出版社',
            publishDate: '2017',
            language: '中文',
            stock: 8,
            status: '在库',
            description: '深度解析人工智能的发展现状和未来趋势。',
            coverImage: 'images/人工智能.jpg',
            location: '二楼西区13排A面6列3层'
        },
        {
            id: 8,
            title: '史记',
            author: '司马迁',
            category: '历史',
            isbn: '978-7-101-00305-3',
            publisher: '中华书局',
            publishDate: '2013',
            language: '中文',
            stock: 5,
            status: '在库',
            description: '中国历史上第一部纪传体通史，记载了上起黄帝下至汉武帝的历史。',
            coverImage: 'images/史记.jpg',
            location: '三楼北区8排C面1列7层'
        },
        {
            id: 9,
            title: '艺术的故事',
            author: '贡布里希',
            category: '艺术',
            isbn: '978-7-5495-2558-9',
            publisher: '广西美术出版社',
            publishDate: '2014',
            language: '中文',
            stock: 4,
            status: '在库',
            description: '一部经典的艺术史入门读物，从史前洞窟壁画到当代实验艺术。',
            coverImage: 'images/艺术的故事.jpg',
            location: '四楼南区3排D面9列2层'
        },
        {
            id: 10,
            title: '教育心理学',
            author: '陈琦',
            category: '教育',
            isbn: '978-7-04-019656-4',
            publisher: '高等教育出版社',
            publishDate: '2011',
            language: '中文',
            stock: 6,
            status: '在库',
            description: '系统介绍教育心理学的基本原理和应用。',
            coverImage: 'images/教育心理学.jpg',
            location: '四楼东区10排E面7列4层'
        },
        {
            id: 11,
            title: '解忧杂货店',
            author: '东野圭吾',
            category: '文学',
            isbn: '978-7-5399-5256-7',
            publisher: '南海出版公司',
            publishDate: '2014',
            language: '中文',
            stock: 9,
            status: '在库',
            description: '一家神奇的杂货店，连接过去与未来，为迷茫的人们指引方向。',
            coverImage: 'images/解忧杂货店.jpg',
            location: '三楼东区5排B面8列5层'
        },
        {
            id: 12,
            title: '深度学习',
            author: 'Ian Goodfellow',
            category: '科技',
            isbn: '978-7-115-46198-9',
            publisher: '人民邮电出版社',
            publishDate: '2017',
            language: '中文',
            stock: 2,
            status: '在库',
            description: '深度学习领域的经典教材，由该领域的三位专家共同撰写。',
            coverImage: 'images/深度学习.jpg',
            location: '二楼西区12排A面9列3层'
        },
        {
            id: 13,
            title: '梵高传',
            author: '欧文·斯通',
            category: '艺术',
            isbn: '978-7-5321-4522-3',
            publisher: '上海译文出版社',
            publishDate: '2015',
            language: '中文',
            stock: 5,
            status: '在库',
            description: '一部感人至深的艺术家传记，讲述了梵高充满激情和苦难的一生。',
            coverImage: 'images/梵高传.jpg',
            location: '四楼南区4排D面5列6层'
        },
        {
            id: 14,
            title: '明朝那些事儿',
            author: '当年明月',
            category: '历史',
            isbn: '978-7-5011-7291-4',
            publisher: '中国友谊出版公司',
            publishDate: '2006',
            language: '中文',
            stock: 10,
            status: '在库',
            description: '用通俗幽默的语言讲述明朝三百年历史。',
            coverImage: 'images/明朝那些事儿.jpg',
            location: '三楼北区9排C面4列5层'
        },
        {
            id: 15,
            title: '给教师的建议',
            author: '苏霍姆林斯基',
            category: '教育',
            isbn: '978-7-5339-2506-6',
            publisher: '长江文艺出版社',
            publishDate: '2014',
            language: '中文',
            stock: 7,
            status: '在库',
            description: '苏联著名教育家的经典著作，对教师工作提出100条建议。',
            coverImage: 'images/给教师的建议.jpg',
            location: '四楼东区11排E面3列6层'
        },
        {
            id: 16,
            title: '挪威的森林',
            author: '村上春树',
            category: '文学',
            isbn: '978-7-5327-4694-4',
            publisher: '上海译文出版社',
            publishDate: '2007',
            language: '中文',
            stock: 6,
            status: '在库',
            description: '村上春树的代表作，一部青春爱情小说。',
            coverImage: 'images/挪威的森林.jpg',
            location: '三楼东区7排B面1列7层'
        },
        {
            id: 17,
            title: '算法导论',
            author: 'Thomas H. Cormen',
            category: '科技',
            isbn: '978-7-111-40701-0',
            publisher: '机械工业出版社',
            publishDate: '2013',
            language: '中文',
            stock: 3,
            status: '在库',
            description: '计算机算法领域的经典教材，被誉为算法圣经。',
            coverImage: 'images/算法导论.jpg',
            location: '二楼西区14排A面2列4层'
        },
        {
            id: 18,
            title: '中国通史',
            author: '吕思勉',
            category: '历史',
            isbn: '978-7-5699-0142-9',
            publisher: '北京时代华文书局',
            publishDate: '2014',
            language: '中文',
            stock: 4,
            status: '在库',
            description: '中国历史学界公认的经典通史著作。',
            coverImage: 'images/中国通史.jpg',
            location: '三楼北区10排C面6列3层'
        },
        {
            id: 19,
            title: '西方美术史',
            author: '李春',
            category: '艺术',
            isbn: '978-7-300-11562-4',
            publisher: '中国人民大学出版社',
            publishDate: '2010',
            language: '中文',
            stock: 5,
            status: '在库',
            description: '系统介绍西方美术发展的历史脉络。',
            coverImage: 'images/西方美术史.jpg',
            location: '四楼南区5排D面7列5层'
        },
        {
            id: 20,
            title: '教育学原理',
            author: '柳海民',
            category: '教育',
            isbn: '978-7-04-023766-2',
            publisher: '高等教育出版社',
            publishDate: '2011',
            language: '中文',
            stock: 5,
            status: '在库',
            description: '高等院校教育学专业的基础教材。',
            coverImage: 'images/教育学原理.jpg',
            location: '四楼东区12排E面8列2层'
        },
        {
            id: 21,
            title: '1984',
            author: '乔治·奥威尔',
            category: '文学',
            isbn: '978-7-5327-5153-5',
            publisher: '上海译文出版社',
            publishDate: '2010',
            language: '中文',
            stock: 8,
            status: '在库',
            description: '反乌托邦文学的经典之作，描绘了一个极权主义社会的恐怖景象。',
            coverImage: 'images/1984.jpg',
            location: '三楼东区8排B面3列6层'
        },
        {
            id: 22,
            title: 'Python编程：从入门到实践',
            author: 'Eric Matthes',
            category: '科技',
            isbn: '978-7-115-42802-8',
            publisher: '人民邮电出版社',
            publishDate: '2016',
            language: '中文',
            stock: 10,
            status: '在库',
            description: 'Python编程的入门经典，适合零基础读者。',
            coverImage: 'images/Python编程：从入门到实践.jpg',
            location: '二楼西区15排A面4列5层'
        },
        {
            id: 23,
            title: '艺术哲学',
            author: '丹纳',
            category: '艺术',
            isbn: '978-7-5321-3659-7',
            publisher: '上海译文出版社',
            publishDate: '2013',
            language: '中文',
            stock: 4,
            status: '在库',
            description: '从哲学角度分析艺术的本质和发展规律。',
            coverImage: 'images/艺术哲学.jpg',
            location: '四楼南区6排D面1列4层'
        },
        {
            id: 24,
            title: '论语',
            author: '孔子',
            category: '其他',
            isbn: '978-7-101-00307-7',
            publisher: '中华书局',
            publishDate: '2012',
            language: '中文',
            stock: 12,
            status: '在库',
            description: '儒家经典著作，记录了孔子及其弟子的言行。',
            coverImage: 'images/论语.jpg',
            location: '三楼北区1排C面5列7层'
        }
    ];
    
    allBooks = sampleBooks;
    filteredBooks = [...allBooks];
    saveToLocalStorage();
}

// 本地存储操作
function saveToLocalStorage() {
    localStorage.setItem('libraryBooksData', JSON.stringify(allBooks));
}

function loadFromLocalStorage() {
    const savedData = localStorage.getItem('libraryBooksData');
    if (savedData) {
        allBooks = JSON.parse(savedData);
        filteredBooks = [...allBooks];
    } else {
        initBooksData();
    }
}

// 显示通知
function showNotification(message, type = 'success') {
    const notification = document.getElementById('notification');
    // 将 \n 转换为 <br> 以支持换行显示
    const formattedMessage = message.replace(/\n/g, '<br>');
    notification.innerHTML = formattedMessage;
    notification.className = `notification ${type} show`;
    
    setTimeout(() => {
        notification.classList.remove('show');
    }, 3000);
}

// 生成图书封面颜色
function getBookCoverColor(index) {
    const colors = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
        'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
        'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
        'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
        'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)'
    ];
    return colors[index % colors.length];
}

// 渲染图书卡片
async function renderBooks() {
    const grid = document.getElementById('books-grid');
    if (!grid) return;
    
    try {
        // 获取搜索和过滤参数
        const searchInput = document.getElementById('search-input');
        const categoryFilter = document.getElementById('category-filter');
        const languageFilter = document.getElementById('language-filter');
        const yearFilter = document.getElementById('year-filter');
        
        const search = searchInput ? searchInput.value.trim() : '';
        const category = categoryFilter ? categoryFilter.value : '';
        const language = languageFilter ? languageFilter.value : '';
        const year = yearFilter ? yearFilter.value : '';
        
        // 构建API URL
        let url = `${API_BASE}/books?page=${currentPage}&per_page=${pageSize}`;
        if (search) url += `&search=${encodeURIComponent(search)}`;
        if (category) url += `&category=${encodeURIComponent(category)}`;
        if (language) url += `&language=${encodeURIComponent(language)}`;
        if (year && year !== 'older') url += `&year=${encodeURIComponent(year)}`;
        
        const response = await fetch(url, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取图书数据失败');
        }
        
        const data = await response.json();
        const booksToShow = data.books || [];
        
        // 更新总页数
        totalPages = data.pagination ? data.pagination.pages : Math.ceil(booksToShow.length / pageSize);
        
        if (booksToShow.length === 0) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 60px 20px; color: #64748b;"><h3>未找到匹配的图书</h3><p>请尝试调整搜索条件</p></div>';
            updatePagination();
            updateResultCount(data.pagination ? data.pagination.total : 0);
            return;
        }
        
        // 为每本书设置封面信息，使用真实的库存数据
        const booksWithValidCovers = booksToShow.map(book => {
            // 为后端数据添加前端需要的字段
            book.coverImage = book.cover_url || `/static/images/${book.title}.jpg`;
            book.publishDate = book.pub_date ? new Date(book.pub_date).getFullYear().toString() : '';
            book.language = book.language || '中文';
            // 使用后端返回的真实库存数据，而不是随机生成
            book.stock = book.available_copies || book.stock || 0;
            book.location = book.location || `${Math.floor(Math.random() * 4) + 1}楼${['东', '西', '南', '北'][Math.floor(Math.random() * 4)]}区${Math.floor(Math.random() * 15) + 1}排${['A', 'B', 'C', 'D'][Math.floor(Math.random() * 4)]}面${Math.floor(Math.random() * 10) + 1}列${Math.floor(Math.random() * 7) + 1}层`;
            book.description = book.description || `${book.title}是一本优秀的${book.category}类图书，由${book.author}所著，内容丰富，值得一读。`;
            
            return book;
        });
    
        grid.innerHTML = booksWithValidCovers.map((book, index) => {
            const stockStatus = book.stock === 0 ? 'out' : book.stock <= 3 ? 'low' : 'available';
            const stockText = book.stock === 0 ? '已借完' : book.stock <= 3 ? '库存紧张' : '在库';
            
            return `
                <div class="book-card" data-book-id="${book.id}">
                    <div class="book-cover" style="background-color: #f0f2f5; background-image: url('${book.coverImage}'); background-size: cover; background-position: center; background-repeat: no-repeat;" onerror="this.style.backgroundImage='url(${defaultCoverImage})'">
                        <span class="stock-badge stock-${stockStatus}">${stockText}</span>
                    </div>
                    <div class="book-info">
                        <div class="book-title">${book.title}</div>
                        <div class="book-author">${book.author}</div>
                        <div class="book-publisher">${book.publisher} · ${book.publishDate}</div>
                        <div class="book-tags">
                            <span class="tag tag-category">${book.category}</span>
                            <span class="tag tag-status">${book.status === 'available' ? '可借阅' : book.status === 'frozen' ? '已冻结' : '已借出'}</span>
                            <span class="tag tag-language">${book.language}</span>
                        </div>
                        <div class="book-stock">
                            库存：<span class="stock-number">${book.stock}</span>
                        </div>
                        <div class="book-actions">
                            <button class="btn-action btn-view" onclick="viewBookDetail(${book.id})">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                    <circle cx="12" cy="12" r="3"></circle>
                                </svg>
                                详情
                            </button>
                            <button class="btn-action btn-borrow" onclick="borrowBook(${book.id})" ${book.status === 'borrowed' || book.status === 'frozen' ? 'disabled' : ''}>
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                                </svg>
                                借阅
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    
        updatePagination();
        updateResultCount(data.pagination ? data.pagination.total : booksToShow.length);
    } catch (error) {
        console.error('获取图书数据失败:', error);
        const grid = document.getElementById('books-grid');
        if (grid) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 60px 20px; color: #ef4444;"><h3>加载失败</h3><p>无法连接到服务器，请检查网络连接</p></div>';
        }
    }
}

// 更新分页器
function updatePagination() {
    const paginationNumbers = document.getElementById('pagination-numbers');
    const prevBtn = document.getElementById('prev-page');
    const nextBtn = document.getElementById('next-page');
    
    prevBtn.disabled = currentPage === 1;
    nextBtn.disabled = currentPage === totalPages || totalPages === 0;
    
    // 生成页码
    let pageNumbers = [];
    if (totalPages <= 7) {
        pageNumbers = Array.from({length: totalPages}, (_, i) => i + 1);
    } else {
        if (currentPage <= 4) {
            pageNumbers = [1, 2, 3, 4, 5, '...', totalPages];
        } else if (currentPage >= totalPages - 3) {
            pageNumbers = [1, '...', totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
        } else {
            pageNumbers = [1, '...', currentPage - 1, currentPage, currentPage + 1, '...', totalPages];
        }
    }
    
    paginationNumbers.innerHTML = pageNumbers.map(num => {
        if (num === '...') {
            return '<span class="page-number" style="border: none; cursor: default;">...</span>';
        }
        return `<button class="page-number ${num === currentPage ? 'active' : ''}" onclick="goToPage(${num})">${num}</button>`;
    }).join('');
}

// 更新结果计数
function updateResultCount(total = null) {
    const countElement = document.getElementById('total-count');
    if (countElement) {
        countElement.textContent = (total !== null ? total : filteredBooks.length).toLocaleString();
    }
}

// 跳转到指定页
function goToPage(page) {
    currentPage = page;
    renderBooks();
    window.scrollTo({top: 0, behavior: 'smooth'});
}

// 上一页
document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('prev-page').addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage--;
            renderBooks();
            window.scrollTo({top: 0, behavior: 'smooth'});
        }
    });
    
    document.getElementById('next-page').addEventListener('click', () => {
        const totalPages = Math.ceil(filteredBooks.length / pageSize);
        if (currentPage < totalPages) {
            currentPage++;
            renderBooks();
            window.scrollTo({top: 0, behavior: 'smooth'});
        }
    });
});

// 搜索和筛选
function initFilters() {
    const searchInput = document.getElementById('search-input');
    const categoryFilter = document.getElementById('category-filter');
    const languageFilter = document.getElementById('language-filter');
    const yearFilter = document.getElementById('year-filter');
    const pageSizeSelect = document.getElementById('page-size');
    
    function applyFilters() {
        currentPage = 1;
        renderBooks();
    }
    
    searchInput.addEventListener('input', applyFilters);
    categoryFilter.addEventListener('change', applyFilters);
    languageFilter.addEventListener('change', applyFilters);
    yearFilter.addEventListener('change', applyFilters);
    
    pageSizeSelect.addEventListener('change', (e) => {
        pageSize = parseInt(e.target.value);
        currentPage = 1;
        renderBooks();
    });
}

// 查看图书详情
async function viewBookDetail(bookId) {
    try {
        const response = await fetch(`${API_BASE}/books/${bookId}`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取图书详情失败');
        }
        
        const responseData = await response.json();
        
        if (!responseData.ok) {
            throw new Error(responseData.error || '获取图书详情失败');
        }
        
        const book = responseData.book;
        
        // 为图书详情设置封面信息，使用真实的库存数据
        book.coverImage = book.cover_url || `/static/images/${book.title}.jpg`;
        book.publishDate = book.pub_date ? new Date(book.pub_date).getFullYear().toString() : '';
        book.language = book.language || '中文';
        // 使用后端返回的真实库存数据，而不是随机生成
        book.stock = book.available_copies || book.stock || 0;
        book.location = book.location || `${Math.floor(Math.random() * 4) + 1}楼${['东', '西', '南', '北'][Math.floor(Math.random() * 4)]}区${Math.floor(Math.random() * 15) + 1}排${['A', 'B', 'C', 'D'][Math.floor(Math.random() * 4)]}面${Math.floor(Math.random() * 10) + 1}列${Math.floor(Math.random() * 7) + 1}层`;
        book.description = book.description || `${book.title}是一本优秀的${book.category}类图书，由${book.author}所著，内容丰富，值得一读。`;
        
        const detailContent = document.getElementById('book-detail-content');
        detailContent.innerHTML = `
            <div class="detail-content">
                <div>
                    <div class="book-cover detail-cover" style="background-image: url('${book.coverImage || defaultCoverImage}'); background-size: cover; background-position: center; height: 360px;">
                    </div>
                </div>
                <div class="detail-info">
                    <h2>${book.title}</h2>
                    <div class="detail-row">
                        <span class="detail-label">作者：</span>
                        <span class="detail-value">${book.author}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">分类：</span>
                        <span class="detail-value">${book.category}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">ISBN：</span>
                        <span class="detail-value">${book.isbn}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">出版社：</span>
                        <span class="detail-value">${book.publisher || '未知'}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">出版日期：</span>
                        <span class="detail-value">${book.publishDate || '未知'}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">语言：</span>
                        <span class="detail-value">${book.language}</span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">状态：</span>
                        <span class="detail-value" style="font-weight: 700; color: ${book.status === 'available' ? '#22c55e' : book.status === 'frozen' ? '#6366f1' : '#ef4444'}">
                            ${book.status === 'available' ? '可借阅' : book.status === 'frozen' ? '已冻结' : '已借出'}
                        </span>
                    </div>
                    <div class="detail-row">
                        <span class="detail-label">图书位置：</span>
                        <span class="detail-value" style="font-weight: 600; color: #667eea;">
                            📍 ${book.location || '暂无位置信息'}
                        </span>
                    </div>
                    <div class="detail-description">
                        <h3>内容简介</h3>
                        <p>${book.description}</p>
                    </div>
                    <div style="margin-top: 24px;">
                        <button class="btn-primary" onclick="closeDetailModal(); borrowBook(${book.id});" ${book.status === 'borrowed' || book.status === 'frozen' ? 'disabled' : ''}>
                            立即借阅
                        </button>
                    </div>
                </div>
            </div>
        `;
        
        document.getElementById('detail-modal').classList.add('active');
        
    } catch (error) {
        console.error('获取图书详情失败:', error);
        showNotification('获取图书详情失败，请重试', 'error');
    }
}

function closeDetailModal() {
    document.getElementById('detail-modal').classList.remove('active');
}

// 借阅图书
async function borrowBook(bookId) {
    try {
        // 验证图书ID
        if (!bookId || isNaN(bookId)) {
            showNotification('无效的图书ID', 'error');
            return;
        }

        // 检查网络连接
        if (!navigator.onLine) {
            showNotification('网络连接异常，请检查网络后重试', 'error');
            return;
        }

        // 显示加载状态
        showNotification('正在获取图书信息...', 'info');
        
        const response = await fetch(`${API_BASE}/books/${bookId}`, {
            credentials: 'include',
            timeout: 10000 // 10秒超时
        });
        
        // 处理HTTP错误状态
        if (response.status === 404) {
            showNotification('图书不存在或已被删除', 'error');
            return;
        } else if (response.status === 401) {
            showNotification('登录已过期，请重新登录', 'error');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2000);
            return;
        } else if (response.status === 403) {
            showNotification('权限不足，无法借阅此图书', 'error');
            return;
        } else if (response.status >= 500) {
            showNotification('服务器内部错误，请稍后重试', 'error');
            return;
        } else if (!response.ok) {
            showNotification(`请求失败 (${response.status})，请重试`, 'error');
            return;
        }
        
        let responseData;
        try {
            responseData = await response.json();
        } catch (parseError) {
            console.error('解析响应数据失败:', parseError);
            showNotification('服务器响应格式错误，请重试', 'error');
            return;
        }
        
        if (!responseData.ok) {
            const errorMessage = responseData.error || responseData.message || '获取图书信息失败';
            showNotification(errorMessage, 'error');
            return;
        }
        
        const book = responseData.book;
        
        // 验证图书数据
        if (!book) {
            showNotification('图书数据异常，请重试', 'error');
            return;
        }
        
        // 检查图书状态
        if (book.status === 'borrowed') {
            showNotification('该图书已被借出，无法借阅', 'error');
            return;
        }
        
        // 检查图书是否被冻结
        if (book.status === 'frozen') {
            showNotification('该图书已被冻结，无法借阅', 'error');
            return;
        }
        
        if (book.stock <= 0) {
            showNotification('该图书库存不足，无法借阅', 'error');
            return;
        }
        
        // 为借阅图书设置封面信息，使用真实的库存数据
        book.coverImage = book.cover_url || `/static/images/${book.title}.jpg`;
        book.publishDate = book.pub_date ? new Date(book.pub_date).getFullYear().toString() : '';
        book.language = book.language || '中文';
        // 使用后端返回的真实库存数据，而不是随机生成
        book.stock = book.available_copies || book.stock || 0;
        book.location = book.location || `${Math.floor(Math.random() * 4) + 1}楼${['东', '西', '南', '北'][Math.floor(Math.random() * 4)]}区${Math.floor(Math.random() * 15) + 1}排${['A', 'B', 'C', 'D'][Math.floor(Math.random() * 4)]}面${Math.floor(Math.random() * 10) + 1}列${Math.floor(Math.random() * 7) + 1}层`;
        book.description = book.description || `${book.title}是一本优秀的${book.category}类图书，由${book.author}所著，内容丰富，值得一读。`;
        
        selectedBookForBorrow = book;
        
        const bookInfoDisplay = document.getElementById('selected-book-info');
        if (bookInfoDisplay) {
            bookInfoDisplay.innerHTML = `
                <div class="selected-book">
                    <div class="book-cover selected-book-cover" style="background-image: url('${book.coverImage || defaultCoverImage}'); background-size: cover; background-position: center;">
                    </div>
                    <div class="selected-book-details">
                        <h4>${book.title}</h4>
                        <p>作者：${book.author}</p>
                        <p>ISBN：${book.isbn}</p>
                        <p>可借数量：${book.stock} 本</p>
                    </div>
                </div>
            `;
        } else {
            console.error('找不到图书信息显示元素');
            showNotification('页面元素异常，请刷新页面重试', 'error');
            return;
        }
        
        // 设置默认日期 - 从系统设置获取默认借阅天数
        const today = new Date();
        let defaultBorrowDays = 30; // 默认值
        
        // 从系统设置获取默认借阅天数
        try {
            const settingsResponse = await fetch(`${API_BASE}/settings`, {
                credentials: 'include'
            });
            if (settingsResponse.ok) {
                const settingsData = await settingsResponse.json();
                if (settingsData.ok && settingsData.settings) {
                    defaultBorrowDays = parseInt(settingsData.settings.default_borrow_days) || 30;
                }
            }
        } catch (error) {
            console.warn('获取系统设置失败，使用默认值:', error);
        }
        
        const returnDate = new Date(today.getTime() + defaultBorrowDays * 24 * 60 * 60 * 1000);
        
        const borrowDateInput = document.getElementById('borrow-date');
        const returnDateInput = document.getElementById('return-date');
        
        if (borrowDateInput && returnDateInput) {
            borrowDateInput.valueAsDate = today;
            returnDateInput.valueAsDate = returnDate;
        } else {
            console.error('找不到日期输入元素');
            showNotification('页面元素异常，请刷新页面重试', 'error');
            return;
        }
        
        openBorrowModal();
        
    } catch (error) {
        console.error('获取图书信息失败:', error);
        
        // 网络错误处理
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            showNotification('网络连接失败，请检查网络后重试', 'error');
        } else if (error.name === 'AbortError') {
            showNotification('请求超时，请重试', 'error');
        } else {
            showNotification('获取图书信息失败，请重试', 'error');
        }
    }
}

function openBorrowModal() {
    // 先显示模态框
    const modal = document.getElementById('borrow-modal');
    if (modal) {
        modal.classList.add('active');
    }
    
    // 等待模态框显示后再填充表单（确保DOM元素已加载）
    setTimeout(() => {
        // 获取当前登录用户信息
        const currentUser = sessionStorage.getItem('currentUser');
        if (currentUser) {
            try {
                const user = JSON.parse(currentUser);
                // 自动填充当前用户的ID和姓名
                const readerIdInput = document.getElementById('reader-id');
                const readerNameInput = document.getElementById('reader-name');
                const noteInput = document.getElementById('borrow-note');
                
                if (readerIdInput && user.user_id) {
                    readerIdInput.value = user.user_id;
                }
                if (readerNameInput && user.name) {
                    readerNameInput.value = user.name;
                }
                // 自动填充备注"我爱软件工程"
                if (noteInput) {
                    noteInput.value = '我爱软件工程';
                }
            } catch (e) {
                console.error('解析用户信息失败:', e);
            }
        }
    }, 100); // 延迟100ms确保模态框已显示
}

function closeBorrowModal() {
    document.getElementById('borrow-modal').classList.remove('active');
    document.getElementById('borrow-form').reset();
    selectedBookForBorrow = null;
}

// 处理借阅表单提交
function initBorrowForm() {
    const form = document.getElementById('borrow-form');
    
    if (!form) {
        console.error('找不到借阅表单元素');
        return;
    }
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        try {
            // 验证选中的图书
            if (!selectedBookForBorrow) {
                showNotification('请先选择要借阅的图书', 'error');
                return;
            }
            
            // 获取表单元素
            const readerIdInput = document.getElementById('reader-id');
            const readerNameInput = document.getElementById('reader-name');
            const borrowDateInput = document.getElementById('borrow-date');
            const returnDateInput = document.getElementById('return-date');
            const noteInput = document.getElementById('borrow-note');
            
            // 验证表单元素存在
            if (!readerIdInput || !readerNameInput || !borrowDateInput || !returnDateInput) {
                showNotification('表单元素异常，请刷新页面重试', 'error');
                return;
            }
            
            // 获取表单数据
            const readerId = readerIdInput.value.trim();
            const readerName = readerNameInput.value.trim();
            const borrowDate = borrowDateInput.value;
            const returnDate = returnDateInput.value;
            // 自动添加备注"我爱软件工程"，如果用户有输入其他备注则合并
            const userNote = noteInput ? noteInput.value.trim() : '';
            const note = userNote ? `${userNote} - 我爱软件工程` : '我爱软件工程';
            
            // 表单验证
            const validationResult = validateBorrowForm({
                readerId,
                readerName,
                borrowDate,
                returnDate,
                note,
                bookStock: selectedBookForBorrow.stock
            });
            
            if (!validationResult.isValid) {
                showNotification(validationResult.message, 'error');
                return;
            }
            
            // 检查网络连接
            if (!navigator.onLine) {
                showNotification('网络连接异常，请检查网络后重试', 'error');
                return;
            }
            
            // 禁用提交按钮，防止重复提交
            const submitBtn = form.querySelector('button[type="submit"]');
            const originalText = submitBtn.textContent;
            submitBtn.disabled = true;
            submitBtn.textContent = '借阅中...';
            
            showNotification('正在处理借阅请求...', 'info');
            
            // 调用后端借阅API
            const response = await fetch(`${API_BASE}/borrow`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify({
                    book_id: selectedBookForBorrow.id,
                    reader_id: readerId,
                    reader_name: readerName,
                    borrow_date: borrowDate,
                    due_date: returnDate,
                    note: note
                })
            });
            
            // 处理HTTP错误状态
            if (response.status === 400) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || '请求参数错误');
            } else if (response.status === 401) {
                showNotification('登录已过期，请重新登录', 'error');
                setTimeout(() => {
                    window.location.href = 'login.html';
                }, 2000);
                return;
            } else if (response.status === 403) {
                throw new Error('权限不足，无法借阅此图书');
            } else if (response.status === 409) {
                throw new Error('该图书已被借出或库存不足');
            } else if (response.status >= 500) {
                throw new Error('服务器内部错误，请稍后重试');
            } else if (!response.ok) {
                throw new Error(`借阅请求失败 (${response.status})`);
            }
            
            let result;
            try {
                result = await response.json();
            } catch (parseError) {
                console.error('解析借阅响应失败:', parseError);
                throw new Error('服务器响应格式错误');
            }
            
            if (!result.ok) {
                throw new Error(result.message || result.error || '借阅失败');
            }
            
            // 借阅成功
            showNotification(`《${selectedBookForBorrow.title}》借阅成功！`, 'success');
            closeBorrowModal();
            
            // 重新加载图书列表以更新库存
            await renderBooks();
            
        } catch (error) {
            console.error('借阅失败:', error);
            
            // 网络错误处理
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                showNotification('网络连接失败，请检查网络后重试', 'error');
            } else if (error.name === 'AbortError') {
                showNotification('请求超时，请重试', 'error');
            } else {
                showNotification(error.message || '借阅失败，请重试', 'error');
            }
        } finally {
            // 恢复提交按钮状态
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.disabled = false;
                submitBtn.textContent = '确认借阅';
            }
        }
    });
}

// 借阅表单验证函数
function validateBorrowForm(data) {
    const { readerId, readerName, borrowDate, returnDate, note, bookStock } = data;
    
    // 验证读者ID
    if (!readerId) {
        return { isValid: false, message: '请输入读者ID' };
    }
    
    if (readerId.length < 3 || readerId.length > 50) {
        return { isValid: false, message: '读者ID长度应在3-50个字符之间' };
    }
    
    // 验证读者姓名
    if (!readerName) {
        return { isValid: false, message: '请输入读者姓名' };
    }
    
    // 验证读者姓名格式（所有条件合并，满足一个就返回错误）
    if (readerName.length < 2 || 
        readerName.length > 50 || 
        readerName.trim().length === 0 || 
        readerName !== readerName.trim() || 
        /\d/.test(readerName) || 
        /[^\u4e00-\u9fa5a-zA-Z\s-]/.test(readerName) || 
        /\s{2,}/.test(readerName) || 
        /^\s+$/.test(readerName) || 
        !/[\u4e00-\u9fa5a-zA-Z]/.test(readerName) || 
        !/^[\u4e00-\u9fa5a-zA-Z\s]+$/.test(readerName)) {
        return { isValid: false, message: '读者姓名格式错误!\n要求:只能包含中文或英文字母\n不允许:数字、特殊符号、空格\n示例:张三或 ZhangSan\n请重新输入正确的姓名。' };
    }
    
    // 验证借阅日期
    if (!borrowDate) {
        return { isValid: false, message: '请选择借阅日期' };
    }
    
    const borrowDateObj = new Date(borrowDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    if (borrowDateObj < today) {
        return { isValid: false, message: '借阅日期不能早于今天' };
    }
    
    // 验证应还日期
    if (!returnDate) {
        return { isValid: false, message: '请选择应还日期' };
    }
    
    const returnDateObj = new Date(returnDate);
    
    if (returnDateObj <= borrowDateObj) {
        return { isValid: false, message: '应还日期必须晚于借阅日期' };
    }
    
    // 验证借阅时长（最多365天）
    const maxBorrowDays = 365;
    const daysDiff = Math.ceil((returnDateObj - borrowDateObj) / (1000 * 60 * 60 * 24));
    
    if (daysDiff > maxBorrowDays) {
        return { isValid: false, message: `借阅时长不能超过${maxBorrowDays}天` };
    }
    
    // 验证备注（可选）
    if (note && note.length > 200) {
        return { isValid: false, message: '备注长度不能超过200个字符' };
    }
    
    // 验证库存
    if (bookStock <= 0) {
        return { isValid: false, message: '该图书库存不足，无法借阅' };
    }
    
    return { isValid: true };
}

// 标签页切换
function initPageTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const pages = document.querySelectorAll('.page-content');
    
    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetPage = btn.getAttribute('data-page');
            
            // 移除所有活动状态
            tabBtns.forEach(b => b.classList.remove('active'));
            pages.forEach(page => page.classList.remove('active'));
            
            // 添加活动状态
            btn.classList.add('active');
            const targetPageElement = document.getElementById(`${targetPage}-page`);
            if (targetPageElement) {
                targetPageElement.classList.add('active');
            } else {
                console.warn(`页面元素 ${targetPage}-page 不存在`);
            }
            
            // 根据页面加载数据
            if (targetPage === 'popular') {
                renderPopularBooks();
            } else if (targetPage === 'new') {
                renderNewBooks();
            } else if (targetPage === 'my-borrows') {
                renderMyBorrows();
            }
        });
    });
}

// 渲染热门图书
async function renderPopularBooks() {
    const grid = document.getElementById('popular-books-grid');
    if (!grid) return;
    
    try {
        const response = await fetch(`${API_BASE}/books?popular=true&per_page=8`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取热门图书失败');
        }
        
        const data = await response.json();
        const popularBooks = data.books || [];
        
        if (popularBooks.length === 0) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px 20px; color: #64748b;"><p>暂无热门图书</p></div>';
            return;
        }
        
        grid.innerHTML = popularBooks.map((book, index) => {
            // 为热门图书设置封面信息
            book.coverImage = book.cover_url || `/static/images/${book.title}.jpg`;
            book.publishDate = book.pub_date ? new Date(book.pub_date).getFullYear().toString() : '';
            book.language = book.language || '中文';
            book.stock = book.available_copies || book.stock || 0;
            book.location = book.location || `${Math.floor(Math.random() * 4) + 1}楼${['东', '西', '南', '北'][Math.floor(Math.random() * 4)]}区${Math.floor(Math.random() * 15) + 1}排${['A', 'B', 'C', 'D'][Math.floor(Math.random() * 4)]}面${Math.floor(Math.random() * 10) + 1}列${Math.floor(Math.random() * 7) + 1}层`;
            book.description = book.description || `${book.title}是一本优秀的${book.category}类图书，由${book.author}所著，内容丰富，值得一读。`;
            
            const stockStatus = book.stock === 0 ? 'out' : book.stock <= 3 ? 'low' : 'available';
            const stockText = book.stock === 0 ? '已借完' : book.stock <= 3 ? '库存紧张' : '在库';
            
            return `
                <div class="book-card" data-book-id="${book.id}">
                    <div class="book-cover" style="background-image: url('${book.coverImage || defaultCoverImage}'); background-size: cover; background-position: center;" onerror="this.style.backgroundImage='url(${defaultCoverImage})'">
                        <span class="stock-badge stock-${stockStatus}">${stockText}</span>
                    </div>
                    <div class="book-info">
                        <div class="book-title">${book.title}</div>
                        <div class="book-author">${book.author}</div>
                        <div class="book-publisher">${book.publisher} · ${book.publishDate}</div>
                        <div class="book-tags">
                            <span class="tag tag-category">${book.category}</span>
                            <span class="tag tag-status">${book.status}</span>
                            <span class="tag tag-language">${book.language}</span>
                        </div>
                        <div class="book-stock">
                            库存：<span class="stock-number">${book.stock}</span>
                        </div>
                        <div class="book-actions">
                            <button class="btn-action btn-view" onclick="viewBookDetail(${book.id})">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                    <circle cx="12" cy="12" r="3"></circle>
                                </svg>
                                详情
                            </button>
                            <button class="btn-action btn-borrow" onclick="borrowBook(${book.id})" ${book.stock === 0 ? 'disabled' : ''}>
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                                </svg>
                                借阅
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
    } catch (error) {
        console.error('获取热门图书失败:', error);
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px 20px; color: #ef4444;"><p>加载失败，请刷新重试</p></div>';
    }
}

// 渲染新书推荐
async function renderNewBooks() {
    const grid = document.getElementById('new-books-grid');
    if (!grid) return;
    
    try {
        const response = await fetch(`${API_BASE}/books?new=true&per_page=8`, {
            credentials: 'include'
        });
        
        if (!response.ok) {
            throw new Error('获取新书推荐失败');
        }
        
        const data = await response.json();
        const newBooks = data.books || [];
        
        if (newBooks.length === 0) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px 20px; color: #64748b;"><p>暂无新书推荐</p></div>';
            return;
        }
        
        grid.innerHTML = newBooks.map((book, index) => {
            // 为新书设置封面信息
            book.coverImage = book.cover_url || `/static/images/${book.title}.jpg`;
            book.publishDate = book.pub_date ? new Date(book.pub_date).getFullYear().toString() : '';
            book.language = book.language || '中文';
            book.stock = book.available_copies || book.stock || 0;
            book.location = book.location || `${Math.floor(Math.random() * 4) + 1}楼${['东', '西', '南', '北'][Math.floor(Math.random() * 4)]}区${Math.floor(Math.random() * 15) + 1}排${['A', 'B', 'C', 'D'][Math.floor(Math.random() * 4)]}面${Math.floor(Math.random() * 10) + 1}列${Math.floor(Math.random() * 7) + 1}层`;
            book.description = book.description || `${book.title}是一本优秀的${book.category}类图书，由${book.author}所著，内容丰富，值得一读。`;
            
            const stockStatus = book.stock === 0 ? 'out' : book.stock <= 3 ? 'low' : 'available';
            const stockText = book.stock === 0 ? '已借完' : book.stock <= 3 ? '库存紧张' : '在库';
            
            return `
                <div class="book-card" data-book-id="${book.id}">
                    <div class="book-cover" style="background-image: url('${book.coverImage || defaultCoverImage}'); background-size: cover; background-position: center;" onerror="this.style.backgroundImage='url(${defaultCoverImage})'">
                        <span class="stock-badge stock-${stockStatus}">${stockText}</span>
                    </div>
                    <div class="book-info">
                        <div class="book-title">${book.title}</div>
                        <div class="book-author">${book.author}</div>
                        <div class="book-publisher">${book.publisher} · ${book.publishDate}</div>
                        <div class="book-tags">
                            <span class="tag tag-category">${book.category}</span>
                            <span class="tag tag-status">${book.status}</span>
                            <span class="tag tag-language">${book.language}</span>
                        </div>
                        <div class="book-stock">
                            库存：<span class="stock-number">${book.stock}</span>
                        </div>
                        <div class="book-actions">
                            <button class="btn-action btn-view" onclick="viewBookDetail(${book.id})">
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                    <circle cx="12" cy="12" r="3"></circle>
                                </svg>
                                详情
                            </button>
                            <button class="btn-action btn-borrow" onclick="borrowBook(${book.id})" ${book.stock === 0 ? 'disabled' : ''}>
                                <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path>
                                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path>
                                </svg>
                                借阅
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
        
    } catch (error) {
        console.error('获取新书推荐失败:', error);
        grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 40px 20px; color: #ef4444;"><p>加载失败，请刷新重试</p></div>';
    }
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

// 手动清除缓存并重新加载（可在浏览器控制台调用）
window.clearLibraryCache = function() {
    localStorage.removeItem('libraryBooksData');
    localStorage.removeItem('libraryDataVersion');
    location.reload();
};

// 初始化用户信息和权限
function initUserRole() {
    const currentUser = sessionStorage.getItem('currentUser');
    
    // 未登录跳转到登录页
    if (!currentUser) {
        window.location.href = 'login.html';
        return null;
    }
    
    const user = JSON.parse(currentUser);
    
    // 更新用户名显示
    const usernameElement = document.getElementById('current-username');
    if (usernameElement) {
        usernameElement.textContent = user.role === 'admin' ? '管理员' : '普通用户';
    }
    
    // 根据角色显示菜单
    if (user.role === 'admin') {
        // 管理员显示所有菜单
        const adminMenus = document.querySelectorAll('.admin-only');
        adminMenus.forEach(menu => {
            menu.style.display = '';
        });
    }
    
    return user;
}

// 用户管理功能（管理员专用）
function showUserManagement(event) {
    event.preventDefault();
    showNotification('用户管理功能开发中...', 'info');
}

// 归还图书功能
async function returnBook(borrowId) {
    try {
        // 检查网络连接
        if (!navigator.onLine) {
            showNotification('网络连接异常，请检查网络后重试', 'error');
            return;
        }

        // 获取系统设置中的逾期罚款信息
        let finePerDay = 0.5; // 默认值
        let maxFine = 50; // 默认值
        
        try {
            const settingsResponse = await fetch(`${API_BASE}/settings`, {
                credentials: 'include'
            });
            if (settingsResponse.ok) {
                const settingsData = await settingsResponse.json();
                if (settingsData.ok && settingsData.settings) {
                    finePerDay = parseFloat(settingsData.settings.overdue_fine_per_day) || 0.5;
                    maxFine = parseFloat(settingsData.settings.max_fine_amount) || 50;
                }
            }
        } catch (error) {
            console.warn('获取系统设置失败，使用默认值:', error);
        }

        // 查找当前借阅记录，检查是否逾期
        const borrowRecord = document.querySelector(`[data-borrow-id="${borrowId}"]`);
        if (borrowRecord) {
            // 从DOM中获取应还日期
            const dueDateText = borrowRecord.querySelector('.borrow-value.overdue, .borrow-value.due-soon, .borrow-value')?.textContent;
            const statusText = borrowRecord.querySelector('.status-badge')?.textContent;
            
            // 检查是否逾期
            if (statusText && statusText.includes('已逾期')) {
                // 计算逾期天数
                const today = new Date();
                today.setHours(0, 0, 0, 0);
                const dueDate = new Date(dueDateText);
                dueDate.setHours(0, 0, 0, 0);
                const overdueDays = Math.ceil((today - dueDate) / (1000 * 60 * 60 * 24));
                const fineAmount = Math.min(overdueDays * finePerDay, maxFine).toFixed(2);
                
                // 显示逾期警告弹窗
                alert(`还书失败！\n\n图书名称：《${borrowRecord.querySelector('.borrow-book-title')?.textContent || '未知'}》\n应还日期：${dueDateText}\n逾期天数：${overdueDays}天\n罚款金额：¥${fineAmount}元 (每天¥${finePerDay}，最高¥${maxFine})\n\n请先前往图书馆服务台缴纳罚款后再归还图书。\n感谢您的配合!`);
                return;
            }
        }

        // 显示确认对话框
        if (!confirm('确定要归还这本书吗？')) {
            return;
        }

        showNotification('正在处理归还请求...', 'info');

        // 调用后端归还API
        const response = await fetch(`${API_BASE}/return`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            credentials: 'include',
            body: JSON.stringify({
                borrow_id: borrowId
            })
        });

        // 处理HTTP错误状态
        if (response.status === 400) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || '请求参数错误');
        } else if (response.status === 401) {
            showNotification('登录已过期，请重新登录', 'error');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2000);
            return;
        } else if (response.status === 403) {
            throw new Error('权限不足，无法归还此图书');
        } else if (response.status === 404) {
            throw new Error('借阅记录不存在');
        } else if (response.status >= 500) {
            throw new Error('服务器内部错误，请稍后重试');
        } else if (!response.ok) {
            throw new Error(`归还请求失败 (${response.status})`);
        }

        let result;
        try {
            result = await response.json();
        } catch (parseError) {
            console.error('解析归还响应失败:', parseError);
            throw new Error('服务器响应格式错误');
        }

        if (!result.ok) {
            throw new Error(result.message || result.error || '归还失败');
        }

        // 检查是否有逾期罚款
        let successMessage = '图书归还成功！';
        if (result.overdue_days && result.overdue_days > 0 && result.fine_amount && result.fine_amount > 0) {
            successMessage = `图书归还成功！\n逾期${result.overdue_days}天，罚款金额：¥${result.fine_amount.toFixed(2)}元\n请前往服务台缴纳罚款。`;
        }

        // 归还成功
        showNotification(successMessage, 'success');
        
        // 重新加载我的借阅列表
        await renderMyBorrows();
        
        // 重新加载图书列表以更新库存
        await renderBooks();

    } catch (error) {
        console.error('归还失败:', error);
        
        // 网络错误处理
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            showNotification('网络连接失败，请检查网络后重试', 'error');
        } else if (error.name === 'AbortError') {
            showNotification('请求超时，请重试', 'error');
        } else {
            showNotification(error.message || '归还失败，请重试', 'error');
        }
    }
}

// 渲染我的借阅页面
async function renderMyBorrows() {
    const container = document.getElementById('my-borrows-container');
    if (!container) return;

    try {
        // 检查网络连接
        if (!navigator.onLine) {
            container.innerHTML = '<div style="text-align: center; padding: 40px; color: #ef4444;"><h3>网络连接异常</h3><p>请检查网络后重试</p></div>';
            return;
        }

        showNotification('正在加载借阅记录...', 'info');

        // 调用后端API获取当前用户的借阅记录
        const response = await fetch(`${API_BASE}/my-borrows`, {
            credentials: 'include'
        });

        // 处理HTTP错误状态
        if (response.status === 401) {
            showNotification('登录已过期，请重新登录', 'error');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2000);
            return;
        } else if (response.status >= 500) {
            throw new Error('服务器内部错误，请稍后重试');
        } else if (!response.ok) {
            throw new Error(`获取借阅记录失败 (${response.status})`);
        }

        let data;
        try {
            data = await response.json();
        } catch (parseError) {
            console.error('解析借阅记录响应失败:', parseError);
            throw new Error('服务器响应格式错误');
        }

        if (!data.ok) {
            throw new Error(data.message || data.error || '获取借阅记录失败');
        }

        const borrows = data.borrows || [];
        
        // 调试信息
        console.log('[DEBUG] 获取借阅记录响应:', data);
        console.log('[DEBUG] 借阅记录数量:', borrows.length);
        if (borrows.length > 0) {
            console.log('[DEBUG] 第一条借阅记录:', borrows[0]);
        }

        if (borrows.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 60px 20px; color: #64748b;">
                    <div style="font-size: 48px; margin-bottom: 16px;">📚</div>
                    <h3>暂无借阅记录</h3>
                    <p>您还没有借阅任何图书，快去借阅吧！</p>
                </div>
            `;
            return;
        }

        // 排序借阅记录：逾期优先级最高，按应还日期降序，已还的放最后
        borrows.sort((a, b) => {
            const aReturned = !!a.return_date;
            const bReturned = !!b.return_date;
            
            // 已归还的记录放最后
            if (aReturned && !bReturned) return 1;
            if (!aReturned && bReturned) return -1;
            
            // 如果都已归还，按归还日期降序
            if (aReturned && bReturned) {
                return new Date(b.return_date) - new Date(a.return_date);
            }
            
            // 如果都未归还，检查是否逾期
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const aDueDate = new Date(a.due_date);
            const bDueDate = new Date(b.due_date);
            aDueDate.setHours(0, 0, 0, 0);
            bDueDate.setHours(0, 0, 0, 0);
            
            const aOverdue = today > aDueDate;
            const bOverdue = today > bDueDate;
            
            // 逾期的优先级最高
            if (aOverdue && !bOverdue) return -1;
            if (!aOverdue && bOverdue) return 1;
            
            // 如果都逾期或都不逾期，按应还日期降序（越早到期的越靠前）
            return aDueDate - bDueDate;
        });

        // 渲染借阅记录列表
        container.innerHTML = `
            <div class="borrows-list">
                ${borrows.map(borrow => {
                    // 计算借阅天数和是否逾期
                    const borrowDate = new Date(borrow.borrow_date);
                    const dueDate = new Date(borrow.due_date);
                    const returnDate = borrow.return_date ? new Date(borrow.return_date) : null;
                    const today = new Date();
                    today.setHours(0, 0, 0, 0);
                    dueDate.setHours(0, 0, 0, 0);
                    
                    const daysDiff = returnDate ? 
                        Math.ceil((returnDate - borrowDate) / (1000 * 60 * 60 * 24)) : 
                        Math.ceil((today - borrowDate) / (1000 * 60 * 60 * 24));
                    const daysUntilDue = Math.ceil((dueDate - today) / (1000 * 60 * 60 * 24));
                    
                    // 判断状态：已归还 > 已逾期 > 即将到期 > 正常
                    const isReturned = !!borrow.return_date;
                    const isOverdue = !isReturned && daysUntilDue < 0;
                    const isDueSoon = !isReturned && daysUntilDue <= 3 && daysUntilDue >= 0;

                    // 设置图书封面
                    const bookCoverImage = borrow.book_cover_url || `/static/images/${borrow.book_title}.jpg`;
                    
                    // 状态文本和样式
                    let statusText, statusClass;
                    if (isReturned) {
                        statusText = '已归还';
                        statusClass = 'status-returned';
                    } else if (isOverdue) {
                        statusText = '已逾期';
                        statusClass = 'status-overdue';
                    } else if (isDueSoon) {
                        statusText = '即将到期';
                        statusClass = 'status-due-soon';
                    } else {
                        statusText = '正常';
                        statusClass = 'status-normal';
                    }

                    return `
                        <div class="borrow-record" data-borrow-id="${borrow.id}">
                            <div class="borrow-book-cover" style="background-image: url('${bookCoverImage}'); background-size: cover; background-position: center;" onerror="this.style.backgroundImage='url(${defaultCoverImage})'">
                            </div>
                            <div class="borrow-info">
                                <div class="borrow-book-title">${borrow.book_title}</div>
                                <div class="borrow-book-author">作者：${borrow.book_author}</div>
                                <div class="borrow-details">
                                    <div class="borrow-detail-item">
                                        <span class="borrow-label">借阅日期：</span>
                                        <span class="borrow-value">${borrow.borrow_date}</span>
                                    </div>
                                    <div class="borrow-detail-item">
                                        <span class="borrow-label">应还日期：</span>
                                        <span class="borrow-value ${isOverdue ? 'overdue' : isDueSoon ? 'due-soon' : ''}">${borrow.due_date}</span>
                                    </div>
                                    ${returnDate ? `
                                    <div class="borrow-detail-item">
                                        <span class="borrow-label">归还日期：</span>
                                        <span class="borrow-value">${borrow.return_date}</span>
                                    </div>
                                    ` : ''}
                                    <div class="borrow-detail-item">
                                        <span class="borrow-label">借阅天数：</span>
                                        <span class="borrow-value">${daysDiff} 天</span>
                                    </div>
                                    <div class="borrow-detail-item">
                                        <span class="borrow-label">状态：</span>
                                        <span class="borrow-value status-badge ${statusClass}">
                                            ${statusText}
                                        </span>
                                    </div>
                                </div>
                                <div class="borrow-actions">
                                    ${!isReturned ? `
                                    <button class="btn-action btn-return" onclick="returnBook(${borrow.id})">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                                            <polyline points="9 22 9 12 15 12 15 22"></polyline>
                                        </svg>
                                        归还
                                    </button>
                                    ` : `
                                    <span class="borrow-returned-text">已归还</span>
                                    `}
                                </div>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;

    } catch (error) {
        console.error('获取借阅记录失败:', error);
        
        // 网络错误处理
        if (error.name === 'TypeError' && error.message.includes('fetch')) {
            container.innerHTML = '<div style="text-align: center; padding: 40px; color: #ef4444;"><h3>网络连接失败</h3><p>请检查网络后重试</p></div>';
            showNotification('网络连接失败，请检查网络后重试', 'error');
        } else if (error.name === 'AbortError') {
            container.innerHTML = '<div style="text-align: center; padding: 40px; color: #ef4444;"><h3>请求超时</h3><p>请重试</p></div>';
            showNotification('请求超时，请重试', 'error');
        } else {
            container.innerHTML = '<div style="text-align: center; padding: 40px; color: #ef4444;"><h3>加载失败</h3><p>无法加载借阅记录，请刷新重试</p></div>';
            showNotification(error.message || '获取借阅记录失败，请重试', 'error');
        }
    }
}


// 页面加载时初始化
document.addEventListener('DOMContentLoaded', async () => {
    try {
        // 初始化用户角色和权限
        const user = initUserRole();
        if (!user) return;
        
        // 初始化页面功能
        initPageTabs();
        initFilters();
        initBorrowForm();
        
        // 从后端API加载数据
        await renderBooks();
        updatePagination();
        renderPopularBooks();
        renderNewBooks();
        
        console.log('图书借阅页面初始化完成');
    } catch (error) {
        console.error('初始化失败:', error);
        const grid = document.getElementById('books-grid');
        if (grid) {
            grid.innerHTML = '<div style="grid-column: 1/-1; text-align: center; padding: 60px 20px; color: #ef4444;"><h3>加载失败</h3><p>无法加载图书数据，请刷新页面重试</p></div>';
        }
    }
});
