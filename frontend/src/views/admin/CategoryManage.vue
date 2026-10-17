<template>
  <div class="category-manage">
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 添加分类
      </el-button>
    </div>

    <div class="category-grid">
      <div v-for="cat in list" :key="cat.id" class="category-card">
        <div class="cat-icon" :style="{ background: catColors[cat.id % catColors.length] }">
          <el-icon size="24" color="white"><Folder /></el-icon>
        </div>
        <div class="cat-info">
          <div class="cat-name">{{ cat.name }}</div>
          <div class="cat-desc">{{ cat.description || '暂无描述' }}</div>
          <div class="cat-meta">排序: {{ cat.sort || 0 }}</div>
        </div>
        <div class="cat-actions">
          <el-button size="small" @click="openDialog(cat)">编辑</el-button>
          <el-button size="small" type="danger" circle @click="handleDelete(cat.id)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>
      <div class="category-card add-card" @click="openDialog()">
        <el-icon size="32" color="#ccc"><Plus /></el-icon>
        <span class="add-text">添加分类</span>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑分类' : '添加分类'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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
import { categoryApi } from '../../api'

const list = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({})
const catColors = ['linear-gradient(135deg,#409eff,#337ecc)', 'linear-gradient(135deg,#67c23a,#529b2e)', 'linear-gradient(135deg,#e6a23c,#b88230)', 'linear-gradient(135deg,#f56c6c,#c45656)', 'linear-gradient(135deg,#722ed1,#531dab)']

const loadList = async () => { const res = await categoryApi.list(); list.value = res.data }

const openDialog = (row) => {
  if (row) { editId.value = row.id; form.value = { ...row } }
  else { editId.value = null; form.value = { name: '', description: '', sort: 0 } }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.name) { ElMessage.warning('请输入分类名称'); return }
  await categoryApi.save(editId.value ? { ...form.value, id: editId.value } : form.value)
  ElMessage.success('保存成功'); dialogVisible.value = false; loadList()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该分类?', '提示', { type: 'warning' })
  await categoryApi.delete(id); ElMessage.success('删除成功'); loadList()
}

onMounted(loadList)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
.category-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.category-card {
  background: white; border-radius: 12px; padding: 20px; display: flex; align-items: center; gap: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: all 0.2s;
}
.category-card:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(0,0,0,0.06); }
.cat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.cat-info { flex: 1; min-width: 0; }
.cat-name { font-size: 16px; font-weight: 600; color: #333; }
.cat-desc { font-size: 13px; color: #999; margin-top: 2px; }
.cat-meta { font-size: 12px; color: #ccc; margin-top: 4px; }
.cat-actions { display: flex; gap: 6px; flex-shrink: 0; }
.add-card { flex-direction: column; justify-content: center; cursor: pointer; border: 2px dashed #e0e0e0; }
.add-card:hover { border-color: #2d8c3c; }
.add-text { font-size: 14px; color: #999; margin-top: 8px; }
</style>
