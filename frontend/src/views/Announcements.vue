<template>
  <div class="announcement-page">
    <div class="page-banner">
      <h1>公告中心</h1>
      <p>平台最新动态、活动与通知</p>
    </div>

    <div class="filter-tabs">
      <div class="filter-tab" :class="{ active: !filterType }" @click="filterType = null">全部</div>
      <div class="filter-tab" :class="{ active: filterType === 0 }" @click="filterType = 0">
        <el-icon><Bell /></el-icon> 系统公告
      </div>
      <div class="filter-tab" :class="{ active: filterType === 1 }" @click="filterType = 1">
        <el-icon><Present /></el-icon> 活动
      </div>
      <div class="filter-tab" :class="{ active: filterType === 2 }" @click="filterType = 2">
        <el-icon><ChatLineSquare /></el-icon> 通知
      </div>
    </div>

    <div class="announcement-list">
      <div v-for="a in filteredList" :key="a.id" class="announce-card" @click="openDetail(a)">
        <div class="announce-type-icon" :style="{ background: typeStyles[a.type].bg, color: typeStyles[a.type].text }">
          <el-icon size="22"><component :is="typeStyles[a.type].icon" /></el-icon>
        </div>
        <div class="announce-main">
          <div class="announce-title-row">
            <span class="announce-title">{{ a.title }}</span>
            <el-tag :type="a.type === 0 ? 'primary' : a.type === 1 ? 'warning' : 'info'" effect="plain" size="small" round>
              {{ typeStyles[a.type].label }}
            </el-tag>
          </div>
          <p class="announce-preview">{{ a.content }}</p>
          <span class="announce-date">
            <el-icon><Clock /></el-icon> {{ a.createTime?.replace('T', ' ').substring(0, 16) }}
          </span>
        </div>
        <div class="announce-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>
    <el-empty v-if="filteredList.length === 0" description="暂无公告" />

    <el-dialog v-model="detailDialog" :title="current?.title" width="560px">
      <div class="detail-content" v-if="current">
        <div class="detail-tags">
          <el-tag :type="current.type === 0 ? 'primary' : current.type === 1 ? 'warning' : 'info'" effect="dark" round>
            {{ typeStyles[current.type].label }}
          </el-tag>
          <span class="detail-time">
            <el-icon><Clock /></el-icon> {{ current.createTime?.replace('T', ' ').substring(0, 16) }}
          </span>
        </div>
        <el-divider />
        <div class="detail-body">{{ current.content }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { announcementApi } from '../api'

const list = ref([])
const filterType = ref(null)
const detailDialog = ref(false)
const current = ref(null)

const typeStyles = [
  { label: '系统公告', bg: '#ecf5ff', text: '#409eff', icon: 'Bell' },
  { label: '活动', bg: '#fdf6ec', text: '#e6a23c', icon: 'Present' },
  { label: '通知', bg: '#f4f4f5', text: '#909399', icon: 'ChatLineSquare' }
]

const filteredList = computed(() => {
  if (filterType.value === null) return list.value
  return list.value.filter(a => a.type === filterType.value)
})

const loadList = async () => {
  const res = await announcementApi.list()
  list.value = res.data.filter(a => a.status === 1)
}

const openDetail = (a) => { current.value = a; detailDialog.value = true }

onMounted(loadList)
</script>

<style scoped>
.announcement-page { max-width: 900px; margin: 0 auto; }
.page-banner { background: linear-gradient(135deg, #1a5c25, #2d8c3c); border-radius: 16px; padding: 40px; margin-bottom: 24px; color: white; text-align: center; }
.page-banner h1 { font-size: 28px; margin-bottom: 8px; }
.page-banner p { font-size: 14px; opacity: 0.85; }

.filter-tabs { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.filter-tab { padding: 10px 20px; background: white; border-radius: 10px; font-size: 14px; color: #666; border: 1px solid #e8e8e8; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 6px; }
.filter-tab:hover { border-color: #67c23a; color: #2d8c3c; }
.filter-tab.active { background: #2d8c3c; color: white; border-color: #2d8c3c; }

.announcement-list { display: flex; flex-direction: column; gap: 12px; }
.announce-card { display: flex; align-items: center; gap: 16px; background: white; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); cursor: pointer; transition: all 0.2s; }
.announce-card:hover { transform: translateX(4px); box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.announce-type-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.announce-main { flex: 1; min-width: 0; }
.announce-title-row { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.announce-title { font-size: 15px; font-weight: 600; color: #333; }
.announce-preview { font-size: 13px; color: #999; margin-bottom: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.announce-date { font-size: 12px; color: #ccc; display: flex; align-items: center; gap: 4px; }
.announce-arrow { color: #ccc; flex-shrink: 0; }

.detail-content { padding: 8px; }
.detail-tags { display: flex; align-items: center; gap: 16px; }
.detail-time { font-size: 13px; color: #999; display: flex; align-items: center; gap: 4px; }
.detail-body { font-size: 15px; color: #555; line-height: 1.8; }
</style>
