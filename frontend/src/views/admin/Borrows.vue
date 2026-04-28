<template>
  <div class="borrows-management">
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">借阅记录</h1>
        <p class="page-subtitle">管理所有图书的借阅和归还记录</p>
      </div>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon total">
          <el-icon><Document /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.total }}</div>
          <div class="stat-label">总借阅数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon active">
          <el-icon><Loading /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.active }}</div>
          <div class="stat-label">借阅中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon overdue">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number overdue-number">{{ stats.overdue }}</div>
          <div class="stat-label">已逾期</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon returned">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ stats.returned }}</div>
          <div class="stat-label">已归还</div>
        </div>
      </div>
    </div>

    <div class="filter-section">
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索书名、作者、读者姓名或读者证号..."
          size="large"
          clearable
          @keyup.enter="loadBorrows"
          @clear="loadBorrows"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="filter-fields">
        <el-select
          v-model="searchForm.bookId"
          placeholder="筛选图书"
          size="large"
          clearable
          @change="loadBorrows"
          filterable
        >
          <el-option
            v-for="book in bookOptions"
            :key="book.id"
            :label="book.title"
            :value="book.id"
          />
        </el-select>
        <el-select
          v-model="searchForm.status"
          placeholder="状态筛选"
          size="large"
          clearable
          @change="loadBorrows"
        >
          <el-option label="全部" value="" />
          <el-option label="借阅中" value="active" />
          <el-option label="已归还" value="returned" />
          <el-option label="已逾期" value="overdue" />
        </el-select>
        <el-button type="primary" size="large" @click="loadBorrows">
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
          共 <span class="highlight">{{ pagination.total }}</span> 条记录
        </div>
      </div>
      <el-table
        :data="borrows"
        v-loading="loading"
        style="width: 100%"
        :row-key="(row) => row.id"
        :stripe="true"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="图书信息" min-width="200">
          <template #default="{ row }">
            <div class="book-info-cell">
              <div
                class="book-cover-small"
                :style="{ backgroundImage: `url(${getCoverImage(row)})` }"
              ></div>
              <div class="book-details">
                <div class="book-title">{{ row.book_title }}</div>
                <div class="book-author">{{ row.book_author }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="读者信息" width="160">
          <template #default="{ row }">
            <div class="reader-info">
              <div class="reader-name">{{ row.reader_name }}</div>
              <div class="reader-id">证号: {{ row.reader_id }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="借阅日期" width="120" align="center">
          <template #default="{ row }">
            {{ formatDate(row.borrow_date) }}
          </template>
        </el-table-column>
        <el-table-column label="应还日期" width="120" align="center">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row) && !row.return_date }">
              {{ formatDate(row.due_date) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="归还日期" width="120" align="center">
          <template #default="{ row }">
            {{ row.return_date ? formatDate(row.return_date) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="逾期天数" width="100" align="center">
          <template #default="{ row }">
            <span v-if="isOverdue(row) && !row.return_date" class="text-danger">
              {{ getOverdueDays(row) }} 天
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row)" size="small">
              {{ getStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="viewDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button
              v-if="!row.return_date"
              type="success"
              text
              size="small"
              @click="processReturn(row)"
            >
              <el-icon><CircleCheck /></el-icon>
              归还
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
          @current-change="loadBorrows"
          @size-change="loadBorrows"
        />
      </div>
    </div>

    <el-dialog
      v-model="detailDialogVisible"
      title="借阅详情"
      width="600px"
    >
      <div v-if="selectedBorrow" class="borrow-detail">
        <div class="detail-header">
          <div
            class="detail-cover"
            :style="{ backgroundImage: `url(${getCoverImage(selectedBorrow)})` }"
          ></div>
          <div class="detail-info">
            <h2>{{ selectedBorrow.book_title }}</h2>
            <p class="author">{{ selectedBorrow.book_author }}</p>
            <el-tag :type="getStatusTagType(selectedBorrow)" size="large">
              {{ getStatusText(selectedBorrow) }}
            </el-tag>
          </div>
        </div>
        <el-divider />
        <el-descriptions :column="2" border>
          <el-descriptions-item label="借阅ID">{{ selectedBorrow.id }}</el-descriptions-item>
          <el-descriptions-item label="图书ISBN">{{ selectedBorrow.book_isbn || '-' }}</el-descriptions-item>
          <el-descriptions-item label="读者姓名">{{ selectedBorrow.reader_name }}</el-descriptions-item>
          <el-descriptions-item label="读者证号">{{ selectedBorrow.reader_id }}</el-descriptions-item>
          <el-descriptions-item label="借阅日期">
            {{ formatDate(selectedBorrow.borrow_date) }}
          </el-descriptions-item>
          <el-descriptions-item label="应还日期">
            {{ formatDate(selectedBorrow.due_date) }}
          </el-descriptions-item>
          <el-descriptions-item label="归还日期">
            {{ selectedBorrow.return_date ? formatDate(selectedBorrow.return_date) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="逾期天数">
            <span v-if="isOverdue(selectedBorrow) && !selectedBorrow.return_date" class="text-danger">
              {{ getOverdueDays(selectedBorrow) }} 天
            </span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="预计罚款" v-if="isOverdue(selectedBorrow) && !selectedBorrow.return_date">
            <span class="text-warning">¥{{ calculateFine(selectedBorrow).toFixed(2) }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <div v-if="selectedBorrow.note" class="detail-note">
          <h4>备注</h4>
          <p>{{ selectedBorrow.note }}</p>
        </div>
      </div>
      <template #footer v-if="selectedBorrow && !selectedBorrow.return_date">
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="processReturn(selectedBorrow)">
          办理归还
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="returnDialogVisible"
      title="办理归还"
      width="500px"
    >
      <div v-if="returningBorrow" class="return-confirm">
        <div class="return-book-info">
          <div
            class="return-cover"
            :style="{ backgroundImage: `url(${getCoverImage(returningBorrow)})` }"
          ></div>
          <div class="return-details">
            <h3>{{ returningBorrow.book_title }}</h3>
            <p>读者：{{ returningBorrow.reader_name }}</p>
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
            <span class="summary-label">应缴罚款</span>
            <span class="summary-value text-warning">¥{{ calculateFine(returningBorrow).toFixed(2) }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="returnDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="returnLoading" @click="confirmReturn">
          确认归还
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Loading,
  Warning,
  CircleCheck,
  Search,
  View
} from '@element-plus/icons-vue'
import * as borrowsApi from '@/api/borrows'
import * as booksApi from '@/api/books'
import * as settingsApi from '@/api/settings'

const loading = ref(false)
const returnLoading = ref(false)
const borrows = ref([])
const bookOptions = ref([])
const detailDialogVisible = ref(false)
const returnDialogVisible = ref(false)
const selectedBorrow = ref(null)
const returningBorrow = ref(null)

let settings = {
  overdue_fine_per_day: 0.5,
  max_fine_amount: 50
}

const pagination = reactive({
  total: 0,
  pages: 0
})

const searchForm = reactive({
  keyword: '',
  bookId: null,
  status: '',
  page: 1,
  perPage: 20
})

const stats = computed(() => {
  const allBorrows = borrows.value
  const total = pagination.total || 0
  const active = allBorrows.filter(b => !b.return_date).length
  const overdue = allBorrows.filter(b => !b.return_date && isOverdue(b)).length
  const returned = allBorrows.filter(b => b.return_date).length
  return { total, active, overdue, returned }
})

onMounted(async () => {
  await loadSettings()
  await loadBooks()
  loadBorrows()
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

async function loadBooks() {
  try {
    const response = await booksApi.getBooks({ perPage: 100 })
    bookOptions.value = response.books || []
  } catch (error) {
    console.error('加载图书列表失败:', error)
  }
}

async function loadBorrows() {
  loading.value = true
  try {
    const response = await borrowsApi.getAllBorrows()
    let data = response.borrows || []

    if (searchForm.keyword) {
      const keyword = searchForm.keyword.toLowerCase()
      data = data.filter(b =>
        b.book_title?.toLowerCase().includes(keyword) ||
        b.book_author?.toLowerCase().includes(keyword) ||
        b.reader_name?.toLowerCase().includes(keyword) ||
        b.reader_id?.toLowerCase().includes(keyword)
      )
    }

    if (searchForm.bookId) {
      data = data.filter(b => b.book_id === searchForm.bookId)
    }

    if (searchForm.status) {
      switch (searchForm.status) {
        case 'active':
          data = data.filter(b => !b.return_date)
          break
        case 'returned':
          data = data.filter(b => b.return_date)
          break
        case 'overdue':
          data = data.filter(b => !b.return_date && isOverdue(b))
          break
      }
    }

    pagination.total = data.length

    const start = (searchForm.page - 1) * searchForm.perPage
    const end = start + searchForm.perPage
    borrows.value = data.slice(start, end)
  } catch (error) {
    ElMessage.error('加载借阅记录失败')
    console.error('加载借阅记录失败:', error)
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.keyword = ''
  searchForm.bookId = null
  searchForm.status = ''
  searchForm.page = 1
  loadBorrows()
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

function getStatusText(borrow) {
  if (borrow.return_date) return '已归还'
  if (isOverdue(borrow)) return '已逾期'
  return '借阅中'
}

function getStatusTagType(borrow) {
  if (borrow.return_date) return 'info'
  if (isOverdue(borrow)) return 'danger'
  return 'success'
}

function viewDetail(borrow) {
  selectedBorrow.value = borrow
  detailDialogVisible.value = true
}

function processReturn(borrow) {
  returningBorrow.value = borrow
  detailDialogVisible.value = false
  returnDialogVisible.value = true
}

async function confirmReturn() {
  if (!returningBorrow.value) return

  const borrow = returningBorrow.value
  let message = '确定要办理归还吗？'
  if (isOverdue(borrow)) {
    message = `该图书已逾期 ${getOverdueDays(borrow)} 天，预计罚款 ¥${calculateFine(borrow).toFixed(2)}。确定要办理归还吗？`
  }

  await ElMessageBox.confirm(message, '确认归还', {
    confirmButtonText: '确认归还',
    cancelButtonText: '取消',
    type: 'warning'
  })

  returnLoading.value = true
  try {
    const response = await borrowsApi.returnBook({
      borrow_id: borrow.id
    })

    if (response.ok) {
      let successMessage = '归还成功！'
      if (response.overdue_days > 0) {
        successMessage = `归还成功！逾期 ${response.overdue_days} 天，罚款 ¥${response.fine_amount.toFixed(2)}`
      }
      ElMessage.success(successMessage)
      returnDialogVisible.value = false
      loadBorrows()
    } else {
      throw new Error(response.error || '归还失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '归还失败')
  } finally {
    returnLoading.value = false
  }
}
</script>

<style scoped>
.borrows-management {
  height: 100%;
}

.page-header {
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

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: var(--shadow-sm);
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

.stat-icon.overdue {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #b91c1c;
}

.stat-icon.returned {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  color: #3730a3;
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
  margin-top: 4px;
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
  width: 50px;
  height: 66px;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
}

.book-info-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.book-details {
  display: flex;
  flex-direction: column;
}

.book-title {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
}

.book-author {
  font-size: 12px;
  color: var(--text-secondary);
}

.reader-info {
  display: flex;
  flex-direction: column;
}

.reader-name {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
}

.reader-id {
  font-size: 12px;
  color: var(--text-secondary);
}

.text-danger {
  color: var(--danger-red);
  font-weight: 600;
}

.text-warning {
  color: var(--warning-yellow);
  font-weight: 600;
}

.borrow-detail {
  padding: 8px 0;
}

.detail-header {
  display: flex;
  gap: 24px;
}

.detail-cover {
  width: 100px;
  height: 133px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}

.detail-info h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.detail-info .author {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0 0 12px 0;
}

.detail-note {
  margin-top: 20px;
}

.detail-note h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.detail-note p {
  color: var(--text-secondary);
  line-height: 1.8;
  margin: 0;
}

.return-confirm {
  padding: 8px 0;
}

.return-book-info {
  display: flex;
  gap: 16px;
  align-items: center;
}

.return-cover {
  width: 80px;
  height: 106px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  background-size: cover;
  background-position: center;
}

.return-details h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.return-details p {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
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

@media (max-width: 1200px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
}
</style>
