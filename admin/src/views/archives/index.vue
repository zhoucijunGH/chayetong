<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '@/api'
import type { Fruit, FruitForm } from '@/types'
import { fmtMoney } from '@/utils/format'

const keyword = ref('')
const enabled = ref('')
const loading = ref(false)
const list = ref<Fruit[]>([])

const enabledOptions = [
  { label: '全部状态', value: '' },
  { label: '启用', value: '1' },
  { label: '停用', value: '0' }
]

async function load() {
  loading.value = true
  try {
    list.value = await api.fruits.list({ keyword: keyword.value, enabled: enabled.value })
  } finally {
    loading.value = false
  }
}

function search() {
  load()
}

function reset() {
  keyword.value = ''
  enabled.value = ''
  load()
}

// ========== CRUD 弹窗 ==========
const dialogVisible = ref(false)
const dialogTitle = ref('新增果品')
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

function defaultForm(): FruitForm {
  return { name: '', spec: '', price: 0, unit: '斤', enabled: 1 }
}

const form = reactive<FruitForm>(defaultForm())

const rules: FormRules = {
  name: [{ required: true, message: '请输入品类名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

function openCreate() {
  editingId.value = null
  dialogTitle.value = '新增果品'
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row: Fruit) {
  editingId.value = row.id
  dialogTitle.value = '编辑果品'
  Object.assign(form, {
    name: row.name,
    spec: row.spec,
    price: row.price,
    unit: row.unit,
    enabled: row.enabled == null ? 1 : Number(row.enabled)
  })
  dialogVisible.value = true
}

async function save() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = { ...form, enabled: Number(form.enabled) ? 1 : 0 }
    if (editingId.value != null) {
      await api.fruits.update(editingId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await api.fruits.create(payload)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function remove(row: Fruit) {
  ElMessageBox.confirm(`确认删除果品「${row.name}」？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await api.fruits.remove(row.id)
      ElMessage.success('已删除')
      load()
    })
    .catch(() => {})
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-header">
      <h3>果品档案</h3>
      <div class="page-header-desc">维护收购的果品品类、规格、单价与启用状态</div>
    </div>

    <!-- 筛选栏 -->
    <div class="page-card query-bar">
      <el-input
        v-model="keyword"
        placeholder="品类 / 规格"
        clearable
        style="width: 200px"
        @keyup.enter="search"
      />
      <el-select v-model="enabled" style="width: 130px">
        <el-option v-for="o in enabledOptions" :key="o.value" :label="o.label" :value="o.value" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div style="flex: 1"></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增果品</el-button>
    </div>

    <!-- 果品表格 -->
    <div class="page-card">
      <el-table v-loading="loading" :data="list" stripe style="width: 100%">
        <el-table-column prop="name" label="品类" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="120">
          <template #default="{ row }">{{ row.spec || '-' }}</template>
        </el-table-column>
        <el-table-column label="单价(元)" width="120" align="right">
          <template #default="{ row }">{{ fmtMoney(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="100">
          <template #default="{ row }">{{ row.unit || '-' }}</template>
        </el-table-column>
        <el-table-column label="启用" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="Number(row.enabled) ? 'success' : 'info'" size="small">
              {{ Number(row.enabled) ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无果品档案" />
        </template>
      </el-table>
    </div>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="品类" prop="name">
          <el-input v-model="form.name" placeholder="如：沃柑、砂糖橘" clearable />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.spec" placeholder="如：80果、通货" clearable />
        </el-form-item>
        <el-form-item label="单价" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="如：斤、公斤、件" clearable />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped></style>
