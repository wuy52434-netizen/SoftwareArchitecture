<template>
  <div class="books-management">
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">图书管理</h1>
        <p class="page-subtitle">管理图书馆的所有图书资源</p>
      </div>
      <el-button type="primary" size="large" @click="openAddModal">
        <el-icon><Plus /></el-icon>
        添加图书
      </el-button>
    </div>

    <div class="filter-section">
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索图书名称、作者或ISBN..."
          size="large"
          clearable
          @keyup.enter="loadBooks"
          @clear="loadBooks"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="filter-fields">
        <el-select
          v-model="searchForm.category"
          placeholder="分类筛选"
          size="large"
          clearable
          @change="loadBooks"
        >
          <el-option label="全部" value="" />
          <el-option label="文学" value="文学" />
          <el-option label="科技" value="科技" />
          <el-option label="历史" value="历史" />
          <el-option label="艺术" value="艺术" />
          <el-option label="教育" value="教育" />
          <el-option label="其他" value="其他" />
        </el-select>
        <el-select
          v-model="searchForm.status"
          placeholder="状态筛选"
          size="large"
          clearable
          @change="loadBooks"
        >
          <el-option label="全部" value="" />
          <el-option label="可借阅" value="available" />
          <el-option label="已借出" value="borrowed" />
          <el-option label="已冻结" value="frozen" />
        </el-select>
        <el-button type="primary" size="large" @click="loadBooks">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button size="large" @click="resetSearch">
          重置
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <div class="table-header">
        <div class="table-info">
          共 <span class="highlight">{{ pagination.total }}</span> 本图书
        </div>
      </div>
      <el-table
        :data="books"
        v-loading="loading"
        style="width: 100%"
        :row-key="(row) => row.id"
        :stripe="true"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="封面" width="100" align="center">
          <template #default="{ row }">
            <div
              class="book-cover-small"
              :style="{ backgroundImage: `url(${getCoverImage(row)})` }"
            ></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="书名" min-width="180">
          <template #default="{ row }">
            <div class="book-title-cell">
              <span class="title-text">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120" />
        <el-table-column prop="category" label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row)" size="small">
              {{ getStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': getStock(row) === 0 }">
              {{ getStock(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="viewBook(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button type="primary" text size="small" @click="editBook(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              type="danger"
              text
              size="small"
              @click="deleteBook(row)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="searchForm.page"
          v-model:page-size="searchForm.perPage"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadBooks"
          @size-change="loadBooks"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑图书' : '添加图书'"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="bookFormRef"
        :model="bookForm"
        :rules="bookRules"
        label-width="100px"
        class="book-form"
      >
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="书名" prop="title">
              <el-input v-model="bookForm.title" placeholder="请输入书名" size="large" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="作者" prop="author">
              <el-input v-model="bookForm.author" placeholder="请输入作者" size="large" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="bookForm.category" placeholder="请选择分类" size="large" style="width: 100%">
                <el-option label="文学" value="文学" />
                <el-option label="科技" value="科技" />
                <el-option label="历史" value="历史" />
                <el-option label="艺术" value="艺术" />
                <el-option label="教育" value="教育" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="ISBN" prop="isbn">
              <el-input v-model="bookForm.isbn" placeholder="请输入ISBN" size="large" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="出版社">
              <el-input v-model="bookForm.publisher" placeholder="请输入出版社" size="large" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出版日期">
              <el-date-picker
                v-model="bookForm.pubDate"
                type="date"
                placeholder="选择出版日期"
                size="large"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="库存数量" prop="stock">
              <el-input-number
                v-model="bookForm.stock"
                :min="0"
                :max="1000"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number
                v-model="bookForm.price"
                :min="0"
                :max="9999"
                :precision="2"
                size="large"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="bookForm.status" placeholder="请选择状态" size="large" style="width: 100%">
                <el-option label="可借阅" value="available" />
                <el-option label="已借出" value="borrowed" />
                <el-option label="已冻结" value="frozen" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="语言">
              <el-select v-model="bookForm.language" placeholder="请选择语言" size="large" style="width: 100%">
                <el-option label="中文" value="中文" />
                <el-option label="英文" value="English" />
                <el-option label="日文" value="日文" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="位置">
          <el-input v-model="bookForm.location" placeholder="请输入馆藏位置" size="large" />
        </el-form-item>
        <el-form-item label="封面图片">
          <el-input v-model="bookForm.coverUrl" placeholder="请输入封面图片URL" size="large" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="bookForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入图书简介"
            size="large"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="large" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="large" :loading="submitLoading" @click="submitBookForm">
          {{ isEdit ? '保存修改' : '添加图书' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailDialogVisible"
      title="图书详情"
      width="600px"
    >
      <div v-if="selectedBook" class="book-detail">
        <div class="detail-header">
          <div
            class="detail-cover"
            :style="{ backgroundImage: `url(${getCoverImage(selectedBook)})` }"
          ></div>
          <div class="detail-info">
            <h2>{{ selectedBook.title }}</h2>
            <p class="author">{{ selectedBook.author }}</p>
            <el-tag :type="getStatusTagType(selectedBook)" size="large">
              {{ getStatusText(selectedBook) }}
            </el-tag>
          </div>
        </div>
        <el-divider />
        <el-descriptions :column="2" border>
          <el-descriptions-item label="分类">{{ selectedBook.category }}</el-descriptions-item>
          <el-descriptions-item label="ISBN">{{ selectedBook.isbn }}</el-descriptions-item>
          <el-descriptions-item label="出版社">{{ selectedBook.publisher || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="出版日期">
            {{ selectedBook.pub_date || selectedBook.publishDate || '未知' }}
          </el-descriptions-item>
          <el-descriptions-item label="库存数量">
            {{ getStock(selectedBook) }}
          </el-descriptions-item>
          <el-descriptions-item label="馆藏位置">
            {{ selectedBook.location || '未知' }}
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="selectedBook.description" class="detail-description">
          <h4>内容简介</h4>
          <p>{{ selectedBook.description }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Search,
  View,
  Edit,
  Delete
} from '@element-plus/icons-vue'
import * as booksApi from '@/api/books'

const loading = ref(false)
const submitLoading = ref(false)
const books = ref([])
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const isEdit = ref(false)
const selectedBook = ref(null)
const bookFormRef = ref(null)

const pagination = reactive({
  total: 0,
  pages: 0
})

const searchForm = reactive({
  keyword: '',
  category: '',
  status: '',
  page: 1,
  perPage: 20
})

const bookForm = reactive({
  id: null,
  title: '',
  author: '',
  category: '',
  isbn: '',
  publisher: '',
  pubDate: '',
  stock: 1,
  price: 0,
  status: 'available',
  language: '中文',
  location: '',
  coverUrl: '',
  description: ''
})

const bookRules = {
  title: [{ required: true, message: '请输入书名', trigger: 'blur' }],
  author: [{ required: true, message: '请输入作者', trigger: 'blur' }],
  isbn: [{ required: true, message: '请输入ISBN', trigger: 'blur' }]
}

onMounted(() => {
  loadBooks()
})

async function loadBooks() {
  loading.value = true
  try {
    const params = {
      page: searchForm.page,
      perPage: searchForm.perPage
    }

    if (searchForm.keyword) {
      params.search = searchForm.keyword
    }
    if (searchForm.category) {
      params.category = searchForm.category
    }
    if (searchForm.status) {
      params.status = searchForm.status
    }

    const response = await booksApi.getBooks(params)
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

function resetSearch() {
  searchForm.keyword = ''
  searchForm.category = ''
  searchForm.status = ''
  searchForm.page = 1
  loadBooks()
}

function getCoverImage(book) {
  return book.cover_url || book.coverImage || book.coverUrl || `/static/images/${book.title}.jpg`
}

function getStock(book) {
  return book?.availableCopies ?? book?.available_copies ?? book?.stock ?? 0
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

function openAddModal() {
  isEdit.value = false
  selectedBook.value = null
  resetBookForm()
  dialogVisible.value = true
}

function editBook(book) {
  isEdit.value = true
  selectedBook.value = book
  bookForm.id = book.id
  bookForm.title = book.title
  bookForm.author = book.author
  bookForm.category = book.category || ''
  bookForm.isbn = book.isbn
  bookForm.publisher = book.publisher || ''
  bookForm.pubDate = book.publicationDate || book.pub_date || book.publishDate || ''
  bookForm.stock = book.totalCopies ?? book.total_copies ?? book.stock ?? book.available_copies ?? 0
  bookForm.price = book.price || 0
  bookForm.status = book.status
  bookForm.language = book.language || '中文'
  bookForm.location = book.location || ''
  bookForm.coverUrl = book.coverUrl || book.cover_url || book.coverImage || ''
  bookForm.description = book.description || book.summary || ''
  dialogVisible.value = true
}

function viewBook(book) {
  selectedBook.value = book
  detailDialogVisible.value = true
}

async function deleteBook(book) {
  await ElMessageBox.confirm(
    `确定要删除《${book.title}》吗？此操作不可恢复。`,
    '确认删除',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  try {
    await booksApi.deleteBook(book.id)
    ElMessage.success('删除成功')
    loadBooks()
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function resetBookForm() {
  bookForm.id = null
  bookForm.title = ''
  bookForm.author = ''
  bookForm.category = ''
  bookForm.isbn = ''
  bookForm.publisher = ''
  bookForm.pubDate = ''
  bookForm.stock = 1
  bookForm.price = 0
  bookForm.status = 'available'
  bookForm.language = '中文'
  bookForm.location = ''
  bookForm.coverUrl = ''
  bookForm.description = ''
}

async function submitBookForm() {
  if (!bookFormRef.value) return

  await bookFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const categoryMap = { '文学': 1, '历史': 2, '科技': 3, '艺术': 4, '教育': 5, '其他': 6 }

      const bookData = {
        title: bookForm.title,
        author: bookForm.author,
        categoryId: categoryMap[bookForm.category] || 6,
        price: bookForm.price || 1,
        summary: bookForm.description || null,
        publicationDate: bookForm.pubDate || null,
        coverImage: bookForm.coverUrl || null,
        status: bookForm.status,
        language: bookForm.language || null,
        description: bookForm.description || null,
        location: bookForm.location || null,
        totalCopies: bookForm.stock ?? 1
      }

      if (isEdit.value && bookForm.id) {
        await booksApi.updateBook(bookForm.id, bookData)
      } else {
        await booksApi.addBook({
          ...bookData,
          isbn: bookForm.isbn
        })
      }

      ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
      dialogVisible.value = false
      loadBooks()
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}
</script>

<style scoped>
.books-management {
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-info .page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.header-info .page-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.filter-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
}

.search-bar {
  margin-bottom: 16px;
}

.filter-fields {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.table-card {
  background: white;
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.table-header {
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
}

.table-info {
  font-size: 14px;
  color: var(--text-secondary);
}

.table-info .highlight {
  color: var(--primary-blue);
  font-weight: 700;
  font-size: 18px;
  margin: 0 4px;
}

.pagination-wrapper {
  padding: 16px 20px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-color);
}

.book-cover-small {
  width: 60px;
  height: 80px;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
}

.book-title-cell {
  display: flex;
  flex-direction: column;
}

.title-text {
  font-weight: 600;
  color: var(--text-primary);
}

.text-danger {
  color: var(--danger-red);
  font-weight: 600;
}

.book-form {
  padding: 16px 0;
}

.book-detail {
  padding: 8px 0;
}

.detail-header {
  display: flex;
  gap: 24px;
}

.detail-cover {
  width: 120px;
  height: 160px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.detail-info h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.detail-info .author {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 12px 0;
}

.detail-description {
  margin-top: 20px;
}

.detail-description h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.detail-description p {
  color: var(--text-secondary);
  line-height: 1.8;
  margin: 0;
}
</style>
