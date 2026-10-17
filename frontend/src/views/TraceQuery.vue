<template>
  <div class="trace-page">
    <!-- Search Hero -->
    <div class="search-hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="hero-label">TRACEABILITY QUERY</div>
        <h1>农产品溯源查询</h1>
        <div class="hero-rule"></div>
        <p>输入溯源码，查看农产品从田间到餐桌的全程信息</p>
        <div class="search-bar">
          <el-input v-model="traceCode" placeholder="请输入溯源码，如：AGRI20260801DLN001ABC" size="large" clearable spellcheck="false"
            @keyup.enter="handleQuery" aria-label="溯源码" style="text-transform: uppercase;">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <button class="search-btn" @click="handleQuery" :disabled="loading">
            {{ loading ? '查询中...' : '立即查询' }}
          </button>
        </div>
        <div class="search-hint">
          <span class="hint-text">示例溯源码：</span>
          <span class="example-link" @click="fillExample('AGRI20260801DLN001ABC')">AGRI20260801DLN001ABC</span>
          <span class="hint-sep">|</span>
          <span class="example-link" @click="fillExample('AGRI20260810ORG002DEF')">AGRI20260810ORG002DEF</span>
        </div>
      </div>
    </div>

    <!-- Error State -->
    <div v-if="queryError" class="error-state">
      <h3>{{ errorMessage || '未找到该溯源码对应的信息' }}</h3>
      <p>请检查输入是否正确，或尝试使用上方示例溯源码</p>
    </div>

    <!-- Result Timeline -->
    <div v-if="result" class="trace-timeline">
      <!-- Batch Summary -->
      <div class="batch-summary">
        <div class="batch-left">
          <div class="batch-label">溯源码</div>
          <div class="batch-code">{{ result.batch.traceCode }}</div>
        </div>
        <div class="batch-right">
          <div class="batch-status-label">当前状态</div>
          <div class="batch-status">{{ statusLabels[result.batch.status] || '未知' }}</div>
        </div>
      </div>

      <!-- Stage 1: Origin -->
      <div class="timeline-stage" v-if="traceSections[0]">
        <div class="stage-header">
          <div class="stage-icon stage-icon-green">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 2v20M2 12h20M12 2l-3 3M12 2l3 3"/></svg>
          </div>
          <div class="stage-info">
            <span class="stage-number">01</span>
            <h3 class="stage-title">种植产地</h3>
            <span class="stage-time">{{ traceSections[0].date || '数据采集中' }}</span>
          </div>
          <el-tag :type="traceSections[0].data ? 'success' : 'info'" size="small" effect="light">
            {{ traceSections[0].data ? '已完成' : '采集中' }}
          </el-tag>
        </div>
        <div class="stage-content" v-if="traceSections[0].data">
          <div class="detail-grid">
            <div class="detail-item" v-for="item in traceSections[0].items" :key="item.label">
              <span class="detail-label">{{ item.label }}</span>
              <span class="detail-value" v-if="item.isQualified">
                <span class="qualified-tag" :class="item.value === 1 ? 'pass' : 'fail'">{{ item.value === 1 ? '合格' : '不合格' }}</span>
              </span>
              <span class="detail-value" v-else>{{ item.value || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="stage-content" v-else>
          <p class="data-pending">数据采集中...</p>
        </div>
      </div>

      <div class="timeline-connector"></div>

      <!-- Stage 2: Quality Inspection -->
      <div class="timeline-stage" v-if="traceSections[1]">
        <div class="stage-header">
          <div class="stage-icon stage-icon-blue">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M9 11l3 3L22 4M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/></svg>
          </div>
          <div class="stage-info">
            <span class="stage-number">02</span>
            <h3 class="stage-title">质量检测</h3>
            <span class="stage-time">{{ traceSections[1].date || '数据采集中' }}</span>
          </div>
          <el-tag :type="traceSections[1].data ? 'success' : 'info'" size="small" effect="light">
            {{ traceSections[1].data ? (traceSections[1].data.qualified === 1 ? '合格' : '不合格') : '采集中' }}
          </el-tag>
        </div>
        <div class="stage-content" v-if="traceSections[1].data">
          <table class="quality-table">
            <thead>
              <tr>
                <th>检测项目</th>
                <th>检测员</th>
                <th>检测日期</th>
                <th>检测结果</th>
                <th>判定</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>{{ traceSections[1].data.inspectionItem || '农残检测' }}</td>
                <td>{{ traceSections[1].data.inspector || '-' }}</td>
                <td>{{ traceSections[1].data.inspectionDate?.replace('T',' ').substring(0, 10) || '-' }}</td>
                <td>{{ traceSections[1].data.result || '-' }}</td>
                <td><span class="qualified-tag" :class="traceSections[1].data.qualified === 1 ? 'pass' : 'fail'">{{ traceSections[1].data.qualified === 1 ? '合格' : '不合格' }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="stage-content" v-else>
          <p class="data-pending">数据采集中...</p>
        </div>
      </div>

      <div class="timeline-connector"></div>

      <!-- Stage 3: Logistics -->
      <div class="timeline-stage" v-if="traceSections[2]">
        <div class="stage-header">
          <div class="stage-icon stage-icon-orange">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="1" y="3" width="15" height="13" rx="2"/><path d="M16 8h4l3 3v5h-7V8z"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
          </div>
          <div class="stage-info">
            <span class="stage-number">03</span>
            <h3 class="stage-title">物流运输</h3>
            <span class="stage-time">{{ traceSections[2].date || '数据采集中' }}</span>
          </div>
          <el-tag :type="traceSections[2].data ? 'success' : 'info'" size="small" effect="light">
            {{ traceSections[2].data?.arrivalDate ? '已到货' : traceSections[2].data ? '运输中' : '采集中' }}
          </el-tag>
        </div>
        <div class="stage-content" v-if="traceSections[2].data">
          <div class="logistics-info">
            <div class="detail-item"><span class="detail-label">承运商</span><span class="detail-value">{{ traceSections[2].data.transportCompany || '-' }}</span></div>
            <div class="detail-item"><span class="detail-label">运输方式</span><span class="detail-value">{{ traceSections[2].data.transportMethod || '-' }}</span></div>
            <div class="detail-item"><span class="detail-label">冷链温度</span><span class="detail-value">{{ traceSections[2].data.temperature || '-' }}</span></div>
            <div class="detail-item"><span class="detail-label">发货日期</span><span class="detail-value">{{ traceSections[2].data.shipDate?.replace('T',' ').substring(0, 10) || '-' }}</span></div>
            <div class="detail-item"><span class="detail-label">发货地</span><span class="detail-value">{{ traceSections[2].data.fromLocation || '-' }}</span></div>
            <div class="detail-item"><span class="detail-label">收货地</span><span class="detail-value">{{ traceSections[2].data.toLocation || '-' }}</span></div>
          </div>
          <!-- Logistics Track -->
          <div class="logistics-track" v-if="traceSections[2].data.shipDate">
            <div class="track-node" v-for="(node, idx) in logisticsNodes" :key="idx">
              <div class="track-dot" :class="{ active: node.active }"></div>
              <div class="track-info">
                <span class="track-time">{{ node.time }}</span>
                <span class="track-desc">{{ node.location }}</span>
              </div>
            </div>
          </div>
        </div>
        <div class="stage-content" v-else>
          <p class="data-pending">数据采集中...</p>
        </div>
      </div>

      <div class="timeline-connector"></div>

      <!-- Stage 4: Product Info -->
      <div class="timeline-stage" v-if="result.product">
        <div class="stage-header">
          <div class="stage-icon stage-icon-green">
            <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>
          </div>
          <div class="stage-info">
            <span class="stage-number">04</span>
            <h3 class="stage-title">商品信息</h3>
          </div>
          <el-tag type="success" size="small" effect="light">已上架</el-tag>
        </div>
        <div class="stage-content">
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-label">商品名称</span><span class="detail-value">{{ result.product.name }}</span></div>
            <div class="detail-item"><span class="detail-label">产地</span><span class="detail-value">{{ result.product.origin || '-' }}</span></div>
            <div class="detail-item"><span class="detail-label">价格</span><span class="detail-value price">￥{{ result.product.price }}</span></div>
            <div class="detail-item"><span class="detail-label">描述</span><span class="detail-value">{{ result.product.description || '-' }}</span></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="!loading && !queryError" class="empty-state">
      <div class="empty-icon">
        <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="currentColor" stroke-width="1"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
      </div>
      <p class="empty-title">输入溯源码查询农产品的全程信息</p>
      <p class="empty-hint">试试示例溯源码，体验完整溯源链路</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { traceQueryApi } from '../api'

const route = useRoute()
const traceCode = ref('')
const result = ref(null)
const loading = ref(false)
const queryError = ref(false)
const errorMessage = ref('')

const statusLabels = ['生产中', '质检中', '运输中', '已到货']
const traceCodePattern = /^AGRI\d{8}[A-Z]{3}\d{3}[A-Z]{3}$/

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : '-'

const traceSections = computed(() => {
  if (!result.value) return []
  const r = result.value
  return [
    {
      title: '种植产地',
      date: r.origin?.plantDate || '',
      data: r.origin,
      items: [
        { label: '农场名称', value: r.origin?.farmName },
        { label: '产地位置', value: r.origin?.location },
        { label: '农户姓名', value: r.origin?.farmerName },
        { label: '种植日期', value: r.origin?.plantDate },
        { label: '采收日期', value: r.origin?.harvestDate },
        { label: '描述', value: r.origin?.description },
      ]
    },
    {
      title: '质量检测',
      date: r.inspection?.inspectionDate || '',
      data: r.inspection,
      items: [
        { label: '质检员', value: r.inspection?.inspector },
        { label: '质检日期', value: r.inspection ? formatTime(r.inspection.inspectionDate) : null },
        { label: '检测项目', value: r.inspection?.inspectionItem },
        { label: '检测结果', value: r.inspection?.result },
        { label: '是否合格', value: r.inspection?.qualified, isQualified: true },
      ]
    },
    {
      title: '物流运输',
      date: r.logistics?.shipDate || '',
      data: r.logistics,
      items: [
        { label: '发货日期', value: r.logistics ? formatTime(r.logistics.shipDate) : null },
        { label: '到达日期', value: r.logistics?.arrivalDate ? formatTime(r.logistics.arrivalDate) : '运输中' },
        { label: '发货地', value: r.logistics?.fromLocation },
        { label: '收货地', value: r.logistics?.toLocation },
        { label: '运输公司', value: r.logistics?.transportCompany },
        { label: '运输方式', value: r.logistics?.transportMethod },
        { label: '冷链温度', value: r.logistics?.temperature },
      ]
    }
  ]
})

const logisticsNodes = computed(() => {
  if (!result.value?.logistics) return []
  const log = result.value.logistics
  const nodes = [
    { time: log.shipDate?.replace('T',' ').substring(0, 16) || '-', location: `${log.fromLocation || '发货地'} 已发货`, active: true },
    { time: '运输中', location: `${log.transportCompany || '承运商'} ${log.transportMethod || ''} 冷链运输中`, active: !log.arrivalDate },
    { time: log.arrivalDate?.replace('T',' ').substring(0, 16) || '-', location: `${log.toLocation || '收货地'} ${log.arrivalDate ? '已签收' : '待签收'}`, active: !!log.arrivalDate },
  ]
  return nodes
})

const fillExample = (code) => { traceCode.value = code; handleQuery() }

onMounted(() => {
  const code = route.query.code
  if (code) {
    traceCode.value = code
    handleQuery()
  }
})

const validateTraceCode = (code) => {
  if (!code || !code.trim()) return { valid: false, message: '请输入溯源码' }
  if (!traceCodePattern.test(code.trim().toUpperCase())) return { valid: false, message: '溯源码格式不正确，正确格式如：AGRI20260801DLN001ABC' }
  return { valid: true }
}

const handleQuery = async () => {
  const code = traceCode.value.trim().toUpperCase()
  traceCode.value = code
  const { valid, message } = validateTraceCode(code)
  if (!valid) { ElMessage.warning(message); return }
  loading.value = true; queryError.value = false; result.value = null
  try {
    const res = await traceQueryApi.query(code)
    if (res.data) result.value = res.data
    else { queryError.value = true; errorMessage.value = '未找到该溯源码对应的信息，请检查输入是否正确' }
  } catch { queryError.value = true; errorMessage.value = '查询失败，请稍后重试' }
  finally { loading.value = false }
}
</script>

<style scoped>
.trace-page { max-width: 900px; margin: 0 auto; }

/* Search Hero */
.search-hero { position: relative; overflow: hidden; margin-bottom: var(--space-2xl); border-radius: var(--radius-lg); }
.hero-bg { position: absolute; inset: 0; background: linear-gradient(135deg, #0f3d24 0%, #1a5d3a 35%, #2d8e5a 70%, #4a9060 100%); }
.hero-bg::after { content: ''; position: absolute; inset: 0; background: radial-gradient(ellipse at 70% 40%, rgba(255,255,255,0.15) 0%, transparent 60%); }
.hero-content { position: relative; z-index: 1; padding: var(--space-2xl) var(--space-xl); text-align: center; color: white; }
.hero-label { font-size: 11px; font-weight: 500; letter-spacing: 0.3em; text-transform: uppercase; opacity: 0.7; margin-bottom: var(--space-md); }
.hero-content h1 { font-family: var(--font-heading); font-size: 32px; font-weight: 700; margin-bottom: var(--space-md); }
.hero-rule { width: 60px; height: 2px; background: rgba(255,255,255,0.5); margin: 0 auto var(--space-md); }
.hero-content > p { font-size: 14px; opacity: 0.85; margin-bottom: var(--space-xl); line-height: 1.7; }

.search-bar { display: flex; gap: 0; max-width: 540px; margin: 0 auto; }
.search-btn {
  background: white; color: var(--color-primary-dark);
  padding: 0 28px; font-size: 14px; font-weight: 600;
  border: none; border-radius: 0 var(--radius-md) var(--radius-md) 0;
  cursor: pointer; transition: all 0.25s var(--ease-out); white-space: nowrap;
}
.search-btn:hover { background: var(--color-primary-bg); }
.search-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.search-hint { margin-top: var(--space-lg); display: flex; align-items: center; justify-content: center; gap: 12px; flex-wrap: wrap; }
.hint-text { font-size: 12px; opacity: 0.6; }
.example-link { font-family: monospace; font-size: 13px; color: rgba(255,255,255,0.9); cursor: pointer; text-decoration: underline; text-underline-offset: 3px; transition: color 0.3s; }
.example-link:hover { color: white; }
.hint-sep { color: rgba(255,255,255,0.3); }

/* Error State */
.error-state { text-align: center; padding: var(--space-2xl) 20px; background: var(--color-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.error-state h3 { font-family: var(--font-heading); font-size: 20px; font-weight: 700; color: var(--color-title); margin-bottom: 8px; }
.error-state p { color: var(--color-text-secondary); font-size: 14px; }

/* Batch Summary */
.batch-summary { display: flex; align-items: center; justify-content: space-between; padding: var(--space-lg); background: var(--color-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); margin-bottom: var(--space-lg); }
.batch-label { font-size: 12px; color: var(--color-text-hint); margin-bottom: 6px; }
.batch-code { font-family: monospace; font-size: 20px; font-weight: 600; color: var(--color-primary); }
.batch-status-label { font-size: 12px; color: var(--color-text-hint); margin-bottom: 6px; }
.batch-status { font-family: var(--font-heading); font-size: 18px; font-weight: 700; color: var(--color-primary); }

/* Timeline Stages */
.trace-timeline { position: relative; }

.timeline-stage { background: var(--color-card); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); overflow: hidden; }

.stage-header { display: flex; align-items: center; gap: var(--space-md); padding: var(--space-md); background: var(--color-bg); }
.stage-icon { width: 48px; height: 48px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; flex-shrink: 0; }
.stage-icon-green { background: var(--color-primary); }
.stage-icon-blue { background: var(--color-info); }
.stage-icon-orange { background: var(--color-accent); }

.stage-info { flex: 1; }
.stage-number { font-size: 12px; color: var(--color-text-hint); font-family: var(--font-number); font-weight: 600; }
.stage-title { font-family: var(--font-heading); font-size: 18px; font-weight: 700; color: var(--color-title); }
.stage-time { font-size: 12px; color: var(--color-text-hint); }

.stage-content { padding: var(--space-md); }

.detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0; }
.detail-item { display: flex; flex-direction: column; gap: 4px; padding: 10px 16px; }
.detail-label { font-size: 12px; color: var(--color-text-hint); }
.detail-value { font-size: 14px; color: var(--color-text); }
.detail-value.price { color: var(--color-danger); font-weight: 600; font-family: var(--font-number); }

.qualified-tag { display: inline-block; font-size: 12px; font-weight: 600; padding: 2px 8px; border-radius: var(--radius-sm); }
.qualified-tag.pass { color: var(--color-primary); background: var(--color-primary-bg); }
.qualified-tag.fail { color: var(--color-danger); background: rgba(229,57,53,0.1); }

.data-pending { text-align: center; padding: var(--space-lg); color: var(--color-text-hint); font-size: 14px; font-style: italic; }

/* Quality Table */
.quality-table { width: 100%; border-collapse: collapse; }
.quality-table th, .quality-table td { padding: 10px 12px; text-align: left; border-bottom: 1px solid var(--color-border-light); font-size: 13px; }
.quality-table th { background: var(--color-primary-bg); color: var(--color-primary-dark); font-weight: 600; }

/* Logistics */
.logistics-info { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0; margin-bottom: var(--space-md); }
.logistics-track { padding-left: var(--space-sm); border-top: 1px solid var(--color-border-light); padding-top: var(--space-md); }
.track-node { display: flex; gap: var(--space-sm); padding-bottom: var(--space-md); position: relative; }
.track-node::before { content: ''; position: absolute; left: 5px; top: 12px; bottom: 0; width: 2px; background: var(--color-border); }
.track-node:last-child::before { display: none; }
.track-dot { width: 12px; height: 12px; border-radius: 50%; background: var(--color-border); flex-shrink: 0; margin-top: 4px; transition: all 0.3s; }
.track-dot.active { background: var(--color-primary); box-shadow: 0 0 0 4px var(--color-primary-bg); }
.track-info { display: flex; flex-direction: column; gap: 2px; }
.track-time { font-size: 12px; color: var(--color-text-hint); }
.track-desc { font-size: 14px; color: var(--color-text); }

/* Connector */
.timeline-connector { width: 2px; height: 24px; background: var(--color-border); margin: 0 auto; }

/* Empty State */
.empty-state { text-align: center; padding: var(--space-2xl) 20px; }
.empty-icon { color: var(--color-border); margin-bottom: var(--space-md); }
.empty-title { font-family: var(--font-heading); font-size: 18px; color: var(--color-title); font-weight: 700; }
.empty-hint { font-size: 13px; color: var(--color-text-secondary); margin-top: 8px; }

@media (max-width: 768px) {
  .search-hero { border-radius: 0; }
  .hero-content { padding: var(--space-lg) var(--space-md); }
  .hero-content h1 { font-size: 24px; }
  .search-bar { flex-direction: column; gap: 8px; }
  .search-btn { padding: 12px 0; text-align: center; border-radius: var(--radius-md); }
  .detail-grid { grid-template-columns: 1fr; }
  .logistics-info { grid-template-columns: 1fr; }
  .batch-summary { flex-direction: column; gap: 12px; text-align: center; }
  .quality-table { font-size: 12px; }
  .quality-table th, .quality-table td { padding: 8px 6px; }
}
</style>
