<template>
  <div class="seasonal-manage">
    <!-- Toolbar -->
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 新建时令
      </el-button>
    </div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat-chip">
        <span class="chip-label">时令记录</span>
        <span class="chip-num">{{ list.length }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">旺季商品</span>
        <span class="chip-num green">{{ peakCount }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">覆盖月份</span>
        <span class="chip-num">{{ monthsCovered }}</span>
      </div>
    </div>

    <!-- Table -->
    <div class="table-card">
      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column label="商品" min-width="180">
          <template #default="{ row }">{{ getProductName(row.productId) }}</template>
        </el-table-column>
        <el-table-column label="月份" width="80">
          <template #default="{ row }">{{ row.month }}月</template>
        </el-table-column>
        <el-table-column label="是否旺季" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isPeak === 1 ? 'success' : 'danger'" effect="plain" size="small">
              {{ row.isPeak === 1 ? '旺季' : '非旺季' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="描述" prop="description" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无时令记录" />
        </template>
      </el-table>
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑时令' : '新建时令'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="商品">
          <el-select v-model="form.productId" placeholder="请选择商品" filterable style="width: 100%">
            <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="月份">
              <el-input-number v-model="form.month" :min="1" :max="12" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="旺季">
              <el-switch v-model="form.isPeak" :active-value="1" :inactive-value="0" active-text="旺季" inactive-text="非旺季" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="时令描述（可选）" />
        </el-form-item>
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
import { seasonalApi, productApi } from '../../api'

const list = ref([])
const products = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({})

const peakCount = computed(() => list.value.filter(i => i.isPeak === 1).length)
const monthsCovered = computed(() => new Set(list.value.map(i => i.month).filter(Boolean)).size)

const getProductName = (id) => {
  const p = products.value.find(p => p.id === id)
  return p ? p.name : '-'
}

const loadList = async () => {
  loading.value = true
  try {
    const res = await seasonalApi.adminList()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

const loadProducts = async () => {
  const res = await productApi.list()
  products.value = res.data
}

const openDialog = (row) => {
  if (row) {
    editId.value = row.id
    form.value = { ...row }
  } else {
    editId.value = null
    form.value = { productId: null, month: 1, isPeak: 1, description: '' }
  }
  dialogVisible.value = true
}

// 保存：seasonalApi.save 为 saveOrUpdate，新建与编辑统一调用
const handleSave = async () => {
  if (!form.value.productId) { ElMessage.warning('请选择商品'); return }
  if (!form.value.month) { ElMessage.warning('请选择月份'); return }
  await seasonalApi.save(form.value)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadList()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该时令记录?', '提示', { type: 'warning' })
  await seasonalApi.delete(id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => { loadList(); loadProducts() })
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.stats-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.stat-chip {
  background: white;
  border-radius: 10px;
  padding: 12px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  flex: 1;
}
.chip-label {
  font-size: 12px;
  color: #999;
}
.chip-num {
  font-size: 22px;
  font-weight: 700;
  color: #333;
}
.chip-num.green {
  color: #1a5d3a;
}

.table-card { overflow-x: auto;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
</style>
