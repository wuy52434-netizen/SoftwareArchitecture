<template>
  <div class="kiosk-container">
    <div class="kiosk-header">
      <div class="header-left">
        <div class="kiosk-logo">
          <el-icon><Reading /></el-icon>
        </div>
        <div class="kiosk-title">
          <h1>智慧图书馆</h1>
          <p>自助借阅终端</p>
        </div>
      </div>
      <div class="header-right">
        <div class="kiosk-info">
          <div class="time-display">{{ currentTime }}</div>
          <div class="location-info">
            <el-icon><Location /></el-icon>
            一楼大厅 · 终端 #01
          </div>
        </div>
        <el-button type="primary" size="large" plain @click="showHelp">
          <el-icon><QuestionFilled /></el-icon>
          帮助
        </el-button>
      </div>
    </div>

    <div class="kiosk-content">
      <router-view />
    </div>

    <div class="kiosk-footer">
      <div class="footer-left">
        <span>系统状态：</span>
        <el-tag type="success" size="large">运行正常</el-tag>
      </div>
      <div class="footer-center">
        <span>技术支持：010-12345678</span>
      </div>
      <div class="footer-right">
        <el-button link size="large" @click="backToHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { 
  Reading, 
  Location, 
  QuestionFilled,
  HomeFilled 
} from '@element-plus/icons-vue'

const router = useRouter()
const currentTime = ref('')
let timer = null

const updateTime = () => {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  const hours = String(now.getHours()).padStart(2, '0')
  const minutes = String(now.getMinutes()).padStart(2, '0')
  const seconds = String(now.getSeconds()).padStart(2, '0')
  currentTime.value = `${year}年${month}月${day}日 ${hours}:${minutes}:${seconds}`
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
  }
})

function showHelp() {
  ElMessageBox.alert(
    `<div style="text-align: left; line-height: 1.8;">
      <p><strong>📚 借阅图书：</strong></p>
      <p>1. 点击"借阅图书"按钮</p>
      <p>2. 将图书放在扫码区，扫描图书条码</p>
      <p>3. 输入读者证号或刷读者证</p>
      <p>4. 确认借阅信息后点击"确认借阅"</p>
      <br>
      <p><strong>📖 归还图书：</strong></p>
      <p>1. 点击"归还图书"按钮</p>
      <p>2. 将图书放在扫码区，扫描图书条码</p>
      <p>3. 确认归还信息后点击"确认归还"</p>
      <p>4. 如有逾期费用，请按提示支付</p>
      <br>
      <p><strong>📱 查询图书：</strong></p>
      <p>1. 点击"查询图书"按钮</p>
      <p>2. 输入书名、作者或ISBN进行搜索</p>
      <p>3. 点击图书查看详情和馆藏位置</p>
      <br>
      <p style="color: var(--text-secondary); font-size: 14px;">
        如有问题，请联系图书馆工作人员或拨打技术支持热线。
      </p>
    </div>`,
    '使用帮助',
    {
      confirmButtonText: '我知道了',
      dangerouslyUseHTMLString: true,
      type: 'info'
    }
  )
}

function backToHome() {
  router.push('/kiosk')
}
</script>

<style scoped>
.kiosk-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 50%, #bae6fd 100%);
}

.kiosk-header {
  height: 100px;
  background: linear-gradient(135deg, #1e3a5f 0%, #2563eb 100%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 48px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.kiosk-logo {
  width: 60px;
  height: 60px;
  background: white;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #1e3a5f;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.kiosk-title h1 {
  font-size: 28px;
  font-weight: 700;
  color: white;
  margin: 0;
  letter-spacing: 2px;
}

.kiosk-title p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  margin: 4px 0 0 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.kiosk-info {
  text-align: right;
  color: white;
}

.time-display {
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 1px;
}

.location-info {
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  opacity: 0.9;
  margin-top: 4px;
}

.kiosk-content {
  flex: 1;
  padding: 40px;
  overflow-y: auto;
}

.kiosk-footer {
  height: 60px;
  background: linear-gradient(135deg, #1e3a5f 0%, #1e40af 100%);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 48px;
  color: white;
  font-size: 14px;
}

.footer-left,
.footer-center,
.footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer-right .el-button {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  padding: 8px 16px;
}

.footer-right .el-button:hover {
  color: white;
  background: rgba(255, 255, 255, 0.1);
}
</style>
