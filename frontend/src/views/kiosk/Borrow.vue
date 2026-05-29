<template>
  <div class="kiosk-borrow">
    <div class="page-header">
      <el-button size="large" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div class="header-title">
        <h2>借阅图书</h2>
        <p>{{ stepTitle }}</p>
      </div>
      <div class="reader-badge" v-if="verifiedReader">
        <el-icon><User /></el-icon>
        <span>{{ verifiedReader.realName || verifiedReader.username }}</span>
        <el-button type="danger" text size="small" @click="logout">退出</el-button>
      </div>
    </div>

    <!-- 第一步：身份验证 -->
    <div class="auth-section" v-if="!verifiedReader">
      <div class="auth-card">
        <div class="auth-icon">
          <el-icon><CreditCard /></el-icon>
        </div>
        <h3>请先验证身份</h3>
        <p>请刷读者证或输入读者证号</p>

        <el-form
          ref="authFormRef"
          :model="authForm"
          :rules="authRules"
          class="auth-form"
          @submit.prevent="verifyReader"
        >
          <el-form-item prop="cardNo">
            <el-input
              v-model="authForm.cardNo"
              placeholder="请输入读者证号"
              size="large"
              prefix-icon="User"
              @keyup.enter="verifyReader"
              autofocus
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="verifyLoading"
            @click="verifyReader"
            class="auth-btn"
          >
            验证身份
          </el-button>
        </el-form>

        <div class="auth-hint">
          <p>演示账号：输入 <strong>user1</strong> 或 <strong>2</strong> 即可验证</p>
        </div>
      </div>
    </div>

    <!-- 第二步：扫书借阅 -->
    <div class="borrow-content" v-else>
      <div class="scan-area">
        <div class="reader-info-bar">
          <div class="reader-avatar">
            <el-icon><User /></el-icon>
          </div>
          <div class="reader-detail">
            <h4>{{ verifiedReader.realName || verifiedReader.username }}</h4>
            <p>已借 {{ verifiedReader.currentBorrowCount || 0 }} / {{ verifiedReader.maxBorrowCount || 5 }} 本</p>
          </div>
          <el-tag type="success">已验证</el-tag>
        </div>

        <div class="scan-placeholder" v-if="!currentBook">
          <div class="scan-icon">
            <el-icon><Camera /></el-icon>
          </div>
          <h3>扫描图书条码</h3>
          <p>将图书放在扫码区，或手动输入图书条码/ISBN</p>
          <el-input
            v-model="isbnInput"
            placeholder="请输入图书条码或ISBN号"
            size="large"
            class="manual-input"
            @keyup.enter="searchByCode"
          >
            <template #append>
              <el-button type="primary" @click="searchByCode">
                查询
              </el-button>
            </template>
          </el-input>
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
            <p class="isbn" v-if="currentBook.barcode">副本条码: {{ currentBook.barcode }}</p>
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
              <p v-if="book.barcode">条码：{{ book.barcode }}</p>
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
            <p class="due-date">应还日期：{{ formatDate(returnDate) }}</p>
          </div>
          <el-button type="primary" size="large" :loading="borrowLoading" @click="confirmBorrow">
            <el-icon><CircleCheck /></el-icon>
            确认借阅
          </el-button>
        </div>
      </div>
    </div>

    <!-- 借阅成功弹窗 -->
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
        <p>{{ verifiedReader?.realName || '读者' }} 已成功借阅 <span class="highlight">{{ borrowedCount }}</span> 本图书</p>
        <div class="borrow-detail">
          <p><strong>借阅日期：</strong>{{ formatDate(new Date()) }}</p>
          <p><strong>应还日期：</strong>{{ formatDate(returnDate) }}</p>
          <p><strong>请按时归还，逾期将产生罚金</strong></p>
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
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft,
  Camera,
  Plus,
  Delete,
  CircleCheck,
  User,
  CreditCard
} from '@element-plus/icons-vue'
import * as booksApi from '@/api/books'
import * as borrowsApi from '@/api/borrows'
import api from '@/api/index'

const router = useRouter()

const isbnInput = ref('')
const currentBook = ref(null)
const selectedBooks = ref([])
const successDialogVisible = ref(false)
const borrowLoading = ref(false)
const verifyLoading = ref(false)
const borrowedCount = ref(0)

const verifiedReader = ref(null)
const authFormRef = ref(null)
const authForm = reactive({ cardNo: '' })
const authRules = {
  cardNo: [{ required: true, message: '请输入读者证号', trigger: 'blur' }]
}

const defaultBorrowDays = 30

const returnDate = computed(() => {
  const date = new Date()
  date.setDate(date.getDate() + defaultBorrowDays)
  return date
})

const stepTitle = computed(() => {
  if (!verifiedReader.value) return '第一步：请验证身份'
  return '第二步：请将图书放置在扫码区域'
})

function goBack() {
  router.push('/kiosk')
}

async function verifyReader() {
  if (!authFormRef.value) return
  await authFormRef.value.validate(async (valid) => {
    if (!valid) return

    verifyLoading.value = true
    try {
      const cardNo = authForm.cardNo.trim()
      const user = await api.get(`/users/card/${cardNo}`)

      if (!user) {
        ElMessage.error('读者证号无效，请重新输入')
        verifyLoading.value = false
        return
      }

      if (user.status !== 'active') {
        ElMessage.error('该账户已被冻结，无法借阅')
        verifyLoading.value = false
        return
      }

      verifiedReader.value = user
      ElMessage.success(`欢迎，${user.realName || user.username}！`)
    } catch (error) {
      ElMessage.error('验证失败：读者证号无效或系统异常')
    } finally {
      verifyLoading.value = false
    }
  })
}

function logout() {
  verifiedReader.value = null
  selectedBooks.value = []
  currentBook.value = null
  authForm.cardNo = ''
}

function getCoverImage(book) {
  return book.cover_url || book.coverImage || `/static/images/${book.title}.jpg`
}

function isAvailable(book) {
  return book.status === 'available' && getStock(book) > 0
}

function getStockStatusType(book) {
  if (!isAvailable(book)) return 'danger'
  return 'success'
}

function getStockText(book) {
  if (!isAvailable(book)) return '不可借阅'
  const stock = getStock(book)
  return `可借阅 (库存: ${stock})`
}

function getStock(book) {
  return book?.availableCopies ?? book?.available_copies ?? book?.stock ?? 0
}

async function searchByCode() {
  if (!isbnInput.value.trim()) {
    ElMessage.warning('请输入图书条码或ISBN号')
    return
  }

  try {
    const response = await booksApi.scanBook(isbnInput.value)
    const book = response?.book
    const copy = response?.copy
    if (!book) {
      ElMessage.warning('未找到该图书')
      return
    }
    currentBook.value = {
      ...book,
      copyId: copy?.copyId,
      barcode: copy?.barcode,
      copyStatus: copy?.status,
      scannedBy: response.inputType
    }
    isbnInput.value = ''
  } catch (error) {
    ElMessage.error('查询图书失败')
  }
}

function addToCart() {
  if (!currentBook.value) return

  if (!isAvailable(currentBook.value)) {
    ElMessage.warning('该图书不可借阅')
    return
  }

  const maxCount = verifiedReader.value?.maxBorrowCount || 5
  const currentCount = verifiedReader.value?.currentBorrowCount || 0

  if (selectedBooks.value.length + currentCount >= maxCount) {
    ElMessage.warning(`您最多可借 ${maxCount} 本，当前已借 ${currentCount} 本`)
    return
  }

  const exists = selectedBooks.value.some(b => {
    if (currentBook.value.copyId && b.copyId) return b.copyId === currentBook.value.copyId
    return b.id === currentBook.value.id || b.isbn === currentBook.value.isbn
  })

  if (exists) {
    ElMessage.warning('该图书副本已添加到借阅列表')
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

async function confirmBorrow() {
  if (borrowLoading.value) {
    return
  }
  if (selectedBooks.value.length === 0) {
    ElMessage.warning('请先添加要借阅的图书')
    return
  }

  borrowLoading.value = true
  try {
    for (const book of selectedBooks.value) {
      await borrowsApi.borrowBook({
        bookId: book.id,
        copyId: book.copyId,
        userId: verifiedReader.value.userId
      })
    }

    borrowedCount.value = selectedBooks.value.length
    borrowLoading.value = false
    successDialogVisible.value = true
  } catch (error) {
    borrowLoading.value = false
    ElMessage.error(error.message || '借阅失败，请重试')
  }
}

function continueBorrow() {
  successDialogVisible.value = false
  selectedBooks.value = []
  clearCurrentBook()
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

.reader-badge {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #ecfdf5;
  padding: 8px 16px;
  border-radius: 20px;
  color: #059669;
  font-weight: 600;
}

/* 身份验证区域 */
.auth-section {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 500px;
}

.auth-card {
  background: white;
  border-radius: 24px;
  padding: 60px 80px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  text-align: center;
  max-width: 500px;
  width: 100%;
}

.auth-icon {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #1d4ed8;
  margin: 0 auto 24px;
}

.auth-card h3 {
  font-size: 24px;
  font-weight: 700;
  color: #1e3a5f;
  margin: 0 0 8px 0;
}

.auth-card > p {
  font-size: 16px;
  color: #64748b;
  margin: 0 0 32px 0;
}

.auth-form {
  max-width: 320px;
  margin: 0 auto;
}

.auth-btn {
  width: 100%;
  margin-top: 8px;
}

.auth-hint {
  margin-top: 24px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.auth-hint p {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

/* 读者信息条 */
.reader-info-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f0fdf4;
  border-radius: 12px;
  margin-bottom: 24px;
}

.reader-avatar {
  width: 40px;
  height: 40px;
  background: #059669;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
}

.reader-detail h4 {
  margin: 0;
  font-size: 15px;
  color: #1e3a5f;
}

.reader-detail p {
  margin: 2px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

/* 借阅内容区 */
.borrow-content {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 32px;
}

.scan-area {
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.scan-placeholder {
  text-align: center;
  padding: 40px 20px;
}

.scan-icon {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: #1d4ed8;
  margin: 0 auto 20px;
  animation: scan-pulse 2s infinite;
}

@keyframes scan-pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4); }
  50% { box-shadow: 0 0 0 20px rgba(59, 130, 246, 0); }
}

.scan-placeholder h3 {
  font-size: 22px;
  font-weight: 600;
  color: #1e3a5f;
  margin: 0 0 8px 0;
}

.scan-placeholder p {
  font-size: 15px;
  color: #64748b;
  margin: 0 0 24px 0;
}

.manual-input {
  max-width: 400px;
  margin: 0 auto;
}

.book-preview {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: 24px;
}

.book-cover {
  width: 140px;
  height: 200px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.book-info h3 {
  font-size: 22px;
  font-weight: 700;
  color: #1e3a5f;
  margin: 0 0 8px 0;
}

.book-info .author {
  font-size: 15px;
  color: #475569;
  margin: 0 0 6px 0;
}

.book-info .isbn {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 12px 0;
}

.book-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
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
  margin: 4px 0;
}

.borrow-summary .highlight {
  font-size: 24px;
  font-weight: 700;
  color: #1d4ed8;
  margin: 0 4px;
}

.borrow-summary .due-date {
  font-size: 13px;
  color: #94a3b8;
}

.list-footer .el-button {
  width: 100%;
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
