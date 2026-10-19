<template>
  <div class="user-manage">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="searchKey" placeholder="搜索用户名或姓名..." clearable style="width: 240px">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
      </div>
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 添加用户
      </el-button>
    </div>

    <div class="stats-row">
      <div class="stat-chip"><span class="chip-label">总用户</span><span class="chip-num">{{ list.length }}</span></div>
      <div class="stat-chip"><span class="chip-label">管理员</span><span class="chip-num red">{{ list.filter(u => u.role === 'ADMIN').length }}</span></div>
      <div class="stat-chip"><span class="chip-label">商家</span><span class="chip-num orange">{{ list.filter(u => u.role === 'MERCHANT').length }}</span></div>
      <div class="stat-chip"><span class="chip-label">农户</span><span class="chip-num blue">{{ list.filter(u => u.role === 'FARMER').length }}</span></div>
      <div class="stat-chip"><span class="chip-label">客户</span><span class="chip-num green">{{ list.filter(u => u.role === 'CONSUMER').length }}</span></div>
      <div class="stat-chip"><span class="chip-label">正常</span><span class="chip-num blue">{{ list.filter(u => u.status === 1).length }}</span></div>
      <!-- 待审核商家：自助注册的商家 status=0，点一下只看这些，方便挨个审核 -->
      <div
        class="stat-chip clickable"
        :class="{ active: pendingOnly }"
        @click="pendingOnly = !pendingOnly"
      >
        <span class="chip-label">{{ pendingOnly ? '正在看：待审核商家' : '待审核商家' }}</span>
        <span class="chip-num orange">{{ pendingMerchants.length }}</span>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="filteredList" style="width: 100%">
        <el-table-column label="用户" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar" :style="{ background: roleColor(row.role) }">
                {{ (row.username || '?').charAt(0).toUpperCase() }}
              </div>
              <div>
                <div class="user-name">{{ row.username }}</div>
                <div class="user-real">{{ row.realName || '-' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" effect="dark" size="small">{{ roleText(row.role) }}</el-tag>
            <!-- 自助注册的商家在审核通过前状态为 0，这里明确标出来 -->
            <el-tag v-if="row.role === 'MERCHANT' && row.status === 0" type="info" effect="plain" size="small" style="margin-left: 4px">待审核</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="160">
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch v-model="row.status" :active-value="1" :inactive-value="0" size="small" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无用户" /></template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑用户' : '添加用户'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码" :required="!editId">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            minlength="6"
            maxlength="20"
            :placeholder="editId ? '留空则不修改密码，重置请输入 6-20 位' : '必填，6-20 位'"
          />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="系统管理员" value="ADMIN" /><el-option label="商家" value="MERCHANT" /><el-option label="农户" value="FARMER" /><el-option label="客户" value="CONSUMER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="正常" inactive-text="禁用" />
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
import { userApi } from '../../api'

const list = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const searchKey = ref('')
const form = ref({})
/** 是否只看待审核商家（status=0 且 role=MERCHANT） */
const pendingOnly = ref(false)
/** 待审核商家列表，用于统计条计数 */
const pendingMerchants = computed(() => list.value.filter(u => u.role === 'MERCHANT' && u.status === 0))

const roleText = (r) => ({ ADMIN: '系统管理员', MERCHANT: '商家', FARMER: '农户', CONSUMER: '客户' }[r] || r)
const roleTagType = (r) => ({ ADMIN: 'danger', MERCHANT: 'warning', FARMER: 'primary', CONSUMER: 'success' }[r] || 'info')
const roleColor = (r) => ({ ADMIN: 'linear-gradient(135deg,#f56c6c,#c45656)', MERCHANT: 'linear-gradient(135deg,#e6a23c,#b88230)', FARMER: 'linear-gradient(135deg,#409eff,#337ecc)', CONSUMER: 'linear-gradient(135deg,#67c23a,#529b2e)' }[r] || '#999')

const filteredList = computed(() => {
  // 待审核筛选优先：只看 status=0 的商家
  let rows = list.value
  if (pendingOnly.value) {
    rows = rows.filter(u => u.role === 'MERCHANT' && u.status === 0)
  }
  if (!searchKey.value) return rows
  return rows.filter(u => u.username?.includes(searchKey.value) || u.realName?.includes(searchKey.value))
})

const loadList = async () => { const res = await userApi.list(); list.value = res.data }

const openDialog = (row) => {
  if (row) { editId.value = row.id; form.value = { ...row, password: '' } }
  else { editId.value = null; form.value = { username: '', password: '', realName: '', phone: '', email: '', role: 'CONSUMER', status: 1 } }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!form.value.username) { ElMessage.warning('请输入用户名'); return }
  // 密码规则：新增必填；编辑留空表示不修改。非空时统一要求 6-20 位
  const pwd = form.value.password || ''
  if (!editId.value && !pwd) { ElMessage.warning('新增用户必须设置密码（6-20 位）'); return }
  if (pwd && (pwd.length < 6 || pwd.length > 20)) { ElMessage.warning('密码长度必须为 6-20 位'); return }
  if (editId.value) { await userApi.update(form.value) }
  else { await userApi.save(form.value) }
  ElMessage.success('保存成功'); dialogVisible.value = false; loadList()
}

const toggleStatus = async (row) => { await userApi.update({ id: row.id, status: row.status }); ElMessage.success(row.status === 1 ? '已启用' : '已禁用') }

const handleDelete = async (id) => {
  await ElMessageBox.confirm('确认删除该用户?', '提示', { type: 'warning' })
  await userApi.delete(id); ElMessage.success('删除成功'); loadList()
}

onMounted(loadList)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-chip { background: white; border-radius: 10px; padding: 12px 20px; display: flex; flex-direction: column; gap: 4px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); flex: 1; }
.chip-label { font-size: 12px; color: #999; }
.chip-num { font-size: 22px; font-weight: 700; color: #333; }
.chip-num.green { color: #67c23a; } .chip-num.red { color: #f56c6c; } .chip-num.orange { color: #e6a23c; } .chip-num.blue { color: #409eff; }
/* 可点击的统计块（待审核商家筛选） */
.stat-chip.clickable { cursor: pointer; transition: all 0.2s; }
.stat-chip.clickable:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.stat-chip.active { border: 1px solid #e6a23c; background: #fdf6ec; }
.table-card { overflow-x: auto; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.user-cell { display: flex; align-items: center; gap: 12px; }
.user-avatar { width: 36px; height: 36px; border-radius: 50%; color: white; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 600; flex-shrink: 0; }
.user-name { font-size: 14px; font-weight: 600; color: #333; }
.user-real { font-size: 12px; color: #999; }
</style>
