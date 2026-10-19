<template>
  <div class="review-manage">
    <div class="toolbar">
      <el-select v-model="filterProductId" placeholder="筛选商品" clearable @change="loadList" style="width: 200px">
        <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-button :type="!filterStatus ? 'primary' : ''" @click="filterStatus = null; loadList()">全部</el-button>
      <el-button :type="filterStatus === 1 ? 'primary' : ''" @click="filterStatus = 1; loadList()">显示中</el-button>
      <el-button :type="filterStatus === 0 ? 'primary' : ''" @click="filterStatus = 0; loadList()">已隐藏</el-button>
    </div>

    <div class="stats-row">
      <div class="stat-chip"><span class="chip-label">总评价</span><span class="chip-num">{{ list.length }}</span></div>
      <div class="stat-chip"><span class="chip-label">显示中</span><span class="chip-num green">{{ list.filter(r => r.status === 1).length }}</span></div>
      <div class="stat-chip"><span class="chip-label">已隐藏</span><span class="chip-num gray">{{ list.filter(r => r.status === 0).length }}</span></div>
      <div class="stat-chip"><span class="chip-label">平均评分</span><span class="chip-num">{{ avgRating }}</span></div>
    </div>

    <div class="review-list">
      <div v-for="r in filteredList" :key="r.id" class="review-card">
        <div class="review-left">
          <div class="review-avatar" :style="{ background: avatarColor(r.userId) }">
            {{ (r.userName || '?').charAt(0).toUpperCase() }}
          </div>
        </div>
        <div class="review-body">
          <div class="review-header">
            <span class="review-user">{{ r.userName || '匿名用户' }}</span>
            <el-rate v-model="r.rating" disabled size="small" />
            <span class="review-product">{{ getProductName(r.productId) }}</span>
            <span class="review-time">{{ formatTime(r.createTime) }}</span>
          </div>
          <div class="review-content">{{ r.content || '未填写评价内容' }}</div>
        </div>
        <div class="review-actions">
          <el-button size="small" :type="r.status === 1 ? 'warning' : 'success'" @click="toggleStatus(r)">
            {{ r.status === 1 ? '隐藏' : '显示' }}
          </el-button>
          <el-button size="small" type="danger" circle @click="handleDelete(r.id)"><el-icon><Delete /></el-icon></el-button>
        </div>
      </div>
    </div>
    <el-empty v-if="filteredList.length === 0" description="暂无评价" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reviewApi, productApi } from '../../api'

const list = ref([])
const products = ref([])
const filterProductId = ref(null)
const filterStatus = ref(null)

const colors = ['linear-gradient(135deg,#409eff,#337ecc)','linear-gradient(135deg,#67c23a,#529b2e)','linear-gradient(135deg,#e6a23c,#b88230)','linear-gradient(135deg,#f56c6c,#c45656)']
const avatarColor = (id) => colors[(id - 1) % colors.length]

const filteredList = computed(() => {
  let result = list.value
  if (filterProductId.value) result = result.filter(r => r.productId === filterProductId.value)
  if (filterStatus.value !== null) result = result.filter(r => r.status === filterStatus.value)
  return result
})

const avgRating = computed(() => {
  if (list.value.length === 0) return '0.0'
  const sum = list.value.reduce((s, r) => s + (r.rating || 0), 0)
  return (sum / list.value.length).toFixed(1)
})

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 10) : ''

const loadList = async () => {
  // mine=true：后端按登录身份过滤，商家只拿到自己商品的评价，管理员仍是全量
  const res = await reviewApi.list(null, null, true)
  list.value = res.data
}

const loadProducts = async () => {
  // 商家只看自己商品，商品名映射也只需要自己的
  const res = await productApi.list({ mine: true })
  products.value = res.data
}

const getProductName = (id) => products.value.find(p => p.id === id)?.name || `商品#${id}`

const toggleStatus = async (r) => {
  await reviewApi.update({ id: r.id, status: r.status === 1 ? 0 : 1 })
  ElMessage.success(r.status === 1 ? '已隐藏' : '已显示')
  loadList()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该评价?', '提示', { type: 'warning' })
  await reviewApi.delete(id); ElMessage.success('删除成功'); loadList()
}

onMounted(() => { loadList(); loadProducts() })
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-chip { background: white; border-radius: 10px; padding: 12px 20px; display: flex; flex-direction: column; gap: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); flex: 1; }
.chip-label { font-size: 12px; color: #999; }
.chip-num { font-size: 22px; font-weight: 700; color: #333; }
.chip-num.green { color: #67c23a; } .chip-num.gray { color: #909399; }

.review-list { display: flex; flex-direction: column; gap: 12px; }
.review-card { background: white; border-radius: 12px; padding: 16px 20px; display: flex; gap: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: box-shadow 0.2s; }
.review-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.review-avatar { width: 40px; height: 40px; border-radius: 50%; color: white; display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 600; flex-shrink: 0; }
.review-body { flex: 1; }
.review-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; flex-wrap: wrap; }
.review-user { font-size: 14px; font-weight: 600; color: #333; }
.review-product { font-size: 12px; color: #999; background: #f5f5f5; padding: 2px 8px; border-radius: 4px; }
.review-time { font-size: 12px; color: #ccc; margin-left: auto; }
.review-content { font-size: 14px; color: #666; line-height: 1.6; }
.review-actions { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
</style>
