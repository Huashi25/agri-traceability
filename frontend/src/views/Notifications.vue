<template>
  <div class="notifications-page">
    <div class="page-hero">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <h1 class="page-title">消息通知</h1>
        <p class="page-subtitle">查看您的订单、溯源、活动等通知消息</p>
      </div>
    </div>

    <div class="notif-toolbar">
      <div class="filter-tabs">
        <div class="filter-tab" :class="{ active: filter === 'all' }" @click="filter = 'all'">全部 ({{ notifications.length }})</div>
        <div class="filter-tab" :class="{ active: filter === 'unread' }" @click="filter = 'unread'">未读 ({{ unreadList.length }})</div>
        <div class="filter-tab" :class="{ active: filter === 'read' }" @click="filter = 'read'">已读 ({{ readList.length }})</div>
      </div>
      <el-button size="small" @click="markAllRead" :disabled="unreadList.length === 0">
        全部标为已读
      </el-button>
    </div>

    <div v-loading="loading" class="notif-list">
      <div v-if="filteredList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无通知" />
      </div>
      <div v-for="n in filteredList" :key="n.id"
        class="notif-row" :class="{ unread: n.isRead === 0 }"
        @click="handleClick(n)">
        <div class="notif-icon" :class="'type-' + n.type">
          <span v-if="n.type === 0">📢</span>
          <span v-else-if="n.type === 1">📦</span>
          <span v-else-if="n.type === 2">🔍</span>
          <span v-else>🎁</span>
        </div>
        <div class="notif-body">
          <div class="notif-top">
            <span class="notif-title">{{ n.title }}</span>
            <span class="notif-time">{{ formatTime(n.createTime) }}</span>
          </div>
          <p class="notif-content">{{ n.content }}</p>
          <div class="notif-bottom">
            <span class="notif-type-tag">{{ typeLabel(n.type) }}</span>
            <div class="notif-actions">
              <span v-if="n.linkUrl" class="notif-link">查看详情 →</span>
              <span class="notif-delete" @click.stop="deleteNotif(n.id)">
                <el-icon><Delete /></el-icon>
              </span>
            </div>
          </div>
        </div>
        <div class="notif-status-dot" v-if="n.isRead === 0"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { notificationApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const notifications = ref([])
const filter = ref('all')

const unreadList = computed(() => notifications.value.filter(n => n.isRead === 0))
const readList = computed(() => notifications.value.filter(n => n.isRead === 1))
const filteredList = computed(() => {
  if (filter.value === 'unread') return unreadList.value
  if (filter.value === 'read') return readList.value
  return notifications.value
})

const typeLabel = (type) => {
  const labels = ['系统通知', '订单消息', '溯源消息', '活动推广']
  return labels[type] || '通知'
}

const formatTime = (time) => {
  if (!time) return ''
  const d = new Date(time.replace(/-/g, '/'))
  const now = new Date()
  const diff = (now - d) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + '分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + '小时前'
  if (diff < 2592000) return Math.floor(diff / 86400) + '天前'
  return time.substring(0, 10)
}

const loadNotifications = async () => {
  loading.value = true
  try {
    const res = await notificationApi.list()
    notifications.value = res.data || []
  } catch {
    ElMessage.error('加载通知失败')
  } finally {
    loading.value = false
  }
}

const handleClick = async (n) => {
  if (n.isRead === 0) {
    try {
      await notificationApi.markRead(n.id)
      n.isRead = 1
    } catch {}
  }
  if (n.linkUrl) router.push(n.linkUrl)
}

const markAllRead = async () => {
  try {
    await notificationApi.markAllRead()
    notifications.value.forEach(n => n.isRead = 1)
    ElMessage.success('已全部标为已读')
  } catch {
    ElMessage.error('操作失败')
  }
}

const deleteNotif = async (id) => {
  try {
    await notificationApi.delete(id)
    notifications.value = notifications.value.filter(n => n.id !== id)
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped>
.notifications-page { max-width: 800px; margin: 0 auto; padding-bottom: 40px; }

.page-hero { position: relative; border-radius: 16px; overflow: hidden; margin-bottom: 24px; }
.hero-bg { position: absolute; inset: 0; background: linear-gradient(135deg, #1a5d3a, #2d8c3c); }
.hero-content { position: relative; z-index: 1; padding: 32px 40px; color: #fff; }
.page-title { font-size: 24px; font-weight: 800; margin-bottom: 6px; }
.page-subtitle { font-size: 13px; opacity: 0.85; }

.notif-toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.filter-tabs { display: flex; gap: 4px; }
.filter-tab {
  padding: 6px 16px; font-size: 13px; border-radius: 9999px;
  background: #f5f5f5; color: #666; cursor: pointer;
  transition: all 0.2s;
}
.filter-tab.active { background: #1a5d3a; color: #fff; }

.notif-list { display: flex; flex-direction: column; gap: 8px; }
.empty-state { padding: 60px 0; }

.notif-row {
  display: flex; gap: 14px; padding: 16px 20px;
  background: #fff; border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  cursor: pointer; transition: all 0.2s;
  position: relative; border: 1px solid #f0f0f0;
}
.notif-row:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); transform: translateY(-1px); }
.notif-row.unread { border-left: 3px solid #2d8c3c; }

.notif-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; flex-shrink: 0;
}
.notif-icon.type-0 { background: #e3f2fd; }
.notif-icon.type-1 { background: #fff3e0; }
.notif-icon.type-2 { background: #e8f5e9; }
.notif-icon.type-3 { background: #fce4ec; }

.notif-body { flex: 1; min-width: 0; }
.notif-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.notif-title { font-size: 14px; font-weight: 600; color: #333; }
.notif-time { font-size: 12px; color: #bbb; flex-shrink: 0; }
.notif-content { font-size: 13px; color: #777; line-height: 1.6; margin-bottom: 8px; }
.notif-bottom { display: flex; justify-content: space-between; align-items: center; }
.notif-type-tag { font-size: 11px; padding: 2px 8px; border-radius: 4px; background: #f5f5f5; color: #999; }
.notif-actions { display: flex; align-items: center; gap: 12px; }
.notif-link { font-size: 12px; color: #2d8c3c; }
.notif-delete { color: #ccc; cursor: pointer; transition: color 0.2s; display: flex; }
.notif-delete:hover { color: #e74c3c; }
.notif-status-dot { position: absolute; top: 16px; right: 16px; width: 8px; height: 8px; border-radius: 50%; background: #e74c3c; }

@media (max-width: 600px) {
  .hero-content { padding: 24px 20px; }
  .notif-toolbar { flex-direction: column; gap: 12px; align-items: flex-start; }
  .notif-row { padding: 14px; }
}
</style>
