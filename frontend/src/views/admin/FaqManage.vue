<template>
  <div class="faq-manage">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="filterCategory" placeholder="全部分类" clearable style="width: 180px" @change="loadList">
          <el-option label="全部分类" value="" />
          <el-option v-for="(label, key) in categoryLabels" :key="key" :label="label" :value="key" />
        </el-select>
      </div>
      <el-button type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 新建问题</el-button>
    </div>

    <div class="table-card">
      <el-table :data="filteredList" style="width: 100%">
        <el-table-column label="问题" min-width="250">
          <template #default="{ row }">
            <span class="question-text">{{ row.question }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag :color="categoryColors[row.category]" effect="dark" size="small">
              {{ categoryLabels[row.category] || row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="回答" min-width="200">
          <template #default="{ row }">
            <div class="answer-cell">{{ row.answer }}</div>
          </template>
        </el-table-column>
        <el-table-column label="浏览量" width="80" align="center">
          <template #default="{ row }">{{ row.viewCount || 0 }}</template>
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
        <template #empty><el-empty description="暂无问题" /></template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑问题' : '新建问题'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="(label, key) in categoryLabels" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题">
          <el-input v-model="form.question" placeholder="请输入问题" />
        </el-form-item>
        <el-form-item label="回答">
          <el-input v-model="form.answer" type="textarea" :rows="5" placeholder="请输入回答" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { faqApi } from '../../api'

const list = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const filterCategory = ref('')
const form = ref({})

const categoryLabels = {
  TRACE: '溯源查询',
  ORDER: '订单问题',
  PAYMENT: '支付问题',
  SHIPPING: '配送问题',
  AFTER_SALE: '售后问题',
  ACCOUNT: '账号问题'
}
const categoryColors = {
  TRACE: '#1a5d3a',
  ORDER: '#2d8c3c',
  PAYMENT: '#3aa856',
  SHIPPING: '#67c23a',
  AFTER_SALE: '#529b2e',
  ACCOUNT: '#3e8e57'
}

const filteredList = computed(() => {
  if (!filterCategory.value) return list.value
  return list.value.filter(f => f.category === filterCategory.value)
})

const loadList = async () => {
  const res = await faqApi.list()
  list.value = res.data
}

const openDialog = (row) => {
  if (row) { editId.value = row.id; form.value = { ...row } }
  else {
    editId.value = null
    form.value = { category: 'TRACE', question: '', answer: '', sort: 0, status: 1 }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.question) { ElMessage.warning('请输入问题'); return }
  if (!form.value.answer) { ElMessage.warning('请输入回答'); return }
  if (editId.value) await faqApi.update(form.value)
  else await faqApi.save(form.value)
  ElMessage.success('保存成功'); dialogVisible.value = false; loadList()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该问题?', '提示', { type: 'warning' })
  await faqApi.delete(id); ElMessage.success('删除成功'); loadList()
}

onMounted(loadList)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.table-card { overflow-x: auto; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.question-text { font-size: 14px; font-weight: 600; color: #333; }
.answer-cell { font-size: 13px; color: #666; line-height: 1.5; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; text-overflow: ellipsis; }
:deep(.el-button--primary) { background-color: #1a5d3a; border-color: #1a5d3a; }
:deep(.el-button--primary:hover) { background-color: #2d8c3c; border-color: #2d8c3c; }
:deep(.el-table th.el-table__cell) { background-color: #f5f8f6; color: #1a5d3a; }
</style>
