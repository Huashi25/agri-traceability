<template>
  <div class="farmer-manage">
    <div class="toolbar">
      <el-input v-if="isAdmin" v-model="searchKey" placeholder="搜索农户姓名..." clearable style="width: 240px" @input="loadList">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button v-if="isAdmin" type="primary" @click="openDialog()"><el-icon><Plus /></el-icon> 添加农户</el-button>
    </div>

    <div class="farmer-grid">
      <div v-for="f in list" :key="f.id" class="farmer-card">
        <div class="farmer-header">
          <div class="farmer-avatar" :style="{ background: avatarColor(f.id) }">
            {{ (f.name || '?').charAt(0) }}
          </div>
          <div class="farmer-meta">
            <div class="farmer-name">{{ f.name }}</div>
            <div class="farmer-crop">{{ f.mainCrop || '暂无作物信息' }}</div>
          </div>
          <el-tag :type="f.status === 1 ? 'success' : 'danger'" effect="dark" size="small">
            {{ f.status === 1 ? '正常' : '禁用' }}
          </el-tag>
        </div>
        <div class="farmer-body">
          <div class="info-row"><el-icon><Phone /></el-icon> {{ f.phone || '未填写' }}</div>
          <div class="info-row"><el-icon><Location /></el-icon> {{ f.farmName || '未填写' }}</div>
          <div class="info-row"><el-icon><MapLocation /></el-icon> {{ f.location || '未填写' }}</div>
          <div class="info-row"><el-icon><Histogram /></el-icon> {{ f.farmArea || 0 }} 亩</div>
          <div class="region-tags" v-if="f.regionTags">
            <el-tag v-for="tag in f.regionTags.split(',')" :key="tag" size="small" type="success" effect="plain" style="margin-right: 4px; margin-bottom: 4px;">{{ tag.trim() }}</el-tag>
          </div>
          <div class="info-desc">{{ f.description || '暂无简介' }}</div>
          <div class="region-desc" v-if="f.regionDescription">
            <el-icon><InfoFilled /></el-icon> {{ f.regionDescription }}
          </div>
        </div>
        <div class="farmer-actions">
          <el-button size="small" @click="openDialog(f)">编辑</el-button>
          <el-button v-if="isAdmin" size="small" type="danger" @click="handleDelete(f.id)">删除</el-button>
        </div>
      </div>
    </div>
    <el-empty v-if="list.length === 0" :description="isFarmer ? '您还未关联农场信息，请联系管理员' : '暂无农户信息'" />

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑农户' : '添加农户'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="农场名称"><el-input v-model="form.farmName" /></el-form-item>
        <el-form-item label="农场位置"><el-input v-model="form.location" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="面积(亩)"><el-input-number v-model="form.farmArea" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="主要作物"><el-input v-model="form.mainCrop" placeholder="如：榴莲、芒果" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-divider content-position="left">产地地图信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="经度"><el-input-number v-model="form.longitude" :precision="6" :step="0.0001" :controls="false" style="width:100%" placeholder="如: 109.5083" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="纬度"><el-input-number v-model="form.latitude" :precision="6" :step="0.0001" :controls="false" style="width:100%" placeholder="如: 18.3200" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="区域标签"><el-input v-model="form.regionTags" placeholder="逗号分隔，如：热带,全年无霜" /></el-form-item>
        <el-form-item label="产地描述"><el-input v-model="form.regionDescription" type="textarea" :rows="2" placeholder="产地气候、土壤等特色描述" /></el-form-item>
        <el-form-item label="产地图片"><el-input v-model="form.regionImage" placeholder="产地图片URL（可选）" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" /></el-form-item>
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
import { farmerApi } from '../../api'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.userInfo?.role === 'ADMIN')
const isFarmer = computed(() => userStore.userInfo?.role === 'FARMER')
const myFarmerId = computed(() => userStore.userInfo?.farmerId || null)

const list = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const searchKey = ref('')
const form = ref({})

const colors = ['linear-gradient(135deg,#409eff,#337ecc)','linear-gradient(135deg,#67c23a,#529b2e)','linear-gradient(135deg,#e6a23c,#b88230)','linear-gradient(135deg,#f56c6c,#c45656)','linear-gradient(135deg,#722ed1,#531dab)']
const avatarColor = (id) => colors[(id - 1) % colors.length]

const loadList = async () => {
  const res = await farmerApi.list(searchKey.value)
  if (isFarmer.value && myFarmerId.value) {
    list.value = res.data.filter(f => f.id === myFarmerId.value)
  } else if (isFarmer.value) {
    list.value = []
  } else {
    list.value = res.data
  }
}

const openDialog = (row) => {
  if (row) { editId.value = row.id; form.value = { ...row } }
  else { editId.value = null; form.value = { name: '', phone: '', farmName: '', location: '', farmArea: 0, mainCrop: '', description: '', longitude: null, latitude: null, regionTags: '', regionDescription: '', regionImage: '', status: 1 } }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.name) { ElMessage.warning('请输入姓名'); return }
  if (editId.value) await farmerApi.update(form.value)
  else await farmerApi.save(form.value)
  ElMessage.success('保存成功'); dialogVisible.value = false; loadList()
}

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该农户?', '提示', { type: 'warning' })
  await farmerApi.delete(id); ElMessage.success('删除成功'); loadList()
}

onMounted(loadList)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.farmer-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.farmer-card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); transition: all 0.2s; }
.farmer-card:hover { transform: translateY(-2px); box-shadow: 0 8px 16px rgba(0,0,0,0.06); }
.farmer-header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.farmer-avatar { width: 48px; height: 48px; border-radius: 12px; color: white; display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 600; flex-shrink: 0; }
.farmer-meta { flex: 1; }
.farmer-name { font-size: 16px; font-weight: 600; color: #333; }
.farmer-crop { font-size: 12px; color: #2d8c3c; margin-top: 2px; }
.farmer-body { display: flex; flex-direction: column; gap: 8px; }
.info-row { display: flex; align-items: center; gap: 8px; font-size: 13px; color: #666; }
.info-desc { font-size: 13px; color: #999; margin-top: 8px; line-height: 1.5; padding: 10px; background: #fafafa; border-radius: 8px; }
.region-tags { margin-top: 4px; }
.region-desc { font-size: 12px; color: #1a5d3a; margin-top: 8px; line-height: 1.5; display: flex; align-items: flex-start; gap: 6px; padding: 8px 10px; background: #f0f7f3; border-radius: 8px; }
.region-desc .el-icon { flex-shrink: 0; margin-top: 2px; }
.farmer-actions { display: flex; gap: 8px; margin-top: 16px; padding-top: 12px; border-top: 1px solid #f5f5f5; }
</style>
