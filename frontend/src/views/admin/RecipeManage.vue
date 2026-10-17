<template>
  <div class="recipe-manage">
    <!-- Toolbar -->
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 新建食谱
      </el-button>
    </div>

    <!-- Table -->
    <div class="table-card">
      <el-table :data="list" v-loading="loading" style="width: 100%">
        <el-table-column label="标题" prop="title" min-width="180" show-overflow-tooltip />
        <el-table-column label="分类" width="80">
          <template #default="{ row }">
            <el-tag :type="categoryTagType[row.category] || 'info'" effect="plain" size="small">
              {{ getCategoryLabel(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="难度" width="80">
          <template #default="{ row }">{{ getDifficultyLabel(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column label="烹饪时间" width="90">
          <template #default="{ row }">{{ row.cookTime || 0 }}分钟</template>
        </el-table-column>
        <el-table-column label="份量" width="60">
          <template #default="{ row }">{{ row.servings || 0 }}</template>
        </el-table-column>
        <el-table-column label="浏览量" width="80">
          <template #default="{ row }">{{ row.viewCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无食谱" />
        </template>
      </el-table>
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑食谱' : '新建食谱'" width="640px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入食谱标题" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
                <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度">
              <el-select v-model="form.difficulty" placeholder="选择难度" style="width: 100%">
                <el-option v-for="d in difficultyOptions" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="烹饪时间">
              <div class="input-with-unit">
                <el-input-number v-model="form.cookTime" :min="0" style="flex: 1" />
                <span class="unit-text">分钟</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="份量">
              <div class="input-with-unit">
                <el-input-number v-model="form.servings" :min="1" style="flex: 1" />
                <span class="unit-text">人份</span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="简短介绍这道菜" />
        </el-form-item>
        <el-form-item label="步骤">
          <el-input
            v-model="form.steps"
            type="textarea"
            :rows="8"
            placeholder='[{"step":1,"desc":"清洗食材"},{"step":2,"desc":"下锅翻炒"}]'
          />
          <div class="form-tip">步骤以 JSON 格式存储，例如：[{"step":1,"desc":"清洗食材"},{"step":2,"desc":"下锅翻炒"}]，可直接粘贴 JSON 文本。</div>
        </el-form-item>
        <el-form-item label="封面图">
          <el-input v-model="form.coverImage" placeholder="图片地址（可选）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" inactive-text="下架" />
        </el-form-item>
        <el-divider content-position="left">关联食材（可选）</el-divider>
        <div class="ingredients-section">
          <div class="ingredient-row" v-for="(ing, idx) in ingredients" :key="idx">
            <el-select v-model="ing.productId" placeholder="选择商品" filterable style="flex: 1" @click="loadProductsIfNeeded">
              <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
            <el-input v-model="ing.amount" placeholder="如: 200g" style="width: 120px" />
            <el-checkbox v-model="ing.isMain" :true-label="1" :false-label="0">主料</el-checkbox>
            <el-button type="danger" size="small" @click="ingredients.splice(idx, 1)" circle>
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button @click="addIngredient" type="primary" plain size="small" style="margin-top: 8px">
            <el-icon><Plus /></el-icon> 添加食材
          </el-button>
          <div class="form-tip">关联食材后，用户在食谱详情页可直接将食材加入购物车</div>
        </div>
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
import { recipeApi, productApi } from '../../api'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editId = ref(null)
const form = ref({})
const ingredients = ref([])
const productList = ref([])
const productsLoaded = ref(false)

const categoryOptions = [
  { value: 'DESSERT', label: '甜品' },
  { value: 'STAPLE', label: '主食' },
  { value: 'SOUP', label: '汤品' },
  { value: 'SIDE', label: '小菜' },
  { value: 'JUICE', label: '果汁' }
]
const difficultyOptions = [
  { value: 'SIMPLE', label: '简单' },
  { value: 'EASY', label: '入门' },
  { value: 'NORMAL', label: '普通' },
  { value: 'HARD', label: '挑战' }
]
const categoryTagType = {
  DESSERT: 'warning',
  STAPLE: 'primary',
  SOUP: 'info',
  SIDE: 'success',
  JUICE: 'danger'
}

const getCategoryLabel = (v) => categoryOptions.find(c => c.value === v)?.label || (v || '-')
const getDifficultyLabel = (v) => difficultyOptions.find(d => d.value === v)?.label || (v || '-')

const loadList = async () => {
  loading.value = true
  try {
    const res = await recipeApi.list()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

const loadProductsIfNeeded = async () => {
  if (!productsLoaded.value) {
    try {
      const res = await productApi.list({ pageSize: 999 })
      productList.value = res.data?.records || res.data || []
      productsLoaded.value = true
    } catch (e) {
      console.error('加载商品列表失败', e)
    }
  }
}

const addIngredient = () => {
  ingredients.value.push({ productId: null, amount: '', isMain: 0 })
}

const openDialog = async (row) => {
  await loadProductsIfNeeded()
  if (row) {
    editId.value = row.id
    form.value = { ...row }
    try {
      const res = await recipeApi.getProducts(row.id)
      ingredients.value = (res.data || []).map(p => ({
        productId: p.productId,
        amount: p.amount || '',
        isMain: p.isMain || 0
      }))
    } catch (e) {
      ingredients.value = []
    }
  } else {
    editId.value = null
    form.value = {
      title: '',
      category: 'DESSERT',
      difficulty: 'NORMAL',
      cookTime: 30,
      servings: 2,
      description: '',
      steps: '',
      coverImage: '',
      status: 1
    }
    ingredients.value = []
  }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.title) { ElMessage.warning('请输入食谱标题'); return }
  try {
    let recipeId = editId.value
    if (editId.value) {
      await recipeApi.update(form.value)
    } else {
      const res = await recipeApi.save(form.value)
      recipeId = res.data
    }
    if (recipeId && ingredients.value.length > 0) {
      const validIngredients = ingredients.value.filter(i => i.productId)
      if (validIngredients.length > 0) {
        await recipeApi.saveProducts(recipeId, validIngredients.map(i => ({
          recipeId: recipeId,
          productId: i.productId,
          amount: i.amount,
          isMainIngredient: i.isMain
        })))
      }
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该食谱?', '提示', { type: 'warning' })
  await recipeApi.delete(id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-card { overflow-x: auto;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.input-with-unit {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.unit-text {
  font-size: 13px;
  color: #999;
  flex-shrink: 0;
}

.form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.5;
}

.ingredients-section {
  padding: 0 0 0 90px;
}
.ingredient-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
</style>
