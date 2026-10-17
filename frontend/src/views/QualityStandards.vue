<template>
  <div class="quality-page">
    <!-- Hero -->
    <section class="hero-banner section-anim">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <span class="hero-badge">
          <el-icon><CircleCheck /></el-icon>
          品质为先 · 标准先行
        </span>
        <h1 class="hero-title">品质标准</h1>
        <p class="hero-subtitle">严守国家标准，全程可溯源，从田间到餐桌层层把关，守护舌尖上的安全</p>
      </div>
    </section>

    <!-- Quality standards section -->
    <section class="block">
      <div class="block-header">
        <h2 class="section-title"><span class="title-bar"></span>品质标准</h2>
        <p class="block-desc">对标国家食品安全与农产品质量标准，确保每一件商品经得起检验</p>
      </div>

      <div class="standards-grid" v-loading="loadingStandards">
        <div
          v-for="(s, idx) in standards"
          :key="s.num || idx"
          class="standard-card"
          :style="{ '--i': idx }"
        >
          <div class="card-top">
            <div class="s-icon">
              <el-icon><component :is="resolveStandardIcon(s, idx)" /></el-icon>
            </div>
            <span class="s-num">{{ String(s.num ?? idx + 1).padStart(2, '0') }}</span>
          </div>
          <h3 class="s-title">{{ s.title }}</h3>
          <div class="s-code" v-if="s.code">
            <el-tag size="small" effect="light" round>{{ s.code }}</el-tag>
          </div>
          <p class="s-desc">{{ s.description }}</p>
        </div>

        <!-- Fallback skeletons when API empty -->
        <template v-if="!loadingStandards && !standards.length">
          <div v-for="(s, idx) in fallbackStandards" :key="'fb-' + idx" class="standard-card" :style="{ '--i': idx }">
            <div class="card-top">
              <div class="s-icon"><el-icon><component :is="s.icon" /></el-icon></div>
              <span class="s-num">{{ String(idx + 1).padStart(2, '0') }}</span>
            </div>
            <h3 class="s-title">{{ s.title }}</h3>
            <div class="s-code"><el-tag size="small" effect="light" round>{{ s.code }}</el-tag></div>
            <p class="s-desc">{{ s.description }}</p>
          </div>
        </template>
      </div>
    </section>

    <!-- Trace process section -->
    <section class="block">
      <div class="block-header">
        <h2 class="section-title"><span class="title-bar"></span>溯源流程</h2>
        <p class="block-desc">四段式全链路溯源，每一步都有据可查、有迹可循</p>
      </div>

      <div class="trace-wrap" v-loading="loadingProcess">
        <div class="trace-line"></div>
        <div class="trace-grid">
          <div
            v-for="(step, idx) in traceSteps"
            :key="step.num || idx"
            class="trace-step"
            :style="{ '--i': idx, '--step-color': step.color || processColors[idx % 4] }"
          >
            <div class="step-icon" :style="{ background: step.color || processColors[idx % 4] }">
              <el-icon><component :is="resolveTraceIcon(step, idx)" /></el-icon>
            </div>
            <div class="step-connector" v-if="idx < traceSteps.length - 1">
              <el-icon><ArrowRightBold /></el-icon>
            </div>
            <span class="step-stage">{{ step.stage || step.title }}</span>
            <h3 class="step-title">{{ step.title }}</h3>
            <p class="step-desc">{{ step.description }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Promise section -->
    <section class="promise-card section-anim">
      <div class="promise-bg"></div>
      <div class="promise-content">
        <div class="promise-left">
          <span class="promise-badge"><el-icon><Medal /></el-icon> 品质承诺</span>
          <h2>四重保障，安心之选</h2>
          <p>源头可溯 · 标准可依 · 质检可信 · 流程可见，我们用严格的标准守护每一份食材的安全与新鲜。</p>
        </div>
        <div class="promise-right">
          <div class="promise-stat" v-for="p in promises" :key="p.label">
            <div class="p-num">{{ p.value }}</div>
            <div class="p-label">{{ p.label }}</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { qualityApi } from '@/api'

const standards = ref([])
const traceSteps = ref([])
const loadingStandards = ref(false)
const loadingProcess = ref(false)

// Known-valid Element Plus icon names (globally registered)
const ICON_SET = new Set([
  'CircleCheck', 'CircleCheckFilled', 'Document', 'DocumentChecked', 'DocumentCopy',
  'Files', 'Stamp', 'Medal', 'GoldMedal', 'Aim', 'Histogram', 'TrendCharts',
  'DataAnalysis', 'Cpu', 'Lock', 'Setting', 'Tools', 'Odometer', 'Monitor',
  'Sunny', 'Scissor', 'Crop', 'View', 'Van', 'Box', 'Warning', 'WarningFilled',
  'Cherry', 'Apple', 'Grape', 'Pear', 'Lemon', 'Dish', 'Picture', 'Coordinate',
  'Connection', 'Coin', 'Wallet', 'Goods', 'Shop', 'TakeawayBox', 'Timer'
])

const standardFallback = ['CircleCheck', 'Document', 'Stamp', 'Medal', 'Histogram', 'Aim']
const traceFallback = ['Sunny', 'Scissor', 'CircleCheck', 'Van']

const resolveStandardIcon = (item, idx) => {
  if (item.icon && ICON_SET.has(item.icon)) return item.icon
  return standardFallback[idx % standardFallback.length]
}

const traceKeywordMap = {
  '种植': 'Sunny', 'plant': 'Sunny', '播': 'Sunny', '育': 'Sunny',
  '采摘': 'Scissor', '收': 'Scissor', 'harvest': 'Scissor', '采': 'Scissor',
  '质检': 'CircleCheck', '检': 'CircleCheck', 'quality': 'CircleCheck', '验': 'CircleCheck',
  '物流': 'Van', '运输': 'Van', 'logistics': 'Van', '配': 'Van', '送': 'Van'
}
const resolveTraceIcon = (item, idx) => {
  if (item.icon && ICON_SET.has(item.icon)) return item.icon
  const hay = `${item.title || ''} ${item.stage || ''}`
  for (const key in traceKeywordMap) {
    if (hay.includes(key)) return traceKeywordMap[key]
  }
  return traceFallback[idx % traceFallback.length]
}

const processColors = ['#1a5d3a', '#2d8c3c', '#3da655', '#46a85a']

const fallbackStandards = [
  { title: '农药残留限量', code: 'GB 2763-2021', description: '严格遵循国家食品中农药最大残留限量标准，确保农产品农残指标合规可控。', icon: 'CircleCheck' },
  { title: '食品中污染物限量', code: 'GB 2762-2022', description: '对铅、镉、汞等重金属污染物设定严格限量，从源头把控食品安全。', icon: 'Document' },
  { title: '绿色食品标准', code: 'NY/T 391', description: '产地环境、生产操作、包装储运全程符合绿色食品质量标准体系。', icon: 'Stamp' },
  { title: '农产品质量安全', code: 'GB/T 20014', description: '良好农业规范认证，覆盖种植、采收、储运全链条质量控制。', icon: 'Medal' },
  { title: '食品添加剂使用', code: 'GB 2760-2014', description: '规范食品添加剂使用范围与剂量，杜绝滥用与超量添加。', icon: 'Histogram' },
  { title: '产地环境质量', code: 'GB 15618', description: '土壤、灌溉水、空气质量达标，保障农产品生长环境安全洁净。', icon: 'Aim' }
]

const promises = [
  { value: '100%', label: '可溯源覆盖' },
  { value: '4道', label: '品控关卡' },
  { value: 'GB', label: '国家标准' },
  { value: '0', label: '安全隐患' }
]

const loadStandards = async () => {
  loadingStandards.value = true
  try {
    const res = await qualityApi.standards()
    standards.value = res.data || []
  } catch (e) {
    standards.value = []
  } finally {
    loadingStandards.value = false
  }
}

const loadTraceProcess = async () => {
  loadingProcess.value = true
  try {
    const res = await qualityApi.traceProcess()
    traceSteps.value = res.data || []
  } catch (e) {
    traceSteps.value = []
  } finally {
    loadingProcess.value = false
  }
}

onMounted(() => {
  loadStandards()
  loadTraceProcess()
})
</script>

<style scoped>
.quality-page { max-width: 1200px; margin: 0 auto; }

/* Hero */
.hero-banner {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 36px;
  min-height: 220px;
  display: flex;
  align-items: center;
}
.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a5d3a 0%, #2d8c3c 55%, #46a85a 100%);
}
.hero-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 85% 25%, rgba(255,255,255,0.16), transparent 45%),
    radial-gradient(circle at 12% 85%, rgba(232,245,233,0.2), transparent 50%);
}
.hero-content { position: relative; z-index: 1; padding: 44px; color: #fff; width: 100%; }
.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  background: rgba(255,255,255,0.16);
  border: 1px solid rgba(255,255,255,0.28);
  border-radius: 9999px;
  font-size: 13px;
  backdrop-filter: blur(8px);
  margin-bottom: 16px;
}
.hero-title { font-size: 40px; font-weight: 800; margin-bottom: 10px; letter-spacing: 1px; }
.hero-subtitle { font-size: 15px; opacity: 0.9; line-height: 1.6; max-width: 620px; }

/* Block */
.block { margin-bottom: 44px; }
.block-header { margin-bottom: 24px; }
.section-title { font-size: 22px; font-weight: 700; color: #1a1a1a; display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.title-bar { width: 4px; height: 22px; background: #2d8c3c; border-radius: 2px; }
.block-desc { font-size: 14px; color: #888; }

/* Standards grid */
.standards-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.standard-card {
  position: relative;
  background: #fff;
  border-radius: 12px;
  padding: 26px 24px;
  border: 1px solid #f0f3f0;
  transition: all 0.3s ease;
  animation: cardIn 0.5s ease both;
  animation-delay: calc(var(--i, 0) * 0.06s);
  overflow: hidden;
}
.standard-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(90deg, #1a5d3a, #2d8c3c);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.35s ease;
}
.standard-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
  border-color: #c8e6c9;
}
.standard-card:hover::before { transform: scaleX(1); }
@keyframes cardIn {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.s-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: linear-gradient(135deg, #e8f5e9, #d7ecd9);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2d8c3c;
  font-size: 24px;
  transition: all 0.3s ease;
}
.standard-card:hover .s-icon {
  background: linear-gradient(135deg, #1a5d3a, #2d8c3c);
  color: #fff;
  transform: rotate(-6deg) scale(1.06);
}
.s-num {
  font-size: 32px;
  font-weight: 800;
  color: #e8f5e9;
  line-height: 1;
}
.s-title { font-size: 17px; font-weight: 600; color: #1a1a1a; margin-bottom: 10px; }
.s-code { margin-bottom: 12px; }
.s-code :deep(.el-tag) { color: #2d8c3c; border-color: #c8e6c9; background: #e8f5e9; font-weight: 600; }
.s-desc { font-size: 13px; color: #888; line-height: 1.7; }

/* Trace process */
.trace-wrap { position: relative; }
.trace-line {
  position: absolute;
  top: 26px;
  left: 8%;
  right: 8%;
  height: 2px;
  background: repeating-linear-gradient(90deg, #c8e6c9 0, #c8e6c9 8px, transparent 8px, transparent 16px);
}
.trace-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  position: relative;
}
.trace-step {
  text-align: center;
  position: relative;
  background: #fff;
  border-radius: 12px;
  padding: 28px 18px 24px;
  border: 1px solid #f0f3f0;
  transition: all 0.3s ease;
  animation: cardIn 0.5s ease both;
  animation-delay: calc(var(--i, 0) * 0.08s);
}
.trace-step:hover {
  transform: translateY(-6px);
  box-shadow: 0 8px 30px rgba(0,0,0,0.12);
  border-color: var(--step-color, #2d8c3c);
}
.step-icon {
  width: 56px;
  height: 56px;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #fff;
  font-size: 26px;
  position: relative;
  z-index: 1;
  box-shadow: 0 6px 16px rgba(0,0,0,0.12);
  border: 4px solid #fff;
}
.step-connector {
  position: absolute;
  top: 26px;
  right: -22px;
  z-index: 2;
  width: 28px;
  height: 28px;
  border-radius: 9999px;
  background: #fff;
  border: 1px solid #e8f5e9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2d8c3c;
}
.step-stage {
  display: inline-block;
  padding: 3px 12px;
  background: #e8f5e9;
  color: #1a5d3a;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
}
.step-title { font-size: 16px; font-weight: 600; color: #1a1a1a; margin-bottom: 8px; }
.step-desc { font-size: 13px; color: #999; line-height: 1.7; }

/* Promise */
.promise-card {
  position: relative;
  border-radius: 20px;
  overflow: hidden;
  margin-bottom: 20px;
}
.promise-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1a5d3a 0%, #2d8c3c 100%);
}
.promise-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 90% 10%, rgba(255,255,255,0.14), transparent 45%);
}
.promise-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 40px 44px;
  color: #fff;
  gap: 32px;
}
.promise-left { flex: 1; }
.promise-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  background: rgba(255,255,255,0.18);
  border: 1px solid rgba(255,255,255,0.28);
  border-radius: 9999px;
  font-size: 13px;
  margin-bottom: 14px;
}
.promise-left h2 { font-size: 26px; font-weight: 800; margin-bottom: 10px; }
.promise-left p { font-size: 14px; opacity: 0.9; line-height: 1.7; max-width: 520px; }
.promise-right { display: flex; gap: 32px; }
.promise-stat { text-align: center; }
.p-num { font-size: 34px; font-weight: 800; }
.p-label { font-size: 13px; opacity: 0.85; margin-top: 2px; }

.section-anim { animation: fadeInUp 0.6s ease both; }
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 992px) {
  .standards-grid { grid-template-columns: repeat(2, 1fr); }
  .trace-grid { grid-template-columns: repeat(2, 1fr); }
  .trace-line { display: none; }
  .step-connector { display: none; }
  .promise-content { flex-direction: column; align-items: flex-start; }
  .promise-right { gap: 24px; }
}
@media (max-width: 576px) {
  .standards-grid { grid-template-columns: 1fr; }
  .trace-grid { grid-template-columns: 1fr; }
  .hero-content, .promise-content { padding: 32px 24px; }
  .hero-title { font-size: 30px; }
  .promise-right { width: 100%; justify-content: space-between; }
}
</style>
