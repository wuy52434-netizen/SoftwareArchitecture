<template>
  <div class="my-borrows-page">
    <div class="page-header">
      <h1 class="page-title">
        <el-icon><Document /></el-icon>
        我的借阅
      </h1>
      <p class="page-subtitle">查看和管理您的借阅记录</p>
    </div>

    <div class="borrow-stats">
      <div class="stat-card">
        <div class="stat-icon total">
          <el-icon><Collection /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ borrows.length }}</div>
          <div class="stat-label">总借阅次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon active">
          <el-icon><Loading /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ activeBorrows.length }}</div>
          <div class="stat-label">借阅中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon returned">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ returnedBorrows.length }}</div>
          <div class="stat-label">已归还</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon overdue">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number overdue-number">{{ overdueBorrows.length }}</div>
          <div class="stat-label">已逾期</div>
        </div>
      </div>
    </div>

    <div class="filter-tabs">
      <el-radio-group v-model="currentTab" size="large">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="active">借阅中</el-radio-button>
        <el-radio-button value="returned">已归还</el-radio-button>
        <el-radio-button value="overdue">已逾期</el-radio-button>
      </el-radio-group>
    </div>

    <div class="borrows-list" v-loading="loading">
      <div
        v-for="borrow in filteredBorrows"
        :key="borrow.id"
        class="borrow-record"
        :class="{ 'is-overdue': isOverdue(borrow) && !borrow.return_date }"
      >
        <div
          class="book-cover"
          :style="{ backgroundImage: `url(${getCoverImage(borrow)})` }"
        ></div>
        <div class="borrow-info">
          <div class="book-header">
            <h3 class="book-title">{{ borrow.book_title }}</h3>
            <el-tag :type="getBorrowStatusType(borrow)" size="large">
              {{ getBorrowStatusText(borrow) }}
            </el-tag>
          </div>
          <p class="book-author">{{ borrow.book_author }}</p>
          <div class="borrow-details">
            <div class="detail-item">
              <span class="detail-label">借阅日期</span>
              <span class="detail-value">{{ formatDate(borrow.borrow_date) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">应还日期</span>
              <span class="detail-value" :class="{ 'text-danger': isOverdue(borrow) && !borrow.return_date }">
                {{ formatDate(borrow.due_date) }}
              </span>
            </div>
            <div class="detail-item" v-if="borrow.return_date">
              <span class="detail-label">归还日期</span>
              <span class="detail-value">{{ formatDate(borrow.return_date) }}</span>
            </div>
            <div class="detail-item" v-if="isOverdue(borrow) && !borrow.return_date">
              <span class="detail-label">逾期天数</span>
              <span class="detail-value text-danger">
                {{ getOverdueDays(borrow) }} 天
              </span>
            </div>
          </div>
          <div class="borrow-actions" v-if="!borrow.return_date">
            <el-button
              type="primary"
              size="large"
              :loading="returningIds.includes(borrow.id)"
              @click="returnBook(borrow)"
            >
              <el-icon><CircleCheck /></el-icon>
              归还图书
            </el-button>
            <el-button
              type="warning"
              size="large"
              plain
              :loading="renewingIds.includes(borrow.id)"
              @click="renewBook(borrow)"
            >
              <el-icon><Refresh /></el-icon>
              续借
            </el-button>
          </div>
        </div>
      </div>

      <div class="empty-state" v-if="!loading && filteredBorrows.length === 0">
        <el-empty :description="getEmptyDescription()" />
      </div>
    </div>

    <el-dialog
      v-model="returnDialogVisible"
      title="归还图书"
      width="400px"
    >
      <div v-if="returningBorrow" class="return-info">
        <div class="book-info-row">
          <div
            class="book-cover-small"
            :style="{ backgroundImage: `url(${getCoverImage(returningBorrow)})` }"
          ></div>
          <div class="book-info-text">
            <h4>{{ returningBorrow.book_title }}</h4>
            <p>{{ returningBorrow.book_author }}</p>
          </div>
        </div>
        
        <el-divider />
        
        <div class="return-summary">
          <div class="summary-item">
            <span class="summary-label">借阅日期</span>
            <span class="summary-value">{{ formatDate(returningBorrow.borrow_date) }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">应还日期</span>
            <span class="summary-value">{{ formatDate(returningBorrow.due_date) }}</span>
          </div>
          <div class="summary-item" v-if="isOverdue(returningBorrow)">
            <span class="summary-label">逾期天数</span>
            <span class="summary-value text-danger">{{ getOverdueDays(returningBorrow) }} 天</span>
          </div>
          <div class="summary-item" v-if="isOverdue(returningBorrow)">
            <span class="summary-label">预计罚款</span>
            <span class="summary-value text-warning">¥{{ calculateFine(returningBorrow).toFixed(2) }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="returning" @click="confirmReturn">
          确认归还
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Document, 
  Collection, 
  Loading, 
  CircleCheck, 
  Warning,
  Refresh
} from '@element-plus/icons-vue'
import * as borrowsApi from '@/api/borrows'
import * as settingsApi from '@/api/settings'

const borrows = ref([])
const loading = ref(false)
const currentTab = ref('all')
const returningBorrow = ref(null)
const returnDialogVisible = ref(false)
const returning = ref(false)
const returningIds = ref([])
const renewingIds = ref([])

let settings = {
  overdue_fine_per_day: 0.5,
  max_fine_amount: 50
}

const activeBorrows = computed(() => 
  borrows.value.filter(b => !b.return_date)
)

const returnedBorrows = computed(() => 
  borrows.value.filter(b => b.return_date)
)

const overdueBorrows = computed(() => 
  activeBorrows.value.filter(b => isOverdue(b))
)

const filteredBorrows = computed(() => {
  switch (currentTab.value) {
    case 'active':
      return activeBorrows.value
    case 'returned':
      return returnedBorrows.value
    case 'overdue':
      return overdueBorrows.value
    default:
      return borrows.value
  }
})

onMounted(async () => {
  await loadSettings()
  await loadMyBorrows()
})

async function loadSettings() {
  try {
    const response = await settingsApi.getSettings()
    if (response) {
      settings.overdue_fine_per_day = parseFloat(response.overdueFinePerDay) || 0.5
      settings.max_fine_amount = parseFloat(response.maxFineAmount) || 50
    }
  } catch (error) {
    console.error('加载系统设置失败:', error)
  }
}

async function loadMyBorrows() {
  loading.value = true
  try {
    const response = await borrowsApi.getMyBorrows()
    const rawRecords = Array.isArray(response) ? response : (response.records || response.borrows || [])
    borrows.value = rawRecords.map(b => ({
      id: b.recordId || b.id,
      book_id: b.bookId || b.book_id,
      book_title: b.bookTitle || b.book_title || '未知图书',
      book_author: b.bookAuthor || b.book_author || '',
      book_cover_url: b.bookCoverUrl || b.book_cover_url,
      borrow_date: b.borrowDate || b.borrow_date,
      due_date: b.dueDate || b.due_date,
      return_date: b.returnDate || b.return_date,
      status: b.status
    }))
  } catch (error) {
    ElMessage.error('加载借阅记录失败')
    console.error('加载借阅记录失败:', error)
  } finally {
    loading.value = false
  }
}

function getCoverImage(borrow) {
  return borrow.book_cover_url || `/static/images/${borrow.book_title}.jpg`
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

function isOverdue(borrow) {
  if (!borrow.due_date) return false
  const dueDate = new Date(borrow.due_date)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return today > dueDate
}

function getOverdueDays(borrow) {
  if (!borrow.due_date) return 0
  const dueDate = new Date(borrow.due_date)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const diff = Math.ceil((today - dueDate) / (1000 * 60 * 60 * 24))
  return diff > 0 ? diff : 0
}

function calculateFine(borrow) {
  const overdueDays = getOverdueDays(borrow)
  const fine = overdueDays * settings.overdue_fine_per_day
  return Math.min(fine, settings.max_fine_amount)
}

function getBorrowStatusType(borrow) {
  if (borrow.return_date) return 'info'
  if (isOverdue(borrow)) return 'danger'
  return 'success'
}

function getBorrowStatusText(borrow) {
  if (borrow.return_date) return '已归还'
  if (isOverdue(borrow)) return '已逾期'
  return '借阅中'
}

function getEmptyDescription() {
  switch (currentTab.value) {
    case 'active':
      return '暂无在借图书'
    case 'returned':
      return '暂无已归还记录'
    case 'overdue':
      return '暂无逾期图书'
    default:
      return '暂无借阅记录'
  }
}

function returnBook(borrow) {
  returningBorrow.value = borrow
  returnDialogVisible.value = true
}

async function confirmReturn() {
  if (!returningBorrow.value) return

  returning.value = true
  returningIds.value.push(returningBorrow.value.id)

  try {
    await borrowsApi.returnBook({
      borrowId: returningBorrow.value.id
    })

    ElMessage.success('归还成功！')
    returnDialogVisible.value = false
    loadMyBorrows()
  } catch (error) {
    ElMessage.error(error.message || '归还失败，请重试')
  } finally {
    returning.value = false
    returningIds.value = returningIds.value.filter(id => id !== returningBorrow.value?.id)
  }
}

async function renewBook(borrow) {
  try {
    await ElMessageBox.confirm(
      `确定要续借《${borrow.book_title}》30 天吗？`,
      '续借确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    renewingIds.value.push(borrow.id)
    await borrowsApi.renewBook(borrow.id, 30)
    ElMessage.success('续借成功')
    await loadMyBorrows()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '续借失败，请重试')
    }
  } finally {
    renewingIds.value = renewingIds.value.filter(id => id !== borrow.id)
  }
}
</script>

<style scoped>
.my-borrows-page {
  background: var(--bg-primary);
  border-radius: 12px;
  padding: 32px;
  box-shadow: var(--shadow-sm);
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--text-secondary);
}

.borrow-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  margin-bottom: 32px;
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
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
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

.stat-icon.total {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
}

.stat-icon.active {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #15803d;
}

.stat-icon.returned {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  color: #3730a3;
}

.stat-icon.overdue {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #b91c1c;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
}

.stat-number.overdue-number {
  color: var(--danger-red);
}

.stat-label {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

.filter-tabs {
  margin-bottom: 24px;
}

.borrows-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.borrow-record {
  display: flex;
  gap: 24px;
  padding: 24px;
  background: white;
  border-radius: 16px;
  border: 2px solid var(--border-color);
  transition: all 0.3s ease;
}

.borrow-record:hover {
  border-color: var(--primary-blue);
  box-shadow: var(--shadow-md);
}

.borrow-record.is-overdue {
  border-color: var(--danger-red);
  background: linear-gradient(135deg, #fef2f2 0%, #fff 100%);
}

.book-cover {
  width: 120px;
  height: 160px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.borrow-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.book-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.book-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0;
}

.book-author {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.borrow-details {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.detail-value {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 600;
}

.detail-value.text-danger {
  color: var(--danger-red);
}

.borrow-actions {
  display: flex;
  gap: 12px;
  margin-top: auto;
  padding-top: 12px;
}

.empty-state {
  padding: 60px 20px;
}

.return-info {
  text-align: center;
}

.book-info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  justify-content: center;
}

.book-cover-small {
  width: 80px;
  height: 110px;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
}

.book-info-text {
  text-align: left;
}

.book-info-text h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.book-info-text p {
  font-size: 14px;
  color: var(--text-secondary);
}

.return-summary {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.summary-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.summary-value.text-danger {
  color: var(--danger-red);
}

.summary-value.text-warning {
  color: var(--warning-yellow);
}

@media (max-width: 1024px) {
  .borrow-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .my-borrows-page {
    padding: 20px;
  }
  
  .borrow-stats {
    grid-template-columns: 1fr;
  }
  
  .borrow-record {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }
  
  .book-cover {
    width: 100%;
    height: 200px;
    align-self: center;
  }
  
  .book-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .borrow-details {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  
  .borrow-actions {
    flex-direction: column;
    width: 100%;
  }
  
  .borrow-actions .el-button {
    width: 100%;
  }
}
</style>
