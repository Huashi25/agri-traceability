<template>
  <div class="stock-warning">
    <!-- 工具栏：阈值可调，改完立即重查 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="toolbar-label">预警阈值（库存 ≤）</span>
        <el-input-number
          v-model="threshold"
          :min="0"
          :max="9999"
          :step="1"
          controls-position="right"
          style="width: 120px"
          @change="loadData"
        />
        <el-button :loading="loading" @click="loadData">刷新</el-button>
      </div>
      <el-button type="primary" @click="goProductManage">去商品管理补货</el-button>
    </div>

    <!-- 汇总 -->
    <div class="stats-row">
      <div class="stat-chip">
        <span class="chip-label">断货商品</span>
        <span class="chip-num red">{{ data.outCount || 0 }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">库存偏低</span>
        <span class="chip-num orange">{{ data.lowCount || 0 }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">合计需关注</span>
        <span class="chip-num blue">{{ needCount }}</span>
      </div>
    </div>

    <!-- 明细 -->
    <div class="table-card">
      <el-table :data="data.list || []" v-loading="loading" style="width: 100%">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="product-cell">
              <div class="product-thumb">
                <img v-if="row.image" :src="row.image" :alt="row.name" />
                <el-icon v-else :size="20" color="#c0c4cc"><Goods /></el-icon>
              </div>
              <div class="product-meta">
                <div class="product-name">{{ row.name }}</div>
                <div class="product-origin">{{ row.origin || '未填写产地' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="当前库存" width="110">
          <template #default="{ row }">
            <span class="stock-num" :class="row.level === 'OUT' ? 'danger' : 'warn'">{{ row.stock }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.level === 'OUT' ? 'danger' : 'warning'" effect="dark" size="small">
              {{ row.level === 'OUT' ? '已断货' : '库存偏低' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="单价" width="110">
          <template #default="{ row }">
            <span class="price-text">￥{{ Number(row.price || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="累计销量" width="110">
          <template #default="{ row }">{{ row.salesCount || 0 }} 件</template>
        </el-table-column>

        <el-table-column label="操作" width="130" fixed="right">
          <template #default>
            <el-button size="small" type="primary" plain @click="goProductManage">去补货</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="`当前没有库存 ≤ ${threshold} 的上架商品`" />
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { stockApi } from '../../api'

const router = useRouter()

const loading = ref(false)
/** 预警阈值，与后端默认值保持一致 */
const threshold = ref(10)
const data = ref({})

/** 需要关注的总数（断货 + 偏低） */
const needCount = computed(() => (data.value.outCount || 0) + (data.value.lowCount || 0))

/**
 * 加载低库存商品。
 * 后端已按当前身份收窄范围：商家只看自己的商品，管理员看全平台。
 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await stockApi.warning(threshold.value)
    data.value = res.data || {}
  } catch (e) {
    console.error('加载库存预警失败', e)
    ElMessage.error('加载库存预警失败')
  } finally {
    loading.value = false
  }
}

const goProductManage = () => {
  router.push('/admin/products')
}

onMounted(loadData)
</script>

<style scoped>
.stock-warning { display: flex; flex-direction: column; gap: 16px; }

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: white;
  padding: 14px 16px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.toolbar-label { font-size: 13px; color: #666; }

.stats-row { display: flex; gap: 12px; flex-wrap: wrap; }
.stat-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  background: white;
  padding: 12px 18px;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.chip-label { font-size: 13px; color: #888; }
.chip-num { font-size: 20px; font-weight: 700; color: #1a1a1a; }
.chip-num.red { color: #e53935; }
.chip-num.orange { color: #f59e0b; }
.chip-num.blue { color: #2196f3; }

.table-card {
  background: white;
  border-radius: 12px;
  padding: 8px 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.product-cell { display: flex; align-items: center; gap: 12px; }
.product-thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.product-thumb img { width: 100%; height: 100%; object-fit: cover; }
.product-meta { min-width: 0; }
.product-name { font-size: 14px; color: #1a1a1a; font-weight: 500; }
.product-origin { font-size: 12px; color: #999; margin-top: 2px; }

.stock-num { font-size: 16px; font-weight: 700; }
.stock-num.danger { color: #e53935; }
.stock-num.warn { color: #f59e0b; }
.price-text { color: #e65100; font-weight: 600; }
</style>
