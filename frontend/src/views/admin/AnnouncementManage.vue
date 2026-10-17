<template>
  <div class="announcement-manage">
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 发布公告</el-button>
    </div>

    <div class="announcement-list">
      <div v-for="a in list" :key="a.id" class="announce-card">
        <div class="announce-left">
          <div class="announce-icon" :style="{ background: typeColors[a.type].bg, color: typeColors[a.type].text }">
            <el-icon size="20"><component :is="typeColors[a.type].icon" /></el-icon>
          </div>
        </div>
        <div class="announce-body">
          <div class="announce-header">
            <span class="announce-title">{{ a.title }}</span>
            <el-tag :type="a.type === 0 ? 'primary' : a.type === 1 ? 'warning' : 'info'" effect="plain" size="small">
              {{ typeLabels[a.type] }}
            </el-tag>
            <el-tag :type="a.status === 1 ? 'success' : 'info'" effect="dark" size="small">
              {{ a.status === 1 ? '已发布' : '草稿' }}
            </el-tag>
            <span class="announce-time">{{ formatTime(a.createTime) }}</span>
          </div>
          <div class="announce-content">{{ a.content }}</div>
        </div>
        <div class="announce-actions">
          <el-button size="small" @click="openDialog(a)">编辑</el-button>
          <el-button size="small" type="danger" circle @click="handleDelete(a.id)"><el-icon><Delete /></el-icon></el-button>
        </div>
      </div>
    </div>
    <el-empty v-if="list.length === 0" description="暂无公告" />

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑公告' : '发布公告'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入公告标题" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入公告内容" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="系统公告" :value="0" /><el-option label="活动" :value="1" /><el-option label="通知" :value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="发布" inactive-text="草稿" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { announcementApi } from '../../api'

const list = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({})

const typeLabels = ['系统公告', '活动', '通知']
const typeColors = [
  { bg: '#ecf5ff', text: '#409eff', icon: 'Bell' },
  { bg: '#fdf6ec', text: '#e6a23c', icon: 'Present' },
  { bg: '#f4f4f5', text: '#909399', icon: 'ChatLineSquare' }
]

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

const loadList = async () => {
  const res = await announcementApi.list()
  list.value = res.data
}

const openDialog = (row) => {
  if (row) { editId.value = row.id; form.value = { ...row } }
  else { editId.value = null; form.value = { title: '', content: '', type: 0, status: 1, sort: 0 } }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.title) { ElMessage.warning('请输入标题'); return }
  if (editId.value) await announcementApi.update(form.value)
  else await announcementApi.save(form.value)
  ElMessage.success('保存成功'); dialogVisible.value = false; loadList()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该公告?', '提示', { type: 'warning' })
  await announcementApi.delete(id); ElMessage.success('删除成功'); loadList()
}

onMounted(loadList)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.announcement-list { display: flex; flex-direction: column; gap: 12px; }
.announce-card { background: white; border-radius: 12px; padding: 16px 20px; display: flex; gap: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: box-shadow 0.2s; }
.announce-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.06); }
.announce-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.announce-body { flex: 1; min-width: 0; }
.announce-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.announce-title { font-size: 15px; font-weight: 600; color: #333; }
.announce-time { font-size: 12px; color: #ccc; margin-left: auto; }
.announce-content { font-size: 14px; color: #666; line-height: 1.6; }
.announce-actions { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
</style>
