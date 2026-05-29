<template>
  <div class="settings-management">
    <div class="page-header">
      <div class="header-info">
        <h1 class="page-title">系统设置</h1>
        <p class="page-subtitle">配置图书馆借阅规则和系统参数</p>
      </div>
    </div>

    <div class="settings-container">
      <div class="settings-section">
        <div class="section-header">
          <div class="section-icon">
            <el-icon><Setting /></el-icon>
          </div>
          <div class="section-info">
            <h2 class="section-title">借阅规则设置</h2>
            <p class="section-desc">配置图书借阅的相关规则</p>
          </div>
        </div>

        <el-form
          ref="settingsFormRef"
          :model="settingsForm"
          :rules="settingsRules"
          label-width="180px"
          class="settings-form"
        >
          <div class="settings-grid">
            <el-form-item label="默认借阅天数" prop="default_borrow_days">
              <div class="setting-input-wrapper">
                <el-input-number
                  v-model="settingsForm.default_borrow_days"
                  :min="1"
                  :max="365"
                  :step="1"
                  size="large"
                  style="width: 200px"
                />
                <span class="setting-unit">天</span>
              </div>
              <p class="setting-hint">设置图书的默认借阅期限</p>
            </el-form-item>

            <el-form-item label="单次最大借阅数量" prop="max_borrow_count">
              <div class="setting-input-wrapper">
                <el-input-number
                  v-model="settingsForm.max_borrow_count"
                  :min="1"
                  :max="20"
                  :step="1"
                  size="large"
                  style="width: 200px"
                />
                <span class="setting-unit">本</span>
              </div>
              <p class="setting-hint">限制读者单次可借阅的图书数量</p>
            </el-form-item>

            <el-form-item label="允许续借次数" prop="max_renewal_times">
              <div class="setting-input-wrapper">
                <el-input-number
                  v-model="settingsForm.max_renewal_times"
                  :min="0"
                  :max="10"
                  :step="1"
                  size="large"
                  style="width: 200px"
                />
                <span class="setting-unit">次</span>
              </div>
              <p class="setting-hint">设置图书可以续借的最大次数</p>
            </el-form-item>

            <el-form-item label="逾期罚款 (每天)" prop="overdue_fine_per_day">
              <div class="setting-input-wrapper">
                <el-input-number
                  v-model="settingsForm.overdue_fine_per_day"
                  :min="0"
                  :max="100"
                  :step="0.1"
                  :precision="1"
                  size="large"
                  style="width: 200px"
                />
                <span class="setting-unit">元</span>
              </div>
              <p class="setting-hint">设置图书逾期后每天的罚款金额</p>
            </el-form-item>

            <el-form-item label="最高罚款上限" prop="max_fine_amount">
              <div class="setting-input-wrapper">
                <el-input-number
                  v-model="settingsForm.max_fine_amount"
                  :min="0"
                  :max="1000"
                  :step="1"
                  size="large"
                  style="width: 200px"
                />
                <span class="setting-unit">元</span>
              </div>
              <p class="setting-hint">设置单本图书的最高罚款金额</p>
            </el-form-item>

            <el-form-item label="到期提醒提前天数" prop="reminder_days_before_due">
              <div class="setting-input-wrapper">
                <el-input-number
                  v-model="settingsForm.reminder_days_before_due"
                  :min="0"
                  :max="30"
                  :step="1"
                  size="large"
                  style="width: 200px"
                />
                <span class="setting-unit">天</span>
              </div>
              <p class="setting-hint">在图书到期前几天发送提醒通知</p>
            </el-form-item>
          </div>

          <el-divider />

          <div class="settings-actions">
            <el-button type="primary" size="large" :loading="submitLoading" @click="saveSettings">
              <el-icon><CircleCheck /></el-icon>
              保存设置
            </el-button>
            <el-button size="large" @click="resetSettings">
              <el-icon><Refresh /></el-icon>
              重置为默认值
            </el-button>
          </div>
        </el-form>
      </div>

      <div class="settings-section">
        <div class="section-header">
          <div class="section-icon info">
            <el-icon><InfoFilled /></el-icon>
          </div>
          <div class="section-info">
            <h2 class="section-title">系统信息</h2>
            <p class="section-desc">查看当前系统的基本信息</p>
          </div>
        </div>

        <el-descriptions :column="2" border class="system-info">
          <el-descriptions-item label="系统版本">
            <el-tag size="large" type="primary">v1.0.0</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="运行环境">
            <span>Vue 3 + Vite 5 + Element Plus</span>
          </el-descriptions-item>
          <el-descriptions-item label="后端服务">
            <el-tag size="large" type="success">Spring Boot 微服务</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="API地址">
            <span>http://localhost:8080</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="settings-section">
        <div class="section-header">
          <div class="section-icon warning">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="section-info">
            <h2 class="section-title">数据管理</h2>
            <p class="section-desc">系统数据的备份和恢复操作</p>
          </div>
        </div>

        <div class="data-actions">
          <el-button type="primary" size="large" @click="exportData">
            <el-icon><Download /></el-icon>
            导出数据
          </el-button>
          <input
            ref="importFileRef"
            type="file"
            accept="application/json"
            style="display: none"
            @change="handleImportFile"
          />
          <el-button type="warning" size="large" @click="importData">
            <el-icon><Upload /></el-icon>
            导入数据
          </el-button>
          <el-button type="danger" size="large" @click="clearData">
            <el-icon><Delete /></el-icon>
            清空数据
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Setting,
  InfoFilled,
  Warning,
  CircleCheck,
  Refresh,
  Download,
  Upload,
  Delete
} from '@element-plus/icons-vue'
import * as settingsApi from '@/api/settings'

const submitLoading = ref(false)
const settingsFormRef = ref(null)
const importFileRef = ref(null)

const defaultSettings = {
  default_borrow_days: 30,
  max_borrow_count: 5,
  max_renewal_times: 1,
  overdue_fine_per_day: 0.5,
  max_fine_amount: 50,
  reminder_days_before_due: 3
}

const settingsForm = reactive({ ...defaultSettings })

const settingsRules = {
  default_borrow_days: [
    { required: true, message: '请输入默认借阅天数', trigger: 'blur' },
    { type: 'number', min: 1, max: 365, message: '借阅天数应在1-365天之间', trigger: 'blur' }
  ],
  max_borrow_count: [
    { required: true, message: '请输入最大借阅数量', trigger: 'blur' },
    { type: 'number', min: 1, max: 20, message: '借阅数量应在1-20本之间', trigger: 'blur' }
  ],
  overdue_fine_per_day: [
    { required: true, message: '请输入逾期罚款金额', trigger: 'blur' },
    { type: 'number', min: 0, max: 100, message: '罚款金额应在0-100元之间', trigger: 'blur' }
  ],
  max_fine_amount: [
    { required: true, message: '请输入最高罚款上限', trigger: 'blur' },
    { type: 'number', min: 0, max: 1000, message: '罚款上限应在0-1000元之间', trigger: 'blur' }
  ]
}

onMounted(() => {
  loadSettings()
})

async function loadSettings() {
  try {
    const response = await settingsApi.getSettings()
    if (response) {
      settingsForm.default_borrow_days = response.borrowDays || 30
      settingsForm.max_borrow_count = response.maxBorrowCount || 5
      settingsForm.max_renewal_times = response.maxRenewalTimes || 1
      settingsForm.overdue_fine_per_day = parseFloat(response.overdueFinePerDay) || 0.5
      settingsForm.max_fine_amount = parseFloat(response.maxFineAmount) || 50
      settingsForm.reminder_days_before_due = response.reminderDaysBeforeDue || 3
    }
  } catch (error) {
    console.error('加载系统设置失败:', error)
  }
}

async function saveSettings() {
  if (!settingsFormRef.value) return

  await settingsFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitLoading.value = true
    try {
      await settingsApi.updateSettings({
        borrowDays: settingsForm.default_borrow_days,
        maxBorrowCount: settingsForm.max_borrow_count,
        maxRenewalTimes: settingsForm.max_renewal_times,
        overdueFinePerDay: settingsForm.overdue_fine_per_day,
        maxFineAmount: settingsForm.max_fine_amount,
        reminderDaysBeforeDue: settingsForm.reminder_days_before_due
      })
      ElMessage.success('设置保存成功')
    } catch (error) {
      ElMessage.error(error.message || '保存失败，请重试')
    } finally {
      submitLoading.value = false
    }
  })
}

async function resetSettings() {
  await ElMessageBox.confirm(
    '确定要将所有设置重置为默认值吗？',
    '确认重置',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )

  Object.assign(settingsForm, defaultSettings)
  ElMessage.success('已重置为默认值')
}

function exportData() {
  const payload = {
    exportedAt: new Date().toISOString(),
    type: 'library-system-settings',
    version: '1.0.0',
    settings: { ...settingsForm }
  }
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `library-settings-${new Date().toISOString().slice(0, 10)}.json`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('设置数据已导出')
}

function importData() {
  importFileRef.value?.click()
}

function handleImportFile(event) {
  const file = event.target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = async () => {
    try {
      const payload = JSON.parse(String(reader.result || '{}'))
      const importedSettings = payload.settings || payload
      Object.assign(settingsForm, {
        ...settingsForm,
        ...importedSettings
      })
      await saveSettings()
      ElMessage.success('设置数据已导入')
    } catch (error) {
      ElMessage.error('导入失败，请选择有效的 JSON 文件')
    } finally {
      event.target.value = ''
    }
  }
  reader.readAsText(file, 'utf-8')
}

async function clearData() {
  await ElMessageBox.confirm(
    '此操作将清除当前浏览器登录态和缓存，并把借阅规则重置为默认值。确定要继续吗？',
    '清理本地数据',
    {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'error',
      confirmButtonClass: 'el-button--danger'
    }
  )

  Object.assign(settingsForm, defaultSettings)
  await settingsApi.updateSettings({
    borrowDays: settingsForm.default_borrow_days,
    maxBorrowCount: settingsForm.max_borrow_count,
    maxRenewalTimes: settingsForm.max_renewal_times,
    overdueFinePerDay: settingsForm.overdue_fine_per_day,
    maxFineAmount: settingsForm.max_fine_amount,
    reminderDaysBeforeDue: settingsForm.reminder_days_before_due
  })
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('currentUser')
  ElMessage.success('本地数据已清理，系统设置已重置')
}
</script>

<style scoped>
.settings-management {
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

.settings-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.settings-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: var(--shadow-sm);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.section-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
}

.section-icon.info {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #059669;
}

.section-icon.warning {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #d97706;
}

.section-info .section-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.section-info .section-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.settings-form {
  padding: 16px 0;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.setting-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
}

.setting-unit {
  font-size: 14px;
  color: var(--text-secondary);
}

.setting-hint {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 4px 0 0 0;
}

.settings-actions {
  display: flex;
  gap: 16px;
  justify-content: flex-start;
}

.system-info {
  padding: 8px 0;
}

.data-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

@media (max-width: 1024px) {
  .settings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
