<template>
  <div class="users-management">
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">管理图书馆的所有用户账户</p>
      </div>
      <el-button type="primary" size="large" @click="openAddModal">
        <el-icon><Plus /></el-icon>
        添加用户
      </el-button>
    </div>

    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon total">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ users.length }}</div>
          <div class="stat-label">总用户数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon admin">
          <el-icon><UserFilled /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ adminCount }}</div>
          <div class="stat-label">管理员</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon active">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-number">{{ activeCount }}</div>
          <div class="stat-label">活跃用户</div>
        </div>
      </div>
    </div>

    <div class="filter-section">
      <div class="search-bar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索用户名、姓名或邮箱..."
          size="large"
          clearable
          @keyup.enter="loadUsers"
          @clear="loadUsers"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="filter-fields">
        <el-select
          v-model="searchForm.role"
          placeholder="角色筛选"
          size="large"
          clearable
          @change="loadUsers"
        >
          <el-option label="全部" value="" />
          <el-option label="管理员" value="admin" />
          <el-option label="普通用户" value="user" />
        </el-select>
        <el-button type="primary" size="large" @click="loadUsers">
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
          共 <span class="highlight">{{ users.length }}</span> 位用户
        </div>
      </div>
      <el-table
        :data="filteredUsers"
        v-loading="loading"
        style="width: 100%"
        :row-key="(row) => row.id"
        :stripe="true"
      >
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="用户信息" min-width="180">
          <template #default="{ row }">
            <div class="user-info-cell">
              <el-avatar :size="40" class="user-avatar">
                <el-icon><User /></el-icon>
              </el-avatar>
              <div class="user-details">
                <div class="user-name">{{ row.real_name || row.name || row.username }}</div>
                <div class="user-username">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" width="180">
          <template #default="{ row }">
            {{ row.email || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" size="small">
              {{ row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDate(row.created_at || row.registerTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" size="small">
              {{ row.status === 'active' ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" text size="small" @click="editUser(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button
              v-if="row.role !== 'admin'"
              type="danger"
              text
              size="small"
              @click="deleteUser(row)"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="100px"
        class="user-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" size="large" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="userForm.password"
            :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"
            size="large"
            :type="showPassword ? 'text' : 'password'"
            show-password
          />
        </el-form-item>
        <el-form-item label="姓名" prop="real_name">
          <el-input v-model="userForm.real_name" placeholder="请输入真实姓名" size="large" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" size="large" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="userForm.role" size="large">
            <el-radio value="user">普通用户</el-radio>
            <el-radio value="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="userForm.status" size="large">
            <el-radio value="active">正常</el-radio>
            <el-radio value="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="large" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="large" :loading="submitLoading" @click="submitUserForm">
          {{ isEdit ? '保存修改' : '添加用户' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Search,
  Edit,
  Delete,
  User,
  UserFilled,
  CircleCheck
} from '@element-plus/icons-vue'
import * as usersApi from '@/api/users'

const loading = ref(false)
const submitLoading = ref(false)
const showPassword = ref(false)
const users = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const userFormRef = ref(null)

const pagination = reactive({
  total: 0,
  pages: 0
})

const searchForm = reactive({
  keyword: '',
  role: '',
  page: 1,
  perPage: 50
})

const userForm = reactive({
  id: null,
  username: '',
  password: '',
  real_name: '',
  email: '',
  role: 'user',
  status: 'active'
})

const userRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  real_name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const adminCount = computed(() => {
  return users.value.filter(u => u.role === 'admin').length
})

const activeCount = computed(() => {
  return users.value.filter(u => u.status === 'active' || !u.status).length
})

const filteredUsers = computed(() => {
  let result = [...users.value]

  if (searchForm.keyword) {
    const keyword = searchForm.keyword.toLowerCase()
    result = result.filter(u =>
      u.username?.toLowerCase().includes(keyword) ||
      u.real_name?.toLowerCase().includes(keyword) ||
      u.name?.toLowerCase().includes(keyword) ||
      u.email?.toLowerCase().includes(keyword)
    )
  }

  if (searchForm.role) {
    result = result.filter(u => u.role === searchForm.role)
  }

  return result
})

onMounted(() => {
  loadUsers()
})

async function loadUsers() {
  loading.value = true
  try {
    const response = await usersApi.getUsers()
    users.value = response.users || []
  } catch (error) {
    ElMessage.error('加载用户列表失败')
    console.error('加载用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

function resetSearch() {
  searchForm.keyword = ''
  searchForm.role = ''
  searchForm.page = 1
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function openAddModal() {
  isEdit.value = false
  resetUserForm()
  dialogVisible.value = true
}

function editUser(user) {
  isEdit.value = true
  userForm.id = user.id
  userForm.username = user.username
  userForm.password = ''
  userForm.real_name = user.real_name || user.name || ''
  userForm.email = user.email || ''
  userForm.role = user.role || 'user'
  userForm.status = user.status || 'active'
  dialogVisible.value = true
}

async function deleteUser(user) {
  await ElMessageBox.confirm(
    `确定要删除用户「${user.real_name || user.name || user.username}」吗？此操作不可恢复。`,
    '确认删除',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  try {
    const response = await usersApi.deleteUser(user.id)
    if (response.ok) {
      ElMessage.success('删除成功')
      loadUsers()
    } else {
      throw new Error(response.error || '删除失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '删除失败')
  }
}

function resetUserForm() {
  userForm.id = null
  userForm.username = ''
  userForm.password = ''
  userForm.real_name = ''
  userForm.email = ''
  userForm.role = 'user'
  userForm.status = 'active'
}

async function submitUserForm() {
  if (!userFormRef.value) return

  await userFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      const userData = {
        username: userForm.username,
        real_name: userForm.real_name,
        email: userForm.email,
        role: userForm.role,
        status: userForm.status
      }

      if (userForm.password) {
        userData.password = userForm.password
      }

      let response
      if (isEdit.value && userForm.id) {
        response = { ok: true }
        ElMessage.warning('更新用户功能需要后端API支持')
      } else {
        response = await usersApi.addUser(userData)
      }

      if (response.ok) {
        ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
        dialogVisible.value = false
        loadUsers()
      } else {
        throw new Error(response.error || (isEdit.value ? '更新失败' : '添加失败'))
      }
    } catch (error) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}
</script>

<style scoped>
.users-management {
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

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
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

.stat-icon.admin {
  background: linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%);
  color: #be185d;
}

.stat-icon.active {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #15803d;
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

.user-info-cell {
  display: flex;
  gap: 12px;
  align-items: center;
}

.user-avatar {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 14px;
}

.user-username {
  font-size: 12px;
  color: var(--text-secondary);
}

.user-form {
  padding: 16px 0;
}

@media (max-width: 1024px) {
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .page-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
}
</style>
