<template>
  <div class="product-manage">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchKey" placeholder="搜索商品名称..." clearable style="width: 240px"
          @input="filterList">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 添加商品
      </el-button>
    </div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat-chip">
        <span class="chip-label">全部商品</span>
        <span class="chip-num">{{ list.length }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">已上架</span>
        <span class="chip-num green">{{ list.filter(i => i.status === 1).length }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">已下架</span>
        <span class="chip-num gray">{{ list.filter(i => i.status === 0).length }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">总库存</span>
        <span class="chip-num">{{ list.reduce((s, i) => s + (i.stock || 0), 0) }}</span>
      </div>
    </div>

    <!-- Table -->
    <div class="table-card">
      <el-table :data="filteredList" v-loading="loading" style="width: 100%">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="product-cell">
              <div class="product-thumb">
                <img :src="getProductImage(row)" :alt="row.name" />
              </div>
              <div class="product-meta">
                <div class="product-name">{{ row.name }}</div>
                <div class="product-desc">{{ row.description || '暂无描述' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getCategoryName(row.categoryId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="140">
          <template #default="{ row }">
            <div class="price-cell">
              <span class="price-text">￥{{ Number(row.price || 0).toFixed(2) }}</span>
              <span class="price-cross" v-if="row.originalPrice">￥{{ Number(row.originalPrice || 0).toFixed(2) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="100" prop="stock" />
        <el-table-column label="销量" width="90">
          <template #default="{ row }">{{ row.salesCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="评分" width="90">
          <template #default="{ row }">
            <el-rate :model-value="Number(row.rating) || 0" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="产地" width="120">
          <template #default="{ row }">{{ row.origin || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" size="small"
              @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无商品" />
        </template>
      </el-table>
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑商品' : '添加商品'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="商品名称"><el-input v-model="form.name" placeholder="请输入商品名称" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="价格">
              <el-input-number v-model="form.price" :precision="2" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :precision="2" :min="0" placeholder="划线价" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="库存">
              <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产地"><el-input v-model="form.origin" placeholder="如：海南三亚" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="商品图片">
          <el-upload
            class="product-uploader"
            action="#"
            :show-file-list="false"
            :http-request="handleUpload"
            accept="image/jpeg,image/png,image/gif,image/webp"
          >
            <img v-if="form.image" :src="form.image" class="upload-preview" alt="商品图片" />
            <div v-else class="upload-placeholder">
              <el-icon size="28"><Plus /></el-icon>
              <span>点击上传图片</span>
            </div>
          </el-upload>
          <div class="upload-tip">支持 JPG/PNG/GIF/WEBP，不超过 5MB</div>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
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
import { productApi, categoryApi, uploadApi } from '../../api'

import durianImg from '../../assets/images/product-durian.jpg'
import orangeImg from '../../assets/images/product-orange.jpg'
import riceImg from '../../assets/images/product-rice.jpg'
import scallionImg from '../../assets/images/product-scallion.jpg'

const productImages = { '榴莲': durianImg, '脐橙': orangeImg, '大米': riceImg, '葱': scallionImg }
const getProductImage = (p) => {
  if (!p) return durianImg
  if (p.image) return p.image
  for (const key in productImages) { if (p.name?.includes(key)) return productImages[key] }
  return durianImg
}

const list = ref([])
const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editId = ref(null)
const searchKey = ref('')
const form = ref({})

const filteredList = computed(() => {
  if (!searchKey.value) return list.value
  return list.value.filter(i => i.name?.includes(searchKey.value))
})

const loadList = async () => {
  loading.value = true
  try {
    const res = await productApi.list({})
    list.value = res.data
  } finally { loading.value = false }
}

const loadCategories = async () => {
  const res = await categoryApi.list()
  categories.value = res.data
}

const getCategoryName = (id) => categories.value.find(c => c.id === id)?.name || '-'

const filterList = () => {}

const openDialog = (row) => {
  if (row) {
    editId.value = row.id
    form.value = { ...row }
  } else {
    editId.value = null
    form.value = { name: '', categoryId: null, price: 0, originalPrice: null, stock: 0, origin: '', description: '', status: 1 }
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.name) { ElMessage.warning('请输入商品名称'); return }
  if (editId.value) {
    await productApi.update(form.value)
  } else {
    await productApi.save(form.value)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadList()
}

const toggleStatus = async (row) => {
  await productApi.update({ id: row.id, status: row.status })
  ElMessage.success(row.status === 1 ? '已上架' : '已下架')
}

const uploading = ref(false)
const handleUpload = async (options) => {
  const formData = new FormData()
  formData.append('file', options.file)
  uploading.value = true
  try {
    const res = await uploadApi.image(formData)
    if (res.code === 200) {
      form.value.image = res.data
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该商品?', '提示', { type: 'warning' })
  await productApi.delete(id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => { loadList(); loadCategories() })
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }

.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-chip {
  background: white;
  border-radius: 10px;
  padding: 12px 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  flex: 1;
}
.chip-label { font-size: 12px; color: #999; }
.chip-num { font-size: 22px; font-weight: 700; color: #333; }
.chip-num.green { color: #67c23a; }
.chip-num.gray { color: #999; }

.table-card {
  background: white;
  border-radius: 12px;
  overflow-x: auto;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.product-cell { display: flex; align-items: center; gap: 12px; }
.product-thumb { width: 50px; height: 50px; border-radius: 8px; overflow: hidden; flex-shrink: 0; background: #f9f9f9; }
.product-thumb img { width: 100%; height: 100%; object-fit: cover; }
.product-meta { min-width: 0; }
.product-name { font-size: 14px; font-weight: 600; color: #333; }
.product-desc { font-size: 12px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.price-text { font-size: 16px; font-weight: 700; color: #f56c6c; }
.price-cell { display: flex; flex-direction: column; gap: 2px; }
.price-cross { font-size: 12px; color: #c0c0c0; text-decoration: line-through; }

.product-uploader { display: inline-block; }
.upload-preview { width: 120px; height: 120px; object-fit: cover; border-radius: 8px; border: 1px solid #e0e0e0; }
.upload-placeholder {
  width: 120px; height: 120px;
  border: 1px dashed #c0c0c0; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 8px; color: #999; cursor: pointer; transition: border-color 0.3s;
}
.upload-placeholder:hover { border-color: #1a5d3a; color: #1a5d3a; }
.upload-tip { font-size: 12px; color: #999; margin-top: 4px; }
</style>
