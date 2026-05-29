<template>
  <div class="kiosk-search">
    <div class="page-header">
      <el-button size="large" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div class="header-title">
        <h2>查询图书</h2>
        <p>搜索您感兴趣的图书</p>
      </div>
    </div>

    <div class="search-section">
      <el-input
        v-model="searchForm.keyword"
        placeholder="输入书名、作者或ISBN进行搜索"
        size="large"
        class="main-search"
        @keyup.enter="searchBooks"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button type="primary" @click="searchBooks" :loading="loading">
            搜索
          </el-button>
        </template>
      </el-input>

      <div class="quick-search">
        <span class="quick-label">热门搜索：</span>
        <el-tag
          v-for="(tag, index) in hotTags"
          :key="index"
          size="large"
          effect="plain"
          class="hot-tag"
          @click="quickSearch(tag)"
        >
          {{ tag }}
        </el-tag>
      </div>

      <div class="filter-tags">
        <span class="filter-label">分类筛选：</span>
        <el-radio-group v-model="searchForm.category" size="large">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="文学">文学</el-radio-button>
          <el-radio-button value="科技">科技</el-radio-button>
          <el-radio-button value="历史">历史</el-radio-button>
          <el-radio-button value="艺术">艺术</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="results-section" v-if="searched">
      <div class="results-header">
        <div class="results-info">
          共找到 <span class="highlight">{{ pagination.total }}</span> 本图书
        </div>
        <div class="results-sort">
          <span>排序：</span>
          <el-select v-model="searchForm.sort" size="large" @change="searchBooks">
            <el-option label="相关度" value="" />
            <el-option label="出版日期" value="pub_date" />
            <el-option label="库存数量" value="stock" />
          </el-select>
        </div>
      </div>

      <div class="books-grid" v-loading="loading">
        <div
          v-for="book in books"
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
            <div class="book-meta">
              <el-tag size="small" type="info">{{ book.category }}</el-tag>
              <span class="stock-info">
                库存：{{ getStock(book) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="empty-state" v-if="!loading && books.length === 0">
        <el-empty description="未找到匹配的图书，请尝试其他关键词" />
      </div>

      <div class="pagination-container" v-if="pagination.pages > 1">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.perPage"
          :page-sizes="[12, 24, 48]"
          :total="pagination.total"
          layout="prev, pager, next"
          @current-change="searchBooks"
          @size-change="searchBooks"
        />
      </div>
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      title="图书详情"
      width="700px"
    >
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
            <p><span>出版年份：</span>{{ getPublishYear(selectedBook) }}</p>
            <p>
              <span>状态：</span>
              <el-tag :type="getStatusTagType(selectedBook)" size="large">
                {{ getStatusText(selectedBook) }}
              </el-tag>
            </p>
            <p><span>库存：</span>{{ getStock(selectedBook) }} 本</p>
            <p><span>位置：</span>{{ selectedBook.location || '请咨询工作人员' }}</p>
          </div>
          <div class="detail-description" v-if="selectedBook.description">
            <h3>内容简介</h3>
            <p>{{ selectedBook.description }}</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button size="large" @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          type="primary"
          size="large"
          :disabled="selectedBook?.status !== 'available'"
          @click="goToBorrow"
        >
          去借阅
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Search
} from '@element-plus/icons-vue'
import * as booksApi from '@/api/books'

const router = useRouter()

const loading = ref(false)
const searched = ref(false)
const books = ref([])
const selectedBook = ref(null)
const detailDialogVisible = ref(false)

const hotTags = ['三体', '活着', '人类简史', '百年孤独', 'JavaScript', 'Python']

const searchForm = reactive({
  keyword: '',
  category: '',
  sort: '',
  page: 1,
  perPage: 24
})

const pagination = reactive({
  total: 0,
  pages: 0
})

onMounted(() => {
  searchForm.page = 1
  searchForm.perPage = 24
})

function goBack() {
  router.push('/kiosk')
}

function quickSearch(tag) {
  searchForm.keyword = tag
  searchBooks()
}

async function searchBooks() {
  if (!searchForm.keyword.trim() && !searchForm.category) {
    ElMessage.warning('请输入搜索关键词或选择分类')
    return
  }

  loading.value = true
  searched.value = true

  try {
    const params = {
      search: searchForm.keyword,
      page: searchForm.page,
      perPage: searchForm.perPage
    }

    if (searchForm.category) {
      params.category = searchForm.category
    }

    const response = await booksApi.getBooks(params)
    books.value = response.records || response.books || []
    pagination.total = response.total || response.pagination?.total || 0
    pagination.pages = response.pages || response.pagination?.pages || 0
  } catch (error) {
    ElMessage.error('搜索失败')
    console.error('搜索失败:', error)
  } finally {
    loading.value = false
  }
}

function getCoverImage(book) {
  return book.cover_url || book.coverImage || `/static/images/${book.title}.jpg`
}

function getStockStatus(book) {
  const stock = getStock(book)
  if (stock === 0) return 'out'
  if (stock <= 3) return 'low'
  return 'available'
}

function getStock(book) {
  return book?.availableCopies ?? book?.available_copies ?? book?.stock ?? 0
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

function getPublishYear(book) {
  if (book.publishDate) return book.publishDate
  if (book.pub_date) {
    return new Date(book.pub_date).getFullYear().toString()
  }
  return '未知'
}

function viewBookDetail(book) {
  selectedBook.value = book
  detailDialogVisible.value = true
}

function goToBorrow() {
  detailDialogVisible.value = false
  router.push({
    path: '/kiosk/borrow',
    query: { bookId: selectedBook.value.id, borrow: '1' }
  })
}
</script>

<style scoped>
.kiosk-search {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
}

.header-title h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1e3a5f;
  margin: 0 0 4px 0;
}

.header-title p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.search-section {
  background: white;
  border-radius: 20px;
  padding: 40px;
  margin-bottom: 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.main-search {
  margin-bottom: 24px;
}

.main-search :deep(.el-input__wrapper) {
  padding: 8px 16px;
  border-radius: 16px;
}

.main-search :deep(.el-input__inner) {
  font-size: 18px;
}

.quick-search,
.filter-tags {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.quick-search:last-child,
.filter-tags:last-child {
  margin-bottom: 0;
}

.quick-label,
.filter-label {
  font-size: 14px;
  color: #64748b;
  font-weight: 500;
}

.hot-tag {
  cursor: pointer;
  transition: all 0.3s ease;
}

.hot-tag:hover {
  background: #dbeafe;
  color: #1d4ed8;
  border-color: #93c5fd;
}

.results-section {
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #e2e8f0;
}

.results-info {
  font-size: 16px;
  color: #64748b;
}

.results-info .highlight {
  font-size: 24px;
  font-weight: 700;
  color: #1d4ed8;
  margin: 0 4px;
}

.results-sort {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.book-card {
  background: #f8fafc;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.book-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
  border-color: #3b82f6;
}

.book-cover {
  width: 100%;
  height: 200px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  position: relative;
}

.stock-badge {
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
  color: #059669;
}

.stock-low {
  color: #d97706;
}

.stock-out {
  color: #b91c1c;
}

.book-info {
  padding: 16px;
}

.book-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e3a5f;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
  min-height: 44px;
}

.book-author {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 12px;
}

.book-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stock-info {
  font-size: 12px;
  color: #64748b;
}

.empty-state {
  padding: 60px 20px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding-top: 24px;
  border-top: 2px solid #e2e8f0;
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
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.detail-info h2 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #1e3a5f;
}

.detail-meta p {
  margin-bottom: 12px;
  font-size: 14px;
  color: #64748b;
}

.detail-meta p span {
  font-weight: 600;
  color: #1e3a5f;
  margin-right: 4px;
}

.detail-description {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 2px solid #e2e8f0;
}

.detail-description h3 {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #1e3a5f;
}

.detail-description p {
  color: #64748b;
  line-height: 1.8;
}

@media (max-width: 1024px) {
  .quick-search,
  .filter-tags {
    flex-wrap: wrap;
  }
  
  .books-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }
  
  .book-cover {
    height: 180px;
  }
  
  .book-detail {
    grid-template-columns: 1fr;
  }
}
</style>
