<template>
  <div class="books-page">
    <div class="page-header">
      <h1 class="page-title">图书资源库</h1>
      <el-button type="primary" size="large" @click="openBorrowModal">
        <el-icon><Plus /></el-icon>
        借阅图书
      </el-button>
    </div>

    <div class="filter-bar">
      <div class="search-box">
        <el-input
          v-model="searchForm.search"
          placeholder="搜索书名、作者或ISBN..."
          prefix-icon="Search"
          size="large"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
      </div>
      <el-select
        v-model="searchForm.category"
        placeholder="全部分类"
        size="large"
        clearable
        @change="handleSearch"
      >
        <el-option label="文学" value="文学" />
        <el-option label="科技" value="科技" />
        <el-option label="历史" value="历史" />
        <el-option label="艺术" value="艺术" />
        <el-option label="教育" value="教育" />
        <el-option label="其他" value="其他" />
      </el-select>
      <el-select
        v-model="searchForm.language"
        placeholder="全部语言"
        size="large"
        clearable
        @change="handleSearch"
      >
        <el-option label="中文" value="中文" />
        <el-option label="英文" value="英文" />
      </el-select>
      <el-select
        v-model="searchForm.year"
        placeholder="出版年份"
        size="large"
        clearable
        @change="handleSearch"
      >
        <el-option label="2024年" value="2024" />
        <el-option label="2023年" value="2023" />
        <el-option label="2022年" value="2022" />
        <el-option label="2021年" value="2021" />
        <el-option label="更早" value="older" />
      </el-select>
    </div>

    <div class="result-bar">
      <div class="result-info">
        共找到 <span class="highlight-number">{{ pagination.total }}</span> 本图书
      </div>
      <div class="display-options">
        <span>每页显示：</span>
        <el-select v-model="searchForm.perPage" size="small" @change="handleSearch">
          <el-option :value="12" label="12" />
          <el-option :value="24" label="24" />
          <el-option :value="48" label="48" />
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
          <div class="book-publisher">{{ book.publisher }} · {{ getPublishYear(book) }}</div>
          <div class="book-tags">
            <span class="tag tag-category">{{ book.category }}</span>
            <span class="tag" :class="'tag-' + getStatusClass(book)">
              {{ getStatusText(book) }}
            </span>
          </div>
          <div class="book-stock">
            库存：<span class="stock-number">{{ getStock(book) }}</span>
          </div>
          <div class="book-actions">
            <el-button type="default" size="small" @click.stop="viewBookDetail(book)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button
              type="primary"
              size="small"
              :disabled="book.status !== 'available'"
              @click.stop="borrowBook(book)"
            >
              <el-icon><Document /></el-icon>
              借阅
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="empty-state" v-if="!loading && books.length === 0">
      <el-empty description="未找到匹配的图书，请调整搜索条件" />
    </div>

    <div class="pagination-container" v-if="pagination.pages > 1">
      <el-pagination
        v-model:current-page="searchForm.page"
        v-model:page-size="searchForm.perPage"
        :page-sizes="[12, 24, 48]"
        :total="pagination.total"
        layout="prev, pager, next, jumper"
        @current-change="handleSearch"
        @size-change="handleSearch"
      />
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      title="图书详情"
      width="700px"
      :close-on-click-modal="false"
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
            <p><span>出版日期：</span>{{ getPublishYear(selectedBook) }}</p>
            <p>
              <span>状态：</span>
              <el-tag :type="getStatusTagType(selectedBook)">
                {{ getStatusText(selectedBook) }}
              </el-tag>
            </p>
            <p><span>库存：</span>{{ getStock(selectedBook) }} 本</p>
            <p><span>位置：</span>{{ selectedBook.location || '暂无位置信息' }}</p>
          </div>
          <div class="detail-description" v-if="selectedBook.description">
            <h3>内容简介</h3>
            <p>{{ selectedBook.description }}</p>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          type="primary"
          :disabled="selectedBook?.status !== 'available'"
          @click="borrowBookFromDetail"
        >
          立即借阅
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="borrowDialogVisible"
      title="借阅图书"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="borrowFormRef"
        :model="borrowForm"
        :rules="borrowRules"
        label-width="100px"
      >
        <el-form-item label="图书信息">
          <div class="book-info-display" v-if="selectedBook">
            <div class="selected-book">
              <div
                class="selected-book-cover"
                :style="{ backgroundImage: `url(${getCoverImage(selectedBook)})` }"
              ></div>
              <div class="selected-book-details">
                <h4>{{ selectedBook.title }}</h4>
                <p>作者：{{ selectedBook.author }}</p>
                <p>ISBN：{{ selectedBook.isbn }}</p>
                <p>可借数量：{{ getStock(selectedBook) }} 本</p>
              </div>
            </div>
          </div>
          <p v-else class="info-placeholder">请从列表中选择要借阅的图书</p>
        </el-form-item>
        <el-form-item label="学工号" prop="readerId">
          <el-input v-model="borrowForm.readerId" placeholder="请输入读者证号" />
        </el-form-item>
        <el-form-item label="读者姓名" prop="readerName">
          <el-input v-model="borrowForm.readerName" placeholder="请输入读者姓名" />
        </el-form-item>
        <el-form-item label="借阅日期" prop="borrowDate">
          <el-date-picker
            v-model="borrowForm.borrowDate"
            type="date"
            placeholder="选择借阅日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="应还日期" prop="returnDate">
          <el-date-picker
            v-model="borrowForm.returnDate"
            type="date"
            placeholder="选择应还日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="borrowForm.note"
            type="textarea"
            :rows="3"
            placeholder="选填，可以添加借阅备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="borrowLoading" @click="submitBorrow">
          确认借阅
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, View, Document } from '@element-plus/icons-vue'
import * as booksApi from '@/api/books'
import * as borrowsApi from '@/api/borrows'
import * as settingsApi from '@/api/settings'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const books = ref([])
const loading = ref(false)
const selectedBook = ref(null)
const detailDialogVisible = ref(false)
const borrowDialogVisible = ref(false)
const borrowFormRef = ref(null)
const borrowLoading = ref(false)

const searchForm = reactive({
  search: '',
  category: '',
  language: '',
  year: '',
  page: 1,
  perPage: 24
})

const pagination = reactive({
  total: 0,
  pages: 0
})

const borrowForm = reactive({
  bookId: null,
  readerId: '',
  readerName: '',
  borrowDate: '',
  returnDate: '',
  note: ''
})

const borrowRules = {
  readerId: [{ required: true, message: '请输入读者ID', trigger: 'blur' }],
  readerName: [
    { required: true, message: '请输入读者姓名', trigger: 'blur' },
    { 
      pattern: /^[\u4e00-\u9fa5a-zA-Z\s]+$/, 
      message: '姓名只能包含中文或英文字母', 
      trigger: 'blur' 
    }
  ],
  borrowDate: [{ required: true, message: '请选择借阅日期', trigger: 'change' }],
  returnDate: [{ required: true, message: '请选择应还日期', trigger: 'change' }]
}

let defaultBorrowDays = 30

onMounted(async () => {
  await loadSettings()
  await loadBooks()

  if (route.query.bookId && route.query.borrow === '1') {
    const bookId = parseInt(route.query.bookId)
    try {
      const book = await booksApi.getBook(bookId)
      if (book && book.id) {
        openBorrowModalWithBook(book)
      }
    } catch (e) {
      console.error('获取图书详情失败:', e)
    }
  }
})

watch(() => route.query, async (query) => {
  if (query.bookId && query.borrow === '1') {
    const bookId = parseInt(query.bookId)
    try {
      const book = await booksApi.getBook(bookId)
      if (book && book.id) {
        openBorrowModalWithBook(book)
      }
    } catch (e) {
      console.error('获取图书详情失败:', e)
    }
  }
})

async function loadSettings() {
  try {
    const response = await settingsApi.getSettings()
    if (response && response.borrowDays) {
      defaultBorrowDays = parseInt(response.borrowDays) || 30
    }
  } catch (error) {
    console.error('加载系统设置失败:', error)
  }
}

async function loadBooks() {
  loading.value = true
  try {
    const response = await booksApi.getBooks(searchForm)
    books.value = response.records || response.books || []
    pagination.total = response.total || response.pagination?.total || 0
    pagination.pages = response.pages || response.pagination?.pages || 0
  } catch (error) {
    ElMessage.error('加载图书列表失败')
    console.error('加载图书列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  searchForm.page = 1
  loadBooks()
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

function getStatusClass(book) {
  const status = book.status
  if (status === 'available') return 'status-available'
  if (status === 'borrowed') return 'status-borrowed'
  if (status === 'frozen') return 'status-frozen'
  return 'status-unknown'
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

function borrowBook(book) {
  if (book.status !== 'available') {
    ElMessage.warning('该图书不可借阅')
    return
  }
  openBorrowModalWithBook(book)
}

function borrowBookFromDetail() {
  if (!selectedBook.value) return
  detailDialogVisible.value = false
  openBorrowModalWithBook(selectedBook.value)
}

function openBorrowModal() {
  if (!selectedBook.value) {
    ElMessage.warning('请先选择要借阅的图书')
    return
  }
  openBorrowModalWithBook(selectedBook.value)
}

function openBorrowModalWithBook(book) {
  selectedBook.value = book
  borrowForm.bookId = book.id
  
  const user = authStore.user
  if (user) {
    borrowForm.readerId = user.userId || user.user_id || user.id || ''
    borrowForm.readerName = user.realName || user.name || user.real_name || user.username || ''
  }
  
  borrowForm.note = '我爱软件工程'
  
  const today = new Date()
  borrowForm.borrowDate = formatDate(today)
  const returnDate = new Date(today.getTime() + defaultBorrowDays * 24 * 60 * 60 * 1000)
  borrowForm.returnDate = formatDate(returnDate)
  
  borrowDialogVisible.value = true
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function submitBorrow() {
  if (!borrowFormRef.value) return
  
  await borrowFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    const borrowDate = new Date(borrowForm.borrowDate)
    const returnDate = new Date(borrowForm.returnDate)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    borrowDate.setHours(0, 0, 0, 0)
    
    if (borrowDate < today) {
      ElMessage.error('借阅日期不能早于今天')
      return
    }
    
    if (returnDate <= borrowDate) {
      ElMessage.error('应还日期必须晚于借阅日期')
      return
    }
    
    borrowLoading.value = true
    try {
      await borrowsApi.borrowBook({
        bookId: borrowForm.bookId,
        userId: authStore.user?.userId || authStore.user?.id,
        borrowDate: borrowForm.borrowDate,
        dueDate: borrowForm.returnDate,
        readerId: borrowForm.readerId,
        readerName: borrowForm.readerName,
        note: borrowForm.note
      })

      ElMessage.success(`《${selectedBook.value.title}》借阅成功！`)
      borrowDialogVisible.value = false
      loadBooks()
    } catch (error) {
      ElMessage.error(error.message || '借阅失败，请重试')
    } finally {
      borrowLoading.value = false
    }
  })
}
</script>

<style scoped>
.books-page {
  background: var(--bg-primary);
  border-radius: 12px;
  padding: 32px;
  box-shadow: var(--shadow-sm);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 280px;
}

.result-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.result-info {
  font-size: 14px;
  color: var(--text-secondary);
}

.highlight-number {
  color: var(--text-primary);
  font-weight: 700;
  font-size: 16px;
}

.display-options {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-secondary);
}

.books-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
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
  height: 240px;
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
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
  min-height: 44px;
}

.book-author {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.book-publisher {
  font-size: 12px;
  color: var(--text-light);
  margin-bottom: 12px;
}

.book-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
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

.tag-status-available {
  background: #d1fae5;
  color: #065f46;
}

.tag-status-borrowed {
  background: #dbeafe;
  color: #1e40af;
}

.tag-status-frozen {
  background: #e0e7ff;
  color: #3730a3;
}

.book-stock {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stock-number {
  font-weight: 700;
  color: var(--text-primary);
}

.book-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  padding: 60px 20px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding-top: 24px;
  border-top: 1px solid var(--border-color);
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

.book-info-display {
  background: var(--bg-secondary);
  padding: 16px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.info-placeholder {
  color: var(--text-secondary);
  font-size: 14px;
  text-align: center;
  padding: 20px;
}

.selected-book {
  display: flex;
  gap: 16px;
}

.selected-book-cover {
  width: 80px;
  height: 100px;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.selected-book-details h4 {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.selected-book-details p {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

@media (max-width: 768px) {
  .books-page {
    padding: 20px;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .filter-bar {
    flex-direction: column;
  }
  
  .search-box {
    width: 100%;
    min-width: auto;
  }
  
  .books-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }
  
  .book-cover {
    height: 200px;
  }
  
  .book-detail {
    grid-template-columns: 1fr;
  }
}
</style>
