<template>
  <div class="origin-manage">
    <!-- 工具栏：搜索 + 新增（农户/管理员可编辑） -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input
          v-model="searchKey"
          placeholder="搜索农场名称 / 农户姓名 / 产地位置..."
          clearable
          style="width: 300px"
          @input="applyFilter"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-tag v-if="isFarmer" type="success" effect="plain" size="large">农户模式：维护本农场产地信息</el-tag>
      </div>
      <el-button v-if="canEdit" type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 新增产地信息
      </el-button>
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <el-table :data="filteredList" v-loading="loading" style="width: 100%">
        <el-table-column label="批次编号" width="160">
          <template #default="{ row }">
            <span class="batch-no">{{ batchNoOf(row.batchId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="140">
          <template #default="{ row }">
            <span class="product-name-text">{{ productNameOf(row.batchId) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="farmName" label="农场名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="location" label="产地位置" min-width="170" show-overflow-tooltip />
        <el-table-column prop="farmerName" label="农户姓名" width="110" />
        <el-table-column label="种植日期" width="120">
          <template #default="{ row }">{{ formatDate(row.plantDate) }}</template>
        </el-table-column>
        <el-table-column label="采收日期" width="120">
          <template #default="{ row }">{{ formatDate(row.harvestDate) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="产地描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="录入时间" width="150">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">{{ canEdit ? '编辑' : '查看' }}</el-button>
            <el-button v-if="canEdit" size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无产地信息，请先在溯源管理中创建批次" />
        </template>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" :disabled="!canEdit">
        <!-- 关联批次：新增时必填，编辑时锁定（批次归属不可改，避免破坏溯源链） -->
        <el-form-item label="关联批次" prop="batchId">
          <el-select
            v-model="form.batchId"
            placeholder="请选择该产地信息所属的溯源批次"
            filterable
            :disabled="!!editId || !canEdit"
            style="width: 100%"
          >
            <el-option
              v-for="b in batchList"
              :key="b.id"
              :label="batchOptionLabel(b)"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="农场名称" prop="farmName">
          <el-input v-model="form.farmName" placeholder="如：云南蒙自石榴种植园" />
        </el-form-item>
        <el-form-item label="产地位置" prop="location">
          <el-input v-model="form.location" placeholder="如：云南省红河州蒙自市" />
        </el-form-item>
        <el-form-item label="农户名称" prop="farmerName">
          <el-input v-model="form.farmerName" placeholder="如：王建国" />
        </el-form-item>
        <!-- 两个日期并排，各自独立校验 -->
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="种植日期" prop="plantDate">
              <el-date-picker
                v-model="form.plantDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采收日期" prop="harvestDate">
              <el-date-picker
                v-model="form.harvestDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="产地描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="产地气候、土壤、种植方式等特色描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="canEdit" type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/**
 * OriginManage.vue —— 农户产地信息管理模块（管理端独立菜单页）
 *
 * 职责：对 origin_info 表做完整的增删改查（CRUD）。
 *   后端接口（已存在，本页不新增后端）：
 *     GET    /api/trace/origin/list?batchId=   → 列表（batchId 可选）
 *     POST   /api/trace/origin                 → 新增（saveOrUpdate）
 *     PUT    /api/trace/origin                 → 更新（updateById）
 *     DELETE /api/trace/origin/{id}            → 删除
 *   接口封装位置：src/api/index.js 的 originApi
 *
 * 权限口径（与后端 OriginInfoController 的 @RequireRole 保持一致）：
 *   - ADMIN / FARMER：可增删改
 *   - 其它角色：只读
 *
 * 数据关联说明：产地信息通过 batchId 挂在「溯源批次」下，
 * 所以页面需要同时加载批次列表（traceBatchApi）与商品列表（productApi）用于展示编号与商品名。
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { originApi, traceBatchApi, productApi } from '../../api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

// ---------- 权限 ----------
/** 当前角色是否可编辑（与后端 @RequireRole({"ADMIN","FARMER"}) 对齐） */
const canEdit = computed(() => ['ADMIN', 'FARMER'].includes(userStore.userInfo?.role || ''))
/** 是否农户角色（用于展示提示标签） */
const isFarmer = computed(() => userStore.userInfo?.role === 'FARMER')

// ---------- 列表数据 ----------
const list = ref([])            // 产地信息原始列表
const batchList = ref([])       // 溯源批次（用于下拉选择 + 编号/商品名映射）
const products = ref([])        // 商品（用于展示商品名）
const loading = ref(false)
const searchKey = ref('')       // 前端搜索关键词

// ---------- 对话框 ----------
const dialogVisible = ref(false)
const dialogTitle = ref('新增产地信息')
const editId = ref(null)        // 非空表示编辑模式
const saving = ref(false)
const formRef = ref(null)
const form = ref({})

/** 表单校验规则：与后端实体字段保持一致（OriginInfo 无额外约束，此处做前端必要校验） */
const rules = {
  batchId: [{ required: true, message: '请选择关联的溯源批次', trigger: 'change' }],
  farmName: [{ required: true, message: '请输入农场名称', trigger: 'blur' }],
  location: [{ required: true, message: '请输入产地位置', trigger: 'blur' }],
  farmerName: [{ required: true, message: '请输入农户名称', trigger: 'blur' }]
}

// ---------- 查询辅助 ----------
/**
 * 根据批次 id 找批次对象
 * @param {number} batchId 批次主键
 * @returns {object|null} 批次对象
 */
const findBatch = (batchId) => batchList.value.find(b => b.id === batchId) || null

/** 取批次编号用于表格展示（找不到时显示商品#id 风格占位） */
const batchNoOf = (batchId) => findBatch(batchId)?.batchNo || `批次#${batchId}`

/** 取批次对应商品名用于表格展示 */
const productNameOf = (batchId) => {
  const b = findBatch(batchId)
  if (!b) return '—'
  return products.value.find(p => p.id === b.productId)?.name || `商品#${b.productId}`
}

/** 下拉选项文案：批次编号 + 商品名，便于农户辨认 */
const batchOptionLabel = (b) => {
  const pname = products.value.find(p => p.id === b.productId)?.name || `商品#${b.productId}`
  return `${b.batchNo}（${pname}）`
}

/** 日期格式化：只取 YYYY-MM-DD */
const formatDate = (d) => d ? String(d).substring(0, 10) : '—'

/** 时间格式化：YYYY-MM-DD HH:mm */
const formatTime = (t) => t ? String(t).replace('T', ' ').substring(0, 16) : '—'

/** 前端搜索过滤：农场名 / 农户名 / 产地位置 三个字段模糊匹配 */
const filteredList = computed(() => {
  const key = searchKey.value.trim().toLowerCase()
  if (!key) return list.value
  return list.value.filter(r =>
    (r.farmName || '').toLowerCase().includes(key) ||
    (r.farmerName || '').toLowerCase().includes(key) ||
    (r.location || '').toLowerCase().includes(key)
  )
})

// ---------- 数据加载 ----------
/**
 * 加载产地信息列表
 * 逐批次查询：originApi.list(batchId) 支持不传 batchId 查全部，
 * 这里直接查全部再在前端做搜索过滤，减少请求次数。
 */
const loadList = async () => {
  loading.value = true
  try {
    const res = await originApi.list()
    list.value = res.data || []
  } catch (e) {
    ElMessage.error('加载产地信息失败')
    list.value = []
  } finally {
    loading.value = false
  }
}

/** 加载溯源批次（新增时需要选择归属批次） */
const loadBatches = async () => {
  try {
    const res = await traceBatchApi.list()
    batchList.value = res.data || []
  } catch (e) {
    batchList.value = []
  }
}

/** 加载商品（用于把批次映射成商品名） */
const loadProducts = async () => {
  try {
    const res = await productApi.list({})
    products.value = res.data || []
  } catch (e) {
    products.value = []
  }
}

// ---------- 交互 ----------
/**
 * 打开新增/编辑对话框
 * @param {object} [row] 传入则为编辑模式；不传为新增
 */
const openDialog = (row) => {
  if (row) {
    editId.value = row.id
    dialogTitle.value = canEdit.value ? '编辑产地信息' : '查看产地信息'
    // 浅拷贝，避免直接改动表格行数据
    form.value = { ...row }
  } else {
    editId.value = null
    dialogTitle.value = '新增产地信息'
    // 默认值：农户角色若有 farmerId 关联可预填农户名，这里留空由用户填写
    form.value = {
      batchId: null,
      farmName: '',
      location: '',
      farmerName: '',
      plantDate: null,
      harvestDate: null,
      description: ''
    }
  }
  dialogVisible.value = true
}

/**
 * 保存（新增走 POST，编辑走 PUT）
 * 后端 save 用 saveOrUpdate：主键为空即新增，非空即更新；
 * 这里显式区分接口，语义更清晰。
 */
const handleSave = async () => {
  // 先做表单校验，未通过直接返回
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 业务校验：采收日期不能早于种植日期
  if (form.value.plantDate && form.value.harvestDate &&
      form.value.harvestDate < form.value.plantDate) {
    ElMessage.warning('采收日期不能早于种植日期')
    return
  }

  saving.value = true
  try {
    if (editId.value) {
      await originApi.update(form.value)
    } else {
      await originApi.save(form.value)
    }
    ElMessage.success('产地信息已保存')
    dialogVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

/** 删除产地信息（前端二次确认） */
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该产地信息？删除后该批次的产地溯源信息将为空。', '提示', {
      type: 'warning',
      confirmButtonText: '确定删除',
      cancelButtonText: '取消'
    })
  } catch {
    return // 用户取消
  }
  try {
    await originApi.delete(id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

/** 搜索框输入后由 computed 自动过滤，这里保留空函数便于后续加防抖 */
const applyFilter = () => {}

// ---------- 生命周期 ----------
onMounted(() => {
  loadBatches()
  loadProducts()
  loadList()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.table-card { background: white; border-radius: 12px; padding: 16px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.batch-no { font-family: Consolas, Monaco, monospace; font-size: 13px; color: #2d8c3c; font-weight: 600; }
.product-name-text { color: #333; }
</style>
