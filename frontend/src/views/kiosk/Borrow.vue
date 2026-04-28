<template>
  <div class="kiosk-borrow">
    <div class="page-header">
      <el-button size="large" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div class="header-title">
        <h2>借阅图书</h2>
        <p>请将图书放置在扫码区域</p>
      </div>
    </div>

    <div class="borrow-content">
      <div class="scan-area">
        <div class="scan-placeholder" v-if="!currentBook">
          <div class="scan-icon">
            <el-icon><Camera /></el-icon>
          </div>
          <h3>扫描图书条码</h3>
          <p>将图书放在扫码区，或手动输入ISBN</p>
          <el-input
            v-model="isbnInput"
            placeholder="请输入ISBN号"
            size="large"
            class="manual-input"
            @keyup.enter="searchByISBN"
          >
            <template #append>
              <el-button type="primary" @click="searchByISBN">
                查询
              </el-button>
            </template>
          </el-input>
          <div class="scan-hint">
            <p>📖 支持一次借阅多本图书</p>
            <p>🔍 也可以点击下方按钮查询图书</p>
          </div>
        </div>

        <div class="book-preview" v-else>
          <div
            class="book-cover"
            :style="{ backgroundImage: `url(${getCoverImage(currentBook)})` }"
          ></div>
          <div class="book-info">
            <h3>{{ currentBook.title }}</h3>
            <p class="author">{{ currentBook.author }}</p>
            <p class="isbn">ISBN: {{ currentBook.isbn }}</p>
            <p class="publisher">{{ currentBook.publisher || '未知出版社' }}</p>
            <el-tag :type="getStockStatusType(currentBook)" size="large">
              {{ getStockText(currentBook) }}
            </el-tag>
          </div>
          <div class="book-actions">
            <el-button type="primary" size="large" @click="addToCart" :disabled="!isAvailable(currentBook)">
              <el-icon><Plus /></el-icon>
              添加到借阅列表
            </el-button>
            <el-button size="large" @click="clearCurrentBook">
              清除
            </el-button>
          </div>
        </div>
      </div>

      <div class="borrow-list">
        <div class="list-header">
          <h3>待借阅列表</h3>
          <span class="book-count">{{ selectedBooks.length }} 本</span>
        </div>

        <div class="list-content" v-if="selectedBooks.length > 0">
          <div
            v-for="(book, index) in selectedBooks"
            :key="index"
            class="list-item"
          >
            <div class="item-index">{{ index + 1 }}</div>
            <div
              class="item-cover"
              :style="{ backgroundImage: `url(${getCoverImage(book)})` }"
            ></div>
            <div class="item-info">
              <h4>{{ book.title }}</h4>
              <p>{{ book.author }}</p>
            </div>
            <el-button type="danger" text @click="removeBook(index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="list-empty" v-else>
          <el-empty description="请扫描或添加要借阅的图书" />
        </div>

        <div class="list-footer" v-if="selectedBooks.length > 0">
          <div class="borrow-summary">
            <p>共 <span class="highlight">{{ selectedBooks.length }}</span> 本图书</p>
          </div>
          <el-button type="primary" size="large" @click="confirmBorrow">
            <el-icon><CircleCheck /></el-icon>
            确认借阅
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="readerDialogVisible"
      title="读者身份验证"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="readerFormRef"
        :model="readerForm"
        :rules="readerRules"
        label-width="100px"
        class="reader-form"
      >
        <el-form-item label="读者证号" prop="readerId">
          <el-input
            v-model="readerForm.readerId"
            placeholder="请输入读者证号或刷读者证"
            size="large"
          />
        </el-form-item>
        <el-form-item label="读者姓名" prop="readerName">
          <el-input
            v-model="readerForm.readerName"
            placeholder="请输入读者姓名"
            size="large"
          />
        </el-form-item>
        <el-form-item label="借阅日期">
          <el-input
            :value="formatDate(new Date())"
            disabled
            size="large"
          />
        </el-form-item>
        <el-form-item label="应还日期">
          <el-input
            :value="formatDate(returnDate)"
            disabled
            size="large"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="large" @click="readerDialogVisible = false">取消</el-button>
        <el-button type="primary" size="large" :loading="borrowLoading" @click="submitBorrow">
          确认借阅
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="successDialogVisible"
      title="借阅成功"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="success-content">
        <div class="success-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <h3>借阅成功！</h3>
        <p>您已成功借阅 <span class="highlight">{{ selectedBooks.length }}</span> 本图书</p>
        <div class="borrow-detail">
          <p><strong>借阅日期：</strong>{{ formatDate(new Date()) }}</p>
          <p><strong>应还日期：</strong>{{ formatDate(returnDate) }}</p>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" size="large" @click="continueBorrow">
          继续借阅
        </el-button>
        <el-button size="large" @click="finishBorrow">
          完成
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Camera,
  Plus,
  Delete,
  CircleCheck
} from '@element-plus/icons-vue'
import * as booksApi from '@/api/books'
import * as borrowsApi from '@/api/borrows'
import * as settingsApi from '@/api/settings'

const router = useRouter()

const isbnInput = ref('')
const currentBook = ref(null)
const selectedBooks = ref([])
const readerDialogVisible = ref(false)
const successDialogVisible = ref(false)
const borrowLoading = ref(false)
const readerFormRef = ref(null)

let defaultBorrowDays = 30

const returnDate = computed(() => {
  const date = new Date()
  date.setDate(date.getDate() + defaultBorrowDays)
  return date
})

const readerForm = reactive({
  readerId: '',
  readerName: ''
})

const readerRules = {
  readerId: [{ required: true, message: '请输入读者证号', trigger: 'blur' }],
  readerName: [{ required: true, message: '请输入读者姓名', trigger: 'blur' }]
}

onMounted(async () => {
  await loadSettings()
})

async function loadSettings() {
  try {
    const response = await settingsApi.getSettings()
    if (response.ok && response.settings) {
      defaultBorrowDays = parseInt(response.settings.default_borrow_days) || 30
    }
  } catch (error) {
    console.error('加载系统设置失败:', error)
  }
}

function goBack() {
  router.push('/kiosk')
}

function getCoverImage(book) {
  return book.cover_url || book.coverImage || `/static/images/${book.title}.jpg`
}

function isAvailable(book) {
  return book.status === 'available' && (book.available_copies || book.stock || 0) > 0
}

function getStockStatusType(book) {
  if (!isAvailable(book)) return 'danger'
  return 'success'
}

function getStockText(book) {
  if (!isAvailable(book)) return '不可借阅'
  return `可借阅 (库存: ${book.available_copies || book.stock || 0})`
}

async function searchByISBN() {
  if (!isbnInput.value.trim()) {
    ElMessage.warning('请输入ISBN号')
    return
  }

  try {
    const response = await booksApi.getBooks({ search: isbnInput.value, perPage: 10 })
    if (response.books && response.books.length > 0) {
      currentBook.value = response.books[0]
      isbnInput.value = ''
    } else {
      ElMessage.warning('未找到该ISBN对应的图书')
    }
  } catch (error) {
    ElMessage.error('查询图书失败')
    console.error('查询图书失败:', error)
  }
}

function addToCart() {
  if (!currentBook.value) return

  if (!isAvailable(currentBook.value)) {
    ElMessage.warning('该图书不可借阅')
    return
  }

  const exists = selectedBooks.value.some(
    b => b.id === currentBook.value.id || b.isbn === currentBook.value.isbn
  )

  if (exists) {
    ElMessage.warning('该图书已添加到借阅列表')
    return
  }

  if (selectedBooks.value.length >= 5) {
    ElMessage.warning('一次最多借阅5本图书')
    return
  }

  selectedBooks.value.push({ ...currentBook.value })
  clearCurrentBook()
  ElMessage.success('已添加到借阅列表')
}

function clearCurrentBook() {
  currentBook.value = null
  isbnInput.value = ''
}

function removeBook(index) {
  selectedBooks.value.splice(index, 1)
}

function confirmBorrow() {
  if (selectedBooks.value.length === 0) {
    ElMessage.warning('请先添加要借阅的图书')
    return
  }
  readerDialogVisible.value = true
}

async function submitBorrow() {
  if (!readerFormRef.value) return

  await readerFormRef.value.validate(async (valid) => {
    if (!valid) return

    borrowLoading.value = true
    try {
      for (const book of selectedBooks.value) {
        await borrowsApi.borrowBook({
          book_id: book.id,
          reader_id: readerForm.readerId,
          reader_name: readerForm.readerName,
          note: '自助终端借阅 - 我爱软件工程'
        })
      }

      borrowLoading.value = false
      readerDialogVisible.value = false
      successDialogVisible.value = true
    } catch (error) {
      borrowLoading.value = false
      ElMessage.error(error.message || '借阅失败，请重试')
    }
  })
}

function continueBorrow() {
  successDialogVisible.value = false
  selectedBooks.value = []
  clearCurrentBook()
  readerForm.readerId = ''
  readerForm.readerName = ''
}

function finishBorrow() {
  successDialogVisible.value = false
  router.push('/kiosk')
}

function formatDate(date) {
  return date.toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.kiosk-borrow {
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

.borrow-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 32px;
}

.scan-area {
  background: white;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.scan-placeholder {
  text-align: center;
  padding: 40px;
}

.scan-icon {
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56px;
  color: #1d4ed8;
  margin: 0 auto 24px;
  animation: scan-pulse 2s infinite;
}

@keyframes scan-pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4);
  }
  50% {
    box-shadow: 0 0 0 20px rgba(59, 130, 246, 0);
  }
}

.scan-placeholder h3 {
  font-size: 24px;
  font-weight: 600;
  color: #1e3a5f;
  margin: 0 0 12px 0;
}

.scan-placeholder p {
  font-size: 16px;
  color: #64748b;
  margin: 0 0 32px 0;
}

.manual-input {
  max-width: 400px;
  margin: 0 auto 32px;
}

.scan-hint {
  background: #f1f5f9;
  border-radius: 12px;
  padding: 20px;
}

.scan-hint p {
  margin: 8px 0;
  color: #64748b;
  font-size: 14px;
}

.book-preview {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 32px;
}

.book-cover {
  width: 160px;
  height: 220px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.book-info h3 {
  font-size: 24px;
  font-weight: 700;
  color: #1e3a5f;
  margin: 0 0 12px 0;
}

.book-info .author {
  font-size: 16px;
  color: #475569;
  margin: 0 0 8px 0;
}

.book-info .isbn {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 8px 0;
}

.book-info .publisher {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 16px 0;
}

.book-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.borrow-list {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 2px solid #e2e8f0;
  margin-bottom: 16px;
}

.list-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1e3a5f;
  margin: 0;
}

.book-count {
  background: #dbeafe;
  color: #1d4ed8;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
}

.list-content {
  flex: 1;
  overflow-y: auto;
  max-height: 400px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 12px;
}

.item-index {
  width: 28px;
  height: 28px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.item-cover {
  width: 48px;
  height: 64px;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e3a5f;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-info p {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

.list-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.list-footer {
  padding-top: 16px;
  border-top: 2px solid #e2e8f0;
  margin-top: auto;
}

.borrow-summary {
  text-align: center;
  margin-bottom: 16px;
}

.borrow-summary p {
  font-size: 16px;
  color: #64748b;
}

.borrow-summary .highlight {
  font-size: 24px;
  font-weight: 700;
  color: #1d4ed8;
  margin: 0 4px;
}

.list-footer .el-button {
  width: 100%;
}

.reader-form {
  padding: 16px 0;
}

.success-content {
  text-align: center;
  padding: 24px 0;
}

.success-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #059669;
  margin: 0 auto 24px;
}

.success-content h3 {
  font-size: 24px;
  font-weight: 700;
  color: #1e3a5f;
  margin: 0 0 12px 0;
}

.success-content p {
  font-size: 16px;
  color: #64748b;
  margin: 0 0 24px 0;
}

.success-content .highlight {
  font-size: 28px;
  font-weight: 700;
  color: #059669;
  margin: 0 4px;
}

.borrow-detail {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
  text-align: left;
}

.borrow-detail p {
  margin: 8px 0;
  font-size: 14px;
}

@media (max-width: 1024px) {
  .borrow-content {
    grid-template-columns: 1fr;
  }
  
  .book-preview {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .book-cover {
    margin: 0 auto;
  }
  
  .book-actions {
    justify-content: center;
  }
}
</style>
