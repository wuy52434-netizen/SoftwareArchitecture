<template>
  <div class="home-page">
    <div class="welcome-section">
      <div class="welcome-content">
        <h1 class="welcome-title">欢迎使用图书自动借书机系统</h1>
        <p class="welcome-subtitle">
          快速检索图书、在线借阅、轻松管理您的阅读生活
        </p>
        <el-button type="primary" size="large" @click="goToBooks">
          <el-icon><Search /></el-icon>
          开始检索图书
        </el-button>
      </div>
      <div class="welcome-illustration">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 300 200" class="illustration">
          <defs>
            <linearGradient id="grad1" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:#667eea;stop-opacity:1" />
              <stop offset="100%" style="stop-color:#764ba2;stop-opacity:1" />
            </linearGradient>
          </defs>
          <rect x="20" y="40" width="60" height="120" rx="4" fill="#e0e7ff" />
          <rect x="90" y="40" width="60" height="120" rx="4" fill="#ddd6fe" />
          <rect x="160" y="40" width="60" height="120" rx="4" fill="#c7d2fe" />
          <rect x="30" y="50" width="40" height="8" rx="2" fill="#667eea" />
          <rect x="30" y="65" width="35" height="8" rx="2" fill="#764ba2" />
          <rect x="30" y="80" width="45" height="8" rx="2" fill="#667eea" />
          <rect x="100" y="50" width="40" height="8" rx="2" fill="#764ba2" />
          <rect x="100" y="65" width="35" height="8" rx="2" fill="#667eea" />
          <rect x="100" y="80" width="45" height="8" rx="2" fill="#764ba2" />
          <rect x="170" y="50" width="40" height="8" rx="2" fill="#667eea" />
          <rect x="170" y="65" width="35" height="8" rx="2" fill="#764ba2" />
          <rect x="170" y="80" width="45" height="8" rx="2" fill="#667eea" />
          <circle cx="250" cy="100" r="25" fill="url(#grad1)" />
          <circle cx="250" cy="90" r="8" fill="white" />
          <path d="M238 120 Q250 130 262 120" stroke="white" stroke-width="3" fill="none" />
        </svg>
      </div>
    </div>

    <div class="stats-section">
      <div class="stat-card">
        <div class="stat-icon books">
          <el-icon><Collection /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.totalBooks }}</div>
          <div class="stat-label">馆藏图书</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon borrows">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.myBorrows }}</div>
          <div class="stat-label">我的借阅</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon overdue">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.overdue }}</div>
          <div class="stat-label">已逾期</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon popular">
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.popular }}</div>
          <div class="stat-label">热门图书</div>
        </div>
      </div>
    </div>

    <div class="section popular-section">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><Star /></el-icon>
          热门图书
        </h2>
        <el-button text @click="goToBooks">
          查看更多 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="books-grid" v-if="popularBooks.length > 0">
        <div 
          v-for="book in popularBooks" 
          :key="book.id" 
          class="book-card"
          @click="viewBookDetail(book)"
        >
          <div 
            class="book-cover" 
            :style="{ backgroundImage: `url(${getCoverImage(book)})` }"
          >
            <span 
              class="stock-badge" 
              :class="'stock-' + getStockStatus(book)"
            >
              {{ getStockText(book) }}
            </span>
          </div>
          <div class="book-info">
            <div class="book-title">{{ book.title }}</div>
            <div class="book-author">{{ book.author }}</div>
            <div class="book-tags">
              <span class="tag tag-category">{{ book.category }}</span>
              <span class="tag tag-status">{{ getStatusText(book) }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无热门图书" />
    </div>

    <div class="section new-section">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><MagicStick /></el-icon>
          新书推荐
        </h2>
        <el-button text @click="goToBooks">
          查看更多 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="books-grid" v-if="newBooks.length > 0">
        <div 
          v-for="book in newBooks" 
          :key="book.id" 
          class="book-card"
          @click="viewBookDetail(book)"
        >
          <div 
            class="book-cover" 
            :style="{ backgroundImage: `url(${getCoverImage(book)})` }"
          >
            <span 
              class="stock-badge" 
              :class="'stock-' + getStockStatus(book)"
            >
              {{ getStockText(book) }}
            </span>
          </div>
          <div class="book-info">
            <div class="book-title">{{ book.title }}</div>
            <div class="book-author">{{ book.author }}</div>
            <div class="book-tags">
              <span class="tag tag-category">{{ book.category }}</span>
              <span class="tag tag-status">{{ getStatusText(book) }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无新书推荐" />
    </div>

    <el-dialog v-model="detailDialogVisible" title="图书详情" width="700px">
      <div v-if="selectedBook" class="book-detail">
        <div class="detail-cover">
          <img :src="getCoverImage(selectedBook)" alt="封面" />
        </div>
        <div class="detail-info">
          <h2>{{ selectedBook.title }}</h2>
          <div class="detail-meta">
            <p><span>作者：</span>{{ selectedBook.author }}</p>
            <p><span>分类：</span>{{ selectedBook.category }}</p>
            <p><span>ISBN：</span>{{ selectedBook.isbn }}</p>
            <p><span>出版社：</span>{{ selectedBook.publisher || '未知' }}</p>
            <p><span>出版日期：</span>{{ selectedBook.publishDate || '未知' }}</p>
            <p>
              <span>状态：</span>
              <el-tag :type="getStatusTagType(selectedBook)">
                {{ getStatusText(selectedBook) }}
              </el-tag>
            </p>
            <p><span>库存：</span>{{ selectedBook.available_copies || selectedBook.stock || 0 }} 本</p>
          </div>
          <div class="detail-description" v-if="selectedBook.description">
            <h3>内容简介</h3>
            <p>{{ selectedBook.description }}</p>
          </div>
          <div class="detail-actions">
            <el-button 
              type="primary" 
              size="large"
              :disabled="selectedBook.status !== 'available'"
              @click="borrowBook"
            >
              立即借阅
            </el-button>
            <el-button size="large" @click="detailDialogVisible = false">
              关闭
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Search, 
  Collection, 
  Document, 
  Warning, 
  TrendCharts,
  Star,
  ArrowRight,
  MagicStick
} from '@element-plus/icons-vue'
import * as booksApi from '@/api/books'
import * as borrowsApi from '@/api/borrows'

const router = useRouter()

const popularBooks = ref([])
const newBooks = ref([])
const selectedBook = ref(null)
const detailDialogVisible = ref(false)

const stats = reactive({
  totalBooks: 0,
  myBorrows: 0,
  overdue: 0,
  popular: 0
})

onMounted(async () => {
  await Promise.all([
    loadPopularBooks(),
    loadNewBooks(),
    loadMyBorrows()
  ])
})

async function loadPopularBooks() {
  try {
    const response = await booksApi.getPopularBooks()
    popularBooks.value = response.books || []
    stats.popular = popularBooks.value.length
  } catch (error) {
    console.error('加载热门图书失败:', error)
  }
}

async function loadNewBooks() {
  try {
    const response = await booksApi.getBooks({ perPage: 8 })
    newBooks.value = response.books || []
    stats.totalBooks = response.pagination?.total || newBooks.value.length
  } catch (error) {
    console.error('加载新书失败:', error)
  }
}

async function loadMyBorrows() {
  try {
    const response = await borrowsApi.getMyBorrows()
    const borrows = response.borrows || []
    stats.myBorrows = borrows.length
    const today = new Date()
    stats.overdue = borrows.filter(b => {
      if (b.return_date) return false
      const dueDate = new Date(b.due_date)
      return today > dueDate
    }).length
  } catch (error) {
    console.error('加载我的借阅失败:', error)
  }
}

function goToBooks() {
  router.push('/portal/books')
}

function viewBookDetail(book) {
  selectedBook.value = book
  detailDialogVisible.value = true
}

function getCoverImage(book) {
  return book.cover_url || book.coverImage || `/static/images/${book.title}.jpg`
}

function getStockStatus(book) {
  const stock = book.available_copies || book.stock || 0
  if (stock === 0) return 'out'
  if (stock <= 3) return 'low'
  return 'available'
}

function getStockText(book) {
  const status = getStockStatus(book)
  if (status === 'out') return '已借完'
  if (status === 'low') return '库存紧张'
  return '在库'
}

function getStatusText(book) {
  const status = book.status
  if (status === 'available') return '可借阅'
  if (status === 'borrowed') return '已借出'
  if (status === 'frozen') return '已冻结'
  return '未知'
}

function getStatusTagType(book) {
  const status = book.status
  if (status === 'available') return 'success'
  if (status === 'borrowed') return 'info'
  if (status === 'frozen') return 'warning'
  return 'info'
}

async function borrowBook() {
  if (!selectedBook.value) return
  router.push({
    path: '/portal/books',
    query: { bookId: selectedBook.value.id, borrow: '1' }
  })
  detailDialogVisible.value = false
}
</script>

<style scoped>
.home-page {
  max-width: 1400px;
  margin: 0 auto;
}

.welcome-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 48px;
  align-items: center;
  margin-bottom: 48px;
  padding: 48px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 24px;
  color: white;
}

.welcome-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 16px;
  line-height: 1.3;
}

.welcome-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 32px;
  line-height: 1.6;
}

.welcome-illustration {
  display: flex;
  justify-content: center;
  align-items: center;
}

.illustration {
  width: 300px;
  height: 200px;
}

.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 48px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: white;
  border-radius: 16px;
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--border-color);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.stat-icon.books {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
}

.stat-icon.borrows {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #15803d;
}

.stat-icon.overdue {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #b91c1c;
}

.stat-icon.popular {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #92400e;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.section {
  margin-bottom: 48px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
}

.book-card {
  background: white;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
  cursor: pointer;
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary-blue);
}

.book-cover {
  width: 100%;
  height: 200px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 12px;
  position: relative;
}

.book-cover .stock-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(4px);
}

.stock-available {
  color: var(--success-green);
}

.stock-low {
  color: var(--warning-yellow);
}

.stock-out {
  color: var(--danger-red);
}

.book-info {
  padding: 16px;
}

.book-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-author {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.book-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.tag-category {
  background: #dbeafe;
  color: #1e40af;
}

.tag-status {
  background: #d1fae5;
  color: #065f46;
}

.book-detail {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 32px;
}

.detail-cover {
  width: 100%;
}

.detail-cover img {
  width: 100%;
  border-radius: 12px;
  box-shadow: var(--shadow-md);
}

.detail-info h2 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 20px;
  color: var(--text-primary);
}

.detail-meta p {
  margin-bottom: 12px;
  font-size: 14px;
  color: var(--text-secondary);
}

.detail-meta p span {
  font-weight: 600;
  color: var(--text-primary);
  margin-right: 4px;
}

.detail-description {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
}

.detail-description h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.detail-description p {
  color: var(--text-secondary);
  line-height: 1.8;
}

.detail-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

@media (max-width: 1024px) {
  .welcome-section {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .welcome-illustration {
    order: -1;
  }
  
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .book-detail {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-section {
    grid-template-columns: 1fr;
  }
  
  .books-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  
  .book-cover {
    height: 160px;
  }
}
</style>
