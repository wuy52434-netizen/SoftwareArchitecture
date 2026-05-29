<template>
  <div class="dashboard-page" v-loading="loading">
    <div class="page-header">
      <div>
        <h1 class="page-title">统计看板</h1>
        <p class="page-subtitle">借阅数据、库存状态和读者活跃度</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadDashboard">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="metric-grid">
      <div v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-icon" :class="metric.type">
          <el-icon><component :is="metric.icon" /></el-icon>
        </div>
        <div>
          <div class="metric-value">{{ metric.value }}</div>
          <div class="metric-label">{{ metric.label }}</div>
        </div>
      </div>
    </div>

    <div class="chart-grid">
      <section class="chart-panel chart-wide">
        <div class="panel-header">
          <h2>近 7 日借阅趋势</h2>
          <el-tag type="success" effect="plain">ECharts</el-tag>
        </div>
        <div ref="trendChartRef" class="chart-box"></div>
      </section>

      <section class="chart-panel">
        <div class="panel-header">
          <h2>图书分类占比</h2>
        </div>
        <div ref="categoryChartRef" class="chart-box"></div>
      </section>

      <section class="chart-panel">
        <div class="panel-header">
          <h2>热门图书排行</h2>
        </div>
        <div ref="popularChartRef" class="chart-box"></div>
      </section>

      <section class="chart-panel chart-wide">
        <div class="panel-header">
          <h2>借阅时段分布</h2>
        </div>
        <div ref="hourlyChartRef" class="chart-box compact"></div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  Collection,
  Document,
  Refresh,
  Tickets,
  User,
  Warning
} from '@element-plus/icons-vue'
import * as statsApi from '@/api/stats'

const loading = ref(false)
const dashboard = ref(createFallbackStats())

const trendChartRef = ref(null)
const categoryChartRef = ref(null)
const popularChartRef = ref(null)
const hourlyChartRef = ref(null)

const chartInstances = []

const metrics = computed(() => [
  {
    label: '馆藏总量',
    value: dashboard.value.totalBooks ?? 0,
    icon: Collection,
    type: 'books'
  },
  {
    label: '可借图书',
    value: dashboard.value.availableBooks ?? 0,
    icon: Tickets,
    type: 'available'
  },
  {
    label: '读者人数',
    value: dashboard.value.totalUsers ?? 0,
    icon: User,
    type: 'users'
  },
  {
    label: '当前借阅',
    value: dashboard.value.activeBorrows ?? 0,
    icon: Document,
    type: 'borrows'
  },
  {
    label: '今日借书',
    value: dashboard.value.todayBorrows ?? 0,
    icon: Document,
    type: 'today'
  },
  {
    label: '逾期记录',
    value: dashboard.value.overdueCount ?? 0,
    icon: Warning,
    type: 'overdue'
  }
])

onMounted(async () => {
  await loadDashboard()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  chartInstances.forEach((chart) => chart.dispose())
})

async function loadDashboard() {
  loading.value = true
  try {
    dashboard.value = await statsApi.getDashboardStats()
  } catch (error) {
    ElMessage.warning('统计服务暂不可用，已显示演示数据')
    dashboard.value = createFallbackStats()
  } finally {
    loading.value = false
    await nextTick()
    renderCharts()
  }
}

function renderCharts() {
  renderTrendChart()
  renderCategoryChart()
  renderPopularChart()
  renderHourlyChart()
}

function getChart(elementRef) {
  if (!elementRef.value) return null
  const existing = echarts.getInstanceByDom(elementRef.value)
  if (existing) return existing
  const chart = echarts.init(elementRef.value)
  chartInstances.push(chart)
  return chart
}

function renderTrendChart() {
  const chart = getChart(trendChartRef)
  if (!chart) return
  const data = dashboard.value.borrowTrend || []
  chart.setOption({
    color: ['#2563eb'],
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 24, top: 32, bottom: 32 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((item) => item.name)
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '借阅量',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        areaStyle: { opacity: 0.14 },
        data: data.map((item) => Number(item.value) || 0)
      }
    ]
  })
}

function renderCategoryChart() {
  const chart = getChart(categoryChartRef)
  if (!chart) return
  const data = dashboard.value.categoryDistribution || []
  chart.setOption({
    color: ['#2563eb', '#16a34a', '#f59e0b', '#dc2626', '#7c3aed', '#0891b2'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        name: '分类占比',
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '44%'],
        data: data.map((item) => ({ name: item.name, value: Number(item.value) || 0 }))
      }
    ]
  })
}

function renderPopularChart() {
  const chart = getChart(popularChartRef)
  if (!chart) return
  const data = (dashboard.value.popularBooks || []).slice(0, 8).reverse()
  chart.setOption({
    color: ['#16a34a'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 88, right: 24, top: 24, bottom: 28 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: data.map((item) => item.name),
      axisLabel: { width: 74, overflow: 'truncate' }
    },
    series: [
      {
        name: '借阅次数',
        type: 'bar',
        barWidth: 14,
        data: data.map((item) => Number(item.value) || 0)
      }
    ]
  })
}

function renderHourlyChart() {
  const chart = getChart(hourlyChartRef)
  if (!chart) return
  const data = dashboard.value.hourlyDistribution || []
  chart.setOption({
    color: ['#f59e0b'],
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 24, top: 28, bottom: 32 },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.name)
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '操作次数',
        type: 'bar',
        barWidth: 16,
        data: data.map((item) => Number(item.value) || 0)
      }
    ]
  })
}

function resizeCharts() {
  chartInstances.forEach((chart) => chart.resize())
}

function createFallbackStats() {
  return {
    totalBooks: 120,
    availableBooks: 95,
    totalUsers: 500,
    activeBorrows: 35,
    overdueCount: 3,
    todayBorrows: 18,
    borrowTrend: [
      { name: '05-22', value: 24 },
      { name: '05-23', value: 31 },
      { name: '05-24', value: 18 },
      { name: '05-25', value: 39 },
      { name: '05-26', value: 42 },
      { name: '05-27', value: 36 },
      { name: '05-28', value: 28 }
    ],
    categoryDistribution: [
      { name: '文学', value: 35 },
      { name: '技术', value: 25 },
      { name: '历史', value: 15 },
      { name: '教育', value: 10 },
      { name: '艺术', value: 10 },
      { name: '其他', value: 5 }
    ],
    popularBooks: [
      { name: '三体', value: 156 },
      { name: '活着', value: 142 },
      { name: '百年孤独', value: 128 },
      { name: '人类简史', value: 115 },
      { name: '算法导论', value: 98 },
      { name: '深度学习', value: 76 }
    ],
    hourlyDistribution: [
      { name: '08:00', value: 6 },
      { name: '09:00', value: 12 },
      { name: '10:00', value: 18 },
      { name: '11:00', value: 15 },
      { name: '12:00', value: 9 },
      { name: '13:00', value: 13 },
      { name: '14:00', value: 22 },
      { name: '15:00', value: 27 },
      { name: '16:00', value: 25 },
      { name: '17:00', value: 19 },
      { name: '18:00', value: 11 },
      { name: '19:00', value: 8 },
      { name: '20:00', value: 5 }
    ]
  }
}
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.page-title {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--text-secondary);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.metric-card {
  min-height: 104px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
}

.metric-icon {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.metric-icon.books {
  color: #1d4ed8;
  background: #dbeafe;
}

.metric-icon.available {
  color: #15803d;
  background: #dcfce7;
}

.metric-icon.users {
  color: #7c3aed;
  background: #ede9fe;
}

.metric-icon.borrows {
  color: #0891b2;
  background: #cffafe;
}

.metric-icon.today {
  color: #d97706;
  background: #fef3c7;
}

.metric-icon.overdue {
  color: #b91c1c;
  background: #fee2e2;
}

.metric-value {
  font-size: 28px;
  line-height: 1;
  font-weight: 700;
  color: var(--text-primary);
}

.metric-label {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

.chart-panel {
  min-width: 0;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  padding: 20px;
  box-shadow: var(--shadow-sm);
}

.chart-wide {
  grid-column: 1 / -1;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.chart-box {
  width: 100%;
  height: 320px;
}

.chart-box.compact {
  height: 260px;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }
}
</style>
