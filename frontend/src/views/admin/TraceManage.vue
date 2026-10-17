<template>
  <div class="trace-manage">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-info">
        <el-button type="primary" v-if="canCreateBatch" @click="openBatchDialog()">
          <el-icon><Plus /></el-icon> 创建溯源批次
        </el-button>
        <el-tag v-if="userRole === 'FARMER'" type="success" effect="plain" size="large">农户模式：管理产地与质检信息</el-tag>
        <el-tag v-else-if="userRole === 'MERCHANT'" type="warning" effect="plain" size="large">商家模式：管理物流信息</el-tag>
      </div>
    </div>

    <!-- Stats -->
    <div class="stats-row">
      <div class="stat-chip">
        <span class="chip-label">总批次</span>
        <span class="chip-num">{{ batchList.length }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">生产中</span>
        <span class="chip-num green">{{ batchList.filter(b => b.status === 0).length }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">质检中</span>
        <span class="chip-num blue">{{ batchList.filter(b => b.status === 1).length }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">运输中</span>
        <span class="chip-num orange">{{ batchList.filter(b => b.status === 2).length }}</span>
      </div>
      <div class="stat-chip">
        <span class="chip-label">已到货</span>
        <span class="chip-num gray">{{ batchList.filter(b => b.status === 3).length }}</span>
      </div>
    </div>

    <!-- Table -->
    <div class="table-card">
      <el-table :data="batchList" v-loading="loading" style="width: 100%">
        <el-table-column label="批次编号" width="180">
          <template #default="{ row }">
            <span class="batch-no">{{ row.batchNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品" min-width="140">
          <template #default="{ row }">
            <span class="product-name-text">{{ getProductName(row.productId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="溯源码" width="220">
          <template #default="{ row }">
            <div class="trace-code-cell">
              <code class="trace-code">{{ row.traceCode }}</code>
              <el-button text size="small" @click="copyCode(row.traceCode)">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="dark" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="溯源信息" width="280">
          <template #default="{ row }">
            <div class="trace-status-row">
              <el-tooltip content="产地信息" placement="top">
                <el-button size="small" :type="hasOrigin(row.id) ? 'success' : 'info'" circle @click="openOriginDialog(row)">
                  <el-icon><Location /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="质检信息" placement="top">
                <el-button size="small" :type="hasInspection(row.id) ? 'success' : 'info'" circle @click="openInspectionDialog(row)">
                  <el-icon><CircleCheck /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="物流信息" placement="top">
                <el-button size="small" :type="hasLogistics(row.id) ? 'success' : 'info'" circle @click="openLogisticsDialog(row)">
                  <el-icon><Van /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="二维码" placement="top">
                <el-button size="small" type="primary" circle @click="openQrDialog(row)">
                  <el-icon><Grid /></el-icon>
                </el-button>
              </el-tooltip>
              <el-button v-if="canDeleteBatch" size="small" type="danger" circle @click="handleDelete(row.id)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无溯源批次" />
        </template>
      </el-table>
    </div>

    <!-- 创建批次对话框 -->
    <el-dialog v-model="batchDialog" title="创建溯源批次" width="450px">
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="商品">
          <el-select v-model="batchForm.productId" placeholder="选择商品" filterable style="width: 100%">
            <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次编号">
          <el-input v-model="batchForm.batchNo" placeholder="留空自动生成" />
        </el-form-item>
        <el-form-item label="溯源码">
          <el-input v-model="batchForm.traceCode" placeholder="留空自动生成" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialog = false">取消</el-button>
        <el-button type="primary" @click="createBatch">创建</el-button>
      </template>
    </el-dialog>

    <!-- 产地信息对话框 -->
    <el-dialog v-model="originDialog" :title="canEditOrigin ? '产地信息' : '产地信息（只读）'" width="500px">
      <el-form :model="originForm" label-width="90px" :disabled="!canEditOrigin">
        <el-form-item label="农场名称"><el-input v-model="originForm.farmName" /></el-form-item>
        <el-form-item label="产地位置"><el-input v-model="originForm.location" /></el-form-item>
        <el-form-item label="农户名称"><el-input v-model="originForm.farmerName" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="种植日期"><el-date-picker v-model="originForm.plantDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="采收日期"><el-date-picker v-model="originForm.harvestDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="产地描述"><el-input v-model="originForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="originDialog = false">关闭</el-button>
        <el-button v-if="canEditOrigin" type="primary" @click="saveOrigin">保存</el-button>
      </template>
    </el-dialog>

    <!-- 质检信息对话框 -->
    <el-dialog v-model="inspectionDialog" :title="canEditInspection ? '质检信息' : '质检信息（只读）'" width="500px">
      <el-form :model="inspectionForm" label-width="90px" :disabled="!canEditInspection">
        <el-form-item label="质检员"><el-input v-model="inspectionForm.inspector" /></el-form-item>
        <el-form-item label="质检日期"><el-date-picker v-model="inspectionForm.inspectionDate" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="检测项目"><el-input v-model="inspectionForm.inspectionItem" /></el-form-item>
        <el-form-item label="检测结果"><el-input v-model="inspectionForm.result" /></el-form-item>
        <el-form-item label="是否合格">
          <el-switch v-model="inspectionForm.qualified" :active-value="1" :inactive-value="0" active-text="合格" inactive-text="不合格" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inspectionDialog = false">关闭</el-button>
        <el-button v-if="canEditInspection" type="primary" @click="saveInspection">保存</el-button>
      </template>
    </el-dialog>

    <!-- 物流信息对话框 -->
    <el-dialog v-model="logisticsDialog" :title="canEditLogistics ? '物流信息' : '物流信息（只读）'" width="500px">
      <el-form :model="logisticsForm" label-width="90px" :disabled="!canEditLogistics">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="发货日期"><el-date-picker v-model="logisticsForm.shipDate" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="到达日期"><el-date-picker v-model="logisticsForm.arrivalDate" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="发货地"><el-input v-model="logisticsForm.fromLocation" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="收货地"><el-input v-model="logisticsForm.toLocation" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="运输公司"><el-input v-model="logisticsForm.transportCompany" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="运输方式"><el-input v-model="logisticsForm.transportMethod" placeholder="如：冷链运输" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="冷链温度"><el-input v-model="logisticsForm.temperature" placeholder="如：2-8℃" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="logisticsDialog = false">关闭</el-button>
        <el-button v-if="canEditLogistics" type="primary" @click="saveLogistics">保存</el-button>
      </template>
    </el-dialog>

    <!-- QR Code Dialog -->
    <el-dialog v-model="qrDialog" title="溯源二维码" width="320px" align-center>
      <div class="qr-dialog-content" v-if="currentBatch">
        <div class="qr-product-name">{{ getProductName(currentBatch.productId) }}</div>
        <div class="qr-batch-code">批次: {{ currentBatch.batchNo }}</div>
        <div class="qr-trace-code">溯源码: {{ currentBatch.traceCode }}</div>
        <div class="qr-image-box">
          <QrCode :text="`${windowLocation}/trace?code=${currentBatch.traceCode}`" :size="200" />
        </div>
        <p class="qr-tip">扫码即可查看完整溯源信息</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { traceBatchApi, productApi, originApi, inspectionApi, logisticsApi } from '../../api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const userRole = computed(() => userStore.userInfo?.role || '')
const canCreateBatch = computed(() => ['ADMIN', 'FARMER'].includes(userRole.value))
const canDeleteBatch = computed(() => userRole.value === 'ADMIN')
const canEditOrigin = computed(() => ['ADMIN', 'FARMER'].includes(userRole.value))
const canEditInspection = computed(() => ['ADMIN', 'FARMER'].includes(userRole.value))
const canEditLogistics = computed(() => ['ADMIN', 'MERCHANT'].includes(userRole.value))

const batchList = ref([])
const products = ref([])
const loading = ref(false)
const originMap = ref({})
const inspectionMap = ref({})
const logisticsMap = ref({})

const batchDialog = ref(false)
const batchForm = ref({ productId: null, batchNo: '', traceCode: '' })
const originDialog = ref(false)
const originForm = ref({})
const inspectionDialog = ref(false)
const inspectionForm = ref({})
const logisticsDialog = ref(false)
const logisticsForm = ref({})

const statusText = (s) => ['生产中', '质检中', '运输中', '已到货'][s] || '未知'
const statusTagType = (s) => ['warning', 'primary', 'info', 'success'][s] || 'info'
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

const getProductName = (id) => products.value.find(p => p.id === id)?.name || `商品#${id}`
const hasOrigin = (batchId) => !!originMap.value[batchId]
const hasInspection = (batchId) => !!inspectionMap.value[batchId]
const hasLogistics = (batchId) => !!logisticsMap.value[batchId]

const loadBatches = async () => {
  loading.value = true
  try {
    const res = await traceBatchApi.list()
    batchList.value = res.data
    for (const b of batchList.value) {
      try {
        const o = await originApi.list(b.id); originMap.value[b.id] = o.data[0] || null
      } catch {}
      try {
        const i = await inspectionApi.list(b.id); inspectionMap.value[b.id] = i.data[0] || null
      } catch {}
      try {
        const l = await logisticsApi.list(b.id); logisticsMap.value[b.id] = l.data[0] || null
      } catch {}
    }
  } finally { loading.value = false }
}

const loadProducts = async () => {
  const res = await productApi.list({})
  products.value = res.data
}

const openBatchDialog = () => {
  batchForm.value = { productId: null, batchNo: '', traceCode: '' }
  batchDialog.value = true
}

const createBatch = async () => {
  if (!batchForm.value.productId) { ElMessage.warning('请选择商品'); return }
  try {
    const res = await traceBatchApi.create(batchForm.value)
    ElMessage.success(`创建成功，溯源码: ${res.data.traceCode}`)
    batchDialog.value = false
    loadBatches()
  } catch (e) {
    ElMessage.error('创建失败')
  }
}

const copyCode = (code) => {
  navigator.clipboard.writeText(code)
  ElMessage.success('溯源码已复制')
}

const openOriginDialog = (row) => {
  const existing = originMap.value[row.id]
  if (existing) { originForm.value = { ...existing } }
  else { originForm.value = { batchId: row.id, farmName: '', location: '', farmerName: '', plantDate: null, harvestDate: null, description: '' } }
  originDialog.value = true
}

const saveOrigin = async () => {
  try {
    await originApi.save(originForm.value)
    ElMessage.success('产地信息已保存')
    originDialog.value = false
    loadBatches()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const openInspectionDialog = (row) => {
  const existing = inspectionMap.value[row.id]
  if (existing) { inspectionForm.value = { ...existing } }
  else { inspectionForm.value = { batchId: row.id, inspector: '', inspectionDate: null, inspectionItem: '', result: '', qualified: 1 } }
  inspectionDialog.value = true
}

const saveInspection = async () => {
  try {
    await inspectionApi.save(inspectionForm.value)
    ElMessage.success('质检信息已保存')
    inspectionDialog.value = false
    loadBatches()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const openLogisticsDialog = (row) => {
  const existing = logisticsMap.value[row.id]
  if (existing) { logisticsForm.value = { ...existing } }
  else { logisticsForm.value = { batchId: row.id, shipDate: null, fromLocation: '', toLocation: '', transportCompany: '', transportMethod: '', temperature: '', arrivalDate: null } }
  logisticsDialog.value = true
}

const saveLogistics = async () => {
  try {
    await logisticsApi.save(logisticsForm.value)
    ElMessage.success('物流信息已保存')
    logisticsDialog.value = false
    loadBatches()
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该批次?', '提示', { type: 'warning' })
    await traceBatchApi.delete(id)
    ElMessage.success('删除成功')
    loadBatches()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => { loadBatches(); loadProducts() })
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }

.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-chip { background: white; border-radius: 10px; padding: 12px 20px; display: flex; flex-direction: column; gap: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); flex: 1; }
.chip-label { font-size: 12px; color: #999; }
.chip-num { font-size: 22px; font-weight: 700; color: #333; }
.chip-num.green { color: #67c23a; }
.chip-num.blue { color: #409eff; }
.chip-num.orange { color: #e6a23c; }
.chip-num.gray { color: #909399; }

.table-card { overflow-x: auto; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }

.batch-no { font-family: monospace; font-size: 13px; color: #666; }
.product-name-text { font-size: 14px; color: #333; font-weight: 500; }
.trace-code-cell { display: flex; align-items: center; gap: 4px; }
.trace-code { font-family: monospace; font-size: 12px; color: #409eff; background: #f0f7ff; padding: 2px 8px; border-radius: 4px; }
.trace-status-row { display: flex; gap: 6px; }
</style>
