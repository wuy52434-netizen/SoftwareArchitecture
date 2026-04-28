<template>
  <div class="kiosk-return">
    <div class="page-header">
      <el-button size="large" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <div class="header-title">
        <h2>归还图书</h2>
        <p>请将图书放置在扫码区域</p>
      </div>
    </div>

    <div class="return-content">
      <div class="scan-area">
        <div class="scan-placeholder" v-if="!currentBorrow">
          <div class="scan-icon">
            <el-icon><Camera /></el-icon>
          </div>
          <h3>扫描图书条码</h3>
          <p>将图书放在扫码区，或手动输入ISBN/借阅号</p>
          <el-input
            v-model="searchInput"
            placeholder="请输入ISBN或借阅号"
            size="large"
            class="manual-input"
            @keyup.enter="searchForReturn"
          >
            <template #append>
              <el-button type="primary" @click="searchForReturn">
                查询
              </el-button>
            </template>
          </el-input>
          <div class="scan-hint">
            <p>📖 支持一次归还多本图书</p>
            <p>⚠️ 逾期图书将产生罚款</p>
          </div>
        </div>

        <div class="borrow-preview" v-else>
          <div
            class="book-cover"
            :style="{ backgroundImage: `url(${getCoverImage(currentBorrow)})` }"
          ></div>
          <div class="borrow-info">
            <h3>{{ currentBorrow.book_title }}</h3>
            <p class="author">{{ currentBorrow.book_author }}</p>
            <div class="borrow-dates">
              <div class="date-item">
                <span class="date-label">借阅日期</span>
                <span class="date-value">{{ formatDate(currentBorrow.borrow_date) }}</span>
              </div>
              <div class="date-item">
                <span class="date-label">应还日期</span>
                <span class="date-value" :class="{ 'text-danger': isOverdue(currentBorrow) }">
                  {{ formatDate(currentBorrow.due_date) }}
                </span>
              </div>
              <div class="date-item" v-if="isOverdue(currentBorrow)">
                <span class="date-label">逾期天数</span>
                <span class="date-value text-danger">
                  {{ getOverdueDays(currentBorrow) }} 天
                </span>
              </div>
            </div>
            <div class="fine-info" v-if="isOverdue(currentBorrow)">
              <el-alert
                :title="`预计罚款：¥${calculateFine(currentBorrow).toFixed(2)}`"
                type="warning"
                show-icon
                :closable="false"
              >
                <template #default>
                  <p>逾期每天罚款 ¥0.5，最高罚款 ¥50</p>
                </template>
              </el-alert>
            </div>
          </div>
          <div class="borrow-actions">
            <el-button type="primary" size="large" @click="addToReturnList">
              <el-icon><Plus /></el-icon>
              添加到归还列表
            </el-button>
            <el-button size="large" @click="clearCurrentBorrow">
              清除
            </el-button>
          </div>
        </div>
      </div>

      <div class="return-list">
        <div class="list-header">
          <h3>待归还列表</h3>
          <span class="book-count">{{ returnList.length }} 本</span>
        </div>

        <div class="list-content" v-if="returnList.length > 0">
          <div
            v-for="(item, index) in returnList"
            :key="index"
            class="list-item"
            :class="{ 'item-overdue': isOverdue(item) }"
          >
            <div class="item-index">{{ index + 1 }}</div>
            <div
              class="item-cover"
              :style="{ backgroundImage: `url(${getCoverImage(item)})` }"
            ></div>
            <div class="item-info">
              <h4>{{ item.book_title }}</h4>
              <p>{{ item.book_author }}</p>
              <div class="item-meta">
                <span v-if="isOverdue(item)" class="overdue-badge">
                  逾期 {{ getOverdueDays(item) }} 天
                </span>
                <span v-else class="normal-badge">正常</span>
              </div>
            </div>
            <el-button type="danger" text @click="removeFromReturnList(index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="list-empty" v-else>
          <el-empty description="请扫描或查询要归还的图书" />
        </div>

        <div class="list-footer" v-if="returnList.length > 0">
          <div class="return-summary">
            <p>共 <span class="highlight">{{ returnList.length }}</span> 本图书</p>
            <p v-if="totalFine > 0" class="fine-text">
              预计罚款：<span class="highlight fine-amount">¥{{ totalFine.toFixed(2) }}</span>
            </p>
          </div>
          <el-button type="primary" size="large" @click="confirmReturn">
            <el-icon><CircleCheck /></el-icon>
            确认归还
          </el-button>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="successDialogVisible"
      title="归还成功"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="success-content">
        <div class="success-icon">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <h3>归还成功！</h3>
        <p>您已成功归还 <span class="highlight">{{ returnList.length }}</span> 本图书</p>
        <div class="return-detail" v-if="totalFine > 0">
          <el-alert
            :title="`逾期罚款：¥${totalFine.toFixed(2)}`"
            type="warning"
            show-icon
            :closable="false"
          >
            <template #default>
              <p>请前往服务台缴纳罚款</p>
            </template>
          </el-alert>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" size="large" @click="continueReturn">
          继续归还
        </el-button>
        <el-button size="large" @click="finishReturn">
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
import * as borrowsApi from '@/api/borrows'
import * as booksApi from '@/api/books'
import * as settingsApi from '@/api/settings'

const router = useRouter()

const searchInput = ref('')
const currentBorrow = ref(null)
const returnList = ref([])
const successDialogVisible = ref(false)
const returnLoading = ref(false)

let settings = {
  overdue_fine_per_day: 0.5,
  max_fine_amount: 50
}

const totalFine = computed(() => {
  return returnList.value.reduce((sum, item) => {
    return sum + calculateFine(item)
  }, 0)
})

onMounted(async () => {
  await loadSettings()
})

async function loadSettings() {
  try {
    const response = await settingsApi.getSettings()
    if (response.ok && response.settings) {
      settings.overdue_fine_per_day = parseFloat(response.settings.overdue_fine_per_day) || 0.5
      settings.max_fine_amount = parseFloat(response.settings.max_fine_amount) || 50
    }
  } catch (error) {
    console.error('加载系统设置失败:', error)
  }
}

function goBack() {
  router.push('/kiosk')
}

function getCoverImage(item) {
  return item.book_cover_url || `/static/images/${item.book_title}.jpg`
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

function isOverdue(item) {
  if (!item.due_date) return false
  const dueDate = new Date(item.due_date)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return today > dueDate
}

function getOverdueDays(item) {
  if (!item.due_date) return 0
  const dueDate = new Date(item.due_date)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const diff = Math.ceil((today - dueDate) / (1000 * 60 * 60 * 24))
  return diff > 0 ? diff : 0
}

function calculateFine(item) {
  const overdueDays = getOverdueDays(item)
  const fine = overdueDays * settings.overdue_fine_per_day
  return Math.min(fine, settings.max_fine_amount)
}

async function searchForReturn() {
  if (!searchInput.value.trim()) {
    ElMessage.warning('请输入ISBN或借阅号')
    return
  }

  try {
    const borrowsResponse = await borrowsApi.getAllBorrows()
    const allBorrows = borrowsResponse.borrows || []
    
    const activeBorrows = allBorrows.filter(b => !b.return_date)
    
    const match = activeBorrows.find(b => 
      b.book_isbn === searchInput.value.trim() || 
      b.id === parseInt(searchInput.value) ||
      b.book_title.toLowerCase().includes(searchInput.value.toLowerCase())
    )

    if (match) {
      currentBorrow.value = match
      searchInput.value = ''
    } else {
      const booksResponse = await booksApi.getBooks({ search: searchInput.value.trim(), perPage: 10 })
      if (booksResponse.books && booksResponse.books.length > 0) {
        const book = booksResponse.books[0]
        const bookBorrow = activeBorrows.find(b => b.book_id === book.id)
        
        if (bookBorrow) {
          currentBorrow.value = bookBorrow
          searchInput.value = ''
        } else {
          ElMessage.warning('该图书没有在借记录')
        }
      } else {
        ElMessage.warning('未找到相关借阅记录')
      }
    }
  } catch (error) {
    ElMessage.error('查询失败')
    console.error('查询失败:', error)
  }
}

function addToReturnList() {
  if (!currentBorrow.value) return

  const exists = returnList.value.some(
    b => b.id === currentBorrow.value.id
  )

  if (exists) {
    ElMessage.warning('该图书已添加到归还列表')
    return
  }

  if (returnList.value.length >= 10) {
    ElMessage.warning('一次最多归还10本图书')
    return
  }

  returnList.value.push({ ...currentBorrow.value })
  clearCurrentBorrow()
  ElMessage.success('已添加到归还列表')
}

function clearCurrentBorrow() {
  currentBorrow.value = null
  searchInput.value = ''
}

function removeFromReturnList(index) {
  returnList.value.splice(index, 1)
}

async function confirmReturn() {
  if (returnList.value.length === 0) {
    ElMessage.warning('请先添加要归还的图书')
    return
  }

  returnLoading.value = true
  try {
    for (const item of returnList.value) {
      await borrowsApi.returnBook({
        borrow_id: item.id
      })
    }

    returnLoading.value = false
    successDialogVisible.value = true
  } catch (error) {
    returnLoading.value = false
    ElMessage.error(error.message || '归还失败，请重试')
  }
}

function continueReturn() {
  successDialogVisible.value = false
  returnList.value = []
  clearCurrentBorrow()
}

function finishReturn() {
  successDialogVisible.value = false
  router.push('/kiosk')
}
</script>

<style scoped>
.kiosk-return {
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

.return-content {
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

.borrow-preview {
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

.borrow-info h3 {
  font-size: 24px;
  font-weight: 700;
  color: #1e3a5f;
  margin: 0 0 12px 0;
}

.borrow-info .author {
  font-size: 16px;
  color: #475569;
  margin: 0 0 16px 0;
}

.borrow-dates {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.date-item {
  background: #f8fafc;
  padding: 12px;
  border-radius: 8px;
}

.date-label {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}

.date-value {
  font-size: 14px;
  font-weight: 600;
  color: #1e3a5f;
}

.date-value.text-danger {
  color: #b91c1c;
}

.fine-info {
  margin-bottom: 20px;
}

.borrow-actions {
  display: flex;
  gap: 12px;
}

.return-list {
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
  border: 2px solid transparent;
}

.list-item.item-overdue {
  background: #fef2f2;
  border-color: #fecaca;
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
  margin: 0 0 4px 0;
}

.item-meta {
  display: flex;
  gap: 8px;
}

.overdue-badge {
  background: #fee2e2;
  color: #b91c1c;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
}

.normal-badge {
  background: #d1fae5;
  color: #059669;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
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

.return-summary {
  text-align: center;
  margin-bottom: 16px;
}

.return-summary p {
  font-size: 16px;
  color: #64748b;
  margin: 4px 0;
}

.return-summary .highlight {
  font-size: 24px;
  font-weight: 700;
  color: #1d4ed8;
  margin: 0 4px;
}

.return-summary .fine-text {
  color: #c2410c;
}

.return-summary .fine-amount {
  color: #b91c1c;
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

@media (max-width: 1024px) {
  .return-content {
    grid-template-columns: 1fr;
  }
  
  .borrow-preview {
    grid-template-columns: 1fr;
    text-align: center;
  }
  
  .book-cover {
    margin: 0 auto;
  }
  
  .borrow-dates {
    grid-template-columns: 1fr;
  }
  
  .borrow-actions {
    justify-content: center;
  }
}
</style>
