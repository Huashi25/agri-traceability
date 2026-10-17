<template>
  <div class="knowledge-manage">
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 新建文章</el-button>
    </div>

    <div class="table-card">
      <el-table :data="list" style="width: 100%">
        <el-table-column label="标题" min-width="200">
          <template #default="{ row }">
            <div class="title-cell">
              <span class="article-title">{{ row.title }}</span>
              <span v-if="row.coverImage" class="has-cover">有封面</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            <el-tag :color="categoryColors[row.category]" effect="dark" size="small">
              {{ categoryLabels[row.category] || row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" width="150">
          <template #default="{ row }">
            <div class="tags-cell">{{ row.tags || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" width="80" align="center">
          <template #default="{ row }">{{ row.viewCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark" size="small">
              {{ row.status === 1 ? '发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="60" align="center">
          <template #default="{ row }">{{ row.sort || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无文章" /></template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑文章' : '新建文章'" width="640px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入文章标题" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.category" style="width: 100%">
                <el-option v-for="(label, key) in categoryLabels" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImage" placeholder="封面图片URL（可选）" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="请输入文章摘要" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入文章内容" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="标签">
              <el-input v-model="form.tags" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="发布" inactive-text="草稿" />
            </el-form-item>
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
import { knowledgeApi } from '../../api'

const list = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({})

const categoryLabels = {
  SELECTION: '选购指南',
  NUTRITION: '营养价值',
  STORAGE: '储存保鲜',
  USAGE: '食用方法',
  GENERAL: '农业常识'
}
const categoryColors = {
  SELECTION: '#1a5d3a',
  NUTRITION: '#2d8c3c',
  STORAGE: '#3aa856',
  USAGE: '#67c23a',
  GENERAL: '#529b2e'
}

const loadList = async () => {
  const res = await knowledgeApi.adminList()
  list.value = res.data
}

const openDialog = (row) => {
  if (row) { editId.value = row.id; form.value = { ...row } }
  else {
    editId.value = null
    form.value = { title: '', category: 'GENERAL', summary: '', content: '', coverImage: '', tags: '', sort: 0, status: 1 }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.title) { ElMessage.warning('请输入标题'); return }
  if (!form.value.content) { ElMessage.warning('请输入内容'); return }
  if (editId.value) await knowledgeApi.update(form.value)
  else await knowledgeApi.save(form.value)
  ElMessage.success('保存成功'); dialogVisible.value = false; loadList()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该文章?', '提示', { type: 'warning' })
  await knowledgeApi.delete(id); ElMessage.success('删除成功'); loadList()
}

onMounted(loadList)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.table-card { overflow-x: auto; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.title-cell { display: flex; align-items: center; gap: 8px; }
.article-title { font-size: 14px; font-weight: 600; color: #333; }
.has-cover { font-size: 11px; color: #1a5d3a; background: #e8f3ec; padding: 1px 6px; border-radius: 4px; flex-shrink: 0; }
.tags-cell { font-size: 12px; color: #999; }
:deep(.el-button--primary) { background-color: #1a5d3a; border-color: #1a5d3a; }
:deep(.el-button--primary:hover) { background-color: #2d8c3c; border-color: #2d8c3c; }
:deep(.el-table th.el-table__cell) { background-color: #f5f8f6; color: #1a5d3a; }
</style>
