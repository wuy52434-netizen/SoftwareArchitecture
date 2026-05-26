<template>
  <div class="kiosk-home">
    <div class="welcome-section">
      <div class="welcome-icon">
        <el-icon><Pointer /></el-icon>
      </div>
      <div class="welcome-text">
        <h2>欢迎使用智慧图书馆</h2>
        <p>自助借阅终端，让借阅更便捷</p>
      </div>
    </div>

    <div class="main-actions">
      <div class="action-card action-borrow" @click="startBorrow">
        <div class="action-icon">
          <el-icon><Plus /></el-icon>
        </div>
        <div class="action-content">
          <h3>借阅图书</h3>
          <p>Borrow Books</p>
        </div>
        <div class="action-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <div class="action-card action-return" @click="startReturn">
        <div class="action-icon">
          <el-icon><Minus /></el-icon>
        </div>
        <div class="action-content">
          <h3>归还图书</h3>
          <p>Return Books</p>
        </div>
        <div class="action-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <div class="action-card action-search" @click="startSearch">
        <div class="action-icon">
          <el-icon><Search /></el-icon>
        </div>
        <div class="action-content">
          <h3>查询图书</h3>
          <p>Search Books</p>
        </div>
        <div class="action-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <div class="info-section">
      <div class="info-card">
        <div class="info-icon blue">
          <el-icon><Calendar /></el-icon>
        </div>
        <div class="info-text">
          <h4>借阅期限</h4>
          <p>普通图书：30天</p>
          <p>可续借：1次</p>
        </div>
      </div>

      <div class="info-card">
        <div class="info-icon orange">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="info-text">
          <h4>逾期提醒</h4>
          <p>逾期每天罚款 0.5 元</p>
          <p>最高罚款 50 元</p>
        </div>
      </div>

      <div class="info-card">
        <div class="info-icon green">
          <el-icon><Phone /></el-icon>
        </div>
        <div class="info-text">
          <h4>服务热线</h4>
          <p>工作时间：08:00 - 20:00</p>
          <p>010-12345678</p>
        </div>
      </div>
    </div>

    <div class="operation-steps">
      <h3 class="steps-title">操作指引</h3>
      <div class="steps-container">
        <div class="step-item">
          <div class="step-number">1</div>
          <div class="step-content">
            <h4>选择操作</h4>
            <p>点击借阅、归还或查询</p>
          </div>
        </div>
        <div class="step-connector">
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="step-item">
          <div class="step-number">2</div>
          <div class="step-content">
            <h4>扫描条码</h4>
            <p>将图书放在扫码区</p>
          </div>
        </div>
        <div class="step-connector">
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="step-item">
          <div class="step-number">3</div>
          <div class="step-content">
            <h4>身份验证</h4>
            <p>刷读者证或输入卡号</p>
          </div>
        </div>
        <div class="step-connector">
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="step-item">
          <div class="step-number">4</div>
          <div class="step-content">
            <h4>确认完成</h4>
            <p>核对信息后确认操作</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { 
  Pointer,
  Plus,
  Minus,
  Search,
  ArrowRight,
  Calendar,
  Warning,
  Phone
} from '@element-plus/icons-vue'

const router = useRouter()

function startBorrow() {
  router.push('/kiosk/borrow')
}

function startReturn() {
  router.push('/kiosk/return')
}

function startSearch() {
  router.push('/kiosk/search')
}
</script>

<style scoped>
.kiosk-home {
  max-width: 1400px;
  margin: 0 auto;
}

.welcome-section {
  text-align: center;
  margin-bottom: 48px;
}

.welcome-icon {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: white;
  margin: 0 auto 24px;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.4);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.welcome-text h2 {
  font-size: 42px;
  font-weight: 700;
  color: #1e3a5f;
  margin: 0 0 12px 0;
  letter-spacing: 2px;
}

.welcome-text p {
  font-size: 20px;
  color: #64748b;
  margin: 0;
}

.main-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 32px;
  margin-bottom: 48px;
}

.action-card {
  background: white;
  border-radius: 24px;
  padding: 48px 32px;
  display: flex;
  align-items: center;
  gap: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  border: 3px solid transparent;
  position: relative;
  overflow: hidden;
}

.action-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 6px;
}

.action-borrow::before {
  background: linear-gradient(90deg, #10b981, #059669);
}

.action-return::before {
  background: linear-gradient(90deg, #3b82f6, #1d4ed8);
}

.action-search::before {
  background: linear-gradient(90deg, #f59e0b, #d97706);
}

.action-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.15);
}

.action-borrow:hover {
  border-color: #10b981;
}

.action-return:hover {
  border-color: #3b82f6;
}

.action-search:hover {
  border-color: #f59e0b;
}

.action-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40px;
  flex-shrink: 0;
}

.action-borrow .action-icon {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #059669;
}

.action-return .action-icon {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1d4ed8;
}

.action-search .action-icon {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  color: #d97706;
}

.action-content h3 {
  font-size: 28px;
  font-weight: 700;
  color: #1e3a5f;
  margin: 0 0 4px 0;
}

.action-content p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.action-arrow {
  margin-left: auto;
  font-size: 32px;
  color: #cbd5e1;
  transition: all 0.3s ease;
}

.action-card:hover .action-arrow {
  color: #3b82f6;
  transform: translateX(8px);
}

.info-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 48px;
}

.info-card {
  background: white;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.info-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.info-icon.blue {
  background: #dbeafe;
  color: #1d4ed8;
}

.info-icon.orange {
  background: #fed7aa;
  color: #c2410c;
}

.info-icon.green {
  background: #d1fae5;
  color: #059669;
}

.info-text h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1e3a5f;
  margin: 0 0 8px 0;
}

.info-text p {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 4px 0;
}

.info-text p:last-child {
  margin-bottom: 0;
}

.operation-steps {
  background: white;
  border-radius: 20px;
  padding: 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.steps-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e3a5f;
  margin: 0 0 32px 0;
  text-align: center;
}

.steps-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
}

.step-number {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.step-content h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1e3a5f;
  margin: 0 0 4px 0;
}

.step-content p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

.step-connector {
  font-size: 24px;
  color: #cbd5e1;
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .main-actions {
    grid-template-columns: 1fr;
    gap: 24px;
  }
  
  .info-section {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .steps-container {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .step-connector {
    display: none;
  }
}

@media (max-width: 768px) {
  .welcome-text h2 {
    font-size: 32px;
  }
  
  .main-actions {
    gap: 16px;
  }
  
  .action-card {
    padding: 32px 24px;
    flex-direction: column;
    text-align: center;
  }
  
  .action-arrow {
    display: none;
  }
  
  .info-section {
    grid-template-columns: 1fr;
  }
  
  .step-item {
    width: 100%;
    margin-bottom: 16px;
  }
}
</style>
