<template>
  <div class="trace-process-page">
    <div class="page-banner">
      <h1>溯源流程</h1>
      <p>从田间到餐桌，每个环节都有据可查</p>
    </div>

    <!-- Process Steps -->
    <div class="process-section">
      <div v-for="(step, idx) in steps" :key="idx" class="process-row" :class="{ reverse: idx % 2 === 1 }">
        <div class="process-visual" :style="{ background: step.gradient }">
          <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="white" stroke-width="1.5">
            <path :d="step.icon" />
          </svg>
          <div class="process-step-num">第{{ idx + 1 }}步</div>
        </div>
        <div class="process-detail">
          <h2>{{ step.name }}</h2>
          <p class="process-desc">{{ step.desc }}</p>
          <div class="process-items">
            <div v-for="item in step.items" :key="item" class="process-item">
              <el-icon color="#2d8c3c"><CircleCheck /></el-icon> {{ item }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- CTA -->
    <div class="cta-section">
      <h3>立即体验溯源查询</h3>
      <p>输入溯源码，查看农产品的全程信息</p>
      <el-button type="primary" size="large" round @click="$router.push('/trace')" style="height: 48px; padding: 0 40px; font-size: 16px;">
        <el-icon><Search /></el-icon> 前往溯源查询
      </el-button>
    </div>
  </div>
</template>

<script setup>
const steps = [
  {
    name: '种植产地', desc: '记录农产品种植的源头信息，包括农场位置、农户姓名、种植面积、种植和采收日期等，确保产地信息真实可查。',
    gradient: 'linear-gradient(135deg, #67c23a, #529b2e)',
    icon: 'M12 2L4 7v6c0 5 3.5 8.5 8 10 4.5-1.5 8-5 8-10V7l-8-5z',
    items: ['农场名称与位置', '农户姓名与联系方式', '种植面积与主要作物', '种植日期与采收日期']
  },
  {
    name: '质量检测', desc: '对采收的农产品进行专业质检，包括农药残留检测、质量等级评定，合格后才能进入销售环节。',
    gradient: 'linear-gradient(135deg, #409eff, #337ecc)',
    icon: 'M9 11l3 3L22 4M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11',
    items: ['农药残留检测', '质量等级评定', '质检员与质检日期', '检测报告与合格认证']
  },
  {
    name: '物流运输', desc: '从产地到消费者手中的运输全程记录，包括运输公司、运输方式、冷链温度监控、发货和到货时间。',
    gradient: 'linear-gradient(135deg, #e6a23c, #b88230)',
    icon: 'M1 3h15v13H1zM16 8h4l3 3v5h-7V8z',
    items: ['运输公司与运单号', '运输方式（冷链/常温）', '冷链温度记录', '发货地与收货地']
  },
  {
    name: '扫码溯源', desc: '消费者通过溯源码查询农产品的全程信息，从种植到配送的每个环节都透明可见，放心购买。',
    gradient: 'linear-gradient(135deg, #722ed1, #531dab)',
    icon: 'M3 3v18h18M7 14l4-4 4 4 6-6',
    items: ['溯源码唯一标识', '扫码即查全程信息', '时间线展示各环节', '信息不可篡改']
  }
]
</script>

<style scoped>
.trace-process-page { max-width: 1000px; margin: 0 auto; }
.page-banner { background: linear-gradient(135deg, #1a5c25, #2d8c3c); border-radius: 16px; padding: 40px; margin-bottom: 32px; color: white; text-align: center; }
.page-banner h1 { font-size: 28px; margin-bottom: 8px; }
.page-banner p { font-size: 14px; opacity: 0.85; }

.process-section { display: flex; flex-direction: column; gap: 24px; }
.process-row { display: flex; gap: 32px; align-items: center; background: white; border-radius: 16px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: transform 0.2s; }
.process-row:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(0,0,0,0.06); }
.process-row.reverse { flex-direction: row-reverse; }
.process-visual { width: 120px; height: 120px; border-radius: 16px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; flex-shrink: 0; position: relative; }
.process-step-num { color: white; font-size: 12px; opacity: 0.9; }
.process-detail { flex: 1; }
.process-detail h2 { font-size: 20px; color: #333; margin-bottom: 8px; }
.process-desc { font-size: 14px; color: #666; line-height: 1.7; margin-bottom: 16px; }
.process-items { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.process-item { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #555; }

.cta-section { text-align: center; padding: 48px; margin-top: 32px; background: linear-gradient(135deg, #f0f9eb, #e8f5e9); border-radius: 16px; }
.cta-section h3 { font-size: 22px; color: #2d8c3c; margin-bottom: 8px; }
.cta-section p { font-size: 14px; color: #666; margin-bottom: 20px; }

@media (max-width: 768px) {
  .process-row, .process-row.reverse { flex-direction: column; }
  .process-items { grid-template-columns: 1fr; }
}
</style>
