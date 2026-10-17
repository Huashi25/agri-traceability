<template>
  <div class="farmers-page">
    <div class="page-banner">
      <h1>产地直供</h1>
      <p>认识每件农产品背后的种植者，从源头了解您的食物</p>
    </div>

    <div class="farmer-grid" v-loading="loading">
      <div v-for="f in farmers" :key="f.id" class="farmer-card" @click="$router.push(`/farmer/${f.id}`)" style="cursor: pointer;">
        <div class="farmer-banner" :style="f.regionImage ? { backgroundImage: `linear-gradient(rgba(0,0,0,0.35), rgba(0,0,0,0.35)), url(${f.regionImage})`, backgroundSize: 'cover', backgroundPosition: 'center' } : { background: farmerColors[(f.id - 1) % farmerColors.length] }">
          <div class="farmer-avatar-large">
            {{ (f.name || '?').charAt(0) }}
          </div>
          <div class="farmer-status-badge">
            <el-icon><CircleCheck /></el-icon> 认证农户
          </div>
        </div>
        <div class="farmer-content">
          <h3 class="farmer-name">{{ f.name }}</h3>
          <div class="farmer-crop-tag">{{ f.mainCrop }}</div>
          <div class="farmer-info-list">
            <div class="farmer-info-item">
              <el-icon><Location /></el-icon>
              <div><span class="info-label">农场</span><span class="info-value">{{ f.farmName }}</span></div>
            </div>
            <div class="farmer-info-item">
              <el-icon><MapLocation /></el-icon>
              <div><span class="info-label">位置</span><span class="info-value">{{ f.location }}</span></div>
            </div>
            <div class="farmer-info-item">
              <el-icon><Phone /></el-icon>
              <div><span class="info-label">电话</span><span class="info-value">{{ f.phone || '未公开' }}</span></div>
            </div>
            <div class="farmer-info-item">
              <el-icon><Histogram /></el-icon>
              <div><span class="info-label">面积</span><span class="info-value">{{ f.farmArea }} 亩</span></div>
            </div>
          </div>
          <div class="farmer-desc-box">
            <p>{{ f.description || '该农户暂未填写简介' }}</p>
          </div>
          <div class="farmer-region-tags" v-if="f.regionTags">
            <el-tag v-for="tag in f.regionTags.split(',')" :key="tag" size="small" type="success" effect="plain" style="margin-right: 6px; margin-bottom: 4px;">{{ tag.trim() }}</el-tag>
          </div>
          <div class="farmer-region-desc" v-if="f.regionDescription">
            <el-icon><InfoFilled /></el-icon>
            <span>{{ f.regionDescription }}</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-if="!loading && farmers.length === 0" description="暂无农户信息" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { farmerApi } from '../api'

const farmers = ref([])
const loading = ref(false)

const farmerColors = [
  'linear-gradient(135deg, #409eff, #337ecc)',
  'linear-gradient(135deg, #67c23a, #529b2e)',
  'linear-gradient(135deg, #e6a23c, #b88230)',
  'linear-gradient(135deg, #f56c6c, #c45656)',
  'linear-gradient(135deg, #722ed1, #531dab)'
]

const loadFarmers = async () => {
  loading.value = true
  try {
    const res = await farmerApi.list()
    farmers.value = res.data.filter(f => f.status === 1)
  } finally { loading.value = false }
}

onMounted(loadFarmers)
</script>

<style scoped>
.farmers-page { max-width: 1200px; margin: 0 auto; }
.page-banner { background: linear-gradient(135deg, #1a5c25, #2d8c3c); border-radius: 16px; padding: 40px; margin-bottom: 24px; color: white; text-align: center; }
.page-banner h1 { font-size: 28px; margin-bottom: 8px; }
.page-banner p { font-size: 14px; opacity: 0.85; }

.farmer-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 20px; }
.farmer-card { background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: all 0.3s; }
.farmer-card:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.08); }
.farmer-banner { position: relative; height: 100px; display: flex; align-items: center; justify-content: center; }
.farmer-avatar-large { width: 72px; height: 72px; border-radius: 50%; background: rgba(255,255,255,0.25); border: 3px solid white; color: white; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 700; backdrop-filter: blur(10px); }
.farmer-status-badge { position: absolute; top: 12px; right: 12px; background: rgba(255,255,255,0.2); border: 1px solid rgba(255,255,255,0.3); border-radius: 20px; padding: 4px 12px; font-size: 12px; color: white; display: flex; align-items: center; gap: 4px; backdrop-filter: blur(10px); }
.farmer-content { padding: 20px; }
.farmer-name { font-size: 20px; font-weight: 700; color: #333; text-align: center; margin-bottom: 8px; }
.farmer-crop-tag { text-align: center; font-size: 13px; color: #2d8c3c; background: #f0f9eb; border-radius: 20px; padding: 4px 16px; display: inline-block; margin: 0 auto 16px; }
.farmer-info-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
.farmer-info-item { display: flex; align-items: center; gap: 10px; }
.farmer-info-item .el-icon { color: #2d8c3c; flex-shrink: 0; }
.farmer-info-item div { display: flex; gap: 6px; }
.info-label { font-size: 13px; color: #999; min-width: 36px; }
.info-value { font-size: 14px; color: #333; }
.farmer-desc-box { background: #fafafa; border-radius: 10px; padding: 14px; }
.farmer-desc-box p { font-size: 13px; color: #666; line-height: 1.7; }
.farmer-region-tags { margin-top: 10px; }
.farmer-region-desc { margin-top: 8px; display: flex; align-items: flex-start; gap: 6px; font-size: 12px; color: #1a5d3a; line-height: 1.6; padding: 10px 12px; background: #f0f7f3; border-radius: 8px; }
.farmer-region-desc .el-icon { flex-shrink: 0; margin-top: 2px; }
</style>
